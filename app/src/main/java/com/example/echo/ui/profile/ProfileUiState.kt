package com.example.echo.ui.profile

import com.example.echo.domain.model.Post

/**
 * UI State for the Profile screen.
 */
data class ProfileUiState(
    val userPosts: List<Post> = emptyList(),
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
