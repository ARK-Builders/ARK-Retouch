package space.taran.arkretouch.presentation.utils

import android.graphics.Bitmap
import android.graphics.Matrix

fun Bitmap.rotate(
    angle: Float,
    shouldResize: Boolean,
    resize: (Bitmap, Int, Int) -> Bitmap
): Bitmap {
    val matrix = Matrix()
    var newWidth = width
    var newHeight = height
    var source = this
    if (shouldResize) {
        val aspectRatio: Float = newWidth.toFloat() / newHeight.toFloat()
        newHeight = newWidth
        newWidth = (newHeight * aspectRatio).toInt()
        source = resize(this, newWidth, newHeight)
    }
    matrix.postRotate(angle)
    return Bitmap.createBitmap(
        source,
        0,
        0,
        source.width,
        source.height,
        matrix,
        true
    )
}
