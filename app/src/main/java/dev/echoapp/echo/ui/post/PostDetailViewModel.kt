package dev.echoapp.echo.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.model.Comment
import dev.echoapp.echo.domain.model.Report
import dev.echoapp.echo.domain.model.ReportReason
import dev.echoapp.echo.domain.model.ReportType
import dev.echoapp.echo.domain.repository.AuthRepository
import dev.echoapp.echo.domain.usecase.comment.AddCommentUseCase
import dev.echoapp.echo.domain.usecase.comment.DeleteCommentUseCase
import dev.echoapp.echo.domain.usecase.comment.GetCommentsUseCase
import dev.echoapp.echo.domain.usecase.post.GetPostFlowUseCase
import dev.echoapp.echo.domain.usecase.post.ToggleLikeUseCase
import dev.echoapp.echo.domain.usecase.report.SubmitReportUseCase
import dev.echoapp.echo.domain.usecase.user.BlockUserUseCase
import dev.echoapp.echo.domain.usecase.user.ObserveHiddenAuthorIdsUseCase
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
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val submitReportUseCase: SubmitReportUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    observeHiddenAuthorIdsUseCase: ObserveHiddenAuthorIdsUseCase,
    authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])
    private val currentUser = authRepository.getCurrentUser()
    private val currentUserId: String? = currentUser?.id

    /** Guests (anonymous) and signed-out users can't report/block (rules reject it). */
    val isGuest: Boolean = currentUser?.isAnonymous != false

    private val blockedIds = observeHiddenAuthorIdsUseCase()

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
                getCommentsUseCase(postId),
                blockedIds
            ) { post, comments, blocked ->
                PostDetailUiState(
                    post = post,
                    comments = comments.filterNot { it.authorId in blocked },
                    isLoading = false,
                    currentUserId = currentUserId
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

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            deleteCommentUseCase(postId, commentId).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't delete the comment. Please try again.")
            }
        }
    }

    fun reportPost(reason: ReportReason) {
        val post = _uiState.value.post ?: return
        submitReport(Report(ReportType.POST, post.id, post.authorId, reason = reason))
    }

    fun reportComment(comment: Comment, reason: ReportReason) {
        submitReport(
            Report(ReportType.COMMENT, comment.id, comment.authorId, contextId = postId, reason = reason)
        )
    }

    private fun submitReport(report: Report) {
        viewModelScope.launch {
            submitReportUseCase(report)
                .onSuccess { _uiEvent.send("Thanks — your report was submitted.") }
                .onFailure { _uiEvent.send("Couldn't submit your report. Please try again.") }
        }
    }

    /** Block any user by uid (the post's author or a commenter). */
    fun blockUser(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            blockUserUseCase(userId)
                .onSuccess { _uiEvent.send("User blocked.") }
                .onFailure { _uiEvent.send("Couldn't block this user. Please try again.") }
        }
    }
}
