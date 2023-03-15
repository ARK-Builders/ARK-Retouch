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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
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
import space.taran.arkretouch.presentation.drawing.CaptureArea
import space.taran.arkretouch.presentation.drawing.EditManager
import timber.log.Timber
import java.io.File
import java.nio.file.Path
import kotlin.io.path.outputStream

class EditViewModel(
    private val launchedFromIntent: Boolean,
    private val imagePath: Path?,
    private val imageUri: String?,
    private val screenDensity: Float,
) : ViewModel() {
    val editManager = EditManager(screenDensity)

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
            return
        }
        val possibleDrawArea = editManager.availableDrawAreaSize.value
        editManager.captureArea.value =
            CaptureArea(
                0f,
                0f,
                possibleDrawArea.width.toFloat(),
                possibleDrawArea.height.toFloat()
            )
        editManager.initialDrawAreaOffset = Offset(
            editManager.captureArea.value!!.left,
            editManager.captureArea.value!!.top
        )
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
        val captureArea = editManager.captureArea.value!!
        val drawBitmap = ImageBitmap(
            editManager.availableDrawAreaSize.value.width,
            editManager.availableDrawAreaSize.value.height,
            ImageBitmapConfig.Argb8888
        )
        val drawCanvas = Canvas(drawBitmap)
        val combinedBitmap =
            ImageBitmap(
                editManager.availableDrawAreaSize.value.width,
                editManager.availableDrawAreaSize.value.height,
                ImageBitmapConfig.Argb8888
            )
        val combinedCanvas = Canvas(combinedBitmap)
        combinedCanvas.nativeCanvas.setMatrix(editManager.editMatrix)
        editManager.backgroundImage.value?.let {
            combinedCanvas.nativeCanvas.drawBitmap(
                it.asAndroidBitmap(),
                editManager.initialDrawAreaOffset.x,
                editManager.initialDrawAreaOffset.y,
                null
            )
        }
        drawCanvas.nativeCanvas.setMatrix(editManager.editMatrix)
        editManager.drawPaths.forEach {
            drawCanvas.drawPath(it.path, it.paint)
        }
        combinedCanvas.nativeCanvas.setMatrix(null)
        combinedCanvas.nativeCanvas.drawBitmap(
            drawBitmap.asAndroidBitmap(),
            0f,
            0f,
            null
        )
        val captureAreaBitmap = Bitmap.createBitmap(
            combinedBitmap.asAndroidBitmap(),
            captureArea.left.toInt(),
            captureArea.top.toInt(),
            captureArea.width.toInt(),
            captureArea.height.toInt()
        )
        return captureAreaBitmap.asImageBitmap()
    }
}

class EditViewModelFactory @AssistedInject constructor(
    @Assisted private val launchedFromIntent: Boolean,
    @Assisted private val imagePath: Path?,
    @Assisted private val imageUri: String?,
    private val screenDensity: Float,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditViewModel(
            launchedFromIntent,
            imagePath,
            imageUri,
            screenDensity
        ) as T
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
            val possibleDrawAreaSize = editManager.availableDrawAreaSize.value
            val backgroundImage = resize(
                bitmap.asImageBitmap(),
                possibleDrawAreaSize.width,
                possibleDrawAreaSize.height
            )
            val offset =
                editManager.calcImageOffset(possibleDrawAreaSize, backgroundImage)
            editManager.captureArea.value = CaptureArea(
                offset.x,
                offset.y,
                backgroundImage.width.toFloat(),
                backgroundImage.height.toFloat()
            )
            editManager.initialDrawAreaOffset = Offset(
                editManager.captureArea.value!!.left,
                editManager.captureArea.value!!.top
            )
            editManager.backgroundImage.value = backgroundImage
        }

        override fun onLoadCleared(placeholder: Drawable?) {}
    })
}

fun resizeByMax(
    actualWidth: Float,
    actualHeight: Float,
    maxWidth: Float,
    maxHeight: Float
): Size {
    val bitmapRatio = actualWidth / actualHeight
    val maxRatio = maxWidth / maxHeight

    var finalWidth = maxWidth
    var finalHeight = maxHeight

    if (maxRatio > bitmapRatio) {
        finalWidth = maxHeight * bitmapRatio
    } else {
        finalHeight = maxWidth / bitmapRatio
    }

    return Size(finalWidth, finalHeight)
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
