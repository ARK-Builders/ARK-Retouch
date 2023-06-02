package space.taran.arkretouch.presentation.edit.crop

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.unit.IntSize
import space.taran.arkretouch.presentation.drawing.EditManager
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

    private var offset = Offset(0f, 0f)

    private var aspectRatio = 0F

    private var width: Float = 0F
    private var height: Float = 0F

    private var drawAreaSize = IntSize.Zero

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
        paint.color = Color.LightGray
        paint.style = PaintingStyle.Stroke
        paint.strokeWidth = 5F
    }

    fun close() {
        isInitialized = false
    }

    fun init(
        editManager: EditManager,
        bitmap: Bitmap,
        fitBitmap: (Bitmap, Int, Int) -> Bitmap
    ) {
        if (!isInitialized) {
            Timber.tag("crop-window").d("Initialising")
            this.bitmap = bitmap
            drawAreaSize = editManager.drawAreaSize.value
            computeResolution()
            this.bitmap = fitBitmap(
                bitmap,
                width.toInt(),
                height.toInt()
            )
            offset = editManager.calcImageOffset()
            isInitialized = true
        }
    }

    fun updateOffset(offset: Offset) {
        val leftMove = rect.left - this.offset.x
        val topMove = rect.top - this.offset.y
        val newLeft = offset.x + leftMove
        val newTop = offset.y + topMove
        this.offset = offset
        create(
            newLeft,
            newTop,
            newLeft + rect.width,
            newTop + rect.height
        )
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

    private fun computeResolution() {
        width = drawAreaSize.width.toFloat() - 2 * HORIZONTAL_OFFSET
        height = drawAreaSize.height.toFloat() - 2 * VERTICAL_OFFSET
    }

    private fun isAspectRatioFixed() =
        isCropSquare.value || isCrop_4_5.value ||
            isCrop_9_16.value || isCrop_2_3.value

    private fun update() {
        var left = rect.left
        var right = rect.right
        var top = rect.top
        var bottom = rect.bottom

        if (isAspectRatioFixed()) {
            if (isTouchedOnCorner.value) {
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
                val newHeight = (right - left) * aspectRatio
                if (isTouchedTopLeft.value || isTouchedTopRight.value)
                    top = bottom - newHeight
                if (isTouchedBottomLeft.value || isTouchedBottomRight.value)
                    bottom = top + newHeight
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

        fun isWithinBounds() = left >= offset.x &&
            right <= offset.x + bitmap.width &&
            top >= offset.y &&
            bottom <= offset.y + bitmap.height

        if (isTouchedInside.value) {
            right += delta.x
            left += delta.x
            top += delta.y
            bottom += delta.y
            if (left < offset.x) {
                left = offset.x
                right = left + rect.width
            }
            if (right > offset.x + bitmap.width) {
                right = offset.x + bitmap.width
                left = right - rect.width
            }
            if (top < offset.y) {
                top = offset.y
                bottom = top + rect.height
            }
            if (bottom > offset.y + bitmap.height) {
                bottom = offset.y + bitmap.height
                top = bottom - rect.height
            }
        }

        if (isNotMaxSize() && isNotMinSize() && isWithinBounds()) {
            create(
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

    fun resize() {
        if (isInitialized) {
            resizeByAspectRatio()
        }
    }

    private fun resizeByBitmap() {
        val newBottom = bitmap.height.toFloat() + offset.y
        val newRight = bitmap.width.toFloat() + offset.x
        create(
            offset.x,
            offset.y,
            newRight,
            newBottom
        )
    }

    private fun resizeByAspectRatio() {
        if (isCropFree.value) {
            resizeByBitmap()
        } else {
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

    private fun create(
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
        var newWidth = bitmap.width.toFloat()
        var newHeight = bitmap.width * aspectRatio
        var newLeft = offset.x
        var newTop = offset.y +
            (bitmap.height - newHeight) / 2f
        var newRight = newLeft + newWidth
        var newBottom = newTop + newHeight

        if (newHeight > bitmap.height) {
            newHeight = bitmap.height.toFloat()
            newWidth = newHeight / aspectRatio
            newLeft = offset.x + (
                bitmap.width - newWidth
                ) / 2f
            newTop = offset.y
            newRight = newLeft + newWidth
            newBottom = newTop + newHeight
        }

        create(
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
        val x = rect.left - offset.x
        val y = rect.top - offset.y
        return CropParams.create(
            x.toInt(), y.toInt(),
            rect.width.toInt(),
            rect.height.toInt()
        )
    }

    fun getBitmap() = bitmap

    companion object {
        private const val HORIZONTAL_OFFSET = 150F
        private const val VERTICAL_OFFSET = 220F
        private const val SIDE_DETECTOR_TOLERANCE = 50
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
