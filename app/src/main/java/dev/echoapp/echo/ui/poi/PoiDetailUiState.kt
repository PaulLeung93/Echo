package dev.echoapp.echo.ui.poi

import dev.echoapp.echo.domain.model.Poi
import dev.echoapp.echo.domain.model.Post
import dev.echoapp.echo.utils.Constants

/**
 * UI state for the POI detail screen.
 *
 * A POI is a thread of posts: signed-in users physically within
 * [dev.echoapp.echo.utils.Constants.PROXIMITY_RADIUS_METERS] of the POI may add a post.
 * Users may also *favorite* a POI (while in range) to post there regardless of distance.
 * (Client-side gating only — true enforcement lives in Firestore rules.)
 */
data class PoiDetailUiState(
    val poi: Poi? = null,
    /** Posts in this POI's thread, ordered per [sortDescending]. */
    val posts: List<Post> = emptyList(),
    /** Stable uid of the signed-in user; used for non-spoofable post ownership. */
    val currentUserId: String? = null,
    val isLoading: Boolean = false,
    /** Terminal load error (POI failed to load). Transient action errors use uiEvent. */
    val error: String? = null,

    // --- Posting eligibility ---
    val isGuest: Boolean = true,
    /** True once a location fix has been attempted and resolved (success or failure). */
    val locationChecked: Boolean = false,
    /** Distance from the user to this POI in meters, or null if location is unavailable. */
    val distanceMeters: Double? = null,

    // --- Favorites ---
    /** Whether this POI is one of the user's favorites. */
    val isFavorited: Boolean = false,
    /** Epoch millis this POI was favorited, or null if it isn't a favorite. */
    val favoritedAt: Long? = null,
    /** How many favorite slots the user is currently using (across all POIs). */
    val favoriteCount: Int = 0,

    /** Thread sort: true = newest-first (default), false = oldest-first. */
    val sortDescending: Boolean = true
) {
    /** Whether the user is currently within the proximity radius of the POI. */
    val withinRange: Boolean
        get() = distanceMeters != null &&
            distanceMeters <= Constants.PROXIMITY_RADIUS_METERS

    /**
     * Whether the user may add a post to this POI's thread: a real (non-guest) account
     * that is either physically in range *or* has favorited this POI.
     */
    val canPost: Boolean
        get() = !isGuest && (withinRange || isFavorited)

    /** True when the user has already filled every favorite slot. */
    val atFavoriteCap: Boolean
        get() = favoriteCount >= Constants.MAX_FAVORITE_POIS

    /**
     * Whether the user may favorite this POI right now: a real account, not already
     * favorited, within range (you can only favorite a place you're at), and with a
     * free slot.
     */
    val canFavorite: Boolean
        get() = !isGuest && !isFavorited && withinRange && !atFavoriteCap

    /**
     * Millis remaining on this favorite's removal hold given [now], or 0 if it isn't a
     * favorite or the hold has elapsed. Takes [now] explicitly so it stays pure/testable.
     */
    fun holdRemainingMillis(now: Long): Long {
        val since = favoritedAt ?: return 0L
        return (since + Constants.FAVORITE_HOLD_MILLIS - now).coerceAtLeast(0L)
    }

    /** Whether the user may remove this favorite at [now] (favorited and hold elapsed). */
    fun canUnfavorite(now: Long): Boolean = isFavorited && holdRemainingMillis(now) <= 0L
}
