package com.example.echo.domain.usecase.auth

import com.example.echo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for signing out the current user.
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Sign out the current user.
     */
    suspend operator fun invoke() {
        authRepository.signOut()
    }
}
