package dev.echoapp.echo.domain.usecase.post

import dev.echoapp.echo.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for deleting a post.
 */
class DeletePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Delete a post by its ID.
     * @param postId The ID of the post to delete.
     * @return Result indicating success or failure.
     */
    suspend operator fun invoke(postId: String): Result<Unit> {
        return try {
            postRepository.deletePost(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
