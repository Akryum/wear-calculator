package dev.starpad.calculatorforwear.haptics

import android.annotation.SuppressLint
import android.os.Build
import android.view.HapticFeedbackConstants

/**
 * Touch feedback vocabulary shared by every calculator interaction.
 *
 * Each cue names the effect that expresses it best, plus one that every supported watch
 * understands: effects introduced after [MODERN_SDK] are not rejected by older platforms, they are
 * silently ignored, so without the fallback those watches would simply go quiet.
 */
@SuppressLint("InlinedApi") // The API 34 effects inline as plain ints; constantFor picks per SDK.
enum class HapticCue(private val modern: Int, private val legacy: Int) {
  /** One character was added or removed. */
  Tick(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.CLOCK_TICK),

  /** The highlighted radial choice changed while the finger is still down. */
  Selection(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK, HapticFeedbackConstants.CLOCK_TICK),

  /** Holding still has armed a larger action than a plain tap would trigger. */
  Threshold(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE, HapticFeedbackConstants.LONG_PRESS),

  /** A gesture surface opened under the finger. */
  GestureStart(HapticFeedbackConstants.GESTURE_START, HapticFeedbackConstants.GESTURE_START),

  /** An action committed and changed what the screen shows. */
  Confirm(HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.CONFIRM),

  /** The action was refused: nothing to delete, or an expression that does not evaluate. */
  Reject(HapticFeedbackConstants.REJECT, HapticFeedbackConstants.REJECT);

  /** Effect carrying this cue on [sdkInt]. */
  fun constantFor(sdkInt: Int): Int = if (sdkInt >= MODERN_SDK) modern else legacy

  /** Effect to retry when the watch refuses [constantFor], or null when nothing else is left. */
  fun fallbackFor(sdkInt: Int): Int? = legacy.takeIf { it != constantFor(sdkInt) }

  companion object {
    /** First platform providing the richer effects; below it every cue uses its fallback. */
    const val MODERN_SDK = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
  }
}
