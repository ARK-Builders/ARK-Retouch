package space.taran.arkretouch.presentation.edit.crop

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import space.taran.arkretouch.presentation.edit.crop.AspectRatio.CROP_2_3
import space.taran.arkretouch.presentation.edit.crop.AspectRatio.CROP_4_5
import space.taran.arkretouch.presentation.edit.crop.AspectRatio.CROP_9_16
import space.taran.arkretouch.presentation.edit.crop.AspectRatio.CROP_SQUARE
import space.taran.arkretouch.presentation.edit.crop.AspectRatio.isCropFree
import space.taran.arkretouch.presentation.edit.crop.AspectRatio.isCropSquare
import space.taran.arkretouch.presentation.edit.crop.AspectRatio.isCrop_2_3
import space.taran.arkretouch.presentation.edit.crop.AspectRatio.isCrop_4_5
import space.taran.arkretouch.presentation.edit.crop.AspectRatio.isCrop_9_16
import timber.log.Timber

class CropWindow {

    private lateinit var bitmap: Bitmap

    private var aspectRatio = 0F

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
    private val isTouched = mutableStateOf(false)
    private val isTouchedTopLeft = mutableStateOf(false)
    private val isTouchedBottomLeft = mutableStateOf(false)
    private val isTouchedTopRight = mutableStateOf(false)
    private val isTouchedBottomRight = mutableStateOf(false)
    private val isTouchedOnCorner = mutableStateOf(false)

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

    private fun create() {
        width = bitmap.width.toFloat() - HORIZONTAL_OFFSET
        height = bitmap.height.toFloat() - VERTICAL_OFFSET
        rect = Rect(
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            width,
            height
        )
    }

    fun close() {
        isInitialized = false
    }

    fun init(bitmap: Bitmap, fitBitmap: (Bitmap, Int, Int) -> Bitmap) {
        if (!isInitialized) {
            Timber.tag("crop-window").d("Initialising")
            this.bitmap = bitmap
            create()
            this.bitmap = fitBitmap(
                bitmap,
                rect.width.toInt(),
                rect.height.toInt()
            )
            calculateBitmapDiff(bitmap)
            resizeByBitmap()
            isInitialized = true
        }
    }

    fun show(canvas: Canvas) {
        if (isInitialized) {
            update()
        }
        draw(canvas)
    }

    fun setDelta(delta: Offset) {
        this.delta = delta
    }

    fun isAspectRatioFixed() =
        isCropSquare.value || isCrop_4_5.value ||
            isCrop_9_16.value || isCrop_2_3.value

    private fun update() {
        var left = rect.left
        var right = rect.right
        var top = rect.top
        var bottom = rect.bottom

        if (isAspectRatioFixed()) {
            if (isTouchedOnCorner.value) {
                delta = if (delta.x != 0f) {
                    Offset(
                        delta.x,
                        delta.x * aspectRatio
                    )
                } else {
                    Offset(
                        delta.y,
                        delta.y * (1 / aspectRatio)
                    )
                }
                if (isTouchedTopLeft.value) {
                    left = rect.left + delta.x
                    top = rect.top + delta.y
                }
                if (isTouchedTopRight.value) {
                    right = rect.right + delta.x
                    top = rect.top - delta.y
                }
                if (isTouchedBottomLeft.value) {
                    left = rect.left + delta.x
                    bottom = rect.bottom - delta.y
                }
                if (isTouchedBottomRight.value) {
                    right = rect.right + delta.x
                    bottom = rect.bottom + delta.y
                }
            } else {
                if (isTouchedLeft.value) {
                    left = rect.left + delta.x
                    top = rect.top + ((delta.x * aspectRatio) / 2f)
                    bottom = rect.bottom - ((delta.x * aspectRatio) / 2f)
                }
                if (isTouchedRight.value) {
                    right = rect.right + delta.x
                    top = rect.top - ((delta.x * aspectRatio) / 2f)
                    bottom = rect.bottom + ((delta.x * aspectRatio) / 2f)
                }
                if (isTouchedTop.value) {
                    top = rect.top + delta.y
                    left = rect.left + ((delta.y * (1f / aspectRatio)) / 2f)
                    right = rect.right - ((delta.y * (1f / aspectRatio)) / 2f)
                }
                if (isTouchedBottom.value) {
                    bottom = rect.bottom + delta.y
                    left = rect.left - ((delta.y * (1f / aspectRatio)) / 2f)
                    right = rect.right + ((delta.y * (1f / aspectRatio)) / 2f)
                }
            }
        } else {
            left = if (isTouchedLeft.value)
                rect.left + delta.x
            else rect.left
            right = if (isTouchedRight.value)
                rect.right + delta.x
            else rect.right
            top = if (isTouchedTop.value)
                rect.top + delta.y
            else rect.top
            bottom = if (isTouchedBottom.value)
                rect.bottom + delta.y
            else rect.bottom
        }

        fun isNotMinSize() = (right - left) >= MIN_WIDTH &&
            (bottom - top) >= MIN_HEIGHT

        fun isNotMaxSize() = (right - left) <= bitmap.width &&
            (bottom - top) <= bitmap.height

        fun isWithinBounds() = left >= bitmapWidthDiff &&
            right <= bitmapWidthDiff + bitmap.width &&
            top >= bitmapHeightDiff &&
            bottom <= bitmapHeightDiff + bitmap.height

        if (isTouchedInside.value) {
            right += delta.x
            left += delta.x
            top += delta.y
            bottom += delta.y
            if (left < bitmapWidthDiff) {
                left = bitmapWidthDiff
                right = left + rect.width
            }
            if (right > bitmapWidthDiff + bitmap.width) {
                right = bitmapWidthDiff + bitmap.width
                left = right - rect.width
            }
            if (top < bitmapHeightDiff) {
                top = bitmapHeightDiff
                bottom = top + rect.height
            }
            if (bottom > bitmapHeightDiff + bitmap.height) {
                bottom = bitmapHeightDiff + bitmap.height
                top = bottom - rect.height
            }
        }

        if (isNotMaxSize() && isNotMinSize() && isWithinBounds()) {
            recreate(
                left,
                top,
                right,
                bottom
            )
        }
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
        isTouchedTopLeft.value = isTouchedLeft.value && isTouchedTop.value
        isTouchedTopRight.value = isTouchedTop.value && isTouchedRight.value
        isTouchedBottomLeft.value = isTouchedBottom.value && isTouchedLeft.value
        isTouchedBottomRight.value = isTouchedBottom.value && isTouchedRight.value
        isTouchedOnCorner.value = isTouchedTopLeft.value ||
            isTouchedTopRight.value || isTouchedBottomLeft.value ||
            isTouchedBottomRight.value
        isTouched.value = isTouchedLeft.value || isTouchedRight.value ||
            isTouchedTop.value || isTouchedBottom.value ||
            isTouchedInside.value
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

    fun resize() {
        if (isInitialized) {
            resizeByAspectRatio()
        }
    }

    private fun resizeByBitmap() {
        val newBottom = this.bitmap.height + bitmapHeightDiff
        val newRight = this.bitmap.width + bitmapWidthDiff
        recreate(
            bitmapWidthDiff,
            bitmapHeightDiff,
            newRight,
            newBottom
        )
    }

    private fun resizeByAspectRatio() {
        if (isCropFree.value)
            resizeByBitmap()
        else {
            when {
                isCropSquare.value ->
                    aspectRatio = CROP_SQUARE.y / CROP_SQUARE.x
                isCrop_4_5.value ->
                    aspectRatio = CROP_4_5.y / CROP_4_5.x
                isCrop_9_16.value ->
                    aspectRatio = CROP_9_16.y / CROP_9_16.x
                isCrop_2_3.value ->
                    aspectRatio = CROP_2_3.y / CROP_2_3.x
            }
            computeSize()
        }
    }

    private fun recreate(
        newLeft: Float,
        newTop: Float,
        newRight: Float,
        newBottom: Float
    ) {
        rect = Rect(
            newLeft,
            newTop,
            newRight,
            newBottom
        )
    }

    private fun computeSize() {
        val newWidth = bitmap.width
        val newHeight = bitmap.width * aspectRatio
        val newLeft = bitmapWidthDiff
        val newTop = bitmapHeightDiff +
            (bitmap.height - newHeight) / 2
        val newRight = newLeft + newWidth
        val newBottom = newTop + newHeight
        recreate(
            newLeft,
            newTop,
            newRight,
            newBottom
        )
    }

    private fun draw(canvas: Canvas) {
        canvas.drawRect(
            rect,
            paint
        )
    }

    fun getCropParams(): CropParams {
        val x = rect.left - bitmapWidthDiff
        val y = rect.top - bitmapHeightDiff
        return CropParams.create(
            x.toInt(), y.toInt(),
            rect.width.toInt(),
            rect.height.toInt()
        )
    }

    fun getBitmap() = bitmap

    companion object {
        private const val HORIZONTAL_OFFSET = 150F
        private const val VERTICAL_OFFSET = 250F
        private const val SIDE_DETECTOR_TOLERANCE = 30
        private const val MIN_WIDTH = 150F
        private const val MIN_HEIGHT = 150F

        fun computeDeltaX(initialX: Float, currentX: Float) = currentX - initialX

        fun computeDeltaY(initialY: Float, currentY: Float) = currentY - initialY
    }

    class CropParams private constructor(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    ) {
        companion object {
            fun create(
                x: Int,
                y: Int,
                width: Int,
                height: Int
            ) = CropParams(
                x,
                y,
                width,
                height
            )
        }
    }
}
