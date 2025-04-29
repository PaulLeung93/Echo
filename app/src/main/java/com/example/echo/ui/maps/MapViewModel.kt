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
    val posts: StateFlow<List<Post>> = _posts

    init {
        fetchPostsWithLocation()
    }

    private fun fetchPostsWithLocation() {
        viewModelScope.launch {
            db.collection(Constants.COLLECTION_POSTS)
                .get()
                .addOnSuccessListener { snapshot ->
                    val fetchedPosts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)
                    }.filter { post ->
                        post.latitude != null && post.longitude != null
                    }
                    _posts.value = fetchedPosts
                }
                .addOnFailureListener {
                }
        }
    }
}
