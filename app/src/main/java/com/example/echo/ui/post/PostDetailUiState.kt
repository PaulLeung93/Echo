package com.example.echo.ui.post

import com.example.echo.domain.model.Comment
import com.example.echo.domain.model.Post

/**
 * UI State for the Post Detail screen.
 */
data class PostDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
