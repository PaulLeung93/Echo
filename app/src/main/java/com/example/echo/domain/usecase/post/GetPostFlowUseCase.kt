package com.example.echo.domain.usecase.post

import com.example.echo.domain.model.Post
import com.example.echo.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting a single post by ID as a reactive Flow.
 */
class GetPostFlowUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Get a post stream by its ID.
     * @param postId The ID of the post to observe.
     * @return Flow emitting post updates.
     */
    operator fun invoke(postId: String): Flow<Post?> {
        return postRepository.getPostByIdFlow(postId)
    }
}
