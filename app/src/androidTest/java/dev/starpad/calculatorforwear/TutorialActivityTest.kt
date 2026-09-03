package dev.starpad.calculatorforwear

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.starpad.calculatorforwear.tutorial.TUTORIAL_PAGER_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TutorialActivityTest {
  // Empty rule: each test launches the activity itself, after the stored progress is set up.
  @get:Rule
  val compose = createEmptyComposeRule()

  @Before
  fun clearTutorialProgress() {
    preferences().edit().clear().commit()
  }

  @After
  fun clearProgressAfterTest() {
    preferences().edit().clear().commit()
    TutorialReplay.consume(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun unfinishedTutorialRestoresCurrentScene() {
    val store = TutorialProgressStore(ApplicationProvider.getApplicationContext())

    assertFalse(store.isComplete())
    assertEquals(TutorialStep.TAP_NUMBER, store.currentStep())

    store.saveStep(TutorialStep.DRAG_ACTION)

    assertEquals(TutorialStep.DRAG_ACTION, TutorialProgressStore(
      ApplicationProvider.getApplicationContext(),
    ).currentStep())
  }

  @Test
  fun swipingSelectsPageSavesProgressAndPreservesCalculatorInput() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.tutorial_container).visibility)
      }
      compose.onNodeWithText(string(R.string.tutorial_tap_number_title)).assertIsDisplayed()

      swipeToNextPage()
      swipeToNextPage()

      compose.onNodeWithText(string(R.string.tutorial_clear_input_title)).assertIsDisplayed()
      scenario.onActivity { activity ->
        assertTrue(activity.findViewById<TextView>(R.id.txt_input).text.isEmpty())
      }
    }

    assertEquals(
      TutorialStep.CLEAR_INPUT,
      TutorialProgressStore(ApplicationProvider.getApplicationContext()).currentStep(),
    )
  }

  @Test
  fun tutorialReopensOnPersistedPageAfterActivityRecreation() {
    ActivityScenario.launch(MainActivity::class.java).use {
      swipeToNextPage()
    }

    ActivityScenario.launch(MainActivity::class.java).use {
      compose.onNodeWithText(string(R.string.tutorial_drag_action_title)).assertIsDisplayed()
    }
  }

  @Test
  fun skipAndDoneHideTutorialAndMarkComplete() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      compose.onNodeWithText(string(R.string.tutorial_skip)).performClick()
      compose.waitForIdle()
      scenario.onActivity { activity ->
        assertEquals(View.GONE, activity.findViewById<View>(R.id.tutorial_container).visibility)
      }
    }
    assertTrue(TutorialProgressStore(ApplicationProvider.getApplicationContext()).isComplete())

    preferences().edit().clear().commit()
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      repeat(TutorialStep.entries.size - 1) { swipeToNextPage() }
      compose.onNodeWithText(string(R.string.tutorial_done)).performClick()
      compose.waitForIdle()
      scenario.onActivity { activity ->
        assertEquals(View.GONE, activity.findViewById<View>(R.id.tutorial_container).visibility)
      }
    }
    assertTrue(TutorialProgressStore(ApplicationProvider.getApplicationContext()).isComplete())
  }

  @Test
  fun replayRequestedFromSettingsReplaysTutorialExactlyOnce() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    TutorialProgressStore(context).markComplete()
    // Settings only stores the request and finishes, so the calculator picks it up when it resumes.
    TutorialReplay.request(context)

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.tutorial_container).visibility)
      }
      compose.onNodeWithText(string(R.string.tutorial_tap_number_title)).assertIsDisplayed()
    }

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assertEquals(View.GONE, activity.findViewById<View>(R.id.tutorial_container).visibility)
      }
    }
  }

  @Test
  fun rotaryTurnSnapsToNextPageAndSavesProgress() {
    ActivityScenario.launch(MainActivity::class.java).use {
      compose.onNodeWithTag(TUTORIAL_PAGER_TAG).performRotaryScrollInput {
        rotateToScrollVertically(ROTARY_DETENT_PX)
      }
      compose.waitForIdle()

      compose.onNodeWithText(string(R.string.tutorial_drag_action_title)).assertIsDisplayed()
    }

    assertEquals(
      TutorialStep.DRAG_ACTION,
      TutorialProgressStore(ApplicationProvider.getApplicationContext()).currentStep(),
    )
  }

  private fun swipeToNextPage() {
    compose.onNodeWithTag(TUTORIAL_PAGER_TAG).performTouchInput { swipeLeft() }
    compose.waitForIdle()
  }

  private fun string(@StringRes id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

  private fun preferences() = ApplicationProvider.getApplicationContext<Context>()
    .getSharedPreferences(TutorialProgressStore.PREFERENCES_NAME, Context.MODE_PRIVATE)

  private companion object {
    /** About one bezel detent, enough for the pager's snap behaviour to turn one page. */
    const val ROTARY_DETENT_PX = 200f
  }
}
