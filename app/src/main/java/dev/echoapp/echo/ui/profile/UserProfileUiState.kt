package dev.echoapp.echo.ui.profile

import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.model.UserProfile

/**
 * UI state for the public profile screen ([UserProfileScreen]) — a read-only view
 * of *another* user's profile and posts. Distinct from [ProfileUiState], which is
 * the current user's own (editable) profile.
 */
data class UserProfileUiState(
    val userProfile: UserProfile? = null,
    val userPosts: List<Post> = emptyList(),
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    /** True when the viewed profile is the current user's own (hides report/block/follow). */
    val isSelf: Boolean = false,
    /** Whether the current user follows this profile. */
    val isFollowing: Boolean = false,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    /** A follow/unfollow write is in flight (debounces double-taps). */
    val followInFlight: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
