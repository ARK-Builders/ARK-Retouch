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
import androidx.compose.ui.unit.IntSize
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
import space.taran.arkretouch.presentation.utils.getOriginalSized
import space.taran.arkretouch.presentation.utils.rotate
import timber.log.Timber
import java.io.File
import java.lang.NullPointerException
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
    var shouldFit = false

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

    fun rotateImage(
        angle: Float = 0f,
        isFixedAngle: Boolean = false,
        applyRotation: Boolean = false
    ) {
        editManager.apply {
            if (!applyRotation) {
                rotationAngle.value += angle
                val horizontalAxisDetectorModulus = (rotationAngle.value / 90f) % 2f
                val oddModulus = horizontalAxisDetectorModulus % 2f
                val isOdd = oddModulus == 1f || oddModulus == -1f
                shouldFit = isOdd && isFixedAngle
            }
            val bitmap = rotationGrid.getBitmap()
            val imgBitmap = bitmap.rotate(
                rotationAngle.value,
                shouldFit,
                resize = { bitmap1, width, height ->
                    resize(
                        bitmap1.asImageBitmap(),
                        width,
                        height
                    ).asAndroidBitmap()
                }
            )
            val result = if (applyRotation && !shouldFit)
                imgBitmap.getOriginalSized(
                    rotationGrid.getCropParams()
                ).asImageBitmap()
            else imgBitmap.asImageBitmap()
            backgroundImage.value = if (applyRotation) resize(
                result,
                drawAreaSize.value.width,
                drawAreaSize.value.height
            )
            else result
            if (!applyRotation)
                rotationGrid.calcRotatedBitmapOffset()
        }
    }

    fun fitBitmapOnRotateGrid(
        bitmap: ImageBitmap,
        width: Int,
        height: Int
    ): ImageBitmap {
        return resize(bitmap, width, height)
    }

    fun downResizeManually(width: Int = 0, height: Int = 0): IntSize {
        var newWidth = width
        var newHeight = height
        if (width > 0) newHeight = (newWidth / editManager.aspectRatio.value).toInt()
        if (height > 0)
            newWidth = (newHeight * editManager.aspectRatio.value).toInt()
        if (newWidth > 0 && newHeight > 0) editManager.apply {
            val bitmapToResize = editManager.resize.getBitmap().asImageBitmap()
            if (
                newWidth <= bitmapToResize.width &&
                newHeight <= bitmapToResize.height
            ) {
                try {
                    val imgBitmap = resize(
                        bitmapToResize,
                        newWidth,
                        newHeight
                    )
                    backgroundImage.value = imgBitmap
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
            }
        }
        return IntSize(
            newWidth,
            newHeight
        )
    }

    fun getImageUri(
        context: Context = DIManager.component.app(),
        bitmap: Bitmap? = null,
        name: String = ""
    ) = getCachedImageUri(context, bitmap, name)

    private fun getCachedImageUri(
        context: Context,
        bitmap: Bitmap? = null,
        name: String = ""
    ): Uri {
        var uri: Uri? = null
        val imageCacheFolder = File(context.cacheDir, "images")
        val imgBitmap = bitmap ?: getCombinedImageBitmap().asAndroidBitmap()
        try {
            imageCacheFolder.mkdirs()
            val file = File(imageCacheFolder, "image$name.png")
            file.outputStream().use { out ->
                imgBitmap
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

    fun getCombinedImageBitmap(): ImageBitmap {
        val bitmap = editManager.backgroundImage.value
        val size = if (bitmap != null)
            IntSize(
                bitmap.width,
                bitmap.height
            )
        else editManager.drawAreaSize.value
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
                Offset(0f, 0f),
                Paint()
            )
        }
        editManager.drawPaths.forEach {
            drawCanvas.drawPath(it.path, it.paint)
        }
        combinedCanvas.drawImage(drawBitmap, Offset.Zero, Paint())
        return combinedBitmap
    }

    fun confirmExit() = viewModelScope.launch {
        exitConfirmed = true
        delay(2_000)
        exitConfirmed = false
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
            editManager.apply {
                backgroundImage.value =
                    resize(bitmap.asImageBitmap(), areaSize.width, areaSize.height)
                setOriginalBackgroundImage(backgroundImage.value)
            }
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
