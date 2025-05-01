package com.example.echo.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage


    fun submitPost(
        message: String,
        includeLocation: Boolean,
        userLatitude: Double? = null,
        userLongitude: Double? = null,
        tags: List<String> = emptyList(),
        onSuccess: () -> Unit
    ) {

        val trimmedMessage = message.trim()

        if (trimmedMessage.isBlank()) {
            _errorMessage.value = "Please enter a message."
            return
        }

        if (tags.any { it.length > 15 }) {
            _errorMessage.value = "Tags cannot be longer than 15 characters."
            return
        }

        if (tags.size > 3) {
            _errorMessage.value = "You can only add up to 3 tags."
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.isAnonymous) {
            _errorMessage.value = "You must be signed in to perform this action."
            return
        }

        _isLoading.value = true

        val postsCollection = db.collection(Constants.COLLECTION_POSTS)
        val newPostRef = postsCollection.document()

        val post = hashMapOf(
            Constants.FIELD_USERNAME to (currentUser.email ?: "anonymous"),
            Constants.FIELD_MESSAGE to trimmedMessage,
            Constants.FIELD_TIMESTAMP to System.currentTimeMillis(),
            "id" to newPostRef.id,
            Constants.FIELD_TAGS to tags.map { it.trim().lowercase() }
        ).toMutableMap()

        // If the user wants to include location
        if (includeLocation && userLatitude != null && userLongitude != null) {
            post["latitude"] = userLatitude
            post["longitude"] = userLongitude
        }

        newPostRef.set(post)
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = "Failed to post: ${e.message}"
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
