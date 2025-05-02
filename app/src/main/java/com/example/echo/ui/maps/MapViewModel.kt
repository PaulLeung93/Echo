package com.example.echo.ui.map

import android.location.Location
import android.location.Location.distanceBetween
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.models.POI
import com.example.echo.models.Post
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

// Data class to represent a group of nearby posts (i.e., a cluster)
data class ClusterGroup(
    val position: LatLng,
    val posts: List<Post>
)

class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- UI State ---
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _filteredPosts = MutableStateFlow<List<Post>>(emptyList())
    val filteredPosts: StateFlow<List<Post>> = _filteredPosts

    private val _currentTagFilter = MutableStateFlow<String?>(null)
    val currentTagFilter: StateFlow<String?> = _currentTagFilter

    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost

    private val _likesMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val likesMap: StateFlow<Map<String, Int>> = _likesMap

    private val _userLikes = MutableStateFlow<Set<String>>(emptySet())
    val userLikes: StateFlow<Set<String>> = _userLikes

    private val _commentsMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val commentsMap: StateFlow<Map<String, Int>> = _commentsMap

    private val _poiMarkers = MutableStateFlow<List<POI>>(emptyList())
    val poiMarkers: StateFlow<List<POI>> = _poiMarkers

    private val _clusterGroups = MutableStateFlow<List<ClusterGroup>>(emptyList())
    val clusterGroups: StateFlow<List<ClusterGroup>> = _clusterGroups

    init {
        fetchPostsWithLocation()
        fetchPOIMarkers()
    }

    /**
     * Fetch all posts from Firestore with valid lat/lng.
     */
    private fun fetchPostsWithLocation() {
        db.collection(Constants.COLLECTION_POSTS)
            .get()
            .addOnSuccessListener { snapshot ->
                val fetched = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                    .filter { it.latitude != null && it.longitude != null }

                _posts.value = fetched
                _filteredPosts.value = fetched
                _clusterGroups.value = clusterPosts(fetched, zoom = 12f)
                fetchLikesAndComments(fetched.map { it.id })
            }
    }

    /**
     * Create clusters by grouping posts within a certain radius (in meters).
     * Radius adjusts based on zoom level. Zoom >= 17 disables clustering.
     */
    private fun clusterPosts(posts: List<Post>, zoom: Float): List<ClusterGroup> {
        val radiusInMeters = when {
            zoom >= 17 -> 0f
            zoom >= 15 -> 60f
            zoom >= 13 -> 120f
            else -> 200f
        }

        if (radiusInMeters == 0f) {
            return posts.map {
                ClusterGroup(LatLng(it.latitude!!, it.longitude!!), listOf(it))
            }
        }

        val clusters = mutableListOf<ClusterGroup>()
        val used = mutableSetOf<Post>()

        for (post in posts) {
            if (post in used || post.latitude == null || post.longitude == null) continue

            val cluster = mutableListOf(post)
            val latLng = LatLng(post.latitude!!, post.longitude!!)
            used.add(post)

            for (other in posts) {
                if (other in used || other.latitude == null || other.longitude == null) continue
                val otherLatLng = LatLng(other.latitude!!, other.longitude!!)
                if (distanceBetween(latLng, otherLatLng) <= radiusInMeters) {
                    cluster.add(other)
                    used.add(other)
                }
            }

            clusters.add(ClusterGroup(position = latLng, posts = cluster))
        }

        return clusters
    }

    /**
     * Recompute cluster groups based on zoom level and given posts.
     */
    fun refreshClusters(zoomLevel: Float, posts: List<Post>) {
        _clusterGroups.value = clusterPosts(posts, zoomLevel)
    }

    /**
     * Triggered when a marker is tapped. Sets selected post and centers camera.
     */
    fun setSelectedPost(post: Post, camera: CameraPositionState) {
        _selectedPost.value = post

        if (post.latitude != null && post.longitude != null) {
            val target = LatLng(post.latitude, post.longitude)
            viewModelScope.launch {
                camera.animate(CameraUpdateFactory.newLatLng(target))
            }
        }
    }

    /** Clears the selected post. */
    fun clearSelectedPost() {
        _selectedPost.value = null
    }

    /**
     * Applies a tag filter and re-centers map on nearest matching marker.
     */
    fun setTagFilter(tag: String, userLocation: LatLng?, camera: CameraPositionState?) {
        _selectedPost.value = null
        _currentTagFilter.value = tag

        val filtered = _posts.value.filter { post ->
            post.tags.any { it.equals(tag, ignoreCase = true) }
        }

        _filteredPosts.value = filtered
        _clusterGroups.value = clusterPosts(filtered, 12f)

        if (camera != null && userLocation != null && filtered.isNotEmpty()) {
            val nearest = filtered.minByOrNull { post ->
                val postLoc = LatLng(post.latitude!!, post.longitude!!)
                distanceBetween(userLocation, postLoc)
            }

            nearest?.let {
                val target = LatLng(it.latitude!!, it.longitude!!)
                viewModelScope.launch {
                    camera.animate(CameraUpdateFactory.newLatLngZoom(target, 13f))
                }
            }
        }
    }

    /**
     * Clears any applied tag filter.
     */
    fun clearTagFilter() {
        _selectedPost.value = null
        _filteredPosts.value = _posts.value
        _clusterGroups.value = clusterPosts(_posts.value, 12f)
        _currentTagFilter.value = null
    }

    /**
     * Calculates distance between two LatLng points in meters.
     */
    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0]
    }

    /**
     * Fetch POI markers from Firestore.
     */
    fun fetchPOIMarkers() {
        db.collection("points_of_interest")
            .get()
            .addOnSuccessListener { snapshot ->
                val pois = snapshot.documents.mapNotNull { it.toObject(POI::class.java)?.copy(id = it.id) }
                _poiMarkers.value = pois
            }
    }

    /**
     * Toggle like for a post.
     */
    fun toggleLike(postId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val docRef = db.collection(Constants.COLLECTION_POSTS).document(postId)

        val currentlyLiked = _userLikes.value.contains(postId)
        val updatedLikes = _likesMap.value.toMutableMap()
        val updatedUserLikes = _userLikes.value.toMutableSet()

        if (currentlyLiked) {
            docRef.update(Constants.FIELD_LIKES, FieldValue.arrayRemove(currentUserId))
            updatedLikes[postId] = (updatedLikes[postId] ?: 1) - 1
            updatedUserLikes.remove(postId)
        } else {
            docRef.update(Constants.FIELD_LIKES, FieldValue.arrayUnion(currentUserId))
            updatedLikes[postId] = (updatedLikes[postId] ?: 0) + 1
            updatedUserLikes.add(postId)
        }

        _likesMap.value = updatedLikes
        _userLikes.value = updatedUserLikes
    }

    /**
     * Fetch like and comment counts for each post.
     */
    private fun fetchLikesAndComments(postIds: List<String>) {
        val likeCounts = mutableMapOf<String, Int>()
        val commentCounts = mutableMapOf<String, Int>()

        val currentUserId = auth.currentUser?.uid

        postIds.forEach { postId ->
            if (currentUserId != null) {
                db.collection(Constants.COLLECTION_POSTS).document(postId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val likes = doc.get(Constants.FIELD_LIKES) as? List<*>
                        likeCounts[postId] = likes?.size ?: 0

                        if (likes?.contains(currentUserId) == true) {
                            _userLikes.value = _userLikes.value + postId
                        }

                        _likesMap.value = likeCounts
                    }
            }

            db.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .get()
                .addOnSuccessListener { snapshot ->
                    commentCounts[postId] = snapshot.size()
                    _commentsMap.value = commentCounts
                }
        }
    }
}
