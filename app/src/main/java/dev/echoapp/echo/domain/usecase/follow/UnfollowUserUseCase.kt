package dev.echoapp.echo.domain.usecase.follow

import dev.echoapp.echo.domain.repository.FollowRepository
import javax.inject.Inject

/** Unfollow a user. */
class UnfollowUserUseCase @Inject constructor(
    private val followRepository: FollowRepository
) {
    suspend operator fun invoke(targetUid: String): Result<Unit> =
        followRepository.unfollowUser(targetUid)
}
