package dev.echoapp.echo.ui.poi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.model.Coordinates
import dev.echoapp.echo.domain.model.Poi
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.model.Report
import dev.echoapp.echo.domain.model.ReportReason
import dev.echoapp.echo.domain.model.ReportType
import dev.echoapp.echo.domain.repository.AuthRepository
import dev.echoapp.echo.domain.repository.LocationProvider
import dev.echoapp.echo.domain.repository.PoiRepository
import dev.echoapp.echo.domain.usecase.post.GetPoiPostsUseCase
import dev.echoapp.echo.domain.usecase.post.ToggleLikeUseCase
import dev.echoapp.echo.domain.usecase.report.SubmitReportUseCase
import dev.echoapp.echo.domain.usecase.user.BlockUserUseCase
import dev.echoapp.echo.domain.usecase.user.ObserveHiddenAuthorIdsUseCase
import dev.echoapp.echo.utils.distanceMeters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PoiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val poiRepository: PoiRepository,
    authRepository: AuthRepository,
    private val locationProvider: LocationProvider,
    private val getPoiPostsUseCase: GetPoiPostsUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val submitReportUseCase: SubmitReportUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    observeHiddenAuthorIdsUseCase: ObserveHiddenAuthorIdsUseCase
) : ViewModel() {

    private val blockedIds = observeHiddenAuthorIdsUseCase()

    private val _uiState = MutableStateFlow(
        authRepository.getCurrentUser().let { user ->
            PoiDetailUiState(
                currentUserId = user?.id,
                isGuest = user?.isAnonymous != false
            )
        }
    )
    val uiState: StateFlow<PoiDetailUiState> = _uiState.asStateFlow()

    /** One-shot, transient messages (e.g. failed like) shown as snackbars. */
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val poiId: String = checkNotNull(savedStateHandle["poiId"])

    /** Drives the thread query direction; flipped by [toggleSort]. */
    private val sortDescending = MutableStateFlow(true)

    private var userCoordinates: Coordinates? = null

    init {
        loadPoi()
        loadPosts()
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

    private fun loadPosts() {
        viewModelScope.launch {
            sortDescending
                .flatMapLatest { desc -> getPoiPostsUseCase(poiId, desc) }
                .combine(blockedIds) { posts, blocked ->
                    posts.filterNot { it.authorId in blocked }
                }
                .catch { /* thread stream errors are non-fatal; keep showing the POI */ }
                .collect { posts ->
                    _uiState.update { it.copy(posts = posts) }
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

    /** Flip the thread between newest-first and oldest-first. */
    fun toggleSort() {
        val newValue = !sortDescending.value
        sortDescending.value = newValue
        _uiState.update { it.copy(sortDescending = newValue) }
    }

    fun toggleLike(postId: String) {
        if (_uiState.value.isGuest) {
            viewModelScope.launch { _uiEvent.send("Sign in to like posts") }
            return
        }
        viewModelScope.launch {
            toggleLikeUseCase(postId).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't update your like. Please try again.")
            }
        }
    }

    fun reportPost(post: Post, reason: ReportReason) {
        viewModelScope.launch {
            submitReportUseCase(
                Report(ReportType.POST, post.id, post.authorId, reason = reason)
            ).onSuccess { _uiEvent.send("Thanks — your report was submitted.") }
                .onFailure { _uiEvent.send("Couldn't submit your report. Please try again.") }
        }
    }

    fun blockUser(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            blockUserUseCase(userId)
                .onSuccess { _uiEvent.send("User blocked.") }
                .onFailure { _uiEvent.send("Couldn't block this user. Please try again.") }
        }
    }
}
