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
    private val topBoard = mutableListOf<List<Rect>>()
    private val bottomBoard = mutableListOf<List<Rect>>()

    private val boxPaint = Paint().also {
        it.color = Color.LightGray
    }

    fun create(boardSize: Size) {
        val boxSize = Size(50f, 50f)
        val halfHeight = boardSize.height / 2
        val numberOfBoxesOnHalfHeight = (halfHeight / boxSize.width).toInt()
        val numberOfBoxesOnWidth = (boardSize.width / boxSize.width).toInt()
        0.rangeTo(numberOfBoxesOnWidth).forEach { i ->
            val boxes = mutableListOf<Rect>()
            0.rangeTo(numberOfBoxesOnHalfHeight).forEach { j ->
                val offsetX = boxSize.width * i
                val offsetY = halfHeight - (boxSize.height * (j + 1))
                val offset = Offset(offsetX, offsetY)
                val box = Rect(offset, boxSize)
                boxes.add(box)
            }
            topBoard.add(boxes)
        }
        0.rangeTo(numberOfBoxesOnWidth).forEach { i ->
            val boxes = mutableListOf<Rect>()
            0.rangeTo(numberOfBoxesOnHalfHeight).forEach { j ->
                val offsetX = boxSize.width * i
                val offsetY = halfHeight + (boxSize.height * j)
                val offset = Offset(offsetX, offsetY)
                val box = Rect(offset, boxSize)
                boxes.add(box)
            }
            bottomBoard.add(boxes)
        }
    }

    fun draw(canvas: Canvas) {
        drawBoard(canvas, topBoard)
        boxPaint.color = Color.DarkGray
        drawBoard(canvas, bottomBoard)
    }

    private fun switchRectPaintColor(paint: Paint = boxPaint) {
        if (paint.color == Color.LightGray)
            boxPaint.color = Color.DarkGray
        else boxPaint.color = Color.LightGray
    }

    private fun drawBoard(canvas: Canvas, board: List<List<Rect>>) {
        var color = boxPaint.color
        board.forEach { line ->
            line.forEachIndexed { index, box ->
                if (index > 0) switchRectPaintColor()
                if (index == 0) {
                    if (color == boxPaint.color) {
                        switchRectPaintColor()
                    }
                    color = boxPaint.color
                }
                canvas.drawRect(box, boxPaint)
            }
        }
    }
}

private fun transparencyChessBoard(canvas: Canvas, size: Size) {
    val board = TransparencyChessBoard()
    board.create(size)
    board.draw(canvas)
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
