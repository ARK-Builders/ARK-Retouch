@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
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
import space.taran.arkretouch.presentation.picker.toDp
import space.taran.arkretouch.presentation.picker.toPx
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
    val controller = remember {
        SketchbookController().apply {
            setPaintColor(Color.Black)
        }
    }
    var availableSize by remember { mutableStateOf(IntSize.Zero) }
    val sketchbookSize = remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current

    LaunchedEffect(availableSize) {
        if (availableSize == IntSize.Zero) return@LaunchedEffect
        imagePath?.let {
            loadImageWithPath(
                context,
                imagePath,
                controller,
                availableSize,
                sketchbookSize
            )
            return@LaunchedEffect
        }
        imageUri?.let {
            loadImageWithUri(context, imageUri, controller, availableSize, sketchbookSize)
            return@LaunchedEffect
        }
        sketchbookSize.value = availableSize
    }

    Column {
        TopMenu(imagePath, fragmentManager, viewModel, controller, navigateBack)
        Box(
            Modifier
                .fillMaxSize()
                .weight(1f)
                .onSizeChanged {
                    availableSize = it
                }
        ) {
            Sketchbook(
                modifier = Modifier
                    .pointerInteropFilter {
                        viewModel.strokeSliderExpanded.value = false
                        false
                    }
                    .size(
                        sketchbookSize.value.width.toDp(),
                        sketchbookSize.value.height.toDp()
                    )
                    .align(Alignment.Center),
                controller = controller,
                backgroundColor = Color.White,
            )
            StrokeWidthPopup(
                Modifier.align(Alignment.BottomCenter),
                controller,
                viewModel
            )
        }

        EditMenu(controller, viewModel)
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
    var showSavePathDialog by remember { mutableStateOf(false) }

    if (showSavePathDialog)
        SavePathDialog(
            initialImagePath = imagePath,
            fragmentManager = fragmentManager,
            onDismissClick = { showSavePathDialog = false },
            onPositiveClick = { savePath ->
                viewModel.saveImage(savePath, controller.getSketchbookBitmap())
                showSavePathDialog = false
                navigateBack()
            }
        )

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
                    showSavePathDialog = true
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
    val strokeSliderExpanded by viewModel.strokeSliderExpanded.collectAsState()
    var strokeWidth by remember { mutableStateOf(5f) }
    controller.setPaintStrokeWidth(strokeWidth.dp.toPx())
    controller.setEraseRadius(strokeWidth.dp.toPx())
    if (strokeSliderExpanded) {
        Column(
            modifier = modifier
                .background(color = Color.White)
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
                        .height(strokeWidth.dp)
                        .clip(RoundedCornerShape(30))
                        .background(controller.currentPaintColor.value)
                )
            }

            Slider(
                modifier = Modifier
                    .fillMaxWidth(),
                value = strokeWidth,
                onValueChange = {
                    strokeWidth = it
                },
                valueRange = 5f..50f,
            )
        }
    }
}

@Composable
private fun EditMenu(controller: SketchbookController, viewModel: EditViewModel) {
    val colorDialogExpanded = remember { mutableStateOf(false) }

    Column(
        Modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
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

            Box(modifier = Modifier
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
                        viewModel.strokeSliderExpanded.value =
                            !viewModel.strokeSliderExpanded.value
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
