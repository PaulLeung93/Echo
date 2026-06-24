package dev.echoapp.echo.domain.usecase.post

import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.domain.repository.FollowRepository
import dev.echoapp.echo.domain.repository.PostRepository
import dev.echoapp.echo.utils.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

/**
 * The "Following" feed: newest posts from everyone the current user follows,
 * regardless of distance. Re-reads whenever the set of followed users changes.
 * Emits an empty list when the user follows no one (or is a guest).
 */
class GetFollowingFeedUseCase @Inject constructor(
    private val followRepository: FollowRepository,
    private val postRepository: PostRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Post>> =
        followRepository.observeFollowingIds().mapLatest { ids ->
            postRepository.getPostsByAuthors(ids, Constants.POSTS_QUERY_LIMIT)
        }
}
