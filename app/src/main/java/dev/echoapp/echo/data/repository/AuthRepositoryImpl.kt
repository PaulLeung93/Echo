package dev.echoapp.echo.data.repository

import dev.echoapp.echo.data.mapper.UserMapper
import dev.echoapp.echo.di.IoDispatcher
import dev.echoapp.echo.domain.model.User
import dev.echoapp.echo.domain.repository.AuthRepository
import dev.echoapp.echo.utils.mapFirebaseErrorMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Firebase Authentication.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userMapper: UserMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {
    
    override fun getCurrentUser(): User? = userMapper.toDomain(auth.currentUser)

    override fun authState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(userMapper.toDomain(firebaseAuth.currentUser))
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun isSignedIn(): Boolean = auth.currentUser != null
    
    override fun isAuthenticated(): Boolean = auth.currentUser?.isAnonymous == false
    
    override suspend fun signInWithEmail(email: String, password: String): Result<User> = 
        withContext(ioDispatcher) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = userMapper.toDomain(result.user)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Authentication failed"))
                }
            } catch (e: Exception) {
                Result.failure(Exception(mapFirebaseErrorMessage(e.localizedMessage), e))
            }
        }

    override suspend fun signUpWithEmail(email: String, password: String): Result<User> =
        withContext(ioDispatcher) {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = userMapper.toDomain(result.user)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Registration failed"))
                }
            } catch (e: Exception) {
                Result.failure(Exception(mapFirebaseErrorMessage(e.localizedMessage), e))
            }
        }
    
    override suspend fun signInAsGuest(): Result<User> = withContext(ioDispatcher) {
        try {
            val result = auth.signInAnonymously().await()
            val user = userMapper.toDomain(result.user)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Anonymous sign-in failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseErrorMessage(e.localizedMessage), e))
        }
    }
    
    override suspend fun signInWithGoogle(idToken: String): Result<User> = 
        withContext(ioDispatcher) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = userMapper.toDomain(result.user)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Google sign-in failed"))
                }
            } catch (e: Exception) {
                Result.failure(Exception(mapFirebaseErrorMessage(e.localizedMessage), e))
            }
        }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                auth.sendPasswordResetEmail(email).await()
                Result.success(Unit)
            } catch (e: FirebaseAuthInvalidUserException) {
                // No account for this email. Report success anyway so the response
                // is identical whether or not the address is registered — otherwise
                // the screen leaks which emails have accounts (enumeration).
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception(mapFirebaseErrorMessage(e.localizedMessage), e))
            }
        }
    
    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun deleteCurrentAuthAccount(): Result<Unit> = withContext(ioDispatcher) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("No signed-in account to delete."))
        try {
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
