package dev.echoapp.echo.ui.profile

import dev.echoapp.echo.domain.model.Poi
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.model.UserProfile

/** A favorited POI plus when it was favorited (for the removal-hold check). */
data class FavoritePlace(
    val poi: Poi,
    val favoritedAt: Long
)

/**
 * UI State for the Profile screen.
 */
data class ProfileUiState(
    val userPosts: List<Post> = emptyList(),
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val userProfile: UserProfile? = null,
    /** The user's favorited POIs ("My Places"), most-recent first. */
    val favoritePlaces: List<FavoritePlace> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
