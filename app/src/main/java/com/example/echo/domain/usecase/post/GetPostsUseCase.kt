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
     * Fetch one newest-first page via a one-time read (for the paginated feed).
     * @param afterTimestamp Cursor; null for the first page.
     * @param limit Page size.
     */
    suspend fun page(afterTimestamp: Long?, limit: Long): List<Post> =
        postRepository.getPostsPage(afterTimestamp, limit)

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
