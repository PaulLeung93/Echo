package dev.echoapp.echo.domain.usecase.post

import dev.echoapp.echo.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for updating a post's message.
 */
class UpdatePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Update a post's message.
     * @param postId The ID of the post to update.
     * @param newMessage The new message content.
     * @return Result indicating success or failure.
     */
    suspend operator fun invoke(postId: String, newMessage: String): Result<Unit> {
        val trimmedMessage = newMessage.trim()
        if (trimmedMessage.isBlank()) {
            return Result.failure(ValidationException("Message cannot be empty"))
        }
        
        return try {
            postRepository.updatePost(postId, trimmedMessage)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
