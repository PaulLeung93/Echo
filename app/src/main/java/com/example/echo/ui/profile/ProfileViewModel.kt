package com.example.echo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.usecase.auth.GetCurrentUserUseCase
import com.example.echo.domain.usecase.post.DeletePostUseCase
import com.example.echo.domain.usecase.post.GetPostsByAuthorIdUseCase
import com.example.echo.domain.usecase.post.ToggleLikeUseCase
import com.example.echo.domain.usecase.post.UpdatePostUseCase
import com.example.echo.domain.usecase.user.ObserveCurrentUserProfileUseCase
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
    private val toggleLikeUseCase: ToggleLikeUseCase
) : ViewModel() {

    private val user = getCurrentUserUseCase()

    val uiState: StateFlow<ProfileUiState> = if (user != null) {
        combine(
            getPostsByAuthorIdUseCase(user.id),
            observeCurrentUserProfileUseCase()
        ) { posts, profile ->
            ProfileUiState(
                userPosts = posts,
                totalLikes = posts.sumOf { it.likeCount },
                totalComments = posts.sumOf { it.commentCount },
                userProfile = profile,
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
}
