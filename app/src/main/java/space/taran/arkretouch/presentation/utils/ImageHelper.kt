package space.taran.arkretouch.presentation.utils

import android.graphics.Bitmap
import android.graphics.Matrix

fun Bitmap.rotate(
    matrix: Matrix
): Bitmap {
    return Bitmap.createBitmap(
        this,
        0,
        0,
        width,
        height,
        matrix,
        true
    )
}
