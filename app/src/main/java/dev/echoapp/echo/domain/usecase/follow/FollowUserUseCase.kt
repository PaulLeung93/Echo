package dev.echoapp.echo.domain.usecase.follow

import dev.echoapp.echo.domain.repository.FollowRepository
import javax.inject.Inject

/** Follow another user. */
class FollowUserUseCase @Inject constructor(
    private val followRepository: FollowRepository
) {
    suspend operator fun invoke(targetUid: String): Result<Unit> =
        followRepository.followUser(targetUid)
}
