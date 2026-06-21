package dev.echoapp.echo.ui.post

import dev.echoapp.echo.domain.model.Comment
import dev.echoapp.echo.domain.model.Post

/**
 * UI State for the Post Detail screen.
 */
data class PostDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Current viewer's uid — used to gate the per-comment delete affordance. */
    val currentUserId: String? = null
)
