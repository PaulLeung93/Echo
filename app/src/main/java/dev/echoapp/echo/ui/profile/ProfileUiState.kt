package dev.echoapp.echo.ui.profile

import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.model.UserProfile

/**
 * UI State for the Profile screen.
 */
data class ProfileUiState(
    val userPosts: List<Post> = emptyList(),
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
