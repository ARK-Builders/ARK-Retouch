package space.taran.arkretouch.presentation.edit.crop

import androidx.compose.ui.graphics.asImageBitmap
import space.taran.arkretouch.presentation.drawing.EditManager
import space.taran.arkretouch.presentation.edit.Operation
import space.taran.arkretouch.presentation.utils.crop

class CropOperation(
    private val editManager: EditManager
) : Operation {

    override fun apply() {
        editManager.apply {
            cropWindow.apply {
                val image = getBitmap().crop(getCropParams()).asImageBitmap()
                backgroundImage.value = image
                keepEditedPaths()
                addCrop()
                saveRotationAfterOtherOperation()
                scaleToFit()
                toggleCropMode()
            }
        }
    }

    override fun undo() {
        editManager.apply {
            if (cropStack.isNotEmpty()) {
                val image = cropStack.pop()
                redoCropStack.push(backgroundImage.value)
                backgroundImage.value = image
                restoreRotationAfterUndoOtherOperation()
                scaleToFit()
                redrawEditedPaths()
                updateRevised()
            }
        }
    }

    override fun redo() {
        editManager.apply {
            if (redoCropStack.isNotEmpty()) {
                val image = redoCropStack.pop()
                cropStack.push(backgroundImage.value)
                backgroundImage.value = image
                saveRotationAfterOtherOperation()
                scaleToFit()
                keepEditedPaths()
                updateRevised()
            }
        }
    }
}
