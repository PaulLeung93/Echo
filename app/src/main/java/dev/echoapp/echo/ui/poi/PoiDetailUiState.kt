package dev.echoapp.echo.ui.poi

import dev.echoapp.echo.domain.model.Poi
import dev.echoapp.echo.domain.model.Post

/**
 * UI state for the POI detail screen.
 *
 * A POI is a thread of posts: signed-in users physically within
 * [dev.echoapp.echo.utils.Constants.PROXIMITY_RADIUS_METERS] of the POI may add a post.
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

    /** Thread sort: true = newest-first (default), false = oldest-first. */
    val sortDescending: Boolean = true
) {
    /** Whether the user is currently within the proximity radius of the POI. */
    val withinRange: Boolean
        get() = distanceMeters != null &&
            distanceMeters <= dev.echoapp.echo.utils.Constants.PROXIMITY_RADIUS_METERS

    /** Whether the user may add a post to this POI's thread. */
    val canPost: Boolean
        get() = !isGuest && withinRange
}
