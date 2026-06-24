package dev.echoapp.echo.data.repository

import dev.echoapp.echo.di.IoDispatcher
import dev.echoapp.echo.domain.repository.FollowRepository
import dev.echoapp.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-backed follow graph. The only client-writable path is the current
 * user's own `users/{me}/following/{target}` edge; the `followers` mirror and the
 * `followerCount`/`followingCount` fields are maintained by Cloud Functions.
 */
@Singleton
class FollowRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FollowRepository {

    private val usersCollection = firestore.collection(Constants.COLLECTION_USERS)

    private companion object {
        /** Cap on a follower/following list read (v1 has no pagination). */
        const val FOLLOW_LIST_LIMIT = 200L
    }

    private fun followingDoc(meUid: String, targetUid: String) =
        usersCollection.document(meUid)
            .collection(Constants.COLLECTION_FOLLOWING)
            .document(targetUid)

    override suspend fun followUser(targetUid: String): Result<Unit> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in."))
        if (targetUid.isBlank() || targetUid == uid) {
            return@withContext Result.failure(IllegalArgumentException("Can't follow this user."))
        }
        try {
            // serverTimestamp() resolves to request.time, which the rules require for a
            // new edge so the createdAt can't be backdated.
            followingDoc(uid, targetUid)
                .set(mapOf("createdAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(targetUid: String): Result<Unit> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in."))
        if (targetUid.isBlank()) return@withContext Result.success(Unit)
        try {
            followingDoc(uid, targetUid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowerIds(uid: String): List<String> =
        getEdgeIds(uid, Constants.COLLECTION_FOLLOWERS)

    override suspend fun getFollowingIds(uid: String): List<String> =
        getEdgeIds(uid, Constants.COLLECTION_FOLLOWING)

    /** Read a follow subcollection's doc ids (the counterpart uids), newest first, capped. */
    private suspend fun getEdgeIds(uid: String, subcollection: String): List<String> =
        withContext(ioDispatcher) {
            if (uid.isBlank()) return@withContext emptyList()
            val snapshot = usersCollection.document(uid)
                .collection(subcollection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(FOLLOW_LIST_LIMIT)
                .get()
                .await()
            snapshot.documents.map { it.id }
        }

    override fun observeFollowingIds(): Flow<List<String>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = usersCollection.document(uid)
            .collection(Constants.COLLECTION_FOLLOWING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { it.id } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    override fun observeIsFollowing(targetUid: String): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null || targetUid.isBlank()) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val listener = followingDoc(uid, targetUid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(false)
                return@addSnapshotListener
            }
            trySend(snapshot?.exists() == true)
        }
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)
}
