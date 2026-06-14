package com.example.echo.domain.usecase.user

import com.example.echo.domain.model.UserProfile
import com.example.echo.domain.repository.UserRepository
import javax.inject.Inject

/** One-shot read of the current user's profile (null if they haven't set one up). */
class GetCurrentUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): UserProfile? = userRepository.getCurrentUserProfile()
}
