package space.taran.arkretouch.presentation.edit.rotate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.taran.arkretouch.R
import space.taran.arkretouch.presentation.edit.EditViewModel

@Composable
fun RotateOptions(viewModel: EditViewModel) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (viewModel.editManager.isRotateMode.value) {
            SwitchLayoutDialog(viewModel.editManager.showSwitchLayoutDialog) {
                viewModel.editManager.switchLayout()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = viewModel.editManager.smartLayout.value,
                    onCheckedChange = {
                        viewModel.editManager.toggleSmartLayout()
                    }
                )
                Text(stringResource(R.string.smart_layout_switch))
            }
            Row {
                Icon(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            viewModel.editManager.apply {
                                rotate(-90f)
                                invalidatorTick.value++
                            }
                        },
                    imageVector = ImageVector
                        .vectorResource(R.drawable.ic_rotate_left),
                    tint = MaterialTheme.colors.primary,
                    contentDescription = null
                )
                Icon(
                    modifier = Modifier
                        .padding(12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            viewModel.editManager.apply {
                                rotate(90f)
                                invalidatorTick.value++
                            }
                        },
                    imageVector = ImageVector
                        .vectorResource(R.drawable.ic_rotate_right),
                    tint = MaterialTheme.colors.primary,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun SwitchLayoutDialog(
    show: MutableState<Boolean>,
    onConfirm: () -> Unit
) {
    if (!show.value) return

    AlertDialog(
        onDismissRequest = {
            show.value = false
        },
        title = {
            Text(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                text = "Do you want to switch layout?",
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    show.value = false
                    onConfirm()
                }
            ) {
                Text("Switch")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    show.value = false
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
