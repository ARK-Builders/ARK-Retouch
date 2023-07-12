package space.taran.arkretouch.presentation.edit

import android.graphics.Bitmap.CompressFormat
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
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
import space.taran.arkfilepicker.presentation.filepicker.ArkFilePickerFragment
import space.taran.arkfilepicker.presentation.filepicker.ArkFilePickerMode
import space.taran.arkfilepicker.presentation.onArkPathPicked
import space.taran.arkretouch.R
import space.taran.arkretouch.presentation.utils.findNotExistCopyName
import kotlin.io.path.name
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.key
import java.nio.file.Files
import java.util.Locale
import kotlin.streams.toList

@Composable
fun SavePathDialog(
    initialImagePath: Path?,
    fragmentManager: FragmentManager,
    onDismissClick: () -> Unit,
    onPositiveClick: (Path) -> Unit,
    onCompressFormatChanged: (CompressFormat) -> Unit
) {
    var currentPath by remember { mutableStateOf(initialImagePath?.parent) }
    var imagePath by remember { mutableStateOf(initialImagePath) }
    val showOverwriteCheckbox = remember { mutableStateOf(initialImagePath != null) }
    var overwriteOriginalPath by remember { mutableStateOf(false) }
    var name by remember {
        mutableStateOf(
            initialImagePath?.let {
                it.parent.findNotExistCopyName(it.fileName).name
            } ?: "image.png"
        )
    }
    var compressionFormat by remember { mutableStateOf("PNG") }
    var showCompressionFormats by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(overwriteOriginalPath) {
        if (overwriteOriginalPath) {
            imagePath?.let {
                currentPath = it.parent
                name = it.name
            }
            return@LaunchedEffect
        }
        imagePath?.let {
            name = it.parent.findNotExistCopyName(it.fileName).name
        }
    }

    key(showOverwriteCheckbox.value) {
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
                            .newInstance(
                                folderFilePickerConfig(currentPath)
                            )
                            .show(fragmentManager, null)
                        fragmentManager.onArkPathPicked(lifecycleOwner) { path ->
                            currentPath = path
                            currentPath?.let {
                                imagePath = it.resolve(name)
                                showOverwriteCheckbox.value = Files.list(it).toList()
                                    .contains(imagePath)
                                if (showOverwriteCheckbox.value) {
                                    name = it.findNotExistCopyName(
                                        imagePath?.fileName!!
                                    ).name
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = currentPath?.toString()
                            ?: stringResource(R.string.pick_folder)
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        modifier = Modifier.padding(5.dp),
                        value = name.substringBeforeLast('.'),
                        onValueChange = {
                            name = "$it." +
                                compressionFormat.lowercase(Locale.getDefault())
                            currentPath?.let { path ->
                                imagePath = path.resolve(name)
                                showOverwriteCheckbox.value =
                                    Files.list(path).toList()
                                        .contains(imagePath)
                                if (showOverwriteCheckbox.value) {
                                    name = path.findNotExistCopyName(
                                        imagePath?.fileName!!
                                    ).name
                                }
                            }
                        },
                        label = { Text(text = stringResource(R.string.name)) },
                        singleLine = true
                    )
                    Column(
                        Modifier.clickable {
                            showCompressionFormats = !showCompressionFormats
                        }
                    ) {
                        Text(compressionFormat)
                        Icon(
                            if (showCompressionFormats) Icons.Filled.KeyboardArrowUp
                            else Icons.Filled.KeyboardArrowDown,
                            null,
                        )
                        if (showCompressionFormats) {
                            CompressionFormats { name, format ->
                                compressionFormat = name
                                onCompressFormatChanged(format)
                                showCompressionFormats = false
                            }
                        }
                    }
                }
                if (showOverwriteCheckbox.value) {
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
                            if (currentPath != null)
                                onPositiveClick(currentPath!!.resolve(name))
                        }
                    ) {
                        Text(text = stringResource(R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
fun SaveProgress() {
    Dialog(onDismissRequest = {}) {
        Box(
            Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun CompressionFormats(onFormatClick: (String, CompressFormat) -> Unit) {
    Column(
        Modifier.wrapContentSize()
    ) {
        val png = stringResource(R.string.png)
        val jpeg = stringResource(R.string.jpeg)
        val webpLossless = stringResource(R.string.webp_lossless)
        val webpLossy = stringResource(R.string.webp_lossy)
        val webp = stringResource(R.string.webp)
        Text(
            png,
            Modifier.clickable {
                onFormatClick(png, CompressFormat.PNG)
            }
        )
        Text(
            jpeg,
            Modifier.clickable {
                onFormatClick(jpeg, CompressFormat.JPEG)
            }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Text(
                webpLossless,
                Modifier.clickable {
                    onFormatClick(webpLossless, CompressFormat.WEBP_LOSSLESS)
                }
            )
            Text(
                webpLossy,
                Modifier.clickable {
                    onFormatClick(webpLossy, CompressFormat.WEBP_LOSSY)
                }
            )
        } else {
            Text(
                webp,
                Modifier.clickable {
                    onFormatClick(webp, CompressFormat.WEBP)
                }
            )
        }
    }
}

fun folderFilePickerConfig(initialPath: Path?) = ArkFilePickerConfig(
    mode = ArkFilePickerMode.FOLDER,
    initialPath = initialPath,
    showRoots = true,
    rootsFirstPage = true
)

object ImageExtensions {
    const val PNG = "png"
    const val JPEG = "jpeg"
    const val WEBP = "webp"
}
