package dev.arkbuilders.arkretouch.presentation.drawing

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.view.MotionEvent
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import dev.arkbuilders.arkretouch.data.ImageDefaults
import dev.arkbuilders.arkretouch.data.Resolution
import dev.arkbuilders.arkretouch.presentation.edit.ImageViewParams
import dev.arkbuilders.arkretouch.presentation.edit.Operation
import dev.arkbuilders.arkretouch.presentation.edit.blur.BlurOperation
import dev.arkbuilders.arkretouch.presentation.edit.crop.CropOperation
import dev.arkbuilders.arkretouch.presentation.edit.crop.CropWindow
import dev.arkbuilders.arkretouch.presentation.edit.draw.DrawOperation
import dev.arkbuilders.arkretouch.presentation.edit.fitBackground
import dev.arkbuilders.arkretouch.presentation.edit.fitImage
import dev.arkbuilders.arkretouch.presentation.edit.resize.ResizeOperation
import dev.arkbuilders.arkretouch.presentation.edit.rotate.RotateOperation
import dev.arkbuilders.arkretouch.presentation.picker.toDp
import dev.arkbuilders.arkretouch.presentation.utils.calculateRotationFromOneFingerGesture
import timber.log.Timber
import java.util.Stack
import kotlin.system.measureTimeMillis

class EditManager {
    companion object {
        private const val TAG = "EDIT"
        private const val DEFAULT_SCALE_FACTOR = 1f
        private const val DEFAULT_ZOOM_FACTOR = 1f
        private val DEFAULT_OFFSET_FACTOR = Offset.Zero

        private const val DRAW = "draw"
        private const val CROP = "crop"
        private const val RESIZE = "resize"
        private const val ROTATE = "rotate"
        private const val BLUR = "blur"
    }

    private var scale = 1f
    var zoomScale = 1f
    private var offset: Offset = Offset.Zero
    // TODO remove This is not related to here
    var showEyeDropperHint by mutableStateOf(false)

    private var path = Path()
    private val currentPoint = PointF(0f, 0f)

    fun reset() {
        if (isEditMode() || isBlurMode.value) {
            scale = DEFAULT_SCALE_FACTOR
            zoomScale = DEFAULT_ZOOM_FACTOR
            offset = DEFAULT_OFFSET_FACTOR
        }
    }

    fun isEditMode(): Boolean {
        return isRotateMode.value || isZoomMode.value || isPanMode.value
    }

    fun onTouch(event: PointerEvent) {
        when (true) {
            (isRotateMode.value) -> {
                val angle = event
                    .calculateRotationFromOneFingerGesture(calcCenter())
                rotate(angle)
                invalidatorTick.value++
            }

            else -> {
                if (isZoomMode.value) {
                    scale *= event.calculateZoom()
                    zoomScale = scale
                }
                if (isPanMode.value) {
                    val pan = event.calculatePan()
                    offset = Offset(
                        offset.x + pan.x,
                        offset.y + pan.y
                    )
                }
            }
        }
    }

    fun isCroppingMode(): Boolean {
        return isCropMode.value
    }

    @Composable
    fun getBoxSize(): DpSize {
        return DpSize(
            availableDrawAreaSize.value.width.toDp(),
            availableDrawAreaSize.value.height.toDp()
        )
    }

    fun onTouchFilter(event: MotionEvent) {
        val eventX = event.x
        val eventY = event.y
        val tmpMatrix = Matrix()
        matrix.invert(tmpMatrix)
        val mappedXY = floatArrayOf(
            event.x / zoomScale,
            event.y / zoomScale
        )
        tmpMatrix.mapPoints(mappedXY)
        val mappedX = mappedXY[0]
        val mappedY = mappedXY[1]

        when (true) {
            isResizeMode.value -> {}
            isBlurMode.value -> handleBlurEvent(event.action, eventX, eventY)
            isCropMode.value -> handleCropEvent(event.action, eventX, eventY)
            isEyeDropperMode.value -> handleEyeDropEvent(
                event.action,
                event.x,
                event.y
            )

            else -> handleDrawEvent(event.action, mappedX, mappedY)
        }
        invalidatorTick.value++
    }

    private fun handleDrawEvent(action: Int, eventX: Float, eventY: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(eventX, eventY)
                currentPoint.x = eventX
                currentPoint.y = eventY
                drawOperation.draw(path)
                applyOperation()
            }

            MotionEvent.ACTION_MOVE -> {
                path.quadraticBezierTo(
                    currentPoint.x,
                    currentPoint.y,
                    (eventX + currentPoint.x) / 2,
                    (eventY + currentPoint.y) / 2
                )
                currentPoint.x = eventX
                currentPoint.y = eventY
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                // draw a dot
                if (eventX == currentPoint.x &&
                    eventY == currentPoint.y
                ) {
                    path.lineTo(currentPoint.x, currentPoint.y)
                }

                clearRedoPath()
                updateRevised()
                path = Path()
            }

            else -> {}
        }
    }

    private fun handleCropEvent(action: Int, eventX: Float, eventY: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                currentPoint.x = eventX
                currentPoint.y = eventY
                cropWindow.detectTouchedSide(Offset(eventX, eventY))
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = CropWindow.computeDeltaX(currentPoint.x, eventX)
                val deltaY = CropWindow.computeDeltaY(currentPoint.y, eventY)

                cropWindow.setDelta(Offset(deltaX, deltaY))
                currentPoint.x = eventX
                currentPoint.y = eventY
            }
        }
    }

    private fun handleEyeDropEvent(action: Int, eventX: Float, eventY: Float) {
        applyEyeDropper(action, eventX.toInt(), eventY.toInt())
    }

    private fun handleBlurEvent(action: Int, eventX: Float, eventY: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                currentPoint.x = eventX
                currentPoint.y = eventY
            }
            MotionEvent.ACTION_MOVE -> {
                val position = Offset(
                    currentPoint.x,
                    currentPoint.y
                )
                val delta = Offset(
                    CropWindow.computeDeltaX(currentPoint.x, eventX),
                    CropWindow.computeDeltaY(currentPoint.y, eventY)
                )
                blurOperation.move(position, delta)
                currentPoint.x = eventX
                currentPoint.y = eventY
            }
        }
    }

    private fun applyEyeDropper(action: Int, x: Int, y: Int) {
        try {
            val bitmap = getEditedImage().asAndroidBitmap()
            val imageX = (x * bitmapScale.x).toInt()
            val imageY = (y * bitmapScale.y).toInt()
            val pixel = bitmap.getPixel(imageX, imageY)
            val color = Color(pixel)
            if (color == Color.Transparent) {
                showEyeDropperHint = true
                return
            }
            when (action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_UP -> {
                    trackColor(color)
                    toggleEyeDropper()
                    menusVisible = true
                }
            }
            setPaintColor(color)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error applying eye dropper")
        }
    }

    private fun getEditedImage(): ImageBitmap {
        var bitmap = ImageBitmap(
            imageSize.width,
            imageSize.height,
            ImageBitmapConfig.Argb8888
        )
        var pathBitmap: ImageBitmap? = null
        val time = measureTimeMillis {
            val matrix = Matrix()
            if (drawPaths.isNotEmpty()) {
                pathBitmap = ImageBitmap(
                    imageSize.width,
                    imageSize.height,
                    ImageBitmapConfig.Argb8888
                )
                val pathCanvas = Canvas(pathBitmap!!)
                drawPaths.forEach {
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
                    val centerX = imageSize.width / 2f
                    val centerY = imageSize.height / 2f
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
                    val centerX = imageSize.width / 2
                    val centerY = imageSize.height / 2
                    matrix.setRotate(prevRotationAngle, centerX.toFloat(), centerY.toFloat())
                    canvas.nativeCanvas.setMatrix(matrix)
                }
                canvas.drawRect(Rect(Offset.Zero, imageSize.toSize()), backgroundPaint)
                if (drawPaths.isNotEmpty()) {
                    canvas.drawImage(pathBitmap!!, Offset.Zero, Paint())
                }
            }
        }
        Timber.tag(TAG).d("processing edits took ${time / 1000} s ${time % 1000} ms")
        return bitmap
    }

    fun onDraw(context: Context, canvas: Canvas) {
        var matrix = this.matrix
        if (isRotateMode.value || isResizeMode.value || isBlurMode.value)
            matrix = editMatrix
        if (isCropMode.value) matrix = Matrix()
        canvas.nativeCanvas.setMatrix(matrix)
        if (isResizeMode.value) return
        if (isBlurMode.value) {
            blurOperation.draw(context, canvas)
            return
        }
        if (isCropMode.value) {
            cropWindow.show(canvas)
            return
        }
        val rect = Rect(
            Offset.Zero,
            imageSize.toSize()
        )
        canvas.drawRect(
            rect,
            Paint().also { it.color = Color.Transparent }
        )
        canvas.clipRect(rect, ClipOp.Intersect)
        if (drawPaths.isNotEmpty()) {
            drawPaths.forEach {
                canvas.drawPath(it.path, it.paint)
            }
        }
    }

    fun onDrawBackground(canvas: Canvas, matrix: Matrix) {
        backgroundImage.value?.let {
            canvas.nativeCanvas.drawBitmap(
                it.asAndroidBitmap(),
                matrix,
                null
            )
        } ?: run {
            val rect = Rect(Offset.Zero, imageSize.toSize())
            canvas.nativeCanvas.setMatrix(matrix)
            canvas.drawRect(rect, backgroundPaint)
            canvas.clipRect(rect, ClipOp.Intersect)
        }
    }




    private val drawPaint: MutableState<Paint> = mutableStateOf(defaultPaint())

    private val _paintColor: MutableState<Color> =
        mutableStateOf(drawPaint.value.color)
    val paintColor: State<Color> = _paintColor
    private val _backgroundColor = mutableStateOf(Color.Transparent)
    val backgroundColor: State<Color> = _backgroundColor

    private val erasePaint: Paint = Paint().apply {
        shader = null
        color = backgroundColor.value
        style = PaintingStyle.Stroke
        blendMode = BlendMode.SrcOut
    }

    val backgroundPaint: Paint
        get() {
            return Paint().apply {
                color = backgroundImage.value?.let {
                    Color.Transparent
                } ?: backgroundColor.value
            }
        }

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
    val backgroundImage2 = mutableStateOf<ImageBitmap?>(null)
    private val originalBackgroundImage = mutableStateOf<ImageBitmap?>(null)

    val matrix = Matrix()
    val editMatrix = Matrix()
    val backgroundMatrix = Matrix()
    val rectMatrix = Matrix()

    private val matrixScale = mutableStateOf(1f)
    lateinit var bitmapScale: ResizeOperation.Scale
        private set

    val imageSize: IntSize
        get() {
            return if (isResizeMode.value)
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
    val isRotateMode: State<Boolean> = _isRotateMode

    private val _isResizeMode = mutableStateOf(false)
    val isResizeMode: State<Boolean> = _isResizeMode

    private val _isEyeDropperMode = mutableStateOf(false)
    val isEyeDropperMode: State<Boolean> = _isEyeDropperMode

    private val _isBlurMode = mutableStateOf(false)
    val isBlurMode: State<Boolean> = _isBlurMode

    private val _isZoomMode = mutableStateOf(false)
    val isZoomMode: State<Boolean> = _isZoomMode
    private val _isPanMode = mutableStateOf(false)
    val isPanMode: State<Boolean> = _isPanMode

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
        operation.apply()
    }

    private fun undoOperation(operation: Operation) {
        operation.undo()
    }

    private fun redoOperation(operation: Operation) {
        operation.redo()
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
        matrixScale.value = viewParams.scale.x
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
        maxHeight: Int = drawAreaSize.value.height
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
        scaleEditMatrix(viewParams)
        updateAvailableDrawArea(viewParams.drawArea)
        return viewParams
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

    private fun scaleEditMatrix(viewParams: ImageViewParams) {
        editMatrix.setScale(viewParams.scale.x, viewParams.scale.y)
        backgroundMatrix.setScale(viewParams.scale.x, viewParams.scale.y)
        if (prevRotationAngle != 0f && isRotateMode.value) {
            val centerX = viewParams.drawArea.width / 2f
            val centerY = viewParams.drawArea.height / 2f
            editMatrix.postRotate(prevRotationAngle, centerX, centerY)
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

    fun updateAvailableDrawAreaByMatrix() {
        val drawArea = backgroundImage.value?.let {
            val drawWidth = it.width * matrixScale.value
            val drawHeight = it.height * matrixScale.value
            IntSize(
                drawWidth.toInt(),
                drawHeight.toInt()
            )
        } ?: run {
            val drawWidth = resolution.value?.width!! * matrixScale.value
            val drawHeight = resolution.value?.height!! * matrixScale.value
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

    fun rotate(angle: Float) {
        val centerX = availableDrawAreaSize.value.width / 2
        val centerY = availableDrawAreaSize.value.height / 2
        if (isRotateMode.value) {
            rotationAngle.value += angle
            rotateOperation.rotate(
                editMatrix,
                angle,
                centerX.toFloat(),
                centerY.toFloat()
            )
            return
        }
        rotateOperation.rotate(
            matrix,
            angle,
            centerX.toFloat(),
            centerY.toFloat()
        )
    }

    fun addRotation() {
        if (canRedo.value) clearRedo()
        rotationAngles.add(prevRotationAngle)
        undoStack.add(ROTATE)
        prevRotationAngle = rotationAngle.value
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

    fun clearEdits() {
        clearPaths()
        clearResizes()
        clearRotations()
        clearCrop()
        blurOperation.clear()
        undoStack.clear()
        redoStack.clear()
        restoreOriginalBackgroundImage()
        scaleToFit()
        updateRevised()
    }

    private fun clearRedo() {
        redoPaths.clear()
        redoCropStack.clear()
        redoRotationAngles.clear()
        redoResize.clear()
        redoStack.clear()
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
        if (isRotateMode.value) editMatrix.set(matrix)
    }

    fun toggleCropMode() {
        _isCropMode.value = !isCropMode.value
        if (!isCropMode.value) cropWindow.close()
    }

    fun toggleZoomMode() {
        _isZoomMode.value = !isZoomMode.value
    }

    fun togglePanMode() {
        _isPanMode.value = !isPanMode.value
    }

    fun cancelCropMode() {
        backgroundImage.value = backgroundImage2.value
        editMatrix.reset()
    }

    fun cancelRotateMode() {
        rotationAngle.value = prevRotationAngle
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
        val allowedArea = availableDrawAreaSize.value
        val xOffset = ((drawArea.width - allowedArea.width) / 2f)
            .coerceAtLeast(0f)
        val yOffset = ((drawArea.height - allowedArea.height) / 2f)
            .coerceAtLeast(0f)
        return Offset(xOffset, yOffset)
    }

    fun calcCenter() = Offset(
        availableDrawAreaSize.value.width / 2f,
        availableDrawAreaSize.value.height / 2f
    )
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
