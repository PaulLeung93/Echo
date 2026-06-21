package dev.echoapp.echo.domain.usecase.comment

import dev.echoapp.echo.domain.model.Comment
import dev.echoapp.echo.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting comments on a post.
 */
class GetCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    /**
     * Get comments for a specific post.
     * @param postId The ID of the post.
     * @return Flow of comments for reactive updates.
     */
    operator fun invoke(postId: String): Flow<List<Comment>> {
        return commentRepository.getCommentsForPost(postId)
    }
}
