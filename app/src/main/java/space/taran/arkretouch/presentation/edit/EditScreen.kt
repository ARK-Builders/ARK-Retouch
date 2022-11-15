@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.getstream.sketchbook.ColorPickerDialog
import io.getstream.sketchbook.Sketchbook
import io.getstream.sketchbook.SketchbookController
import space.taran.arkretouch.R
import space.taran.arkretouch.di.DIManager
import space.taran.arkretouch.presentation.askWritePermissions
import space.taran.arkretouch.presentation.isWritePermGranted
import space.taran.arkretouch.presentation.picker.toDp
import space.taran.arkretouch.presentation.picker.toPx
import space.taran.arkretouch.presentation.theme.Gray
import java.nio.file.Path

@Composable
fun EditScreen(
    imagePath: Path?,
    imageUri: String?,
    fragmentManager: FragmentManager,
    navigateBack: () -> Unit
) {
    val viewModel: EditViewModel =
        viewModel(factory = DIManager.component.editVMFactory())
    val primaryColor = MaterialTheme.colors.primary
    val controller = remember {
        SketchbookController().apply {
            setPaintColor(primaryColor)
        }
    }
    val availableSize = remember { mutableStateOf(IntSize.Zero) }
    val sketchbookSize = remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current

    ExitDialog(
        viewModel = viewModel,
        navigateBack = { navigateBack() }
    )

    BackHandler {
        viewModel.showExitDialog = true
    }

    LaunchedEffect(availableSize) {
        if (availableSize.value == IntSize.Zero) return@LaunchedEffect
        imagePath?.let {
            loadImageWithPath(
                context,
                imagePath,
                controller,
                availableSize.value,
                sketchbookSize
            )
            return@LaunchedEffect
        }
        imageUri?.let {
            loadImageWithUri(
                context,
                imageUri,
                controller,
                availableSize.value,
                sketchbookSize
            )
            return@LaunchedEffect
        }
        sketchbookSize.value = availableSize.value
    }

    DrawContainer(
        availableSize,
        sketchbookSize.value,
        controller,
        viewModel
    )
    Menus(imagePath, fragmentManager, viewModel, controller, navigateBack)
}

@Composable
private fun Menus(
    imagePath: Path?,
    fragmentManager: FragmentManager,
    viewModel: EditViewModel,
    controller: SketchbookController,
    navigateBack: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            TopMenu(imagePath, fragmentManager, viewModel, controller, navigateBack)
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            EditMenuContainer(controller, viewModel)
        }
    }
}

@Composable
private fun DrawContainer(
    availableSize: MutableState<IntSize>,
    sketchbookSize: IntSize,
    controller: SketchbookController,
    viewModel: EditViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp)
            .onSizeChanged {
                availableSize.value = it
            },
        contentAlignment = Alignment.Center
    ) {
        Sketchbook(
            modifier = Modifier
                .pointerInteropFilter {
                    viewModel.strokeSliderExpanded = false
                    false
                }
                .size(
                    sketchbookSize.width.toDp(),
                    sketchbookSize.height.toDp()
                ),
            controller = controller,
            backgroundColor = Color.White,
        )
    }
}

@Composable
private fun TopMenu(
    imagePath: Path?,
    fragmentManager: FragmentManager,
    viewModel: EditViewModel,
    controller: SketchbookController,
    navigateBack: () -> Unit
) {
    val context = LocalContext.current

    if (viewModel.showSavePathDialog)
        SavePathDialog(
            initialImagePath = imagePath,
            fragmentManager = fragmentManager,
            onDismissClick = { viewModel.showSavePathDialog = false },
            onPositiveClick = { savePath ->
                viewModel.saveImage(savePath, controller.getSketchbookBitmap())
                viewModel.showSavePathDialog = false
                navigateBack()
            }
        )

    if (!viewModel.menusVisible)
        return

    Row(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            modifier = Modifier
                .padding(8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    if (!context.isWritePermGranted()) {
                        context.askWritePermissions()
                        return@clickable
                    }
                    viewModel.showSavePathDialog = true
                },
            imageVector = ImageVector.vectorResource(R.drawable.ic_save),
            tint = MaterialTheme.colors.primary,
            contentDescription = null
        )
    }
}

@Composable
private fun StrokeWidthPopup(
    modifier: Modifier,
    controller: SketchbookController,
    viewModel: EditViewModel
) {
    controller.setPaintStrokeWidth(viewModel.strokeWidth.dp.toPx())
    controller.setEraseRadius(viewModel.strokeWidth.dp.toPx())
    if (viewModel.strokeSliderExpanded) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        )
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(viewModel.strokeWidth.dp)
                        .clip(RoundedCornerShape(30))
                        .background(controller.currentPaintColor.value)
                )
            }

            Slider(
                modifier = Modifier
                    .fillMaxWidth(),
                value = viewModel.strokeWidth,
                onValueChange = {
                    viewModel.strokeWidth = it
                },
                valueRange = 5f..50f,
            )
        }
    }
}

@Composable
private fun EditMenuContainer(
    controller: SketchbookController,
    viewModel: EditViewModel
) {
    Column(
        Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(topStartPercent = 30, topEndPercent = 30))
                .background(Gray)
                .clickable {
                    viewModel.menusVisible = !viewModel.menusVisible
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (viewModel.menusVisible) Icons.Filled.KeyboardArrowDown
                else Icons.Filled.KeyboardArrowUp,
                contentDescription = "",
                modifier = Modifier.size(32.dp),
            )
        }
        AnimatedVisibility(
            visible = viewModel.menusVisible,
            enter = expandVertically(expandFrom = Alignment.Bottom),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            EditMenuContent(controller, viewModel)
        }
    }
}

@Composable
private fun EditMenuContent(
    controller: SketchbookController,
    viewModel: EditViewModel
) {
    val colorDialogExpanded = remember { mutableStateOf(false) }
    Column(
        Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .background(Gray)
    ) {
        StrokeWidthPopup(Modifier, controller, viewModel)

        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { controller.undo() },
                imageVector = ImageVector.vectorResource(R.drawable.ic_undo),
                tint = if (controller.canUndo.value) {
                    MaterialTheme.colors.primary
                } else {
                    Color.Black
                },
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { controller.redo() },
                imageVector = ImageVector.vectorResource(R.drawable.ic_redo),
                tint = if (controller.canRedo.value) {
                    MaterialTheme.colors.primary
                } else {
                    Color.Black
                },
                contentDescription = null
            )

            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color = controller.currentPaintColor.value)
                    .clickable { colorDialogExpanded.value = true }
            )

            ColorPickerDialog(
                controller = controller,
                expanded = colorDialogExpanded,
                initialColor = controller.currentPaintColor.value,
                onColorSelected = { color ->
                    controller.setPaintColor(color)
                }
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        viewModel.strokeSliderExpanded =
                            !viewModel.strokeSliderExpanded
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_line_weight),
                tint = MaterialTheme.colors.primary,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { controller.clearPaths() },
                imageVector = ImageVector.vectorResource(R.drawable.ic_clear),
                tint = MaterialTheme.colors.primary,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { controller.toggleEraseMode() },
                imageVector = ImageVector.vectorResource(R.drawable.ic_eraser),
                tint = if (controller.isEraseMode.value)
                    MaterialTheme.colors.primary
                else
                    Color.Black,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun ExitDialog(
    viewModel: EditViewModel,
    navigateBack: () -> Unit,
) {
    if (!viewModel.showExitDialog) return

    AlertDialog(
        onDismissRequest = {
            viewModel.showExitDialog = false
        },
        title = {
            Text(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                text = "Do you want to save the changes?",
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.showExitDialog = false
                    viewModel.showSavePathDialog = true
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.showExitDialog = false
                    navigateBack()
                }
            ) {
                Text("Exit")
            }
        }
    )
}

private fun loadImageWithPath(
    context: Context,
    image: Path,
    controller: SketchbookController,
    availableSize: IntSize,
    sketchbookSize: MutableState<IntSize>
) {
    initGlideBuilder(context)
        .load(image.toFile())
        .loadInto(controller, availableSize, sketchbookSize)
}

private fun loadImageWithUri(
    context: Context,
    uri: String,
    controller: SketchbookController,
    availableSize: IntSize,
    sketchbookSize: MutableState<IntSize>
) {
    initGlideBuilder(context)
        .load(uri.toUri())
        .loadInto(controller, availableSize, sketchbookSize)
}

private fun initGlideBuilder(context: Context) = Glide
    .with(context)
    .asBitmap()

private fun RequestBuilder<Bitmap>.loadInto(
    controller: SketchbookController,
    availableSize: IntSize,
    sketchbookSize: MutableState<IntSize>
) {
    into(object : CustomTarget<Bitmap>() {
        override fun onResourceReady(
            bitmap: Bitmap,
            transition: Transition<in Bitmap>?
        ) {
            val newBitmap =
                resize(bitmap, availableSize.width, availableSize.height)
            controller.setImageBitmap(newBitmap.asImageBitmap())
            sketchbookSize.value = IntSize(newBitmap.width, newBitmap.height)
        }

        override fun onLoadCleared(placeholder: Drawable?) {}
    })
}

private fun resize(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val bitmapRatio = width.toFloat() / height.toFloat()
    val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()

    var finalWidth = maxWidth
    var finalHeight = maxHeight

    if (maxRatio > bitmapRatio) {
        finalWidth = (maxHeight.toFloat() * bitmapRatio).toInt()
    } else {
        finalHeight = (maxWidth.toFloat() / bitmapRatio).toInt()
    }

    return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
}
