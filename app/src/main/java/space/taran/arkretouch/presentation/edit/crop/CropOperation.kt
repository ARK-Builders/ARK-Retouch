package space.taran.arkretouch.presentation.edit.crop

import space.taran.arkretouch.presentation.drawing.EditManager
import space.taran.arkretouch.presentation.edit.Operation

class CropOperation(private val editManager: EditManager) : Operation {
    override fun apply() {
        editManager.apply {
            cropWindow.apply {
                val params = getCropParams()
                val width = params.width
                val height = params.height
                var maxWidth = availableDrawAreaSize.value.width
                var maxHeight = availableDrawAreaSize.value.height
                val aspectRatio = width.toFloat() / height.toFloat()
                val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()
                val px = maxWidth / 2
                val py = maxHeight / 2
                if (aspectRatio > maxRatio)
                    maxHeight = (maxWidth / aspectRatio).toInt()
                else maxWidth = (maxHeight * aspectRatio).toInt()
                val sx = maxWidth / width
                val sy = maxHeight / height
                cropMatrix.reset()
                cropMatrix.postScale(
                    sx.toFloat(),
                    sy.toFloat(),
                    px.toFloat(),
                    py.toFloat()
                )
            }
        }
    }
}
