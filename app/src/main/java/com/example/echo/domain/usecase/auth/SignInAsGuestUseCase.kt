package com.example.echo.domain.usecase.auth

import com.example.echo.domain.model.User
import com.example.echo.domain.repository.AuthRepository
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
