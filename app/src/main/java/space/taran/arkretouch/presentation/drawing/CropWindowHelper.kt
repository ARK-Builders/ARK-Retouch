package space.taran.arkretouch.presentation.drawing

import android.view.MotionEvent

class CropWindowHelper(
    private val editManager: EditManager,
    private val screenDensity: Float
) {
    private val SIDE_DETECTOR_TOLERANCE = 7 * screenDensity

    private var isTouchedRight = false
    private var isTouchedLeft = false
    private var isTouchedTop = false
    private var isTouchedBottom = false
    private var isTouchedTopLeft = false
    private var isTouchedBottomLeft = false
    private var isTouchedTopRight = false
    private var isTouchedBottomRight = false
    var isTouched = false

    fun detectTouchedSide(event: MotionEvent): Boolean {
        val captureArea = editManager.captureArea.value ?: return false
        val mappedX = event.x - captureArea.left
        val mappedY = event.y - captureArea.top

        isTouchedLeft =
            mappedX >= -SIDE_DETECTOR_TOLERANCE && mappedX <= SIDE_DETECTOR_TOLERANCE
        isTouchedTop =
            mappedY >= -SIDE_DETECTOR_TOLERANCE && mappedY <= SIDE_DETECTOR_TOLERANCE
        isTouchedRight = mappedX >= captureArea.width - SIDE_DETECTOR_TOLERANCE &&
            mappedX <= captureArea.width + SIDE_DETECTOR_TOLERANCE
        isTouchedBottom = mappedY >= captureArea.height - SIDE_DETECTOR_TOLERANCE &&
            mappedY <= captureArea.height + SIDE_DETECTOR_TOLERANCE
        isTouchedTopLeft = isTouchedLeft && isTouchedTop
        isTouchedTopRight = isTouchedTop && isTouchedRight
        isTouchedBottomLeft = isTouchedBottom && isTouchedLeft
        isTouchedBottomRight = isTouchedBottom && isTouchedRight
        isTouched = isTouchedLeft || isTouchedRight ||
            isTouchedTop || isTouchedBottom

        return isTouched
    }

    fun handleMove(dx: Float, dy: Float) {
        val oldCaptureArea = editManager.captureArea.value ?: return
        val captureArea = when {
            isTouchedTopLeft -> oldCaptureArea.copy(
                left = oldCaptureArea.left - dx,
                top = oldCaptureArea.top - dy,
                width = oldCaptureArea.width + dx,
                height = oldCaptureArea.height + dy,
            )
            isTouchedTopRight -> oldCaptureArea.copy(
                top = oldCaptureArea.top - dy,
                width = oldCaptureArea.width - dx,
                height = oldCaptureArea.height + dy,
            )
            isTouchedBottomLeft -> oldCaptureArea.copy(
                left = oldCaptureArea.left - dx,
                width = oldCaptureArea.width + dx,
                height = oldCaptureArea.height - dy,
            )
            isTouchedBottomRight -> oldCaptureArea.copy(
                width = oldCaptureArea.width - dx,
                height = oldCaptureArea.height - dy,
            )
            isTouchedLeft -> oldCaptureArea.copy(
                left = oldCaptureArea.left - dx,
                width = oldCaptureArea.width + dx,
            )
            isTouchedRight -> oldCaptureArea.copy(
                width = oldCaptureArea.width - dx
            )
            isTouchedTop -> oldCaptureArea.copy(
                top = oldCaptureArea.top - dy,
                height = oldCaptureArea.height + dy,
            )
            isTouchedBottom -> oldCaptureArea.copy(
                height = oldCaptureArea.height - dy
            )
            else -> oldCaptureArea
        }
        editManager.captureArea.value = captureArea
    }
}
