package space.taran.arkretouch.presentation.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import space.taran.arkretouch.presentation.edit.crop.CropWindow
import space.taran.arkretouch.presentation.edit.resize.ResizeOperation

fun Bitmap.crop(cropParams: CropWindow.CropParams): Bitmap = Bitmap.createBitmap(
    this,
    cropParams.x,
    cropParams.y,
    cropParams.width,
    cropParams.height
)

fun Bitmap.resize(scale: ResizeOperation.Scale): Bitmap {
    val matrix = Matrix()
    matrix.postScale(scale.x, scale.y)
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
