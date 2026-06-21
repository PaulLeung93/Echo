package dev.echoapp.echo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** DAO for the offline feed cache. */
@Dao
interface PostDao {

    /** Newest-first cached feed; the feed screen's display source of truth. */
    @Query("SELECT * FROM cached_feed_posts ORDER BY timestamp DESC")
    fun observeFeed(): Flow<List<CachedPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(posts: List<CachedPostEntity>)

    @Query("DELETE FROM cached_feed_posts")
    suspend fun clear()

    /**
     * Reflect an optimistic like toggle on the cached row so the UI updates instantly
     * (the network write happens separately and a later refresh reconciles the count).
     */
    @Query("UPDATE cached_feed_posts SET likedByCurrentUser = :liked, likeCount = :likeCount WHERE id = :postId")
    suspend fun updateLike(postId: String, liked: Boolean, likeCount: Int)
}
