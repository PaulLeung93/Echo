package com.example.echo.ui.map

import android.location.Location.distanceBetween
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.models.POI
import com.example.echo.models.Post
import com.example.echo.ui.map.MapUiState.*
import com.example.echo.utils.Constants
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Represents a group of nearby posts (clustered by location)
data class ClusterGroup(
    val position: LatLng,
    val posts: List<Post>
)

class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // UI state exposed to MapScreen
    private val _uiState = MutableStateFlow<MapUiState>(Loading)
    val uiState: StateFlow<MapUiState> = _uiState

    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost

    private val _poiMarkers = MutableStateFlow<List<POI>>(emptyList())
    val poiMarkers: StateFlow<List<POI>> = _poiMarkers

    private val _clusterGroups = MutableStateFlow<List<ClusterGroup>>(emptyList())
    val clusterGroups: StateFlow<List<ClusterGroup>> = _clusterGroups

    // All posts from firestore
    private var allPosts = emptyList<Post>()

    // Filter options
    private var currentTypeFilters = setOf("user posts", "landmark", "park", "college")
    private var currentTag: String? = null

    val markerTypes: Set<String>
        get() = currentTypeFilters


    init {
        fetchPostsWithLocation()
        fetchPOIMarkers()
    }

    /**
     * Fetch all posts from Firestore that include location data.
     */
    fun fetchPostsWithLocation() {
        viewModelScope.launch {
            _uiState.value = Loading
            try {
                val snapshot = db.collection(Constants.COLLECTION_POSTS).get().await()
                val fetched = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                    .filter { it.latitude != null && it.longitude != null }

                allPosts = fetched
                applyCurrentFilters()
            } catch (e: Exception) {
                _uiState.value = Error("Failed to load posts: ${e.message}")
            }
        }
    }

    /**
     * Apply current tag and marker type filters to the post list.
     */
    private fun applyCurrentFilters() {
        viewModelScope.launch {
            val filtered = if ("user posts" in currentTypeFilters) {
                allPosts.filter { post ->
                    val tagMatch = currentTag?.let { tag ->
                        post.tags.any { it.equals(tag, ignoreCase = true) }
                    } ?: true
                    tagMatch
                }
            } else {
                emptyList()
            }

            val (likes, liked, comments) = fetchLikesAndComments(filtered.map { it.id })

            _uiState.value = Success(
                posts = allPosts,
                filteredPosts = filtered,
                postLikes = likes,
                userLikes = liked,
                commentCount = comments,
                currentTag = currentTag
            )

            _clusterGroups.value = clusterPosts(filtered, 12f)
        }
    }

    /**
     * Set a tag to filter by.
     */
    fun setTagFilter(tag: String, userLocation: LatLng?, camera: CameraPositionState?) {
        currentTag = tag
        _selectedPost.value = null
        applyCurrentFilters()

        viewModelScope.launch {
            val filtered = (_uiState.value as? Success)?.filteredPosts ?: return@launch
            if (camera != null && filtered.isNotEmpty()) {
                val cameraLatLng = camera.position.target
                val nearest = filtered.minByOrNull {
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


    /**
     * Remove tag filter.
     */
    fun clearTagFilter() {
        currentTag = null
        _selectedPost.value = null
        applyCurrentFilters()
    }

    /**
     * Update which marker types (User Posts, POIs) are shown.
     */
    fun filterByMarkerTypes(types: Set<String>) {
        currentTypeFilters = types
        applyCurrentFilters()
    }

    /**
     * Cluster posts for current zoom level.
     */
    fun refreshClusters(zoom: Float, posts: List<Post>) {
        _clusterGroups.value = clusterPosts(posts, zoom)
    }

    /**
     * Helper to cluster posts based on distance and zoom.
     */
    private fun clusterPosts(posts: List<Post>, zoom: Float): List<ClusterGroup> {
        val radius = when {
            zoom >= 17 -> 0f
            zoom >= 15 -> 60f
            zoom >= 13 -> 120f
            else -> 200f
        }

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

    /**
     * Called when user taps on a post marker.
     */
    fun setSelectedPost(post: Post, camera: CameraPositionState) {
        _selectedPost.value = post
        val latLng = LatLng(post.latitude ?: return, post.longitude ?: return)
        viewModelScope.launch {
            camera.animate(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    /**
     * Clear selected post preview.
     */
    fun clearSelectedPost() {
        _selectedPost.value = null
    }

    /**
     * Load predefined points of interest from Firestore.
     */
    private fun fetchPOIMarkers() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("pois").get().await()
                val loaded = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    val type = doc.getString("type")
                    val description = doc.getString("description")
                    val geoPoint = doc.getGeoPoint("location")

                    if (name != null && type != null && geoPoint != null) {
                        POI(name, type, geoPoint, description ?: "")
                    } else null
                }
                _poiMarkers.value = loaded
                Log.d("MapViewModel", "Loaded POIs: ${loaded.size}")

            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to fetch POIs", e)
            }
        }
    }

    /**
     * Handle like/unlike action on post.
     */
    fun toggleLike(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection(Constants.COLLECTION_POSTS).document(postId)
        val current = _uiState.value as? Success ?: return

        val isLiked = current.userLikes.contains(postId)
        val updatedLikes = current.postLikes.toMutableMap()
        val updatedUserLikes = current.userLikes.toMutableSet()

        if (isLiked) {
            docRef.update(Constants.FIELD_LIKES, FieldValue.arrayRemove(userId))
            updatedLikes[postId] = (updatedLikes[postId] ?: 1) - 1
            updatedUserLikes.remove(postId)
        } else {
            docRef.update(Constants.FIELD_LIKES, FieldValue.arrayUnion(userId))
            updatedLikes[postId] = (updatedLikes[postId] ?: 0) + 1
            updatedUserLikes.add(postId)
        }

        _uiState.value = current.copy(
            postLikes = updatedLikes,
            userLikes = updatedUserLikes
        )
    }

    /**
     * Utility: Measure distance between two LatLng points in meters.
     */
    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0]
    }

    /**
     * Utility: Load like counts, comment counts, and whether current user liked each post.
     */
    private suspend fun fetchLikesAndComments(
        postIds: List<String>
    ): Triple<Map<String, Int>, Set<String>, Map<String, Int>> {
        val likesMap = mutableMapOf<String, Int>()
        val commentMap = mutableMapOf<String, Int>()
        val userLiked = mutableSetOf<String>()
        val userId = auth.currentUser?.uid

        postIds.forEach { postId ->
            val postRef = db.collection(Constants.COLLECTION_POSTS).document(postId)
            val snapshot = postRef.get().await()

            val likes = snapshot.get(Constants.FIELD_LIKES) as? List<*> ?: emptyList<Any>()
            likesMap[postId] = likes.size
            if (userId != null && likes.contains(userId)) {
                userLiked.add(postId)
            }

            val commentSnap = postRef.collection(Constants.COLLECTION_COMMENTS).get().await()
            commentMap[postId] = commentSnap.size()
        }

        return Triple(likesMap, userLiked, commentMap)
    }
}
