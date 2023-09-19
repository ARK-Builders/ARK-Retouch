package space.taran.arkretouch.presentation.edit.rotate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import space.taran.arkretouch.R
import space.taran.arkretouch.presentation.edit.EditViewModel

@Composable
fun RotateOptions(viewModel: EditViewModel) {
    var enableManualSwitch by remember {
        mutableStateOf(!viewModel.editManager.smartLayout.value)
    }

    Box(
        Modifier.fillMaxWidth().wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp),
            enabled = enableManualSwitch,
            onClick = {
                viewModel.editManager.switchLayout()
            }
        ) {
            Text("Switch")
        }
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = viewModel.editManager.smartLayout.value,
                    onCheckedChange = {
                        viewModel.editManager.toggleSmartLayout()
                        enableManualSwitch = !it
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
                            viewModel.rotate(-90f)
                            viewModel.editManager.apply {
                                if (smartLayout.value) switchLayout()
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
                            viewModel.rotate(90f)
                            viewModel.editManager.apply {
                                if (smartLayout.value) switchLayout()
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
