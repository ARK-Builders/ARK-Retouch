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
                keepCroppedPaths()
                addCrop()
                addAngle()
                resetRotation()
                matrix.reset()
                toggleCropMode()
            }
        }
    }
}
