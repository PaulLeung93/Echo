package com.example.echo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.models.Post
import com.example.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel responsible for loading and managing the current user's posts for the Profile screen.
 * It supports fetching, deleting, and updating posts, and tracking total likes and comments.
 */
class ProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // List of posts made by the currently signed-in user
    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts

    // Flag to track whether post data is still loading
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Maps of individual like and comment counts by post ID
    private val _postLikes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val postLikes: StateFlow<Map<String, Int>> = _postLikes

    private val _postCommentCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val postCommentCounts: StateFlow<Map<String, Int>> = _postCommentCounts

    // Aggregated total like count across all user posts
    private val _totalLikes = MutableStateFlow(0)
    val totalLikes: StateFlow<Int> = _totalLikes

    // Aggregated total comment count across all user posts
    private val _totalComments = MutableStateFlow(0)
    val totalComments: StateFlow<Int> = _totalComments

    init {
        // Initial fetch of posts once the ViewModel is created
        fetchUserPosts()
    }

    /**
     * Retrieves posts made by the signed-in user, ordered from newest to oldest.
     * Also computes total likes and comments.
     */
    private fun fetchUserPosts() {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection(Constants.COLLECTION_POSTS)
                    .whereEqualTo("userId", currentUserId)
                    .orderBy(Constants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }
                _userPosts.value = posts

                // Extract like/comment counts into maps and compute totals
                val likesMap = mutableMapOf<String, Int>()
                val commentsMap = mutableMapOf<String, Int>()
                var likeSum = 0
                var commentSum = 0

                for (doc in snapshot.documents) {
                    val id = doc.id
                    val likes = (doc.get(Constants.FIELD_LIKES) as? List<*>)?.size ?: 0
                    val commentCount = doc.getLong(Constants.FIELD_COMMENT_COUNT)?.toInt() ?: 0
                    likesMap[id] = likes
                    commentsMap[id] = commentCount
                    likeSum += likes
                    commentSum += commentCount
                }

                _postLikes.value = likesMap
                _postCommentCounts.value = commentsMap
                _totalLikes.value = likeSum
                _totalComments.value = commentSum

            } catch (e: Exception) {
                _userPosts.value = emptyList() // Fallback on failure
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a post from Firestore and updates local state and aggregate counts.
     * @param postId ID of the post to delete
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                db.collection(Constants.COLLECTION_POSTS)
                    .document(postId)
                    .delete()
                    .await()

                val updatedPosts = _userPosts.value.filterNot { it.id == postId }
                _userPosts.value = updatedPosts

                // Remove from per-post counts
                _postLikes.value = _postLikes.value - postId
                _postCommentCounts.value = _postCommentCounts.value - postId

                // Recalculate aggregate totals
                _totalLikes.value = _postLikes.value.values.sum()
                _totalComments.value = _postCommentCounts.value.values.sum()

            } catch (e: Exception) {
                // Optional: emit error message to snackbar
            }
        }
    }

    /**
     * Updates the message text of a specific post.
     * @param postId ID of the post to update
     * @param newMessage New message body to save
     */
    fun updatePost(postId: String, newMessage: String) {
        viewModelScope.launch {
            try {
                db.collection(Constants.COLLECTION_POSTS)
                    .document(postId)
                    .update("message", newMessage)
                    .await()

                _userPosts.value = _userPosts.value.map {
                    if (it.id == postId) it.copy(message = newMessage) else it
                }
            } catch (e: Exception) {
                // Optional: emit error message to snackbar
            }
        }
    }

    /**
     * Returns the number of likes for a specific post ID.
     */
    fun getLikeCountForPost(postId: String?): Int = postLikes.value[postId] ?: 0

    /**
     * Returns the number of comments for a specific post ID.
     */
    fun getCommentCountForPost(postId: String?): Int = postCommentCounts.value[postId] ?: 0
}
