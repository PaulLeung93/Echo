package dev.echoapp.echo.domain.usecase.user

import dev.echoapp.echo.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Author uids whose content the current user should not see: the union of users
 * they blocked and users who blocked them (blocking is mutual). Emits an empty
 * set for guests / signed-out users. Drives feed/map/comment filtering.
 */
class ObserveHiddenAuthorIdsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<Set<String>> = userRepository.observeHiddenUserIds()
}
