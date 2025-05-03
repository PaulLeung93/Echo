package com.example.echo.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.models.Comment
import com.example.echo.models.Post
import com.example.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- UI State ---
    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _likeCount = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCount

    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount

    private val _isLikedByUser = MutableStateFlow(false)
    val isLikedByUser: StateFlow<Boolean> = _isLikedByUser

    /**
     * Loads a post and its comments given a post ID.
     * Also fetches like and comment counts and whether the current user liked the post.
     */
    fun loadPostAndComments(postId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val postDoc = db.collection(Constants.COLLECTION_POSTS)
                    .document(postId)
                    .get()
                    .await()

                if (postDoc.exists()) {
                    _post.value = postDoc.toObject(Post::class.java)?.copy(id = postDoc.id)
                } else {
                    _errorMessage.value = "Post not found."
                    return@launch
                }

                val commentsSnapshot = db.collection(Constants.COLLECTION_POSTS)
                    .document(postId)
                    .collection(Constants.COLLECTION_COMMENTS)
                    .orderBy(Constants.FIELD_TIMESTAMP)
                    .get()
                    .await()

                val fetchedComments = commentsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.copy(id = doc.id)
                }

                _comments.value = fetchedComments
                _commentCount.value = fetchedComments.size
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load post or comments: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     * Toggles like for the current user on this post.
     * Updates Firestore and local UI state.
     */
    fun toggleLike(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = db.collection(Constants.COLLECTION_POSTS).document(postId)

        viewModelScope.launch {
            val currentlyLiked = _isLikedByUser.value
            try {
                if (currentlyLiked) {
                    docRef.update(Constants.FIELD_LIKES, FieldValue.arrayRemove(userId)).await()
                    _likeCount.value = (_likeCount.value - 1).coerceAtLeast(0)
                    _isLikedByUser.value = false
                } else {
                    docRef.update(Constants.FIELD_LIKES, FieldValue.arrayUnion(userId)).await()
                    _likeCount.value += 1
                    _isLikedByUser.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update like: ${e.message}"
            }
        }
    }


    /**
     * Adds a comment to the post and updates the comment list.
     */
    fun addComment(postId: String, text: String, onSuccess: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null || currentUser.isAnonymous) {
            _errorMessage.value = "You must be signed in to perform this action."
            return
        }

        val comment = hashMapOf(
            "username" to (currentUser.email ?: "anonymous"),
            "message" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .collection(Constants.COLLECTION_COMMENTS)
            .add(comment)
            .addOnSuccessListener { documentReference ->
                val addedComment = Comment(
                    id = documentReference.id,
                    username = currentUser.email ?: "anonymous",
                    message = text,
                    timestamp = System.currentTimeMillis()
                )

                viewModelScope.launch {
                    val updatedList = _comments.value + addedComment
                    _comments.emit(updatedList) // force recomposition
                    _commentCount.value = updatedList.size
                    onSuccess()
                }

            }
            .addOnFailureListener {
                _errorMessage.value = "Failed to add comment: ${it.message}"
            }
    }

}
