package space.taran.arkretouch.presentation.drawing

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.IntSize
import space.taran.arkretouch.presentation.edit.Operation
import space.taran.arkretouch.presentation.edit.rotate.RotateGrid
import timber.log.Timber
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

    val cropMatrix = Matrix()

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

    val rotationGrid = RotateGrid()

    private val backgroundImage2 = mutableStateOf<ImageBitmap?>(null)
    private val originalBackgroundImage = mutableStateOf<ImageBitmap?>(null)

    var drawAreaSize = mutableStateOf(IntSize.Zero)
    var availableDrawAreaSize = mutableStateOf(IntSize.Zero)

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

    private val rotations = Stack<ImageBitmap>()
    private val redoRotations = Stack<ImageBitmap>()
    private val rotatedStack = Stack<Stack<DrawPath>>()

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    private val _isCropMode = mutableStateOf(false)
    val isCropMode = _isCropMode

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

    private fun cropTwo() {
        cropWindow.apply {
            val params = getCropParams()
            val width = params.width
            val height = params.height
            var maxWidth = drawAreaSize.value.width
            var maxHeight = drawAreaSize.value.height
            val aspectRatio = width.toFloat() / height.toFloat()
            val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()
            val px = maxWidth / 2
            val py = maxHeight / 2
            if (aspectRatio > maxRatio) maxHeight = (maxWidth / aspectRatio).toInt()
            else maxWidth = (maxHeight * aspectRatio).toInt()
            val sx = maxWidth / width
            val sy = maxHeight / height
            availableDrawAreaSize.value = IntSize(
                maxWidth,
                maxHeight
            )
            cropMatrix.reset()
            cropMatrix.postScale(
                sx.toFloat(),
                sy.toFloat(),
                px.toFloat(),
                py.toFloat()
            )
        }
        invalidatorTick.value++
    }

    fun applyOperation(operation: Operation) {
        operation.apply()
    }

    fun applyCrop() {
        cropTwo()
        backgroundImage.value = crop().asImageBitmap()
        // zoomAfterCrop()
    }

    fun zoom(maxSize: IntSize = drawAreaSize.value) {
        val bitmap = backgroundImage.value!!
        val height = bitmap.height.toFloat()
        val width = bitmap.width.toFloat()
        var finalWidth = maxSize.width.toFloat()
        var finalHeight = maxSize.height.toFloat()
        val aspectRatio = width / height
        val maxRatio = finalWidth / finalHeight
        val px = finalWidth / 2f
        val py = finalHeight / 2f
        if (maxRatio > aspectRatio)
            finalWidth = finalHeight * aspectRatio
        else
            finalHeight = finalWidth / aspectRatio
        availableDrawAreaSize.value = IntSize(
            finalWidth.toInt(),
            finalHeight.toInt()
        )
        val sx = finalWidth / width
        val sy = finalHeight / height
        cropMatrix.reset()
        cropMatrix.postScale(sx, sy, px, py)
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
        redoRotations.clear()
        redoCropStack.clear()
    }

    fun updateRevised() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun undoRotate() {
        if (rotations.isNotEmpty()) {
            redoRotations.push(backgroundImage.value)
            backgroundImage.value = rotations.pop()
            redrawRotatedPaths()
        }
    }

    private fun redoRotate() {
        if (redoRotations.isNotEmpty()) {
            rotations.push(backgroundImage.value)
            backgroundImage.value = redoRotations.pop()
            keepRotatedPaths()
        }
    }

    fun addRotation() {
        if (canRedo.value) clearRedo()
        rotations.add(backgroundImage2.value)
        undoStack.add(ROTATE)
        resetRotation()
        keepRotatedPaths()
        updateRevised()
    }

    fun rotateGrid(angle: Float = 0f) {
        rotationAngle.value += angle
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
        backgroundImage.value = backgroundImage2.value
        rotationAngle.value = 0f
    }

    private fun resetRotation() {
        rotationAngle.value = 0f
    }

    private fun clearRotations() {
        rotations.clear()
        redoRotations.clear()
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
        restoreOriginalBackgroundImage()
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
    }

    fun toggleEraseMode() {
        _isEraseMode.value = !isEraseMode.value
    }

    fun toggleRotateMode() {
        _isRotateMode.value = !isRotateMode.value
    }

    fun toggleCropMode() {
        _isCropMode.value = !isCropMode.value
        if (isCropMode.value)
            cropMatrix.reset()
        if (!isCropMode.value) cropWindow.close()
    }

    fun cancelCropMode() {
        backgroundImage.value = backgroundImage2.value
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
        const val DRAW = "draw"
        const val CROP = "crop"
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
