package com.example.echo.domain.usecase.user

import com.example.echo.domain.repository.UserRepository
import javax.inject.Inject

/** Max bio length (mirrors the Firestore rule). */
const val BIO_MAX_LENGTH = 160

/**
 * Validate and update the current user's editable profile fields (first/last
 * name + bio). Username is immutable, so it isn't touched here.
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(firstName: String, lastName: String, bio: String): Result<Unit> {
        val first = firstName.trim()
        val last = lastName.trim()
        val trimmedBio = bio.trim()
        if (first.isBlank()) return Result.failure(IllegalArgumentException("Please enter your first name."))
        if (last.isBlank()) return Result.failure(IllegalArgumentException("Please enter your last name."))
        if (trimmedBio.length > BIO_MAX_LENGTH) {
            return Result.failure(IllegalArgumentException("Bio must be $BIO_MAX_LENGTH characters or fewer."))
        }
        return userRepository.updateProfile(first, last, trimmedBio)
    }
}
