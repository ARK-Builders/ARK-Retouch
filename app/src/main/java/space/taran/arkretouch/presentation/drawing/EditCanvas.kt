@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.drawing

import android.graphics.PointF
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import space.taran.arkretouch.presentation.edit.EditViewModel
import space.taran.arkretouch.presentation.picker.pxToDp
import space.taran.arkretouch.presentation.picker.toPx

@Composable
fun EditCanvas(viewModel: EditViewModel) {
    Box(modifier = Modifier.background(Color.White)) {
        EditCanvasImage(viewModel)
        EditCanvasPaint(viewModel)
    }
}

@Composable
private fun EditCanvasImage(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    val drawArea = editManager.drawArea.value ?: return

    viewModel.editManager.backgroundImage.value?.let { imageBitmap ->
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas {
                it.nativeCanvas.drawBitmap(
                    imageBitmap.asAndroidBitmap(),
                    drawArea.left,
                    drawArea.top,
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
    val drawArea = editManager.drawArea.value ?: return

    Canvas(
        modifier = Modifier
            .padding(
                start = drawArea.left.pxToDp(),
                top = drawArea.top.pxToDp()
            )
            .size(
                width = drawArea.width.pxToDp(),
                height = drawArea.height.pxToDp()
            )
            // Eraser leaves black line instead of erasing without this hack, it uses BlendMode.SrcOut
            // https://stackoverflow.com/questions/65653560/jetpack-compose-applying-porterduffmode-to-image
            // Provide a slight opacity to for compositing into an
            // offscreen buffer to ensure blend modes are applied to empty pixel information
            // By default any alpha != 1.0f will use a compositing layer by default
            .graphicsLayer(alpha = 0.99f)
            .pointerInteropFilter { event ->
                val eventX = event.x
                val eventY = event.y

                when (event.action) {
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
                    else -> false
                }
                editManager.invalidatorTick.value++
                true
            }
    ) {
        // force recomposition on invalidatorTick change
        editManager.invalidatorTick.value

        drawIntoCanvas { canvas ->
            editManager.drawPaths.forEach {
                canvas.drawPath(it.path, it.paint)
            }
        }
    }
}
