package space.taran.arkretouch.presentation.edit.resize

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.taran.arkretouch.R
import space.taran.arkretouch.presentation.drawing.EditManager

@Composable
fun ResizeInput(isVisible: Boolean, editManager: EditManager) {
    if (isVisible) {
        var width by remember {
            mutableStateOf(
                editManager.availableDrawAreaSize.value.width.toString()
            )
        }

        var height by remember {
            mutableStateOf(
                editManager.availableDrawAreaSize.value.height.toString()
            )
        }

        val widthHint = stringResource(
            R.string.width_too_large,
            editManager.originalDrawAreaSize.width
        )
        val digitsHint = stringResource(R.string.digits_only)
        val heightHint = stringResource(
            R.string.height_too_large,
            editManager.originalDrawAreaSize.height
        )
        var hint by remember {
            mutableStateOf("")
        }
        var showHint by remember {
            mutableStateOf(false)
        }

        val scope = MainScope()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Hint(
                hint,
                showHint
            )
            if (showHint) {
                scope.launch {
                    delay(1000)
                    showHint = false
                }
            }
            Row {
                TextField(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    value = width,
                    onValueChange = {
                        width = it
                        if (
                            width.isNotEmpty() &&
                            width.isDigitsOnly() &&
                            width.toInt() > editManager.originalDrawAreaSize.width
                        ) {
                            hint = widthHint
                            showHint = true
                            return@TextField
                        }
                        if (width.isNotEmpty() && !width.isDigitsOnly()) {
                            hint = digitsHint
                            showHint = true
                            return@TextField
                        }
                        showHint = false
                        if (width.isEmpty()) height = width
                        if (width.isNotEmpty() && width.isDigitsOnly()) {
                            height = editManager.resizeDown(width = width.toInt())
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
                        if (
                            height.isNotEmpty() &&
                            height.isDigitsOnly() &&
                            height.toInt() > editManager.originalDrawAreaSize.height
                        ) {
                            hint = heightHint
                            showHint = true
                            return@TextField
                        }
                        if (height.isNotEmpty() && !height.isDigitsOnly()) {
                            hint = digitsHint
                            showHint = true
                            return@TextField
                        }
                        showHint = false
                        if (height.isEmpty()) width = height
                        if (height.isNotEmpty() && height.isDigitsOnly()) {
                            width = editManager.resizeDown(height = height.toInt())
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
    }
}

@Composable
fun Hint(text: String, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(tween(durationMillis = 500)),
        modifier = Modifier
            .background(Color.LightGray, RoundedCornerShape(10))
    ) {
        Text(
            text,
            Modifier.padding(12.dp)
        )
    }
}
