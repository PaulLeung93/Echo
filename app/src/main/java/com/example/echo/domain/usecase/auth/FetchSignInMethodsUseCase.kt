package com.example.echo.domain.usecase.auth

import com.example.echo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for fetching sign-in methods for a given email.
 */
class FetchSignInMethodsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Get sign-in methods.
     * @param email The email to check.
     * @return List of method strings.
     */
    suspend operator fun invoke(email: String): List<String> {
        return authRepository.fetchSignInMethods(email)
    }
}
