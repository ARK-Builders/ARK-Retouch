package space.taran.arkretouch.presentation.edit.rotate

import android.graphics.Matrix
import space.taran.arkretouch.presentation.drawing.EditManager
import space.taran.arkretouch.presentation.edit.Operation
import space.taran.arkretouch.presentation.edit.resize.ResizeOperation
import space.taran.arkretouch.presentation.utils.rotate

class RotateOperation(private val editManager: EditManager) : Operation {

    private var scale = ResizeOperation.Scale(1f, 1f)
    private var cumulativeAngle: Int = 0
    var imageSize = editManager.imageSize
        private set

    fun init() {
        imageSize = editManager.imageSize
        cumulativeAngle = 0
    }

    override fun apply(extraBlock: () -> Unit) {
        editManager.apply {
            toggleRotateMode()
            matrix.set(editMatrix)
            editMatrix.reset()
            imageSizes.push(this@RotateOperation.imageSize)
            addRotation(scale)
        }
    }

    override fun undo() {
        editManager.apply {
            if (rotationAngles.isNotEmpty()) {
                redoRotationAngles.push(prevRotationAngle)
                prevRotationAngle = rotationAngles.pop()
                scaleToFit()
            }
        }
    }

    override fun redo() {
        editManager.apply {
            if (redoRotationAngles.isNotEmpty()) {
                rotationAngles.push(prevRotationAngle)
                prevRotationAngle = redoRotationAngles.pop()
                scaleToFit()
            }
        }
    }

    fun rotate(matrix: Matrix, angle: Float, px: Float, py: Float) {
        matrix.rotate(angle, Center(px, py))
        val shouldSwitchLayout = editManager.rotationAngle.value.toInt() !=
            cumulativeAngle && editManager.rotationAngle.value.toInt() % 45 == 0
        if (shouldSwitchLayout) {
            cumulativeAngle = editManager.rotationAngle.value.toInt()
            if (editManager.smartLayout.value) {
                val viewParams = editManager.switchLayout()
                scale = viewParams.scale
            }
        }
    }

    fun cancel() {
        editManager.updateImageSize(imageSize)
    }

    data class Center(
        val x: Float,
        val y: Float
    )
}
