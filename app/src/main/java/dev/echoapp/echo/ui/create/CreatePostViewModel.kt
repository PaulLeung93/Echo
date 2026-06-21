package dev.echoapp.echo.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.repository.LocationProvider
import dev.echoapp.echo.domain.usecase.post.CreatePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val createPostUseCase: CreatePostUseCase,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    /**
     * Toggle attaching the user's location. When turned on, resolves a fix via
     * [LocationProvider] (high-accuracy, permission-checked); flags it
     * unavailable if no fix can be obtained.
     */
    fun setIncludeLocation(include: Boolean) {
        if (!include) {
            _uiState.update {
                it.copy(
                    includeLocation = false,
                    isLocationLoading = false,
                    locationUnavailable = false,
                    latitude = null,
                    longitude = null
                )
            }
            return
        }
        _uiState.update { it.copy(includeLocation = true, isLocationLoading = true, locationUnavailable = false) }
        viewModelScope.launch {
            val coords = runCatching { locationProvider.getCurrentCoordinates() }.getOrNull()
            _uiState.update {
                it.copy(
                    isLocationLoading = false,
                    latitude = coords?.latitude,
                    longitude = coords?.longitude,
                    locationUnavailable = coords == null
                )
            }
        }
    }

    fun submitPost(message: String, tags: List<String> = emptyList()) {
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

            val state = _uiState.value
            val includeLocation = state.includeLocation
            val lat = if (includeLocation) state.latitude else null
            val lng = if (includeLocation) state.longitude else null

            createPostUseCase(
                message = trimmedMessage,
                includeLocation = includeLocation,
                latitude = lat,
                longitude = lng,
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
