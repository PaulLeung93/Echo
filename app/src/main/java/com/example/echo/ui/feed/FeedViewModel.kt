package com.example.echo.ui.feed

import androidx.compose.runtime.mutableStateMapOf
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
    val posts: StateFlow<List<Post>> = _posts

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _postLikes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val postLikes: StateFlow<Map<String, Int>> = _postLikes

    private val _userLikes = MutableStateFlow<Set<String>>(emptySet()) // postIds user liked
    val userLikes: StateFlow<Set<String>> = _userLikes



    init {
        fetchPosts()
    }

    fun fetchPosts() {
        db.collection(Constants.COLLECTION_POSTS)
            .orderBy(Constants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedPosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)//?.copy(id = doc.id)
                }
                _posts.value = fetchedPosts
                fetchLikesData(fetchedPosts.map { it.id })
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

    private fun fetchLikesData(postIds: List<String>) {
        val currentUserId = auth.currentUser?.uid ?: return
        val updatedLikes = mutableMapOf<String, Int>()
        val likedPosts = mutableSetOf<String>()

        postIds.forEach { postId ->
            val docRef = db.collection(Constants.COLLECTION_POSTS).document(postId)
            docRef.get().addOnSuccessListener { snapshot ->
                val likes = snapshot.get(Constants.FIELD_LIKES) as? List<*>
                updatedLikes[postId] = likes?.size ?: 0
                if (likes?.contains(currentUserId) == true) {
                    likedPosts.add(postId)
                }
                _postLikes.value = updatedLikes
                _userLikes.value = likedPosts
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

}
