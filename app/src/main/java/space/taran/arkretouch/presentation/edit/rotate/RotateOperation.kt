package dev.arkbuilders.arkretouch.presentation.edit.rotate

import android.graphics.Matrix
import dev.arkbuilders.arkretouch.presentation.drawing.EditManager
import dev.arkbuilders.arkretouch.presentation.edit.Operation
import dev.arkbuilders.arkretouch.presentation.utils.rotate

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
    }

    data class Center(
        val x: Float,
        val y: Float
    )
}
