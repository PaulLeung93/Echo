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

    // ---UI state---
    private val _posts = MutableStateFlow<List<Post>>(emptyList())              // All posts
    private val _filteredPosts = MutableStateFlow<List<Post>>(emptyList())      // Filtered posts by tag
    val filteredPosts: StateFlow<List<Post>> = _filteredPosts

    private val _isRefreshing = MutableStateFlow(false)                         // Swipe-to-refresh state
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _postLikes = MutableStateFlow<Map<String, Int>>(emptyMap())     // Post ID → like count
    val postLikes: StateFlow<Map<String, Int>> = _postLikes

    private val _userLikes = MutableStateFlow<Set<String>>(emptySet())          // Posts liked by current user
    val userLikes: StateFlow<Set<String>> = _userLikes

    private val _commentLikes = MutableStateFlow<Map<String, Int>>(emptyMap())  // Post ID → comment count
    val commentLikes: StateFlow<Map<String, Int>> = _commentLikes

    init {
        fetchPosts()
    }

    /**
     * Initial post fetch on ViewModel init.
     */
    fun fetchPosts() {
        db.collection(Constants.COLLECTION_POSTS)
            .orderBy(Constants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedPosts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                _posts.value = fetchedPosts
                _filteredPosts.value = fetchedPosts // show all by default
                fetchLikesAndComments(fetchedPosts.map { it.id })
            }
    }

    /**
     * Handles swipe-to-refresh behavior.
     */
    fun refreshPosts() {
        _isRefreshing.value = true
        db.collection(Constants.COLLECTION_POSTS)
            .orderBy(Constants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val refreshedPosts = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
                _posts.value = refreshedPosts
                _filteredPosts.value = refreshedPosts
                _isRefreshing.value = false
            }
            .addOnFailureListener {
                _isRefreshing.value = false
            }
    }

    /**
     * Fetch like and comment counts for all post IDs.
     */
    fun fetchLikesAndComments(postIds: List<String>) {
        val currentUserId = auth.currentUser?.uid ?: return
        val updatedLikes = mutableMapOf<String, Int>()
        val likedPosts = mutableSetOf<String>()
        val updatedCommentCounts = mutableMapOf<String, Int>()

        postIds.forEach { postId ->
            val postRef = db.collection(Constants.COLLECTION_POSTS).document(postId)

            // Likes
            postRef.get().addOnSuccessListener { snapshot ->
                val likes = snapshot.get(Constants.FIELD_LIKES) as? List<*>
                updatedLikes[postId] = likes?.size ?: 0
                if (likes?.contains(currentUserId) == true) {
                    likedPosts.add(postId)
                }
                _postLikes.value = updatedLikes
                _userLikes.value = likedPosts
            }

            // Comments
            postRef.collection(Constants.COLLECTION_COMMENTS).get()
                .addOnSuccessListener { commentSnapshot ->
                    updatedCommentCounts[postId] = commentSnapshot.size()
                    _commentLikes.value = updatedCommentCounts
                }
        }
    }

    /**
     * Likes or unlikes a post based on current user state.
     */
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

    /**
     * Applies a tag filter to the post list.
     */
    fun setTagFilter(tag: String) {
        val filtered = _posts.value.filter { post ->
            post.tags.any { it.equals(tag, ignoreCase = true) }
        }
        _filteredPosts.value = filtered
    }

    /**
     * Clears the tag filter and shows all posts again.
     */
    fun clearTagFilter() {
        _filteredPosts.value = _posts.value
    }
}
