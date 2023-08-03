package space.taran.arkretouch.presentation.edit.blur

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import com.hoko.blur.processor.HokoBlurBuild
import space.taran.arkretouch.presentation.drawing.EditManager
import space.taran.arkretouch.presentation.edit.Operation
import java.util.Stack

class BlurOperation(private val editManager: EditManager) : Operation {
    private lateinit var blurredBitmap: Bitmap
    private lateinit var brushBitmap: Bitmap
    private lateinit var context: Context
    private val blurs = Stack<ImageBitmap>()
    private val redoBlurs = Stack<ImageBitmap>()
    private var offset = Offset.Zero
    private var bitmapPosition = IntOffset.Zero

    fun init() {
        editManager.apply {
            backgroundImage.value?.let {
                bitmapPosition = IntOffset(
                    (it.width / 2) - (BRUSH_SIZE / 2),
                    (it.height / 2) - (BRUSH_SIZE / 2)
                )
                brushBitmap = Bitmap.createBitmap(
                    it.asAndroidBitmap(),
                    bitmapPosition.x,
                    bitmapPosition.y,
                    BRUSH_SIZE,
                    BRUSH_SIZE
                )
            }
        }
    }

    fun drawBrush(context: Context, canvas: Canvas) {
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

    fun moveBrush(brushPosition: Offset, delta: Offset) {
        val position = Offset(
            brushPosition.x * editManager.bitmapScale.x,
            brushPosition.y * editManager.bitmapScale.y
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
                            BRUSH_SIZE,
                            BRUSH_SIZE
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

    private fun isWithinBounds(image: ImageBitmap) = bitmapPosition.x >= 0 &&
        (bitmapPosition.x + BRUSH_SIZE) <= image.width && bitmapPosition.y >= 0 &&
        (bitmapPosition.y + BRUSH_SIZE) <= image.height

    private fun isBrushTouched(position: Offset): Boolean {
        return position.x >= offset.x && position.x <= (offset.x + BRUSH_SIZE) &&
            position.y >= offset.y && position.y <= (offset.y + BRUSH_SIZE)
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
            blurs.add(it)
            editManager.addBlur()
        }
        editManager.toggleBlurMode()
        editManager.backgroundImage.value = image
    }

    override fun undo() {
        val bitmap = blurs.pop()
        redoBlurs.push(editManager.backgroundImage.value)
        editManager.backgroundImage.value = bitmap
    }

    override fun redo() {
        val bitmap = redoBlurs.pop()
        blurs.push(editManager.backgroundImage.value)
        editManager.backgroundImage.value = bitmap
    }

    private fun blur(context: Context) {
        editManager.apply {
            val blurProcessor = HokoBlurBuild(context)
                .radius(blurIntensity.value.toInt())
                .processor()
            blurredBitmap =
                blurProcessor.blur(brushBitmap)
        }
    }

    companion object {
        private const val BRUSH_SIZE = 250
    }
}
