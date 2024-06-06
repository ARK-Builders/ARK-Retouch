package dev.arkbuilders.arkretouch.presentation.viewmodels

import androidx.compose.runtime.State
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
import dev.arkbuilders.arkretouch.data.model.DrawPath
import dev.arkbuilders.arkretouch.data.model.DrawingState
import dev.arkbuilders.arkretouch.data.model.EditingState
import dev.arkbuilders.arkretouch.data.model.ImageViewParams
import dev.arkbuilders.arkretouch.data.model.Resolution
import dev.arkbuilders.arkretouch.data.repo.OldStorageRepository
import dev.arkbuilders.arkretouch.editing.Operation
import dev.arkbuilders.arkretouch.editing.blur.BlurOperation
import dev.arkbuilders.arkretouch.editing.crop.CropOperation
import dev.arkbuilders.arkretouch.editing.draw.DrawOperation
import dev.arkbuilders.arkretouch.editing.manager.EditManager
import dev.arkbuilders.arkretouch.editing.manager.EditingMode
import dev.arkbuilders.arkretouch.editing.resize.ResizeOperation
import dev.arkbuilders.arkretouch.editing.rotate.RotateOperation
import dev.arkbuilders.arkretouch.utils.copy
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

    var editingState: EditingState by mutableStateOf(EditingState.DEFAULT.copy(usedColors = _usedColors))
        private set

    var drawingState: DrawingState by mutableStateOf(DrawingState())
        private set

    val editManager = EditManager()

    val imageSize: IntSize
        get() = with(editManager) {
            val size = if (isResizing())
                backgroundImage2.value?.let {
                    IntSize(it.width, it.height)
                } ?: originalBackgroundImage.value?.let {
                    IntSize(it.width, it.height)
                } ?: resolution.value?.toIntSize()!!
            else
                backgroundImage.value?.let {
                    IntSize(it.width, it.height)
                } ?: resolution.value?.toIntSize() ?: drawAreaSize.value
            editManager.setImageSize(size)
            size
        }

    private val drawOperation = DrawOperation(editManager)

    private val cropOperation = CropOperation(editManager) {
        clearRedo()
        updateUndoRedoState()
        toggleDraw()
    }

    private val rotateOperation = RotateOperation(editManager) {
        clearRedo()
        updateUndoRedoState()
        toggleDraw()
    }

    private val resizeOperation = ResizeOperation(editManager) {
        clearRedo()
        updateUndoRedoState()
        toggleDraw()
    }

    private val blurOperation = BlurOperation(editManager) {
        clearRedo()
        updateUndoRedoState()
        toggleDraw()
    }

    init {
        viewModelScope.launch {
            if (imageUri == null && imagePath == null) {
                editManager.initDefaults(
                    prefs.readDefaults(),
                    maxResolution
                )
                editManager.setImageSize(imageSize)
            }
            loadDefaultPaintColor()
        }
    }

    fun toggleMenus() {
        showMenus(!editingState.menusVisible)
    }

    fun showMenus(bool: Boolean) {
        editingState = editingState.copy(menusVisible = bool)
    }

    fun setStrokeWidth(width: Float) {
        editingState = editingState.copy(strokeWidth = width)
    }

    fun showSavePathDialog(bool: Boolean) {
        editingState = editingState.copy(showSavePathDialog = bool)
    }

    fun showExitDialog(bool: Boolean) {
        editingState = editingState.copy(showExitDialog = bool)
    }

    fun showMoreOptions(bool: Boolean) {
        editingState = editingState.copy(showMoreOptionsPopup = bool)
    }

    fun showEyeDropperHint(bool: Boolean) {
        editingState = editingState.copy(showEyeDropperHint = bool)
    }

    fun showConfirmClearDialog(bool: Boolean) {
        editingState = editingState.copy(showConfirmClearDialog = bool)
    }

    fun showColorDialog(bool: Boolean) {
        if (isRotating() && isResizing() && isCropping() && isErasing() && isBlurring()) return
        if (isEyeDropping() && bool) {
            toggleDraw()
        }
        editingState = editingState.copy(showColorDialog = bool)
    }

    fun setIsLoaded(bool: Boolean) {
        editingState = editingState.copy(isLoaded = bool)
    }

    fun setBottomButtonsScrollIsAtStart(bool: Boolean) {
        editingState = editingState.copy(bottomButtonsScrollIsAtStart = bool)
    }
    fun setBottomButtonsScrollIsAtEnd(bool: Boolean) {
        editingState = editingState.copy(bottomButtonsScrollIsAtEnd = bool)
    }

    fun setStrokeSliderExpanded(isExpanded: Boolean) {
        editingState = editingState.copy(strokeSliderExpanded = isExpanded)
    }

    fun loadImage(loadByPath: (Path, EditManager) -> Unit, loadByUri: (String, EditManager) -> Unit) {
        editingState = editingState.copy(isLoaded = true)
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
            editingState = editingState.copy(isSavingImage = true)
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
            editingState = editingState.copy(imageSaved = true, isSavingImage = false, showSavePathDialog = false)
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

    fun cancelEyeDropper() {
        onSetPaintColor(editingState.usedColors.last())
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
            onSetPaintColor(color)
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
                it.color = drawingState.backgroundPaint.color
            }
            val drawCanvas = Canvas(drawBitmap)
            val combinedCanvas = Canvas(combinedBitmap)
            val matrix = Matrix().apply {
                if (editManager.rotationAngles.isNotEmpty()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    setRotate(
                        editManager.rotationAngle.floatValue,
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
                        drawingState.backgroundPaint
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
        editingState = editingState.copy(exitConfirmed = true, isLoaded = false)
        delay(2_000)
        editingState = editingState.copy(exitConfirmed = false, isLoaded = true)
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
            if (isEyeDropping()) {
                this@EditViewModel.toggleEyeDropper()
            }
            if (isBlurring()) {
                toggleDraw()
                this@EditViewModel.blurOperation.cancel()
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
        editingState = editingState.copy(mode = EditingMode.DRAW)
    }

    fun toggleErase() {
        if (isErasing()) {
            toggleDraw()
            return
        }
        editingState = editingState.copy(mode = EditingMode.ERASE)
    }

    fun toggleCrop() {
        editingState = editingState.copy(mode = EditingMode.CROP)
    }

    fun toggleResize() {
        showMenus(!isResizing())
        editingState = editingState.copy(mode = EditingMode.RESIZE)
        editManager.setBackgroundImage2()
        val imgBitmap = getEditedImage()
        editManager.backgroundImage.value = imgBitmap
        resizeOperation.init(imgBitmap.asAndroidBitmap())
    }

    fun toggleRotate() {
        editingState = editingState.copy(mode = EditingMode.ROTATE)
    }

    fun toggleZoom() {
        if (isZooming()) {
            toggleDraw()
            return
        }
        editingState = editingState.copy(mode = EditingMode.ZOOM)
    }

    fun togglePan() {
        if (isPanning()) {
            toggleDraw()
            return
        }
        editingState = editingState.copy(mode = EditingMode.PAN)
    }

    fun toggleBlur() {
        editingState = editingState.copy(mode = EditingMode.BLUR)
        editManager.setBackgroundImage2()
        editManager.backgroundImage.value = getEditedImage()
        blurOperation.init()
    }

    fun toggleEyeDropper() {
        if (isEyeDropping()) {
            cancelEyeDropper()
            toggleDraw()
            return
        }
        editingState = editingState.copy(mode = EditingMode.EYEDROPPER)
        showColorDialog(false)
    }

    fun isCropping(): Boolean = editingState.mode == EditingMode.CROP

    fun isRotating(): Boolean = editingState.mode == EditingMode.ROTATE

    fun isResizing(): Boolean = editingState.mode == EditingMode.RESIZE

    fun isErasing(): Boolean = editingState.mode == EditingMode.ERASE

    fun isZooming(): Boolean = editingState.mode == EditingMode.ZOOM

    fun isPanning(): Boolean = editingState.mode == EditingMode.PAN

    fun isBlurring(): Boolean = editingState.mode == EditingMode.BLUR

    fun isEyeDropping(): Boolean = editingState.mode == EditingMode.EYEDROPPER

    fun onRotate(angle: Float) {
        editManager.apply {
            this@EditViewModel.rotateOperation.onRotate(angle)
        }
    }

    fun onResizeDown(width: Int = 0, height: Int = 0): IntSize {
        return resizeOperation.resizeDown(width, height) {
            editManager.backgroundImage.value = it
        }
    }

    fun onBlurSizeChange(size: Float) {
        drawingState = drawingState.copy(blurSize = size)
        blurOperation.setSize(drawingState.blurSize)
        blurOperation.resize()
    }

    fun onBlurIntensityChange(intensity: Float) {
        drawingState = drawingState.copy(blurIntensity = intensity)
        blurOperation.setIntensity(drawingState.blurIntensity)
    }

    fun onBlurMove(position: Offset, delta: Offset) {
        blurOperation.move(position, delta)
    }

    fun onDrawBlur(context: Context, canvas: Canvas) {
        blurOperation.draw(context, canvas)
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
                            cropWindow.updateOnDrawAreaSizeChange()
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
                            return@launch
                        }

                        isRotating() -> {
                            scaleToFitOnEdit(isRotating = true)
                            return@launch
                        }

                        isZooming() -> {
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

    fun onSetPaintColor(color: Color) {
        drawingState = drawingState.copy(
            drawPaint = drawingState.drawPaint.copy().also {
                it.color = color
            }
        )
    }

    fun onSetPaintStrokeWidth(strokeWidth: Float) {
        drawingState.drawPaint.strokeWidth = strokeWidth
        drawingState.erasePaint.strokeWidth = strokeWidth
    }

    fun onSetBackgroundColor(color: Color) {
        drawingState.backgroundPaint.color = color
    }

    fun onDrawPath(path: androidx.compose.ui.graphics.Path) {
        clearRedo()
        editManager.addDrawPath(
            DrawPath(
                path,
                if (isErasing()) drawingState.erasePaint.copy() else drawingState.drawPaint.copy()
            )
        )
    }

    fun onUndoClick() {
        if (editingState.canUndo) {
            editManager.undo { task ->
                operationByTask(task)
            }
        }
        updateUndoRedoState()
        editManager.invalidate()
    }

    fun onRedoClick() {
        if (editingState.canRedo) {
            editManager.redo { task ->
                operationByTask(task)
            }
        }
        updateUndoRedoState()
        editManager.invalidate()
    }

    fun updateUndoRedoState() {
        editManager.updateRevised { canUndo, canRedo ->
            editingState = editingState.copy(canUndo = canUndo, canRedo = canRedo)
        }
    }

    fun onClearEditsConfirm() {
        blurOperation.clear()
        editManager.clearEdits()
        updateUndoRedoState()
    }

    private fun clearRedo() {
        if (editingState.canRedo) {
            editManager.clearRedo()
        }
    }

    private fun operationByTask(task: String): Operation = when (task) {
        EditManager.ROTATE -> rotateOperation
        EditManager.RESIZE -> resizeOperation
        EditManager.CROP -> cropOperation
        EditManager.BLUR -> blurOperation
        else -> drawOperation
    }

    private fun applyEdit() {
        val operation: Operation = when (editingState.mode) {
            EditingMode.CROP -> cropOperation
            EditingMode.RESIZE -> resizeOperation
            EditingMode.ROTATE -> rotateOperation
            EditingMode.BLUR -> blurOperation
            else -> drawOperation
        }
        operation.apply()
        if (operation != drawOperation) { showMenus(true) }
    }

    private fun loadDefaultPaintColor() {
        viewModelScope.launch {
            _usedColors.addAll(prefs.readUsedColors())

            val color = if (_usedColors.isNotEmpty()) {
                _usedColors.last()
            } else {
                Color(primaryColor.toULong()).also { _usedColors.add(it) }
            }

            onSetPaintColor(color)
        }
    }

    fun invalidateCanvas() {
        editManager.invalidate()
    }

    companion object {
        private const val KEEP_USED_COLORS = 20
    }

    fun observeCanvasInvalidator(): State<Int> = editManager.invalidatorTick
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