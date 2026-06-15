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
     * Get posts filtered by a specific tag.
     * @param tag The tag to filter by.
     * @return Flow of filtered posts.
     */
    fun byTag(tag: String): Flow<List<Post>> = postRepository.getPostsByTag(tag)
    
    /**
     * Get posts that have location data for map display.
     * @return Flow of posts with location.
     */
    fun withLocation(): Flow<List<Post>> = postRepository.getPostsWithLocation()
    
    /**
     * Get posts by a specific user.
     * @param userEmail The user's email.
     * @return Flow of user's posts.
     */
    fun byUser(userEmail: String): Flow<List<Post>> = postRepository.getPostsByUsername(userEmail)
}
