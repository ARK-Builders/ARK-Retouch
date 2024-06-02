package dev.arkbuilders.arkretouch.editing.blur

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import android.content.Context
import android.graphics.Bitmap
import com.hoko.blur.processor.HokoBlurBuild
import dev.arkbuilders.arkretouch.editing.Operation
import dev.arkbuilders.arkretouch.editing.manager.EditManager
import java.util.Stack

class BlurOperation(
    private val editManager: EditManager,
    private val onApply: () -> Unit
) : Operation {

    private lateinit var blurredBitmap: Bitmap
    private lateinit var brushBitmap: Bitmap
    private lateinit var context: Context
    private val blurs = Stack<ImageBitmap>()
    private val redoBlurs = Stack<ImageBitmap>()
    private var offset = Offset.Zero
    private var bitmapPosition = IntOffset.Zero

    private var blurSize = BRUSH_SIZE.toFloat()
    private var intensity = 12

    fun init() {
        editManager.apply {
            backgroundImage.value?.let {
                bitmapPosition = IntOffset(
                    (it.width / 2) - (blurSize.toInt() / 2),
                    (it.height / 2) - (blurSize.toInt() / 2)
                )
                brushBitmap = Bitmap.createBitmap(
                    it.asAndroidBitmap(),
                    bitmapPosition.x,
                    bitmapPosition.y,
                    blurSize.toInt(),
                    blurSize.toInt()
                )
            }
            scaleToFitOnEdit()
        }
    }

    fun resize() {
        editManager.backgroundImage.value?.let {
            if (isWithinBounds(it)) {
                brushBitmap = Bitmap.createBitmap(
                    it.asAndroidBitmap(),
                    bitmapPosition.x,
                    bitmapPosition.y,
                    blurSize.toInt(),
                    blurSize.toInt()
                )
            }
        }
    }

    fun draw(context: Context, canvas: Canvas) {
        if (blurSize in MIN_SIZE..MAX_SIZE) {
            editManager.backgroundImage.value?.let {
                this.context = context
                if (isWithinBounds(it)) {
                    offset = Offset(
                        bitmapPosition.x.toFloat(),
                        bitmapPosition.y.toFloat()
                    )
                }
                blur(context)
                canvas.drawImage(
                    blurredBitmap.asImageBitmap(),
                    offset,
                    Paint()
                )
            }
        }
    }

    fun move(blurPosition: Offset, delta: Offset) {
        val position = Offset(
            blurPosition.x * editManager.bitmapScale.x,
            blurPosition.y * editManager.bitmapScale.y
        )
        if (isBrushTouched(position)) {
            editManager.apply {
                bitmapPosition = IntOffset(
                    (offset.x + delta.x).toInt(),
                    (offset.y + delta.y).toInt()
                )
                backgroundImage.value?.let {
                    if (isWithinBounds(it)) {
                        brushBitmap = Bitmap.createBitmap(
                            it.asAndroidBitmap(),
                            bitmapPosition.x,
                            bitmapPosition.y,
                            blurSize.toInt(),
                            blurSize.toInt()
                        )
                    }
                }
            }
        }
    }

    fun clear() {
        blurs.clear()
        redoBlurs.clear()
        editManager.updateRevised()
    }

    fun cancel() {
        editManager.redrawBackgroundImage2()
    }

    fun setSize(size: Float) {
        blurSize = size
    }

    fun setIntensity(intensity: Float) {
        this.intensity = intensity.toInt()
    }

    private fun isWithinBounds(image: ImageBitmap) = bitmapPosition.x >= 0 &&
        (bitmapPosition.x + blurSize) <= image.width &&
        bitmapPosition.y >= 0 && (bitmapPosition.y + blurSize) <= image.height

    private fun isBrushTouched(position: Offset): Boolean {
        return position.x >= offset.x && position.x <= (offset.x + blurSize) &&
            position.y >= offset.y && position.y <= (offset.y + blurSize)
    }

    override fun apply() {
        val image = ImageBitmap(
            editManager.imageSize.width,
            editManager.imageSize.height,
            ImageBitmapConfig.Argb8888
        )
        editManager.backgroundImage.value?.let {
            val canvas = Canvas(image)
            canvas.drawImage(
                it,
                Offset.Zero,
                Paint()
            )
            canvas.drawImage(
                blurredBitmap.asImageBitmap(),
                offset,
                Paint()
            )
            blurs.add(editManager.backgroundImage2.value)
            editManager.addBlur()
        }
        editManager.keepEditedPaths()
        editManager.backgroundImage.value = image
        onApply()
    }

    override fun undo() {
        val bitmap = blurs.pop()
        redoBlurs.push(editManager.backgroundImage.value)
        editManager.backgroundImage.value = bitmap
        editManager.redrawEditedPaths()
    }

    override fun redo() {
        val bitmap = redoBlurs.pop()
        blurs.push(editManager.backgroundImage.value)
        editManager.backgroundImage.value = bitmap
        editManager.keepEditedPaths()
    }

    private fun blur(context: Context) {
        editManager.apply {
            val blurProcessor = HokoBlurBuild(context)
                .radius(intensity)
                .processor()
            blurredBitmap =
                blurProcessor.blur(brushBitmap)
        }
    }

    companion object {
        private const val BRUSH_SIZE = 250
        const val MAX_SIZE = 500f
        const val MIN_SIZE = 100f
    }
}