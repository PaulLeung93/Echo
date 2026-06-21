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
}
