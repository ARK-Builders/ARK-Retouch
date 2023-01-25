package space.taran.arkretouch.presentation.edit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.taran.arkretouch.R
import space.taran.arkretouch.di.DIManager
import space.taran.arkretouch.presentation.drawing.EditManager
import timber.log.Timber
import java.io.File
import java.nio.file.Path
import kotlin.io.path.outputStream

class EditViewModel(
    private val launchedFromIntent: Boolean,
    private val imagePath: Path?,
    private val imageUri: String?,
) : ViewModel() {
    val editManager = EditManager()

    var strokeSliderExpanded by mutableStateOf(false)
    var menusVisible by mutableStateOf(true)
    var strokeWidth by mutableStateOf(5f)
    var showSavePathDialog by mutableStateOf(false)
    var showExitDialog by mutableStateOf(false)
    var showMoreOptionsPopup by mutableStateOf(false)
    var imageSaved by mutableStateOf(false)
    var exitConfirmed = false
        private set

    fun loadImage() {
        imagePath?.let {
            loadImageWithPath(
                DIManager.component.app(),
                imagePath,
                editManager
            )
            return
        }
        imageUri?.let {
            loadImageWithUri(
                DIManager.component.app(),
                imageUri,
                editManager
            )
        }
    }

    fun saveImage(savePath: Path) =
        viewModelScope.launch(Dispatchers.IO) {
            val combinedBitmap = getCombinedImageBitmap()
            savePath.outputStream().use { out ->
                combinedBitmap.asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            imageSaved = true
        }

    fun confirmExit() = viewModelScope.launch {
        exitConfirmed = true
        delay(2_000)
        exitConfirmed = false
    }

    fun shareImage(context: Context) =
        viewModelScope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_SEND)
            val uri = getCachedImageUri(context)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            context.apply {
                startActivity(
                    Intent.createChooser(
                        intent,
                        getString(R.string.share)
                    )
                )
            }
        }

    private fun getCachedImageUri(context: Context): Uri {
        var uri: Uri? = null
        val imageCacheFolder = File(context.cacheDir, "images")
        val combinedBitmap = getCombinedImageBitmap()
        try {
            imageCacheFolder.mkdirs()
            val file = File(imageCacheFolder, "image_to_share.png")
            file.outputStream().use { out ->
                combinedBitmap.asAndroidBitmap()
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

    private fun getCombinedImageBitmap(): ImageBitmap {
        val size = editManager.drawAreaSize.value
        val drawBitmap = ImageBitmap(
            size.width,
            size.height,
            ImageBitmapConfig.Argb8888
        )
        val drawCanvas = Canvas(drawBitmap)
        val combinedBitmap =
            ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888)
        val combinedCanvas = Canvas(combinedBitmap)
        editManager.backgroundImage.value?.let {
            combinedCanvas.drawImage(
                it,
                editManager.calcImageOffset(),
                Paint()
            )
        }
        editManager.drawPaths.forEach {
            drawCanvas.drawPath(it.path, it.paint)
        }
        combinedCanvas.drawImage(drawBitmap, Offset.Zero, Paint())
        return combinedBitmap
    }
}

class EditViewModelFactory @AssistedInject constructor(
    @Assisted private val launchedFromIntent: Boolean,
    @Assisted private val imagePath: Path?,
    @Assisted private val imageUri: String?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditViewModel(launchedFromIntent, imagePath, imageUri) as T
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted launchedFromIntent: Boolean,
            @Assisted imagePath: Path?,
            @Assisted imageUri: String?,
        ): EditViewModelFactory
    }
}

private fun loadImageWithPath(
    context: Context,
    image: Path,
    editManager: EditManager
) {
    initGlideBuilder(context)
        .load(image.toFile())
        .loadInto(editManager)
}

private fun loadImageWithUri(
    context: Context,
    uri: String,
    editManager: EditManager
) {
    initGlideBuilder(context)
        .load(uri.toUri())
        .loadInto(editManager)
}

private fun initGlideBuilder(context: Context) = Glide
    .with(context)
    .asBitmap()
    .skipMemoryCache(true)
    .diskCacheStrategy(DiskCacheStrategy.NONE)

private fun RequestBuilder<Bitmap>.loadInto(
    editManager: EditManager
) {
    into(object : CustomTarget<Bitmap>() {
        override fun onResourceReady(
            bitmap: Bitmap,
            transition: Transition<in Bitmap>?
        ) {
            val areaSize = editManager.drawAreaSize.value
            editManager.backgroundImage.value =
                resize(bitmap.asImageBitmap(), areaSize.width, areaSize.height)
        }

        override fun onLoadCleared(placeholder: Drawable?) {}
    })
}

private fun resize(
    imageBitmap: ImageBitmap,
    maxWidth: Int,
    maxHeight: Int
): ImageBitmap {
    val bitmap = imageBitmap.asAndroidBitmap()
    val width = bitmap.width
    val height = bitmap.height

    val bitmapRatio = width.toFloat() / height.toFloat()
    val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()

    var finalWidth = maxWidth
    var finalHeight = maxHeight

    if (maxRatio > bitmapRatio) {
        finalWidth = (maxHeight.toFloat() * bitmapRatio).toInt()
    } else {
        finalHeight = (maxWidth.toFloat() / bitmapRatio).toInt()
    }

    return Bitmap
        .createScaledBitmap(bitmap, finalWidth, finalHeight, true)
        .asImageBitmap()
}
