package com.example.echo.feature.map.domain

import com.example.echo.domain.model.Comment
import com.example.echo.domain.repository.AuthRepository
import com.example.echo.domain.repository.CommentRepository
import javax.inject.Inject

class AddPoiCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(poiId: String, message: String): Comment {
        if (!authRepository.isAuthenticated()) {
            throw IllegalStateException("User must be authenticated to comment.")
        }
        return commentRepository.addCommentToPoi(poiId, message)
    }
}
