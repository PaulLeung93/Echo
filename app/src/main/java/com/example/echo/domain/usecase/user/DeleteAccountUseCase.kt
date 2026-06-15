package com.example.echo.domain.usecase.user

import com.example.echo.domain.repository.ReauthCredential
import com.example.echo.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import javax.inject.Inject

/**
 * Permanently delete the current user's account after re-authenticating with
 * [reauth]. Maps auth failures to clear, actionable messages.
 */
class DeleteAccountUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(reauth: ReauthCredential): Result<Unit> =
        userRepository.deleteAccount(reauth).recoverCatching { e ->
            when (e) {
                is FirebaseAuthInvalidCredentialsException ->
                    throw IllegalStateException("Incorrect password. Please try again.")
                is FirebaseAuthRecentLoginRequiredException ->
                    throw IllegalStateException("Please sign out and sign back in, then try again.")
                else -> throw e
            }
        }
}
