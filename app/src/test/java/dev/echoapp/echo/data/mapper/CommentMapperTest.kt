package dev.echoapp.echo.data.mapper

import dev.echoapp.echo.data.entity.CommentEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CommentMapper following Given/When/Then structure.
 */
class CommentMapperTest {
    
    private lateinit var mapper: CommentMapper
    
    @Before
    fun setup() {
        mapper = CommentMapper()
    }
    
    @Test
    fun `given entity, when toDomain called, then all fields are mapped correctly`() {
        // Given
        val entity = CommentEntity(
            id = "comment1",
            authorId = "uid-123",
            username = "user@test.com",
            message = "This is a comment",
            timestamp = 1234567890L
        )

        // When
        val result = mapper.toDomain(entity)

        // Then
        assertEquals("comment1", result.id)
        assertEquals("uid-123", result.authorId)
        assertEquals("user@test.com", result.username)
        assertEquals("This is a comment", result.message)
        assertEquals(1234567890L, result.timestamp)
    }
    
    @Test
    fun `given entity with null id, when toDomain called, then id defaults to empty string`() {
        // Given
        val entity = CommentEntity(
            id = null,
            username = "user@test.com",
            message = "Comment without id",
            timestamp = 1234567890L
        )
        
        // When
        val result = mapper.toDomain(entity)
        
        // Then
        assertEquals("", result.id)
    }
    
    @Test
    fun `given entity list, when toDomainList called, then all entities are mapped`() {
        // Given
        val entities = listOf(
            CommentEntity(id = "c1", username = "user1", message = "msg1", timestamp = 1L),
            CommentEntity(id = "c2", username = "user2", message = "msg2", timestamp = 2L)
        )
        
        // When
        val result = mapper.toDomainList(entities)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("c1", result[0].id)
        assertEquals("c2", result[1].id)
    }
    
    @Test
    fun `given parameters, when toFirestoreMap called, then map contains all fields`() {
        // Given
        val authorId = "uid-123"
        val username = "test@test.com"
        val photoUrl = "https://example.com/avatars/uid-123.jpg"
        val message = "Test comment message"

        // When
        val result = mapper.toFirestoreMap(authorId, username, photoUrl, message)

        // Then
        assertEquals(authorId, result["authorId"])
        assertEquals(username, result["username"])
        assertEquals(photoUrl, result["authorPhotoUrl"])
        assertEquals(message, result["message"])
        assertTrue(result.containsKey("timestamp"))
    }
    
    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
