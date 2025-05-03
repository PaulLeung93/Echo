package com.example.echo.ui.feed

import com.example.echo.models.Post

sealed class FeedUiState {
    object Loading : FeedUiState()

    data class Success(
        val posts: List<Post>,
        val filteredPosts: List<Post>,
        val postLikes: Map<String, Int>,
        val userLikes: Set<String>,
        val commentLikes: Map<String, Int>,
        val currentTagFilter: String?,
        val isRefreshing: Boolean = false
    ) : FeedUiState()

    data class Error(val message: String) : FeedUiState()
}
