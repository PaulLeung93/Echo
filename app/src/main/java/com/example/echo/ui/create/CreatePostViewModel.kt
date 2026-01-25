package com.example.echo.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.usecase.post.CreatePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createPostUseCase: CreatePostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun submitPost(
        message: String,
        includeLocation: Boolean,
        userLatitude: Double? = null,
        userLongitude: Double? = null,
        tags: List<String> = emptyList()
    ) {
        val trimmedMessage = message.trim()

        if (trimmedMessage.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a message.") }
            return
        }

        if (tags.any { it.length > 15 }) {
            _uiState.update { it.copy(error = "Tags cannot be longer than 15 characters.") }
            return
        }

        if (tags.size > 3) {
            _uiState.update { it.copy(error = "You can only add up to 3 tags.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val lat = if (includeLocation) userLatitude else null
            val lng = if (includeLocation) userLongitude else null
            
            createPostUseCase(
                message = trimmedMessage,
                includeLocation = includeLocation,
                latitude = userLatitude,
                longitude = userLongitude,
                tags = tags
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to post") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
