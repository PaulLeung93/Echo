package com.example.echo.domain.usecase.comment

import com.example.echo.domain.model.Comment
import com.example.echo.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting comments on a POI.
 */
class GetPoiCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    /**
     * Get comments for a specific POI.
     * @param poiId The ID of the POI.
     * @return Flow of comments for reactive updates.
     */
    operator fun invoke(poiId: String): Flow<List<Comment>> {
        return commentRepository.getCommentsForPoi(poiId)
    }
}
