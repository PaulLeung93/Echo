package dev.echoapp.echo.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.echoapp.echo.domain.repository.AuthorAvatarResolver
import kotlinx.coroutines.flow.flowOf

/**
 * App-wide resolver for authors' current avatars, provided at the app root. Null in
 * contexts where it isn't supplied (e.g. @Preview), in which case avatars fall back
 * to the denormalized URL passed to [AuthorPhotoAvatar].
 */
val LocalAuthorAvatarResolver = staticCompositionLocalOf<AuthorAvatarResolver?> { null }

/**
 * Avatar for a post/comment author that stays fresh: it resolves the author's
 * *current* photo live by [authorId], so a photo change is reflected everywhere the
 * author appears — including older content. [fallbackPhotoUrl] (the value
 * denormalized on the post/comment) is shown instantly and while offline, until the
 * live value arrives, then the live value takes precedence. Falls back to initials
 * when neither is available.
 */
@Composable
fun AuthorPhotoAvatar(
    authorId: String,
    name: String,
    fallbackPhotoUrl: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val resolver = LocalAuthorAvatarResolver.current
    val flow = remember(authorId, resolver) {
        resolver?.photoUrl(authorId) ?: flowOf(null)
    }
    val liveUrl by flow.collectAsState(initial = null)
    ProfileAvatar(
        photoUrl = liveUrl ?: fallbackPhotoUrl,
        name = name,
        modifier = modifier,
        size = size
    )
}
