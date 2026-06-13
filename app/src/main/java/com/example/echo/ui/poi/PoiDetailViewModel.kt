package com.example.echo.ui.poi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.model.Coordinates
import com.example.echo.domain.model.Poi
import com.example.echo.domain.repository.LocationProvider
import com.example.echo.domain.repository.PoiRepository
import com.example.echo.domain.usecase.comment.AddPoiCommentUseCase
import com.example.echo.domain.usecase.comment.DeletePoiCommentUseCase
import com.example.echo.domain.usecase.comment.GetPoiCommentsUseCase
import com.example.echo.utils.distanceMeters
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PoiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val poiRepository: PoiRepository,
    private val auth: FirebaseAuth,
    private val locationProvider: LocationProvider,
    private val getPoiCommentsUseCase: GetPoiCommentsUseCase,
    private val addPoiCommentUseCase: AddPoiCommentUseCase,
    private val deletePoiCommentUseCase: DeletePoiCommentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PoiDetailUiState(
            currentUserId = auth.currentUser?.uid,
            currentUserEmail = auth.currentUser?.email,
            isGuest = auth.currentUser?.isAnonymous != false
        )
    )
    val uiState: StateFlow<PoiDetailUiState> = _uiState.asStateFlow()

    /** One-shot, transient messages (e.g. failed comment actions) shown as snackbars. */
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val poiId: String = checkNotNull(savedStateHandle["poiId"])

    private var userCoordinates: Coordinates? = null

    init {
        loadPoi()
        loadComments()
        loadUserLocation()
    }

    private fun loadPoi() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            poiRepository.getPoiByIdFlow(poiId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { poi ->
                    if (poi != null) {
                        _uiState.update {
                            it.copy(
                                poi = poi,
                                isLoading = false,
                                distanceMeters = distanceTo(poi)
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "POI not found") }
                    }
                }
        }
    }

    private fun loadComments() {
        viewModelScope.launch {
            getPoiCommentsUseCase(poiId)
                .catch { /* comment stream errors are non-fatal; keep showing the POI */ }
                .collect { comments ->
                    _uiState.update { it.copy(comments = comments) }
                }
        }
    }

    private fun loadUserLocation() {
        viewModelScope.launch {
            userCoordinates = locationProvider.getCurrentCoordinates()
            _uiState.update {
                it.copy(locationChecked = true, distanceMeters = distanceTo(it.poi))
            }
        }
    }

    private fun distanceTo(poi: Poi?): Double? {
        val coords = userCoordinates ?: return null
        if (poi == null) return null
        return distanceMeters(coords, Coordinates(poi.latitude, poi.longitude))
    }

    fun addComment(message: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            addPoiCommentUseCase(poiId, message)
                .onSuccess { onSuccess() }
                .onFailure { e -> _uiEvent.send(e.message ?: "Couldn't post your comment. Please try again.") }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                deletePoiCommentUseCase(poiId, commentId)
            } catch (e: Exception) {
                _uiEvent.send(e.message ?: "Failed to delete comment")
            }
        }
    }
}
