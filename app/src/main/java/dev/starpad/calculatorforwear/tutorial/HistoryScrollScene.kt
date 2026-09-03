package dev.starpad.calculatorforwear.tutorial

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.easeInOut
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.easeOut
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.lerp
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.loopFade
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.segment

/**
 * A finger swipes the calculator up, revealing the history section stacked below it the way the
 * real page does: a title, recent rows and the settings button.
 */
internal object HistoryScrollScene : TutorialScene(durationMs = 3200L, previewPhase = 0.56f) {
  private const val SWIPE_START = 0.12f
  private const val SWIPE_END = 0.50f
  private const val LIFT_END = 0.62f

  /** How far the content and the finger travel, as a fraction of the frame radius. */
  private const val TRAVEL = 1.65f

  /** Where the history title rests below the calculator, so it scrolls into view. */
  private const val HISTORY_TOP = 1.2f

  override fun draw(canvas: Canvas, painter: ScenePainter, frame: SceneFrame, phase: Float) {
    val swipe = easeInOut(segment(phase, SWIPE_START, SWIPE_END))
    val scroll = frame.r(TRAVEL) * swipe

    // The whole page fades at the loop edges, as it snaps back to the top when the loop restarts.
    val fade = loopFade(phase)
    val layer = if (fade < 1f) canvas.saveLayerAlpha(null, (fade * 255).toInt()) else canvas.save()
    painter.drawDigitRing(canvas, frame, alpha = 1f, offsetY = -scroll)
    val top = frame.centerY + frame.r(HISTORY_TOP) - scroll
    val x = frame.centerX
    painter.drawText(canvas, painter.historyTitle, x, top, frame.r(0.3f), Color.WHITE, 1f, Typeface.DEFAULT)
    painter.drawText(canvas, painter.historyEntry("75+8", "83"), x, top + frame.r(0.42f), frame.r(0.22f), Color.WHITE, 0.85f, Typeface.DEFAULT)
    painter.drawText(canvas, painter.historyEntry("6×7", "42"), x, top + frame.r(0.74f), frame.r(0.22f), Color.WHITE, 0.85f, Typeface.DEFAULT)
    painter.drawText(canvas, painter.settingsTitle, x, top + frame.r(1.12f), frame.r(0.24f), painter.orange, 1f)
    canvas.restoreToCount(layer)

    val approach = easeOut(segment(phase, 0f, SWIPE_START))
    val liftRaw = segment(phase, SWIPE_END, LIFT_END)
    val lift = easeOut(liftRaw)
    val startY = frame.centerY + frame.r(TRAVEL / 2)
    val y = lerp(startY, frame.centerY - frame.r(TRAVEL / 2), swipe)
    val pressed = phase >= SWIPE_START && phase < SWIPE_END
    // The trail stays on the glass while the page underneath scrolls, so it is drawn unscrolled.
    if (phase >= SWIPE_START) painter.touch.drawTrail(canvas, frame, x, startY, x, y, (1f - lift) * fade)
    painter.touch.draw(canvas, frame, Touch(
      x, y,
      alpha = approach * (1f - lift) * fade,
      pressed = pressed,
      lift = liftRaw,
    ))
  }
}
