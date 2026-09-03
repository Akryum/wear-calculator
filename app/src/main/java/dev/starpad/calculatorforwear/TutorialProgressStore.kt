package dev.starpad.calculatorforwear

import android.content.Context
import androidx.core.content.edit

/** Steps presented by the first-run gesture tutorial. */
enum class TutorialStep {
  TAP_NUMBER,
  DRAG_ACTION,
  CLEAR_INPUT,
  SCROLL_HISTORY;

  /** Returns following tutorial step, or null after final step. */
  fun next(): TutorialStep? = entries.getOrNull(ordinal + 1)
}

/** Persistent first-run tutorial state. */
class TutorialProgressStore(context: Context) {
  private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  /** True after user chooses Skip or Done. */
  fun isComplete(): Boolean = preferences.getBoolean(KEY_COMPLETE, false)

  /** Last unfinished step, safely falling back when stored data is invalid. */
  fun currentStep(): TutorialStep {
    val index = preferences.getInt(KEY_STEP, TutorialStep.TAP_NUMBER.ordinal)
    return TutorialStep.entries.getOrElse(index) { TutorialStep.TAP_NUMBER }
  }

  /** Saves progress after a scene has been reached. */
  fun saveStep(step: TutorialStep) {
    preferences.edit { putInt(KEY_STEP, step.ordinal) }
  }

  /** Makes tutorial permanently hidden for current app data. */
  fun markComplete() {
    preferences.edit {
      putBoolean(KEY_COMPLETE, true)
      remove(KEY_STEP)
    }
  }

  companion object {
    const val PREFERENCES_NAME = "tutorial_progress"
    private const val KEY_COMPLETE = "complete"
    private const val KEY_STEP = "step"
  }
}
