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
                android.util.Log.e("PoiRepository", "Firestore Listener FAILED", error)
                close(error)
                return@addSnapshotListener
            }
// In PoiRepositoryImpl.kt inside the listener

            val source = if (snapshot != null && snapshot.metadata.isFromCache) "LOCAL CACHE" else "SERVER"
            android.util.Log.d("PoiRepository", "Data fetched from: $source")

            val docCount = snapshot?.documents?.size ?: 0
            android.util.Log.d("PoiRepository", "Fetched $docCount documents from Firestore")

            val entities = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val name = doc.getString("name")
                    val type = doc.getString("type")
                    val geoPoint = doc.getGeoPoint("location")
                    val description = doc.getString("description") ?: ""

                    if (name != null && type != null && geoPoint != null) {
                        PoiEntity(
                            id = doc.id,
                            name = name,
                            type = type,
                            location = geoPoint,
                            description = description
                        )
                    } else {
                        android.util.Log.w("PoiRepository", "Missing fields for POI: ${doc.id}")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PoiRepository", "Error parsing POI: ${doc.id}", e)
                    null
                }
            } ?: emptyList()

            // --- NEW LOGGING BLOCK ---
            android.util.Log.d("PoiRepository", "--- START POI LIST ---")
            entities.forEach { poi ->
                android.util.Log.d("PoiRepository", "POI: ${poi.name} | Type: ${poi.type} | Loc: ${poi.location.latitude},${poi.location.longitude}")
            }
            android.util.Log.d("PoiRepository", "--- END POI LIST ---")
            // -------------------------

            val pois = poiMapper.toDomainList(entities)
            trySend(pois)
        }

        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    override fun getPoisByTypes(types: Set<String>): Flow<List<Poi>> =
        getPois().map { pois ->
            pois.filter { poi -> poi.type in types }
        }

    override suspend fun getPoiById(poiId: String): Poi? = withContext(ioDispatcher) {
        try {
            val doc = poisCollection.document(poiId).get().await()
            val name = doc.getString("name")
            val type = doc.getString("type")
            val geoPoint = doc.getGeoPoint("location")
            val description = doc.getString("description") ?: ""

            if (name != null && type != null && geoPoint != null) {
                val entity = PoiEntity(
                    id = doc.id,
                    name = name,
                    type = type,
                    location = geoPoint,
                    description = description
                )
                poiMapper.toDomain(entity)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}