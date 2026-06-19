package com.example.echo.data.repository

import com.example.echo.data.entity.UserProfileEntity
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.UserProfile
import com.example.echo.domain.repository.AuthProvider
import com.example.echo.domain.repository.ReauthCredential
import com.example.echo.domain.repository.UserRepository
import com.example.echo.utils.Constants
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
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

    override fun observeHiddenUserIds(): Flow<Set<String>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptySet())

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

        return combine(blockedByMe, blockedMe) { mine, theirs -> mine + theirs }
    }

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
            blockedUserIds = blockedUserIds
        )
    }
}
