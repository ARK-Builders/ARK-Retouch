package space.taran.arkretouch.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.google.gson.Gson

data class ImageDefaults(
    val colorValue: ULong = Color.White.value,
    val resolution: Resolution = Resolution(0, 0)
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(string: String): ImageDefaults {
            return Gson().fromJson(string, ImageDefaults::class.java)
        }
    }
}

data class Resolution(
    val width: Int,
    val height: Int
) {
    fun toIntSize() = IntSize(this.width, this.height)

    companion object {
        fun fromIntSize(intSize: IntSize) = Resolution(intSize.width, intSize.height)
    }
}
