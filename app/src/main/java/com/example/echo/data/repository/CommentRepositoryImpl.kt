package com.example.echo.data.repository

import com.example.echo.data.entity.CommentEntity
import com.example.echo.data.mapper.CommentMapper
import com.example.echo.data.withWriteTimeout
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.Comment
import com.example.echo.domain.repository.CommentRepository
import com.example.echo.domain.repository.UserRepository
import com.example.echo.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
 * Implementation of CommentRepository using Firebase Firestore.
 */
@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val commentMapper: CommentMapper,
    private val userRepository: UserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CommentRepository {

    /** The caller's chosen handle (validated by the rules), or fail if no profile. */
    private suspend fun requireUsername(): String {
        val result = userRepository.getCurrentUserProfile()
        return result.getOrNull()?.username
            ?: throw (result.exceptionOrNull()
                ?: IllegalStateException("Please finish setting up your profile before commenting."))
    }

    private fun getCommentsCollection(postId: String) =
        firestore.collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .collection(Constants.COLLECTION_COMMENTS)
    
    override fun getCommentsForPost(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = getCommentsCollection(postId)
            .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val entities = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CommentEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                val comments = commentMapper.toDomainList(entities)
                trySend(comments)
            }
        
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)
    
    override suspend fun addComment(postId: String, message: String): Comment = 
        withContext(ioDispatcher) {
            val currentUser = auth.currentUser 
                ?: throw IllegalStateException("User must be signed in to comment")
            
            if (currentUser.isAnonymous) {
                throw IllegalStateException("Anonymous users cannot comment")
            }

            val username = requireUsername()
            val commentMap = commentMapper.toFirestoreMap(
                authorId = currentUser.uid,
                username = username,
                message = message
            )

            val commentRef = getCommentsCollection(postId).document()
            val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)

            // Write the comment and bump the denormalized count in one batch so they can't
            // drift if the process dies between the two writes. Same number of billed writes.
            withWriteTimeout {
                firestore.batch().apply {
                    set(commentRef, commentMap)
                    update(postRef, "commentCount", FieldValue.increment(1))
                }.commit().await()
            }

            Comment(
                id = commentRef.id,
                authorId = currentUser.uid,
                username = username,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        }

    override suspend fun deleteComment(postId: String, commentId: String): Unit =
        withContext(ioDispatcher) {
            val commentRef = getCommentsCollection(postId).document(commentId)
            val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
            withWriteTimeout {
                firestore.batch().apply {
                    delete(commentRef)
                    update(postRef, "commentCount", FieldValue.increment(-1))
                }.commit().await()
            }
            Unit
        }

    private fun getPoiCommentsCollection(poiId: String) = 
        firestore.collection(Constants.COLLECTION_POIS)
            .document(poiId)
            .collection(Constants.COLLECTION_COMMENTS)

    override fun getCommentsForPoi(poiId: String): Flow<List<Comment>> = callbackFlow {
        val listener = getPoiCommentsCollection(poiId)
            .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val entities = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CommentEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                val comments = commentMapper.toDomainList(entities)
                trySend(comments)
            }
        
        awaitClose { listener.remove() }
    }.flowOn(ioDispatcher)

    override suspend fun addCommentToPoi(poiId: String, message: String): Comment = 
        withContext(ioDispatcher) {
            val currentUser = auth.currentUser 
                ?: throw IllegalStateException("User must be signed in to comment")
            
            if (currentUser.isAnonymous) {
                throw IllegalStateException("Anonymous users cannot comment")
            }

            val username = requireUsername()
            val commentMap = commentMapper.toFirestoreMap(
                authorId = currentUser.uid,
                username = username,
                message = message
            )

            val commentRef = getPoiCommentsCollection(poiId).document()
            val poiRef = firestore.collection(Constants.COLLECTION_POIS).document(poiId)

            withWriteTimeout {
                firestore.batch().apply {
                    set(commentRef, commentMap)
                    update(poiRef, "commentCount", FieldValue.increment(1))
                }.commit().await()
            }

            Comment(
                id = commentRef.id,
                authorId = currentUser.uid,
                username = username,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        }

    override suspend fun deleteCommentFromPoi(poiId: String, commentId: String): Unit =
        withContext(ioDispatcher) {
            val commentRef = getPoiCommentsCollection(poiId).document(commentId)
            val poiRef = firestore.collection(Constants.COLLECTION_POIS).document(poiId)
            withWriteTimeout {
                firestore.batch().apply {
                    delete(commentRef)
                    update(poiRef, "commentCount", FieldValue.increment(-1))
                }.commit().await()
            }
            Unit
        }
}
