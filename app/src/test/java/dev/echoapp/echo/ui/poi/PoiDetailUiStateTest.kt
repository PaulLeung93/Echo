package dev.echoapp.echo.ui.poi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoiDetailUiStateTest {

    @Test
    fun `signed-in user within radius can post`() {
        val state = PoiDetailUiState(isGuest = false, distanceMeters = 4_999.0)
        assertTrue(state.withinRange)
        assertTrue(state.canPost)
    }

    @Test
    fun `signed-in user outside radius cannot post`() {
        val state = PoiDetailUiState(isGuest = false, distanceMeters = 5_001.0)
        assertFalse(state.withinRange)
        assertFalse(state.canPost)
    }

    @Test
    fun `guest within radius cannot post`() {
        val state = PoiDetailUiState(isGuest = true, distanceMeters = 100.0)
        assertTrue(state.withinRange)
        assertFalse(state.canPost)
    }

    @Test
    fun `unknown location cannot post`() {
        val state = PoiDetailUiState(isGuest = false, distanceMeters = null)
        assertFalse(state.withinRange)
        assertFalse(state.canPost)
    }

    @Test
    fun `default sort is newest-first`() {
        assertTrue(PoiDetailUiState().sortDescending)
    }
}
