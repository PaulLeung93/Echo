package dev.echoapp.echo.data

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

/** Max time to wait for a Firestore write before treating it as failed. */
const val WRITE_TIMEOUT_MS = 10_000L

/**
 * Runs a Firestore write [block] with a timeout.
 *
 * Firestore retries failing writes (lost network, expired/blocked auth token,
 * etc.) **indefinitely**, so a bare `.await()` can hang forever — leaving the UI
 * spinning with no feedback. Wrapping it bounds the wait and surfaces a clear,
 * retryable error instead. (The underlying write may still sync later once
 * connectivity/auth recovers; this only stops us blocking the UI on it.)
 */
suspend fun <T> withWriteTimeout(block: suspend () -> T): T =
    try {
        withTimeout(WRITE_TIMEOUT_MS) { block() }
    } catch (e: TimeoutCancellationException) {
        throw IOException("Couldn't reach the server. Check your connection and try again.")
    }
