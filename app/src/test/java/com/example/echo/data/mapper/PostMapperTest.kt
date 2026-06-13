package com.example.echo.data.mapper

import com.example.echo.data.entity.PostEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PostMapper following Given/When/Then structure.
 */
class PostMapperTest {
    
    private lateinit var mapper: PostMapper
    
    @Before
    fun setup() {
        mapper = PostMapper()
    }
    
    @Test
    fun `given entity with likes, when toDomain called with current user in likes, then likedByCurrentUser is true`() {
        // Given
        val currentUserId = "user123"
        val entity = PostEntity(
            id = "post1",
            username = "test@test.com",
            message = "Test message",
            timestamp = 1234567890L,
            latitude = 40.7128,
            longitude = -74.0060,
            tags = listOf("tag1", "tag2"),
            likeCount = 5,
            commentCount = 3,
            likes = listOf("user123", "user456")
        )
        
        // When
        val result = mapper.toDomain(entity, currentUserId)
        
        // Then
        assertTrue(result.likedByCurrentUser)
        assertEquals(5, result.likeCount)
        assertEquals("post1", result.id)
    }
    
    @Test
    fun `given entity with likes, when toDomain called with user not in likes, then likedByCurrentUser is false`() {
        // Given
        val currentUserId = "user789"
        val entity = PostEntity(
            id = "post1",
            username = "test@test.com",
            message = "Test message",
            timestamp = 1234567890L,
            likes = listOf("user123", "user456")
        )
        
        // When
        val result = mapper.toDomain(entity, currentUserId)
        
        // Then
        assertFalse(result.likedByCurrentUser)
    }
    
    @Test
    fun `given entity, when toDomain called with null currentUserId, then likedByCurrentUser is false`() {
        // Given
        val entity = PostEntity(
            id = "post1",
            username = "test@test.com",
            message = "Test message",
            timestamp = 1234567890L,
            likes = listOf("user123")
        )
        
        // When
        val result = mapper.toDomain(entity, null)
        
        // Then
        assertFalse(result.likedByCurrentUser)
    }
    
    @Test
    fun `given entity list, when toDomainList called, then all entities are mapped`() {
        // Given
        val entities = listOf(
            PostEntity(id = "post1", username = "user1", message = "msg1", timestamp = 1L),
            PostEntity(id = "post2", username = "user2", message = "msg2", timestamp = 2L),
            PostEntity(id = "post3", username = "user3", message = "msg3", timestamp = 3L)
        )
        
        // When
        val result = mapper.toDomainList(entities, null)
        
        // Then
        assertEquals(3, result.size)
        assertEquals("post1", result[0].id)
        assertEquals("post2", result[1].id)
        assertEquals("post3", result[2].id)
    }
    
    @Test
    fun `given entity with null likeCount, when toDomain called, then likes size is used`() {
        // Given
        val entity = PostEntity(
            id = "post1",
            username = "test@test.com",
            message = "Test message",
            timestamp = 1234567890L,
            likeCount = null,
            likes = listOf("user1", "user2", "user3")
        )
        
        // When
        val result = mapper.toDomain(entity, null)
        
        // Then
        assertEquals(3, result.likeCount)
    }
    
    @Test
    fun `given parameters, when toFirestoreMap called, then map contains all required fields`() {
        // Given
        val authorId = "uid-123"
        val username = "test@test.com"
        val message = "Hello World"
        val latitude = 40.7128
        val longitude = -74.0060
        val tags = listOf("tag1", "tag2")
        val postId = "newPost123"

        // When
        val result = mapper.toFirestoreMap(authorId, username, message, latitude, longitude, tags, postId)

        // Then
        assertEquals(postId, result["id"])
        assertEquals(authorId, result["authorId"])
        assertEquals(username, result["username"])
        assertEquals(message, result["message"])
        assertEquals(tags, result["tags"])
        assertEquals(latitude, result["latitude"])
        assertEquals(longitude, result["longitude"])
        assertEquals(0, result["likeCount"])
        assertEquals(0, result["commentCount"])
    }
    
    @Test
    fun `given null location, when toFirestoreMap called, then map does not contain location fields`() {
        // Given
        val authorId = "uid-123"
        val username = "test@test.com"
        val message = "Hello World"
        val tags = listOf("tag1")
        val postId = "newPost123"

        // When
        val result = mapper.toFirestoreMap(authorId, username, message, null, null, tags, postId)
        
        // Then
        assertFalse(result.containsKey("latitude"))
        assertFalse(result.containsKey("longitude"))
    }
}
