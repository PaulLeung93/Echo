package com.example.echo.ui.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.createBitmap

fun bitmapDescriptorFromVector(context: Context, resId: Int): BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return BitmapDescriptorFactory.defaultMarker()

    val width = 128  // Adjust this as needed (original Google markers are ~48-96px)
    val height = 128

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
