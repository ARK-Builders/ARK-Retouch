package space.taran.arkretouch.presentation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.FragmentManager
import space.taran.arkfilepicker.ArkFilePickerConfig
import space.taran.arkfilepicker.ArkFilePickerFragment
import space.taran.arkfilepicker.ArkFilePickerMode
import space.taran.arkfilepicker.onArkPathPicked
import space.taran.arkretouch.R
import space.taran.arkretouch.presentation.edit.ColorPickerDialog
import space.taran.arkretouch.presentation.edit.resize.Hint
import space.taran.arkretouch.presentation.edit.resize.delayHidingHint
import space.taran.arkretouch.presentation.theme.Gray
import space.taran.arkretouch.presentation.utils.askWritePermissions
import space.taran.arkretouch.presentation.utils.isWritePermGranted
import space.taran.arkretouch.presentation.theme.Purple500
import space.taran.arkretouch.presentation.theme.Purple700
import java.nio.file.Path

@Composable
fun PickerScreen(
    fragmentManager: FragmentManager,
    onNavigateToEdit: (Path?, Color, IntSize) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val showColorDialog = remember { mutableStateOf(false) }
    var backgroundColor by remember { mutableStateOf(Color.White) }
    var defaultResolution by remember { mutableStateOf(IntSize.Zero) }
    var resolution by remember { mutableStateOf(IntSize.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPlaced {
                defaultResolution = it.size
            }
    ) {
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .padding(20.dp)
                .onSizeChanged {
                    size = it
                }
                .clip(RoundedCornerShape(10))
                .background(Purple500)
                .clickable {
                    if (!context.isWritePermGranted()) {
                        context.askWritePermissions()
                        return@clickable
                    }

                    ArkFilePickerFragment
                        .newInstance(imageFilePickerConfig())
                        .show(fragmentManager, null)
                    fragmentManager.onArkPathPicked(lifecycleOwner) {
                        onNavigateToEdit(it, Color.White, IntSize.Zero)
                    }
                }
                .border(2.dp, Purple700, shape = RoundedCornerShape(10)),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.open),
                fontSize = 24.sp,
                color = Color.White
            )
            Icon(
                modifier = Modifier.size(size.height.toDp() / 2),
                imageVector = ImageVector.vectorResource(R.drawable.ic_insert_photo),
                tint = Color.White,
                contentDescription = null
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.or),
            fontSize = 24.sp
        )
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(10))
                    .background(Purple500)
                    .fillMaxWidth()
                    .clickable {
                        onNavigateToEdit(
                            null,
                            backgroundColor,
                            if (resolution == IntSize.Zero)
                                defaultResolution
                            else resolution
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.new_),
                    fontSize = 24.sp,
                    color = Color.White
                )
                Icon(
                    modifier = Modifier
                        .size(size.height.toDp() / 2),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                    tint = Color.White,
                    contentDescription = null
                )
            }

            if (defaultResolution != IntSize.Zero) {
                ColorPickerDialog(
                    isVisible = showColorDialog,
                    initialColor = backgroundColor,
                    onColorChanged = {
                        backgroundColor = it
                    }
                )
                NewImageOptions(
                    defaultResolution,
                    backgroundColor,
                    onResolutionChanged = { width, height ->
                        resolution = IntSize(width, height)
                    },
                    showColorDialog = {
                        showColorDialog.value = true
                    }
                )
            }
        }
    }
}

@Composable
fun NewImageOptions(
    _defaultResolution: IntSize,
    backgroundColor: Color,
    onResolutionChanged: (Int, Int) -> Unit,
    showColorDialog: () -> Unit
) {
    val defaultResolution by remember {
        mutableStateOf(_defaultResolution)
    }
    var width by remember {
        mutableStateOf(defaultResolution.width.toString())
    }
    var height by remember {
        mutableStateOf(defaultResolution.height.toString())
    }
    var showHint by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf("") }
    val heightHint = stringResource(
        R.string.height_too_large,
        defaultResolution.height
    )
    val widthHint = stringResource(
        R.string.width_too_large,
        defaultResolution.width
    )
    val digitsOnlyHint = stringResource(
        R.string.digits_only
    )

    Hint(
        hint
    ) {
        delayHidingHint(it) {
            showHint = false
        }
        showHint
    }

    Row {
        TextField(
            modifier = Modifier
                .padding(end = 6.dp)
                .fillMaxWidth(0.5f),
            value = width,
            onValueChange = {
                if (!it.isDigitsOnly()) {
                    hint = digitsOnlyHint
                    showHint = true
                    return@TextField
                }
                if (
                    it.isNotEmpty() && it.isDigitsOnly() &&
                    it.toInt() > defaultResolution.width
                ) {
                    hint = widthHint
                    showHint = true
                    return@TextField
                }
                width = it
                if (it.isNotEmpty() && it.isDigitsOnly()) {
                    onResolutionChanged(width.toInt(), height.toInt())
                }
            },
            label = {
                Text(
                    stringResource(R.string.width),
                    Modifier.fillMaxWidth(),
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
            modifier = Modifier
                .padding(start = 6.dp)
                .fillMaxWidth(),
            value = height,
            onValueChange = {
                if (!it.isDigitsOnly()) {
                    hint = digitsOnlyHint
                    showHint = true
                    return@TextField
                }
                if (
                    it.isNotEmpty() && it.isDigitsOnly() &&
                    it.toInt() > defaultResolution.height
                ) {
                    hint = heightHint
                    showHint = true
                    return@TextField
                }
                height = it
                if (it.isNotEmpty() && it.isDigitsOnly()) {
                    onResolutionChanged(width.toInt(), height.toInt())
                }
            },
            label = {
                Text(
                    stringResource(R.string.height),
                    Modifier.fillMaxWidth(),
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
    Row(
        Modifier
            .clickable {
                showColorDialog()
            }
    ) {
        Text(
            stringResource(R.string.background),
            Modifier.padding(5.dp, end = 12.dp)
        )
        Box(
            Modifier
                .size(28.dp)
                .padding(2.dp)
                .clip(CircleShape)
                .border(2.dp, Gray, CircleShape)
                .background(backgroundColor)
        )
    }
}

fun imageFilePickerConfig(initPath: Path? = null) = ArkFilePickerConfig(
    mode = ArkFilePickerMode.FILE,
    initialPath = initPath
)

@Composable
fun Int.toDp() = with(LocalDensity.current) {
    this@toDp.toDp()
}

@Composable
fun Dp.toPx() = with(LocalDensity.current) {
    this@toPx.toPx()
}
