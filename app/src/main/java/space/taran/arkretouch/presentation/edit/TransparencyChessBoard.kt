package space.taran.arkretouch.presentation.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer

private class TransparencyChessBoard {
    fun create(boardSize: Size, canvas: Canvas) {
        val boxSize = Size(SQUARE_SIZE, SQUARE_SIZE)
        val numberOfBoxesOnHeight = (boardSize.height / boxSize.width).toInt()
        val numberOfBoxesOnWidth = (boardSize.width / boxSize.width).toInt()
        var color = DARK
        val paint = Paint().also {
            it.color = color
        }

        0.rangeTo(numberOfBoxesOnWidth).forEach { i ->
            0.rangeTo(numberOfBoxesOnHeight).forEach { j ->
                val offsetX = boxSize.width * i
                val offsetY = boxSize.height * j
                val offset = Offset(offsetX, offsetY)
                val box = Rect(offset, boxSize)
                if (j == 0) {
                    if (color == paint.color) {
                        switchPaintColor(paint)
                    }
                    color = paint.color
                }
                switchPaintColor(paint)
                canvas.drawRect(box, paint)
            }
        }
    }

    private fun switchPaintColor(paint: Paint) {
        if (paint.color == DARK)
            paint.color = LIGHT
        else paint.color = DARK
    }

    companion object {
        private const val SQUARE_SIZE = 100f
        private val LIGHT = Color.White
        private val DARK = Color.LightGray
    }
}

private fun transparencyChessBoard(canvas: Canvas, size: Size) {
    TransparencyChessBoard().create(size, canvas)
}

@Composable
fun TransparencyChessBoardCanvas(modifier: Modifier) {
    Canvas(
        modifier
            .background(Color.Transparent)
            .graphicsLayer(alpha = 0.99f)
    ) {
        drawIntoCanvas { canvas ->
            transparencyChessBoard(canvas, size)
        }
    }
}
