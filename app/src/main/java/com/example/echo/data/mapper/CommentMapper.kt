package com.example.echo.data.mapper

import com.example.echo.data.entity.CommentEntity
import com.example.echo.domain.model.Comment
import javax.inject.Inject

/**
 * Mapper for converting between CommentEntity (data layer) and Comment (domain layer).
 */
class CommentMapper @Inject constructor() {
    
    /**
     * Convert CommentEntity to domain Comment.
     * @param entity The Firestore entity to convert.
     * @return The domain Comment model.
     */
    fun toDomain(entity: CommentEntity): Comment {
        return Comment(
            id = entity.id ?: "",
            authorId = entity.authorId,
            username = entity.username,
            authorPhotoUrl = entity.authorPhotoUrl?.ifBlank { null },
            message = entity.message,
            timestamp = entity.timestamp
        )
    }
    
    /**
     * Convert a list of CommentEntities to domain Comments.
     * @param entities The list of Firestore entities.
     * @return List of domain Comment models.
     */
    fun toDomainList(entities: List<CommentEntity>): List<Comment> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Create a map representation for Firestore from comment parameters.
     * @param authorId The stable Firebase Auth uid of the author (used for ownership in security rules).
     * @param username The username of the commenter.
     * @param message The comment message.
     * @return Map representation for Firestore document.
     */
    fun toFirestoreMap(
        authorId: String,
        username: String,
        photoUrl: String?,
        message: String
    ): Map<String, Any> {
        return buildMap {
            put("authorId", authorId)
            put("username", username)
            // Denormalized author avatar; only written when set so the create rule's
            // optional field stays absent otherwise.
            if (!photoUrl.isNullOrBlank()) put("authorPhotoUrl", photoUrl)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
        }
    }
}
