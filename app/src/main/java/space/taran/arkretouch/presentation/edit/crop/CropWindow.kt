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

    private var aspectRatio = Pair(0, 0)
    private var isFixedAspectRatio = false

    private var width: Float = 0F
    private var height: Float = 0F

    private var bitmapWidthDiff = 0F
    private var bitmapHeightDiff = 0F

    private lateinit var rect: Rect

    private val isTouchedRight = mutableStateOf(false)
    private val isTouchedLeft = mutableStateOf(false)
    private val isTouchedTop = mutableStateOf(false)
    private val isTouchedBottom = mutableStateOf(false)
    private val isTouchedInside = mutableStateOf(false)

    private var delta = Offset(
        0F,
        0F
    )

    private val paint = Paint()

    private var isInitialized = false

    init {
        paint.color = Color.Gray
        paint.style = PaintingStyle.Stroke
        paint.strokeWidth = 5F
    }

    fun init(
        bitmap: Bitmap,
        canvas: Canvas,
        fitBitmap: (Bitmap, Int, Int) -> Bitmap
    ) {
        if (!isInitialized) {
            this.bitmap = bitmap
            this.canvas = canvas
            width = bitmap.width.toFloat() - HORIZONTAL_OFFSET
            height = bitmap.height.toFloat() - VERTICAL_OFFSET
            rect = Rect(
                HORIZONTAL_OFFSET,
                VERTICAL_OFFSET,
                width,
                height
            )

            this.bitmap = fitBitmap(
                bitmap,
                rect.width.toInt(),
                rect.height.toInt()
            )

            calculateBitmapDiff(bitmap)
            resizeByBitmap()

            this.canvas.drawRect(rect, paint)
            isInitialized = true
        } else update()
    }

    fun reInit() {
        isInitialized = false
    }

    fun setDelta(delta: Offset) {
        this.delta = delta
    }

    fun fixAspectRatio(aspectRatio: Pair<Int, Int>) {
        this.aspectRatio = aspectRatio
        isFixedAspectRatio = aspectRatio.first > 0 &&
            aspectRatio.second > 0
    }

    private fun update() {
        var left = if (isTouchedLeft.value)
            rect.left + delta.x
        else rect.left
        var right = if (isTouchedRight.value)
            rect.right + delta.x
        else rect.right
        var top = if (isTouchedTop.value)
            rect.top + delta.y
        else rect.top
        var bottom = if (isTouchedBottom.value)
            rect.bottom + delta.y
        else rect.bottom

        if (isTouchedInside.value) {
            left += delta.x
            right += delta.x
            top += delta.y
            bottom += delta.y
        }

        fun isNotMinSize() = (right - left) >= MIN_WIDTH &&
            (bottom - top) >= MIN_HEIGHT

        fun isNotMaxSize() = left >= bitmapWidthDiff &&
            right <= bitmap.width + bitmapWidthDiff &&
            top >= bitmapHeightDiff &&
            bottom <= bitmap.height + bitmapHeightDiff

        if (isNotMaxSize() && isNotMinSize()) {
            rect = Rect(
                left,
                top,
                right,
                bottom
            )
        }
        canvas.drawRect(rect, paint)
    }

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
        isTouchedInside.value = eventPoint.x >=
            rect.left + SIDE_DETECTOR_TOLERANCE &&
            eventPoint.x <= rect.right - SIDE_DETECTOR_TOLERANCE &&
            eventPoint.y >= rect.top + SIDE_DETECTOR_TOLERANCE &&
            eventPoint.y <= rect.bottom - SIDE_DETECTOR_TOLERANCE
    }

    private fun calculateBitmapDiff(bitmap: Bitmap) {
        bitmapWidthDiff = (
            bitmap.width.toFloat() -
                this.bitmap.width.toFloat()
            ) / 2
        bitmapHeightDiff = (
            bitmap.height.toFloat() -
                this.bitmap.height.toFloat()
            ) / 2
    }

    private fun resizeByBitmap() {
        if (
            rect.height.toInt() > this.bitmap.height ||
            rect.width.toInt() > this.bitmap.width
        ) {
            val newBottom = this.bitmap.height + bitmapHeightDiff
            val newRight = this.bitmap.width + bitmapWidthDiff
            rect = Rect(
                bitmapWidthDiff,
                bitmapHeightDiff,
                newRight,
                newBottom
            )
        }
    }

    private fun getCropRect(): Rect {
        isInitialized = false
        return rect
    }

    fun getCropParams(): CropParams {
        val x = rect.left - bitmapWidthDiff
        val y = rect.top - bitmapHeightDiff
        return CropParams(
            x.toInt(), y.toInt(),
            rect.width.toInt(),
            rect.height.toInt()
        )
    }

    fun getBitmap() = bitmap

    companion object {
        const val HORIZONTAL_OFFSET = 150F
        const val VERTICAL_OFFSET = 250F
        const val SIDE_DETECTOR_TOLERANCE = 30
        const val MIN_WIDTH = 150F
        const val MIN_HEIGHT = 150F

        fun computeDeltaX(initialX: Float, currentX: Float) = currentX - initialX

        fun computeDeltaY(initialY: Float, currentY: Float) = currentY - initialY
    }

    data class CropParams internal constructor(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
}
