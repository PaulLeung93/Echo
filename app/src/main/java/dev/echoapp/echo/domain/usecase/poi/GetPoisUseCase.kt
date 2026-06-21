package dev.echoapp.echo.domain.usecase.poi

import dev.echoapp.echo.domain.model.Poi
import dev.echoapp.echo.domain.repository.PoiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting Points of Interest.
 */
class GetPoisUseCase @Inject constructor(
    private val poiRepository: PoiRepository
) {
    /**
     * Get all POIs.
     * @return Flow of POIs for reactive updates.
     */
    operator fun invoke(): Flow<List<Poi>> = poiRepository.getPois()
    
    /**
     * Get POIs filtered by type.
     * @param types Set of POI types to include.
     * @return Flow of filtered POIs.
     */
    fun byTypes(types: Set<String>): Flow<List<Poi>> = poiRepository.getPoisByTypes(types)
}
