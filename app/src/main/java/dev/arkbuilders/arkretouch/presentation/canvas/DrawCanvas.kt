package dev.arkbuilders.arkretouch.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.toSize
import android.graphics.Matrix
import android.graphics.PointF
import android.view.MotionEvent
import dev.arkbuilders.arkretouch.editing.crop.CropWindow.Companion.computeDeltaX
import dev.arkbuilders.arkretouch.editing.crop.CropWindow.Companion.computeDeltaY
import dev.arkbuilders.arkretouch.presentation.viewmodels.EditViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawCanvas(modifier: Modifier, viewModel: EditViewModel, observeInvalidator: State<Int>) {
    val context = LocalContext.current
    val editManager = viewModel.editManager
    var path = Path()
    val currentPoint = PointF(0f, 0f)
    val drawModifier = if (viewModel.isCropping()) Modifier.fillMaxSize()
    else modifier

    fun handleDrawEvent(action: Int, eventX: Float, eventY: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(eventX, eventY)
                currentPoint.x = eventX
                currentPoint.y = eventY
                viewModel.onDrawPath(path)
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
                viewModel.updateUndoRedoState()
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
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                editManager.cropWindow.resetDelta()
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
                viewModel.onBlurMove(position, delta)
                currentPoint.x = eventX
                currentPoint.y = eventY
            }
            else -> {}
        }
    }

    Canvas(
        modifier = drawModifier.pointerInteropFilter { event ->
            val eventX = event.x
            val eventY = event.y
            val tmpMatrix = Matrix()
            editManager.matrix.invert(tmpMatrix)
            val mappedXY = floatArrayOf(
                event.x / editManager.zoomScale,
                event.y / editManager.zoomScale
            )
            tmpMatrix.mapPoints(mappedXY)
            val mappedX = mappedXY[0]
            val mappedY = mappedXY[1]

            when (true) {
                viewModel.isResizing() -> {}
                viewModel.isBlurring() -> handleBlurEvent(
                    event.action,
                    eventX,
                    eventY
                )

                viewModel.isCropping() -> handleCropEvent(
                    event.action,
                    eventX,
                    eventY
                )

                viewModel.isEyeDropping() -> handleEyeDropEvent(
                    event.action,
                    event.x,
                    event.y
                )

                else -> handleDrawEvent(event.action, mappedX, mappedY)
            }
            viewModel.invalidateCanvas()
            true
        }
    ) {
        drawIntoCanvas { canvas ->
            // force recomposition on invalidatorTick change
            observeInvalidator.value
            editManager.apply {
                var matrix = this.matrix
                if (viewModel.isRotating() || viewModel.isResizing() || viewModel.isBlurring())
                    matrix = editMatrix
                if (viewModel.isCropping()) matrix = Matrix()
                canvas.nativeCanvas.setMatrix(matrix)
                if (viewModel.isResizing()) return@drawIntoCanvas
                if (viewModel.isBlurring()) {
                    viewModel.onDrawBlur(context, canvas)
                    return@drawIntoCanvas
                }
                if (viewModel.isCropping()) {
                    editManager.cropWindow.show(canvas)
                    return@drawIntoCanvas
                }
                val rect = Rect(
                    Offset.Zero,
                    viewModel.imageSize.toSize()
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
        }
    }
}