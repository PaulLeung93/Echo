package dev.echoapp.echo.ui.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.createBitmap

/**
 * Cache of rendered marker icons keyed by (drawable resId, scale). Every post/POI
 * marker draws from a tiny set of distinct icons (a few drawables × two scales),
 * so without a cache each marker re-rasterizes an identical bitmap. The descriptors
 * are lightweight and the key space is bounded, so this lives for the process.
 */
private val iconCache = HashMap<Pair<Int, Float>, BitmapDescriptor>()

fun bitmapDescriptorFromVector(context: Context, resId: Int, scale: Float = 1f): BitmapDescriptor {
    iconCache[resId to scale]?.let { return it }

    val drawable = ContextCompat.getDrawable(context, resId) ?: return BitmapDescriptorFactory.defaultMarker()

    val width = (128 * scale).toInt()
    val height = (128 * scale).toInt()

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap).also { iconCache[resId to scale] = it }
}
