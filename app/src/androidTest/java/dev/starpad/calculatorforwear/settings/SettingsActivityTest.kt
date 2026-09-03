package dev.starpad.calculatorforwear.settings

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.starpad.calculatorforwear.CalculatorSettingsStore
import dev.starpad.calculatorforwear.R
import dev.starpad.calculatorforwear.SettingsActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the activity persists what the settings screen reports. */
@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {
  @get:Rule
  val composeRule = createAndroidComposeRule<SettingsActivity>()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val settingsStore = CalculatorSettingsStore(context)

  @After
  fun restoreHaptics() {
    settingsStore.setHapticsEnabled(true)
  }

  @Test
  fun togglingHapticsPersistsPreference() {
    composeRule.onNodeWithText(context.getString(R.string.haptics_enabled)).performClick()

    assertFalse(CalculatorSettingsStore(context).hapticsEnabled())
  }
}
