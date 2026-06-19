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
     * Fetch one page of newest-first posts via a one-time read (not a live listener),
     * so the feed only bills reads for the posts actually scrolled into view.
     * @param afterTimestamp Cursor: return posts strictly older than this timestamp,
     *   or the newest page when null.
     * @param limit Page size.
     */
    suspend fun getPostsPage(afterTimestamp: Long?, limit: Long): List<Post>

    /**
     * Fetch located posts within [radiusMeters] of a center via geohash range queries,
     * so the map only reads documents near the current viewport instead of the whole
     * collection. Returns posts already filtered to the true radius.
     */
    suspend fun getPostsNear(latitude: Double, longitude: Double, radiusMeters: Double): List<Post>
    
    /**
     * Get posts filtered by tag.
     * @param tag The tag to filter by.
     */
    fun getPostsByTag(tag: String): Flow<List<Post>>

    /**
     * Get all posts authored by a user, keyed on the stable [authorId] (uid) so
     * it covers both legacy (email-username) and new (handle-username) posts.
     */
    fun getPostsByAuthorId(authorId: String): Flow<List<Post>>

    /**
     * Get a single post by ID.
     * @param postId The ID of the post.
     */
    suspend fun getPostById(postId: String): Post?
    
    /**
     * Get a post by its ID as a Flow for real-time updates.
     */
    fun getPostByIdFlow(postId: String): Flow<Post?>
    
    /**
     * Create a new post.
     * @param message The post message content.
     * @param latitude Optional latitude for location.
     * @param longitude Optional longitude for location.
     * @param tags List of tags for the post.
     * @return Result containing Success or Failure
     */
    suspend fun createPost(
        message: String,
        latitude: Double?,
        longitude: Double?,
        tags: List<String>
    ): Result<Unit>
    
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
}
