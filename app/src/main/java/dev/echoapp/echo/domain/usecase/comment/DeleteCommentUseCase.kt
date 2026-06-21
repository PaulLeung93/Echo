package dev.echoapp.echo.domain.usecase.comment

import dev.echoapp.echo.domain.repository.CommentRepository
import javax.inject.Inject

/**
 * Use case for deleting a comment from a post. Ownership is enforced server-side
 * by the Firestore rules (author-only delete); the UI only offers it for the
 * viewer's own comments.
 */
class DeleteCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(postId: String, commentId: String): Result<Unit> =
        runCatching { commentRepository.deleteComment(postId, commentId) }
}
