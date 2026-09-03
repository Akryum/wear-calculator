package dev.starpad.calculatorforwear.menu

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

/**
 * Locks in the angle/distance sectors of the radial menu: the mapping is pure geometry with no
 * feedback of its own, so a wrong boundary would silently commit the wrong operation.
 */
class RadialChoiceTest {
  /** Any distance comfortably outside the dead zone. */
  private val far = 100f

  /** Small enough to stay inside a sector, large enough to swamp float rounding. */
  private val epsilon = 0.01

  private fun at(distance: Float, angle: Double) = RadialChoice.at(distance, angle.toFloat())

  @Test
  fun `dead zone selects equals whatever the angle`() {
    assertEquals(RadialChoice.EQUALS, at(0f, 0.0))
    assertEquals(RadialChoice.EQUALS, at(35f, PI / 2))
    assertEquals(RadialChoice.EQUALS, at(RadialChoice.CENTER_RADIUS, -PI))
  }

  @Test
  fun `just outside the dead zone the angle decides`() {
    assertEquals(RadialChoice.ADD, at(RadialChoice.CENTER_RADIUS + 1f, PI / 2))
  }

  // Screen coordinates: y grows downwards, so positive angles point towards the bottom of the
  // watch and the sectors run clockwise from multiply at 3 o'clock.
  @Test
  fun `each sector centre selects its own choice`() {
    assertEquals(RadialChoice.MULTIPLY, at(far, 0.0))
    assertEquals(RadialChoice.DECIMAL, at(far, PI / 4))
    assertEquals(RadialChoice.ADD, at(far, PI / 2))
    assertEquals(RadialChoice.PERCENT, at(far, PI * 3 / 4))
    assertEquals(RadialChoice.DIVIDE, at(far, PI))
    assertEquals(RadialChoice.BACKSPACE, at(far, -PI * 3 / 4))
    assertEquals(RadialChoice.SUBTRACT, at(far, -PI / 2))
    assertEquals(RadialChoice.POWER, at(far, -PI / 4))
  }

  @Test
  fun `divide spans the wrap around at plus and minus pi`() {
    assertEquals(RadialChoice.DIVIDE, at(far, PI * 15 / 16))
    assertEquals(RadialChoice.DIVIDE, at(far, -PI * 15 / 16))
  }

  /**
   * Every sector boundary, checked from both sides: no gap that would fall through to subtract
   * and no overlap that would shadow a neighbour. The exact boundary angle itself is left alone,
   * because which side wins there is decided by float rounding rather than by the sector layout.
   */
  @Test
  fun `neighbouring sectors meet at every boundary`() {
    val boundaries = listOf(
      Triple(-PI * 7 / 8, RadialChoice.DIVIDE, RadialChoice.BACKSPACE),
      Triple(-PI * 5 / 8, RadialChoice.BACKSPACE, RadialChoice.SUBTRACT),
      Triple(-PI * 3 / 8, RadialChoice.SUBTRACT, RadialChoice.POWER),
      Triple(-PI / 8, RadialChoice.POWER, RadialChoice.MULTIPLY),
      Triple(PI / 8, RadialChoice.MULTIPLY, RadialChoice.DECIMAL),
      Triple(PI * 3 / 8, RadialChoice.DECIMAL, RadialChoice.ADD),
      Triple(PI * 5 / 8, RadialChoice.ADD, RadialChoice.PERCENT),
      Triple(PI * 7 / 8, RadialChoice.PERCENT, RadialChoice.DIVIDE),
    )
    boundaries.forEach { (angle, before, after) ->
      assertEquals("below $angle", before, at(far, angle - epsilon))
      assertEquals("above $angle", after, at(far, angle + epsilon))
    }
  }

  @Test
  fun `every choice carries a distinct symbol view and label`() {
    val choices = RadialChoice.entries
    assertEquals(choices.size, choices.map { it.symbol }.toSet().size)
    assertEquals(choices.size, choices.map { it.viewId }.toSet().size)
    assertEquals(choices.size, choices.map { it.labelRes }.toSet().size)
  }
}
