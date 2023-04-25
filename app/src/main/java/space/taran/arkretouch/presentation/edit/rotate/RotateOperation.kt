package space.taran.arkretouch.presentation.edit.rotate

import android.graphics.Matrix
import space.taran.arkretouch.presentation.drawing.EditManager
import space.taran.arkretouch.presentation.edit.Operation
import space.taran.arkretouch.presentation.utils.rotate
import java.util.Stack

class RotateOperation(private val editManager: EditManager) : Operation {

    override fun apply() {
        editManager.apply {
            toggleRotateMode()
            matrix.set(editMatrix)
            editMatrix.reset()
            addRotation()
        }
    }

    override fun undo() {
        editManager.apply {
            if (rotationAngles.isNotEmpty()) {
                redoRotationAngles.push(prevRotationAngle)
                prevRotationAngle = rotationAngles.pop()
                matrix.reset()
                rotate(prevRotationAngle)
            }
        }
    }

    override fun redo() {
        editManager.apply {
            if (redoRotationAngles.isNotEmpty()) {
                rotationAngles.push(prevRotationAngle)
                prevRotationAngle = redoRotationAngles.pop()
                matrix.reset()
                rotate(prevRotationAngle)
            }
        }
    }

    fun rotate(matrix: Matrix, angle: Float, px: Float, py: Float) {
        matrix.rotate(angle, Center(px, py))
    }

    data class Center(
        val x: Float,
        val y: Float
    )
}
