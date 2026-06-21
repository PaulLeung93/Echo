package dev.echoapp.echo.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Resolves an author's *current* avatar URL by their stable uid, live. Posts and
 * comments denormalize a photo URL for instant/offline display, but that value goes
 * stale when the author later changes their photo. Display surfaces use this to
 * resolve the up-to-date avatar by `authorId` so a photo change reflects everywhere
 * — including on the author's older posts and comments.
 */
interface AuthorAvatarResolver {

    /**
     * Live current avatar URL for [authorId], or null when the author has none / is
     * unknown. Emits updates when the author changes their photo.
     */
    fun photoUrl(authorId: String): Flow<String?>
}
