package dev.echoapp.echo.data.mapper

import dev.echoapp.echo.data.entity.PoiEntity
import dev.echoapp.echo.domain.model.Poi
import javax.inject.Inject

/**
 * Mapper for converting between PoiEntity (data layer) and Poi (domain layer).
 * Handles GeoPoint to latitude/longitude conversion.
 */
class PoiMapper @Inject constructor() {
    
    /**
     * Convert PoiEntity to domain Poi.
     * @param entity The Firestore entity to convert.
     * @return The domain Poi model.
     */
    fun toDomain(entity: PoiEntity): Poi {
        return Poi(
            id = entity.id,
            name = entity.name,
            type = entity.type,
            latitude = entity.location.latitude,
            longitude = entity.location.longitude,
            description = entity.description,
            postCount = entity.postCount,
            imageUrl = entity.imageUrl.ifBlank { null },
            lastPostAt = entity.lastPostAt
        )
    }

    /**
     * Convert a list of PoiEntities to domain Pois.
     * @param entities The list of Firestore entities.
     * @return List of domain Poi models.
     */
    fun toDomainList(entities: List<PoiEntity>): List<Poi> {
        return entities.map { toDomain(it) }
    }
}
