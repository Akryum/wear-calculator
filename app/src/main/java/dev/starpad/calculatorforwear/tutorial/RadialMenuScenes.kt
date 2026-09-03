package dev.starpad.calculatorforwear.tutorial

import android.graphics.Canvas
import dev.starpad.calculatorforwear.menu.RadialChoice
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.easeInOut
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.easeOut
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.lerp
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.loopFade
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.segment
import kotlin.math.PI
import kotlin.math.sin

/**
 * Skeleton shared by the radial menu scenes: the finger lands on the center, the digits give way
 * to the menu with equals already highlighted, the finger drags to one choice, holds there and
 * releases. Subclasses pick the choice, the pacing and what the display shows before and after.
 */
internal abstract class RadialMenuScene(
  durationMs: Long,
  previewPhase: Float,
  private val target: RadialChoice,
  protected val keys: Keys,
  /** Display contents before the gesture and once it has been committed. */
  private val before: String,
  private val after: String,
) : TutorialScene(durationMs, previewPhase) {
  /** Loop phases delimiting each stage of the gesture, in order. */
  class Keys(
    val pressStart: Float,
    val pressEnd: Float,
    val dragStart: Float,
    val dragEnd: Float,
    val releaseStart: Float,
    val releaseEnd: Float,
  )

  /** Whether holding has already turned backspace into clear-all at [phase]. */
  protected open fun clearArmed(phase: Float): Boolean = false

  /** Growth of the hovered glyph at [phase]; subclasses may bump it to mark a threshold. */
  protected open fun hoveredScale(phase: Float): Float = 1.3f

  /** Fraction of a long press completed at [phase], shown on the fingertip. */
  protected open fun holdProgress(phase: Float): Float = 0f

  override fun draw(canvas: Canvas, painter: ScenePainter, frame: SceneFrame, phase: Float) {
    // Calculator and menu hand over one after the other, so their glyphs never overlap.
    val pressMid = (keys.pressStart + keys.pressEnd) / 2
    val releaseMid = (keys.releaseStart + keys.releaseEnd) / 2
    val calculatorAlpha = if (phase < releaseMid) 1f - segment(phase, keys.pressStart, pressMid) else segment(phase, releaseMid, keys.releaseEnd)
    val menu = if (phase < keys.releaseStart) segment(phase, pressMid, keys.pressEnd) else 1f - segment(phase, keys.releaseStart, releaseMid)
    val destination = painter.choicePosition(frame, target)
    val drag = easeInOut(segment(phase, keys.dragStart, keys.dragEnd))
    val fingerX = lerp(frame.centerX, destination.x, drag)
    val fingerY = lerp(frame.centerY, destination.y, drag)

    painter.drawDigitRing(canvas, frame, calculatorAlpha)
    val text = if (phase < keys.releaseStart) before else after
    painter.drawDisplay(canvas, frame, text, calculatorAlpha * loopFade(phase))

    if (menu > 0f) {
      val hovered = if (phase < keys.releaseStart) painter.choiceAt(frame, fingerX, fingerY) else null
      painter.drawRadialMenu(canvas, frame, menu, hovered, hoveredScale(phase), clearArmed(phase))
    }

    val approach = easeOut(segment(phase, 0f, keys.pressStart))
    val liftRaw = segment(phase, keys.releaseStart, keys.releaseEnd)
    val lift = easeOut(liftRaw)
    val y = if (phase < keys.pressStart) lerp(frame.centerY + frame.r(0.3f), frame.centerY, approach) else fingerY
    val pressed = phase >= keys.pressStart && phase < keys.releaseStart
    if (phase >= keys.dragStart) painter.touch.drawTrail(canvas, frame, frame.centerX, frame.centerY, fingerX, fingerY, 1f - lift)
    painter.touch.draw(canvas, frame, Touch(
      fingerX, y,
      alpha = approach * (1f - lift),
      pressed = pressed,
      hold = holdProgress(phase),
      lift = liftRaw,
    ))
  }
}

/** Pressing the center and dragging down to "+" appends it to the expression. */
internal object ChooseActionScene : RadialMenuScene(
  durationMs = 3200L,
  previewPhase = 0.6f,
  target = RadialChoice.ADD,
  keys = Keys(
    pressStart = 0.10f,
    pressEnd = 0.18f,
    dragStart = 0.28f,
    dragEnd = 0.52f,
    releaseStart = 0.66f,
    releaseEnd = 0.76f,
  ),
  before = "75",
  after = "75+",
)

/** Dragging to backspace and holding turns it into clear-all, which empties the expression. */
internal object ClearInputScene : RadialMenuScene(
  durationMs = 3800L,
  previewPhase = 0.72f,
  target = RadialChoice.BACKSPACE,
  keys = Keys(
    pressStart = 0.08f,
    pressEnd = 0.15f,
    dragStart = 0.22f,
    dragEnd = 0.40f,
    releaseStart = 0.80f,
    releaseEnd = 0.88f,
  ),
  before = "75+",
  after = "",
) {
  /** Same delay after arriving on backspace as the real long press, at this loop's length. */
  private const val ARM_AT = 0.62f
  private const val ARM_POP = 0.06f

  override fun clearArmed(phase: Float): Boolean = phase >= ARM_AT

  // The rim fills from the moment the finger arrives on backspace until clear-all arms.
  override fun holdProgress(phase: Float): Float = segment(phase, keys.dragEnd, ARM_AT)

  // A brief bump on top of the hover growth, standing in for the haptic the real threshold plays.
  override fun hoveredScale(phase: Float): Float =
    1.3f + 0.3f * sin(PI * segment(phase, ARM_AT, ARM_AT + ARM_POP)).toFloat()
}
