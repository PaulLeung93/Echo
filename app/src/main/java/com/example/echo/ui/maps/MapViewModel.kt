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

// Data class to represent a group of nearby posts (i.e., a cluster)
data class ClusterGroup(
    val position: LatLng,
    val posts: List<Post>
)

class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- UI State ---
    private val _uiState = MutableStateFlow<MapUiState>(Loading)
    val uiState: StateFlow<MapUiState> = _uiState

    // All posts from Firestore
    private var allPosts = emptyList<Post>()

    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost

    private val _poiMarkers = MutableStateFlow<List<POI>>(emptyList())
    val poiMarkers: StateFlow<List<POI>> = _poiMarkers

    private val _clusterGroups = MutableStateFlow<List<ClusterGroup>>(emptyList())
    val clusterGroups: StateFlow<List<ClusterGroup>> = _clusterGroups

    init {
        fetchPostsWithLocation()
        fetchPOIMarkers()
    }

    /**
     * Fetch all posts from Firestore that have valid lat/lng.
     * Also fetch likes/comments and initialize cluster groups.
     */
    private fun fetchPostsWithLocation() {
        viewModelScope.launch {
            _uiState.value = Loading
            try {
                val snapshot = db.collection(Constants.COLLECTION_POSTS).get().await()
                val fetched = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                    .filter { it.latitude != null && it.longitude != null }

                allPosts = fetched
                val postIds = fetched.map { it.id }

                val (likes, liked, comments) = fetchLikesAndComments(postIds)

                _uiState.value = Success(
                    posts = fetched,
                    filteredPosts = fetched,
                    postLikes = likes,
                    userLikes = liked,
                    commentCount = comments
                )

                _clusterGroups.value = clusterPosts(fetched, zoom = 12f)

            } catch (e: Exception) {
                _uiState.value = Error("Failed to load posts: ${e.message}")
            }
        }
    }

    /**
     * Create clusters by grouping posts that are nearby, based on zoom level.
     */
    private fun clusterPosts(posts: List<Post>, zoom: Float): List<ClusterGroup> {
        val radius = when {
            zoom >= 17 -> 0f
            zoom >= 15 -> 60f
            zoom >= 13 -> 120f
            else -> 200f
        }

        if (radius == 0f) {
            return posts.map { ClusterGroup(LatLng(it.latitude!!, it.longitude!!), listOf(it)) }
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
     * Refresh clusters when zoom level changes.
     */
    fun refreshClusters(zoom: Float, posts: List<Post>) {
        _clusterGroups.value = clusterPosts(posts, zoom)
    }

    /**
     * Triggered when a marker is tapped. Sets the selected post and moves the camera to it.
     */
    fun setSelectedPost(post: Post, camera: CameraPositionState) {
        _selectedPost.value = post
        val latLng = LatLng(post.latitude ?: return, post.longitude ?: return)
        viewModelScope.launch {
            camera.animate(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    /** Clears the selected post from state. */
    fun clearSelectedPost() {
        _selectedPost.value = null
    }

    /**
     * Filters posts by a specific tag and optionally recenters on the nearest one.
     */
    fun setTagFilter(tag: String, userLocation: LatLng?, camera: CameraPositionState?) {
        _selectedPost.value = null
        val filtered = allPosts.filter { post ->
            post.tags.any { it.equals(tag, ignoreCase = true) }
        }

        viewModelScope.launch {
            val (likes, liked, comments) = fetchLikesAndComments(filtered.map { it.id })

            _uiState.value = Success(
                posts = allPosts,
                filteredPosts = filtered,
                postLikes = likes,
                userLikes = liked,
                commentCount = comments,
                currentTag = tag
            )
            _clusterGroups.value = clusterPosts(filtered, 12f)

            if (userLocation != null && camera != null && filtered.isNotEmpty()) {
                val nearest = filtered.minByOrNull {
                    distanceBetween(userLocation, LatLng(it.latitude!!, it.longitude!!))
                }

                nearest?.let {
                    camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude!!, it.longitude!!), 13f))
                }
            }
        }
    }

    /**
     * Clears any tag filter and resets the full map state.
     */
    fun clearTagFilter() {
        _selectedPost.value = null
        (_uiState.value as? Success)?.let { current ->
            _uiState.value = current.copy(
                filteredPosts = allPosts,
                currentTag = null
            )
            _clusterGroups.value = clusterPosts(allPosts, 12f)
        }
    }

    /**
     * Loads point-of-interest (POI) markers for static locations on the map.
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
     * Toggles like on a post and updates both Firestore and local state.
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
     * Utility: Calculates the distance in meters between two LatLng points.
     */
    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0]
    }

    /**
     * Utility: Fetches like counts, comment counts, and user-liked posts.
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
