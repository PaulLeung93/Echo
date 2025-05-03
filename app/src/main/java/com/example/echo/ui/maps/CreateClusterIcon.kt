package com.example.echo.ui.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.ContextCompat

fun createClusterIcon(context: android.content.Context, count: Int): Bitmap {
    val radius = 40f
    val bitmap = Bitmap.createBitmap((radius * 2).toInt(), (radius * 2).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(66, 133, 244) // Cluster circle color (Google blue)
    }
    canvas.drawCircle(radius, radius, radius, paint)

    paint.apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val y = radius - ((paint.descent() + paint.ascent()) / 2)
    canvas.drawText(count.toString(), radius, y, paint)

    return bitmap
}
