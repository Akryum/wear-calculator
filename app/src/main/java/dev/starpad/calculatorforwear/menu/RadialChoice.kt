package dev.starpad.calculatorforwear.menu

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import dev.starpad.calculatorforwear.R
import kotlin.math.PI

/**
 * One selectable action of the center radial menu, tying together the text appended to the input,
 * the glyph highlighted under the finger and the name shown on the bezel while dragging.
 *
 * @property symbol what the choice appends to the expression, or a marker for the choices that run
 *   an action instead of appending anything.
 * @property viewId the glyph in `center_menu_layout` that grows while this choice is hovered.
 * @property labelRes the human-readable name shown on the bezel while this choice is hovered.
 */
enum class RadialChoice(
  val symbol: String,
  @param:IdRes val viewId: Int,
  @param:StringRes val labelRes: Int,
) {
  EQUALS("=", R.id.txt_equal, R.string.action_equals),
  ADD("+", R.id.txt_add, R.string.action_add),
  SUBTRACT("-", R.id.txt_subtract, R.string.action_subtract),
  MULTIPLY("×", R.id.txt_multiply, R.string.action_multiply),
  DIVIDE("÷", R.id.txt_divide, R.string.action_divide),
  DECIMAL(".", R.id.txt_dot, R.string.action_decimal),
  PERCENT("%", R.id.txt_percent, R.string.action_percent),
  POWER("^", R.id.txt_pow, R.string.action_power),
  BACKSPACE("backspace", R.id.img_backspace, R.string.action_backspace);

  companion object {
    /** Radius in pixels around the center that keeps selecting equals, whatever the angle. */
    const val CENTER_RADIUS = 36f

    /**
     * Maps a finger position, given as its distance and angle from the center of the screen, to
     * the action it selects. The eight outer choices split the circle into PI/4 sectors centered
     * on their glyphs; angles follow screen coordinates, so they grow clockwise from 3 o'clock.
     */
    fun at(distance: Float, angle: Float): RadialChoice = when {
      distance <= CENTER_RADIUS -> EQUALS
      angle >= -PI * 1 / 8 && angle <= PI / 8 -> MULTIPLY
      angle >= PI / 8 && angle <= PI * 3 / 8 -> DECIMAL
      angle >= PI * 3 / 8 && angle <= PI * 5 / 8 -> ADD
      angle >= PI * 5 / 8 && angle <= PI * 7 / 8 -> PERCENT
      angle >= PI * 7 / 8 || angle <= -PI * 7 / 8 -> DIVIDE
      angle >= -PI * 7 / 8 && angle <= -PI * 5 / 8 -> BACKSPACE
      angle >= -PI * 3 / 8 && angle <= -PI * 1 / 8 -> POWER
      else -> SUBTRACT
    }
  }
}
