package dev.starpad.calculatorforwear.tutorial

import android.graphics.Canvas
import androidx.core.graphics.ColorUtils

/**
 * Drawing area of one scene: a circle of [radius] around ([centerX], [centerY]) standing for the
 * watch face, so scenes place things as fractions of the real half-screen.
 */
internal class SceneFrame(val centerX: Float, val centerY: Float, val radius: Float) {
  /** Converts a length given as a fraction of the radius into pixels. */
  fun r(fraction: Float): Float = radius * fraction
}

/** [color] with its alpha replaced by [alpha], given from 0 to 1. */
internal fun withAlpha(color: Int, alpha: Float): Int =
  ColorUtils.setAlphaComponent(color, (alpha.coerceIn(0f, 1f) * 255).toInt())

/**
 * One looping gesture explanation.
 *
 * Scenes are stateless: everything is derived from the loop [phase] handed to [draw], so pausing,
 * resuming and drawing a still frame need no bookkeeping.
 */
internal abstract class TutorialScene(
  /** Length of one loop. */
  val durationMs: Long,
  /** Most explanatory moment of the loop, drawn as a still when the system disables animations. */
  val previewPhase: Float,
) {
  /** Draws the scene at [phase], between 0 at the start of the loop and 1 at its end. */
  abstract fun draw(canvas: Canvas, painter: ScenePainter, frame: SceneFrame, phase: Float)
}
