package dev.echoapp.echo.domain.usecase.comment

import dev.echoapp.echo.domain.model.Comment
import dev.echoapp.echo.domain.repository.AuthRepository
import dev.echoapp.echo.domain.repository.CommentRepository
import javax.inject.Inject

/**
 * Use case for adding a comment to a POI.
 */
class AddPoiCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) {
    /**
     * Add a comment to a POI.
     * @param poiId The ID of the POI to comment on.
     * @param message The comment message.
     * @return Result containing the created Comment, or exception on failure.
     */
    suspend operator fun invoke(poiId: String, message: String): Result<Comment> {
        // Check if user is authenticated (not anonymous)
        if (!authRepository.isAuthenticated()) {
            return Result.failure(
                IllegalStateException("You must be signed in to comment")
            )
        }
        
        val trimmedMessage = message.trim()
        if (trimmedMessage.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Comment cannot be empty")
            )
        }
        
        return try {
            val comment = commentRepository.addCommentToPoi(poiId, trimmedMessage)
            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
