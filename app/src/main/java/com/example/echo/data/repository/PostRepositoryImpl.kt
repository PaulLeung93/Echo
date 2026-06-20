package com.example.echo.data.repository

import androidx.room.withTransaction
import com.example.echo.data.entity.PostEntity
import com.example.echo.data.local.EchoDatabase
import com.example.echo.data.local.PostDao
import com.example.echo.data.local.toCached
import com.example.echo.data.local.toDomain
import com.example.echo.data.mapper.PostMapper
import com.example.echo.data.withWriteTimeout
import com.example.echo.di.ApplicationScope
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.Post
import com.example.echo.domain.repository.PostRepository
import com.example.echo.domain.repository.UserRepository
import com.example.echo.utils.Constants
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PostRepository using Firebase Firestore.
 */
@Singleton
class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val postMapper: PostMapper,
    private val userRepository: UserRepository,
    private val database: EchoDatabase,
    private val postDao: PostDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope
) : PostRepository {

    private val postsCollection = firestore.collection(Constants.COLLECTION_POSTS)

    /**
     * A single live listener over the newest [Constants.POSTS_QUERY_LIMIT] posts,
     * shared across all collectors (Feed + Map). Without `shareIn`, each
     * `getPosts()` collector opened its own snapshot listener, so overlapping
     * subscriptions (e.g. a Feed↔Map tab switch) doubled the Firestore reads.
     * `WhileSubscribed(5000)` keeps the listener warm for 5s after the last
     * collector leaves (so a quick tab switch reuses it), and `replay = 1` hands a
     * new collector the latest snapshot immediately rather than re-fetching.
     */
    private val sharedPosts: Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(Constants.POSTS_QUERY_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val entities = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PostEntity::class.java)
                } ?: emptyList()

                val currentUserId = auth.currentUser?.uid
                val posts = postMapper.toDomainList(entities, currentUserId)
                trySend(posts)
            }

        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)
        .shareIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    override fun getPosts(): Flow<List<Post>> = sharedPosts

    override fun observeFeed(): Flow<List<Post>> =
        postDao.observeFeed()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun refreshFeed(limit: Long): List<Post> = withContext(ioDispatcher) {
        val page = fetchPage(afterTimestamp = null, limit = limit)
        // Replace in one transaction so the feed flow emits the new page once rather
        // than briefly flashing empty between the clear and the insert.
        database.withTransaction {
            postDao.clear()
            postDao.upsertAll(page.map { it.toCached() })
        }
        page
    }

    override suspend fun loadMoreFeed(afterTimestamp: Long, limit: Long): List<Post> =
        withContext(ioDispatcher) {
            val page = fetchPage(afterTimestamp = afterTimestamp, limit = limit)
            postDao.upsertAll(page.map { it.toCached() })
            page
        }

    override suspend fun setCachedLike(postId: String, liked: Boolean, likeCount: Int) =
        withContext(ioDispatcher) {
            postDao.updateLike(postId, liked, likeCount)
        }

    /**
     * One-time newest-first page read (not a live listener), so the feed only bills
     * reads for the posts actually scrolled into view. Used to refill the Room cache.
     */
    private suspend fun fetchPage(afterTimestamp: Long?, limit: Long): List<Post> {
        var query = postsCollection
            .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(limit)
        if (afterTimestamp != null) {
            query = query.startAfter(afterTimestamp)
        }
        val snapshot = query.get().await()
        val entities = snapshot.documents.mapNotNull { it.toObject(PostEntity::class.java) }
        return postMapper.toDomainList(entities, auth.currentUser?.uid)
    }

    override suspend fun getPostsNear(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double
    ): List<Post> = withContext(ioDispatcher) {
        val center = GeoLocation(latitude, longitude)
        // A circle maps to a set of geohash ranges; query each, then drop the false
        // positives the rectangular ranges pull in just outside the true radius.
        val entities = GeoFireUtils.getGeoHashQueryBounds(center, radiusMeters)
            .map { bound ->
                postsCollection
                    .orderBy(Constants.FIELD_GEOHASH)
                    .startAt(bound.startHash)
                    .endAt(bound.endHash)
                    .get()
                    .await()
            }
            .flatMap { it.documents }
            .mapNotNull { it.toObject(PostEntity::class.java) }
            .distinctBy { it.id }
            .filter { entity ->
                val lat = entity.latitude
                val lng = entity.longitude
                lat != null && lng != null &&
                    GeoFireUtils.getDistanceBetween(GeoLocation(lat, lng), center) <= radiusMeters
            }
        postMapper.toDomainList(entities, auth.currentUser?.uid)
    }

    override fun getPostsByTag(tag: String): Flow<List<Post>> = flow {
        // Server-side filter so we only download (and pay for) posts that actually carry
        // the tag, instead of streaming the whole collection and filtering in Kotlin.
        // Tags are stored lowercased (see PostMapper.toFirestoreMap and the create screen),
        // so a normalized exact match is correct. A one-shot read (not a live listener)
        // suits the short-lived tag view; results are capped and sorted newest-first in
        // memory to avoid needing a composite (array-contains + timestamp) index.
        val normalized = tag.trim().lowercase()
        val snapshot = postsCollection
            .whereArrayContains(Constants.FIELD_TAGS, normalized)
            .limit(Constants.POSTS_QUERY_LIMIT)
            .get()
            .await()
        val entities = snapshot.documents
            .mapNotNull { it.toObject(PostEntity::class.java) }
            .sortedByDescending { it.timestamp }
        emit(postMapper.toDomainList(entities, auth.currentUser?.uid))
    }.flowOn(ioDispatcher)
    
    override fun getPostsByAuthorId(authorId: String): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .whereEqualTo("authorId", authorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entities = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PostEntity::class.java)
                } ?: emptyList()
                val currentUserId = auth.currentUser?.uid
                val posts = postMapper.toDomainList(entities, currentUserId)
                    .sortedByDescending { it.timestamp }
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    override suspend fun getPostById(postId: String): Post? = withContext(ioDispatcher) {
        val doc = postsCollection.document(postId).get().await()
        val entity = doc.toObject(PostEntity::class.java) ?: return@withContext null
        val currentUserId = auth.currentUser?.uid
        postMapper.toDomain(entity, currentUserId)
    }

    override fun getPostByIdFlow(postId: String): Flow<Post?> = callbackFlow {
        val listener = postsCollection.document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val entity = snapshot?.toObject(PostEntity::class.java)
                val currentUserId = auth.currentUser?.uid
                trySend(entity?.let { postMapper.toDomain(it, currentUserId) })
            }
        
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)
    
    override suspend fun createPost(
        message: String,
        latitude: Double?,
        longitude: Double?,
        tags: List<String>
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val currentUser = auth.currentUser 
                ?: return@withContext Result.failure(IllegalStateException("User must be signed in to create a post"))
            
            if (currentUser.isAnonymous) {
                return@withContext Result.failure(IllegalStateException("Anonymous users cannot create posts"))
            }

            // Posts are attributed to the user's chosen handle, validated against
            // their profile by the security rules. Distinguish a read error from a
            // genuinely missing profile so the user sees the right message.
            val profileResult = userRepository.getCurrentUserProfile()
            val username = profileResult.getOrNull()?.username
                ?: return@withContext Result.failure(
                    profileResult.exceptionOrNull()
                        ?: IllegalStateException("Please finish setting up your profile before posting.")
                )

            val newDocRef = postsCollection.document()
            val postMap = postMapper.toFirestoreMap(
                authorId = currentUser.uid,
                username = username,
                message = message,
                latitude = latitude,
                longitude = longitude,
                tags = tags,
                postId = newDocRef.id
            )
            
            withWriteTimeout { newDocRef.set(postMap).await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updatePost(postId: String, newMessage: String): Unit = withContext(ioDispatcher) {
        withWriteTimeout {
            postsCollection.document(postId)
                .update("message", newMessage)
                .await()
        }
        Unit
    }

    override suspend fun deletePost(postId: String): Unit = withContext(ioDispatcher) {
        withWriteTimeout {
            postsCollection.document(postId)
                .delete()
                .await()
        }
        Unit
    }
    
    override suspend fun toggleLike(postId: String): Boolean = withContext(ioDispatcher) {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("User must be signed in to like a post")
        if (currentUser.isAnonymous) {
            throw IllegalStateException("Sign in to like posts")
        }
        val userId = currentUser.uid
        val docRef = postsCollection.document(postId)

        withWriteTimeout {
            // Read and write in one transaction so two simultaneous likes can't both read
            // the same "before" state and clobber each other (which would drop a like and
            // leave likeCount wrong). likeCount is kept == likes.size for the rules.
            // Note: FieldValue.arrayUnion/increment can't be used here — the rules validate
            // the concrete resulting array against likeCount, which opaque operators defeat.
            firestore.runTransaction { txn ->
                val entity = txn.get(docRef).toObject(PostEntity::class.java)
                    ?: throw IllegalStateException("Post not found")

                val isCurrentlyLiked = entity.likes.contains(userId)
                val newLikes =
                    if (isCurrentlyLiked) entity.likes - userId else entity.likes + userId
                txn.update(
                    docRef,
                    mapOf(
                        Constants.FIELD_LIKES to newLikes,
                        "likeCount" to newLikes.size
                    )
                )
                !isCurrentlyLiked
            }.await()
        }
    }
}
