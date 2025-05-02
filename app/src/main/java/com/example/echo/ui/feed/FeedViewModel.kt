package com.example.echo.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.models.Post
import com.example.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _filteredPosts = MutableStateFlow<List<Post>>(emptyList())
    val filteredPosts: StateFlow<List<Post>> = _filteredPosts

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _postLikes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val postLikes: StateFlow<Map<String, Int>> = _postLikes

    private val _userLikes = MutableStateFlow<Set<String>>(emptySet())
    val userLikes: StateFlow<Set<String>> = _userLikes

    private val _commentLikes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val commentLikes: StateFlow<Map<String, Int>> = _commentLikes

    private var currentFilter: String? = null

    init {
        fetchPosts()
    }

    fun fetchPosts() {
        db.collection(Constants.COLLECTION_POSTS)
            .orderBy(Constants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedPosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)
                }
                _posts.value = fetchedPosts
                applyTagFilter()
                fetchLikesAndComments(fetchedPosts.map { it.id })
            }
            .addOnFailureListener {
                // Handle error if needed
            }
    }

    fun refreshPosts() {
        _isRefreshing.value = true
        db.collection(Constants.COLLECTION_POSTS)
            .orderBy(Constants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val refreshedPosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)
                }
                _posts.value = refreshedPosts
                applyTagFilter()
                _isRefreshing.value = false
            }
            .addOnFailureListener {
                _isRefreshing.value = false
            }
    }

    fun fetchLikesAndComments(postIds: List<String>) {
        val currentUserId = auth.currentUser?.uid ?: return
        val updatedLikes = mutableMapOf<String, Int>()
        val likedPosts = mutableSetOf<String>()
        val updatedCommentCounts = mutableMapOf<String, Int>()

        postIds.forEach { postId ->
            val postRef = db.collection(Constants.COLLECTION_POSTS).document(postId)

            postRef.get().addOnSuccessListener { snapshot ->
                val likes = snapshot.get(Constants.FIELD_LIKES) as? List<*>
                updatedLikes[postId] = likes?.size ?: 0
                if (likes?.contains(currentUserId) == true) {
                    likedPosts.add(postId)
                }
                _postLikes.value = updatedLikes
                _userLikes.value = likedPosts
            }

            postRef.collection(Constants.COLLECTION_COMMENTS).get()
                .addOnSuccessListener { commentSnapshot ->
                    updatedCommentCounts[postId] = commentSnapshot.size()
                    _commentLikes.value = updatedCommentCounts
                }
        }
    }

    fun toggleLike(postId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val docRef = db.collection(Constants.COLLECTION_POSTS).document(postId)

        val isLiked = _userLikes.value.contains(postId)
        val newUserLikes = _userLikes.value.toMutableSet()
        val newPostLikes = _postLikes.value.toMutableMap()

        if (isLiked) {
            docRef.update(Constants.FIELD_LIKES, FieldValue.arrayRemove(currentUserId))
            newUserLikes.remove(postId)
            newPostLikes[postId] = (newPostLikes[postId] ?: 1) - 1
        } else {
            docRef.update(Constants.FIELD_LIKES, FieldValue.arrayUnion(currentUserId))
            newUserLikes.add(postId)
            newPostLikes[postId] = (newPostLikes[postId] ?: 0) + 1
        }

        _userLikes.value = newUserLikes
        _postLikes.value = newPostLikes
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
}
