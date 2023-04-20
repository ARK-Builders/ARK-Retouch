package space.taran.arkretouch.presentation.edit.resize

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.core.text.isDigitsOnly
import space.taran.arkretouch.R
import space.taran.arkretouch.presentation.edit.EditViewModel

@Composable
fun ResizeInput(viewModel: EditViewModel) {

    var width by remember {
        mutableStateOf(
            viewModel.editManager.availableDrawAreaSize.value.width.toString()
        )
    }

    var height by remember {
        mutableStateOf(
            viewModel.editManager.availableDrawAreaSize.value.height.toString()
        )
    }

    Row {
        TextField(
            modifier = Modifier.fillMaxWidth(0.5f),
            value = width,
            onValueChange = {
                width = it
                if (width.isEmpty()) height = width
                if (it.isNotEmpty() && it.isDigitsOnly()) {
                    height = viewModel.resizeDown(width = it.toInt())
                        .height.toString()
                }
            },
            label = {
                Text(
                    stringResource(R.string.width),
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            },
            textStyle = TextStyle(
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = height,
            onValueChange = {
                height = it
                if (height.isEmpty()) width = height
                if (it.isNotEmpty() && it.isDigitsOnly()) {
                    width = viewModel.resizeDown(height = it.toInt())
                        .width.toString()
                }
            },
            label = {
                Text(
                    stringResource(R.string.height),
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            },
            textStyle = TextStyle(
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
    }
}
