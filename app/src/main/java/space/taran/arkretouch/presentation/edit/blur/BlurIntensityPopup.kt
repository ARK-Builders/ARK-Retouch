package space.taran.arkretouch.presentation.edit.blur

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.taran.arkretouch.presentation.drawing.EditManager

@Composable
fun BlurIntensityPopup(
    editManager: EditManager
) {
    if (editManager.isBlurMode.value) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                modifier = Modifier
                    .fillMaxWidth(),
                value = editManager.blurIntensity.value,
                onValueChange = {
                    editManager.blurIntensity.value = it
                },
                valueRange = 0f..25f,
            )
        }
    }
}
