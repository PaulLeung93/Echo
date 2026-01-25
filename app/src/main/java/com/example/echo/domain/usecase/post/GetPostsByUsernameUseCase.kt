package com.example.echo.domain.usecase.post

import com.example.echo.domain.model.Post
import com.example.echo.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting posts created by a specific user.
 */
class GetPostsByUsernameUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Get posts stream for a user.
     */
    operator fun invoke(username: String): Flow<List<Post>> {
        return postRepository.getPostsByUsername(username)
    }
}
