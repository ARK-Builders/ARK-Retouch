package space.taran.arkretouch.presentation.edit.rotate

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.unit.IntSize
import space.taran.arkretouch.presentation.drawing.EditManager

class RotateGrid {

    private lateinit var bitmap: ImageBitmap
    private var drawAreaSize = IntSize.Zero
    private lateinit var rect: Rect
    private var xLines = mutableListOf<Pair<Offset, Offset>>()
    private var yLines = mutableListOf<Pair<Offset, Offset>>()
    private lateinit var editManager: EditManager
    private var top = 0f
    private var left = 0f
    private var right = 0f
    private var bottom = 0f
    private var offset = Offset(0f, 0f)
    private var rotatedBitmapOffset = Offset(0f, 0f)
    private var pivot = Offset(0f, 0f)
    private var paint = Paint()
    private var linePaint = Paint()

    init {
        paint.style = PaintingStyle.Stroke
        paint.strokeWidth = 8F
        paint.color = Color.Black
        linePaint.style = PaintingStyle.Stroke
        linePaint.strokeWidth = 4F
        linePaint.color = Color.Black
    }

    fun init(
        editManager: EditManager,
        bitmap: ImageBitmap,
        offset: Offset = Offset(
            0f,
            0f
        )
    ) {
        this.bitmap = bitmap
        this.editManager = editManager
        this.drawAreaSize = editManager.drawAreaSize.value
        this.offset = offset
        left = offset.x
        top = offset.y
        right = left + bitmap.width
        bottom = top + bitmap.height
        calculatePivot()
        create()
    }

    private fun create() {
        rect = Rect(
            left,
            top,
            right,
            bottom
        )
        createLines()
    }

    private fun createLines() {
        val lineXOffset: Float
        var numberOfYSegments = 4f
        var numberOfXSegments = 4f
        val lineYOffset: Float
        var lineDisplacement = 0f
        if (rect.width < rect.height || rect.width == rect.height) {
            lineXOffset = rect.width / numberOfXSegments
            numberOfYSegments = (rect.height / lineXOffset)
            lineYOffset = rect.height / numberOfYSegments
        } else {
            lineYOffset = rect.height / numberOfYSegments
            numberOfXSegments = (rect.width / lineYOffset)
            lineXOffset = rect.width / numberOfXSegments
        }

        for (i in 1..numberOfXSegments.toInt()) {
            lineDisplacement += lineXOffset
            val x = rect.left + lineDisplacement
            val firstPoint = Offset(
                x,
                rect.top
            )
            val secondPoint = Offset(
                x,
                rect.bottom
            )
            xLines.add(
                Pair(
                    firstPoint,
                    secondPoint
                )
            )
        }
        lineDisplacement = 0f
        for (i in 1..numberOfYSegments.toInt()) {
            lineDisplacement += lineYOffset
            val y = rect.top + lineDisplacement
            val firstPoint = Offset(
                rect.left,
                y
            )
            val secondPoint = Offset(
                rect.right,
                y
            )
            yLines.add(
                Pair(
                    firstPoint,
                    secondPoint
                )
            )
        }
    }

    private fun calculatePivot() {
        pivot = Offset(
            drawAreaSize.width / 2f,
            drawAreaSize.height / 2f
        )
    }

    private fun drawLines(canvas: Canvas) {
        for (line in xLines) {
            canvas.drawLine(line.first, line.second, linePaint)
        }
        for (line in yLines) {
            canvas.drawLine(line.first, line.second, linePaint)
        }
    }

    fun get() = rect

    fun draw(canvas: Canvas, angle: Float = 0f) {
        // canvas.rotate(angle, pivot.x, pivot.y)
        // resize()
        canvas.drawRect(rect, paint)
        drawLines(canvas)
    }

    fun getCropParams(): CropParams {
        val x = rect.left - rotatedBitmapOffset.x
        val y = rect.top - rotatedBitmapOffset.y
        return CropParams.create(
            x.toInt(),
            y.toInt(),
            rect.width.toInt(),
            rect.height.toInt()
        )
    }

    fun calcRotatedBitmapOffset() {
        rotatedBitmapOffset = editManager.calcImageOffset()
    }

    private fun resize() {
        val horizontalAxisDetectorModulus =
            (editManager.rotationAngle.value / 90f) % 2f
        val oddModulus = horizontalAxisDetectorModulus % 2f
        val isOdd = oddModulus == 1f || oddModulus == -1f
        if (isOdd) {
            val aspectRatio = rect.width / rect.height
            val newHeight = rect.width
            val newWidth = newHeight * aspectRatio
            top = offset.y + ((rect.height - newHeight) / 2)
            left = offset.x
            right = left + newWidth
            bottom = top + newHeight
            create()
        }
    }

    class CropParams private constructor(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    ) {
        companion object {
            fun create(x: Int, y: Int, width: Int, height: Int) =
                CropParams(x, y, width, height)
        }
    }
}
