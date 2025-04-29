package com.example.echo.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.models.Comment
import com.example.echo.models.Post
import com.example.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadPostAndComments(postId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            // Fetch the post
            db.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        _post.value = document.toObject(Post::class.java)?.copy(id = document.id)
                    } else {
                        _errorMessage.value = "Post not found."
                    }
                }
                .addOnFailureListener { e ->
                    _errorMessage.value = "Failed to load post: ${e.message}"
                }

            // Fetch comments inside subcollection "comments"
            db.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .orderBy(Constants.FIELD_TIMESTAMP)
                .get()
                .addOnSuccessListener { snapshot ->
                    val fetchedComments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)?.copy(id = doc.id)
                    }
                    _comments.value = fetchedComments
                }
                .addOnFailureListener { e ->
                    _errorMessage.value = "Failed to load comments: ${e.message}"
                }
                .addOnCompleteListener {
                    _isLoading.value = false
                }
        }
    }

    fun addComment(postId: String, text: String, onSuccess: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null || currentUser.isAnonymous) {
            _errorMessage.value = "You must be signed in to perform this action."
            return
        }

        val comment = hashMapOf(
            "username" to (currentUser?.email ?: "anonymous"),
            "message" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .collection(Constants.COLLECTION_COMMENTS)
            .add(comment)
            .addOnSuccessListener { documentReference ->
                // Immediately add to the UI
                val addedComment = Comment(
                    id = documentReference.id,
                    username = currentUser.email ?: "anonymous",
                    message = text,
                    timestamp = System.currentTimeMillis()
                )
                _comments.value = _comments.value + addedComment

                onSuccess()
            }
            .addOnFailureListener {
                _errorMessage.value = "Failed to add comment: ${it.message}"
            }
    }


}
