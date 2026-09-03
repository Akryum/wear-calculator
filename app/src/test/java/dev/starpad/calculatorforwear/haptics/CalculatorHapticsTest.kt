package dev.starpad.calculatorforwear.haptics

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorHapticsTest {
  private val played = mutableListOf<Int>()

  private fun haptics(
    enabled: Boolean = true,
    sdkInt: Int = HapticCue.MODERN_SDK,
    accepts: (Int) -> Boolean = { true },
  ) = CalculatorHaptics(
    enabled = { enabled },
    performer = { constant -> played += constant; accepts(constant) },
    sdkInt = sdkInt,
  )

  @Test
  fun disabledPreferenceSuppressesEveryCue() {
    val haptics = haptics(enabled = false)

    HapticCue.entries.forEach { assertFalse(it.name, haptics.play(it)) }

    assertEquals(emptyList<Int>(), played)
  }

  @Test
  fun legacyWatchesOnlyReceiveEffectsTheyUnderstand() {
    val haptics = haptics(sdkInt = HapticCue.MODERN_SDK - 1)

    HapticCue.entries.forEach { haptics.play(it) }

    assertEquals(
      listOf(
        HapticFeedbackConstants.CLOCK_TICK,
        HapticFeedbackConstants.CLOCK_TICK,
        HapticFeedbackConstants.LONG_PRESS,
        HapticFeedbackConstants.GESTURE_START,
        HapticFeedbackConstants.CONFIRM,
        HapticFeedbackConstants.REJECT,
      ),
      played,
    )
  }

  @Test
  fun recentWatchesReceiveTheRicherEffects() {
    val haptics = haptics()

    haptics.play(HapticCue.Selection)
    haptics.play(HapticCue.Threshold)

    assertEquals(
      listOf(
        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK,
        HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE,
      ),
      played,
    )
  }

  @Test
  fun refusedEffectFallsBackOnce() {
    val haptics = haptics(accepts = { it != HapticFeedbackConstants.SEGMENT_FREQUENT_TICK })

    assertTrue(haptics.play(HapticCue.Selection))

    assertEquals(
      listOf(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK, HapticFeedbackConstants.CLOCK_TICK),
      played,
    )
  }

  @Test
  fun refusedEffectWithoutFallbackIsNotRetried() {
    val haptics = haptics(accepts = { false })

    assertFalse(haptics.play(HapticCue.Confirm))

    assertEquals(listOf(HapticFeedbackConstants.CONFIRM), played)
  }
}
