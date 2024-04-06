package dev.arkbuilders.arkretouch.presentation.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.toSize
import dev.arkbuilders.arkretouch.editing.manager.EditManager

@Composable
fun BackgroundCanvas(
    modifier: Modifier,
    isCropping: Boolean,
    editManager: EditManager
) {
    Canvas(modifier) {
        editManager.apply {
            invalidatorTick.value
            var matrix = matrix
            if (
                isCropping || isRotateMode.value ||
                isResizeMode.value || isBlurMode.value
            ) {
                matrix = editMatrix
            }
            drawIntoCanvas { canvas ->
                backgroundImage.value?.let {
                    canvas.nativeCanvas.drawBitmap(
                        it.asAndroidBitmap(),
                        matrix,
                        null
                    )
                } ?: run {
                    val rect = Rect(
                        Offset.Zero,
                        imageSize.toSize()
                    )
                    canvas.nativeCanvas.setMatrix(matrix)
                    canvas.drawRect(rect, backgroundPaint)
                    canvas.clipRect(rect, ClipOp.Intersect)
                }
            }
        }
    }
}