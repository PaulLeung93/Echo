package dev.echoapp.echo.data.repository

import dev.echoapp.echo.data.entity.UserProfileEntity
import dev.echoapp.echo.di.ApplicationScope
import dev.echoapp.echo.di.IoDispatcher
import dev.echoapp.echo.domain.model.UserProfile
import dev.echoapp.echo.domain.repository.AuthProvider
import dev.echoapp.echo.domain.repository.ReauthCredential
import dev.echoapp.echo.domain.repository.UserRepository
import dev.echoapp.echo.utils.Constants
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope
) : UserRepository {

    private val usersCollection = firestore.collection(Constants.COLLECTION_USERS)
    private val usernamesCollection = firestore.collection(Constants.COLLECTION_USERNAMES)

    override suspend fun isUsernameAvailable(username: String): Result<Boolean> =
        withContext(ioDispatcher) {
            try {
                val snapshot = usernamesCollection.document(username.lowercase()).get().await()
                Result.success(!snapshot.exists())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun createProfile(
        firstName: String,
        lastName: String,
        username: String
    ): Result<UserProfile> = withContext(ioDispatcher) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in."))
        val uid = user.uid
        val handle = username.lowercase()

        try {
            val usernameRef = usernamesCollection.document(handle)
            val userRef = usersCollection.document(uid)

            firestore.runTransaction { txn ->
                if (txn.get(usernameRef).exists()) {
                    throw IllegalStateException("That username was just taken. Try another.")
                }
                txn.set(usernameRef, mapOf("uid" to uid))
                txn.set(
                    userRef,
                    mapOf(
                        "uid" to uid,
                        "username" to handle,
                        "firstName" to firstName.trim(),
                        "lastName" to lastName.trim(),
                        "bio" to "",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            // Best-effort: mirror the name onto the Firebase Auth displayName.
            runCatching {
                user.updateProfile(
                    userProfileChangeRequest {
                        displayName = listOf(firstName.trim(), lastName.trim())
                            .filter { it.isNotBlank() }.joinToString(" ")
                    }
                ).await()
            }

            Result.success(
                UserProfile(
                    uid = uid,
                    username = handle,
                    firstName = firstName.trim(),
                    lastName = lastName.trim()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun blockUser(blockedUid: String): Result<Unit> = withContext(ioDispatcher) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in."))
        if (blockedUid.isBlank() || blockedUid == user.uid) {
            return@withContext Result.failure(IllegalArgumentException("Can't block this user."))
        }
        try {
            usersCollection.document(user.uid)
                .update("blockedUserIds", FieldValue.arrayUnion(blockedUid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unblockUser(blockedUid: String): Result<Unit> = withContext(ioDispatcher) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in."))
        try {
            usersCollection.document(user.uid)
                .update("blockedUserIds", FieldValue.arrayRemove(blockedUid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProfilesByIds(uids: List<String>): Result<List<UserProfile>> =
        withContext(ioDispatcher) {
            if (uids.isEmpty()) return@withContext Result.success(emptyList())
            try {
                val profiles = uids.mapNotNull { uid ->
                    usersCollection.document(uid).get().await()
                        .toObject(UserProfileEntity::class.java)?.toDomain()
                }
                Result.success(profiles)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getCurrentUserProfile(): Result<UserProfile?> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext Result.success(null)
        try {
            val profile = usersCollection.document(uid).get().await()
                .toObject(UserProfileEntity::class.java)
                ?.toDomain()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        bio: String
    ): Result<Unit> = withContext(ioDispatcher) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in."))
        try {
            usersCollection.document(user.uid).update(
                mapOf(
                    "firstName" to firstName.trim(),
                    "lastName" to lastName.trim(),
                    "bio" to bio.trim()
                )
            ).await()
            runCatching {
                user.updateProfile(
                    userProfileChangeRequest {
                        displayName = listOf(firstName.trim(), lastName.trim())
                            .filter { it.isNotBlank() }.joinToString(" ")
                    }
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAvatar(imageBytes: ByteArray): Result<String> = withContext(ioDispatcher) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in."))
        try {
            // One file per user (overwritten on each change) so we never accumulate
            // orphaned avatars. The path is keyed on the uid, which the Storage rules
            // require the writer to own.
            val ref = storage.reference.child("avatars/${user.uid}.jpg")
            ref.putBytes(imageBytes).await()
            val url = ref.downloadUrl.await().toString()
            usersCollection.document(user.uid).update("photoUrl", url).await()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeAvatar(): Result<Unit> = withContext(ioDispatcher) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in."))
        try {
            // Best-effort delete of the Storage file — a missing object (e.g. the user
            // never had a photo, or it was already cleared) must not fail the removal.
            runCatching { storage.reference.child("avatars/${user.uid}.jpg").delete().await() }
            // Clearing to "" rather than deleting the field keeps the doc shape stable;
            // toDomain() maps blank → null, so the initials avatar takes over.
            usersCollection.document(user.uid).update("photoUrl", "").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeCurrentUserProfile(): Flow<UserProfile?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(UserProfileEntity::class.java)?.toDomain())
        }
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    /**
     * People hidden from the current user (those they blocked + those who blocked them),
     * shared across all collectors (map, feed, post-detail, POI-detail). Without sharing,
     * each of those view models opened its own pair of snapshot listeners on identical
     * data — one of them a collection query. `WhileSubscribed(5000)` keeps the listeners
     * warm across a quick screen switch; the upstream re-reads `auth.currentUser` when it
     * restarts after the timeout, so it picks up a new uid after a re-login.
     */
    private val sharedHiddenUserIds: Flow<Set<String>> = flow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            emit(emptySet())
            return@flow
        }

        // People I blocked (from my own profile doc).
        val blockedByMe: Flow<Set<String>> =
            observeCurrentUserProfile().map { it?.blockedUserIds?.toSet() ?: emptySet() }

        // People who blocked me (their profile lists my uid). Makes the block mutual:
        // once they block me, their content and mine disappear from each other.
        val blockedMe: Flow<Set<String>> = callbackFlow {
            val registration = usersCollection
                .whereArrayContains("blockedUserIds", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptySet())
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.documents?.map { it.id }?.toSet() ?: emptySet())
                }
            awaitClose { registration.remove() }
        }.flowOn(ioDispatcher)

        emitAll(combine(blockedByMe, blockedMe) { mine, theirs -> mine + theirs })
    }.shareIn(
        scope = appScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
    )

    override fun observeHiddenUserIds(): Flow<Set<String>> = sharedHiddenUserIds

    override fun currentAuthProvider(): AuthProvider {
        val user = auth.currentUser ?: return AuthProvider.NONE
        val providers = user.providerData.map { it.providerId }
        return when {
            providers.contains(GoogleAuthProvider.PROVIDER_ID) -> AuthProvider.GOOGLE
            providers.contains(EmailAuthProvider.PROVIDER_ID) -> AuthProvider.PASSWORD
            else -> AuthProvider.OTHER
        }
    }

    override suspend fun deleteAccount(reauth: ReauthCredential): Result<Unit> = withContext(ioDispatcher) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("You must be signed in."))
        try {
            // Re-authenticate FIRST. This satisfies Firebase's recent-login
            // requirement and validates the credential (wrong password, cancelled
            // Google flow, etc. throw here) — so nothing below runs unless we're
            // certain the auth deletion will be allowed. Destroying the profile
            // before a successful re-auth would orphan a half-deleted account.
            val credential = when (reauth) {
                is ReauthCredential.Password -> {
                    val email = user.email
                        ?: return@withContext Result.failure(IllegalStateException("Your account has no email to re-authenticate with."))
                    EmailAuthProvider.getCredential(email, reauth.password)
                }
                is ReauthCredential.Google -> GoogleAuthProvider.getCredential(reauth.idToken, null)
            }
            user.reauthenticate(credential).await()

            val uid = user.uid
            // Release the username reservation and delete the profile (best-effort
            // so a missing doc doesn't block the auth deletion).
            val handle = runCatching {
                usersCollection.document(uid).get().await().getString("username")
            }.getOrNull()
            if (!handle.isNullOrBlank()) {
                runCatching { usernamesCollection.document(handle).delete().await() }
            }
            runCatching { usersCollection.document(uid).delete().await() }
            // Delete the Firebase Auth account (now permitted — we just re-authed).
            user.delete().await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(IllegalStateException("Incorrect password. Please try again.", e))
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.failure(IllegalStateException("Please sign out and sign back in, then try again.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun UserProfileEntity.toDomain(): UserProfile? {
        if (uid.isBlank() || username.isBlank()) return null
        return UserProfile(
            uid = uid,
            username = username,
            firstName = firstName,
            lastName = lastName,
            bio = bio,
            photoUrl = photoUrl.ifBlank { null },
            blockedUserIds = blockedUserIds
        )
    }
}
