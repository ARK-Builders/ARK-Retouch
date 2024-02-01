package dev.arkbuilders.arkretouch.editing.repository

import androidx.core.content.FileProvider
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import timber.log.Timber
import java.io.File

class FilesRepository {

    fun getCachedImageUri(
        context: Context,
        bitmap: Bitmap,
        name: String = ""
    ): Uri {
        var uri: Uri? = null
        val imageCacheFolder = File(context.cacheDir, "images")
        try {
            imageCacheFolder.mkdirs()
            val file = File(imageCacheFolder, "image$name.png")
            file.outputStream().use { out ->
                bitmap
                    .compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Timber.tag("Cached image path").d(file.path.toString())
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri!!
    }
}