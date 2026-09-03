package dev.starpad.calculatorforwear

import android.content.Context
import android.content.Intent

/** Requests a tutorial replay after Settings returns to calculator activity. */
object TutorialReplay {
  private const val PREFERENCES_NAME = "tutorial_replay"
  private const val KEY_REQUESTED = "requested"

  /** Marks one replay request and returns to calculator task. */
  fun request(context: Context) {
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .edit().putBoolean(KEY_REQUESTED, true).apply()
    context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
  }

  /** Consumes pending replay request exactly once. */
  fun consume(context: Context): Boolean {
    val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    if (!preferences.getBoolean(KEY_REQUESTED, false)) return false
    preferences.edit().remove(KEY_REQUESTED).apply()
    return true
  }
}
