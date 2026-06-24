package dev.echoapp.echo.domain.usecase.user

import dev.echoapp.echo.domain.model.UserProfile
import dev.echoapp.echo.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observe any user's public profile by uid (for the public profile screen). */
class ObserveUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(uid: String): Flow<UserProfile?> =
        userRepository.observeProfileById(uid)
}
