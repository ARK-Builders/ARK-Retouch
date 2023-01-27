package space.taran.arkretouch.presentation.edit.crop

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle

class CropWindow {

    private lateinit var bitmap: Bitmap

    private lateinit var canvas: Canvas

    private var width: Float = 0F
    private var height: Float = 0F

    private lateinit var rect: Rect

    private val isTouchedRight = mutableStateOf(false)
    private val isTouchedLeft = mutableStateOf(false)
    private val isTouchedTop = mutableStateOf(false)
    private val isTouchedBottom = mutableStateOf(false)

    private var delta = Offset(
        0F,
        0F
    )

    private val paint = Paint()

    private var isInitialized = false

    init {
        paint.color = Color.Gray
        paint.style = PaintingStyle.Stroke
        paint.strokeWidth = 8F
    }

    fun init(
        bitmap: Bitmap,
        canvas: Canvas
    ) {
        if (!isInitialized) {
            this.bitmap = bitmap
            this.canvas = canvas
            width = bitmap.width.toFloat() - OFFSET
            height = bitmap.height.toFloat() - OFFSET
            rect = Rect(
                OFFSET,
                OFFSET,
                width,
                height
            )
            this.canvas.drawRect(rect, paint)
            isInitialized = true
        } else {
            update()
        }
    }

    fun reInit() {
        isInitialized = false
    }

    fun setDelta(delta: Offset) {
        this.delta = delta
    }

    private fun update() {
        val left = if (isTouchedLeft.value)
            rect.left + delta.x
        else rect.left
        val right = if (isTouchedRight.value)
            rect.right + delta.x
        else rect.right
        val top = if (isTouchedTop.value)
            rect.top + delta.y
        else rect.top
        val bottom = if (isTouchedBottom.value)
            rect.bottom + delta.y
        else rect.bottom
        rect = Rect(
            left,
            top,
            right,
            bottom
        )
        canvas.drawRect(rect, paint)
    }

    fun isTouched(x: Float, y: Float) =
        rect.contains(
            Offset(
                x,
                y
            )
        )

    fun detectTouchedSide(eventPoint: Offset) {
        isTouchedLeft.value = eventPoint.x >=
            (rect.left - SIDE_DETECTOR_TOLERANCE) &&
            eventPoint.x <= (rect.left + SIDE_DETECTOR_TOLERANCE)
        isTouchedRight.value = eventPoint.x >=
            (rect.right - SIDE_DETECTOR_TOLERANCE) &&
            eventPoint.x <= (rect.right + SIDE_DETECTOR_TOLERANCE)
        isTouchedTop.value = eventPoint.y >=
            (rect.top - SIDE_DETECTOR_TOLERANCE) &&
            eventPoint.y <= (rect.top + SIDE_DETECTOR_TOLERANCE)
        isTouchedBottom.value = eventPoint.y >=
            (rect.bottom - SIDE_DETECTOR_TOLERANCE) &&
            eventPoint.y <= (rect.bottom + SIDE_DETECTOR_TOLERANCE)
    }

    fun getCropRect(): Rect {
        isInitialized = false
        return rect
    }

    fun getBitmap() = bitmap

    companion object {
        const val OFFSET = 100F
        const val SIDE_DETECTOR_TOLERANCE = 30

        fun computeDeltaX(initialX: Float, currentX: Float) = currentX - initialX

        fun computeDeltaY(initialY: Float, currentY: Float) = currentY - initialY
    }
}
