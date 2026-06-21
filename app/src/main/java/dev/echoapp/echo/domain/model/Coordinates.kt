package dev.echoapp.echo.domain.model

/**
 * Domain model representing a geographic coordinate pair.
 * Pure Kotlin representation with no platform (android.location) dependency.
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)
