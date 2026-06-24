package dev.echoapp.echo.domain.usecase.follow

import dev.echoapp.echo.domain.model.UserProfile
import dev.echoapp.echo.domain.repository.FollowRepository
import dev.echoapp.echo.domain.repository.UserRepository
import javax.inject.Inject

/** Which side of the follow graph a list shows. */
enum class FollowListType { FOLLOWERS, FOLLOWING }

/**
 * Resolve a user's followers (or who they follow) to full profiles for the list
 * screen: read the edge uids, then batch-resolve their profiles. A leftover edge to a
 * deleted user resolves to no profile and is silently dropped.
 */
class GetFollowListUseCase @Inject constructor(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(uid: String, type: FollowListType): Result<List<UserProfile>> =
        runCatching {
            val ids = when (type) {
                FollowListType.FOLLOWERS -> followRepository.getFollowerIds(uid)
                FollowListType.FOLLOWING -> followRepository.getFollowingIds(uid)
            }
            if (ids.isEmpty()) return@runCatching emptyList()
            userRepository.getProfilesByIds(ids).getOrThrow()
        }
}
