package com.example.echo.ui.maps

import android.location.Location.distanceBetween
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.model.Poi
import com.example.echo.domain.model.Post
import com.example.echo.domain.usecase.poi.GetPoisUseCase
import com.example.echo.domain.usecase.post.GetPostsUseCase
import com.example.echo.domain.usecase.post.ToggleLikeUseCase
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val getPoisUseCase: GetPoisUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase
) : ViewModel() {

    private val _currentTag = MutableStateFlow<String?>(null)
    private val _activeFilters = MutableStateFlow(setOf("user posts", "landmark", "park", "college"))
    private val _selectedPost = MutableStateFlow<Post?>(null)
    private val _selectedCluster = MutableStateFlow<ClusterGroup?>(null)
    private val _selectedPoiId = MutableStateFlow<String?>(null)

    /**
     * Cluster radius (meters) derived from the camera zoom *bucket*, not the raw
     * zoom. The radius only steps at zoom 13/15/17, so collapsing the continuous
     * zoom to its bucket here lets the StateFlow de-dupe identical values — every
     * fractional zoom delta no longer re-runs the O(n²) clustering.
     */
    private val _clusterRadius = MutableStateFlow(clusterRadiusForZoom(12f))

    /** Posts that can be plotted: have a location and match the active tag/type filter. */
    private val mappablePosts: Flow<List<Post>> = combine(
        getPostsUseCase(),
        _currentTag,
        _activeFilters
    ) { posts, tag, filters ->
        if ("user posts" in filters) {
            posts.filter { post ->
                post.latitude != null && post.longitude != null &&
                    (tag == null || post.tags.any { it.equals(tag, ignoreCase = true) })
            }
        } else {
            emptyList()
        }
    }

    /**
     * Clusters live on their own upstream so they recompute only when the plottable
     * posts or the zoom bucket change — selecting a marker (which only touches the
     * selection flows below) no longer re-clusters every post.
     */
    private val clusters: Flow<List<ClusterGroup>> = combine(
        mappablePosts,
        _clusterRadius
    ) { posts, radius ->
        clusterPosts(posts, radius)
    }

    private val filteredPois: Flow<List<Poi>> = combine(
        getPoisUseCase(),
        _activeFilters
    ) { pois, filters ->
        pois.filter { it.type.lowercase() in filters }
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
        _activeFilters
    ) { tag, filters ->
        FilterState(tag, filters)
    }

    val uiState: StateFlow<MapUiState> = combine(
        mappablePosts,
        clusters,
        filteredPois,
        selection,
        filterState
    ) { posts, clusterGroups, pois, sel, filters ->
        MapUiState(
            posts = posts,
            pois = pois,
            clusters = clusterGroups,
            selectedPost = sel.post,
            selectedCluster = sel.cluster,
            selectedPoi = pois.find { it.id == sel.poiId },
            currentTag = filters.tag,
            activeFilters = filters.active,
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
        val active: Set<String>
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
                    camera.animate(
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
        _activeFilters.value = types
    }

    fun updateZoom(zoom: Float) {
        // Map to the discrete cluster radius so the StateFlow only emits (and only
        // re-clusters) when the zoom crosses a bucket boundary, not on every frame.
        _clusterRadius.value = clusterRadiusForZoom(zoom)
    }

    fun setSelectedPost(post: Post, camera: CameraPositionState) {
        _selectedPost.value = post
        _selectedCluster.value = null
        _selectedPoiId.value = null
        val latLng = LatLng(post.latitude ?: return, post.longitude ?: return)
        viewModelScope.launch {
            camera.animate(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    fun setSelectedPoi(poi: Poi, camera: CameraPositionState) {
        _selectedPoiId.value = poi.id
        _selectedPost.value = null
        _selectedCluster.value = null
        val latLng = LatLng(poi.latitude, poi.longitude)
        viewModelScope.launch {
            camera.animate(CameraUpdateFactory.newLatLng(latLng))
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
            camera.animate(CameraUpdateFactory.newLatLng(cluster.position))
        }
    }

    fun onSelectedPostChanged(post: Post) {
        _selectedPost.value = post
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                toggleLikeUseCase(postId)
            } catch (e: Exception) {
                // handle error
            }
        }
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
