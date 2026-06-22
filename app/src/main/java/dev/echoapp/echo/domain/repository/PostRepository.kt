package dev.echoapp.echo.domain.repository

import dev.echoapp.echo.domain.model.Post
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
     * Observe the locally-cached feed (Room). This is the feed's offline-first display
     * source of truth: it emits instantly on a cold launch and keeps working offline,
     * and is refreshed from Firestore via [refreshFeed] / [loadMoreFeed].
     */
    fun observeFeed(): Flow<List<Post>>

    /**
     * Fetch the newest page from Firestore and replace the cached feed atomically.
     * @param limit Page size.
     * @return The fetched page (so the caller can advance its pagination cursor).
     */
    suspend fun refreshFeed(limit: Long): List<Post>

    /**
     * Fetch the next page (posts strictly older than [afterTimestamp]) from Firestore
     * and append it to the cached feed.
     * @return The fetched page (so the caller can advance its cursor / detect the end).
     */
    suspend fun loadMoreFeed(afterTimestamp: Long, limit: Long): List<Post>

    /**
     * Optimistically reflect a like toggle in the cached feed so the UI updates
     * instantly; the authoritative network write happens via [toggleLike].
     */
    suspend fun setCachedLike(postId: String, liked: Boolean, likeCount: Int)

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
     * Live stream of the posts belonging to a POI's thread (`poiId == poiId`), ordered
     * by timestamp.
     * @param descending true for newest-first (default thread order), false for oldest-first.
     */
    fun getPostsForPoi(poiId: String, descending: Boolean): Flow<List<Post>>
    
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
        tags: List<String>,
        poiId: String? = null,
        poiName: String? = null
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
