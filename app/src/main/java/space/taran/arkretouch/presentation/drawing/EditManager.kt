package space.taran.arkretouch.presentation.drawing

import android.graphics.Matrix
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.IntSize
import space.taran.arkretouch.presentation.edit.rotate.RotateGrid
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
    var backgroundImage2 = mutableStateOf<ImageBitmap?>(null)
    private val originalBackgroundImage = mutableStateOf<ImageBitmap?>(null)

    val matrix = Matrix()
    val rotationMatrix = Matrix()

    val rotationGrid = RotateGrid()

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

    private val _isRotateMode = mutableStateOf(false)
    val isRotateMode = _isRotateMode

    val rotationAngle = mutableStateOf(0F)
    private var prevRotationAngle = 0f

    private val rotations = Stack<ImageBitmap>()
    private val redoRotations = Stack<ImageBitmap>()
    private val rotatedStack = Stack<Stack<DrawPath>>()
    private val rotationAngles = Stack<Float>()
    private val redoRotationAngles = Stack<Float>()

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    internal fun clearRedoPath() {
        redoPaths.clear()
    }

    fun updateRevised() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun rotate(angle: Float) {
        val centerX = drawAreaSize.value.width / 2
        val centerY = drawAreaSize.value.height / 2
        if (isRotateMode.value) rotationAngle.value += angle
        rotationMatrix.postRotate(angle, centerX.toFloat(), centerY.toFloat())
    }

    fun applyRotation(resize: (ImageBitmap, Int, Int) -> ImageBitmap) {
        val bitmap = rotationGrid.getBitmap()
        matrix.set(rotationMatrix)
        backgroundImage.value = resize(
            bitmap.asImageBitmap(),
            drawAreaSize.value.width,
            drawAreaSize.value.height
        )
        addRotation()
        toggleRotateMode()
    }

    private fun undoRotate() {
        if (rotations.isNotEmpty() && rotationAngles.isNotEmpty()) {
            redoRotations.push(backgroundImage.value)
            redoRotationAngles.push(prevRotationAngle)
            prevRotationAngle = rotationAngles.pop()
            rotationMatrix.reset()
            rotate(prevRotationAngle)
            matrix.set(rotationMatrix)
            backgroundImage.value = rotations.pop()
            redrawRotatedPaths()
        }
    }

    private fun redoRotate() {
        if (redoRotations.isNotEmpty()) {
            rotations.push(backgroundImage.value)
            rotationAngles.push(prevRotationAngle)
            prevRotationAngle = redoRotationAngles.pop()
            rotationMatrix.reset()
            rotate(prevRotationAngle)
            matrix.set(rotationMatrix)
            backgroundImage.value = redoRotations.pop()
            keepRotatedPaths()
        }
    }

    private fun addRotation() {
        if (canRedo.value) clearRedo()
        rotations.add(backgroundImage2.value)
        rotationAngles.add(prevRotationAngle)
        undoStack.add(ROTATE)
        prevRotationAngle = rotationAngle.value
        keepRotatedPaths()
        updateRevised()
    }

    private fun keepRotatedPaths() {
        val stack = Stack<DrawPath>()
        if (drawPaths.isNotEmpty()) {
            val size = drawPaths.size
            for (i in 1..size) {
                stack.push(drawPaths.pop())
            }
        }
        rotatedStack.add(stack)
    }

    private fun redrawRotatedPaths() {
        if (rotatedStack.isNotEmpty()) {
            val paths = rotatedStack.pop()
            if (paths.isNotEmpty()) {
                val size = paths.size
                for (i in 1..size) {
                    drawPaths.push(paths.pop())
                }
            }
        }
    }

    fun cancelRotateMode() {
        rotate(prevRotationAngle)
        matrix.set(rotationMatrix)
        backgroundImage.value = backgroundImage2.value
    }

    private fun resetRotation() {
        rotationAngle.value = 0f
    }

    private fun clearRotations() {
        rotations.clear()
        redoRotations.clear()
        rotationAngles.clear()
        redoRotationAngles.clear()
        rotationMatrix.reset()
        prevRotationAngle = 0f
        resetRotation()
    }

    private fun undoDraw() {
        if (drawPaths.isNotEmpty()) {
            redoPaths.push(drawPaths.pop())
        }
    }

    private fun redoDraw() {
        if (redoPaths.isNotEmpty()) {
            drawPaths.push(redoPaths.pop())
        }
    }

    fun undo() {
        if (canUndo.value) {
            val undoTask = undoStack.pop()
            redoStack.push(undoTask)
            Timber.tag("edit-manager").d("undoing $undoTask")
            if (undoTask == ROTATE)
                undoRotate()
            if (undoTask == DRAW)
                undoDraw()
        }
        invalidatorTick.value++
        updateRevised()
    }

    fun redo() {
        if (canRedo.value) {
            val redoTask = redoStack.pop()
            undoStack.push(redoTask)
            Timber.tag("edit-manager").d("redoing $redoTask")
            if (redoTask == ROTATE) {
                redoRotate()
            }
            if (redoTask == DRAW)
                redoDraw()
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

    fun setBackgroundImage2() {
        backgroundImage2.value = backgroundImage.value
    }

    fun setOriginalBackgroundImage(imgBitmap: ImageBitmap?) {
        originalBackgroundImage.value = imgBitmap
    }

    private fun restoreOriginalBackgroundImage() {
        backgroundImage.value = originalBackgroundImage.value
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

    fun clearEdits() {
        clearPaths()
        clearRotations()
        undoStack.clear()
        redoStack.clear()
        matrix.reset()
        restoreOriginalBackgroundImage()
        updateRevised()
    }

    private fun clearRedo() {
        redoPaths.clear()
        redoRotations.clear()
        redoStack.clear()
        updateRevised()
    }

    fun toggleEraseMode() {
        _isEraseMode.value = !isEraseMode.value
    }

    fun toggleRotateMode() {
        _isRotateMode.value = !isRotateMode.value
        rotationMatrix.reset()
        resetRotation()
        invalidatorTick.value++
    }

    fun setPaintStrokeWidth(strokeWidth: Float) {
        drawPaint.value.strokeWidth = strokeWidth
    }

    fun calcImageOffset(): Offset {
        val drawArea = drawAreaSize.value
        val bitmap = backgroundImage.value
        var offset = Offset.Zero
        if (bitmap != null) {
            val xOffset = (drawArea.width - bitmap.width) / 2f
            val yOffset = (drawArea.height - bitmap.height) / 2f
            offset = Offset(xOffset, yOffset)
        }
        return offset
    }

    companion object {
        private const val DRAW = "draw"
        private const val ROTATE = "rotate"
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
