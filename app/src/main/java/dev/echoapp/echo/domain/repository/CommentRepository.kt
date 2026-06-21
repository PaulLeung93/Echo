package dev.echoapp.echo.domain.repository

import dev.echoapp.echo.domain.model.Comment
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Comment operations.
 * Defines the contract for comment-related data access.
 */
interface CommentRepository {
    
    /**
     * Get comments for a specific post.
     * @param postId The ID of the post.
     * @return Flow of comments for reactive updates.
     */
    fun getCommentsForPost(postId: String): Flow<List<Comment>>
    
    /**
     * Add a new comment to a post.
     * @param postId The ID of the post.
     * @param message The comment message content.
     * @return The created Comment object.
     */
    suspend fun addComment(postId: String, message: String): Comment
    
    /**
     * Delete a comment.
     * @param postId The ID of the parent post.
     * @param commentId The ID of the comment to delete.
     */
    suspend fun deleteComment(postId: String, commentId: String)

    /**
     * Get comments for a specific POI.
     * @param poiId The ID of the POI.
     * @return Flow of comments for reactive updates.
     */
    fun getCommentsForPoi(poiId: String): Flow<List<Comment>>

    /**
     * Add a new comment to a POI.
     * @param poiId The ID of the POI.
     * @param message The comment message content.
     * @return The created Comment object.
     */
    suspend fun addCommentToPoi(poiId: String, message: String): Comment

    /**
     * Delete a comment from a POI.
     * @param poiId The ID of the parent POI.
     * @param commentId The ID of the comment to delete.
     */
    suspend fun deleteCommentFromPoi(poiId: String, commentId: String)
}
