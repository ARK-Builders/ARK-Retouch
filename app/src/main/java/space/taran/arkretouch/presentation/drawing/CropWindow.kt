package space.taran.arkretouch.presentation.drawing

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import space.taran.arkretouch.presentation.edit.EditViewModel
import space.taran.arkretouch.presentation.picker.pxToDp

@Composable
fun CropWindow(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    val captureArea = editManager.captureArea.value ?: return

    Box(
        modifier = Modifier
            .padding(
                start = captureArea.left.pxToDp(),
                top = captureArea.top.pxToDp()
            )
            .size(
                width = captureArea.width.pxToDp(),
                height = captureArea.height.pxToDp()
            )
            .border(4.dp, Color.LightGray)
    )
}
