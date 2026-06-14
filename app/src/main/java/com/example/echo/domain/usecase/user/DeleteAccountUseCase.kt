package com.example.echo.domain.usecase.user

import com.example.echo.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import javax.inject.Inject

/**
 * Permanently delete the current user's account. Maps the recent-login
 * requirement to a clear, actionable message.
 */
class DeleteAccountUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        userRepository.deleteAccount().recoverCatching { e ->
            if (e is FirebaseAuthRecentLoginRequiredException) {
                throw IllegalStateException("For your security, please sign out and sign back in, then delete your account.")
            }
            throw e
        }
}
