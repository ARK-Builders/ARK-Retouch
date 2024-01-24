package dev.arkbuilders.arkretouch.editing.model

import androidx.compose.ui.unit.IntSize
import dev.arkbuilders.arkretouch.presentation.edit.resize.ResizeOperation

class ImageViewParams(
    val drawArea: IntSize,
    val scale: ResizeOperation.Scale
)