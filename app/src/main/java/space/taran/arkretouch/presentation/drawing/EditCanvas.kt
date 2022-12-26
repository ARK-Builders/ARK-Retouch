@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.drawing

import android.graphics.Matrix
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.GestureDetectorCompat
import space.taran.arkretouch.presentation.edit.EditViewModel
import timber.log.Timber

@Composable
fun EditDrawCanvas(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    val context = LocalContext.current
    val drawBitmapPaint = Paint()
    val outlineCanvasPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.FILL
        color = Color.LightGray.toArgb()
    }
    val drawAreaBackgroundPaint = Paint().apply {
        style = PaintingStyle.Fill
        color = Color.White
    }
    var path = Path()
    var drawPath: DrawPath? = null
    val currentPoint = PointF(0f, 0f)
    val tmpPointerList = mutableListOf<PointF>()
    val transformationMatrix = Matrix()

    val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val localScale = detector.scaleFactor
                val pivotX = tmpPointerList.map { it.x }.sum() / tmpPointerList.size
                val pivotY = tmpPointerList.map { it.y }.sum() / tmpPointerList.size
                transformationMatrix
                    .postScale(localScale, localScale, pivotX, pivotY)
                editManager.invalidatorTick.value++
                return true
            }

            override fun onScaleBegin(p0: ScaleGestureDetector) = true

            override fun onScaleEnd(p0: ScaleGestureDetector) {}
        }
    )

    val panGestureDetector =
        GestureDetectorCompat(
            context,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onScroll(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (e2.pointerCount < 2) return false
                    transformationMatrix.postTranslate(-distanceX, -distanceY)
                    editManager.invalidatorTick.value++
                    return true
                }
            }
        )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            // Eraser leaves black line instead of erasing without this hack, it uses BlendMode.SrcOut
            // https://stackoverflow.com/questions/65653560/jetpack-compose-applying-porterduffmode-to-image
            // Provide a slight opacity to for compositing into an
            // offscreen buffer to ensure blend modes are applied to empty pixel information
            // By default any alpha != 1.0f will use a compositing layer by default
            .graphicsLayer(alpha = 0.99f)
            .pointerInteropFilter { event ->
                // https://stackoverflow.com/questions/14042673/android-image-transformation-with-matrix-translate-touch-coordinates-back
                val tmpMatrix = Matrix()
                transformationMatrix.invert(tmpMatrix)
                val mappedXY = floatArrayOf(event.x, event.y)
                tmpMatrix.mapPoints(mappedXY)
                val eventX = mappedXY[0]
                val eventY = mappedXY[1]
                Timber.d("mapped x = $eventX")
                Timber.d("mapped y = $eventY")

                tmpPointerList.clear()
                repeat(event.pointerCount) { index ->
                    val coords = MotionEvent.PointerCoords()
                    event.getPointerCoords(index, coords)
                    tmpPointerList.add(PointF(coords.x, coords.y))
                }
                panGestureDetector.onTouchEvent(event)
                scaleGestureDetector.onTouchEvent(event)

                if (event.pointerCount > 1) {
                    // To avoid redundant dots when gesturing
                    drawPath?.let {
                        editManager.drawPaths.remove(it)
                        drawPath = null
                    }

                    editManager.clearRedoPath()
                    editManager.updateRevised()
                    path = Path()
                    return@pointerInteropFilter true
                }

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        path.reset()
                        path.moveTo(eventX, eventY)
                        currentPoint.x = eventX
                        currentPoint.y = eventY
                        drawPath = editManager.addDrawPath(path)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        path.quadraticBezierTo(
                            currentPoint.x,
                            currentPoint.y,
                            (eventX + currentPoint.x) / 2,
                            (eventY + currentPoint.y) / 2
                        )
                        currentPoint.x = eventX
                        currentPoint.y = eventY
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        // draw a dot
                        if (eventX == currentPoint.x &&
                            eventY == currentPoint.y
                        ) {
                            path.lineTo(currentPoint.x, currentPoint.y)
                        }

                        editManager.clearRedoPath()
                        editManager.updateRevised()
                        path = Path()
                    }
                    else -> false
                }
                editManager.invalidatorTick.value++
                true
            }
    ) {
        // force recomposition on invalidatorTick change
        editManager.invalidatorTick.value

        val drawAreaSize = editManager.drawAreaSize.value
        val drawAreaRect = Rect(
            0f,
            0f,
            drawAreaSize.width.toFloat(),
            drawAreaSize.height.toFloat()
        )
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.setMatrix(transformationMatrix)
            canvas.nativeCanvas.drawPaint(outlineCanvasPaint)
            canvas.drawRect(drawAreaRect, drawAreaBackgroundPaint)
            if (editManager.backgroundImage.value != null) {
                canvas.drawImage(
                    editManager.backgroundImage.value!!,
                    editManager.calcImageOffset(),
                    drawBitmapPaint
                )
            }
            editManager.drawPaths.forEach {
                canvas.drawPath(it.path, it.paint)
            }
        }
    }
}
