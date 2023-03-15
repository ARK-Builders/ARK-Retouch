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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.IntSize
import space.taran.arkretouch.presentation.edit.resizeByMax
import java.util.Stack

class EditManager(private val screenDensity: Float) {
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

    val matrix: Matrix
        get() = if (isCropMode.value) cropMatrix else editMatrix

    val editMatrix = Matrix()
    var cropMatrix = Matrix()

    val cropWindowHelper = CropWindowHelper(this, screenDensity)

    var backgroundImage = mutableStateOf<ImageBitmap?>(null)
    var availableDrawAreaSize = mutableStateOf(IntSize.Zero)
    var captureArea = mutableStateOf<CaptureArea?>(null)
    var initialDrawAreaOffset = Offset.Zero

    var invalidatorTick = mutableStateOf(0)
    private val _isCropMode = mutableStateOf(false)
    val isCropMode = _isCropMode
    val isMovementNotRotateMode = mutableStateOf(true)

    private val _currentPaintColor: MutableState<Color> =
        mutableStateOf(drawPaint.value.color)
    val currentPaintColor: State<Color> = _currentPaintColor

    private val _isEraseMode: MutableState<Boolean> = mutableStateOf(false)
    val isEraseMode: State<Boolean> = _isEraseMode

    private val _canUndo: MutableState<Boolean> = mutableStateOf(false)
    val canUndo: State<Boolean> = _canUndo

    private val _canRedo: MutableState<Boolean> = mutableStateOf(false)
    val canRedo: State<Boolean> = _canRedo

    internal fun clearRedoPath() {
        redoPaths.clear()
    }

    fun updateRevised() {
        _canUndo.value = drawPaths.isNotEmpty()
        _canRedo.value = redoPaths.isNotEmpty()
    }

    fun undo() {
        if (canUndo.value) {
            redoPaths.push(drawPaths.pop())
            invalidatorTick.value++
            updateRevised()
        }
    }

    fun redo() {
        if (canRedo.value) {
            drawPaths.push(redoPaths.pop())
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

    fun applyCrop() {
        editMatrix.set(cropMatrix)
        val (newWidth, newHeight) = resizeByMax(
            captureArea.value!!.width,
            captureArea.value!!.height,
            availableDrawAreaSize.value.width.toFloat(),
            availableDrawAreaSize.value.height.toFloat()
        )
        val centerX = availableDrawAreaSize.value.width / 2f
        val centerY = availableDrawAreaSize.value.height / 2f
        val centerCaptureAreaX = captureArea.value!!.left +
            captureArea.value!!.width / 2
        val centerCaptureAreaY = captureArea.value!!.top +
            captureArea.value!!.height / 2
        editMatrix.postTranslate(
            centerX - centerCaptureAreaX,
            centerY - centerCaptureAreaY
        )
        editMatrix.postScale(
            newWidth / captureArea.value!!.width,
            newHeight / captureArea.value!!.height,
            centerX,
            centerY
        )
        val xOffset = (availableDrawAreaSize.value.width - newWidth) / 2f
        val yOffset = (availableDrawAreaSize.value.height - newHeight) / 2f
        captureArea.value = CaptureArea(xOffset, yOffset, newWidth, newHeight)
        cropMatrix = Matrix()
        _isCropMode.value = false
        invalidatorTick.value++
    }

    fun toggleEraseMode() {
        _isEraseMode.value = !isEraseMode.value
    }

    fun toggleCropMode() {
        _isCropMode.value = !isCropMode.value
        if (isCropMode.value) {
            cropMatrix.set(editMatrix)
        }
        invalidatorTick.value++
    }

    fun setPaintStrokeWidth(strokeWidth: Float) {
        drawPaint.value.strokeWidth = strokeWidth
    }

    fun calcImageOffset(
        possibleDrawArea: IntSize,
        backgroundImage: ImageBitmap
    ): Offset {
        val xOffset = (possibleDrawArea.width - backgroundImage.width) / 2f
        val yOffset = (possibleDrawArea.height - backgroundImage.height) / 2f
        return Offset(xOffset, yOffset)
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
