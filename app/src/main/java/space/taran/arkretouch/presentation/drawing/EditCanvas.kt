@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.drawing

import android.graphics.Matrix
import android.graphics.PointF
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.IntSize
import space.taran.arkretouch.presentation.edit.EditViewModel
import space.taran.arkretouch.presentation.picker.toDp
import kotlin.math.atan2
import space.taran.arkretouch.presentation.edit.crop.CropWindow.Companion.computeDeltaX
import space.taran.arkretouch.presentation.edit.crop.CropWindow.Companion.computeDeltaY

@Composable
fun EditCanvas(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    Box(
        Modifier.background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val modifier = if (
            editManager.availableDrawAreaSize.value != IntSize.Zero
        ) Modifier.size(
            editManager.availableDrawAreaSize.value.width.toDp(),
            editManager.availableDrawAreaSize.value.height.toDp()
        )
        else Modifier.fillMaxSize()
        EditCanvasImage(modifier, editManager)
        EditDrawCanvas(modifier, viewModel)
    }
}

@Composable
fun EditCanvasImage(modifier: Modifier, editManager: EditManager) {
    val imageModifier = with(editManager) {
        if (isCropMode.value) Modifier.size(
            backgroundImage.value?.width?.toDp()!!,
            backgroundImage.value?.height?.toDp()!!
        )
        else modifier
    }
    Canvas(imageModifier) {
        editManager.apply {
            invalidatorTick.value
            var matrix = matrix
            drawIntoCanvas { canvas ->
                if (isCropMode.value || isRotateMode.value)
                    matrix = editMatrix
                backgroundImage.value?.let {
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
fun EditDrawCanvas(modifier: Modifier, viewModel: EditViewModel) {
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

    Canvas(
        modifier = drawModifier
            // Eraser leaves black line instead of erasing without this hack, it uses BlendMode.SrcOut
            // https://stackoverflow.com/questions/65653560/jetpack-compose-applying-porterduffmode-to-image
            // Provide a slight opacity to for compositing into an
            // offscreen buffer to ensure blend modes are applied to empty pixel information
            // By default any alpha != 1.0f will use a compositing layer by default
            .graphicsLayer(alpha = 0.99f)
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
                    editManager.isRotateMode.value -> handleRotateEvent(
                        event.action,
                        eventX,
                        eventY
                    )
                    editManager.isCropMode.value -> handleCropEvent(
                        event.action,
                        eventX,
                        eventY
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
                canvas.nativeCanvas.setMatrix(matrix)
                if (isCropMode.value || isRotateMode.value)
                    canvas.nativeCanvas.setMatrix(editMatrix)
                if (isResizeMode.value) return@drawIntoCanvas
                if (isCropMode.value) {
                    editManager.cropWindow.show(canvas)
                    return@drawIntoCanvas
                }
                drawPaths.forEach {
                    canvas.drawPath(it.path, it.paint)
                }
            }
        }
    }
}
