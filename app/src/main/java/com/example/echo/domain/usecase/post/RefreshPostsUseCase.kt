package com.example.echo.domain.usecase.post

import com.example.echo.domain.repository.PostRepository
import javax.inject.Inject

/**
 * Use case for refreshing the posts data from the remote source.
 */
class RefreshPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Trigger a refresh of posts.
     */
    suspend operator fun invoke(): Result<Unit> {
        return postRepository.refreshPosts()
    }
}
