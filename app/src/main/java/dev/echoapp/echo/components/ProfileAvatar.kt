package dev.echoapp.echo.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

/**
 * Circular profile avatar: shows the user's uploaded photo when [photoUrl] is set,
 * otherwise falls back to the deterministic initials avatar. Also falls back to
 * initials while loading or if the image fails to load.
 */
@Composable
fun ProfileAvatar(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    if (photoUrl.isNullOrBlank()) {
        AuthorAvatar(name = name, modifier = modifier, size = size)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(photoUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Profile photo",
        contentScale = ContentScale.Crop,
        loading = { AuthorAvatar(name = name, size = size) },
        error = { AuthorAvatar(name = name, size = size) },
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}
