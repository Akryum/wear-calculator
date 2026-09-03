package dev.starpad.calculatorforwear.tutorial

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import dev.starpad.calculatorforwear.R
import dev.starpad.calculatorforwear.menu.RadialChoice
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Draws the calculator pieces the scenes are made of, in the fixed white and brand-orange palette
 * of the tutorial overlay.
 *
 * Layouts mirror the real screen so it looks familiar once the tutorial is gone: digits sit where
 * MainActivity places them and actions where the radial menu shows them.
 */
internal class ScenePainter(private val context: Context) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
  private val textBounds = Rect()
  private val backspaceIcon = icon(R.drawable.ic_baseline_backspace_24)
  private val clearIcon = icon(R.drawable.ic_baseline_clear_24)

  /** Weight of the real digit buttons and menu glyphs. */
  val mediumTypeface: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

  /** Brand color, used for whatever the finger is acting on. */
  val orange: Int = context.getColor(R.color.brand_orange)

  /** The fingertip and its trail. */
  val touch = TouchIndicator(orange)

  /** Title of the real history section, in the current language. */
  val historyTitle: String get() = context.getString(R.string.history_title)

  /** Label of the real settings button, in the current language. */
  val settingsTitle: String get() = context.getString(R.string.settings_title)

  /** One history row, formatted exactly as the real list formats it. */
  fun historyEntry(expression: String, result: String): String =
    context.getString(R.string.history_entry, expression, result)

  /** Where MainActivity places [digit] on the ring: 0 at the bottom, then clockwise. */
  fun digitPosition(frame: SceneFrame, digit: Int, offsetY: Float = 0f): PointF {
    val angle = PI * 2 * digit / 10 + PI / 2
    return PointF(
      frame.centerX + cos(angle).toFloat() * frame.r(DIGIT_RING),
      frame.centerY + offsetY + sin(angle).toFloat() * frame.r(DIGIT_RING),
    )
  }

  /**
   * Digits as the calculator shows them, shifted by [offsetY]. The [pressed] digit gets the
   * ripple of a real button press, grown to [rippleFraction] of its full size.
   */
  fun drawDigitRing(
    canvas: Canvas,
    frame: SceneFrame,
    alpha: Float,
    offsetY: Float = 0f,
    pressed: Int? = null,
    rippleFraction: Float = 0f,
  ) {
    if (alpha <= 0f) return
    for (digit in 0..9) {
      val position = digitPosition(frame, digit, offsetY)
      if (digit == pressed && rippleFraction > 0f) {
        fillCircle(canvas, position.x, position.y, frame.r(RIPPLE) * rippleFraction, orange, alpha * 0.65f)
      }
      drawText(canvas, digit.toString(), position.x, position.y, frame.r(DIGIT_SIZE), Color.WHITE, alpha * 0.9f)
    }
  }

  /** Position of [choice] in the frame, mirroring the real center menu layout. */
  fun choicePosition(frame: SceneFrame, choice: RadialChoice): PointF {
    val angle = when (choice) {
      RadialChoice.EQUALS -> return PointF(frame.centerX, frame.centerY)
      RadialChoice.MULTIPLY -> 0.0
      RadialChoice.DECIMAL -> PI / 4
      RadialChoice.ADD -> PI / 2
      RadialChoice.PERCENT -> PI * 3 / 4
      RadialChoice.DIVIDE -> PI
      RadialChoice.BACKSPACE -> -PI * 3 / 4
      RadialChoice.SUBTRACT -> -PI / 2
      RadialChoice.POWER -> -PI / 4
    }
    return PointF(
      frame.centerX + cos(angle).toFloat() * frame.r(MENU_RING),
      frame.centerY + sin(angle).toFloat() * frame.r(MENU_RING),
    )
  }

  /** The choice the real menu selects for a finger at ([x], [y]), dead zone included. */
  fun choiceAt(frame: SceneFrame, x: Float, y: Float): RadialChoice {
    val dx = x - frame.centerX
    val dy = y - frame.centerY
    if (hypot(dx, dy) <= frame.r(DEAD_ZONE)) return RadialChoice.EQUALS
    return RadialChoice.at(RadialChoice.CENTER_RADIUS + 1f, atan2(dy, dx))
  }

  /**
   * Radial menu at [alpha], with the [hovered] choice grown by [hoveredScale] and tinted the way
   * the real one grows under the finger. [clearArmed] swaps backspace for the clear-all icon.
   */
  fun drawRadialMenu(
    canvas: Canvas,
    frame: SceneFrame,
    alpha: Float,
    hovered: RadialChoice?,
    hoveredScale: Float = HOVER_SCALE,
    clearArmed: Boolean = false,
  ) {
    if (alpha <= 0f) return
    RadialChoice.entries.forEach { choice ->
      val position = choicePosition(frame, choice)
      val isHovered = choice == hovered
      val scale = if (isHovered) hoveredScale else 1f
      val color = if (isHovered) orange else Color.WHITE
      val glyphAlpha = alpha * if (isHovered) 1f else MENU_ALPHA
      if (choice == RadialChoice.BACKSPACE) {
        val icon = if (clearArmed) clearIcon else backspaceIcon
        drawIcon(canvas, icon, position.x, position.y, frame.r(ICON_SIZE) * scale, color, glyphAlpha)
      } else {
        drawText(canvas, choice.symbol, position.x, position.y, frame.r(GLYPH_SIZE) * scale, color, glyphAlpha)
      }
    }
  }

  /** Expression display in the middle of the screen, [scale] letting a new digit pop in. */
  fun drawDisplay(canvas: Canvas, frame: SceneFrame, text: String, alpha: Float, scale: Float = 1f) {
    if (text.isEmpty()) return
    drawText(canvas, text, frame.centerX, frame.centerY, frame.r(DISPLAY_SIZE) * scale, Color.WHITE, alpha, Typeface.DEFAULT)
  }

  /** Draws [text] with its ink centered on ([x], [y]), so "+" and "-" line up like real TextViews. */
  fun drawText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    size: Float,
    color: Int,
    alpha: Float,
    typeface: Typeface = mediumTypeface,
  ) {
    if (alpha <= 0f) return
    textPaint.typeface = typeface
    textPaint.textSize = size
    textPaint.color = withAlpha(color, alpha)
    textPaint.getTextBounds(text, 0, text.length, textBounds)
    canvas.drawText(text, x, y - textBounds.exactCenterY(), textPaint)
  }

  private fun drawIcon(canvas: Canvas, icon: Drawable, x: Float, y: Float, size: Float, color: Int, alpha: Float) {
    if (alpha <= 0f) return
    val half = (size / 2).toInt()
    icon.setBounds(x.toInt() - half, y.toInt() - half, x.toInt() + half, y.toInt() + half)
    icon.setTint(color)
    icon.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    icon.draw(canvas)
  }

  private fun fillCircle(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int, alpha: Float) {
    paint.style = Paint.Style.FILL
    paint.color = withAlpha(color, alpha)
    canvas.drawCircle(x, y, radius, paint)
  }

  private fun icon(@DrawableRes id: Int): Drawable =
    requireNotNull(AppCompatResources.getDrawable(context, id)).mutate()

  private companion object {
    /** Digit ring radius; the real buttons sit at about 0.77 of the half-screen. */
    const val DIGIT_RING = 0.8f
    const val DIGIT_SIZE = 0.3f
    const val RIPPLE = 0.3f

    /** Every real menu glyph sits at about half the half-screen from the center. */
    const val MENU_RING = 0.58f
    const val GLYPH_SIZE = 0.32f
    const val ICON_SIZE = 0.3f

    /** The real dead zone is 36px of a 240px half-screen. */
    const val DEAD_ZONE = 0.15f

    /** Rest alpha and hover growth of the real menu glyphs. */
    const val MENU_ALPHA = 0.7f
    const val HOVER_SCALE = 1.3f

    const val DISPLAY_SIZE = 0.3f
  }
}
