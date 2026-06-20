package com.example.echo.domain.usecase.post

import com.example.echo.domain.model.Post
import com.example.echo.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for fetching posts with optional tag filtering.
 * Encapsulates the business logic for retrieving post lists.
 */
class GetPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Get all posts.
     * @return Flow of posts for reactive updates.
     */
    operator fun invoke(): Flow<List<Post>> = postRepository.getPosts()

    /**
     * Observe the offline-first cached feed (Room). Emits instantly on cold launch and
     * works offline; refreshed from the network via [refresh] / [loadMore].
     */
    fun feed(): Flow<List<Post>> = postRepository.observeFeed()

    /** Fetch the newest page and replace the cached feed. Returns the fetched page. */
    suspend fun refresh(limit: Long): List<Post> = postRepository.refreshFeed(limit)

    /** Fetch the next page (older than [afterTimestamp]) and append to the cache. */
    suspend fun loadMore(afterTimestamp: Long, limit: Long): List<Post> =
        postRepository.loadMoreFeed(afterTimestamp, limit)

    /** Optimistically reflect a like toggle in the cached feed. */
    suspend fun setCachedLike(postId: String, liked: Boolean, likeCount: Int) =
        postRepository.setCachedLike(postId, liked, likeCount)

    /**
     * Fetch located posts within [radiusMeters] of a center (geohash range query),
     * for the viewport-bounded map.
     */
    suspend fun near(latitude: Double, longitude: Double, radiusMeters: Double): List<Post> =
        postRepository.getPostsNear(latitude, longitude, radiusMeters)
    
    /**
     * Get posts filtered by a specific tag.
     * @param tag The tag to filter by.
     * @return Flow of filtered posts.
     */
    fun byTag(tag: String): Flow<List<Post>> = postRepository.getPostsByTag(tag)
}
