package dev.echoapp.echo.domain.repository

import dev.echoapp.echo.domain.model.Poi
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Point of Interest operations.
 * Defines the contract for POI-related data access.
 */
interface PoiRepository {
    
    /**
     * Get all Points of Interest.
     * @return Flow of POIs for reactive updates.
     */
    fun getPois(): Flow<List<Poi>>
    
    /**
     * Get POIs filtered by type.
     * @param types Set of POI types to include.
     * @return Flow of filtered POIs.
     */
    fun getPoisByTypes(types: Set<String>): Flow<List<Poi>>
    
    /**
     * Get a single POI by ID with real-time updates.
     * @param poiId The ID of the POI.
     * @return Flow of the POI, or null if not found.
     */
    fun getPoiByIdFlow(poiId: String): Flow<Poi?>

    /**
     * Locally adjust a cached POI's denormalized `postCount` by [delta] so the map
     * (which serves POIs from a session-long, TTL-gated cache) reflects an in-session
     * add/delete immediately, without re-reading the collection. The authoritative
     * count still lives on the POI document in Firestore; this only keeps the cache
     * in step until the next TTL sync. No-op if the POI isn't currently cached.
     */
    fun adjustCachedPostCount(poiId: String, delta: Int)
}
