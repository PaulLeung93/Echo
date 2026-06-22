package dev.echoapp.echo.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.echoapp.echo.domain.model.Coordinates
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.model.Report
import dev.echoapp.echo.domain.model.ReportReason
import dev.echoapp.echo.domain.model.ReportType
import dev.echoapp.echo.domain.repository.AuthRepository
import dev.echoapp.echo.domain.repository.LocationProvider
import dev.echoapp.echo.domain.usecase.post.DeletePostUseCase
import dev.echoapp.echo.domain.usecase.post.GetPostsUseCase
import dev.echoapp.echo.domain.usecase.post.GetPostsByTagUseCase
import dev.echoapp.echo.domain.usecase.post.ToggleLikeUseCase
import dev.echoapp.echo.domain.usecase.post.UpdatePostUseCase
import dev.echoapp.echo.domain.usecase.report.SubmitReportUseCase
import dev.echoapp.echo.domain.usecase.user.BlockUserUseCase
import dev.echoapp.echo.domain.usecase.user.ObserveHiddenAuthorIdsUseCase
import dev.echoapp.echo.ui.common.MapFocusManager
import dev.echoapp.echo.utils.Constants
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
    private val deletePostUseCase: DeletePostUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val submitReportUseCase: SubmitReportUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    observeHiddenAuthorIdsUseCase: ObserveHiddenAuthorIdsUseCase,
    private val locationProvider: LocationProvider,
    private val mapFocusManager: MapFocusManager,
    authRepository: AuthRepository
) : ViewModel() {

    /**
     * Record which located post the user tapped to view on the map; the screen then
     * navigates to the Map tab, where [mapFocusManager]'s request is consumed. No-op for
     * a post without coordinates.
     */
    fun focusPostOnMap(post: Post) {
        val lat = post.latitude ?: return
        val lng = post.longitude ?: return
        mapFocusManager.request(post.id, lat, lng)
    }

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
    // The default feed is backed by a Room cache (offline-first source of truth): it
    // paints instantly on cold launch and works offline, while pages are fetched from
    // Firestore and written through to the cache — so a session only bills reads for
    // what's actually scrolled to.
    private val cachedFeed: Flow<List<Post>> = getPostsUseCase.feed()
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
            // The tag view is a short-lived one-shot query (not cached); blocked
            // authors are still filtered out reactively.
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
            combine(cachedFeed, _isLoadingFirstPage, blockedIds) { posts, loadingFirst, blocked ->
                FeedUiState(
                    posts = posts.filterNot { it.authorId in blocked },
                    currentTag = null,
                    // With a warm cache the feed shows instantly; only spin when we have
                    // nothing cached yet and the first network page is still loading.
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
            // The repo writes the page through to Room; the cached feed flow drives the
            // UI, so we only track the cursor / end-of-feed here.
            val cursor = oldestTimestamp
            val page = if (reset || cursor == null) {
                getPostsUseCase.refresh(Constants.FEED_PAGE_SIZE)
            } else {
                getPostsUseCase.loadMore(cursor, Constants.FEED_PAGE_SIZE)
            }
            _endReached.value = page.size < Constants.FEED_PAGE_SIZE
            page.lastOrNull()?.let { oldestTimestamp = it.timestamp }
        } catch (e: Exception) {
            // Offline / failed refresh: keep whatever the cache already shows and tell
            // the user, rather than wiping the feed.
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

    /** Delete one of the current user's own posts. */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't delete the post. Please try again.")
            }
        }
    }

    /** Edit the message of one of the current user's own posts. */
    fun updatePost(postId: String, newMessage: String) {
        viewModelScope.launch {
            updatePostUseCase(postId, newMessage).onFailure { e ->
                _uiEvent.send(e.message ?: "Couldn't update the post. Please try again.")
            }
        }
    }

    fun toggleLike(postId: String) {
        val current = uiState.value.posts.find { it.id == postId } ?: return
        val newLiked = !current.likedByCurrentUser
        val newCount = (current.likeCount + if (newLiked) 1 else -1).coerceAtLeast(0)
        viewModelScope.launch {
            // Optimistically update the cache (the feed's source of truth) so the heart
            // flips instantly; the network write follows and we revert on failure.
            getPostsUseCase.setCachedLike(postId, newLiked, newCount)
            toggleLikeUseCase(postId)
                .onFailure { e ->
                    getPostsUseCase.setCachedLike(postId, current.likedByCurrentUser, current.likeCount)
                    _uiEvent.send(e.message ?: "Couldn't update your like. Please try again.")
                }
        }
    }
}
