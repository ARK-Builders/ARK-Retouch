package space.taran.arkretouch.presentation.edit.crop

import androidx.compose.ui.graphics.asImageBitmap
import space.taran.arkretouch.presentation.drawing.EditManager
import space.taran.arkretouch.presentation.edit.Operation
import space.taran.arkretouch.presentation.edit.resize
import space.taran.arkretouch.presentation.utils.crop

class CropOperation(
    private val editManager: EditManager
) : Operation {

    override fun apply() {
        editManager.apply {
            cropWindow.apply {
                val image = resize(
                    getBitmap().crop(getCropParams()).asImageBitmap(),
                    availableDrawAreaSize.value.width,
                    availableDrawAreaSize.value.height
                )
                updateAvailableDrawArea(image)
                backgroundImage.value = image
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
                updateAvailableDrawArea(image)
                restoreRotationAfterUndoOtherOperation()
                backgroundImage.value = image
                redrawEditedPaths()
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
                updateAvailableDrawArea(image)
                backgroundImage.value = image
                keepEditedPaths()
                updateRevised()
            }
        }
    }
}
