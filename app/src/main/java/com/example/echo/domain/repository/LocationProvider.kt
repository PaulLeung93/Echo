package com.example.echo.domain.repository

import com.example.echo.domain.model.Coordinates

/**
 * Abstraction over the device's location services.
 * Keeps the domain/presentation layers free of platform location APIs.
 */
interface LocationProvider {

    /**
     * Get the device's current coordinates.
     * @return the current [Coordinates], or null if location is unavailable
     *         (e.g. permission not granted or no fix obtainable).
     */
    suspend fun getCurrentCoordinates(): Coordinates?
}
