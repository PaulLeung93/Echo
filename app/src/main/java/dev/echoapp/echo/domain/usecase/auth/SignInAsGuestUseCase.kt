package dev.echoapp.echo.domain.usecase.auth

import dev.echoapp.echo.domain.model.User
import dev.echoapp.echo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for signing in anonymously as a guest.
 */
class SignInAsGuestUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Sign in anonymously.
     * @return Result containing User on success, or exception on failure.
     */
    suspend operator fun invoke(): Result<User> {
        return authRepository.signInAsGuest()
    }
}
