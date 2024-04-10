package dev.arkbuilders.arkretouch.editing.crop

import androidx.compose.ui.graphics.asImageBitmap
import dev.arkbuilders.arkretouch.editing.Operation
import dev.arkbuilders.arkretouch.editing.manager.EditManager
import dev.arkbuilders.arkretouch.utils.crop

class CropOperation(
    private val editManager: EditManager,
    val onApply: () -> Unit
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
                onApply()
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