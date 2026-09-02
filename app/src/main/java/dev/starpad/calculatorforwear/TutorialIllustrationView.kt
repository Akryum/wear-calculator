package dev.starpad.calculatorforwear

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Decorative, looping visual explanation for one calculator gesture. */
class TutorialIllustrationView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
) : View(context, attrs) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private var step = TutorialStep.TAP_NUMBER
  private var phase = 0f
  private var animator: ValueAnimator? = null

  /** Shows specified gesture and restarts its loop. */
  fun showStep(value: TutorialStep) {
    step = value
    phase = 0f
    restartAnimation()
    invalidate()
  }

  /** Stops animation while activity is not visible. */
  fun pauseAnimation() {
    animator?.cancel()
    animator = null
  }

  /** Restarts animation after activity returns to foreground. */
  fun resumeAnimation() {
    restartAnimation()
  }

  override fun onDetachedFromWindow() {
    pauseAnimation()
    super.onDetachedFromWindow()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val centerX = width / 2f
    val centerY = height * 0.32f
    val radius = min(width, height) * 0.19f

    when (step) {
      TutorialStep.TAP_NUMBER -> drawNumberTap(canvas, centerX, centerY, radius)
      TutorialStep.DRAG_ACTION -> drawActionDrag(canvas, centerX, centerY, radius)
      TutorialStep.CLEAR_INPUT -> drawClearGesture(canvas, centerX, centerY, radius)
    }
  }

  private fun restartAnimation() {
    pauseAnimation()
    if (!ValueAnimator.areAnimatorsEnabled() || !isAttachedToWindow) return

    animator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = LOOP_DURATION_MS
      repeatCount = ValueAnimator.INFINITE
      interpolator = LinearInterpolator()
      addUpdateListener {
        phase = it.animatedValue as Float
        invalidate()
      }
      start()
    }
  }

  private fun drawNumberTap(canvas: Canvas, x: Float, y: Float, radius: Float) {
    val selectedIndex = 1
    val pulse = if (phase < 0.45f) 1f + phase * 0.25f else 1.12f - (phase - 0.45f) * 0.22f
    val ringRadius = radius * 0.76f
    val buttonRadius = radius * 0.22f

    repeat(8) { index ->
      val angle = (PI * 2 * index / 8 - PI / 2).toFloat()
      val buttonX = x + cos(angle.toDouble()).toFloat() * ringRadius
      val buttonY = y + sin(angle.toDouble()).toFloat() * ringRadius
      val selected = index == selectedIndex
      drawCircle(canvas, buttonX, buttonY, buttonRadius * if (selected) pulse else 1f, selected)
      drawLabel(canvas, ((index + 1) % 10).toString(), buttonX, buttonY + buttonRadius * 0.32f, buttonRadius * 0.9f, Color.WHITE)
    }

    val angle = (PI * 2 * selectedIndex / 8 - PI / 2).toFloat()
    val targetX = x + cos(angle.toDouble()).toFloat() * ringRadius
    val targetY = y + sin(angle.toDouble()).toFloat() * ringRadius
    val fingerProgress = min(phase / 0.45f, 1f)
    drawFinger(canvas, targetX, targetY + radius * 0.52f * (1f - fingerProgress), buttonRadius * 0.35f, fingerProgress)
  }

  private fun drawActionDrag(canvas: Canvas, x: Float, y: Float, radius: Float) {
    val targetY = y - radius * 0.7f
    val dragProgress = ((phase - 0.25f) / 0.55f).coerceIn(0f, 1f)
    val fingerY = y + radius * 0.45f * (1f - min(phase / 0.25f, 1f)) - (y - targetY) * dragProgress
    val selectorVisible = phase > 0.2f

    drawCircle(canvas, x, y, radius * if (phase < 0.25f) 0.38f else 0.5f, selectorVisible)
    drawLabel(canvas, "=", x, y + radius * 0.17f, radius * 0.55f, Color.WHITE)
    drawCircle(canvas, x, targetY, radius * 0.3f, dragProgress > 0.65f)
    drawLabel(canvas, "+", x, targetY + radius * 0.13f, radius * 0.52f, Color.WHITE)

    paint.color = ORANGE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = radius * 0.07f
    paint.strokeCap = Paint.Cap.ROUND
    canvas.drawPath(Path().apply {
      moveTo(x, y - radius * 0.45f)
      lineTo(x, targetY + radius * 0.36f)
    }, paint)
    drawFinger(canvas, x, fingerY, radius * 0.24f, 1f)
  }

  private fun drawClearGesture(canvas: Canvas, x: Float, y: Float, radius: Float) {
    val targetX = x - radius * 1.05f
    val targetY = y - radius * 0.65f
    val dragProgress = ((phase - 0.18f) / 0.4f).coerceIn(0f, 1f)
    val holding = phase > 0.63f
    val fingerX = x - (x - targetX) * dragProgress
    val fingerY = y - (y - targetY) * dragProgress

    drawCircle(canvas, x, y, radius * 0.43f, false)
    drawLabel(canvas, "=", x, y + radius * 0.16f, radius * 0.52f, Color.WHITE)
    drawCircle(canvas, targetX, targetY, radius * if (holding) 0.36f else 0.3f, holding)
    drawLabel(canvas, if (holding) "C" else "⌫", targetX, targetY + radius * 0.12f, radius * 0.42f, Color.WHITE)

    paint.color = ORANGE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = radius * 0.07f
    paint.strokeCap = Paint.Cap.ROUND
    canvas.drawLine(x - radius * 0.35f, y - radius * 0.22f, targetX + radius * 0.28f, targetY + radius * 0.18f, paint)
    drawFinger(canvas, fingerX, fingerY, radius * 0.24f, if (holding) 1.2f else 1f)
  }

  private fun drawCircle(canvas: Canvas, x: Float, y: Float, radius: Float, active: Boolean) {
    paint.style = Paint.Style.FILL
    paint.color = if (active) ORANGE else Color.rgb(48, 48, 48)
    canvas.drawCircle(x, y, radius, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = radius * 0.07f
    paint.color = if (active) Color.rgb(255, 205, 133) else Color.rgb(105, 105, 105)
    canvas.drawCircle(x, y, radius, paint)
  }

  private fun drawFinger(canvas: Canvas, x: Float, y: Float, radius: Float, scale: Float) {
    paint.style = Paint.Style.FILL
    paint.color = Color.argb((210 * scale.coerceAtMost(1f)).toInt(), 255, 255, 255)
    canvas.drawCircle(x, y, radius * scale, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = radius * 0.16f
    paint.color = Color.argb(170, 0, 0, 0)
    canvas.drawCircle(x, y, radius * scale, paint)
  }

  private fun drawLabel(canvas: Canvas, text: String, x: Float, baseline: Float, size: Float, color: Int) {
    paint.style = Paint.Style.FILL
    paint.color = color
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = size
    canvas.drawText(text, x, baseline, paint)
  }

  private companion object {
    const val LOOP_DURATION_MS = 2_000L
    const val ORANGE = 0xFFDC8F3D.toInt()
  }
}
