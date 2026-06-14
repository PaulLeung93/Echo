package com.example.echo.data.repository

import com.example.echo.data.entity.UserProfileEntity
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.UserProfile
import com.example.echo.domain.repository.UserRepository
import com.example.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
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
        val email = user.email ?: ""
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
                        "email" to email,
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
                    lastName = lastName.trim(),
                    email = email
                )
            )
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

    private fun UserProfileEntity.toDomain(): UserProfile? {
        if (uid.isBlank() || username.isBlank()) return null
        return UserProfile(
            uid = uid,
            username = username,
            firstName = firstName,
            lastName = lastName,
            email = email
        )
    }
}
