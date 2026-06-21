package dev.echoapp.echo.domain.usecase.user

import dev.echoapp.echo.domain.repository.UserRepository
import javax.inject.Inject

/** Unblock a previously blocked user. */
class UnblockUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(blockedUid: String): Result<Unit> =
        userRepository.unblockUser(blockedUid)
}
