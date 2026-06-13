package com.example.echo.utils

import com.example.echo.domain.model.Coordinates
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Great-circle distance between two coordinates in meters (Haversine formula).
 * Pure Kotlin — no platform dependency — so it is unit-testable.
 */
fun distanceMeters(a: Coordinates, b: Coordinates): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)

    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1 - h))
}

/** Human-friendly distance label, e.g. "Here", "250 m away", or "1.2 km away". */
fun formatDistance(meters: Double): String = when {
    meters < 15 -> "Here" // within GPS jitter — reads better than "0 m away"
    meters < 1000 -> "${meters.roundToInt()} m away"
    else -> "%.1f km away".format(meters / 1000)
}
