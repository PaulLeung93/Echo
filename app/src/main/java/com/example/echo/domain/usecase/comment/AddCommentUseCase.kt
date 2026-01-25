package com.example.echo.domain.usecase.comment

import com.example.echo.domain.model.Comment
import com.example.echo.domain.repository.AuthRepository
import com.example.echo.domain.repository.CommentRepository
import javax.inject.Inject

/**
 * Use case for adding a comment to a post.
 */
class AddCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) {
    /**
     * Add a comment to a post.
     * @param postId The ID of the post to comment on.
     * @param message The comment message.
     * @return Result containing the created Comment, or exception on failure.
     */
    suspend operator fun invoke(postId: String, message: String): Result<Comment> {
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
            val comment = commentRepository.addComment(postId, trimmedMessage)
            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
