package com.example.echo.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.models.Post
import com.example.echo.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

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
            }
            .addOnFailureListener {
                // Handle error if needed (optional: log it)
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
                _isRefreshing.value = false
            }
            .addOnFailureListener {
                _isRefreshing.value = false
                // Optional: log or show a toast/snackbar
            }
    }

}
