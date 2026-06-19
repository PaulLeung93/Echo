package com.example.echo.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.model.Coordinates
import com.example.echo.domain.model.Post
import com.example.echo.domain.model.Report
import com.example.echo.domain.model.ReportReason
import com.example.echo.domain.model.ReportType
import com.example.echo.domain.repository.AuthRepository
import com.example.echo.domain.repository.LocationProvider
import com.example.echo.domain.usecase.post.GetPostsUseCase
import com.example.echo.domain.usecase.post.GetPostsByTagUseCase
import com.example.echo.domain.usecase.post.ToggleLikeUseCase
import com.example.echo.domain.usecase.report.SubmitReportUseCase
import com.example.echo.domain.usecase.user.BlockUserUseCase
import com.example.echo.domain.usecase.user.ObserveHiddenAuthorIdsUseCase
import com.example.echo.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val getPostsByTagUseCase: GetPostsByTagUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val submitReportUseCase: SubmitReportUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    observeHiddenAuthorIdsUseCase: ObserveHiddenAuthorIdsUseCase,
    private val locationProvider: LocationProvider,
    authRepository: AuthRepository
) : ViewModel() {

    /** Current user's uid, to distinguish own posts (no report/block) from others'. */
    val currentUserId: String? = authRepository.getCurrentUser()?.id

    private val blockedIds: Flow<Set<String>> = observeHiddenAuthorIdsUseCase()

    private val _currentTag = MutableStateFlow<String?>(null)

    /** The user's current location, for showing how far away each post is. Null if unavailable. */
    private val _userCoordinates = MutableStateFlow<Coordinates?>(null)
    val userCoordinates: StateFlow<Coordinates?> = _userCoordinates.asStateFlow()

    private val _neighborhoodName = MutableStateFlow<String?>(null)
    val neighborhoodName: StateFlow<String?> = _neighborhoodName.asStateFlow()

    // --- Paginated (untagged) feed state ---
    // The default feed pages in via one-time reads instead of a live 200-post
    // listener, so a session only bills reads for what's actually scrolled to.
    private val _pagedPosts = MutableStateFlow<List<Post>>(emptyList())
    private val _isLoadingFirstPage = MutableStateFlow(true)
    private val _endReached = MutableStateFlow(false)
    private var oldestTimestamp: Long? = null

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    init {
        viewModelScope.launch {
            val coords = runCatching { locationProvider.getCurrentCoordinates() }.getOrNull()
            _userCoordinates.value = coords
            if (coords != null) {
                _neighborhoodName.value = runCatching { locationProvider.getNeighborhoodName(coords) }.getOrNull()
            }
        }
        viewModelScope.launch { loadPage(reset = true) }
    }

    val uiState: StateFlow<FeedUiState> = _currentTag.flatMapLatest { tag ->
        if (tag != null) {
            // The tag view stays live (rare and short-lived); it reflects likes via
            // its own listener, so optimistic updates aren't needed there.
            combine(
                getPostsByTagUseCase(tag).catch { emit(emptyList()) },
                blockedIds
            ) { posts, blocked ->
                FeedUiState(
                    posts = posts.filterNot { it.authorId in blocked },
                    currentTag = tag,
                    isLoading = false
                )
            }
        } else {
            combine(_pagedPosts, _isLoadingFirstPage, blockedIds) { posts, loadingFirst, blocked ->
                FeedUiState(
                    posts = posts.filterNot { it.authorId in blocked },
                    currentTag = null,
                    isLoading = loadingFirst && posts.isEmpty()
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedUiState(isLoading = true)
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** One-shot messages (e.g. a failed like) shown as snackbars. */
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    /** Load the next page when the user nears the end of the list (untagged feed only). */
    fun loadMore() {
        if (_currentTag.value != null) return
        if (_isLoadingMore.value || _isLoadingFirstPage.value || _endReached.value) return
        viewModelScope.launch { loadPage(reset = false) }
    }

    private suspend fun loadPage(reset: Boolean) {
        if (reset) {
            _isLoadingFirstPage.value = true
            _endReached.value = false
            oldestTimestamp = null
        } else {
            _isLoadingMore.value = true
        }
        try {
            val page = getPostsUseCase.page(
                afterTimestamp = if (reset) null else oldestTimestamp,
                limit = Constants.FEED_PAGE_SIZE
            )
            _endReached.value = page.size < Constants.FEED_PAGE_SIZE
            page.lastOrNull()?.let { oldestTimestamp = it.timestamp }
            _pagedPosts.value = if (reset) page else _pagedPosts.value + page
        } catch (e: Exception) {
            _uiEvent.send(e.message ?: "Couldn't load posts. Please try again.")
        } finally {
            _isLoadingFirstPage.value = false
            _isLoadingMore.value = false
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadPage(reset = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setTagFilter(tag: String) {
        _currentTag.value = tag
    }

    fun clearTagFilter() {
        // Return to the already-loaded paginated feed (no extra reads); pull-to-refresh
        // re-primes it if the user wants the latest.
        _currentTag.value = null
    }

    /** Report another user's post for moderation review. */
    fun reportPost(post: Post, reason: ReportReason) {
        viewModelScope.launch {
            submitReportUseCase(
                Report(
                    type = ReportType.POST,
                    targetId = post.id,
                    targetAuthorId = post.authorId,
                    reason = reason
                )
            ).onSuccess {
                _uiEvent.send("Thanks — your report was submitted.")
            }.onFailure {
                _uiEvent.send("Couldn't submit your report. Please try again.")
            }
        }
    }

    /** Block a post's author; their content disappears from the feed/map. */
    fun blockUser(post: Post) {
        viewModelScope.launch {
            blockUserUseCase(post.authorId)
                .onSuccess { _uiEvent.send("User blocked.") }
                .onFailure { _uiEvent.send("Couldn't block this user. Please try again.") }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            toggleLikeUseCase(postId)
                .onSuccess { nowLiked ->
                    // No live listener backs the paginated feed, so reflect the like
                    // locally instead of re-reading the post.
                    _pagedPosts.update { posts ->
                        posts.map {
                            if (it.id == postId) {
                                it.copy(
                                    likedByCurrentUser = nowLiked,
                                    likeCount = (it.likeCount + if (nowLiked) 1 else -1).coerceAtLeast(0)
                                )
                            } else it
                        }
                    }
                }
                .onFailure { e ->
                    _uiEvent.send(e.message ?: "Couldn't update your like. Please try again.")
                }
        }
    }
}
