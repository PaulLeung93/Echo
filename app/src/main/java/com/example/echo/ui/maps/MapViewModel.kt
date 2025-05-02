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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // ---UI State---
    private val _posts = MutableStateFlow<List<Post>>(emptyList())          // All posts
    private val _filteredPosts = MutableStateFlow<List<Post>>(emptyList())  // Posts matching the current filter
    val filteredPosts: StateFlow<List<Post>> = _filteredPosts

    private val _selectedPost = MutableStateFlow<Post?>(null)               // Currently selected post (map tap)
    val selectedPost: StateFlow<Post?> = _selectedPost

    private val _likesMap = MutableStateFlow<Map<String, Int>>(emptyMap())  // Post ID → like count
    val likesMap: StateFlow<Map<String, Int>> = _likesMap

    private val _commentsMap = MutableStateFlow<Map<String, Int>>(emptyMap()) // Post ID → comment count
    val commentsMap: StateFlow<Map<String, Int>> = _commentsMap

    private val _poiMarkers = MutableStateFlow<List<POI>>(emptyList())      // Points of interest
    val poiMarkers: StateFlow<List<POI>> = _poiMarkers

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
                _filteredPosts.value = fetched // show all by default
                fetchLikesAndComments(fetched.map { it.id })
            }
    }

    /**
     * Fetch like and comment counts for a list of post IDs.
     */
    private fun fetchLikesAndComments(postIds: List<String>) {
        val likeCounts = mutableMapOf<String, Int>()
        val commentCounts = mutableMapOf<String, Int>()

        postIds.forEach { postId ->
            // Likes
            db.collection(Constants.COLLECTION_POSTS).document(postId)
                .get()
                .addOnSuccessListener { doc ->
                    val likes = doc.get(Constants.FIELD_LIKES) as? List<*>
                    likeCounts[postId] = likes?.size ?: 0
                    _likesMap.value = likeCounts
                }

            // Comments
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

    /**
     * Triggered when a marker is tapped. Sets selected post and centers camera on it.
     */
    fun setSelectedPost(post: Post, camera: CameraPositionState) {
        _selectedPost.value = post

        if (post.latitude != null && post.longitude != null) {
            val target = LatLng(post.latitude, post.longitude)
            viewModelScope.launch {
                camera.animate(CameraUpdateFactory.newLatLngZoom(target, 14f))
            }
        }
    }

    // For clearing selection
    fun clearSelectedPost() {
        _selectedPost.value = null
    }


    /**
     * Applies a tag filter and re-centers map on nearest matching marker to user.
     */
    fun setTagFilter(tag: String, userLocation: LatLng?, camera: CameraPositionState?) {
        _selectedPost.value = null

        // Filter posts matching the tag (case-insensitive)
        val filtered = _posts.value.filter { post ->
            post.tags.any { it.equals(tag, ignoreCase = true) }
        }

        _filteredPosts.value = filtered

        // Recenter to nearest post if location is available
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
    }

    /**
     * Calculates the distance between two LatLng points in meters.
     */
    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0]
    }

    /**
     * Fetches all POI markers from Firestore.
     */
    fun fetchPOIMarkers() {
        db.collection("points_of_interest")
            .get()
            .addOnSuccessListener { snapshot ->
                val pois = snapshot.documents.mapNotNull { it.toObject(POI::class.java)?.copy(id = it.id) }
                _poiMarkers.value = pois
            }
    }
}
