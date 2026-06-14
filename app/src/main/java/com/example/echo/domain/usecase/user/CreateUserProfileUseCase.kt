package com.example.echo.domain.usecase.user

import com.example.echo.domain.model.UserProfile
import com.example.echo.domain.repository.UserRepository
import com.example.echo.utils.isValidUsername
import javax.inject.Inject

/**
 * Validates the profile inputs and atomically claims the username + writes the
 * profile for the current user.
 */
class CreateUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        username: String
    ): Result<UserProfile> {
        val first = firstName.trim()
        val last = lastName.trim()
        val handle = username.trim().lowercase()

        if (first.isBlank()) return Result.failure(IllegalArgumentException("Please enter your first name."))
        if (last.isBlank()) return Result.failure(IllegalArgumentException("Please enter your last name."))
        if (!isValidUsername(handle)) {
            return Result.failure(IllegalArgumentException("Pick a valid username (3–20 chars: letters, numbers, _)."))
        }
        return userRepository.createProfile(first, last, handle)
    }
}
