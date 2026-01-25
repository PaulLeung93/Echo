package com.example.echo.domain.usecase.auth

import com.example.echo.domain.model.User
import com.example.echo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for signing in with Google.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Sign in with Google ID token.
     * @param idToken The Google ID token.
     * @return Result containing User on success, or exception on failure.
     */
    suspend operator fun invoke(idToken: String): Result<User> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Google ID token is required"))
        }
        
        return authRepository.signInWithGoogle(idToken)
    }
}
