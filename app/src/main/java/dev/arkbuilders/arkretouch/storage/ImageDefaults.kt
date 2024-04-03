package dev.arkbuilders.arkretouch.storage

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.Serializable

/**
 * We use serialization for these classes because we write them to storage as JSON text,
 * To convert them correctly we use [kotlinx.serialization.encodeToString] and [kotlinx.serialization.decodeFromString] which requires an object to be [@Serializable]
 * Refer here [dev.arkbuilders.arkretouch.storage.OldStorageRepository.persistDefaults]
*/

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