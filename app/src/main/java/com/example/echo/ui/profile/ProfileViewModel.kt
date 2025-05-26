package com.example.echo.ui.profile

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
     * Uses denormalized likeCount and commentCount fields.
     */
    private fun fetchUserPosts() {
        val email = auth.currentUser?.email ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection(Constants.COLLECTION_POSTS)
                    .whereEqualTo("username", email)
                    .get()
                    .await()

                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }
                _userPosts.value = posts

                // Extract denormalized like and comment counts directly from post objects
                val likesMap = posts.associate { it.id to (it.likeCount ?: 0) }
                val commentsMap = posts.associate { it.id to (it.commentCount ?: 0) }

                _postLikes.value = likesMap
                _postCommentCounts.value = commentsMap
                _totalLikes.value = likesMap.values.sum()
                _totalComments.value = commentsMap.values.sum()

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

    /**
     * Toggles the like status for a specific post ID.
     * Updates Firestore and local UI state accordingly.
     */
    fun toggleLike(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection(Constants.COLLECTION_POSTS).document(postId)

        viewModelScope.launch {
            try {
                val isLiked = _postLikes.value[postId]?.let { it > 0 &&
                        _userPosts.value.firstOrNull { it.id == postId }?.likes?.contains(userId) == true
                } ?: false

                if (isLiked) {
                    docRef.update(Constants.FIELD_LIKES, FieldValue.arrayRemove(userId)).await()
                } else {
                    docRef.update(Constants.FIELD_LIKES, FieldValue.arrayUnion(userId)).await()
                }

                // Re-fetch the updated post to ensure we have fresh counts
                val snapshot = docRef.get().await()
                val updatedPost = snapshot.toObject(Post::class.java)?.copy(id = postId) ?: return@launch

                // Update local list of posts
                val updatedPosts = _userPosts.value.map {
                    if (it.id == postId) updatedPost else it
                }
                _userPosts.value = updatedPosts

                // Update count maps and totals
                _postLikes.value = updatedPosts.associate { it.id to (it.likeCount ?: 0) }
                _totalLikes.value = _postLikes.value.values.sum()

            } catch (e: Exception) {
                // Optional: emit error to UI
            }
        }
    }

}
