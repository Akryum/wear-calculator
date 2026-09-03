package dev.starpad.calculatorforwear

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.withClip
import dev.starpad.calculatorforwear.tutorial.ChooseActionScene
import dev.starpad.calculatorforwear.tutorial.ClearInputScene
import dev.starpad.calculatorforwear.tutorial.DigitTapScene
import dev.starpad.calculatorforwear.tutorial.HistoryScrollScene
import dev.starpad.calculatorforwear.tutorial.SceneFrame
import dev.starpad.calculatorforwear.tutorial.ScenePainter
import dev.starpad.calculatorforwear.tutorial.TutorialScene
import kotlin.math.min

/**
 * Decorative, looping visual explanation for one calculator gesture. It animates only while it
 * is [active], attached and in a visible window, and freezes where it is otherwise.
 */
class TutorialIllustrationView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
) : View(context, attrs) {
  private val painter = ScenePainter(context)
  private var scene: TutorialScene = DigitTapScene
  private var phase = 0f
  private var animator: ValueAnimator? = null

  /** Drawing area for the current size, rebuilt on layout rather than on every frame. */
  private var frame: SceneFrame? = null

  /** Gesture shown; changing it restarts the loop from the beginning. */
  var step: TutorialStep = TutorialStep.TAP_NUMBER
    set(value) {
      if (field == value) return
      field = value
      scene = when (value) {
        TutorialStep.TAP_NUMBER -> DigitTapScene
        TutorialStep.DRAG_ACTION -> ChooseActionScene
        TutorialStep.CLEAR_INPUT -> ClearInputScene
        TutorialStep.SCROLL_HISTORY -> HistoryScrollScene
      }
      phase = 0f
      if (animator != null) {
        pauseAnimation()
        resumeAnimation()
      }
      invalidate()
    }

  /** Whether this page is the one on screen; only the active page animates. */
  var active: Boolean = false
    set(value) {
      field = value
      syncAnimation()
    }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    syncAnimation()
  }

  override fun onDetachedFromWindow() {
    pauseAnimation()
    super.onDetachedFromWindow()
  }

  override fun onWindowVisibilityChanged(visibility: Int) {
    super.onWindowVisibilityChanged(visibility)
    syncAnimation()
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    val innerWidth = (w - paddingLeft - paddingRight).toFloat()
    val innerHeight = (h - paddingTop - paddingBottom).toFloat()
    // The padded area stands for the whole watch face, scaled to the largest circle that fits.
    frame = if (innerWidth <= 0f || innerHeight <= 0f) null else SceneFrame(
      centerX = paddingLeft + innerWidth / 2,
      centerY = paddingTop + innerHeight / 2,
      radius = min(innerWidth, innerHeight) / 2,
    )
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val frame = frame ?: return
    val shownPhase = if (ValueAnimator.areAnimatorsEnabled()) phase else scene.previewPhase
    canvas.withClip(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom) {
      scene.draw(this, painter, frame, shownPhase)
    }
  }

  /** Runs the loop only while it can be seen, continuing from where it was frozen. */
  private fun syncAnimation() {
    if (active && isAttachedToWindow && windowVisibility == VISIBLE) resumeAnimation() else pauseAnimation()
  }

  private fun pauseAnimation() {
    animator?.cancel()
    animator = null
  }

  private fun resumeAnimation() {
    if (animator != null || !ValueAnimator.areAnimatorsEnabled()) return
    animator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = scene.durationMs
      repeatCount = ValueAnimator.INFINITE
      interpolator = LinearInterpolator()
      addUpdateListener {
        phase = it.animatedValue as Float
        invalidate()
      }
      start()
      setCurrentFraction(phase)
    }
  }
}
