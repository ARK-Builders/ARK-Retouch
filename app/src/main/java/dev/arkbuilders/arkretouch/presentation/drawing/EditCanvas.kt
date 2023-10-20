@file:OptIn(ExperimentalComposeUiApi::class)

package dev.arkbuilders.arkretouch.presentation.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arkbuilders.arkretouch.presentation.edit.EditViewModel
import dev.arkbuilders.arkretouch.presentation.edit.TransparencyChessBoardCanvas

@Composable
fun EditCanvas(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    val scale by viewModel.scale.collectAsStateWithLifecycle()
    val offset by viewModel.offset.collectAsStateWithLifecycle()

    Box(contentAlignment = Alignment.Center) {
        val modifier = Modifier
            .size(editManager.getBoxSize())
            .graphicsLayer {
                editManager.reset()
                /**
                 * Eraser leaves black line instead of erasing without this hack, it uses BlendMode.SrcOut
                 * https://stackoverflow.com/questions/65653560/jetpack-compose-applying-porterduffmode-to-image
                 * Provide a slight opacity to for compositing into an
                 * offscreen buffer to ensure blend modes are applied to empty pixel information
                 * By default any alpha != 1.0f will use a compositing layer by default
                 * */
                alpha = 0.99f

                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
        TransparencyChessBoardCanvas(modifier, editManager)
        BackgroundCanvas(modifier, editManager)
        DrawCanvas(modifier, viewModel)
    }
    if (editManager.isEditMode()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Any()) {
                    forEachGesture {
                        awaitPointerEventScope {
                            awaitFirstDown()
                            do {
                                val event = awaitPointerEvent()
                                editManager.onTouch(event)
                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
        ) {}
    }
}

@Composable
fun BackgroundCanvas(modifier: Modifier, editManager: EditManager) {
    Canvas(modifier) {
        editManager.apply {
            invalidatorTick.value
            var matrix = matrix
            if (
                isCropMode.value || isRotateMode.value ||
                isResizeMode.value || isBlurMode.value
            )
                matrix = editMatrix
            drawIntoCanvas { canvas ->
                editManager.onDrawBackground(canvas, matrix)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawCanvas(modifier: Modifier, viewModel: EditViewModel) {
    val context = LocalContext.current
    val editManager = viewModel.editManager

    val drawModifier = if (editManager.isCroppingMode()) Modifier.fillMaxSize()
    else modifier

    Canvas(modifier = drawModifier.pointerInteropFilter { event ->
        editManager.onTouchFilter(event)
        true
    }) {
        // force recomposition on invalidatorTick change
        editManager.invalidatorTick.value
        drawIntoCanvas { canvas -> editManager.onDraw(context, canvas) }
    }
}
