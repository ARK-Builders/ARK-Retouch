@file:OptIn(ExperimentalComposeUiApi::class)

package space.taran.arkretouch.presentation.drawing

import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.GestureDetectorCompat
import space.taran.arkretouch.presentation.edit.EditViewModel

@Composable
fun EditDrawCanvas(viewModel: EditViewModel) {
    val editManager = viewModel.editManager
    var path = Path()
    val context = LocalContext.current
    val tmpPointerList = mutableListOf<PointF>()
    var accumulativeScale = remember { 1f }
    var accumulativeScalePivot = remember { Offset.Zero }
    var accumulativeTranslateOffset = remember { Offset.Zero }

    val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (tmpPointerList.size != 2) return false
                val localScale = detector.scaleFactor
                accumulativeScale *= localScale
                val pivotX = (tmpPointerList[0].x + tmpPointerList[1].x) / 2
                val pivotY = (tmpPointerList[0].y + tmpPointerList[1].y) / 2
                accumulativeScalePivot = Offset(
                    (accumulativeScalePivot.x + pivotX) / 2,
                    (accumulativeScalePivot.y + pivotY) / 2
                )
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
                    if (e2.pointerCount != 3) return false

                    accumulativeTranslateOffset =
                        Offset(
                            accumulativeTranslateOffset.x - distanceX,
                            accumulativeTranslateOffset.y - distanceY
                        )
                    editManager.invalidatorTick.value++
                    return true
                }
            }
        )

    val currentPoint = PointF(0f, 0f)

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
                val eventX = event.x - accumulativeTranslateOffset.x
                val eventY = event.y - accumulativeTranslateOffset.y

                tmpPointerList.clear()
                repeat(event.pointerCount) { index ->
                    val coords = MotionEvent.PointerCoords()
                    event.getPointerCoords(index, coords)
                    tmpPointerList.add(PointF(coords.x, coords.y))
                }
                panGestureDetector.onTouchEvent(event)
                scaleGestureDetector.onTouchEvent(event)

                if (event.pointerCount > 1) {
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
                        editManager.addDrawPath(path)
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

        scale(
            scaleX = accumulativeScale,
            scaleY = accumulativeScale,
            pivot = accumulativeScalePivot
        ) {
            translate(
                accumulativeTranslateOffset.x,
                accumulativeTranslateOffset.y
            ) {
                if (editManager.backgroundImage.value != null) {
                    drawImage(
                        editManager.backgroundImage.value!!,
                        editManager.calcImageOffset()
                    )
                }
                drawIntoCanvas { canvas ->
                    editManager.drawPaths.forEach {
                        canvas.drawPath(it.path, it.paint)
                    }
                }
            }
        }
    }
}
