package dev.arkbuilders.arkretouch.editing.manager

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import android.graphics.Matrix
import dev.arkbuilders.arkretouch.data.model.DrawPath
import dev.arkbuilders.arkretouch.data.model.ImageDefaults
import dev.arkbuilders.arkretouch.data.model.ImageViewParams
import dev.arkbuilders.arkretouch.data.model.Resolution
import dev.arkbuilders.arkretouch.editing.Operation
import dev.arkbuilders.arkretouch.editing.crop.CropWindow
import dev.arkbuilders.arkretouch.editing.resize.ResizeOperation
import dev.arkbuilders.arkretouch.editing.rotate.RotateOperation
import dev.arkbuilders.arkretouch.presentation.viewmodels.fitBackground
import dev.arkbuilders.arkretouch.presentation.viewmodels.fitImage
import timber.log.Timber
import java.util.Stack

class EditManager {

    var imageSize: IntSize = IntSize.Zero
        private set

    private val _backgroundColor = mutableStateOf(Color.Transparent)

    val cropWindow = CropWindow(this)

    val drawPaths = Stack<DrawPath>()

    val redoPaths = Stack<DrawPath>()

    val backgroundImage = mutableStateOf<ImageBitmap?>(null)
    val backgroundImage2 = mutableStateOf<ImageBitmap?>(null)
    val originalBackgroundImage = mutableStateOf<ImageBitmap?>(null)

    val matrix = Matrix()
    val editMatrix = Matrix()
    val backgroundMatrix = Matrix()

    private var matrixScale = 1f
    var zoomScale = 1f
    lateinit var bitmapScale: ResizeOperation.Scale
        private set

    private val _resolution = mutableStateOf<Resolution?>(null)
    val resolution: State<Resolution?> = _resolution
    var drawAreaSize = mutableStateOf(IntSize.Zero)
    val availableDrawAreaSize = mutableStateOf(IntSize.Zero)

    var invalidatorTick = mutableIntStateOf(0)
        private set

    val rotationAngle = mutableFloatStateOf(0F)
    var prevRotationAngle = 0f

    private val editedPaths = Stack<Stack<DrawPath>>()

    val redoResize = Stack<ImageBitmap>()
    val resizes = Stack<ImageBitmap>()
    val rotationAngles = Stack<Float>()
    val redoRotationAngles = Stack<Float>()

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    val cropStack = Stack<ImageBitmap>()
    val redoCropStack = Stack<ImageBitmap>()

    fun setImageSize(size: IntSize) {
        if (size != IntSize.Zero) {
            imageSize = size
        }
    }

    fun scaleToFit() {
        val viewParams = backgroundImage.value?.let {
            fitImage(
                it,
                drawAreaSize.value.width,
                drawAreaSize.value.height
            )
        } ?: run {
            fitBackground(
                imageSize,
                drawAreaSize.value.width,
                drawAreaSize.value.height
            )
        }
        matrixScale = viewParams.scale.x
        scaleMatrix(viewParams)
        updateAvailableDrawArea(viewParams.drawArea)
        val bitmapXScale =
            imageSize.width.toFloat() / viewParams.drawArea.width.toFloat()
        val bitmapYScale =
            imageSize.height.toFloat() / viewParams.drawArea.height.toFloat()
        bitmapScale = ResizeOperation.Scale(
            bitmapXScale,
            bitmapYScale
        )
    }

    fun scaleToFitOnEdit(
        maxWidth: Int = drawAreaSize.value.width,
        maxHeight: Int = drawAreaSize.value.height,
        isRotating: Boolean = false
    ): ImageViewParams {
        val viewParams = backgroundImage.value?.let {
            fitImage(it, maxWidth, maxHeight)
        } ?: run {
            fitBackground(
                imageSize,
                maxWidth,
                maxHeight
            )
        }
        scaleEditMatrix(viewParams, isRotating)
        updateAvailableDrawArea(viewParams.drawArea)
        return viewParams
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

    fun updateAvailableDrawArea(area: IntSize) {
        availableDrawAreaSize.value = area
    }

    fun updateRevised(updateState: (Boolean, Boolean) -> Unit) {
        updateState(undoStack.isNotEmpty(), redoStack.isNotEmpty())
    }

    fun RotateOperation.onRotate(angle: Float) {
        val centerX = availableDrawAreaSize.value.width / 2
        val centerY = availableDrawAreaSize.value.height / 2
        rotationAngle.floatValue += angle
        rotate(
            editMatrix,
            angle,
            centerX.toFloat(),
            centerY.toFloat()
        )
    }

    fun addRotation() {
        rotationAngles.add(prevRotationAngle)
        undoStack.add(ROTATE)
        prevRotationAngle = rotationAngle.floatValue
    }

    fun addResize() {
        resizes.add(backgroundImage2.value)
        undoStack.add(RESIZE)
        keepEditedPaths()
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
        cropStack.add(backgroundImage2.value)
        undoStack.add(CROP)
    }

    fun addBlur() {
        undoStack.add(BLUR)
    }

    fun undo(operationByTask: (String) -> Operation) {
        val undoTask = undoStack.pop()
        redoStack.push(undoTask)
        Timber.tag("edit-manager").d("undoing $undoTask")
        undoOperation(operationByTask(undoTask))
    }

    fun redo(operationByTask: (String) -> Operation) {
        val redoTask = redoStack.pop()
        undoStack.push(redoTask)
        Timber.tag("edit-manager").d("redoing $redoTask")
        redoOperation(operationByTask(redoTask))
    }

    fun invalidate() {
        invalidatorTick.intValue++
    }

    fun saveRotationAfterOtherOperation() {
        addAngle()
        resetRotation()
    }

    fun restoreRotationAfterUndoOtherOperation() {
        if (rotationAngles.isNotEmpty()) {
            prevRotationAngle = rotationAngles.pop()
            rotationAngle.floatValue = prevRotationAngle
        }
    }

    fun addDrawPath(path: DrawPath) {
        drawPaths.add(path)
        undoStack.add(DRAW)
    }

    fun clearEdits() {
        clearPaths()
        clearResizes()
        clearRotations()
        clearCrop()
        undoStack.clear()
        redoStack.clear()
        restoreOriginalBackgroundImage()
        scaleToFit()
    }

    fun clearRedo() {
        redoPaths.clear()
        redoCropStack.clear()
        redoRotationAngles.clear()
        redoResize.clear()
        redoStack.clear()
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

    fun cancelCropMode() {
        backgroundImage.value = backgroundImage2.value
        editMatrix.reset()
    }

    fun cancelRotateMode() {
        rotationAngle.floatValue = prevRotationAngle
        editMatrix.reset()
    }

    fun cancelResizeMode() {
        backgroundImage.value = backgroundImage2.value
        editMatrix.reset()
    }

    fun calcCenter() = Offset(
        availableDrawAreaSize.value.width / 2f,
        availableDrawAreaSize.value.height / 2f
    )

    internal fun clearRedoPath() {
        redoPaths.clear()
    }

    private fun addAngle() {
        rotationAngles.add(prevRotationAngle)
    }

    private fun clearPaths() {
        drawPaths.clear()
        redoPaths.clear()
        invalidate()
    }

    private fun clearResizes() {
        resizes.clear()
        redoResize.clear()
    }

    private fun resetRotation() {
        rotationAngle.floatValue = 0f
        prevRotationAngle = 0f
    }

    private fun clearRotations() {
        rotationAngles.clear()
        redoRotationAngles.clear()
        resetRotation()
    }

    private fun updateAvailableDrawArea(bitmap: ImageBitmap? = backgroundImage.value) {
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

    private fun clearCrop() {
        cropStack.clear()
        redoCropStack.clear()
    }

    private fun restoreOriginalBackgroundImage() {
        backgroundImage.value = originalBackgroundImage.value
        updateAvailableDrawArea()
    }

    private fun scaleMatrix(viewParams: ImageViewParams) {
        matrix.setScale(viewParams.scale.x, viewParams.scale.y)
        backgroundMatrix.setScale(viewParams.scale.x, viewParams.scale.y)
        if (prevRotationAngle != 0f) {
            val centerX = viewParams.drawArea.width / 2f
            val centerY = viewParams.drawArea.height / 2f
            matrix.postRotate(prevRotationAngle, centerX, centerY)
        }
    }

    private fun scaleEditMatrix(viewParams: ImageViewParams, isRotating: Boolean) {
        editMatrix.setScale(viewParams.scale.x, viewParams.scale.y)
        backgroundMatrix.setScale(viewParams.scale.x, viewParams.scale.y)
        if (rotationAngle.floatValue != 0f && isRotating) {
            val centerX = viewParams.drawArea.width / 2f
            val centerY = viewParams.drawArea.height / 2f
            editMatrix.postRotate(rotationAngle.floatValue, centerX, centerY)
        }
    }

    private fun undoOperation(operation: Operation) {
        operation.undo()
    }

    private fun redoOperation(operation: Operation) {
        operation.redo()
    }

    companion object {
        const val DRAW = "draw"
        const val CROP = "crop"
        const val RESIZE = "resize"
        const val ROTATE = "rotate"
        const val BLUR = "blur"
    }
}