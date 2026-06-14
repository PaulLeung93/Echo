package com.example.echo.domain.usecase.user

import com.example.echo.domain.repository.UserRepository
import com.example.echo.utils.isValidUsername
import javax.inject.Inject

/** Result of checking a username, for driving the sign-up field's UI state. */
enum class UsernameStatus { Available, Taken, Invalid, Error }

/**
 * Validates a username's format, then checks availability against the
 * `usernames` reservation index.
 */
class CheckUsernameAvailabilityUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(username: String): UsernameStatus {
        val handle = username.trim().lowercase()
        if (!isValidUsername(handle)) return UsernameStatus.Invalid
        return userRepository.isUsernameAvailable(handle).fold(
            onSuccess = { available -> if (available) UsernameStatus.Available else UsernameStatus.Taken },
            onFailure = { UsernameStatus.Error }
        )
    }
}
