package com.example.echo.ui.poi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.model.Comment
import com.example.echo.domain.model.Coordinates
import com.example.echo.domain.model.Poi
import com.example.echo.domain.model.Report
import com.example.echo.domain.model.ReportReason
import com.example.echo.domain.model.ReportType
import com.example.echo.domain.repository.AuthRepository
import com.example.echo.domain.repository.LocationProvider
import com.example.echo.domain.repository.PoiRepository
import com.example.echo.domain.usecase.comment.AddPoiCommentUseCase
import com.example.echo.domain.usecase.comment.DeletePoiCommentUseCase
import com.example.echo.domain.usecase.comment.GetPoiCommentsUseCase
import com.example.echo.domain.usecase.report.SubmitReportUseCase
import com.example.echo.domain.usecase.user.BlockUserUseCase
import com.example.echo.domain.usecase.user.ObserveHiddenAuthorIdsUseCase
import com.example.echo.utils.distanceMeters
import kotlinx.coroutines.flow.combine
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
    authRepository: AuthRepository,
    private val locationProvider: LocationProvider,
    private val getPoiCommentsUseCase: GetPoiCommentsUseCase,
    private val addPoiCommentUseCase: AddPoiCommentUseCase,
    private val deletePoiCommentUseCase: DeletePoiCommentUseCase,
    private val submitReportUseCase: SubmitReportUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    observeHiddenAuthorIdsUseCase: ObserveHiddenAuthorIdsUseCase
) : ViewModel() {

    private val blockedIds = observeHiddenAuthorIdsUseCase()

    private val _uiState = MutableStateFlow(
        authRepository.getCurrentUser().let { user ->
            PoiDetailUiState(
                currentUserId = user?.id,
                currentUserEmail = user?.email,
                isGuest = user?.isAnonymous != false
            )
        }
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
            combine(
                getPoiCommentsUseCase(poiId),
                blockedIds
            ) { comments, blocked -> comments.filterNot { it.authorId in blocked } }
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
            deletePoiCommentUseCase(poiId, commentId).onFailure { e ->
                _uiEvent.send(e.message ?: "Failed to delete comment")
            }
        }
    }

    fun reportComment(comment: Comment, reason: ReportReason) {
        viewModelScope.launch {
            submitReportUseCase(
                Report(ReportType.COMMENT, comment.id, comment.authorId, contextId = poiId, reason = reason)
            ).onSuccess { _uiEvent.send("Thanks — your report was submitted.") }
                .onFailure { _uiEvent.send("Couldn't submit your report. Please try again.") }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            blockUserUseCase(userId)
                .onSuccess { _uiEvent.send("User blocked.") }
                .onFailure { _uiEvent.send("Couldn't block this user. Please try again.") }
        }
    }
}
