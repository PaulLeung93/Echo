package dev.echoapp.echo.domain.usecase.user

import dev.echoapp.echo.domain.repository.UserRepository
import javax.inject.Inject

/** Block another user so their posts/comments are hidden from the current user. */
class BlockUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(blockedUid: String): Result<Unit> =
        userRepository.blockUser(blockedUid)
}
