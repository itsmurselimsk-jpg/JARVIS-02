package com.blurr.voice.views

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

/**
 * Legacy View-compatible JARVIS Arc Core renderer used in XML-backed activities.
 */
class DeltaSymbolView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var glowAnimator: ValueAnimator? = null
    private var rotationAngle = 0f
    private var pulseFraction = 0.4f

    private var coreColor: Int = ContextCompat.getColor(context, R.color.delta_idle)

    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3f * resources.displayMetrics.density
        color = coreColor
    }

    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2f * resources.displayMetrics.density
        color = Color.parseColor("#0088FF")
    }

    private val centerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = coreColor
        alpha = 90
    }

    private val outerRect = RectF()
    private val innerRect = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cx = w / 2f
        val cy = h / 2f
        val radius = (w.coerceAtMost(h) / 2f) * 0.78f
        outerRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        val innerR = radius * 0.68f
        innerRect.set(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (width.coerceAtMost(height) / 2f) * 0.78f

        centerGlowPaint.color = coreColor
        centerGlowPaint.alpha = (60 + 80 * pulseFraction).toInt()
        canvas.drawCircle(cx, cy, radius * (0.38f + 0.08f * pulseFraction), centerGlowPaint)

        for (i in 0 until 4) {
            canvas.drawArc(outerRect, rotationAngle + i * 90f + 10f, 62f, false, outerRingPaint)
        }
        for (i in 0 until 3) {
            canvas.drawArc(innerRect, -rotationAngle * 1.4f + i * 120f + 14f, 84f, false, innerRingPaint)
        }
        canvas.drawCircle(cx, cy, radius * 0.2f, outerRingPaint)
    }

    fun setColor(color: Int) {
        coreColor = color
        outerRingPaint.color = color
        if (glowAnimator?.isRunning == true) {
            outerRingPaint.setShadowLayer(18f, 0f, 0f, color)
        }
        invalidate()
    }

    fun startGlow() {
        if (glowAnimator?.isRunning == true) return
        glowAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 4000L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                rotationAngle = animator.animatedValue as Float
                pulseFraction = kotlin.math.abs(kotlin.math.sin(Math.toRadians(rotationAngle.toDouble()))).toFloat()
                outerRingPaint.setShadowLayer(10f + 14f * pulseFraction, 0f, 0f, coreColor)
                invalidate()
            }
        }
        glowAnimator?.start()
    }

    fun stopGlow() {
        glowAnimator?.cancel()
        glowAnimator = null
        outerRingPaint.clearShadowLayer()
        pulseFraction = 0.25f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopGlow()
    }
}
