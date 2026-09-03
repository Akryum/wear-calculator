package dev.starpad.calculatorforwear.haptics

import android.os.Build
import android.view.View
import dev.starpad.calculatorforwear.CalculatorSettingsStore

/**
 * Plays calculator cues, enforcing the user's touch feedback preference in a single place.
 *
 * Every input is a parameter rather than something read from the platform, so the gating rule and
 * the per-SDK effect mapping can both be exercised without a watch.
 *
 * @param enabled Whether the user wants touch feedback; read at play time so a preference changed
 *   in the settings screen applies without recreating anything.
 * @param performer Sends one effect to the platform, reporting whether it produced anything.
 * @param sdkInt Platform the effects are chosen for.
 */
class CalculatorHaptics(
  private val enabled: () -> Boolean,
  private val performer: (Int) -> Boolean,
  private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
  /**
   * Plays [cue] unless the user turned feedback off.
   *
   * Watches vary in which effects they actually implement, so a refused effect is retried once with
   * the cue's fallback instead of leaving the interaction silent.
   *
   * @return whether the watch produced feedback.
   */
  fun play(cue: HapticCue): Boolean {
    if (!enabled()) return false
    if (performer(cue.constantFor(sdkInt))) return true
    val fallback = cue.fallbackFor(sdkInt) ?: return false
    return performer(fallback)
  }
}

/**
 * Cues played through [view], gated by the preference stored in [settings].
 *
 * Feedback bubbles up to the window, so a single view anchors the cues for a whole screen.
 */
fun CalculatorHaptics(view: View, settings: CalculatorSettingsStore) = CalculatorHaptics(
  enabled = settings::hapticsEnabled,
  // Explicit lambda: View has both a one and a two argument overload of this method.
  performer = { constant -> view.performHapticFeedback(constant) },
)
