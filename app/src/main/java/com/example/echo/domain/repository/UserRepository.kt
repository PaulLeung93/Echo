package com.example.echo.domain.repository

import com.example.echo.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository for user profiles (`users/{uid}`) and the username reservation
 * index (`usernames/{handle}`) that enforces case-insensitive uniqueness.
 */
interface UserRepository {

    /** True if [username] (lowercased) is not yet claimed. */
    suspend fun isUsernameAvailable(username: String): Result<Boolean>

    /**
     * Atomically claim [username] and write the profile for the current user
     * (transaction: fails if the handle was taken in a race). Also sets the
     * Firebase Auth displayName to "First Last".
     */
    suspend fun createProfile(firstName: String, lastName: String, username: String): Result<UserProfile>

    /**
     * One-shot read of the current user's profile.
     * - `success(profile)` — the profile exists.
     * - `success(null)` — confirmed: no profile (or not signed in).
     * - `failure(e)` — couldn't read (network/transient); distinct from "absent"
     *   so callers don't mistake a read error for a missing profile.
     */
    suspend fun getCurrentUserProfile(): Result<UserProfile?>

    /** Observe the current user's profile (null when signed out or no profile yet). */
    fun observeCurrentUserProfile(): Flow<UserProfile?>
}
