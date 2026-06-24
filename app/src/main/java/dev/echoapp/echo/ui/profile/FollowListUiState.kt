package dev.echoapp.echo.ui.profile

import dev.echoapp.echo.domain.model.UserProfile
import dev.echoapp.echo.domain.usecase.follow.FollowListType

/** UI state for the follower / following list screen. */
data class FollowListUiState(
    val selectedType: FollowListType = FollowListType.FOLLOWERS,
    val profiles: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
