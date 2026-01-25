package com.example.echo.domain.usecase.auth

import com.example.echo.domain.model.User
import com.example.echo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for signing in with email and password.
 */
class SignInWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Sign in with email and password.
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
        
        if (trimmedPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("Password cannot be empty"))
        }
        
        return authRepository.signInWithEmail(trimmedEmail, trimmedPassword)
    }
}
