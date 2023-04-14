package space.taran.arkretouch.presentation.drawing

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.IntSize
import space.taran.arkretouch.presentation.edit.Operation
import space.taran.arkretouch.presentation.edit.resize.Resize
import timber.log.Timber
import space.taran.arkretouch.presentation.edit.crop.CropWindow
import java.util.Stack

class EditManager {
    private val drawPaint: MutableState<Paint> = mutableStateOf(defaultPaint())
    private val erasePaint: Paint = Paint().apply {
        shader = null
        color = Color.Transparent
        style = PaintingStyle.Stroke
        blendMode = BlendMode.SrcOut
    }

    val cropWindow = CropWindow()

    val currentPaint: Paint
        get() = if (isEraseMode.value) {
            erasePaint
        } else {
            drawPaint.value
        }

    val drawPaths = Stack<DrawPath>()
    private val redoPaths = Stack<DrawPath>()

    private val croppedPathsStack = Stack<Stack<DrawPath>>()

    var backgroundImage = mutableStateOf<ImageBitmap?>(null)

    private val backgroundImage2 = mutableStateOf<ImageBitmap?>(null)
    private val originalBackgroundImage = mutableStateOf<ImageBitmap?>(null)

    var drawAreaSize = mutableStateOf(IntSize.Zero)
    val availableDrawAreaSize = mutableStateOf(IntSize.Zero)

    var invalidatorTick = mutableStateOf(0)

    private val _currentPaintColor: MutableState<Color> =
        mutableStateOf(drawPaint.value.color)
    val currentPaintColor: State<Color> = _currentPaintColor

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

    val resize = Resize()
    var bitmapWidth = 0
    var bitmapHeight = 0
    val aspectRatio = mutableStateOf(1f)

    val rotationAngle = mutableStateOf(0F)

    private val rotations = Stack<ImageBitmap>()
    private val redoRotations = Stack<ImageBitmap>()

    private val editedPaths = Stack<Stack<DrawPath>>()

    private val redoResize = Stack<ImageBitmap>()
    private val resizes = Stack<ImageBitmap>()

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    private val _isCropMode = mutableStateOf(false)
    val isCropMode = _isCropMode

    private val cropStack = Stack<ImageBitmap>()
    private val redoCropStack = Stack<ImageBitmap>()

    fun applyOperation(operation: Operation) {
        operation.apply()
    }

    fun updateAvailableDrawArea(bitmap: ImageBitmap? = backgroundImage.value) {
        if (bitmap == null) {
            availableDrawAreaSize.value = drawAreaSize.value
            return
        }
        availableDrawAreaSize.value = IntSize(
            bitmap.width,
            bitmap.height
        )
    }

    internal fun clearRedoPath() {
        redoPaths.clear()
    }

    fun keepCroppedPaths() {
        val stack = Stack<DrawPath>()
        if (drawPaths.isNotEmpty()) {
            val size = drawPaths.size
            for (i in 1..size) {
                stack.push(drawPaths.pop())
            }
        }
        croppedPathsStack.add(stack)
        updateRevised()
    }

    fun updateRevised() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun addResize() {
        if (canRedo.value) clearRedo()
        resizes.add(backgroundImage2.value)
        undoStack.add(RESIZE)
        keepEditedPaths()
        updateRevised()
    }

    private fun undoResize() {
        if (resizes.isNotEmpty()) {
            redoResize.push(backgroundImage.value)
            backgroundImage.value = resizes.pop()
            updateAvailableDrawArea()
            redrawEditedPaths()
        }
    }

    private fun redoResize() {
        if (redoResize.isNotEmpty()) {
            resizes.push(backgroundImage.value)
            backgroundImage.value = redoResize.pop()
            updateAvailableDrawArea()
            keepEditedPaths()
        }
    }

    private fun keepEditedPaths() {
        val stack = Stack<DrawPath>()
        if (drawPaths.isNotEmpty()) {
            val size = drawPaths.size
            for (i in 1..size) {
                stack.push(drawPaths.pop())
            }
        }
        editedPaths.add(stack)
    }

    private fun redrawEditedPaths() {
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

    fun cancelRotateMode() {
        backgroundImage.value = backgroundImage2.value
        rotationAngle.value = 0f
    }

    fun addCrop() {
        if (canRedo.value) clearRedo()
        cropStack.add(backgroundImage2.value)
        undoStack.add(CROP)
        updateRevised()
    }

    private fun redrawCroppedPaths() {
        if (croppedPathsStack.isNotEmpty()) {
            val paths = croppedPathsStack.pop()
            if (paths.isNotEmpty()) {
                val size = paths.size
                for (i in 1..size) {
                    drawPaths.push(paths.pop())
                }
            }
        }
    }

    private fun undoCrop() {
        if (cropStack.isNotEmpty()) {
            redoCropStack.push(backgroundImage.value)
            backgroundImage.value = cropStack.pop()
            updateAvailableDrawArea()
            redrawCroppedPaths()
            updateRevised()
        }
    }

    private fun redoCrop() {
        if (redoCropStack.isNotEmpty()) {
            cropStack.push(backgroundImage.value)
            backgroundImage.value = redoCropStack.pop()
            updateAvailableDrawArea()
            keepCroppedPaths()
            updateRevised()
        }
    }

    private fun clearCroppedPaths() {
        croppedPathsStack.clear()
    }

    private fun undoDraw() {
        if (drawPaths.isNotEmpty()) {
            redoPaths.push(drawPaths.pop())
            updateRevised()
            return
        }
    }

    private fun redoDraw() {
        if (redoPaths.isNotEmpty()) {
            drawPaths.push(redoPaths.pop())
            updateRevised()
            return
        }
    }

    fun undo() {
        if (canUndo.value) {
            val undoTask = undoStack.pop()
            redoStack.push(undoTask)
            Timber.tag("edit-manager").d("undoing $undoTask")
            if (undoTask == DRAW)
                undoDraw()
            if (undoTask == RESIZE)
                undoResize()
            if (undoTask == CROP)
                undoCrop()
        }
        invalidatorTick.value++
        updateRevised()
    }

    fun redo() {
        if (canRedo.value) {
            val redoTask = redoStack.pop()
            undoStack.push(redoTask)
            Timber.tag("edit-manager").d("redoing $redoTask")
            if (redoTask == DRAW)
                redoDraw()
            if (redoTask == RESIZE)
                redoResize()
            if (redoTask == CROP) {
                redoCrop()
            }
        }
        invalidatorTick.value++
        updateRevised()
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
        _currentPaintColor.value = color
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

    fun clearEdits() {
        clearPaths()
        clearResizes()
        clearCrop()
        undoStack.clear()
        redoStack.clear()
        restoreOriginalBackgroundImage()
        updateRevised()
    }

    private fun clearRedo() {
        redoPaths.clear()
        redoRotations.clear()
        redoResize.clear()
        redoCropStack.clear()
        redoStack.clear()
    }

    private fun clearCrop() {
        cropStack.clear()
        redoCropStack.clear()
        clearCroppedPaths()
        updateRevised()
    }

    fun setBackgroundImage2() {
        backgroundImage2.value = backgroundImage.value
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
    }

    fun toggleCropMode() {
        _isCropMode.value = !isCropMode.value
        if (!isCropMode.value) cropWindow.close()
    }

    fun cancelCropMode() {
        backgroundImage.value = backgroundImage2.value
        updateAvailableDrawArea()
    }

    fun toggleResizeMode() {
        _isResizeMode.value = !isResizeMode.value
    }

    fun cancelResizeMode() {
        backgroundImage.value = backgroundImage2.value
        updateAvailableDrawArea()
    }

    fun setPaintStrokeWidth(strokeWidth: Float) {
        drawPaint.value.strokeWidth = strokeWidth
    }

    fun calcImageOffset(): Offset {
        val drawArea = drawAreaSize.value
        val bitmap = backgroundImage.value
        var xOffset = 0f
        var yOffset = 0f
        if (bitmap != null) {
            xOffset = (drawArea.width - bitmap.width) / 2f
            yOffset = (drawArea.height - bitmap.height) / 2f
        }
        return Offset(xOffset, yOffset)
    }

    private companion object {
        private const val DRAW = "draw"
        private const val CROP = "crop"
        private const val RESIZE = "resize"
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
