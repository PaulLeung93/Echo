package dev.echoapp.echo.domain.usecase.user

import dev.echoapp.echo.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Upload a new avatar (compressed JPEG bytes) for the current user and persist its
 * URL on their profile. Returns the download URL on success.
 */
class UpdateAvatarUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): Result<String> {
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Couldn't read the selected image."))
        }
        return userRepository.updateAvatar(imageBytes)
    }
}
