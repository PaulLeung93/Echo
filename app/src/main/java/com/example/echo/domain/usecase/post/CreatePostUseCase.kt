package com.example.echo.domain.usecase.post

import com.example.echo.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for creating a new post.
 * Encapsulates validation and business logic for post creation.
 */
class CreatePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Create a new post with validation.
     * @param message The post message content.
     * @param includeLocation Whether to include location data.
     * @param latitude Optional latitude coordinate.
     * @param longitude Optional longitude coordinate.
     * @param tags List of tags for the post.
     * @return Result with success or exception on failure.
     */
    suspend operator fun invoke(
        message: String,
        includeLocation: Boolean,
        latitude: Double?,
        longitude: Double?,
        tags: List<String>
    ): Result<Unit> {
        // Validate message
        val trimmedMessage = message.trim()
        if (trimmedMessage.isBlank()) {
            return Result.failure(ValidationException("Message cannot be empty"))
        }
        
        // Validate tags
        if (tags.any { it.length > MAX_TAG_LENGTH }) {
            return Result.failure(
                ValidationException("Tags cannot be longer than $MAX_TAG_LENGTH characters")
            )
        }
        
        if (tags.size > MAX_TAGS_COUNT) {
            return Result.failure(
                ValidationException("You can only add up to $MAX_TAGS_COUNT tags")
            )
        }
        
        // Determine location to store
        val finalLatitude = if (includeLocation) latitude else null
        val finalLongitude = if (includeLocation) longitude else null
        
        return postRepository.createPost(
            message = trimmedMessage,
            latitude = finalLatitude,
            longitude = finalLongitude,
            tags = tags.map { it.trim().lowercase() }
        )
    }
    
    companion object {
        const val MAX_TAG_LENGTH = 15
        const val MAX_TAGS_COUNT = 3
    }
}

/**
 * Exception thrown when input validation fails.
 */
class ValidationException(message: String) : Exception(message)
