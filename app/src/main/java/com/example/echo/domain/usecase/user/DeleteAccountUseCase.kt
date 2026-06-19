package com.example.echo.domain.usecase.user

import com.example.echo.domain.repository.ReauthCredential
import com.example.echo.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Permanently delete the current user's account after re-authenticating with
 * [reauth]. The repository re-auths first and maps provider-specific failures
 * (wrong password, recent-login-required) to user-facing messages, so this
 * stays free of any auth-provider types.
 */
class DeleteAccountUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(reauth: ReauthCredential): Result<Unit> =
        userRepository.deleteAccount(reauth)
}
