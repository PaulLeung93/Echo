package dev.echoapp.echo.domain.usecase.user

import dev.echoapp.echo.domain.model.UserProfile
import dev.echoapp.echo.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observe the current user's profile (e.g. for the Profile screen). */
class ObserveCurrentUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<UserProfile?> = userRepository.observeCurrentUserProfile()
}
