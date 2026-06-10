package com.example.echo.utils

import com.example.echo.domain.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun `distance between identical coordinates is zero`() {
        val p = Coordinates(40.7128, -74.0060)
        assertEquals(0.0, distanceMeters(p, p), 0.001)
    }

    @Test
    fun `one degree of latitude is about 111 km`() {
        val a = Coordinates(0.0, 0.0)
        val b = Coordinates(1.0, 0.0)
        // ~111,195 m along a meridian; allow 100 m tolerance
        assertEquals(111_195.0, distanceMeters(a, b), 100.0)
    }

    @Test
    fun `5 km north is approximately the proximity radius`() {
        val origin = Coordinates(40.7128, -74.0060)
        // ~5 km north in latitude degrees
        val fiveKmNorth = Coordinates(40.7128 + 5000.0 / 111_195.0, -74.0060)
        val d = distanceMeters(origin, fiveKmNorth)
        assertEquals(5000.0, d, 25.0)
        assertTrue(d <= Constants.PROXIMITY_RADIUS_METERS)
    }

    @Test
    fun `distance is symmetric`() {
        val a = Coordinates(34.0522, -118.2437)
        val b = Coordinates(40.7128, -74.0060)
        assertEquals(distanceMeters(a, b), distanceMeters(b, a), 0.001)
    }
}
