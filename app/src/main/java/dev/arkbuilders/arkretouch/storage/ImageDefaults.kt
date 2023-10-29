package dev.arkbuilders.arkretouch.storage

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.Serializable

// FIXME: Replace with Parcelable
@Serializable
data class ImageDefaults(
    val colorValue: ULong = Color.White.value,
    val resolution: Resolution? = null
)

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