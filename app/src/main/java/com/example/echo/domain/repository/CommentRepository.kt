package com.example.echo.domain.repository

import com.example.echo.domain.model.Comment
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
}
