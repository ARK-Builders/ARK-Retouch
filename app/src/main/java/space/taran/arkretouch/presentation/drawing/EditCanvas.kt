@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.drawing

import android.graphics.PointF
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import space.taran.arkretouch.presentation.edit.EditViewModel
import space.taran.arkretouch.presentation.edit.crop.CropWindow
import space.taran.arkretouch.presentation.edit.crop.CropWindow.Companion.computeDeltaX
import space.taran.arkretouch.presentation.edit.crop.CropWindow.Companion.computeDeltaY
import space.taran.arkretouch.presentation.utils.crop
import timber.log.Timber

@Composable
fun EditCanvas(viewModel: EditViewModel) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        viewModel.editManager.backgroundImage.value?.let { imageBitmap ->
            drawImage(
                imageBitmap,
                topLeft = viewModel.editManager.calcImageOffset()
            )
        }
    }
    EditDrawCanvas(viewModel)
}

@Composable
fun EditDrawCanvas(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    var path = Path()
    val currentPoint = PointF(0f, 0f)
    val cropWindow = CropWindow()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            // Eraser leaves black line instead of erasing without this hack, it uses BlendMode.SrcOut
            // https://stackoverflow.com/questions/65653560/jetpack-compose-applying-porterduffmode-to-image
            // Provide a slight opacity to for compositing into an
            // offscreen buffer to ensure blend modes are applied to empty pixel information
            // By default any alpha != 1.0f will use a compositing layer by default
            .graphicsLayer(alpha = 0.99f)
            .pointerInteropFilter { event ->
                val eventX = event.x
                val eventY = event.y

                if (!editManager.isCropMode.value)
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
                else
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            currentPoint.x = eventX
                            currentPoint.y = eventY
                            cropWindow.detectTouchedSide(
                                Offset(eventX, eventY)
                            )
                            Timber.tag("is touched").d(
                                cropWindow.isTouched(eventX, eventY).toString()
                            )
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = computeDeltaX(currentPoint.x, eventX)
                            val deltaY = computeDeltaY(currentPoint.y, eventY)

                            cropWindow.setDelta(Offset(deltaX, deltaY))
                            currentPoint.x = eventX
                            currentPoint.y = eventY
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            cropWindow.detectTouchedSide(
                                Offset(eventX, eventY)
                            )
                        }
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

            if (editManager.isCropMode.value) {
                cropWindow.init(
                    viewModel.getCombinedImageBitmap().asAndroidBitmap(),
                    canvas
                )
                editManager.crop = {
                    with(cropWindow) {
                        editManager.keepCroppedPaths()
                        getBitmap().crop(
                            getCropRect()
                        )
                    }
                }
            } else cropWindow.reInit()
        }
    }
}
