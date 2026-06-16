package com.example.echo.domain.usecase.user

import com.example.echo.domain.repository.UserRepository
import javax.inject.Inject

/** Block another user so their posts/comments are hidden from the current user. */
class BlockUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(blockedUid: String): Result<Unit> =
        userRepository.blockUser(blockedUid)
}
