package dev.arkbuilders.arkretouch.presentation.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dev.arkbuilders.arkretouch.di.DIManager
import dev.arkbuilders.arkretouch.presentation.drawing.EditManager
import java.nio.file.Path

class BitmapLoader(
    val context: Context = DIManager.component.app(),
    val editManager: EditManager
) {

    private val glideBuilder = Glide
        .with(context)
        .asBitmap()
        .skipMemoryCache(true)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
    fun loadImageFromPath(path: Path) {
        loadImage(path)
    }

    fun loadImageFromUri(uri: Uri) {
        glideBuilder
            .load(uri)
            .loadInto()
    }

    private fun loadImage(
        resourcePath: Path,
    ) {
        glideBuilder
            .load(resourcePath.toFile())
            .loadInto()
    }

    private fun RequestBuilder<Bitmap>.loadInto() {
        into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(
                bitmap: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                editManager.apply {
                    backgroundImage.value = bitmap.asImageBitmap()
                    setOriginalBackgroundImage(backgroundImage.value)
                    scaleToFit()
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }
}
