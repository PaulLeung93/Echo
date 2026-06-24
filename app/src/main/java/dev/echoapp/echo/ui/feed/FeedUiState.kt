package dev.echoapp.echo.ui.feed

import dev.echoapp.echo.domain.model.Post

/** Which feed the user is viewing: the nearby/neighborhood feed or the people they follow. */
enum class FeedMode { NEARBY, FOLLOWING }

/**
 * UI State for the Feed screen.
 */
data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentTag: String? = null,
    val feedMode: FeedMode = FeedMode.NEARBY
)
