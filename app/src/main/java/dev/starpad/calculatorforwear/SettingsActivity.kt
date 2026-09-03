package dev.starpad.calculatorforwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.starpad.calculatorforwear.settings.SettingsScreen
import dev.starpad.calculatorforwear.settings.SettingsTheme

/** Lets users control calculator preferences without leaving the app. */
class SettingsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val settingsStore = CalculatorSettingsStore(this)
    val historyStore = CalculationHistoryStore(this)

    setContent {
      var hapticsEnabled by rememberSaveable { mutableStateOf(settingsStore.hapticsEnabled()) }

      SettingsTheme {
        SettingsScreen(
          hapticsEnabled = hapticsEnabled,
          onHapticsEnabledChange = { enabled ->
            hapticsEnabled = enabled
            settingsStore.setHapticsEnabled(enabled)
          },
          onShowTutorial = {
            TutorialReplay.request(this)
            finish()
          },
          onClearHistory = historyStore::clear,
        )
      }
    }
  }
}
