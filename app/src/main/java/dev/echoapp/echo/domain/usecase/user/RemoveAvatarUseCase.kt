package dev.echoapp.echo.domain.usecase.user

import dev.echoapp.echo.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Remove the current user's avatar — deletes the stored photo and clears the
 * `photoUrl` on their profile so they fall back to the initials avatar.
 */
class RemoveAvatarUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> = userRepository.removeAvatar()
}
