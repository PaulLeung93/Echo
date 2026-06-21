package dev.echoapp.echo.ui.feed

import dev.echoapp.echo.domain.model.Post

/**
 * UI State for the Feed screen.
 */
data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentTag: String? = null
)
