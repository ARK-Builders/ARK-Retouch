package dev.arkbuilders.arkretouch.presentation.popups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.arkbuilders.arkretouch.R

@Composable
fun BlurIntensityPopup(
    isBlurring: Boolean,
    intensity: Float,
    size: Float,
    onIntensityChange: (Float) -> Unit,
    onSizeChange: (Float) -> Unit,
) {
    if (isBlurring) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column {
                Text(stringResource(R.string.blur_intensity))
                Slider(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = intensity, // editManager.blurIntensity.value,
                    onValueChange = {
                        onIntensityChange(it)
                        // editManager.blurIntensity.value = it
                    },
                    valueRange = 0f..25f,
                )
            }
            Column {
                Text(stringResource(R.string.blur_size))
                Slider(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = size, // editManager.blurOperation.blurSize.value,
                    onValueChange = {
                        onSizeChange(it)
                        // editManager.blurOperation.blurSize.value = it
                        // editManager.blurOperation.resize()
                    },
                    valueRange = 100f..500f,
                )
            }
        }
    }
}