package com.y60.zoomableimageview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import java.util.*

/**
 * Created by yusuke on 10/10/2017.
 */

class ZoomableImageView : android.support.v7.widget.AppCompatImageView, ScaleGestureDetector.OnScaleGestureListener {
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var defaultValues: FloatArray? = null

    private var listener: ZoomListener? = null
    private var zooming = false;

    private var defaultScale: Float = 0f
    private var isLandscape = true //上下が余るならtrue
    private var defaultMinLength: Float = 0f //余ってる画像の辺の初期の長さ

    private var singleStartX = Float.NaN
    private var singleStartY = Float.NaN
    private var singleStartTX = Float.NaN
    private var singleStartTY = Float.NaN
    private var completelyScrolledRight = true
    private var completelyScrolledLeft = true
    private val velocityTracker = VelocityTracker.obtain()//1点タッチのスクロールの速度を計算
    private var isBeingTouched = false

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var startScale: Float = 0f//画像の初期位置
    private var startFocalX: Float = 0f
    private var startFocalY: Float = 0f
    private var startSpan: Float = 0f//ジェスチャーの開始位置
    private var defaultK: Float = 0f
    private var values = FloatArray(9)

    private var mImageMatrix: Matrix? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        scaleGestureDetector = ScaleGestureDetector(context, this)
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                reloadMatrixValues()
                velocityTracker.clear()
                if (values[Matrix.MSCALE_X] == defaultScale) {
                    values = Arrays.copyOf(defaultValues!!, 9)
                    values[Matrix.MSCALE_X] *= 2f
                    values[Matrix.MSCALE_Y] *= 2f
                    values[Matrix.MTRANS_X] = -width / 2 + values[Matrix.MTRANS_X] * 2
                    values[Matrix.MTRANS_Y] = -height / 2 + values[Matrix.MTRANS_Y] * 2

                } else {//デフォルトに戻す
                    values = Arrays.copyOf(defaultValues!!, 9)
                }
                setMatrixValues()
                return true
            }
        })
    }

    fun setZoomListener(listener: ZoomListener) {
        this.listener = listener
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mImageMatrix = imageMatrix
        if (mImageMatrix != null) {
            mImageMatrix!!.getValues(values)
            defaultScale = values[Matrix.MSCALE_X]
            defaultValues = Arrays.copyOf(values, 9)
            val drawable = drawable
            if (drawable != null) {
                val rect = drawable.bounds
                isLandscape = (right - left) * rect.height() <= rect.width() * (bottom - top)
                if (isLandscape) {//上下が余っている
                    defaultMinLength = (width * rect.height()).toFloat() / rect.width()
                } else {//左右が余っている
                    defaultMinLength = (height * rect.width()).toFloat() / rect.height()
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //2点タッチは任せる（スケーリングと移動）
        scaleGestureDetector!!.onTouchEvent(event)
        gestureDetector!!.onTouchEvent(event)

        //1点タッチのとき（移動のみ）
        if (event.action == MotionEvent.ACTION_DOWN) {
            isBeingTouched = true
        } else if (event.action == MotionEvent.ACTION_MOVE && event.pointerCount == 1) {
            reloadMatrixValues()
            val index = event.actionIndex
            if (java.lang.Float.isNaN(singleStartX)) {//初回
                velocityTracker.clear()
                velocityTracker.addMovement(event)
                singleStartX = event.getX(index)
                singleStartY = event.getY(index)
                singleStartTX = values[Matrix.MTRANS_X]
                singleStartTY = values[Matrix.MTRANS_Y]
            } else {//2回目以降
                velocityTracker.addMovement(event)
                values[Matrix.MTRANS_X] = singleStartTX + event.getX(index) - singleStartX
                values[Matrix.MTRANS_Y] = singleStartTY + event.getY(index) - singleStartY
                setMatrixValues()
            }
        } else if (event.action == MotionEvent.ACTION_UP) {//タッチがなくなったとき
            isBeingTouched = false
            singleStartX = Float.NaN////1点タッチでなくなったことを保存
            velocityTracker.computeCurrentVelocity(10)
            val vx = velocityTracker.xVelocity
            val vy = velocityTracker.yVelocity
            val animator = ValueAnimator.ofFloat(1f, 0f)
            animator.addUpdateListener { valueAnimator ->
                if (isBeingTouched) animator.cancel()
                reloadMatrixValues()
                val f = 1f - valueAnimator.animatedFraction
                values[Matrix.MTRANS_X] += vx * f
                values[Matrix.MTRANS_Y] += vy * f
                setMatrixValues()
            }
            animator.duration = 1000
            animator.start()
        } else if (event.pointerCount > 1) {//2点以上になったとき
            singleStartX = Float.NaN//1点タッチでなくなったことを保存
        }
        if(zooming != ( values[Matrix.MSCALE_X] != defaultScale)) {
            zooming = !zooming
            if(listener!=null)listener!!.onZoomStateChanged(zooming)
        }
        return true
    }

    override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
        startFocalX = scaleGestureDetector.focusX
        startFocalY = scaleGestureDetector.focusY
        startSpan = scaleGestureDetector.currentSpan

        reloadMatrixValues()
        startX = values[Matrix.MTRANS_X]
        startY = values[Matrix.MTRANS_Y]
        startScale = values[Matrix.MSCALE_X]
        defaultK = defaultScale / startScale
        return true
    }

    override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
        reloadMatrixValues()
        var k = scaleGestureDetector.currentSpan / startSpan//指の幅が何倍になったか
        values[Matrix.MSCALE_X] = k * startScale
        //初期スケールより小さくはしない
        if (!isValidScale(values[Matrix.MSCALE_X])) {
            k = defaultK
            values[Matrix.MSCALE_X] = defaultScale
        }
        values[Matrix.MSCALE_Y] = values[Matrix.MSCALE_X]
        //ここまでスケールの計算

        //位置の計算
        values[Matrix.MTRANS_X] = startFocalX + (startX - startFocalX) * k + (scaleGestureDetector.focusX - startFocalX)
        values[Matrix.MTRANS_Y] = startFocalY + (startY - startFocalY) * k + (scaleGestureDetector.focusY - startFocalY)
        setMatrixValues()
        return true
    }


    override fun onScaleEnd(scaleGestureDetector: ScaleGestureDetector) {

    }

    private fun reloadMatrixValues() {
        mImageMatrix = imageMatrix
        mImageMatrix!!.getValues(values)
    }

    private fun setMatrixValues() {
        validateX()
        mImageMatrix!!.setValues(values)
        imageMatrix = mImageMatrix
        invalidate()
    }

    private fun isValidScale(scale: Float): Boolean {
        return scale > defaultScale
    }

    private fun validateX() {
        if (isLandscape) {
            values[Matrix.MTRANS_X] = validateTrans1(values[Matrix.MTRANS_X], values[Matrix.MSCALE_X], width.toFloat())
            values[Matrix.MTRANS_Y] = validateTrans2(values[Matrix.MTRANS_Y], values[Matrix.MSCALE_Y], height.toFloat())
        } else {
            values[Matrix.MTRANS_Y] = validateTrans1(values[Matrix.MTRANS_Y], values[Matrix.MSCALE_Y], height.toFloat())
            values[Matrix.MTRANS_X] = validateTrans2(values[Matrix.MTRANS_X], values[Matrix.MSCALE_X], width.toFloat())
        }
    }

    private fun validateTrans1(t: Float, scale: Float, length: Float): Float {//画面の比より長い辺に関するルール
        if (scale == defaultScale) {
            if (isLandscape) {
                completelyScrolledRight = true
                completelyScrolledLeft = true
            }
            return 0f
        }
        if (isLandscape) {
            //どっちにも余地はない
            completelyScrolledLeft = false
            completelyScrolledRight = false
        }
        if (t > 0) {
            if (isLandscape) completelyScrolledLeft = true//水平方向について計算しているとき
            return 0f//左側（上側）に余地がある
        }
        val temp = (1 - scale / defaultScale) * length
        if (temp > t) {//右側（下側）に余地がある
            if (isLandscape) completelyScrolledRight = true//水平方向について計算しているとき
            return temp
        }


        return t
    }

    private fun validateTrans2(t: Float, scale: Float, length: Float): Float {//画面の比より短い辺に関するルール
        if (!isLandscape) {
            if (defaultScale == scale) {
                completelyScrolledRight = true
                completelyScrolledLeft = true
            } else {
                completelyScrolledLeft = false
                completelyScrolledRight = false
            }
        }

        val temp = defaultMinLength * scale / defaultScale//画像が画面に占める幅
        if (temp > length) {
            if (t > 0) return 0f
            if (t + temp < length) return length - temp
        } else {
            return (length - temp) / 2
        }
        /*length/=2;
        if(length<t){
            if(!isLandscape)completelyScrolledLeft=true;//水平方向について計算しているとき
            return length;
        }
        if(length>t+temp){
            if(!isLandscape)completelyScrolledRight=true;//水平方向について計算しているとき
            return length-temp;
        }*/
        return t
    }


}

