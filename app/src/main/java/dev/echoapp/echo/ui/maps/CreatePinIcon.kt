package dev.echoapp.echo.ui.maps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Glyph-in-circle POI markers.
 *
 * Draws a solid colored disc in the POI's category color, ringed by a white
 * outer border, with a white category glyph centered. Fully programmatic (like
 * [createClusterIcon]) so shape / color / ring / glyph are all tweakable here.
 *
 * These are round (no pointer), so the Marker should be anchored at its center
 * — Offset(0.5f, 0.5f) — rather than the default bottom-center.
 *
 * Tweak knobs: [BASE_RADIUS], [RING_WIDTH], [GLYPH_FRACTION].
 */
private const val BASE_RADIUS = 50f
private const val RING_WIDTH = 8f
private const val GLYPH_FRACTION = 0.6f // glyph box side as a fraction of the colored disc diameter

/** Per-category disc fills. Mid-tones chosen for white ring + glyph contrast. */
enum class PinCategory(val color: Int) {
    POST(Color.rgb(0xAD, 0x34, 0x18)),     // burnt-coral (M3 primary, #AD3418) — fallback for unknown types
    PARK(Color.rgb(0x2D, 0x5A, 0x35)),     // sage green
    COLLEGE(Color.rgb(0x18, 0x2C, 0x78)),  // deep indigo (distinct from map's water/road blues)
    LANDMARK(Color.rgb(0xE0, 0x8A, 0x1E)); // amber

    companion object {
        fun fromPoiType(type: String): PinCategory = when (type.lowercase()) {
            "park" -> PARK
            "college" -> COLLEGE
            "landmark" -> LANDMARK
            else -> POST
        }
    }
}

/**
 * Base disc scale for a given (integer-bucketed) map zoom. POIs are admin-curated
 * and can bunch up tightly at city scale, so discs shrink when zoomed out and grow
 * as you zoom in. Bucketing by whole zoom levels keeps the number of distinct
 * bitmaps tiny — only a handful per category — so [poiPinDescriptor]'s cache stays
 * small and icons regenerate only when crossing a level, never per camera frame.
 */
fun markerScaleForZoom(zoom: Int): Float = when {
    zoom <= 10 -> 0.55f
    zoom <= 12 -> 0.8f
    zoom <= 14 -> 1.0f
    zoom <= 16 -> 1.2f
    else -> 1.4f
}

/**
 * Cached descriptor for a POI disc. Keyed by category + quantized scale so repeated
 * POIs of the same type/zoom share one bitmap. Lives for the process — the key space
 * is bounded (4 categories × a few zoom buckets × selected/not).
 */
private val pinCache = HashMap<Pair<PinCategory, Int>, BitmapDescriptor>()

fun poiPinDescriptor(category: PinCategory, scale: Float): BitmapDescriptor {
    val key = category to (scale * 100).toInt()
    pinCache[key]?.let { return it }
    return BitmapDescriptorFactory.fromBitmap(createPinIcon(category, scale))
        .also { pinCache[key] = it }
}

fun createPinIcon(category: PinCategory, scale: Float = 1f): Bitmap {
    val radius = BASE_RADIUS * scale
    val ring = RING_WIDTH * scale
    val size = (radius * 2).toInt()
    val cx = radius
    val cy = radius

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // White outer ring, then the colored disc inside it.
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.WHITE
    canvas.drawCircle(cx, cy, radius, paint)
    val discRadius = radius - ring
    paint.color = category.color
    canvas.drawCircle(cx, cy, discRadius, paint)

    // White glyph centered inside the disc.
    val box = discRadius * 2 * GLYPH_FRACTION
    val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    when (category) {
        PinCategory.POST -> drawSpeechBubble(canvas, cx, cy, box, glyph)
        PinCategory.PARK -> drawTree(canvas, cx, cy, box, glyph)
        PinCategory.COLLEGE -> drawCap(canvas, cx, cy, box, glyph)
        PinCategory.LANDMARK -> drawArch(canvas, cx, cy, box, glyph)
    }

    return bitmap
}

private fun drawSpeechBubble(c: Canvas, cx: Float, cy: Float, box: Float, p: Paint) {
    val w = box * 0.9f
    val h = box * 0.62f
    val rect = RectF(cx - w / 2, cy - h / 2 - box * 0.06f, cx + w / 2, cy + h / 2 - box * 0.06f)
    c.drawRoundRect(rect, h * 0.35f, h * 0.35f, p)
    Path().apply {
        moveTo(cx - w * 0.18f, rect.bottom - 1f)
        lineTo(cx - w * 0.30f, rect.bottom + box * 0.20f)
        lineTo(cx + w * 0.02f, rect.bottom - 1f)
        close()
        c.drawPath(this, p)
    }
}

private fun drawTree(c: Canvas, cx: Float, cy: Float, box: Float, p: Paint) {
    val canopyR = box * 0.30f
    val canopyCy = cy - box * 0.10f
    c.drawCircle(cx, canopyCy, canopyR, p)
    c.drawCircle(cx - canopyR * 0.7f, canopyCy + canopyR * 0.25f, canopyR * 0.7f, p)
    c.drawCircle(cx + canopyR * 0.7f, canopyCy + canopyR * 0.25f, canopyR * 0.7f, p)
    val trunk = box * 0.10f
    c.drawRect(cx - trunk / 2, canopyCy, cx + trunk / 2, cy + box * 0.42f, p)
}

private fun drawCap(c: Canvas, cx: Float, cy: Float, box: Float, p: Paint) {
    val w = box * 0.55f
    val h = box * 0.26f
    // Mortarboard diamond
    Path().apply {
        moveTo(cx, cy - h)
        lineTo(cx + w, cy - h * 0.05f)
        lineTo(cx, cy + h * 0.9f)
        lineTo(cx - w, cy - h * 0.05f)
        close()
        c.drawPath(this, p)
    }
    // Cap base under the board
    val baseW = box * 0.42f
    c.drawRect(cx - baseW / 2, cy + h * 0.5f, cx + baseW / 2, cy + box * 0.32f, p)
}

private fun drawArch(c: Canvas, cx: Float, cy: Float, box: Float, p: Paint) {
    val w = box * 0.62f
    val pillar = box * 0.16f
    val top = cy - box * 0.30f
    val bottom = cy + box * 0.34f
    // Two pillars + connecting top bar = gateway/arch
    c.drawRect(cx - w / 2, top, cx - w / 2 + pillar, bottom, p)
    c.drawRect(cx + w / 2 - pillar, top, cx + w / 2, bottom, p)
    c.drawRoundRect(
        RectF(cx - w / 2, top, cx + w / 2, top + pillar * 1.4f),
        pillar * 0.6f, pillar * 0.6f, p
    )
}
