package dev.starpad.calculatorforwear

import android.content.Context
import androidx.core.content.edit

/** Requests a tutorial replay, read back by the calculator when Settings finishes. */
object TutorialReplay {
  private const val PREFERENCES_NAME = "tutorial_replay"
  private const val KEY_REQUESTED = "requested"

  /**
   * Marks one replay request. The caller finishes Settings right after, which resumes the
   * calculator underneath it, so no intent is needed to bring that activity back.
   */
  fun request(context: Context) {
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .edit { putBoolean(KEY_REQUESTED, true) }
  }

  /** Consumes pending replay request exactly once. */
  fun consume(context: Context): Boolean {
    val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    if (!preferences.getBoolean(KEY_REQUESTED, false)) return false
    preferences.edit { remove(KEY_REQUESTED) }
    return true
  }
}
