package com.example.echo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.usecase.auth.GetCurrentUserUseCase
import com.example.echo.domain.usecase.post.DeletePostUseCase
import com.example.echo.domain.usecase.post.GetPostsByUsernameUseCase
import com.example.echo.domain.usecase.post.ToggleLikeUseCase
import com.example.echo.domain.usecase.post.UpdatePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getPostsByUsernameUseCase: GetPostsByUsernameUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase
) : ViewModel() {

    private val user = getCurrentUserUseCase()
    
    val uiState: StateFlow<ProfileUiState> = if (user?.email != null) {
        getPostsByUsernameUseCase(user.email).map { posts ->
            ProfileUiState(
                userPosts = posts,
                totalLikes = posts.sumOf { it.likeCount },
                totalComments = posts.sumOf { it.commentCount },
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

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                deletePostUseCase(postId)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun updatePost(postId: String, newMessage: String) {
        viewModelScope.launch {
            try {
                updatePostUseCase(postId, newMessage)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                toggleLikeUseCase(postId)
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
