package com.example.echo.data.repository

import com.example.echo.data.entity.CommentEntity
import com.example.echo.data.mapper.CommentMapper
import com.example.echo.di.IoDispatcher
import com.example.echo.domain.model.Comment
import com.example.echo.domain.repository.CommentRepository
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
 * Implementation of CommentRepository using Firebase Firestore.
 */
@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val commentMapper: CommentMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CommentRepository {
    
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
            
            val commentMap = commentMapper.toFirestoreMap(
                username = currentUser.email ?: "anonymous",
                message = message
            )
            
            val docRef = getCommentsCollection(postId).add(commentMap).await()
            
            // Update comment count on post
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .update("commentCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            
            Comment(
                id = docRef.id,
                username = currentUser.email ?: "anonymous",
                message = message,
                timestamp = System.currentTimeMillis()
            )
        }
    
    override suspend fun deleteComment(postId: String, commentId: String): Unit = 
        withContext(ioDispatcher) {
            getCommentsCollection(postId).document(commentId).delete().await()
            
            // Update comment count on post
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .update("commentCount", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
            Unit
        }
}
