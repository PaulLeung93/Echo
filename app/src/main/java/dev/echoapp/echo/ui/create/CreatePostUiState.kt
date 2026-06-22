package dev.echoapp.echo.ui.create

/**
 * UI State for the Create Post screen.
 */
data class CreatePostUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    // --- Location ---
    /** Whether the user wants to attach their location to this post. */
    val includeLocation: Boolean = false,
    /** Resolving the location fix after the user opted in. */
    val isLocationLoading: Boolean = false,
    /** A fix was requested but couldn't be obtained (no permission / no fix). */
    val locationUnavailable: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // --- POI post mode ---
    /** Set when composing a post for a POI thread; location is snapped to the POI. */
    val poiId: String? = null,
    val poiName: String? = null
) {
    /** True when this screen is composing a post attached to a POI. */
    val isPoiPost: Boolean get() = !poiId.isNullOrBlank()
}
