package space.taran.arkretouch.presentation.utils

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect

fun Bitmap.crop(cropRect: Rect): Bitmap = Bitmap.createBitmap(
    this,
    cropRect.left.toInt(),
    cropRect.top.toInt(),
    cropRect.width.toInt(),
    cropRect.height.toInt()
)
