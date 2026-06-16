package com.example.echo.domain.usecase.user

import com.example.echo.domain.repository.UserRepository
import javax.inject.Inject

/** Unblock a previously blocked user. */
class UnblockUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(blockedUid: String): Result<Unit> =
        userRepository.unblockUser(blockedUid)
}
