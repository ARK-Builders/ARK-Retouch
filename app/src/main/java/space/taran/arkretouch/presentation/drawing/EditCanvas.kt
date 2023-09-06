@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.drawing

import android.graphics.Matrix
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.toSize
import androidx.core.view.GestureDetectorCompat
import space.taran.arkretouch.presentation.edit.EditViewModel
import kotlin.math.atan2
import space.taran.arkretouch.presentation.edit.crop.CropWindow.Companion.computeDeltaX
import space.taran.arkretouch.presentation.edit.crop.CropWindow.Companion.computeDeltaY
import space.taran.arkretouch.presentation.picker.toDp

@Composable
fun EditCanvas(viewModel: EditViewModel) {
    val context = LocalContext.current
    val currentPoint = PointF(0f, 0f)
    val editManager = viewModel.editManager
    val modifier = Modifier.size(
        editManager.availableDrawAreaSize.value.width.toDp(),
        editManager.availableDrawAreaSize.value.height.toDp()
    ).graphicsLayer(alpha = 0.99f)
    val tmpPointerList = remember {
        mutableListOf<PointF>()
    }
    val panGestureDetector = remember {
        GestureDetectorCompat(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (e2.pointerCount > 1) return false
                    editManager.matrix.postTranslate(-distanceX, -distanceY)
                    editManager.backgroundMatrix
                        .postTranslate(-distanceX, -distanceY)
                    return true
                }
            }
        )
    }
    val scaleGestureDetector = remember {
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scale = detector.scaleFactor
                    val pivotX = tmpPointerList.map { it.x }.sum() /
                        tmpPointerList.size
                    val pivotY = tmpPointerList.map { it.y }.sum() /
                        tmpPointerList.size
                    editManager.matrix.postScale(
                        scale,
                        scale,
                        pivotX,
                        pivotY
                    )
                    editManager.backgroundMatrix.postScale(
                        scale,
                        scale,
                        pivotX,
                        pivotY
                    )
                    editManager.invalidatorTick.value++
                    return true
                }
            }
        )
    }

    fun handleRotateEvent(action: Int, eventX: Float, eventY: Float) {
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                val centerX = editManager.drawAreaSize.value.width / 2
                val centerY = editManager.drawAreaSize.value.height / 2
                val prevDX = currentPoint.x - centerX
                val prevDY = currentPoint.y - centerY
                val dx = eventX - centerX
                val dy = eventY - centerY
                val angle1 = atan2(prevDY, prevDX)
                val angle2 = atan2(dy, dx)
                val degreesAngle =
                    Math.toDegrees(
                        (angle2 - angle1).toDouble()
                    )
                if (degreesAngle != 0.0)
                    editManager.rotate(degreesAngle.toFloat())
                currentPoint.x = eventX
                currentPoint.y = eventY
            }
            MotionEvent.ACTION_DOWN -> {
                currentPoint.x = eventX
                currentPoint.y = eventY
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {}
        }
    }

    Box(contentAlignment = Alignment.Center) {
        BackgroundCanvas(modifier, editManager)
        DrawCanvas(modifier, viewModel)
    }
    if (
        editManager.isRotateMode.value || editManager.isZoomMode.value ||
        editManager.isPanMode.value
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInteropFilter {
                    tmpPointerList.clear()
                    repeat(it.pointerCount) { index ->
                        val coords = MotionEvent.PointerCoords()
                        it.getPointerCoords(index, coords)
                        tmpPointerList.add(PointF(coords.x, coords.y))
                    }
                    when (true) {
                        editManager.isRotateMode.value ->
                            handleRotateEvent(it.action, it.x, it.y)

                        else -> {
                            if (editManager.isZoomMode.value)
                                scaleGestureDetector.onTouchEvent(it)
                            if (editManager.isPanMode.value)
                                panGestureDetector.onTouchEvent(it)
                        }
                    }
                    editManager.invalidatorTick.value++
                    true
                }
        ) {}
    }
}

@Composable
fun BackgroundCanvas(
    modifier: Modifier,
    editManager: EditManager
) {
    Canvas(modifier) {
        editManager.apply {
            invalidatorTick.value
            var matrix = matrix
            drawIntoCanvas { canvas ->
                backgroundImage.value?.let {
                    if (
                        isCropMode.value || isRotateMode.value ||
                        isResizeMode.value || isBlurMode.value
                    )
                        matrix = editMatrix
                    canvas.nativeCanvas.drawBitmap(
                        it.asAndroidBitmap(),
                        matrix,
                        null
                    )
                }
            }
        }
    }
}

@Composable
fun DrawCanvas(modifier: Modifier, viewModel: EditViewModel) {
    val context = LocalContext.current
    val editManager = viewModel.editManager
    var path = Path()
    val currentPoint = PointF(0f, 0f)
    val drawModifier = if (editManager.isCropMode.value) Modifier.fillMaxSize()
    else modifier

    fun handleDrawEvent(action: Int, eventX: Float, eventY: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(eventX, eventY)
                currentPoint.x = eventX
                currentPoint.y = eventY
                editManager.apply {
                    drawOperation.draw(path)
                    applyOperation()
                }
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
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                // draw a dot
                if (eventX == currentPoint.x &&
                    eventY == currentPoint.y
                ) {
                    path.lineTo(currentPoint.x, currentPoint.y)
                }

                editManager.clearRedoPath()
                editManager.updateRevised()
                path = Path()
            }
            else -> {}
        }
    }

    fun handleCropEvent(action: Int, eventX: Float, eventY: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                currentPoint.x = eventX
                currentPoint.y = eventY
                editManager.cropWindow.detectTouchedSide(
                    Offset(eventX, eventY)
                )
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX =
                    computeDeltaX(currentPoint.x, eventX)
                val deltaY =
                    computeDeltaY(currentPoint.y, eventY)

                editManager.cropWindow.setDelta(
                    Offset(
                        deltaX,
                        deltaY
                    )
                )
                currentPoint.x = eventX
                currentPoint.y = eventY
            }
        }
    }

    fun handleEyeDropEvent(action: Int, eventX: Float, eventY: Float) {
        viewModel.applyEyeDropper(action, eventX.toInt(), eventY.toInt())
    }

    fun handleBlurEvent(action: Int, eventX: Float, eventY: Float) {
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
                    computeDeltaX(currentPoint.x, eventX),
                    computeDeltaY(currentPoint.y, eventY)
                )
                editManager.blurOperation.move(position, delta)
                currentPoint.x = eventX
                currentPoint.y = eventY
            }
            else -> {}
        }
    }

    Canvas(
        modifier = drawModifier
            // Eraser leaves black line instead of erasing without this hack, it uses BlendMode.SrcOut
            // https://stackoverflow.com/questions/65653560/jetpack-compose-applying-porterduffmode-to-image
            // Provide a slight opacity to for compositing into an
            // offscreen buffer to ensure blend modes are applied to empty pixel information
            // By default any alpha != 1.0f will use a compositing layer by default
            .pointerInteropFilter { event ->
                val eventX = event.x
                val eventY = event.y
                val tmpMatrix = Matrix()
                editManager.matrix.invert(tmpMatrix)
                val mappedXY = floatArrayOf(event.x, event.y)
                tmpMatrix.mapPoints(mappedXY)
                val mappedX = mappedXY[0]
                val mappedY = mappedXY[1]

                when (true) {
                    editManager.isResizeMode.value -> {}
                    editManager.isBlurMode.value -> handleBlurEvent(
                        event.action,
                        eventX,
                        eventY
                    )

                    editManager.isCropMode.value -> handleCropEvent(
                        event.action,
                        eventX,
                        eventY
                    )

                    editManager.isEyeDropperMode.value -> handleEyeDropEvent(
                        event.action,
                        event.x,
                        event.y
                    )

                    else -> handleDrawEvent(event.action, mappedX, mappedY)
                }
                editManager.invalidatorTick.value++
                true
            }
    ) {
        // force recomposition on invalidatorTick change
        editManager.invalidatorTick.value
        drawIntoCanvas { canvas ->
            editManager.apply {
                var matrix = this.matrix
                if (isRotateMode.value || isResizeMode.value || isBlurMode.value)
                    matrix = editMatrix
                if (isCropMode.value) matrix = Matrix()
                canvas.nativeCanvas.setMatrix(matrix)
                if (isResizeMode.value) return@drawIntoCanvas
                if (isBlurMode.value) {
                    editManager.blurOperation.draw(context, canvas)
                    return@drawIntoCanvas
                }
                if (isCropMode.value) {
                    editManager.cropWindow.show(canvas)
                    return@drawIntoCanvas
                }
                val rect = Rect(Offset.Zero, imageSize.toSize())
                canvas.drawRect(rect, backgroundPaint)
                canvas.clipRect(rect, ClipOp.Intersect)
                drawPaths.forEach {
                    canvas.drawPath(it.path, it.paint)
                }
            }
        }
    }
}
