@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.drawing

import android.graphics.Matrix
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.GestureDetectorCompat
import space.taran.arkretouch.presentation.edit.EditViewModel
import space.taran.arkretouch.presentation.picker.pxToDp
import kotlin.math.atan2

@Composable
fun EditCanvas(viewModel: EditViewModel) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        EditCanvasImage(viewModel)
        EditCanvasPaint(viewModel)
        CropWindowHolder(viewModel)
    }
}

@Composable
private fun CropWindowHolder(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    val captureArea = editManager.captureArea.value ?: return

    if (editManager.isCropMode.value) {
        Canvas(Modifier.fillMaxSize()) {
            val topLeft = Offset(captureArea.left, captureArea.top)
            val size = Size(captureArea.width, captureArea.height)
            val circlePath = Path().apply {
                addRect(Rect(topLeft, size))
            }
            clipPath(circlePath, clipOp = ClipOp.Difference) {
                drawRect(SolidColor(Color.Black.copy(alpha = 0.6f)))
            }
        }
        CropWindow(viewModel = viewModel)
    }
}

@Composable
private fun EditCanvasImage(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    val initialDrawAreaOffset = editManager.initialDrawAreaOffset

    editManager.invalidatorTick.value

    viewModel.editManager.backgroundImage.value?.let { bitmap ->
        Canvas(
            modifier = Modifier
                .size(
                    width = editManager.availableDrawAreaSize.value.width.pxToDp(),
                    height = editManager.availableDrawAreaSize.value.height.pxToDp()
                )
        ) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.setMatrix(editManager.matrix)
                canvas.nativeCanvas.drawBitmap(
                    bitmap.asAndroidBitmap(),
                    initialDrawAreaOffset.x,
                    initialDrawAreaOffset.y,
                    null
                )
            }
        }
    }
}

@Composable
fun EditCanvasPaint(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    var path = Path()
    val currentPoint = PointF(0f, 0f)
    val context = LocalContext.current
    val tmpPointerList = remember { mutableListOf<PointF>() }

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
                    return true
                }
            }
        )
    }

    val scaleGestureDetector = remember {
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val localScale = detector.scaleFactor
                    val pivotX =
                        tmpPointerList.map { it.x }.sum() / tmpPointerList.size
                    val pivotY =
                        tmpPointerList.map { it.y }.sum() / tmpPointerList.size
                    editManager.matrix
                        .postScale(localScale, localScale, pivotX, pivotY)
                    editManager.invalidatorTick.value++
                    return true
                }

                override fun onScaleBegin(p0: ScaleGestureDetector) = true

                override fun onScaleEnd(p0: ScaleGestureDetector) {}
            }
        )
    }

    fun handleDrawEvent(action: Int, eventX: Float, eventY: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(eventX, eventY)
                currentPoint.x = eventX
                currentPoint.y = eventY
                editManager.addDrawPath(path)
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

    fun handleRotateEvent(action: Int, eventX: Float, eventY: Float) {
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                val centerX = editManager.availableDrawAreaSize.value.width / 2
                val centerY = editManager.availableDrawAreaSize.value.height / 2
                val prevDX = currentPoint.x - centerX
                val prevDY = currentPoint.y - centerY

                val dx = eventX - centerX
                val dy = eventY - centerY

                val angle = -Math.toDegrees(
                    (atan2(prevDY, prevDX) - atan2(dy, dx)).toDouble()
                )

                if (angle != 0.0) {
                    editManager.matrix.postRotate(
                        angle.toFloat(),
                        centerX.toFloat(),
                        centerY.toFloat()
                    )
                }

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

    fun handleCropWindowTouch(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (editManager.cropWindowHelper.detectTouchedSide(event)) {
                    currentPoint.x = event.x
                    currentPoint.y = event.y
                    true
                } else
                    false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!editManager.cropWindowHelper.isTouched)
                    return false

                editManager.cropWindowHelper.handleMove(
                    currentPoint.x - event.x,
                    currentPoint.y - event.y
                )
                currentPoint.x = event.x
                currentPoint.y = event.y
                true
            }
            else -> false
        }
    }

    Canvas(
        modifier = Modifier
            .size(
                width = editManager.availableDrawAreaSize.value.width.pxToDp(),
                height = editManager.availableDrawAreaSize.value.height.pxToDp()
            )
            // Eraser leaves black line instead of erasing without this hack, it uses BlendMode.SrcOut
            // https://stackoverflow.com/questions/65653560/jetpack-compose-applying-porterduffmode-to-image
            // Provide a slight opacity to for compositing into an
            // offscreen buffer to ensure blend modes are applied to empty pixel information
            // By default any alpha != 1.0f will use a compositing layer by default
            .graphicsLayer(alpha = 0.99f)
            .pointerInteropFilter { event ->
                val tmpMatrix = Matrix()
                editManager.matrix.invert(tmpMatrix)
                val mappedXY = floatArrayOf(event.x, event.y)
                tmpMatrix.mapPoints(mappedXY)
                val mappedX = mappedXY[0]
                val mappedY = mappedXY[1]

                tmpPointerList.clear()
                repeat(event.pointerCount) { index ->
                    val coords = MotionEvent.PointerCoords()
                    event.getPointerCoords(index, coords)
                    tmpPointerList.add(PointF(coords.x, coords.y))
                }

                if (editManager.isCropMode.value) {
                    if (editManager.isMovementNotRotateMode.value) {
                        val handled = handleCropWindowTouch(event)
                        if (!handled) {
                            panGestureDetector.onTouchEvent(event)
                            scaleGestureDetector.onTouchEvent(event)
                        }
                    } else
                        handleRotateEvent(event.action, event.x, event.y)
                } else
                    handleDrawEvent(event.action, mappedX, mappedY)

                editManager.invalidatorTick.value++
                true
            }
    ) {
        // force recomposition on invalidatorTick change
        editManager.invalidatorTick.value

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.setMatrix(editManager.matrix)
            val tempDrawBitmap = ImageBitmap(
                editManager.availableDrawAreaSize.value.width,
                editManager.availableDrawAreaSize.value.height,
                ImageBitmapConfig.Argb8888
            )
            editManager.drawPaths.forEach {
                canvas.drawPath(it.path, it.paint)
            }
            canvas.nativeCanvas.drawBitmap(
                tempDrawBitmap.asAndroidBitmap(),
                0f,
                0f,
                null
            )
        }
    }
}
