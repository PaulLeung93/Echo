package dev.echoapp.echo.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * One-way "follow" graph. The client writes only its *own* `following/{target}`
 * edge; a Cloud Function mirrors the `followers` entry onto the target and keeps
 * the denormalized counts in sync (see docs/launch/FOLLOW_FEATURE.md, Appendix B).
 */
interface FollowRepository {

    /** Follow [targetUid] (stamped server-side). Idempotent if already following. */
    suspend fun followUser(targetUid: String): Result<Unit>

    /** Unfollow [targetUid]. Idempotent if not following. */
    suspend fun unfollowUser(targetUid: String): Result<Unit>

    /**
     * Observe whether the current user follows [targetUid]. Emits false when signed
     * out / guest, or on a read error.
     */
    fun observeIsFollowing(targetUid: String): Flow<Boolean>

    /**
     * Observe the uids the current user follows (drives the Following feed). Emits an
     * empty list when signed out / guest, or on a read error.
     */
    fun observeFollowingIds(): Flow<List<String>>

    /** One-shot read of [uid]'s followers (newest first, capped). For the list screen. */
    suspend fun getFollowerIds(uid: String): List<String>

    /** One-shot read of who [uid] follows (newest first, capped). For the list screen. */
    suspend fun getFollowingIds(uid: String): List<String>
}
