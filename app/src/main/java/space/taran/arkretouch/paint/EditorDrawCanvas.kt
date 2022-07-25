package space.taran.arkretouch.paint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import space.taran.arkretouch.R

class EditorDrawCanvas(context: Context, attrs: AttributeSet) :
    View(context, attrs) {
    private var rectNew: Rect? = null
    private var offsetY: Float = 0f
    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f
    private var mColor = 0
    private var mWasMultitouch = false

    private var mPaths = LinkedHashMap<Path, PaintOptions>()
    private var mPaint = Paint()
    private var mPath = Path()
    private var mPaintOptions = PaintOptions()

    private var backgroundBitmap: Bitmap? = null
    private var mCropSelectionPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.BEVEL
        strokeCap = Paint.Cap.SQUARE
        strokeWidth = 5f
        isAntiAlias = true
    }

    init {
        mColor = ContextCompat.getColor(context, R.color.color_primary)
        mPaint.apply {
            color = mColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 40f
            isAntiAlias = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, null)
        }

        for ((key, value) in mPaths) {
            changePaint(value)
            canvas.drawPath(key, mPaint)
        }

        changePaint(mPaintOptions)
        canvas.drawPath(mPath, mPaint)
        rectNew?.let {
            canvas.drawRect(
                it.left.toFloat(), it.top.toFloat(),
                it.right.toFloat(),
                it.bottom.toFloat(), mCropSelectionPaint
            )
            canvas.clipOutRect(
                it.left.toFloat(), it.top.toFloat(),
                it.right.toFloat(),
                it.bottom.toFloat()
            )
            canvas.drawColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.crop_image_view_background,
                    resources.newTheme()
                )
            )
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mWasMultitouch = false
                mStartX = x
                mStartY = y
                actionDown(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !mWasMultitouch) {
                    actionMove(x, y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> actionUp()
            MotionEvent.ACTION_POINTER_DOWN -> mWasMultitouch = true
        }

        invalidate()
        return true
    }

    private fun actionDown(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mCurX = x
        mCurY = y
    }

    private fun actionMove(x: Float, y: Float) {
        mPath.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2)
        mCurX = x
        mCurY = y
    }

    private fun actionUp() {
        if (!mWasMultitouch) {
            mPath.lineTo(mCurX, mCurY)

            // draw a dot on click
            if (mStartX == mCurX && mStartY == mCurY) {
                mPath.lineTo(mCurX, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY + 2)
                mPath.lineTo(mCurX + 1, mCurY)
            }
        }

        mPaths[mPath] = mPaintOptions
        mPath = Path()
        mPaintOptions = PaintOptions(mPaintOptions.color, mPaintOptions.strokeWidth)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        mPaint.color = paintOptions.color
        mPaint.strokeWidth = paintOptions.strokeWidth
    }

    fun updateColor(newColor: Int) {
        mPaintOptions.color = newColor
    }

    fun updateBrushSize(newBrushSize: Int) {
        mPaintOptions.strokeWidth =
            resources.getDimension(R.dimen.full_brush_size) * (newBrushSize / 100f)
    }

    fun updateBackgroundBitmap(bitmap: Bitmap) {
        backgroundBitmap = bitmap
        offsetY = (height - bitmap.height) / 2f
        invalidate()
    }

    fun clear() {
        mPaths.clear()
        backgroundBitmap?.eraseColor(Color.WHITE)
        layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
        layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
        requestLayout()
    }

    fun getBitmap(): Bitmap? {
        if (backgroundBitmap == null) {
            return null
        }
        val rect = this.rectNew
        drawRect()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        drawRect(rect)
        return bitmap
    }

    fun undo() {
        if (mPaths.isEmpty()) {
            return
        }

        val lastKey = mPaths.keys.lastOrNull()
        mPaths.remove(lastKey)
        invalidate()
    }

    fun drawRect(rect: Rect? = null) {
        this.rectNew = rect
        invalidate()
    }

    fun getCropImage(): Bitmap? {
        val bitmap = getBitmap()
        return rectNew?.let { rect ->
            val left = rect.left
            val top = rect.top
            val rectWidth = rect.width()
            val rectHeight = rect.height()
            bitmap?.let {
                Bitmap.createBitmap(
                    it,
                    left,
                    top,
                    rectWidth,
                    rectHeight
                )
            }
        } ?: bitmap
    }

    fun isCanvasChanged(): Boolean {
        return mPaths.isNotEmpty()
    }

    fun getDrawingDetails(): LinkedHashMap<Path, PaintOptions> {
        return mPaths
    }

    fun updateDrawingDetails(hashMap: java.util.LinkedHashMap<*, *>?) {
        if (hashMap != null) {
            this.mPaths = (hashMap as LinkedHashMap<Path, PaintOptions>)
            invalidate()
        }
    }
}
