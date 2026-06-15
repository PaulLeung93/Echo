package com.example.echo.domain.usecase.auth

import com.example.echo.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Discards a freshly-created account that never finished profile setup (the user
 * cancelled on the Complete Profile screen, or signed up with Google and backed
 * out). Deletes the Firebase Auth account directly — no re-auth is needed for a
 * recent sign-in. If Firebase still demands a recent login (an older session),
 * falls back to signing out so the user is never trapped on the profile screen.
 */
class DeleteProvisionalAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.deleteCurrentAuthAccount().onFailure {
            authRepository.signOut()
        }
    }
}
