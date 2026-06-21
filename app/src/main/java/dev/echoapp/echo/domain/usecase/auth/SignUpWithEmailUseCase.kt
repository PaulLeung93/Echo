package dev.echoapp.echo.domain.usecase.auth

import dev.echoapp.echo.domain.model.User
import dev.echoapp.echo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for signing up with email and password.
 */
class SignUpWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Sign up with email and password.
     * @param email The user's email.
     * @param password The user's password.
     * @return Result containing User on success, or exception on failure.
     */
    suspend operator fun invoke(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        
        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty"))
        }
        
        if (trimmedPassword.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters")
            )
        }
        
        return authRepository.signUpWithEmail(trimmedEmail, trimmedPassword)
    }
    
    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
