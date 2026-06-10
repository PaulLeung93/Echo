package com.example.echo.feature.map.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.repository.PoiRepository
import com.example.echo.domain.usecase.comment.AddPoiCommentUseCase
import com.example.echo.domain.usecase.comment.DeletePoiCommentUseCase
import com.example.echo.domain.usecase.comment.GetPoiCommentsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PoiDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val poiRepository: PoiRepository,
    private val auth: FirebaseAuth,
    private val getPoiCommentsUseCase: GetPoiCommentsUseCase,
    private val addPoiCommentUseCase: AddPoiCommentUseCase,
    private val deletePoiCommentUseCase: DeletePoiCommentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoiDetailUiState(currentUserEmail = auth.currentUser?.email))
    val uiState: StateFlow<PoiDetailUiState> = _uiState.asStateFlow()

    private val poiId: String = checkNotNull(savedStateHandle["poiId"])

    init {
        loadPoi()
        loadComments()
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
                        _uiState.update { it.copy(poi = poi, isLoading = false) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "POI not found") }
                    }
                }
        }
    }

    private fun loadComments() {
        viewModelScope.launch {
            getPoiCommentsUseCase(poiId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { comments ->
                    _uiState.update { it.copy(comments = comments) }
                }
        }
    }

    fun addComment(message: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                addPoiCommentUseCase(poiId, message)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                deletePoiCommentUseCase(poiId, commentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
