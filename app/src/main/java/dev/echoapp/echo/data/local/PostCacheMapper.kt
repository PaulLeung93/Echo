package dev.echoapp.echo.data.local

import dev.echoapp.echo.domain.model.Post

/**
 * Mapping between the cached Room row and the domain [Post]. The feed already maps
 * Firestore → [Post]; these keep the local cache in the same domain shape so the
 * ViewModel never sees the storage type.
 */
fun Post.toCached(): CachedPostEntity = CachedPostEntity(
    id = id,
    authorId = authorId,
    username = username,
    authorPhotoUrl = authorPhotoUrl,
    message = message,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude,
    tags = tags,
    likeCount = likeCount,
    commentCount = commentCount,
    likedByCurrentUser = likedByCurrentUser
)

fun CachedPostEntity.toDomain(): Post = Post(
    id = id,
    authorId = authorId,
    username = username,
    authorPhotoUrl = authorPhotoUrl,
    message = message,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude,
    tags = tags,
    likeCount = likeCount,
    commentCount = commentCount,
    likedByCurrentUser = likedByCurrentUser
)
