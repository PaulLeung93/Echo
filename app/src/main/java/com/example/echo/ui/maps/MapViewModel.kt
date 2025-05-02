package com.example.echo.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.models.Post
import com.example.echo.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _filteredPosts = MutableStateFlow<List<Post>>(emptyList())
    val filteredPosts: StateFlow<List<Post>> = _filteredPosts

    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost

    private val _likesMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val likesMap: StateFlow<Map<String, Int>> = _likesMap

    private val _commentsMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val commentsMap: StateFlow<Map<String, Int>> = _commentsMap

    private var currentFilter: String? = null

    init {
        fetchPostsWithLocation()
    }

    private fun fetchPostsWithLocation() {
        db.collection(Constants.COLLECTION_POSTS)
            .get()
            .addOnSuccessListener { snapshot ->
                val fetched = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                    .filter { it.latitude != null && it.longitude != null }

                _posts.value = fetched
                applyTagFilter()
                fetchLikesAndComments(fetched.map { it.id })
            }
    }

    private fun fetchLikesAndComments(postIds: List<String>) {
        val likeCounts = mutableMapOf<String, Int>()
        val commentCounts = mutableMapOf<String, Int>()

        postIds.forEach { postId ->
            db.collection(Constants.COLLECTION_POSTS).document(postId)
                .get()
                .addOnSuccessListener { doc ->
                    val likes = doc.get(Constants.FIELD_LIKES) as? List<*>
                    likeCounts[postId] = likes?.size ?: 0
                    _likesMap.value = likeCounts
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

    fun setSelectedPost(post: Post) {
        _selectedPost.value = post
    }

    fun setTagFilter(tag: String) {
        currentFilter = tag
        applyTagFilter()
    }

    fun clearTagFilter() {
        currentFilter = null
        _filteredPosts.value = _posts.value
    }

    private fun applyTagFilter() {
        _filteredPosts.value = if (!currentFilter.isNullOrBlank()) {
            _posts.value.filter { it.tags.any { tag -> tag.equals(currentFilter, ignoreCase = true) } }
        } else {
            _posts.value
        }
    }

    fun getLikesCount(postId: String): Int {
        return _likesMap.value[postId] ?: 0
    }

    fun getCommentCount(postId: String): Int {
        return _commentsMap.value[postId] ?: 0
    }
}
