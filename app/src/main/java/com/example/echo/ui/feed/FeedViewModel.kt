package com.example.echo.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.models.Post
import com.example.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // UI State
    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState

    // Single Source of Truth
    private var allPosts: List<Post> = emptyList()

    init {
        fetchPosts()
    }

    /**
     * Fetch all posts from Firestore and update UI state.
     */
    fun fetchPosts() {
        viewModelScope.launch {
            _uiState.value = FeedUiState.Loading

            try {
                val snapshot = db.collection(Constants.COLLECTION_POSTS)
                    .orderBy(Constants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                allPosts = posts

                val currentUserId = auth.currentUser?.uid
                val isAnonymous = auth.currentUser?.isAnonymous == true

                // Use denormalized fields
                val postLikes = posts.associate { it.id to (it.likeCount ?: 0) }
                val commentCounts = posts.associate { it.id to (it.commentCount ?: 0) }

                // Track which posts the current user liked
                val userLiked = if (isAnonymous || currentUserId == null) {
                    emptySet()
                } else {
                    posts.filter { it.likes.contains(currentUserId) }
                        .map { it.id }
                        .toSet()
                }

                _uiState.value = FeedUiState.Success(
                    posts = posts,
                    filteredPosts = posts,
                    postLikes = postLikes,
                    userLikes = userLiked,
                    commentCount = commentCounts,
                    currentTag = null,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = FeedUiState.Error("Failed to fetch posts: ${e.message}")
            }
        }
    }

    /**
     * Refresh post data (used for pull-to-refresh).
     */
    fun refreshPosts() {
        val currentTag = (_uiState.value as? FeedUiState.Success)?.currentTag

        viewModelScope.launch {
            try {
                val snapshot = db.collection(Constants.COLLECTION_POSTS)
                    .orderBy(Constants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                val refreshedPosts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                allPosts = refreshedPosts

                val filtered = if (currentTag != null) {
                    refreshedPosts.filter { post ->
                        post.tags.any { it.equals(currentTag, ignoreCase = true) }
                    }
                } else {
                    refreshedPosts
                }

                val currentUserId = auth.currentUser?.uid
                val isAnonymous = auth.currentUser?.isAnonymous == true

                // Use denormalized fields
                val postLikes = refreshedPosts.associate { it.id to (it.likeCount ?: 0) }
                val commentCounts = refreshedPosts.associate { it.id to (it.commentCount ?: 0) }

                // Track which posts the current user liked
                val userLiked = if (isAnonymous || currentUserId == null) {
                    emptySet()
                } else {
                    refreshedPosts.filter { it.likes.contains(currentUserId) }
                        .map { it.id }
                        .toSet()
                }

                _uiState.value = FeedUiState.Success(
                    posts = refreshedPosts,
                    filteredPosts = filtered,
                    postLikes = postLikes,
                    userLikes = userLiked,
                    commentCount = commentCounts,
                    currentTag = currentTag,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = FeedUiState.Error("Failed to refresh posts: ${e.message}")
            }
        }
    }

    /**
     * Filter posts by tag.
     */
    fun setTagFilter(tag: String) {
        val currentState = _uiState.value
        if (currentState is FeedUiState.Success) {
            val filtered = allPosts.filter { post ->
                post.tags.any { it.equals(tag, ignoreCase = true) }
            }
            _uiState.value = currentState.copy(
                filteredPosts = filtered,
                currentTag = tag
            )
        }
    }

    /**
     * Clear tag filter and show all posts.
     */
    fun clearTagFilter() {
        val currentState = _uiState.value
        if (currentState is FeedUiState.Success) {
            _uiState.value = currentState.copy(
                filteredPosts = allPosts,
                currentTag = null
            )
        }
    }

    /**
     * Toggle like/unlike on a post.
     */
    fun toggleLike(postId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentState = _uiState.value

        if (currentState !is FeedUiState.Success) return

        val isLiked = currentState.userLikes.contains(postId)
        val docRef = db.collection(Constants.COLLECTION_POSTS).document(postId)

        viewModelScope.launch {
            try {
                if (isLiked) {
                    docRef.update(Constants.FIELD_LIKES, FieldValue.arrayRemove(currentUserId)).await()
                    docRef.update("likeCount", FieldValue.increment(-1)).await()
                } else {
                    docRef.update(Constants.FIELD_LIKES, FieldValue.arrayUnion(currentUserId)).await()
                    docRef.update("likeCount", FieldValue.increment(1)).await()
                }

                // Update UI optimistically
                val updatedLikes = currentState.postLikes.toMutableMap()
                updatedLikes[postId] = (updatedLikes[postId] ?: 0) + if (isLiked) -1 else 1

                val updatedUserLikes = currentState.userLikes.toMutableSet().apply {
                    if (isLiked) remove(postId) else add(postId)
                }

                _uiState.value = currentState.copy(
                    postLikes = updatedLikes,
                    userLikes = updatedUserLikes
                )
            } catch (e: Exception) {
                // Optional: handle like failure
            }
        }
    }
}
