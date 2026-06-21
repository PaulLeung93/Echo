package dev.echoapp.echo.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A request to open the map centered on a post, with that post's bottom card shown. */
data class MapFocusRequest(
    val postId: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * App-scoped hand-off for "open this post on the map". The feed writes a request and
 * then navigates to the Map tab; the map reads it to center the camera and select the
 * post.
 *
 * This is deliberately *not* passed as a navigation argument. The Map is a bottom-nav
 * destination, so routing to it with a one-off argument route bypasses — and desyncs —
 * the bottom nav's saveState/restoreState back stack (which left the Feed tab unable to
 * return). Going through this singleton lets the badge navigate to the Map tab exactly
 * like the bottom bar does, keeping the back stack consistent.
 */
@Singleton
class MapFocusManager @Inject constructor() {
    private val _request = MutableStateFlow<MapFocusRequest?>(null)
    val request: StateFlow<MapFocusRequest?> = _request.asStateFlow()

    fun request(postId: String, latitude: Double, longitude: Double) {
        _request.value = MapFocusRequest(postId, latitude, longitude)
    }

    /** Clear the pending request once the map has acted on it. */
    fun consume() {
        _request.value = null
    }
}
