package com.example.echo.ui.poi

import com.example.echo.domain.model.Comment
import com.example.echo.domain.model.Poi

/**
 * UI state for the POI detail screen.
 *
 * Commenting is proximity-gated: only signed-in users physically within
 * [com.example.echo.utils.Constants.PROXIMITY_RADIUS_METERS] of the POI may comment.
 * (Client-side gating only — true enforcement lives in Firestore rules.)
 */
data class PoiDetailUiState(
    val poi: Poi? = null,
    val comments: List<Comment> = emptyList(),
    /** Stable uid of the signed-in user; used for non-spoofable comment ownership. */
    val currentUserId: String? = null,
    val currentUserEmail: String? = null,
    val isLoading: Boolean = false,
    /** Terminal load error (POI failed to load). Transient action errors use uiEvent. */
    val error: String? = null,

    // --- Commenting eligibility ---
    val isGuest: Boolean = true,
    /** True once a location fix has been attempted and resolved (success or failure). */
    val locationChecked: Boolean = false,
    /** Distance from the user to this POI in meters, or null if location is unavailable. */
    val distanceMeters: Double? = null
) {
    /** Whether the user is currently within the proximity radius of the POI. */
    val withinRange: Boolean
        get() = distanceMeters != null &&
            distanceMeters <= com.example.echo.utils.Constants.PROXIMITY_RADIUS_METERS

    /** Whether the comment input should be enabled for this user. */
    val canComment: Boolean
        get() = !isGuest && withinRange
}
