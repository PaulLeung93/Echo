package dev.echoapp.echo.domain.usecase.comment

import dev.echoapp.echo.domain.repository.CommentRepository
import javax.inject.Inject

/**
 * Use case for deleting a comment from a POI.
 */
class DeletePoiCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(poiId: String, commentId: String): Result<Unit> =
        runCatching { commentRepository.deleteCommentFromPoi(poiId, commentId) }
}
