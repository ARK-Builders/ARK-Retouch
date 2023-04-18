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
import space.taran.arkretouch.presentation.edit.Operation
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

    val matrix = Matrix()
    val editMatrix = Matrix()

    var drawAreaSize = mutableStateOf(IntSize.Zero)
    val availableDrawAreaSize = mutableStateOf(IntSize.Zero)
    val backgroundSize = mutableStateOf(IntSize.Zero)

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
    private val rotationAngles = Stack<Float>()
    private val redoRotationAngles = Stack<Float>()

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

    fun rotate(angle: Float) {
        val centerX = availableDrawAreaSize.value.width / 2
        val centerY = availableDrawAreaSize.value.height / 2
        if (isRotateMode.value) {
            rotationAngle.value += angle
            editMatrix.postRotate(angle, centerX.toFloat(), centerY.toFloat())
            return
        }
        matrix.postRotate(angle, centerX.toFloat(), centerY.toFloat())
    }

    private fun undoRotate() {
        if (rotationAngles.isNotEmpty()) {
            redoRotationAngles.push(prevRotationAngle)
            prevRotationAngle = rotationAngles.pop()
            matrix.reset()
            rotate(prevRotationAngle)
        }
    }

    private fun redoRotate() {
        if (redoRotationAngles.isNotEmpty()) {
            rotationAngles.push(prevRotationAngle)
            prevRotationAngle = redoRotationAngles.pop()
            matrix.reset()
            rotate(prevRotationAngle)
        }
    }

    fun addRotation() {
        if (canRedo.value) clearRedo()
        rotationAngles.add(prevRotationAngle)
        undoStack.add(ROTATE)
        prevRotationAngle = rotationAngle.value
        updateRevised()
    }

    fun addAngle() {
        rotationAngles.add(prevRotationAngle)
    }

    fun resetRotation() {
        rotationAngle.value = 0f
        prevRotationAngle = 0f
    }

    private fun clearRotations() {
        rotations.clear()
        redoRotations.clear()
        rotationAngles.clear()
        redoRotationAngles.clear()
        resetRotation()
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
            val image = cropStack.pop()
            redoCropStack.push(backgroundImage.value)
            updateAvailableDrawArea(image)
            if (rotationAngles.isNotEmpty()) {
                matrix.reset()
                prevRotationAngle = rotationAngles.pop()
                rotate(prevRotationAngle)
            }
            backgroundImage.value = image
            redrawCroppedPaths()
            updateRevised()
        }
    }

    private fun redoCrop() {
        if (redoCropStack.isNotEmpty()) {
            val image = redoCropStack.pop()
            addAngle()
            resetRotation()
            matrix.reset()
            cropStack.push(backgroundImage.value)
            updateAvailableDrawArea(image)
            backgroundImage.value = image
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
            if (undoTask == ROTATE)
                undoRotate()
            if (undoTask == DRAW)
                undoDraw()
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
            if (redoTask == ROTATE) {
                redoRotate()
            }
            if (redoTask == DRAW)
                redoDraw()
            if (redoTask == CROP && redoCropStack.isNotEmpty()) {
                redoCrop()
            }
            invalidatorTick.value++
            updateRevised()
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

    fun clearEdits() {
        clearPaths()
        clearRotations()
        clearCrop()
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
        if (isRotateMode.value) editMatrix.set(matrix)
    }

    fun toggleCropMode() {
        _isCropMode.value = !isCropMode.value
        if (!isCropMode.value) cropWindow.close()
    }

    fun cancelCropMode() {
        backgroundImage.value = backgroundImage2.value
        updateAvailableDrawArea()
    }

    fun cancelRotateMode() {
        rotationAngle.value = prevRotationAngle
        editMatrix.reset()
    }

    fun setPaintStrokeWidth(strokeWidth: Float) {
        drawPaint.value.strokeWidth = strokeWidth
    }

    fun calcImageOffset(): Offset {
        val drawArea = drawAreaSize.value
        val bitmap = backgroundImage.value
        var offset = Offset.Zero
        if (bitmap != null) {
            val xOffset = (drawArea.width - bitmap.width) / 2
            val yOffset = (drawArea.height - bitmap.height) / 2
            offset = Offset(xOffset.toFloat(), yOffset.toFloat())
        }
        return offset
    }

    private companion object {
        private const val DRAW = "draw"
        private const val CROP = "crop"
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
