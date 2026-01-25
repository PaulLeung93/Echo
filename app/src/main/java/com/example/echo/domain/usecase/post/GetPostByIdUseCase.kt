package com.example.echo.domain.usecase.post

import com.example.echo.domain.model.Post
import com.example.echo.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for getting a single post by ID.
 */
class GetPostByIdUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Get a post by its ID.
     * @param postId The ID of the post to retrieve.
     * @return The post, or null if not found.
     */
    suspend operator fun invoke(postId: String): Post? {
        return postRepository.getPostById(postId)
    }
}
