package space.taran.arkretouch.presentation.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentManager
import java.nio.file.Path
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.taran.arkfilepicker.ArkFilePickerConfig
import space.taran.arkfilepicker.ArkFilePickerFragment
import space.taran.arkfilepicker.ArkFilePickerMode
import space.taran.arkfilepicker.onArkPathPicked
import space.taran.arkretouch.R
import space.taran.arkretouch.presentation.findNotExistCopyName
import kotlin.io.path.name

@Composable
fun SavePathDialog(
    initialImagePath: Path?,
    fragmentManager: FragmentManager,
    onDismissClick: () -> Unit,
    onPositiveClick: (Path) -> Unit
) {
    var currentPath by remember { mutableStateOf(initialImagePath?.parent) }
    var overwriteOriginalPath by remember { mutableStateOf(false) }
    var name by remember {
        mutableStateOf(
            initialImagePath?.let {
                it.parent.findNotExistCopyName(it.fileName).name
            } ?: "image.png"
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(overwriteOriginalPath) {
        if (overwriteOriginalPath) {
            currentPath = initialImagePath!!.parent
            name = initialImagePath.name
        }
    }

    Dialog(onDismissRequest = onDismissClick) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.White, RoundedCornerShape(5))
                .padding(5.dp)
        ) {
            Text(
                modifier = Modifier.padding(5.dp),
                text = stringResource(R.string.location),
                fontSize = 18.sp
            )
            TextButton(
                onClick = {
                    ArkFilePickerFragment
                        .newInstance(folderFilePickerConfig(currentPath))
                        .show(fragmentManager, null)
                    fragmentManager.onArkPathPicked(lifecycleOwner) {
                        currentPath = it
                    }
                }
            ) {
                Text(
                    text = currentPath?.toString()
                        ?: stringResource(R.string.pick_folder)
                )
            }
            OutlinedTextField(
                modifier = Modifier.padding(5.dp),
                value = name.toString(),
                onValueChange = {
                    name = it
                },
                label = { Text(text = stringResource(R.string.name)) },
                singleLine = true
            )

            initialImagePath?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5))
                        .clickable {
                            overwriteOriginalPath = !overwriteOriginalPath
                        }
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = overwriteOriginalPath,
                        onCheckedChange = {
                            overwriteOriginalPath = !overwriteOriginalPath
                        }
                    )
                    Text(text = stringResource(R.string.overwrite_original_file))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    modifier = Modifier.padding(5.dp),
                    onClick = onDismissClick
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
                Button(
                    modifier = Modifier.padding(5.dp),
                    onClick = {
                        if (currentPath != null && name != null)
                            onPositiveClick(currentPath!!.resolve(name))
                    }
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            }
        }
    }
}

fun folderFilePickerConfig(initialPath: Path?) = ArkFilePickerConfig(
    mode = ArkFilePickerMode.FOLDER,
    initialPath = initialPath
)
