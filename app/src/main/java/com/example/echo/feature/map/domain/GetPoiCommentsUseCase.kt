package com.example.echo.feature.map.domain

import com.example.echo.domain.model.Comment
import com.example.echo.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPoiCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    operator fun invoke(poiId: String): Flow<List<Comment>> {
        return commentRepository.getCommentsForPoi(poiId)
    }
}
