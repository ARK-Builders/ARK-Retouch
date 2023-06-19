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
                scaleToFit()
                keepEditedPaths()
                addCrop()
                saveRotationAfterOtherOperation()
                toggleCropMode()
            }
        }
    }

    override fun undo() {
        editManager.apply {
            if (cropStack.isNotEmpty()) {
                val image = cropStack.pop()
                redoCropStack.push(backgroundImage.value)
                restoreRotationAfterUndoOtherOperation()
                backgroundImage.value = image
                redrawEditedPaths()
                scaleToFit()
                updateRevised()
            }
        }
    }

    override fun redo() {
        editManager.apply {
            if (redoCropStack.isNotEmpty()) {
                val image = redoCropStack.pop()
                saveRotationAfterOtherOperation()
                cropStack.push(backgroundImage.value)
                backgroundImage.value = image
                keepEditedPaths()
                scaleToFit()
                updateRevised()
            }
        }
    }
}
