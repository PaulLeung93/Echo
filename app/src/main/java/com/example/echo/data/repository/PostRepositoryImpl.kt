package com.example.echo.data.repository

import com.example.echo.data.entity.PostEntity
import com.example.echo.data.mapper.PostMapper
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.Post
import com.example.echo.domain.repository.PostRepository
import com.example.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PostRepository {
    
    private val postsCollection = firestore.collection(Constants.COLLECTION_POSTS)
    
    override fun getPosts(): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
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
    
    override fun getPostsByTag(tag: String): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val entities = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PostEntity::class.java)
                }?.filter { entity ->
                    entity.tags.any { it.equals(tag, ignoreCase = true) }
                } ?: emptyList()
                
                val currentUserId = auth.currentUser?.uid
                val posts = postMapper.toDomainList(entities, currentUserId)
                trySend(posts)
            }
        
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)
    
    override fun getPostsWithLocation(): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val entities = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PostEntity::class.java)
                }?.filter { it.latitude != null && it.longitude != null } ?: emptyList()
                
                val currentUserId = auth.currentUser?.uid
                val posts = postMapper.toDomainList(entities, currentUserId)
                trySend(posts)
            }
        
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)
    
    
    override fun getPostsByUsername(username: String): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .whereEqualTo("username", username)
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
            
            val newDocRef = postsCollection.document()
            val postMap = postMapper.toFirestoreMap(
                authorId = currentUser.uid,
                username = currentUser.email ?: "anonymous",
                message = message,
                latitude = latitude,
                longitude = longitude,
                tags = tags,
                postId = newDocRef.id
            )
            
            newDocRef.set(postMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updatePost(postId: String, newMessage: String): Unit = withContext(ioDispatcher) {
        postsCollection.document(postId)
            .update("message", newMessage)
            .await()
        Unit
    }
    
    override suspend fun deletePost(postId: String): Unit = withContext(ioDispatcher) {
        postsCollection.document(postId)
            .delete()
            .await()
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
        val doc = docRef.get().await()
        val entity = doc.toObject(PostEntity::class.java) 
            ?: throw IllegalStateException("Post not found")
        
        val isCurrentlyLiked = entity.likes.contains(userId)

        // Update likes and the denormalized likeCount atomically (single write) so they
        // can never drift and so security rules can require likeCount == likes.size().
        val newLikes = if (isCurrentlyLiked) entity.likes - userId else entity.likes + userId
        docRef.update(
            mapOf(
                Constants.FIELD_LIKES to newLikes,
                "likeCount" to newLikes.size
            )
        ).await()

        !isCurrentlyLiked
    }
    
    override suspend fun refreshPosts(): Result<Unit> {
        return Result.success(Unit)
    }
}
