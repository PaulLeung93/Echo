package com.example.echo.domain.repository

import com.example.echo.domain.model.Post
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Post operations.
 * Defines the contract for data access without specifying implementation details.
 */
interface PostRepository {
    
    /**
     * Get all posts as a Flow for reactive updates.
     */
    fun getPosts(): Flow<List<Post>>
    
    /**
     * Get posts filtered by tag.
     * @param tag The tag to filter by.
     */
    fun getPostsByTag(tag: String): Flow<List<Post>>
    
    /**
     * Get posts that have location data (for map display).
     */
    fun getPostsWithLocation(): Flow<List<Post>>
    
    /**
     * Get posts by a specific user.
     * @param userEmail The user's email address.
     */
    fun getPostsByUser(userEmail: String): Flow<List<Post>>
    
    /**
     * Get a single post by ID.
     * @param postId The ID of the post.
     */
    suspend fun getPostById(postId: String): Post?
    
    /**
     * Create a new post.
     * @param message The post message content.
     * @param latitude Optional latitude for location.
     * @param longitude Optional longitude for location.
     * @param tags List of tags for the post.
     * @return The ID of the created post.
     */
    suspend fun createPost(
        message: String,
        latitude: Double?,
        longitude: Double?,
        tags: List<String>
    ): String
    
    /**
     * Update an existing post's message.
     * @param postId The ID of the post to update.
     * @param newMessage The updated message content.
     */
    suspend fun updatePost(postId: String, newMessage: String)
    
    /**
     * Delete a post.
     * @param postId The ID of the post to delete.
     */
    suspend fun deletePost(postId: String)
    
    /**
     * Toggle like status for a post.
     * @param postId The ID of the post.
     * @return True if the post is now liked, false if unliked.
     */
    suspend fun toggleLike(postId: String): Boolean
    
    /**
     * Refresh posts from the remote data source.
     */
    suspend fun refreshPosts()
}
