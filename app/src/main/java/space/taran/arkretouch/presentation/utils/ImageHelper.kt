package space.taran.arkretouch.presentation.utils

import android.graphics.Bitmap
import space.taran.arkretouch.presentation.edit.crop.CropWindow

fun Bitmap.crop(cropParams: CropWindow.CropParams): Bitmap = Bitmap.createBitmap(
    this,
    cropParams.x,
    cropParams.y,
    cropParams.width,
    cropParams.height
)
