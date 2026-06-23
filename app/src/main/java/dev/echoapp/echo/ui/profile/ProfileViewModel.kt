package dev.echoapp.echo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.usecase.auth.GetCurrentUserUseCase
import dev.echoapp.echo.domain.usecase.post.DeletePostUseCase
import dev.echoapp.echo.domain.usecase.post.GetPostsByAuthorIdUseCase
import dev.echoapp.echo.domain.usecase.post.ToggleLikeUseCase
import dev.echoapp.echo.domain.usecase.post.UpdatePostUseCase
import dev.echoapp.echo.domain.usecase.user.ObserveCurrentUserProfileUseCase
import dev.echoapp.echo.domain.repository.PoiRepository
import dev.echoapp.echo.domain.repository.UserRepository
import dev.echoapp.echo.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getPostsByAuthorIdUseCase: GetPostsByAuthorIdUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val userRepository: UserRepository,
    private val poiRepository: PoiRepository
) : ViewModel() {

    private val user = getCurrentUserUseCase()

    /**
     * The user's favorited POIs resolved to full [Poi]s, most-recent first. POIs come
     * from the local cache ([PoiRepository.getPois]), so this costs no extra reads; a
     * favorite whose POI isn't in the cache is simply skipped.
     */
    private val favoritePlaces: Flow<List<FavoritePlace>> =
        combine(userRepository.observeFavoritePois(), poiRepository.getPois()) { favorites, pois ->
            val byId = pois.associateBy { it.id }
            favorites.entries
                .mapNotNull { (poiId, at) -> byId[poiId]?.let { FavoritePlace(it, at) } }
                .sortedByDescending { it.favoritedAt }
        }

    val uiState: StateFlow<ProfileUiState> = if (user != null) {
        combine(
            getPostsByAuthorIdUseCase(user.id),
            observeCurrentUserProfileUseCase(),
            favoritePlaces
        ) { posts, profile, places ->
            ProfileUiState(
                userPosts = posts,
                totalLikes = posts.sumOf { it.likeCount },
                totalComments = posts.sumOf { it.commentCount },
                userProfile = profile,
                favoritePlaces = places,
                isLoading = false
            )
        }.catch { e ->
            emit(ProfileUiState(error = e.message))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState(isLoading = true)
        )
    } else {
        MutableStateFlow(ProfileUiState(error = "User not logged in")).asStateFlow()
    }

    /** One-shot messages (e.g. a failed delete/edit) shown as snackbars. */
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun deletePost(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't delete the post. Please try again.")
            }
        }
    }

    fun updatePost(postId: String, newMessage: String) {
        viewModelScope.launch {
            updatePostUseCase(postId, newMessage).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't update the post. Please try again.")
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            toggleLikeUseCase(postId).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't update your like. Please try again.")
            }
        }
    }

    /**
     * Remove a favorited place. Blocked until the slot's 7-day hold has elapsed (the
     * rules enforce the same); the pre-check just gives immediate, specific feedback.
     */
    fun removeFavorite(poiId: String, favoritedAt: Long) {
        val remaining = favoritedAt + Constants.FAVORITE_HOLD_MILLIS - System.currentTimeMillis()
        if (remaining > 0) {
            val dayMillis = 24L * 60 * 60 * 1000
            val days = ((remaining + dayMillis - 1) / dayMillis).toInt()
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
    }
}
