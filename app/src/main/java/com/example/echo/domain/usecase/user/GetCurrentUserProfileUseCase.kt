package com.example.echo.domain.usecase.user

import com.example.echo.domain.model.UserProfile
import com.example.echo.domain.repository.UserRepository
import javax.inject.Inject

/**
 * One-shot read of the current user's profile. `success(null)` means confirmed
 * "no profile"; `failure` means the read itself failed (don't treat as absent).
 */
class GetCurrentUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<UserProfile?> = userRepository.getCurrentUserProfile()
}
