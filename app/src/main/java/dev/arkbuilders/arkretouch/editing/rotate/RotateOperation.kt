package dev.arkbuilders.arkretouch.editing.rotate

import android.graphics.Matrix
import dev.arkbuilders.arkretouch.editing.Operation
import dev.arkbuilders.arkretouch.editing.manager.EditManager
import dev.arkbuilders.arkretouch.utils.rotate

class RotateOperation(
    private val editManager: EditManager,
    private val onApply: () -> Unit
) : Operation {

    override fun apply() {
        editManager.apply {
            matrix.set(editMatrix)
            editMatrix.reset()
            addRotation()
            onApply()
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