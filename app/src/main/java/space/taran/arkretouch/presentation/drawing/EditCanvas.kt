@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.drawing

import android.graphics.PointF
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import space.taran.arkretouch.presentation.edit.EditViewModel
import space.taran.arkretouch.presentation.picker.toDp
import kotlin.math.atan2

@Composable
fun EditCanvas(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    Box(
        Modifier.background(Color.White)
    ) {
        var bitmap: ImageBitmap? = null
        if (!editManager.isRotateMode.value)
            bitmap = editManager.backgroundImage.value
        val modifier = if (bitmap != null)
            Modifier
                .size(
                    bitmap.width.toDp(),
                    bitmap.height.toDp()
                )
        else Modifier.fillMaxSize()
        EditDrawCanvas(modifier, viewModel)
    }
}

@Composable
fun EditDrawCanvas(modifier: Modifier, viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    var path = Path()
    val currentPoint = PointF(0f, 0f)

    Canvas(
        modifier = modifier
            // Eraser leaves black line instead of erasing without this hack, it uses BlendMode.SrcOut
            // https://stackoverflow.com/questions/65653560/jetpack-compose-applying-porterduffmode-to-image
            // Provide a slight opacity to for compositing into an
            // offscreen buffer to ensure blend modes are applied to empty pixel information
            // By default any alpha != 1.0f will use a compositing layer by default
            .graphicsLayer(alpha = 0.99f)
            .pointerInteropFilter { event ->
                val eventX = event.x
                val eventY = event.y

                if (!editManager.isRotateMode.value)
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
                else when (event.action) {
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
                editManager.invalidatorTick.value++
                true
            }
    ) {
        // force recomposition on invalidatorTick change
        editManager.invalidatorTick.value

        drawIntoCanvas { canvas ->
            editManager.apply {
                if (isRotateMode.value)
                    canvas.nativeCanvas.setMatrix(rotationMatrix)
                backgroundImage.value?.let {
                    canvas.nativeCanvas.drawBitmap(
                        it.asAndroidBitmap(),
                        if (editManager.isRotateMode.value)
                            editManager.calcImageOffset().x
                        else
                            0f,
                        if (editManager.isRotateMode.value)
                            editManager.calcImageOffset().y
                        else
                            0f,
                        null
                    )
                }
                if (!isRotateMode.value)
                    drawPaths.forEach {
                        canvas.drawPath(it.path, it.paint)
                    }
                if (isRotateMode.value)
                    rotationGrid.draw(canvas)
            }
        }
    }
}
