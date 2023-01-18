package space.taran.arkretouch.presentation.drawing

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.IntSize
import timber.log.Timber
import java.util.Stack

class EditManager {
    private val drawPaint: MutableState<Paint> = mutableStateOf(defaultPaint())
    private val erasePaint: Paint = Paint().apply {
        shader = null
        color = Color.Transparent
        style = PaintingStyle.Stroke
        blendMode = BlendMode.SrcOut
    }
    val currentPaint: Paint
        get() = if (isEraseMode.value) {
            erasePaint
        } else {
            drawPaint.value
        }

    val drawPaths = Stack<DrawPath>()
    private val redoPaths = Stack<DrawPath>()

    var backgroundImage = mutableStateOf<ImageBitmap?>(null)
    var drawAreaSize = mutableStateOf(IntSize.Zero)

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

    val cropCounter = mutableStateOf(0)

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    private val cropStack = Stack<String>()
    private val redoCropStack = Stack<String>()

    lateinit var crop: () -> Unit

    lateinit var refresh: (String) -> Unit

    private var currentUri = ""

    internal fun clearRedoPath() {
        redoPaths.clear()
    }

    private fun clearRedo() {
        redoStack.clear()
        redoPaths.clear()
        redoCropStack.clear()
    }

    fun updateRevised() {
        _canUndo.value = drawPaths.isNotEmpty() ||
            cropStack.isNotEmpty()
        _canRedo.value = redoPaths.isNotEmpty() ||
            redoCropStack.isNotEmpty()
    }

    fun notifyImageCropped(uri: String) {
        if (canRedo.value) clearRedo()
        drawPaths.clear()
        cropStack.add(currentUri)
        currentUri = uri
        undoStack.add(CROP)
        cropCounter.value += 1
        updateRevised()
    }

    fun getUri() = currentUri

    fun setUriToCrop(uri: String) {
        currentUri = uri
        Timber.tag("Back uri").d(currentUri)
    }

    private fun undoCrop() {
        redoCropStack.push(currentUri)
        currentUri = cropStack.pop()
        Timber.tag("Undo uri").d(currentUri)
        updateRevised()
        cropCounter.value += 1
        refresh(currentUri)
    }

    private fun redoCrop() {
        cropStack.push(currentUri)
        currentUri = redoCropStack.pop()
        Timber.tag("Redo uri").d(currentUri)
        updateRevised()
        cropCounter.value -= 1
        refresh(currentUri)
    }

    fun undo() {
        if (canUndo.value) {
            val undoTask = undoStack.pop()
            invalidatorTick.value++
            redoStack.push(undoTask)
            if (undoTask == DRAW && drawPaths.isNotEmpty()) {
                redoPaths.push(drawPaths.pop())
                updateRevised()
                return
            }
            if (undoTask == CROP && cropStack.isNotEmpty()) {
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
            if (redoTask == DRAW && redoPaths.isNotEmpty()) {
                drawPaths.push(redoPaths.pop())
                updateRevised()
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

    fun clearPaths() {
        drawPaths.clear()
        redoPaths.clear()
        invalidatorTick.value++
        updateRevised()
    }

    fun clearCrop() {}

    fun clearEdits() {
        clearPaths()
        clearCrop()
    }

    fun toggleEraseMode() {
        _isEraseMode.value = !isEraseMode.value
    }

    fun setPaintStrokeWidth(strokeWidth: Float) {
        drawPaint.value.strokeWidth = strokeWidth
    }

    fun calcImageOffset(): Offset {
        val drawArea = drawAreaSize.value
        val bitmap = backgroundImage.value!!
        val xOffset = (drawArea.width - bitmap.width) / 2f
        val yOffset = (drawArea.height - bitmap.height) / 2f
        return Offset(xOffset, yOffset)
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
