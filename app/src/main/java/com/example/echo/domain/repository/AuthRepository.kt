package com.example.echo.domain.repository

import com.example.echo.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Authentication operations.
 * Defines the contract for auth-related data access.
 */
interface AuthRepository {

    /**
     * Get the currently signed-in user, or null if not signed in.
     */
    fun getCurrentUser(): User?

    /**
     * Observe the signed-in user reactively. Emits the current user immediately,
     * then again whenever auth state changes — including when Firebase signs the
     * user out on an expired/revoked session. Null when signed out.
     */
    fun authState(): Flow<User?>
    
    /**
     * Check if a user is currently signed in.
     */
    fun isSignedIn(): Boolean
    
    /**
     * Check if the current user is authenticated (not anonymous).
     */
    fun isAuthenticated(): Boolean
    
    /**
     * Sign in with email and password.
     * @param email The user's email.
     * @param password The user's password.
     * @return Result containing User on success or exception on failure.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    
    /**
     * Sign up with email and password.
     * @param email The user's email.
     * @param password The user's password.
     * @return Result containing User on success or exception on failure.
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<User>
    
    /**
     * Sign in anonymously as a guest.
     * @return Result containing User on success or exception on failure.
     */
    suspend fun signInAsGuest(): Result<User>
    
    /**
     * Sign in with Google credential.
     * @param idToken The Google ID token.
     * @return Result containing User on success or exception on failure.
     */
    suspend fun signInWithGoogle(idToken: String): Result<User>
    
    /**
     * Send a password reset email.
     * @param email The email address to send reset link to.
     * @return Result indicating success or failure.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    
    /**
     * Sign out the current user.
     */
    suspend fun signOut()

    /**
     * Delete the current Firebase Auth account directly, without re-authentication.
     * Intended for discarding a *freshly created* account that never completed
     * profile setup (the recent sign-in satisfies Firebase's recency check). May
     * fail with a recent-login-required error for an older session — callers
     * should fall back to [signOut] in that case. Account-deletion that releases
     * a username + profile and re-auths first lives in
     * [com.example.echo.domain.repository.UserRepository.deleteAccount].
     */
    suspend fun deleteCurrentAuthAccount(): Result<Unit>
}
