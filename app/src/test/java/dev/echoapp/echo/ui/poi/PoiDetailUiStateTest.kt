package dev.echoapp.echo.ui.poi

import dev.echoapp.echo.utils.Constants
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

    // --- Favorites ---

    @Test
    fun `favorited user out of range can post`() {
        val state = PoiDetailUiState(isGuest = false, distanceMeters = 50_000.0, isFavorited = true)
        assertFalse(state.withinRange)
        assertTrue(state.canPost)
    }

    @Test
    fun `favorited guest still cannot post`() {
        val state = PoiDetailUiState(isGuest = true, distanceMeters = 50_000.0, isFavorited = true)
        assertFalse(state.canPost)
    }

    @Test
    fun `can favorite when in range with a free slot`() {
        val state = PoiDetailUiState(isGuest = false, distanceMeters = 100.0, favoriteCount = 2)
        assertTrue(state.canFavorite)
    }

    @Test
    fun `cannot favorite when out of range`() {
        val state = PoiDetailUiState(isGuest = false, distanceMeters = 5_001.0, favoriteCount = 0)
        assertFalse(state.canFavorite)
    }

    @Test
    fun `cannot favorite when at the cap`() {
        val state = PoiDetailUiState(
            isGuest = false,
            distanceMeters = 100.0,
            favoriteCount = Constants.MAX_FAVORITE_POIS
        )
        assertTrue(state.atFavoriteCap)
        assertFalse(state.canFavorite)
    }

    @Test
    fun `cannot favorite a POI already favorited`() {
        val state = PoiDetailUiState(isGuest = false, distanceMeters = 100.0, isFavorited = true)
        assertFalse(state.canFavorite)
    }

    @Test
    fun `cannot unfavorite before the hold elapses`() {
        val now = 1_000_000_000_000L
        val state = PoiDetailUiState(isFavorited = true, favoritedAt = now)
        // One day in, with a 7-day hold, removal is still locked.
        val oneDayLater = now + 24L * 60 * 60 * 1000
        assertFalse(state.canUnfavorite(oneDayLater))
        assertTrue(state.holdRemainingMillis(oneDayLater) > 0)
    }

    @Test
    fun `can unfavorite once the hold elapses`() {
        val now = 1_000_000_000_000L
        val state = PoiDetailUiState(isFavorited = true, favoritedAt = now)
        val afterHold = now + Constants.FAVORITE_HOLD_MILLIS + 1
        assertTrue(state.canUnfavorite(afterHold))
        assertEquals(0L, state.holdRemainingMillis(afterHold))
    }
}
