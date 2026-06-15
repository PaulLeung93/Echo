package com.example.echo.data.repository

import com.example.echo.data.entity.PoiEntity
import com.example.echo.data.mapper.PoiMapper
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.Poi
import com.example.echo.domain.repository.PoiRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val poiMapper: PoiMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PoiRepository {

    private val poisCollection = firestore.collection("pois")

    override fun getPois(): Flow<List<Poi>> = callbackFlow {
        val listener = poisCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val entities = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val name = doc.getString("name")
                    val type = doc.getString("type")
                    val geoPoint = doc.getGeoPoint("location")
                    val description = doc.getString("description") ?: ""
                    val commentCount = doc.getLong("commentCount")?.toInt() ?: 0
                    val imageUrl = doc.getString("imageUrl") ?: ""

                    if (name != null && type != null && geoPoint != null) {
                        PoiEntity(
                            id = doc.id,
                            name = name,
                            type = type,
                            location = geoPoint,
                            description = description,
                            commentCount = commentCount,
                            imageUrl = imageUrl
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            val pois = poiMapper.toDomainList(entities)
            trySend(pois)
        }

        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    override fun getPoisByTypes(types: Set<String>): Flow<List<Poi>> =
        getPois().map { pois ->
            pois.filter { poi -> poi.type in types }
        }

    override fun getPoiByIdFlow(poiId: String): Flow<Poi?> = callbackFlow {
        val listener = poisCollection.document(poiId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val name = snapshot.getString("name")
                val type = snapshot.getString("type")
                val geoPoint = snapshot.getGeoPoint("location")
                val description = snapshot.getString("description") ?: ""
                val commentCount = snapshot.getLong("commentCount")?.toInt() ?: 0
                val imageUrl = snapshot.getString("imageUrl") ?: ""

                if (name != null && type != null && geoPoint != null) {
                    val entity = PoiEntity(
                        id = snapshot.id,
                        name = name,
                        type = type,
                        location = geoPoint,
                        description = description,
                        commentCount = commentCount,
                        imageUrl = imageUrl
                    )
                    trySend(poiMapper.toDomain(entity))
                } else {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)
}