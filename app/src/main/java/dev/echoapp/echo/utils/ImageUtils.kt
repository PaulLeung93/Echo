package dev.echoapp.echo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Helpers for preparing user-picked images for upload. */
object ImageUtils {

    /**
     * Decode the image at [uri], downscale its longest edge to [maxDimension], and
     * re-encode as a JPEG at [quality]. Avatars don't need full resolution, so this
     * keeps uploads small and predictable (and well under the Storage size cap).
     * Returns null if the image can't be read/decoded.
     */
    fun compressImageForUpload(
        context: Context,
        uri: Uri,
        maxDimension: Int = 512,
        quality: Int = 85
    ): ByteArray? = try {
        val resolver = context.contentResolver

        // 1) Read just the bounds so we can sub-sample instead of decoding full-size.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sample = 1
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            while (largest / sample > maxDimension * 2) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }

            if (decoded == null) {
                null
            } else {
                // 2) Scale the longest edge to exactly maxDimension (keep aspect ratio).
                val scale = maxDimension.toFloat() / maxOf(decoded.width, decoded.height)
                val bitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * scale).toInt().coerceAtLeast(1),
                        (decoded.height * scale).toInt().coerceAtLeast(1),
                        true
                    ).also { if (it != decoded) decoded.recycle() }
                } else {
                    decoded
                }

                ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    bitmap.recycle()
                    out.toByteArray()
                }
            }
        }
    } catch (e: Exception) {
        null
    }
}
