package dev.arkbuilders.arkretouch.common.model

import android.os.Parcelable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dev.arkbuilders.arkretouch.edit.manager.EditManager
import java.io.File
import kotlinx.parcelize.Parcelize

sealed class ImageContainer : Parcelable {

    companion object {
        operator fun invoke(imagePath: File) =
            Path(imagePath)

        operator fun invoke(iconUrl: String) =
            Uri(iconUrl)
    }

    abstract fun applyTo(editManager: EditManager)

    @Parcelize
    class Path(val imagePath: File) : ImageContainer() {
        override fun applyTo(editManager: EditManager) {
            // TODO: Load bitmap into edit manager
        }
    }

    @Parcelize
    class Uri(
        private val uri: String
    ) : ImageContainer() {
        override fun applyTo(editManager: EditManager) {
            // TODO: Load bitmap into edit manager

        }
    }
}
