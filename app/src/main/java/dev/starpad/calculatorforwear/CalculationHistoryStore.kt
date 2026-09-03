package dev.starpad.calculatorforwear

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** A completed calculation shown in calculator history. */
data class CalculationHistoryEntry(
  val expression: String,
  val result: String,
)

/** Persists a bounded, newest-first calculation history on this watch. */
class CalculationHistoryStore(context: Context) {
  private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  /** Returns saved entries, ignoring corrupted entries instead of breaking calculator startup. */
  fun load(): List<CalculationHistoryEntry> = runCatching {
    val values = JSONArray(preferences.getString(KEY_HISTORY, "[]"))
    buildList {
      for (index in 0 until values.length()) {
        val value = values.optJSONObject(index) ?: continue
        val expression = value.optString(KEY_EXPRESSION)
        val result = value.optString(KEY_RESULT)
        if (expression.isNotBlank() && result.isNotBlank()) add(CalculationHistoryEntry(expression, result))
      }
    }.take(MAX_ENTRIES)
  }.getOrDefault(emptyList())

  /** Adds completed calculation and removes entries older than [MAX_ENTRIES]. */
  fun append(expression: String, result: String) {
    val entries = listOf(CalculationHistoryEntry(expression, result)) + load()
    save(entries.take(MAX_ENTRIES))
  }

  /** Removes every saved calculation. */
  fun clear() {
    preferences.edit { remove(KEY_HISTORY) }
  }

  private fun save(entries: List<CalculationHistoryEntry>) {
    val values = JSONArray()
    entries.forEach { entry ->
      values.put(JSONObject().apply {
        put(KEY_EXPRESSION, entry.expression)
        put(KEY_RESULT, entry.result)
      })
    }
    preferences.edit { putString(KEY_HISTORY, values.toString()) }
  }

  companion object {
    const val MAX_ENTRIES = 15
    private const val PREFERENCES_NAME = "calculation_history"
    private const val KEY_HISTORY = "entries"
    private const val KEY_EXPRESSION = "expression"
    private const val KEY_RESULT = "result"
  }
}
