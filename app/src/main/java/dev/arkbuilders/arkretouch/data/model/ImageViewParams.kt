package dev.arkbuilders.arkretouch.data.model

import androidx.compose.ui.unit.IntSize
import dev.arkbuilders.arkretouch.editing.resize.ResizeOperation

class ImageViewParams(
    val drawArea: IntSize,
    val scale: ResizeOperation.Scale
)