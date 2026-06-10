package com.example.echo.data.mapper

import com.example.echo.data.entity.PoiEntity
import com.example.echo.domain.model.Poi
import com.google.firebase.firestore.GeoPoint
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
            commentCount = entity.commentCount
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
    
    /**
     * Convert domain Poi to entity.
     * @param poi The domain model.
     * @return The Firestore entity.
     */
    fun toEntity(poi: Poi): PoiEntity {
        return PoiEntity(
            id = poi.id,
            name = poi.name,
            type = poi.type,
            location = GeoPoint(poi.latitude, poi.longitude),
            description = poi.description,
            commentCount = poi.commentCount
        )
    }
}
