package dev.starpad.calculatorforwear

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.starpad.calculatorforwear.databinding.ActivitySettingsBinding

/** Lets users control calculator preferences without leaving the app. */
class SettingsActivity : AppCompatActivity() {
  private lateinit var binding: ActivitySettingsBinding
  private lateinit var settingsStore: CalculatorSettingsStore
  private lateinit var historyStore: CalculationHistoryStore

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySettingsBinding.inflate(layoutInflater)
    setContentView(binding.root)

    settingsStore = CalculatorSettingsStore(this)
    historyStore = CalculationHistoryStore(this)
    binding.hapticsSwitch.isChecked = settingsStore.hapticsEnabled()
    binding.hapticsSwitch.setOnCheckedChangeListener { _, enabled -> settingsStore.setHapticsEnabled(enabled) }
    binding.showTutorialButton.setOnClickListener {
      finish()
      TutorialReplay.request(this)
    }
    binding.clearHistoryButton.setOnClickListener { confirmClearHistory() }
  }

  private fun confirmClearHistory() {
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.clear_history_title)
      .setMessage(R.string.clear_history_message)
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton(R.string.clear_history_confirm) { _, _ -> historyStore.clear() }
      .show()
  }
}
