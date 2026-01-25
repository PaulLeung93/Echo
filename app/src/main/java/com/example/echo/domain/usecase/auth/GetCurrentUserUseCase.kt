package com.example.echo.domain.usecase.auth

import com.example.echo.domain.model.User
import com.example.echo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for getting the current authenticated user.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Get the currently signed-in user.
     * @return The current User, or null if not signed in.
     */
    operator fun invoke(): User? = authRepository.getCurrentUser()
    
    /**
     * Check if a user is currently signed in.
     */
    fun isSignedIn(): Boolean = authRepository.isSignedIn()
    
    /**
     * Check if the current user is authenticated (not anonymous).
     */
    fun isAuthenticated(): Boolean = authRepository.isAuthenticated()
}
