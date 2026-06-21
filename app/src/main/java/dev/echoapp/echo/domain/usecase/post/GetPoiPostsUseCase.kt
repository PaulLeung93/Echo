package dev.echoapp.echo.domain.usecase.post

import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for streaming the posts that make up a POI's thread.
 */
class GetPoiPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    /**
     * @param poiId The POI whose thread to observe.
     * @param descending true for newest-first (default), false for oldest-first.
     */
    operator fun invoke(poiId: String, descending: Boolean = true): Flow<List<Post>> =
        postRepository.getPostsForPoi(poiId, descending)
}
