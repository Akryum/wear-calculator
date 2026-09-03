package dev.starpad.calculatorforwear

import android.content.Context
import androidx.core.content.edit

/** Stores user-controlled calculator behavior. */
class CalculatorSettingsStore(context: Context) {
  private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  /** Whether accepted calculator actions should provide touch feedback. */
  fun hapticsEnabled(): Boolean = preferences.getBoolean(KEY_HAPTICS_ENABLED, true)

  /** Updates touch feedback preference. */
  fun setHapticsEnabled(enabled: Boolean) {
    preferences.edit { putBoolean(KEY_HAPTICS_ENABLED, enabled) }
  }

  companion object {
    private const val PREFERENCES_NAME = "calculator_settings"
    private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
  }
}
