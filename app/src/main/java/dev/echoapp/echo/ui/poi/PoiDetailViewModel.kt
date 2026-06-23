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
import dev.echoapp.echo.data.preferences.UserPreferencesRepository
import dev.echoapp.echo.domain.repository.AuthRepository
import dev.echoapp.echo.domain.repository.LocationProvider
import dev.echoapp.echo.domain.repository.PoiRepository
import dev.echoapp.echo.domain.repository.UserRepository
import dev.echoapp.echo.utils.Constants
import dev.echoapp.echo.domain.usecase.post.DeletePostUseCase
import dev.echoapp.echo.domain.usecase.post.GetPoiPostsUseCase
import dev.echoapp.echo.domain.usecase.post.ToggleLikeUseCase
import dev.echoapp.echo.domain.usecase.post.UpdatePostUseCase
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

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PoiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val poiRepository: PoiRepository,
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferencesRepository,
    private val locationProvider: LocationProvider,
    private val getPoiPostsUseCase: GetPoiPostsUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
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
        observeFavorite()
        // Opening the thread counts as "viewing" this POI: stamp the time so the map's
        // activity glow clears for it (and only re-lights if a newer echo arrives).
        viewModelScope.launch { userPreferences.markPoiViewed(poiId) }
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

    /** Keep favorite state (this POI + total slots used) in sync with the user's profile. */
    private fun observeFavorite() {
        viewModelScope.launch {
            userRepository.observeFavoritePois().collect { favorites ->
                _uiState.update {
                    it.copy(
                        isFavorited = poiId in favorites,
                        favoritedAt = favorites[poiId],
                        favoriteCount = favorites.size
                    )
                }
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

    /** Delete one of the current user's own posts in this thread. */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId)
                .onSuccess {
                    // The Cloud Function decrements the POI document's postCount on
                    // delete; mirror that in the map's cached copy so its count drops
                    // right away rather than lingering until the next cache TTL sync.
                    poiRepository.adjustCachedPostCount(poiId, -1)
                }
                .onFailure { e ->
                    _uiEvent.send(e.message ?: "Couldn't delete the post. Please try again.")
                }
        }
    }

    /** Edit the message of one of the current user's own posts in this thread. */
    fun updatePost(postId: String, newMessage: String) {
        viewModelScope.launch {
            updatePostUseCase(postId, newMessage).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't update the post. Please try again.")
            }
        }
    }

    /**
     * Toggle this POI as a favorite. Favoriting requires being in range and a free slot;
     * unfavoriting is blocked until the slot's hold has elapsed. The rules enforce the
     * same constraints — these checks just give immediate, specific feedback.
     */
    fun toggleFavorite() {
        val state = _uiState.value
        if (state.isGuest) {
            viewModelScope.launch { _uiEvent.send("Sign in to favorite places") }
            return
        }

        if (state.isFavorited) {
            val remaining = state.holdRemainingMillis(System.currentTimeMillis())
            if (remaining > 0) {
                val days = ((remaining + DAY_MILLIS - 1) / DAY_MILLIS).toInt()
                viewModelScope.launch {
                    _uiEvent.send("You can remove this place in $days day${if (days == 1) "" else "s"}.")
                }
                return
            }
            viewModelScope.launch {
                userRepository.unfavoritePoi(poiId)
                    .onSuccess { _uiEvent.send("Removed from your places.") }
                    .onFailure { _uiEvent.send("Couldn't remove this place. Please try again.") }
            }
            return
        }

        // Adding a new favorite.
        if (state.atFavoriteCap) {
            viewModelScope.launch {
                _uiEvent.send("You've used all ${Constants.MAX_FAVORITE_POIS} of your places — remove one first.")
            }
            return
        }
        if (!state.withinRange) {
            val km = (Constants.PROXIMITY_RADIUS_METERS / 1000).toInt()
            viewModelScope.launch { _uiEvent.send("Get within $km km of this place to favorite it.") }
            return
        }
        viewModelScope.launch {
            userRepository.favoritePoi(poiId)
                .onSuccess { _uiEvent.send("Added to your places — you can post here anytime.") }
                .onFailure { _uiEvent.send("Couldn't favorite this place. Please try again.") }
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
