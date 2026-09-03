package dev.starpad.calculatorforwear.tutorial

import android.graphics.Canvas
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.easeInOut
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.easeOut
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.lerp
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.segment

/** A finger taps two digits on the ring and each one lands in the display, as on the real screen. */
internal object DigitTapScene : TutorialScene(durationMs = 3200L, previewPhase = 0.15f) {
  private const val FIRST_DIGIT = 7
  private const val SECOND_DIGIT = 5

  /** Keyframes: each tap presses for [TAP] then releases for [TAP]. */
  private const val FIRST_TAP = 0.10f
  private const val SECOND_TAP = 0.50f
  private const val TAP = 0.08f
  private const val LEAVE_START = 0.66f
  private const val LEAVE_END = 0.80f
  private const val CLEAR_START = 0.90f

  override fun draw(canvas: Canvas, painter: ScenePainter, frame: SceneFrame, phase: Float) {
    val first = painter.digitPosition(frame, FIRST_DIGIT)
    val second = painter.digitPosition(frame, SECOND_DIGIT)
    val firstTap = tap(phase, FIRST_TAP)
    val secondTap = tap(phase, SECOND_TAP)
    val pressed = when {
      firstTap.pressing -> FIRST_DIGIT
      secondTap.pressing -> SECOND_DIGIT
      else -> null
    }
    painter.drawDigitRing(
      canvas, frame, alpha = 1f,
      pressed = pressed,
      rippleFraction = maxOf(firstTap.ripple, secondTap.ripple),
    )

    // Each release drops its digit into the display, which empties again before the loop restarts.
    val secondLanded = segment(phase, SECOND_TAP + TAP, SECOND_TAP + 2 * TAP)
    val firstLanded = segment(phase, FIRST_TAP + TAP, FIRST_TAP + 2 * TAP)
    val text = if (secondLanded > 0f) "$FIRST_DIGIT$SECOND_DIGIT" else FIRST_DIGIT.toString()
    val landing = if (secondLanded > 0f) secondLanded else firstLanded
    val displayAlpha = firstLanded * (1f - segment(phase, CLEAR_START, 1f))
    painter.drawDisplay(canvas, frame, text, displayAlpha, scale = lerp(0.8f, 1f, easeOut(landing)))

    // The finger fades in beside the first digit, travels to the second and drifts away.
    val approach = easeOut(segment(phase, 0f, FIRST_TAP))
    val travel = easeInOut(segment(phase, FIRST_TAP + 2 * TAP, SECOND_TAP))
    val liftRaw = segment(phase, LEAVE_START, LEAVE_END)
    val leave = easeOut(liftRaw)
    val x = when {
      phase < FIRST_TAP + 2 * TAP -> lerp(first.x + frame.r(0.35f), first.x, approach)
      phase < LEAVE_START -> lerp(first.x, second.x, travel)
      else -> lerp(second.x, second.x - frame.r(0.25f), leave)
    }
    val y = when {
      phase < FIRST_TAP + 2 * TAP -> lerp(first.y + frame.r(0.35f), first.y, approach)
      phase < LEAVE_START -> lerp(first.y, second.y, travel)
      else -> lerp(second.y, second.y + frame.r(0.1f), leave)
    }
    painter.touch.draw(canvas, frame, Touch(
      x, y,
      alpha = approach * (1f - leave),
      pressed = pressed != null,
      lift = liftRaw,
    ))
  }

  /** Press and release of one tap starting at [start]: the ripple grows, then fades on release. */
  private fun tap(phase: Float, start: Float): Tap {
    val press = segment(phase, start, start + TAP)
    val release = segment(phase, start + TAP, start + 2 * TAP)
    return Tap(pressing = press > 0f && release == 0f, ripple = easeOut(press) * (1f - release))
  }

  private class Tap(val pressing: Boolean, val ripple: Float)
}
