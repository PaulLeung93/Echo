package com.example.echo.domain.usecase.post

import com.example.echo.domain.model.Post
import com.example.echo.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Get all posts authored by a user, keyed on their stable uid (covers both
 * legacy email-username posts and new handle-username posts).
 */
class GetPostsByAuthorIdUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(authorId: String): Flow<List<Post>> =
        postRepository.getPostsByAuthorId(authorId)
}
