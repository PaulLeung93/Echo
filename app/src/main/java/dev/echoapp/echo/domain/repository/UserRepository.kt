package dev.echoapp.echo.domain.repository

import dev.echoapp.echo.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * A fresh credential used to re-authenticate the user immediately before a
 * sensitive action (account deletion). Firebase requires a recent login, and
 * re-auth must succeed *before* any data is destroyed so a stale session can't
 * leave a half-deleted account.
 */
sealed interface ReauthCredential {
    /** Email/password accounts: the user re-enters their password. */
    data class Password(val password: String) : ReauthCredential

    /** Google accounts: a freshly obtained Google ID token. */
    data class Google(val idToken: String) : ReauthCredential
}

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

    /** Update the current user's editable profile fields (name + bio). */
    suspend fun updateProfile(firstName: String, lastName: String, bio: String): Result<Unit>

    /**
     * Upload [imageBytes] (a compressed JPEG) as the current user's avatar to Cloud
     * Storage, then persist the resulting download URL to their profile's `photoUrl`.
     * @return the download URL on success.
     */
    suspend fun updateAvatar(imageBytes: ByteArray): Result<String>

    /**
     * Remove the current user's avatar: best-effort delete of the Storage file, then
     * clear `photoUrl` on their profile so they fall back to the initials avatar.
     */
    suspend fun removeAvatar(): Result<Unit>

    /** Add [blockedUid] to the current user's blocked list (idempotent). */
    suspend fun blockUser(blockedUid: String): Result<Unit>

    /** Remove [blockedUid] from the current user's blocked list (idempotent). */
    suspend fun unblockUser(blockedUid: String): Result<Unit>

    /**
     * Favorite [poiId] for the current user (stamped server-side). Lets them post to
     * that POI beyond the proximity radius. The cap + hold are enforced by the rules;
     * callers should pre-check eligibility for UX. Idempotent if already favorited.
     */
    suspend fun favoritePoi(poiId: String): Result<Unit>

    /**
     * Remove [poiId] from the current user's favorites. The rules reject this until the
     * slot's hold has elapsed, so callers should pre-check. Idempotent if not favorited.
     */
    suspend fun unfavoritePoi(poiId: String): Result<Unit>

    /** Observe the current user's favorited POIs (poiId -> favoritedAt millis). */
    fun observeFavoritePois(): Flow<Map<String, Long>>

    /** Resolve profiles for [uids] (e.g. to show usernames in the blocked list). */
    suspend fun getProfilesByIds(uids: List<String>): Result<List<UserProfile>>

    /**
     * Author uids whose content the current user should NOT see — the union of
     * people they blocked *and* people who blocked them (block is mutual). Empty
     * for guests / signed-out users. Used to filter feed/map/comments.
     */
    fun observeHiddenUserIds(): Flow<Set<String>>

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

    /**
     * Observe any user's public profile by [uid] (null when it doesn't exist or on a
     * read error). Used by the public profile screen to show someone else's info.
     */
    fun observeProfileById(uid: String): Flow<UserProfile?>

    /** The sign-in provider of the current user (for choosing the re-auth flow). */
    fun currentAuthProvider(): AuthProvider

    /**
     * Permanently delete the current user. Re-authenticates with [reauth] first;
     * only if that succeeds does it release the username, delete the profile doc,
     * and delete the Firebase Auth account — so a failed/stale re-auth never
     * leaves a half-deleted account.
     */
    suspend fun deleteAccount(reauth: ReauthCredential): Result<Unit>
}

/** Which sign-in method the current account uses. */
enum class AuthProvider { PASSWORD, GOOGLE, OTHER, NONE }
