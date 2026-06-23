package dev.echoapp.echo.ui.maps

import android.location.Location.distanceBetween
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.model.Poi
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.repository.AuthRepository
import dev.echoapp.echo.domain.usecase.poi.GetPoisUseCase
import dev.echoapp.echo.domain.usecase.post.DeletePostUseCase
import dev.echoapp.echo.domain.usecase.post.GetPostsUseCase
import dev.echoapp.echo.domain.usecase.post.ToggleLikeUseCase
import dev.echoapp.echo.domain.usecase.post.UpdatePostUseCase
import dev.echoapp.echo.data.preferences.UserPreferencesRepository
import dev.echoapp.echo.domain.usecase.user.ObserveHiddenAuthorIdsUseCase
import dev.echoapp.echo.ui.common.MapFocusManager
import dev.echoapp.echo.utils.Constants
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val getPoisUseCase: GetPoisUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val userPreferences: UserPreferencesRepository,
    private val mapFocusManager: MapFocusManager,
    observeHiddenAuthorIdsUseCase: ObserveHiddenAuthorIdsUseCase,
    authRepository: AuthRepository
) : ViewModel() {

    /** Current user's uid and guest flag, to gate owner-only edit/delete on map cards. */
    val currentUserId: String? = authRepository.getCurrentUser()?.id
    val isGuest: Boolean = authRepository.getCurrentUser()?.isAnonymous != false

    /** One-shot messages (e.g. a failed delete/edit) shown as snackbars. */
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    /** Pending "open this post on the map" request from the feed; null when none. */
    val mapFocus = mapFocusManager.request

    /** Clear the focus request once the map has centered on and selected the post. */
    fun consumeMapFocus() = mapFocusManager.consume()

    private val blockedIds: Flow<Set<String>> = observeHiddenAuthorIdsUseCase()

    private val _currentTag = MutableStateFlow<String?>(null)

    /**
     * Active marker-type filters, persisted across sessions via DataStore so a user's
     * chosen view survives restarts. [filterByMarkerTypes] writes the preference and
     * this flow re-emits, keeping it the single source of truth.
     */
    private val activeFilters: StateFlow<Set<String>> = userPreferences.mapMarkerFilters
        .stateIn(viewModelScope, SharingStarted.Eagerly, Constants.DEFAULT_MAP_FILTERS)

    private val _selectedPost = MutableStateFlow<Post?>(null)
    private val _selectedCluster = MutableStateFlow<ClusterGroup?>(null)
    private val _selectedPoiId = MutableStateFlow<String?>(null)

    /**
     * Posts deleted in this session, hidden from the markers optimistically. The map's
     * post source is a one-shot viewport query (not a live listener), so without this a
     * deleted post's marker would linger until the next bounds-triggered re-fetch.
     * Reverted if the server delete fails.
     */
    private val _deletedPostIds = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Bumped to force [nearbyPosts] to re-run its viewport query even when the bounds
     * haven't changed — e.g. on returning to the map after creating a post elsewhere,
     * so the new marker (and any counts mutated off-screen) show up.
     */
    private val _refreshKey = MutableStateFlow(0)

    /**
     * Cluster radius (meters) derived from the camera zoom *bucket*, not the raw
     * zoom. The radius only steps at zoom 13/15/17, so collapsing the continuous
     * zoom to its bucket here lets the StateFlow de-dupe identical values — every
     * fractional zoom delta no longer re-runs the O(n²) clustering.
     */
    private val _clusterRadius = MutableStateFlow(clusterRadiusForZoom(12f))

    /**
     * The current map viewport (padded), pushed from the screen whenever the camera
     * comes to rest. Markers are culled to this box so we only cluster/draw what's on
     * screen instead of every fetched post/POI. `null` until the map first settles,
     * in which case nothing is culled (we show everything we have).
     */
    private val _visibleBounds = MutableStateFlow<LatLngBounds?>(null)

    /**
     * Whether the camera is zoomed in far enough to load posts (see
     * [Constants.MIN_POSTS_ZOOM]). Posts are fetched by an uncapped viewport-radius
     * query, so when zoomed out past this we skip the query entirely rather than read
     * every post in a country-sized radius. Updated from [updateZoom].
     */
    private val _postsZoomEnabled = MutableStateFlow(false)

    /**
     * Whether the camera is zoomed in far enough to draw POI markers (see
     * [Constants.MIN_POIS_ZOOM]). POIs cost no reads (cache-loaded), so this is purely
     * to declutter the overlapping-disc "blob" at far zoom. Updated from [updateZoom].
     */
    private val _poisZoomEnabled = MutableStateFlow(false)

    /**
     * Located posts near the current viewport, fetched via a geohash range query each
     * time the camera settles (new bounds). This replaces the old newest-200 live
     * listener, so the map only reads posts near what's on screen. Empty until the
     * first bounds arrive.
     */
    private val nearbyPosts: Flow<List<Post>> = combine(
        _visibleBounds,
        _postsZoomEnabled,
        _refreshKey
    ) { bounds, zoomEnabled, key -> Triple(bounds, zoomEnabled, key) }
        .distinctUntilChanged()
        .flatMapLatest { (bounds, zoomEnabled, _) ->
        flow {
            // No query when zoomed out past the threshold, or before the first settle.
            if (bounds == null || !zoomEnabled) {
                emit(emptyList())
                return@flow
            }
            val center = bounds.center
            // Radius = center→corner, so the circle covers the (padded) viewport box.
            val radius = distanceBetween(center, bounds.northeast).toDouble()
            emit(
                runCatching {
                    getPostsUseCase.near(center.latitude, center.longitude, radius)
                }.getOrDefault(emptyList())
            )
        }
    }

    /**
     * The post source feeding the markers. With no tag we use the viewport-bounded
     * [nearbyPosts]; with a tag we fall back to the recent-posts listener so a tag
     * search can fly to a match *anywhere*, not just within the current view.
     */
    private val postSource: Flow<List<Post>> = _currentTag.flatMapLatest { tag ->
        if (tag == null) {
            nearbyPosts
        } else {
            getPostsUseCase().map { posts ->
                posts.filter { it.tags.any { t -> t.equals(tag, ignoreCase = true) } }
            }
        }
    }

    /**
     * Posts that can be plotted as standalone markers: located, pass the type filter,
     * and not from a blocked user. POI posts ([Post.poiId] set) are excluded — they
     * live inside their POI's thread and must never draw their own user marker (their
     * coordinates are the POI's, so without this they'd pin on top of the POI disc).
     */
    private val mappablePosts: Flow<List<Post>> = combine(
        postSource,
        activeFilters,
        blockedIds,
        _deletedPostIds
    ) { posts, filters, blocked, deleted ->
        if ("user posts" in filters) {
            posts.filter {
                it.latitude != null && it.longitude != null &&
                    it.poiId == null && it.authorId !in blocked &&
                    it.id !in deleted
            }
        } else {
            emptyList()
        }
    }

    /**
     * Plottable posts culled to the current viewport (plus margin). Clustering and
     * marker drawing both work off this smaller set, so off-screen posts cost nothing.
     * When bounds are unknown (map not yet settled) we pass everything through.
     */
    private val visiblePosts: Flow<List<Post>> = combine(
        mappablePosts,
        _visibleBounds
    ) { posts, bounds ->
        if (bounds == null) posts
        else posts.filter { bounds.contains(LatLng(it.latitude!!, it.longitude!!)) }
    }

    /**
     * Clusters live on their own upstream so they recompute only when the visible
     * posts or the zoom bucket change — selecting a marker (which only touches the
     * selection flows below) no longer re-clusters every post.
     */
    private val clusters: Flow<List<ClusterGroup>> = combine(
        visiblePosts,
        _clusterRadius
    ) { posts, radius ->
        clusterPosts(posts, radius)
    }

    /** All POIs matching the active type filter (used for selection lookup). */
    private val filteredPois: Flow<List<Poi>> = combine(
        getPoisUseCase(),
        activeFilters
    ) { pois, filters ->
        pois.filter { it.type.lowercase() in filters }
    }

    /** POIs culled to the viewport — only these get drawn as markers. */
    private val visiblePois: Flow<List<Poi>> = combine(
        filteredPois,
        _visibleBounds,
        _poisZoomEnabled
    ) { pois, bounds, zoomEnabled ->
        when {
            // Zoomed out past the declutter threshold → draw no POI markers.
            !zoomEnabled -> emptyList()
            bounds == null -> pois
            else -> pois.filter { bounds.contains(LatLng(it.latitude, it.longitude)) }
        }
    }

    /** Viewport-culled markers, grouped so they flow as one value into [uiState]. */
    private val renderData: Flow<RenderData> = combine(
        clusters,
        visiblePois
    ) { clusterGroups, pois ->
        RenderData(clusterGroups, pois)
    }

    private val selection: Flow<Selection> = combine(
        _selectedPost,
        _selectedCluster,
        _selectedPoiId
    ) { post, cluster, poiId ->
        Selection(post, cluster, poiId)
    }

    private val filterState: Flow<FilterState> = combine(
        _currentTag,
        activeFilters,
        _postsZoomEnabled
    ) { tag, filters, postsEnabled ->
        FilterState(tag, filters, postsZoomedOut = !postsEnabled)
    }

    val uiState: StateFlow<MapUiState> = combine(
        mappablePosts,
        renderData,
        filteredPois,
        selection,
        filterState
    ) { posts, render, allPois, sel, filters ->
        MapUiState(
            // `posts` stays the full set so tag search can target off-screen posts;
            // only the drawn markers (clusters/pois) are viewport-culled.
            posts = posts,
            pois = render.pois,
            clusters = render.clusters,
            selectedPost = sel.post,
            selectedCluster = sel.cluster,
            selectedPoi = allPois.find { it.id == sel.poiId },
            currentTag = filters.tag,
            activeFilters = filters.active,
            postsZoomedOut = filters.postsZoomedOut,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState(isLoading = true)
    )

    /** Selection state grouped so it flows as one value into the [uiState] combine. */
    private data class Selection(
        val post: Post?,
        val cluster: ClusterGroup?,
        val poiId: String?
    )

    /** Tag + marker-type filters grouped so they flow as one value into [uiState]. */
    private data class FilterState(
        val tag: String?,
        val active: Set<String>,
        val postsZoomedOut: Boolean
    )

    /** Viewport-culled markers (post clusters + POIs) for the current frame. */
    private data class RenderData(
        val clusters: List<ClusterGroup>,
        val pois: List<Poi>
    )

    fun setTagFilter(tag: String, camera: CameraPositionState?) {
        _currentTag.value = tag
        _selectedPost.value = null
        _selectedPoiId.value = null
        
        viewModelScope.launch {
            val state = uiState.value
            if (camera != null && state.posts.isNotEmpty()) {
                val cameraLatLng = camera.position.target
                val nearest = state.posts.minByOrNull {
                    distanceBetween(cameraLatLng, LatLng(it.latitude!!, it.longitude!!))
                }

                nearest?.let {
                    camera.animateSafely(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude!!, it.longitude!!),
                            camera.position.zoom
                        )
                    )
                }
            }
        }
    }

    fun clearTagFilter() {
        _currentTag.value = null
        _selectedPost.value = null
        _selectedPoiId.value = null
    }

    fun filterByMarkerTypes(types: Set<String>) {
        // Persist the choice; the activeFilters flow re-emits and updates the map.
        viewModelScope.launch { userPreferences.setMapMarkerFilters(types) }
    }

    fun updateZoom(zoom: Float) {
        // Map to the discrete cluster radius so the StateFlow only emits (and only
        // re-clusters) when the zoom crosses a bucket boundary, not on every frame.
        _clusterRadius.value = clusterRadiusForZoom(zoom)
        // Gate post loading: zoomed out past the threshold, don't query posts at all.
        _postsZoomEnabled.value = zoom >= Constants.MIN_POSTS_ZOOM
        // Gate POI drawing (cache-loaded, so this is declutter-only, not a read saving).
        _poisZoomEnabled.value = zoom >= Constants.MIN_POIS_ZOOM
    }

    /**
     * Push the latest map viewport so markers can be culled to what's on screen.
     * Called when the camera settles; the bounds are padded so markers just off the
     * edges are kept loaded and don't pop in while panning.
     */
    fun updateVisibleBounds(bounds: LatLngBounds) {
        // Skip the update when the previous (padded) box still fully contains the new
        // viewport: the posts we already fetched cover it, so a small pan/zoom that
        // revealed nothing new shouldn't re-run the (billed) geohash query batch. Only
        // a move that exposes area outside the padded box triggers a fresh fetch.
        val current = _visibleBounds.value
        if (current != null &&
            current.contains(bounds.northeast) &&
            current.contains(bounds.southwest)
        ) {
            return
        }
        _visibleBounds.value = bounds.padded()
    }

    fun setSelectedPost(post: Post, camera: CameraPositionState) {
        _selectedPost.value = post
        _selectedCluster.value = null
        _selectedPoiId.value = null
        val latLng = LatLng(post.latitude ?: return, post.longitude ?: return)
        viewModelScope.launch {
            camera.animateSafely(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    fun setSelectedPoi(poi: Poi, camera: CameraPositionState) {
        _selectedPoiId.value = poi.id
        _selectedPost.value = null
        _selectedCluster.value = null
        val latLng = LatLng(poi.latitude, poi.longitude)
        viewModelScope.launch {
            camera.animateSafely(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    fun clearSelectedPost() {
        _selectedPost.value = null
        _selectedCluster.value = null
        _selectedPoiId.value = null
    }

    fun onClusterClick(cluster: ClusterGroup, camera: CameraPositionState) {
        _selectedCluster.value = cluster
        _selectedPost.value = cluster.posts.firstOrNull()
        _selectedPoiId.value = null
        viewModelScope.launch {
            camera.animateSafely(CameraUpdateFactory.newLatLng(cluster.position))
        }
    }

    fun onSelectedPostChanged(post: Post) {
        _selectedPost.value = post
    }

    /**
     * Re-run the viewport post query without waiting for a camera move. Call when the
     * map regains focus so a post created (or liked/edited/deleted) on another screen is
     * reflected, since the post source is a one-shot query rather than a live listener.
     */
    fun refresh() {
        _refreshKey.value++
    }

    fun toggleLike(postId: String) {
        // The map's post source is a one-shot query, and the selection cards render from
        // captured Post snapshots — so flip the snapshot optimistically (the heart/count
        // update instantly), then write through and roll back with a message on failure.
        val current = findSelectedPost(postId)
        if (current != null) {
            val newLiked = !current.likedByCurrentUser
            val newCount = (current.likeCount + if (newLiked) 1 else -1).coerceAtLeast(0)
            patchSelectedPost(postId) {
                it.copy(likedByCurrentUser = newLiked, likeCount = newCount)
            }
        }
        viewModelScope.launch {
            toggleLikeUseCase(postId).onFailure { e ->
                if (current != null) {
                    patchSelectedPost(postId) {
                        it.copy(
                            likedByCurrentUser = current.likedByCurrentUser,
                            likeCount = current.likeCount
                        )
                    }
                }
                _uiEvent.send(e.message ?: "Couldn't update your like. Please try again.")
            }
        }
    }

    /** Delete one of the current user's own posts, then dismiss the selection card. */
    fun deletePost(postId: String) {
        // Hide the marker immediately (the one-shot source won't drop it on its own) and
        // dismiss the card; restore it if the server delete fails.
        _deletedPostIds.update { it + postId }
        clearSelectedPost()
        viewModelScope.launch {
            deletePostUseCase(postId)
                .onFailure { e ->
                    _deletedPostIds.update { it - postId }
                    _uiEvent.send(e.message ?: "Couldn't delete the post. Please try again.")
                }
        }
    }

    /** Edit the message of one of the current user's own posts. */
    fun updatePost(postId: String, newMessage: String) {
        viewModelScope.launch {
            updatePostUseCase(postId, newMessage)
                .onSuccess {
                    // Reflect the edit in any open card snapshot right away.
                    patchSelectedPost(postId) { it.copy(message = newMessage) }
                }
                .onFailure { e ->
                    _uiEvent.send(e.message ?: "Couldn't update the post. Please try again.")
                }
        }
    }

    /** The currently-selected post (single or within the open cluster) matching [postId]. */
    private fun findSelectedPost(postId: String): Post? =
        _selectedPost.value?.takeIf { it.id == postId }
            ?: _selectedCluster.value?.posts?.find { it.id == postId }

    /** Apply [transform] to the matching post in both the single and cluster selections. */
    private fun patchSelectedPost(postId: String, transform: (Post) -> Post) {
        _selectedPost.update { post ->
            if (post?.id == postId) transform(post) else post
        }
        _selectedCluster.update { cluster ->
            cluster?.copy(posts = cluster.posts.map { if (it.id == postId) transform(it) else it })
        }
    }

    /**
     * Expand the viewport by [fraction] on each edge so markers just outside the
     * screen stay loaded, avoiding visible pop-in while panning. Latitude is clamped
     * to the valid range; longitude isn't wrapped (fine for this app's coverage).
     */
    private fun LatLngBounds.padded(fraction: Double = 0.3): LatLngBounds {
        val latPad = (northeast.latitude - southwest.latitude) * fraction
        val lngPad = (northeast.longitude - southwest.longitude) * fraction
        return LatLngBounds(
            LatLng(
                (southwest.latitude - latPad).coerceAtLeast(-90.0),
                southwest.longitude - lngPad
            ),
            LatLng(
                (northeast.latitude + latPad).coerceAtMost(90.0),
                northeast.longitude + lngPad
            )
        )
    }

    /** The cluster radius (meters) for a given zoom. Bucketed so it only steps at 13/15/17. */
    private fun clusterRadiusForZoom(zoom: Float): Float = when {
        zoom >= 17 -> 0f
        zoom >= 15 -> 60f
        zoom >= 13 -> 120f
        else -> 200f
    }

    private fun clusterPosts(posts: List<Post>, radius: Float): List<ClusterGroup> {
        if (radius == 0f) {
            return posts.map {
                ClusterGroup(LatLng(it.latitude!!, it.longitude!!), listOf(it))
            }
        }

        val clusters = mutableListOf<ClusterGroup>()
        val used = mutableSetOf<Post>()

        for (post in posts) {
            if (post in used || post.latitude == null || post.longitude == null) continue

            val cluster = mutableListOf(post)
            val latLng = LatLng(post.latitude, post.longitude)
            used.add(post)

            for (other in posts) {
                if (other in used || other.latitude == null || other.longitude == null) continue
                val otherLatLng = LatLng(other.latitude, other.longitude)
                if (distanceBetween(latLng, otherLatLng) <= radius) {
                    cluster.add(other)
                    used.add(other)
                }
            }

            clusters.add(ClusterGroup(latLng, cluster))
        }

        return clusters
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0]
    }
}
