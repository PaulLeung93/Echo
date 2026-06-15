package com.example.echo.ui.maps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

/**
 * WORKSHOP PROTOTYPE — circle + short pointer markers.
 *
 * Draws a rounded circle with a small downward pointer (so it still marks the
 * exact coordinate) filled in [color], with a white category glyph inside.
 * Everything is programmatic, like [createClusterIcon], so shape / color / tail
 * length / glyph are all tweakable here while we settle on a look. The Marker's
 * default anchor (0.5, 1.0) lines the pointer tip up with the post/POI position.
 *
 * Tweak knobs: [BASE_RADIUS], [TAIL_HEIGHT], [GLYPH_FRACTION].
 */
private const val BASE_RADIUS = 44f
private const val TAIL_HEIGHT = 22f
private const val GLYPH_FRACTION = 0.62f // glyph box side as a fraction of circle diameter

/** Per-category marker fills. Mid-tones chosen for white-glyph contrast. */
enum class PinCategory(val color: Int) {
    POST(Color.rgb(0xFE, 0x6E, 0x4C)),     // coral (brand)
    PARK(Color.rgb(0x2D, 0x5A, 0x35)),     // sage green
    COLLEGE(Color.rgb(0x1F, 0x3A, 0x93)),  // indigo
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

fun createPinIcon(category: PinCategory, scale: Float = 1f): Bitmap {
    val radius = BASE_RADIUS * scale
    val tail = TAIL_HEIGHT * scale
    val cx = radius
    val cy = radius
    val width = (radius * 2).toInt()
    val height = (radius * 2 + tail).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = category.color }

    // Body: pointer triangle first, then the circle on top so the seam is hidden.
    val spread = Math.toRadians(38.0)
    Path().apply {
        moveTo((cx - radius * sin(spread)).toFloat(), (cy + radius * cos(spread)).toFloat())
        lineTo(cx, cy + radius + tail)
        lineTo((cx + radius * sin(spread)).toFloat(), (cy + radius * cos(spread)).toFloat())
        close()
        canvas.drawPath(this, fill)
    }
    canvas.drawCircle(cx, cy, radius, fill)

    // Glyph
    val box = radius * 2 * GLYPH_FRACTION
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
