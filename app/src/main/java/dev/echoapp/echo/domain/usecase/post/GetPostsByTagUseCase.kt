package dev.echoapp.echo.domain.usecase.post

import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting posts filtered by a tag.
 */
class GetPostsByTagUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * Get posts stream filtered by tag.
     */
    operator fun invoke(tag: String): Flow<List<Post>> {
        return postRepository.getPostsByTag(tag)
    }
}
