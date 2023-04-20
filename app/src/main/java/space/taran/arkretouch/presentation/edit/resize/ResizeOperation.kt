package space.taran.arkretouch.presentation.edit.resize

import space.taran.arkretouch.presentation.drawing.EditManager
import space.taran.arkretouch.presentation.edit.Operation

class ResizeOperation(private val editManager: EditManager) : Operation {
    override fun apply() {
        editManager.apply {
            addResize()
            toggleResizeMode()
        }
    }
}
