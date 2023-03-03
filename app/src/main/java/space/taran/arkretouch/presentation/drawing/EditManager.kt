package space.taran.arkretouch.presentation.drawing

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.IntSize
import space.taran.arkretouch.presentation.edit.crop.CropWindow
import space.taran.arkretouch.presentation.utils.crop
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
    var bitmapSize = mutableStateOf(IntSize.Zero)

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

    private val _isCropMode = mutableStateOf(false)
    val isCropMode = _isCropMode

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    private val cropStack = Stack<ImageBitmap>()
    private val redoCropStack = Stack<ImageBitmap>()

    private fun crop(): Bitmap {
        keepCroppedPaths()
        return with(cropWindow) {
            getBitmap().crop(
                getCropParams()
            )
        }
    }

    internal fun clearRedoPath() {
        redoPaths.clear()
    }

    private fun keepCroppedPaths() {
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

    private fun clearRedo() {
        redoStack.clear()
        redoPaths.clear()
        redoCropStack.clear()
    }

    fun updateRevised() {
        _canUndo.value = drawPaths.isNotEmpty() ||
            cropStack.isNotEmpty() ||
            croppedPathsStack.isNotEmpty()
        _canRedo.value = redoPaths.isNotEmpty() ||
            redoCropStack.isNotEmpty()
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
            redrawCroppedPaths()
            updateRevised()
        }
    }

    private fun redoCrop() {
        if (redoCropStack.isNotEmpty()) {
            cropStack.push(backgroundImage.value)
            backgroundImage.value = redoCropStack.pop()
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
            invalidatorTick.value++
            redoStack.push(undoTask)
            if (undoTask == DRAW) {
                undoDraw()
                return
            }
            if (undoTask == CROP) {
                undoCrop()
                return
            }
        }
    }

    fun redo() {
        if (canRedo.value) {
            val redoTask = redoStack.pop()
            invalidatorTick.value++
            undoStack.push(redoTask)
            if (redoTask == DRAW) {
                redoDraw()
                return
            }
            if (redoTask == CROP && redoCropStack.isNotEmpty()) {
                redoCrop()
                return
            }
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
        _currentPaintColor.value = color
    }

    private fun clearPaths() {
        drawPaths.clear()
        redoPaths.clear()
        invalidatorTick.value++
        updateRevised()
    }

    private fun clearCrop() {
        cropStack.clear()
        redoCropStack.clear()
        clearCroppedPaths()
        updateRevised()
    }

    fun clearEdits() {
        clearPaths()
        clearCrop()
        restoreOriginalBackgroundImage()
    }

    fun setBackgroundImage2() {
        backgroundImage2.value = backgroundImage.value
    }

    fun setOriginalBackgroundImage(imgBitmap: ImageBitmap?) {
        originalBackgroundImage.value = imgBitmap
    }

    private fun restoreOriginalBackgroundImage() {
        backgroundImage.value = originalBackgroundImage.value
    }

    fun toggleEraseMode() {
        _isEraseMode.value = !isEraseMode.value
    }

    fun toggleCropMode() {
        _isCropMode.value = !isCropMode.value
    }

    fun cancelCropMode() {
        backgroundImage.value = backgroundImage2.value
    }

    fun setPaintStrokeWidth(strokeWidth: Float) {
        drawPaint.value.strokeWidth = strokeWidth
    }

    fun calcImageOffset(): Offset {
        var offset = Offset(0f, 0f)
        if (backgroundImage.value != null) {
            val drawArea = drawAreaSize.value
            val bitmap = backgroundImage.value!!
            val xOffset = (drawArea.width - bitmap.width) / 2f
            val yOffset = (drawArea.height - bitmap.height) / 2f
            offset = Offset(xOffset, yOffset)
        }
        return offset
    }

    fun resizeCroppedBitmap(
        resize: (ImageBitmap, Int, Int) -> ImageBitmap
    ) {
        val drawArea = drawAreaSize.value
        backgroundImage.value = resize(
            crop().asImageBitmap(),
            drawArea.width,
            drawArea.height
        )
    }

    private companion object {
        const val DRAW = "draw"
        const val CROP = "crop"
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