package space.taran.arkretouch.presentation.edit.resize

import android.graphics.Bitmap

class Resize {
    private lateinit var bitmap: Bitmap
    var aspectRatio: Float = 1f

    fun init(bitmap: Bitmap) {
        this.bitmap = bitmap
        aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    }

    fun getBitmap() = bitmap
}
