package com.example.echo.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.usecase.comment.AddCommentUseCase
import com.example.echo.domain.usecase.comment.GetCommentsUseCase
import com.example.echo.domain.usecase.post.GetPostFlowUseCase
import com.example.echo.domain.usecase.post.ToggleLikeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val getPostFlowUseCase: GetPostFlowUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow(PostDetailUiState(isLoading = true))
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    /** Transient action errors (failed like/comment) — shown as a snackbar, not
     *  the terminal `error` field which would replace the whole screen. */
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadPostDetail()
    }

    private fun loadPostDetail() {
        viewModelScope.launch {
            combine(
                getPostFlowUseCase(postId),
                getCommentsUseCase(postId)
            ) { post, comments ->
                PostDetailUiState(
                    post = post,
                    comments = comments,
                    isLoading = false
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            toggleLikeUseCase(postId).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't update your like. Please try again.")
            }
        }
    }

    fun addComment(text: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            addCommentUseCase(postId, text)
                .onSuccess { onComplete() }
                .onFailure { e ->
                    _uiEvent.send(e.message ?: "Couldn't post your comment. Please try again.")
                }
        }
    }
}
