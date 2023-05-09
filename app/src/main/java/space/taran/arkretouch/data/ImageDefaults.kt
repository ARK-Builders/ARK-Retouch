package space.taran.arkretouch.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ImageDefaults(
    val colorValue: ULong = Color.White.value,
    val resolution: Resolution? = null
) {
    override fun toString() = Json.encodeToString(this)

    companion object {
        fun fromString(string: String): ImageDefaults {
            return Json.decodeFromString(string)
        }
    }
}

@Serializable
data class Resolution(
    val width: Int,
    val height: Int
) {
    fun toIntSize() = IntSize(this.width, this.height)

    companion object {
        fun fromIntSize(intSize: IntSize) = Resolution(intSize.width, intSize.height)
    }
}
