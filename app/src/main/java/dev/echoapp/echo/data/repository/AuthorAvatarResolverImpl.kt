package dev.echoapp.echo.data.repository

import dev.echoapp.echo.di.ApplicationScope
import dev.echoapp.echo.di.IoDispatcher
import dev.echoapp.echo.domain.repository.AuthorAvatarResolver
import dev.echoapp.echo.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves authors' current avatars via per-uid Firestore snapshot listeners, shared
 * across every card that shows the same author. One listener per distinct author is
 * kept warm while observed ([SharingStarted.WhileSubscribed]); the flow is cached per
 * uid so the feed, post detail, comments, and profile all reuse it. Because it's a
 * live read of `users/{uid}.photoUrl`, changing a photo updates the avatar everywhere
 * it appears, old content included.
 */
@Singleton
class AuthorAvatarResolverImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthorAvatarResolver {

    private val usersCollection = firestore.collection(Constants.COLLECTION_USERS)
    private val flows = ConcurrentHashMap<String, Flow<String?>>()

    override fun photoUrl(authorId: String): Flow<String?> {
        if (authorId.isBlank()) return flowOf(null)
        return flows.getOrPut(authorId) {
            callbackFlow {
                val registration = usersCollection.document(authorId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.getString("photoUrl")?.ifBlank { null })
                    }
                awaitClose { registration.remove() }
            }
                .flowOn(ioDispatcher)
                .distinctUntilChanged()
                .shareIn(appScope, SharingStarted.WhileSubscribed(5_000), replay = 1)
        }
    }
}
