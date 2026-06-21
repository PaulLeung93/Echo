package dev.echoapp.echo.domain.usecase.post

import dev.echoapp.echo.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for toggling like status on a post.
 * Encapsulates the like/unlike business logic.
 */
class ToggleLikeUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Toggle like status for a post.
     * @param postId The ID of the post to like/unlike.
     * @return Result with true if now liked, false if unliked.
     */
    suspend operator fun invoke(postId: String): Result<Boolean> {
        return try {
            val isNowLiked = postRepository.toggleLike(postId)
            Result.success(isNowLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
