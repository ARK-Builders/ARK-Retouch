package dev.arkbuilders.arkretouch.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import dev.arkbuilders.arkretouch.presentation.picker.toDp
import dev.arkbuilders.arkretouch.presentation.viewmodels.EditViewModel
import dev.arkbuilders.arkretouch.presentation.views.TransparencyChessBoardCanvas
import dev.arkbuilders.arkretouch.utils.calculateRotationFromOneFingerGesture

@Composable
fun EditCanvasScreen(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun resetScaleAndTranslate() {
        editManager.apply {
            if (
                viewModel.isRotating() || viewModel.isCropping() || viewModel.isResizing() ||
                isBlurMode.value
            ) {
                scale = 1f; zoomScale = scale; offset = Offset.Zero
            }
        }
    }

    Box(contentAlignment = Alignment.Center) {
        val modifier = Modifier
            .size(
                editManager.availableDrawAreaSize.value.width.toDp(),
                editManager.availableDrawAreaSize.value.height.toDp()
            )
            .graphicsLayer {
                resetScaleAndTranslate()

                // Eraser leaves black line instead of erasing without this hack, it uses BlendMode.SrcOut
                // https://stackoverflow.com/questions/65653560/jetpack-compose-applying-porterduffmode-to-image
                // Provide a slight opacity to for compositing into an
                // offscreen buffer to ensure blend modes are applied to empty pixel information
                // By default any alpha != 1.0f will use a compositing layer by default
                alpha = 0.99f

                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
        TransparencyChessBoardCanvas(modifier, viewModel.imageSize, editManager)
        BackgroundCanvas(
            modifier,
            viewModel.isCropping(),
            viewModel.isRotating(),
            viewModel.isResizing(),
            viewModel.imageSize,
            viewModel.drawingState.backgroundPaint,
            editManager
        )
        DrawCanvas(modifier, viewModel)
    }
    if (
        viewModel.isRotating() || viewModel.isZooming() ||
        viewModel.isPanning()
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Any()) {
                    forEachGesture {
                        awaitPointerEventScope {
                            awaitFirstDown()
                            do {
                                val event = awaitPointerEvent()
                                when (true) {
                                    (viewModel.isRotating()) -> {
                                        val angle = event
                                            .calculateRotationFromOneFingerGesture(
                                                editManager.calcCenter()
                                            )
                                        viewModel.onRotate(angle)
                                        editManager.invalidatorTick.value++
                                    }
                                    else -> {
                                        if (viewModel.isZooming()) {
                                            scale *= event.calculateZoom()
                                            editManager.zoomScale = scale
                                        }
                                        if (viewModel.isPanning()) {
                                            val pan = event.calculatePan()
                                            offset = Offset(
                                                offset.x + pan.x,
                                                offset.y + pan.y
                                            )
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
        ) {}
    }
}