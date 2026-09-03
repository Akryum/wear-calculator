package dev.starpad.calculatorforwear.settings

import android.content.Context
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.starpad.calculatorforwear.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies settings actions reach the hosting activity and destructive actions ask first. */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
  @get:Rule
  val composeRule = createComposeRule()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val hapticsChanges = mutableListOf<Boolean>()
  private var tutorialRequests = 0
  private var historyClears = 0

  @Test
  fun hapticsSwitchReflectsAndReportsState() {
    showSettings(hapticsEnabled = true)

    switchNode().assertIsOn().performClick()

    assertEquals(listOf(false), hapticsChanges)
  }

  @Test
  fun disabledHapticsRenderUncheckedSwitch() {
    showSettings(hapticsEnabled = false)

    switchNode().assertIsOff()
  }

  @Test
  fun tutorialButtonRequestsReplay() {
    showSettings()

    composeRule.onNodeWithText(string(R.string.show_tutorial)).performClick()

    assertEquals(1, tutorialRequests)
  }

  @Test
  fun clearHistoryOnlyClearsAfterConfirmation() {
    showSettings()

    composeRule.onNodeWithText(string(R.string.clear_history)).performClick()
    composeRule.onNodeWithText(string(R.string.clear_history_title)).assertExists()
    assertEquals(0, historyClears)

    composeRule.onNodeWithText(string(R.string.clear_history_confirm)).performClick()

    assertEquals(1, historyClears)
    composeRule.onNodeWithText(string(R.string.clear_history_title)).assertDoesNotExist()
  }

  @Test
  fun cancellingConfirmationKeepsHistory() {
    showSettings()

    composeRule.onNodeWithText(string(R.string.clear_history)).performClick()
    composeRule.onNodeWithText(string(android.R.string.cancel)).performClick()

    assertEquals(0, historyClears)
    composeRule.onNodeWithText(string(R.string.clear_history_title)).assertDoesNotExist()
  }

  private fun showSettings(hapticsEnabled: Boolean = true) {
    composeRule.setContent {
      SettingsTheme {
        SettingsScreen(
          hapticsEnabled = hapticsEnabled,
          onHapticsEnabledChange = { hapticsChanges.add(it) },
          onShowTutorial = { tutorialRequests++ },
          onClearHistory = { historyClears++ },
        )
      }
    }
  }

  private fun switchNode() = composeRule.onNodeWithText(string(R.string.haptics_enabled))

  private fun string(id: Int) = context.getString(id)
}
