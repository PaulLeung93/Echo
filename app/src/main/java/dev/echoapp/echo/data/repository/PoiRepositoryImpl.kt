package dev.echoapp.echo.data.repository

import dev.echoapp.echo.data.entity.PoiEntity
import dev.echoapp.echo.data.mapper.PoiMapper
import dev.echoapp.echo.data.preferences.UserPreferencesRepository
import dev.echoapp.echo.di.IoDispatcher
import dev.echoapp.echo.domain.model.Poi
import dev.echoapp.echo.domain.repository.PoiRepository
import dev.echoapp.echo.utils.Constants
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val poiMapper: PoiMapper,
    private val preferences: UserPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PoiRepository {

    private val poisCollection = firestore.collection("pois")

    /**
     * Process-lifetime cache so the map list and the type filter share a single
     * fetch and never re-read POIs from Firestore within a session.
     */
    private val cache = MutableStateFlow<List<Poi>?>(null)
    private val loadMutex = Mutex()

    /**
     * POIs are admin-curated reference data, so instead of a live snapshot listener
     * (which re-read the whole collection every session and held an open connection)
     * we serve a cache-first list: the on-device Firestore cache costs **0 billed
     * reads** and survives restarts, and we only hit the server when the cache is
     * empty (first run on a device) or the [Constants.POIS_CACHE_TTL_MS] window has
     * elapsed. The flow stays open and re-emits if a TTL sync refreshes the cache.
     */
    override fun getPois(): Flow<List<Poi>> = flow {
        ensureLoaded()
        emitAll(cache.filterNotNull())
    }.flowOn(ioDispatcher)

    private suspend fun ensureLoaded() = loadMutex.withLock {
        val now = System.currentTimeMillis()
        val stale = now - preferences.poisLastSync.first() > Constants.POIS_CACHE_TTL_MS

        // Already have a fresh in-memory copy this process → no Firestore touch.
        if (cache.value != null && !stale) return@withLock

        val snapshot = when {
            // TTL elapsed → reconcile from the server (one billed batch); fall back to
            // the disk cache if we're offline so the map still shows something.
            stale -> runCatching { poisCollection.get(Source.SERVER).await() }
                .onSuccess { preferences.setPoisLastSync(now) }
                .getOrElse { poisCollection.get(Source.CACHE).await() }

            // Fresh per the TTL but not yet loaded this process → serve the disk cache
            // for free; only the very first run on a device (empty cache) pays a read.
            else -> runCatching { poisCollection.get(Source.CACHE).await() }
                .getOrNull()
                ?.takeIf { !it.isEmpty }
                ?: poisCollection.get(Source.SERVER).await()
                    .also { preferences.setPoisLastSync(now) }
        }
        cache.value = parsePois(snapshot)
    }

    private fun parsePois(snapshot: QuerySnapshot): List<Poi> {
        val entities = snapshot.documents.mapNotNull(::toPoiEntity)
        return poiMapper.toDomainList(entities)
    }

    private fun toPoiEntity(doc: DocumentSnapshot): PoiEntity? = try {
        val name = doc.getString("name")
        val type = doc.getString("type")
        val geoPoint = doc.getGeoPoint("location")
        if (name != null && type != null && geoPoint != null) {
            PoiEntity(
                id = doc.id,
                name = name,
                type = type,
                location = geoPoint,
                description = doc.getString("description") ?: "",
                commentCount = doc.getLong("commentCount")?.toInt() ?: 0,
                imageUrl = doc.getString("imageUrl") ?: ""
            )
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

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