package dev.arkbuilders.arkretouch.presentation.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.view.MotionEvent
import dev.arkbuilders.arkretouch.data.model.EditingState
import dev.arkbuilders.arkretouch.data.model.ImageViewParams
import dev.arkbuilders.arkretouch.data.model.Resolution
import dev.arkbuilders.arkretouch.data.repo.OldStorageRepository
import dev.arkbuilders.arkretouch.editing.Operation
import dev.arkbuilders.arkretouch.editing.crop.CropOperation
import dev.arkbuilders.arkretouch.editing.manager.EditManager
import dev.arkbuilders.arkretouch.editing.manager.EditingMode
import dev.arkbuilders.arkretouch.editing.resize.ResizeOperation
import dev.arkbuilders.arkretouch.editing.rotate.RotateOperation
import dev.arkbuilders.arkretouch.utils.loadImageWithPath
import dev.arkbuilders.arkretouch.utils.loadImageWithUri
import timber.log.Timber
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EditViewModel(
    private val primaryColor: Long,
    private val launchedFromIntent: Boolean,
    private val imagePath: Path?,
    private val imageUri: String?,
    private val maxResolution: Resolution,
    private val prefs: OldStorageRepository
) : ViewModel() {

    private val _usedColors = mutableListOf<Color>()

    private var _editingState: EditingState by mutableStateOf(EditingState.DEFAULT.copy(usedColors = _usedColors))
    val editingState: EditingState get() = _editingState

    val editManager = EditManager()

    val imageSize: IntSize
        get() = with(editManager) {
            if (isResizing())
                backgroundImage2.value?.let {
                    IntSize(it.width, it.height)
                } ?: originalBackgroundImage.value?.let {
                    IntSize(it.width, it.height)
                } ?: resolution.value?.toIntSize()!!
            else
                backgroundImage.value?.let {
                    IntSize(it.width, it.height)
                } ?: resolution.value?.toIntSize() ?: drawAreaSize.value
        }

    private val cropOperation = CropOperation(editManager) {
        toggleDraw()
    }

    private val rotateOperation = RotateOperation(editManager) {
        toggleDraw()
    }

    private val resizeOperation = ResizeOperation(editManager) {
        toggleDraw()
    }

    init {
        viewModelScope.launch {
            if (imageUri == null && imagePath == null) {
                editManager.initDefaults(
                    prefs.readDefaults(),
                    maxResolution
                )
            }
            editManager.setImageSize(imageSize)
            loadDefaultPaintColor()
        }
    }

    fun toggleMenus() {
        showMenus(!editingState.menusVisible)
    }

    fun showMenus(bool: Boolean) {
        _editingState = editingState.copy(menusVisible = bool)
    }

    fun setStrokeWidth(width: Float) {
        _editingState = editingState.copy(strokeWidth = width)
    }

    fun showSavePathDialog(bool: Boolean) {
        _editingState = editingState.copy(showSavePathDialog = bool)
    }

    fun showExitDialog(bool: Boolean) {
        _editingState = editingState.copy(showExitDialog = bool)
    }

    fun showMoreOptions(bool: Boolean) {
        _editingState = editingState.copy(showMoreOptionsPopup = bool)
    }

    fun showEyeDropperHint(bool: Boolean) {
        _editingState = editingState.copy(showEyeDropperHint = bool)
    }

    fun showConfirmClearDialog(bool: Boolean) {
        _editingState = editingState.copy(showConfirmClearDialog = bool)
    }

    fun setIsLoaded(bool: Boolean) {
        _editingState = editingState.copy(isLoaded = bool)
    }

    fun setBottomButtonsScrollIsAtStart(bool: Boolean) {
        _editingState = editingState.copy(bottomButtonsScrollIsAtStart = bool)
    }
    fun setBottomButtonsScrollIsAtEnd(bool: Boolean) {
        _editingState = editingState.copy(bottomButtonsScrollIsAtEnd = bool)
    }

    fun setStrokeSliderExpanded(isExpanded: Boolean) {
        _editingState = editingState.copy(strokeSliderExpanded = isExpanded)
    }

    fun loadImage(loadByPath: (Path, EditManager) -> Unit, loadByUri: (String, EditManager) -> Unit) {
        _editingState = editingState.copy(isLoaded = true)
        imagePath?.let {
            loadByPath(it, editManager)
            return
        }
        imageUri?.let {
            loadByUri(it, editManager)
            return
        }
        editManager.scaleToFit()
    }

    fun saveImage(context: Context, path: Path) {
        viewModelScope.launch(Dispatchers.IO) {
            _editingState = editingState.copy(isSavingImage = true)
            val combinedBitmap = getEditedImage()

            path.outputStream().use { out ->
                combinedBitmap.asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(path.toString()),
                arrayOf("image/*")
            ) { _, _ -> }
            _editingState = editingState.copy(imageSaved = true, isSavingImage = false, showSavePathDialog = false)
        }
    }

    fun shareImage(root: Path, provideUri: (File) -> Uri, startShare: (Intent) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_SEND)
            val tempPath = createTempFile(createTempDirectory(root.resolve("images/")), suffix = ".png")
            val bitmap = getEditedImage().asAndroidBitmap()
            tempPath.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, provideUri(tempPath.toFile()))
            startShare(intent)
        }
    }

    fun trackColor(color: Color) {
        _usedColors.remove(color)
        _usedColors.add(color)

        val excess = _usedColors.size - KEEP_USED_COLORS
        repeat(excess) {
            _usedColors.removeFirst()
        }

        viewModelScope.launch {
            prefs.persistUsedColors(editingState.usedColors)
        }
    }

    fun toggleEyeDropper() {
        editManager.toggleEyeDropper()
    }

    fun cancelEyeDropper() {
        editManager.setPaintColor(editingState.usedColors.last())
    }

    fun applyEyeDropper(action: Int, x: Int, y: Int) {
        try {
            val bitmap = getEditedImage().asAndroidBitmap()
            val imageX = (x * editManager.bitmapScale.x).toInt()
            val imageY = (y * editManager.bitmapScale.y).toInt()
            val pixel = bitmap.getPixel(imageX, imageY)
            val color = Color(pixel)
            if (color == Color.Transparent) {
                showEyeDropperHint(true)
                return
            }
            when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                    trackColor(color)
                    toggleEyeDropper()
                    showMenus(true)
                }
            }
            editManager.setPaintColor(color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCombinedImageBitmap(): ImageBitmap {
        val size = imageSize
        val drawBitmap = ImageBitmap(
            size.width,
            size.height,
            ImageBitmapConfig.Argb8888
        )
        val combinedBitmap =
            ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888)

        val time = measureTimeMillis {
            val backgroundPaint = Paint().also {
                it.color = editManager.backgroundColor.value
            }
            val drawCanvas = Canvas(drawBitmap)
            val combinedCanvas = Canvas(combinedBitmap)
            val matrix = Matrix().apply {
                if (editManager.rotationAngles.isNotEmpty()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    setRotate(
                        editManager.rotationAngle.value,
                        centerX.toFloat(),
                        centerY.toFloat()
                    )
                }
            }
            combinedCanvas.drawRect(
                Rect(Offset.Zero, size.toSize()),
                backgroundPaint
            )
            combinedCanvas.nativeCanvas.setMatrix(matrix)
            editManager.backgroundImage.value?.let {
                combinedCanvas.drawImage(
                    it,
                    Offset.Zero,
                    Paint()
                )
            }
            editManager.drawPaths.forEach {
                drawCanvas.drawPath(it.path, it.paint)
            }
            combinedCanvas.drawImage(drawBitmap, Offset.Zero, Paint())
        }
        Timber.tag("edit-viewmodel: getCombinedImageBitmap").d(
            "processing edits took ${time / 1000} s ${time % 1000} ms"
        )
        return combinedBitmap
    }

    fun getEditedImage(): ImageBitmap {
        val size = imageSize
        var bitmap = ImageBitmap(
            size.width,
            size.height,
            ImageBitmapConfig.Argb8888
        )
        var pathBitmap: ImageBitmap? = null
        val time = measureTimeMillis {
            editManager.apply {
                val matrix = Matrix()
                if (editManager.drawPaths.isNotEmpty()) {
                    pathBitmap = ImageBitmap(
                        size.width,
                        size.height,
                        ImageBitmapConfig.Argb8888
                    )
                    val pathCanvas = Canvas(pathBitmap!!)
                    editManager.drawPaths.forEach {
                        pathCanvas.drawPath(it.path, it.paint)
                    }
                }
                backgroundImage.value?.let {
                    val canvas = Canvas(bitmap)
                    if (prevRotationAngle == 0f && drawPaths.isEmpty()) {
                        bitmap = it
                        return@let
                    }
                    if (prevRotationAngle != 0f) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        matrix.setRotate(prevRotationAngle, centerX, centerY)
                    }
                    canvas.nativeCanvas.drawBitmap(
                        it.asAndroidBitmap(),
                        matrix,
                        null
                    )
                    if (drawPaths.isNotEmpty()) {
                        canvas.nativeCanvas.drawBitmap(
                            pathBitmap?.asAndroidBitmap()!!,
                            matrix,
                            null
                        )
                    }
                } ?: run {
                    val canvas = Canvas(bitmap)
                    if (prevRotationAngle != 0f) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        matrix.setRotate(
                            prevRotationAngle,
                            centerX.toFloat(),
                            centerY.toFloat()
                        )
                        canvas.nativeCanvas.setMatrix(matrix)
                    }
                    canvas.drawRect(
                        Rect(Offset.Zero, size.toSize()),
                        backgroundPaint
                    )
                    if (drawPaths.isNotEmpty()) {
                        canvas.drawImage(
                            pathBitmap!!,
                            Offset.Zero,
                            Paint()
                        )
                    }
                }
            }
        }
        Timber.tag("edit-viewmodel: getEditedImage").d(
            "processing edits took ${time / 1000} s ${time % 1000} ms"
        )
        return bitmap
    }

    fun confirmExit() = viewModelScope.launch {
        _editingState = editingState.copy(exitConfirmed = true, isLoaded = false)
        delay(2_000)
        _editingState = editingState.copy(exitConfirmed = false, isLoaded = true)
    }

    fun applyOperation() {
        applyEdit()
    }

    fun cancelOperation() {
        editManager.apply {
            if (isRotating()) {
                toggleDraw()
                cancelRotateMode()
            }
            if (isCropping()) {
                toggleDraw()
                cancelCropMode()
            }
            if (isResizing()) {
                toggleDraw()
                cancelResizeMode()
            }
            if (isEyeDropperMode.value) {
                toggleEyeDropper()
                cancelEyeDropper()
            }
            if (isBlurMode.value) {
                toggleBlurMode()
                blurOperation.cancel()
            }
            showMenus(true)
            scaleToFit()
        }
    }

    fun persistDefaults(color: Color, resolution: Resolution) {
        viewModelScope.launch {
            prefs.persistDefaults(color, resolution)
        }
    }

    fun toggleDraw() {
        _editingState = editingState.copy(mode = EditingMode.DRAW)
    }

    fun toggleErase() {
        _editingState = editingState.copy(mode = EditingMode.ERASE)
    }

    fun toggleCrop() {
        _editingState = editingState.copy(mode = EditingMode.CROP)
    }

    fun toggleResize() {
        _editingState = editingState.copy(mode = EditingMode.RESIZE)
    }

    fun toggleRotate() {
        _editingState = editingState.copy(mode = EditingMode.ROTATE)
    }

    fun toggleZoom() {
        _editingState = editingState.copy(mode = EditingMode.ZOOM)
    }

    fun togglePan() {
        _editingState = editingState.copy(mode = EditingMode.PAN)
    }

    fun toggleBlur() {
        _editingState = editingState.copy(mode = EditingMode.BLUR)
    }

    fun isCropping(): Boolean = editingState.mode == EditingMode.CROP

    fun isRotating(): Boolean = editingState.mode == EditingMode.ROTATE

    fun isResizing(): Boolean = editingState.mode == EditingMode.RESIZE

    fun onRotate(angle: Float) {
        editManager.apply {
            this@EditViewModel.rotateOperation.onRotate(angle)
        }
    }

    fun onDrawContainerSizeChanged(newSize: IntSize, context: Context) {
        viewModelScope.launch {
            if (newSize == IntSize.Zero) return@launch
            if (editingState.showSavePathDialog) return@launch
            editManager.drawAreaSize.value = newSize
            if (editingState.isLoaded) {
                editManager.apply {
                    when (true) {
                        isCropping() -> {
                            cropWindow.updateOnDrawAreaSizeChange(newSize)
                            return@launch
                        }

                        isResizing() -> {
                            if (
                                backgroundImage.value?.width ==
                                this@EditViewModel.imageSize.width &&
                                backgroundImage.value?.height ==
                                this@EditViewModel.imageSize.height
                            ) {
                                val editMatrixScale = scaleToFitOnEdit().scale
                                this@EditViewModel.resizeOperation
                                    .updateEditMatrixScale(editMatrixScale)
                            }
                            /* if (resizeOperation.isApplied()) {
                                resizeOperation.resetApply()
                            }*/
                            return@launch
                        }

                        isRotating() -> {
                            scaleToFitOnEdit(isRotating = true)
                            return@launch
                        }

                        isZoomMode.value -> {
                            return@launch
                        }

                        else -> {
                            scaleToFit()
                            return@launch
                        }
                    }
                }
            }
            loadImage(
                loadByPath = { path, editManager -> loadImageWithPath(context, path, editManager) },
                loadByUri = { uri, editManager -> loadImageWithUri(context, uri, editManager) }
            )
        }
    }

    private fun applyEdit() {
        val operation: Operation = with(editManager) {
            when (editingState.mode) {
                EditingMode.CROP -> this@EditViewModel.cropOperation
                EditingMode.RESIZE -> this@EditViewModel.resizeOperation
                EditingMode.ROTATE -> this@EditViewModel.rotateOperation
                EditingMode.BLUR -> blurOperation
                else -> drawOperation
            }
        }
        operation.apply()
        if (operation != editManager.drawOperation) { showMenus(true) }
    }

    private fun loadDefaultPaintColor() {
        viewModelScope.launch {
            _usedColors.addAll(prefs.readUsedColors())

            val color = if (_usedColors.isNotEmpty()) {
                _usedColors.last()
            } else {
                Color(primaryColor.toULong()).also { _usedColors.add(it) }
            }

            editManager.setPaintColor(color)
        }
    }

    companion object {
        private const val KEEP_USED_COLORS = 20
    }
}

fun resize(
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

fun fitImage(
    imageBitmap: ImageBitmap,
    maxWidth: Int,
    maxHeight: Int
): ImageViewParams {
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
    return ImageViewParams(
        IntSize(
            finalWidth,
            finalHeight,
        ),
        ResizeOperation.Scale(
            finalWidth.toFloat() / width.toFloat(),
            finalHeight.toFloat() / height.toFloat()
        )
    )
}

fun fitBackground(
    resolution: IntSize,
    maxWidth: Int,
    maxHeight: Int
): ImageViewParams {

    val width = resolution.width
    val height = resolution.height

    val resolutionRatio = width.toFloat() / height.toFloat()
    val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()

    var finalWidth = maxWidth
    var finalHeight = maxHeight

    if (maxRatio > resolutionRatio) {
        finalWidth = (maxHeight.toFloat() * resolutionRatio).toInt()
    } else {
        finalHeight = (maxWidth.toFloat() / resolutionRatio).toInt()
    }
    return ImageViewParams(
        IntSize(
            finalWidth,
            finalHeight,
        ),
        ResizeOperation.Scale(
            finalWidth.toFloat() / width.toFloat(),
            finalHeight.toFloat() / height.toFloat()
        )
    )
}