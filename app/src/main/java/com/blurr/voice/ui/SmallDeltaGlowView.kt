package com.blurr.voice.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.blurr.voice.R
import kotlin.math.abs
import kotlin.math.sin

class SmallDeltaGlowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var coreColor = ContextCompat.getColor(context, R.color.delta_idle)
    private var rotationAngle = 0f
    private var pulse = 0.35f
    private var glowAnimator: ValueAnimator? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6070D16")
    }

    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        color = coreColor
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = coreColor
    }

    private val arcBounds = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cx = w / 2f
        val cy = h / 2f
        val r = (w.coerceAtMost(h) / 2f) * 0.72f
        arcBounds.set(cx - r, cy - r, cx + r, cy + r)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = width.coerceAtMost(height) / 2f

        canvas.drawCircle(cx, cy, radius, backgroundPaint)
        for (i in 0 until 3) {
            canvas.drawArc(arcBounds, rotationAngle + i * 120f + 12f, 84f, false, outerRingPaint)
        }
        corePaint.color = coreColor
        corePaint.alpha = (160 + 95 * pulse).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, radius * (0.24f + 0.06f * pulse), corePaint)
    }

    fun setColor(color: Int) {
        coreColor = color
        outerRingPaint.color = color
        if (glowAnimator?.isRunning == true) {
            outerRingPaint.setShadowLayer(10f, 0f, 0f, color)
        }
        invalidate()
    }

    fun startGlow() {
        if (glowAnimator?.isRunning == true) return
        glowAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2600L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                rotationAngle = animator.animatedValue as Float
                pulse = abs(sin(Math.toRadians(rotationAngle.toDouble()))).toFloat()
                outerRingPaint.setShadowLayer(4f + 8f * pulse, 0f, 0f, coreColor)
                invalidate()
            }
        }
        glowAnimator?.start()
    }

    fun stopGlow() {
        glowAnimator?.cancel()
        glowAnimator = null
        outerRingPaint.clearShadowLayer()
        pulse = 0.25f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopGlow()
    }
}