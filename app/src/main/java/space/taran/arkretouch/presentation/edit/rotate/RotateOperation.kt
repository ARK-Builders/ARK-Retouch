package space.taran.arkretouch.presentation.edit.rotate

import space.taran.arkretouch.presentation.drawing.EditManager
import space.taran.arkretouch.presentation.edit.Operation

class RotateOperation(private val editManager: EditManager) : Operation {

    override fun apply() {
        editManager.apply {
            toggleRotateMode()
            matrix.set(editMatrix)
            editMatrix.reset()
            addRotation()
        }
    }
}
