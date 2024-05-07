package dev.arkbuilders.arkretouch.data.model

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import dev.arkbuilders.arkretouch.utils.defaultPaint

data class DrawingState(
    val currentPaint: Paint = defaultPaint(),
    val drawPaint: Paint = defaultPaint(),
    val backgroundPaint: Paint = Paint().also { it.color = Color.Transparent },
    val erasePaint: Paint = Paint().apply {
        shader = null
        color = backgroundPaint.color
        style = PaintingStyle.Stroke
        blendMode = BlendMode.SrcOut
    }
)