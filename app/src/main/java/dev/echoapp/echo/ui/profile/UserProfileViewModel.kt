package dev.echoapp.echo.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.model.Report
import dev.echoapp.echo.domain.model.ReportReason
import dev.echoapp.echo.domain.model.ReportType
import dev.echoapp.echo.domain.usecase.auth.GetCurrentUserUseCase
import dev.echoapp.echo.domain.usecase.post.GetPostsByAuthorIdUseCase
import dev.echoapp.echo.domain.usecase.post.ToggleLikeUseCase
import dev.echoapp.echo.domain.usecase.follow.FollowUserUseCase
import dev.echoapp.echo.domain.usecase.follow.ObserveIsFollowingUseCase
import dev.echoapp.echo.domain.usecase.follow.UnfollowUserUseCase
import dev.echoapp.echo.domain.usecase.report.SubmitReportUseCase
import dev.echoapp.echo.domain.usecase.user.BlockUserUseCase
import dev.echoapp.echo.domain.usecase.user.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the public profile screen for the user at the `uid` nav argument. Combines
 * that user's profile with their posts (reused for the stats). Moderation (report /
 * block) and liking mirror the post-detail flow so the post cards behave the same.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCurrentUserUseCase: GetCurrentUserUseCase,
    observeUserProfileUseCase: ObserveUserProfileUseCase,
    getPostsByAuthorIdUseCase: GetPostsByAuthorIdUseCase,
    observeIsFollowingUseCase: ObserveIsFollowingUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val submitReportUseCase: SubmitReportUseCase,
    private val blockUserUseCase: BlockUserUseCase
) : ViewModel() {

    private val uid: String = savedStateHandle.get<String>("uid").orEmpty()

    private val currentUser = getCurrentUserUseCase()
    val currentUserId: String? = currentUser?.id
    val isGuest: Boolean = currentUser?.isAnonymous != false
    private val isSelf: Boolean = uid.isNotBlank() && uid == currentUserId

    /** A follow/unfollow write is in flight (debounces double-taps). */
    private val followInFlight = MutableStateFlow(false)

    val uiState: StateFlow<UserProfileUiState> = if (uid.isBlank()) {
        MutableStateFlow(UserProfileUiState(error = "This profile is unavailable.")).asStateFlow()
    } else {
        combine(
            observeUserProfileUseCase(uid),
            getPostsByAuthorIdUseCase(uid),
            observeIsFollowingUseCase(uid),
            followInFlight
        ) { profile, posts, isFollowing, inFlight ->
            UserProfileUiState(
                userProfile = profile,
                userPosts = posts,
                totalLikes = posts.sumOf { it.likeCount },
                totalComments = posts.sumOf { it.commentCount },
                isSelf = isSelf,
                isFollowing = isFollowing,
                followerCount = profile?.followerCount ?: 0,
                followingCount = profile?.followingCount ?: 0,
                followInFlight = inFlight,
                isLoading = false
            )
        }.catch { e ->
            emit(UserProfileUiState(error = e.message, isSelf = isSelf))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfileUiState(isLoading = true, isSelf = isSelf)
        )
    }

    /** One-shot messages (failed like/report/block, guest prompts) shown as snackbars. */
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun toggleLike(postId: String) {
        if (isGuest) {
            viewModelScope.launch { _uiEvent.send("Sign in to like posts") }
            return
        }
        viewModelScope.launch {
            toggleLikeUseCase(postId).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't update your like. Please try again.")
            }
        }
    }

    /** Follow this profile, or unfollow if already following. No-op for self/guests. */
    fun toggleFollow() {
        if (isGuest) {
            viewModelScope.launch { _uiEvent.send("Sign in to follow people") }
            return
        }
        if (isSelf || uid.isBlank() || followInFlight.value) return
        val currentlyFollowing = uiState.value.isFollowing
        viewModelScope.launch {
            followInFlight.value = true
            val result = if (currentlyFollowing) {
                unfollowUserUseCase(uid)
            } else {
                followUserUseCase(uid)
            }
            result.onFailure { e ->
                _uiEvent.send(
                    e.message ?: if (currentlyFollowing) {
                        "Couldn't unfollow. Please try again."
                    } else {
                        "Couldn't follow. Please try again."
                    }
                )
            }
            followInFlight.value = false
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

    /** Report the user whose profile this is (targetId and author are their uid). */
    fun reportUser(reason: ReportReason) {
        if (uid.isBlank() || isSelf) return
        viewModelScope.launch {
            submitReportUseCase(
                Report(ReportType.USER, uid, uid, reason = reason)
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
