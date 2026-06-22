package dev.echoapp.echo.ui.maps

import com.google.android.gms.maps.CameraUpdate
import com.google.maps.android.compose.CameraPositionState
import kotlin.coroutines.cancellation.CancellationException

/**
 * Animate the camera, swallowing the "Camera stopped during a cancellation" exception
 * that maps-compose throws when an in-flight animation is interrupted (e.g. the screen
 * is left, or the app is backgrounded mid-animation). That exception is otherwise
 * uncaught and crashes the app. Genuine coroutine cancellation is rethrown so
 * structured concurrency keeps working.
 */
internal suspend fun CameraPositionState.animateSafely(update: CameraUpdate) {
    try {
        animate(update)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Interrupted camera animation — non-fatal, ignore.
    }
}
