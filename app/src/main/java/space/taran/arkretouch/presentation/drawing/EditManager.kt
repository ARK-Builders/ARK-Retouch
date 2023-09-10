package space.taran.arkretouch.presentation.drawing

import android.graphics.Matrix
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.IntSize
import space.taran.arkretouch.data.ImageDefaults
import space.taran.arkretouch.data.Resolution
import space.taran.arkretouch.presentation.edit.ImageViewParams
import space.taran.arkretouch.presentation.edit.Operation
import space.taran.arkretouch.presentation.edit.blur.BlurOperation
import space.taran.arkretouch.presentation.edit.crop.CropOperation
import timber.log.Timber
import space.taran.arkretouch.presentation.edit.crop.CropWindow
import space.taran.arkretouch.presentation.edit.draw.DrawOperation
import space.taran.arkretouch.presentation.edit.fit
import space.taran.arkretouch.presentation.edit.resize.ResizeOperation
import space.taran.arkretouch.presentation.edit.rotate.RotateOperation
import java.util.Stack

class EditManager {
    private val drawPaint: MutableState<Paint> = mutableStateOf(defaultPaint())

    private val _paintColor: MutableState<Color> =
        mutableStateOf(drawPaint.value.color)
    val paintColor: State<Color> = _paintColor

    private val erasePaint: Paint = Paint().apply {
        shader = null
        color = Color.Transparent
        style = PaintingStyle.Stroke
        blendMode = BlendMode.SrcOut
    }

    private val _smartLayout = mutableStateOf(false)
    val smartLayout: State<Boolean> = _smartLayout
    var smartSwitchInitiated = false
    var showSwitchLayoutButton = mutableStateOf(false)

    val blurIntensity = mutableStateOf(12f)

    val cropWindow = CropWindow(this)

    val drawOperation = DrawOperation(this)
    val resizeOperation = ResizeOperation(this)
    val rotateOperation = RotateOperation(this)
    val cropOperation = CropOperation(this)
    val blurOperation = BlurOperation(this)

    private val currentPaint: Paint
        get() = when (true) {
            isEraseMode.value -> erasePaint
            else -> drawPaint.value
        }

    val drawPaths = Stack<DrawPath>()
    val redoPaths = Stack<DrawPath>()

    val backgroundImage = mutableStateOf<ImageBitmap?>(null)
    private val _backgroundColor = mutableStateOf(Color.Transparent)
    val backgroundColor: State<Color> = _backgroundColor
    val backgroundImage2 = mutableStateOf<ImageBitmap?>(null)
    private val originalBackgroundImage = mutableStateOf<ImageBitmap?>(null)

    val matrix = Matrix()
    val editMatrix = Matrix()
    lateinit var matrixScale: ResizeOperation.Scale
        private set
    lateinit var bitmapScale: ResizeOperation.Scale
        private set

    var imageSize: IntSize = IntSize.Zero
        private set
    val imageSizes = Stack<IntSize>()
    private val redoImageSizes = Stack<IntSize>()

    private val _resolution = mutableStateOf<Resolution?>(null)
    val resolution: State<Resolution?> = _resolution
    var drawAreaSize = mutableStateOf(IntSize.Zero)
    val availableDrawAreaSize = mutableStateOf(IntSize.Zero)

    var invalidatorTick = mutableStateOf(0)

    private val _isEraseMode: MutableState<Boolean> = mutableStateOf(false)
    val isEraseMode: State<Boolean> = _isEraseMode

    private val _canUndo: MutableState<Boolean> = mutableStateOf(false)
    val canUndo: State<Boolean> = _canUndo

    private val _canRedo: MutableState<Boolean> = mutableStateOf(false)
    val canRedo: State<Boolean> = _canRedo

    private val _isRotateMode = mutableStateOf(false)
    val isRotateMode = _isRotateMode

    private val _isResizeMode = mutableStateOf(false)
    val isResizeMode = _isResizeMode

    private val _isEyeDropperMode = mutableStateOf(false)
    val isEyeDropperMode = _isEyeDropperMode

    private val _isBlurMode = mutableStateOf(false)
    val isBlurMode = _isBlurMode

    val rotationAngle = mutableStateOf(0F)
    var prevRotationAngle = 0f

    private val editedPaths = Stack<Stack<DrawPath>>()

    val redoResize = Stack<ImageBitmap>()
    val resizes = Stack<ImageBitmap>()
    val rotationAngles = Stack<Float>()
    val redoRotationAngles = Stack<Float>()

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    private val _isCropMode = mutableStateOf(false)
    val isCropMode = _isCropMode

    val cropStack = Stack<ImageBitmap>()
    val redoCropStack = Stack<ImageBitmap>()

    fun applyOperation() {
        val operation: Operation =
            when (true) {
                isRotateMode.value -> rotateOperation
                isCropMode.value -> cropOperation
                isBlurMode.value -> blurOperation
                isResizeMode.value -> resizeOperation
                else -> drawOperation
            }
        operation.apply {
            if (!isRotateMode.value) {
                imageSizes.push(imageSize)
                updateImageSize()
            }
        }
    }

    private fun undoOperation(operation: Operation) {
        if (imageSizes.isNotEmpty()) {
            redoImageSizes.push(imageSize)
            imageSize = imageSizes.pop()
        }
        operation.undo()
    }

    private fun redoOperation(operation: Operation) {
        if (redoImageSizes.isNotEmpty()) {
            imageSizes.push(imageSize)
            imageSize = redoImageSizes.pop()
        }
        operation.redo()
    }

    fun updateImageSize(size: IntSize = IntSize.Zero) {
        if (size != IntSize.Zero) {
            imageSize = size
            return
        }
        imageSize = if (isResizeMode.value)
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

    fun scaleToFit() {
        val viewParams = fit(
            imageSize,
            drawAreaSize.value.width,
            drawAreaSize.value.height
        )
        matrixScale = viewParams.scale
        scaleMatrix(viewParams)
        updateAvailableDrawArea(viewParams.drawArea)
        updateBitmapScale(viewParams)
    }

    fun scaleToFitOnEdit(
        maxWidth: Int = drawAreaSize.value.width,
        maxHeight: Int = drawAreaSize.value.height
    ): ImageViewParams {
        val viewParams = fit(imageSize, maxWidth, maxHeight)
        scaleEditMatrix(viewParams)
        updateAvailableDrawArea(viewParams.drawArea)
        updateBitmapScale(viewParams)
        return viewParams
    }

    private fun scaleMatrix(viewParams: ImageViewParams) {
        matrix.setScale(viewParams.scale.x, viewParams.scale.y)
        if (prevRotationAngle != 0f) {
            val centerX = viewParams.drawArea.width / 2f
            val centerY = viewParams.drawArea.height / 2f
            val offset = calcOffset(scale = viewParams.scale)
            matrix.postTranslate(offset.x, offset.y)
            matrix.postRotate(prevRotationAngle, centerX, centerY)
        }
    }

    private fun scaleEditMatrix(viewParams: ImageViewParams) {
        editMatrix.setScale(viewParams.scale.x, viewParams.scale.y)
        if (isRotateMode.value) {
            val centerX = viewParams.drawArea.width / 2f
            val centerY = viewParams.drawArea.height / 2f
            val offset = calcOffset(scale = viewParams.scale)
            editMatrix.postTranslate(offset.x, offset.y)
            editMatrix.postRotate(rotationAngle.value, centerX, centerY)
        }
    }
    fun setBackgroundColor(color: Color) {
        _backgroundColor.value = color
    }

    fun setImageResolution(value: Resolution) {
        _resolution.value = value
    }

    fun initDefaults(defaults: ImageDefaults, maxResolution: Resolution) {
        defaults.resolution?.let {
            _resolution.value = it
        }
        if (resolution.value == null)
            _resolution.value = maxResolution
        _backgroundColor.value = Color(defaults.colorValue)
    }

    fun updateBitmapScale(viewParams: ImageViewParams) {
        val bitmapXScale =
            imageSize.width.toFloat() / viewParams.drawArea.width.toFloat()
        val bitmapYScale =
            imageSize.height.toFloat() / viewParams.drawArea.height.toFloat()
        bitmapScale = ResizeOperation.Scale(
            bitmapXScale,
            bitmapYScale
        )
    }
    fun switchLayout(): ImageViewParams {
        val flippedImageSize = IntSize(this.imageSize.height, this.imageSize.width)
        updateImageSize(flippedImageSize)
        val viewParams = fit(
            flippedImageSize,
            drawAreaSize.value.width,
            drawAreaSize.value.height
        )
        updateAvailableDrawArea(viewParams.drawArea)
        return viewParams
    }

    fun toggleSmartLayout() {
        _smartLayout.value = !smartLayout.value
    }

    fun calcOffset(
        drawArea: IntSize = availableDrawAreaSize.value,
        scale: ResizeOperation.Scale = ResizeOperation.Scale(1f, 1f)
    ): Offset {
        val imageSize = backgroundImage.value?.let {
            IntSize(it.width, it.height)
        } ?: resolution.value.let { IntSize(it?.width!!, it.height) }
        val xOffset = (drawArea.width - (imageSize.width * scale.x)) / 2
        val yOffset = (drawArea.height - (imageSize.height * scale.y)) / 2
        return Offset(xOffset, yOffset)
    }
    fun updateAvailableDrawAreaByMatrix() {
        val drawArea = backgroundImage.value?.let {
            val drawWidth = it.width * matrixScale.x
            val drawHeight = it.height * matrixScale.y
            IntSize(
                drawWidth.toInt(),
                drawHeight.toInt()
            )
        } ?: run {
            val drawWidth = resolution.value?.width!! * matrixScale.x
            val drawHeight = resolution.value?.height!! * matrixScale.y
            IntSize(
                drawWidth.toInt(),
                drawHeight.toInt()
            )
        }
        updateAvailableDrawArea(drawArea)
    }
    fun updateAvailableDrawArea(bitmap: ImageBitmap? = backgroundImage.value) {
        if (bitmap == null) {
            resolution.value?.let {
                availableDrawAreaSize.value = it.toIntSize()
            }
            return
        }
        availableDrawAreaSize.value = IntSize(
            bitmap.width,
            bitmap.height
        )
    }
    fun updateAvailableDrawArea(area: IntSize) {
        availableDrawAreaSize.value = area
    }

    internal fun clearRedoPath() {
        redoPaths.clear()
    }

    fun toggleEyeDropper() {
        _isEyeDropperMode.value = !isEyeDropperMode.value
    }

    fun updateRevised() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun resizeDown(width: Int = 0, height: Int = 0) =
        resizeOperation.resizeDown(width, height) {
            backgroundImage.value = it
        }

    fun rotate(angle: Float, toggleLayoutSwitch: () -> Unit) {
        val centerX = availableDrawAreaSize.value.width / 2
        val centerY = availableDrawAreaSize.value.height / 2
        if (isRotateMode.value) {
            rotationAngle.value += angle
            rotateOperation.rotate(
                editMatrix,
                angle,
                centerX.toFloat(),
                centerY.toFloat(),
                toggleLayoutSwitch
            )
        }
        rotateOperation.rotate(
            matrix,
            angle,
            centerX.toFloat(),
            centerY.toFloat(),
        ) {}
    }

    fun addRotation(scale: ResizeOperation.Scale) {
        if (canRedo.value) clearRedo()
        rotationAngles.add(prevRotationAngle)
        undoStack.add(ROTATE)
        prevRotationAngle = rotationAngle.value
        matrixScale = scale
        updateRevised()
    }

    private fun addAngle() {
        rotationAngles.add(prevRotationAngle)
    }

    fun addResize() {
        if (canRedo.value) clearRedo()
        resizes.add(backgroundImage2.value)
        undoStack.add(RESIZE)
        keepEditedPaths()
        updateRevised()
    }

    fun keepEditedPaths() {
        val stack = Stack<DrawPath>()
        if (drawPaths.isNotEmpty()) {
            val size = drawPaths.size
            for (i in 1..size) {
                stack.push(drawPaths.pop())
            }
        }
        editedPaths.add(stack)
    }

    fun redrawEditedPaths() {
        if (editedPaths.isNotEmpty()) {
            val paths = editedPaths.pop()
            if (paths.isNotEmpty()) {
                val size = paths.size
                for (i in 1..size) {
                    drawPaths.push(paths.pop())
                }
            }
        }
    }

    fun addCrop() {
        if (canRedo.value) clearRedo()
        cropStack.add(backgroundImage2.value)
        undoStack.add(CROP)
        updateRevised()
    }

    fun addBlur() {
        if (canRedo.value) clearRedo()
        undoStack.add(BLUR)
        updateRevised()
    }

    private fun operationByTask(task: String) = when (task) {
        ROTATE -> rotateOperation
        RESIZE -> resizeOperation
        CROP -> cropOperation
        BLUR -> blurOperation
        else -> drawOperation
    }

    fun undo() {
        if (canUndo.value) {
            val undoTask = undoStack.pop()
            redoStack.push(undoTask)
            Timber.tag("edit-manager").d("undoing $undoTask")
            undoOperation(operationByTask(undoTask))
        }
        invalidatorTick.value++
        updateRevised()
    }

    fun redo() {
        if (canRedo.value) {
            val redoTask = redoStack.pop()
            undoStack.push(redoTask)
            Timber.tag("edit-manager").d("redoing $redoTask")
            redoOperation(operationByTask(redoTask))
            invalidatorTick.value++
            updateRevised()
        }
    }

    fun saveRotationAfterOtherOperation() {
        addAngle()
        resetRotation()
    }

    fun restoreRotationAfterUndoOtherOperation() {
        if (rotationAngles.isNotEmpty()) {
            prevRotationAngle = rotationAngles.pop()
            rotationAngle.value = prevRotationAngle
        }
    }

    fun addDrawPath(path: Path) {
        drawPaths.add(
            DrawPath(
                path,
                currentPaint.copy().apply {
                    strokeWidth = drawPaint.value.strokeWidth
                }
            )
        )
        if (canRedo.value) clearRedo()
        undoStack.add(DRAW)
    }

    fun setPaintColor(color: Color) {
        drawPaint.value.color = color
        _paintColor.value = color
    }

    private fun clearPaths() {
        drawPaths.clear()
        redoPaths.clear()
        invalidatorTick.value++
        updateRevised()
    }

    private fun clearResizes() {
        resizes.clear()
        redoResize.clear()
        updateRevised()
    }

    private fun resetRotation() {
        rotationAngle.value = 0f
        prevRotationAngle = 0f
    }

    private fun clearRotations() {
        rotationAngles.clear()
        redoRotationAngles.clear()
        resetRotation()
    }

    private fun clearImageSizes() {
        imageSizes.clear()
        redoImageSizes.clear()
    }

    fun clearEdits() {
        clearPaths()
        clearResizes()
        clearRotations()
        clearCrop()
        blurOperation.clear()
        undoStack.clear()
        redoStack.clear()
        clearImageSizes()
        restoreOriginalBackgroundImage()
        updateImageSize()
        scaleToFit()
        updateRevised()
    }

    private fun clearRedo() {
        redoPaths.clear()
        redoCropStack.clear()
        redoRotationAngles.clear()
        redoResize.clear()
        redoStack.clear()
        redoImageSizes.clear()
        updateRevised()
    }

    private fun clearCrop() {
        cropStack.clear()
        redoCropStack.clear()
        updateRevised()
    }

    fun setBackgroundImage2() {
        backgroundImage2.value = backgroundImage.value
    }

    fun redrawBackgroundImage2() {
        backgroundImage.value = backgroundImage2.value
    }

    fun setOriginalBackgroundImage(imgBitmap: ImageBitmap?) {
        originalBackgroundImage.value = imgBitmap
    }

    private fun restoreOriginalBackgroundImage() {
        backgroundImage.value = originalBackgroundImage.value
        updateAvailableDrawArea()
    }

    fun toggleEraseMode() {
        _isEraseMode.value = !isEraseMode.value
    }

    fun toggleRotateMode() {
        _isRotateMode.value = !isRotateMode.value
        if (isRotateMode.value) {
            editMatrix.set(matrix)
            smartSwitchInitiated = false
        }
    }

    fun toggleCropMode() {
        _isCropMode.value = !isCropMode.value
        if (!isCropMode.value) cropWindow.close()
    }

    fun cancelCropMode() {
        backgroundImage.value = backgroundImage2.value
        editMatrix.reset()
    }

    fun cancelRotateMode() {
        rotationAngle.value = prevRotationAngle
        rotateOperation.cancel()
        editMatrix.reset()
    }

    fun toggleResizeMode() {
        _isResizeMode.value = !isResizeMode.value
    }

    fun cancelResizeMode() {
        backgroundImage.value = backgroundImage2.value
        editMatrix.reset()
    }

    fun toggleBlurMode() {
        _isBlurMode.value = !isBlurMode.value
    }
    fun setPaintStrokeWidth(strokeWidth: Float) {
        drawPaint.value.strokeWidth = strokeWidth
    }

    fun calcImageOffset(): Offset {
        val drawArea = drawAreaSize.value
        var offset = Offset.Zero
        backgroundImage.value?.let {
            val xOffset = (
                (drawArea.width - it.width) / 2f
                ).coerceAtLeast(0f)
            val yOffset = (
                (drawArea.height - it.height) / 2f
                ).coerceAtLeast(0f)
            offset = Offset(xOffset, yOffset)
        }
        return offset
    }

    private companion object {
        private const val DRAW = "draw"
        private const val CROP = "crop"
        private const val RESIZE = "resize"
        private const val ROTATE = "rotate"
        private const val BLUR = "blur"
    }

    object Layout {
        const val PORTRAIT = "portrait"
        const val LANDSCAPE = "landscape"
    }
}

class DrawPath(
    val path: Path,
    val paint: Paint
)

fun Paint.copy(): Paint {
    val from = this
    return Paint().apply {
        alpha = from.alpha
        isAntiAlias = from.isAntiAlias
        color = from.color
        blendMode = from.blendMode
        style = from.style
        strokeWidth = from.strokeWidth
        strokeCap = from.strokeCap
        strokeJoin = from.strokeJoin
        strokeMiterLimit = from.strokeMiterLimit
        filterQuality = from.filterQuality
        shader = from.shader
        colorFilter = from.colorFilter
        pathEffect = from.pathEffect
        asFrameworkPaint().apply {
            maskFilter = from.asFrameworkPaint().maskFilter
        }
    }
}

fun defaultPaint(): Paint {
    return Paint().apply {
        color = Color.White
        strokeWidth = 14f
        isAntiAlias = true
        style = PaintingStyle.Stroke
        strokeJoin = StrokeJoin.Round
        strokeCap = StrokeCap.Round
    }
}
