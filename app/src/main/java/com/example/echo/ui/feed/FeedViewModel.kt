package com.example.echo.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.domain.usecase.post.GetPostsUseCase
import com.example.echo.domain.usecase.post.GetPostsByTagUseCase
import com.example.echo.domain.usecase.post.ToggleLikeUseCase
import com.example.echo.domain.usecase.post.RefreshPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val getPostsByTagUseCase: GetPostsByTagUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val refreshPostsUseCase: RefreshPostsUseCase
) : ViewModel() {

    private val _currentTag = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<FeedUiState> = combine(
        _currentTag.flatMapLatest { tag ->
            if (tag == null) getPostsUseCase() else getPostsByTagUseCase(tag)
        }.catch { e -> emit(emptyList()) },
        _currentTag
    ) { posts, tag ->
        FeedUiState(
            posts = posts,
            currentTag = tag,
            isLoading = false // Flow-based real-time updates don't strictly have a "loading" start here
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedUiState(isLoading = true)
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                refreshPostsUseCase()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setTagFilter(tag: String) {
        _currentTag.value = tag
    }

    fun clearTagFilter() {
        _currentTag.value = null
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                toggleLikeUseCase(postId)
            } catch (e: Exception) {
                // Potential error handling channel
            }
        }
    }
}
