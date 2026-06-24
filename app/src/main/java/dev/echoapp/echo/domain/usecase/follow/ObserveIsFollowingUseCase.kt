package dev.echoapp.echo.domain.usecase.follow

import dev.echoapp.echo.domain.repository.FollowRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observe whether the current user follows [targetUid]. */
class ObserveIsFollowingUseCase @Inject constructor(
    private val followRepository: FollowRepository
) {
    operator fun invoke(targetUid: String): Flow<Boolean> =
        followRepository.observeIsFollowing(targetUid)
}
