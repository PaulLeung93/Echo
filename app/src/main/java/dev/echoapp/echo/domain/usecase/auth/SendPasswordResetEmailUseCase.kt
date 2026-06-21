package dev.echoapp.echo.domain.usecase.auth

import dev.echoapp.echo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for sending a password reset email.
 */
class SendPasswordResetEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Send password reset link to email.
     * @param email The target email address.
     * @return Result of the operation.
     */
    suspend operator fun invoke(email: String): Result<Unit> {
        return authRepository.sendPasswordResetEmail(email)
    }
}
