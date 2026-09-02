package dev.starpad.calculatorforwear

import android.view.View
import android.widget.TextView
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.button.MaterialButton
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TutorialActivityTest {
  @Before
  fun clearTutorialProgress() {
    preferences().edit().clear().commit()
  }

  @After
  fun clearProgressAfterTest() {
    preferences().edit().clear().commit()
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
  fun tutorialConsumesSceneTapsUntilDone() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val overlay = activity.findViewById<View>(R.id.tutorial_container)
        val input = activity.findViewById<TextView>(R.id.txt_input)
        val action = activity.findViewById<MaterialButton>(R.id.tutorial_action_button)

        assertEquals(View.VISIBLE, overlay.visibility)
        assertEquals(activity.getString(R.string.tutorial_skip), action.text)
        overlay.performClick()
      }
      SystemClock.sleep(350)
      scenario.onActivity { activity ->
        activity.findViewById<View>(R.id.tutorial_container).performClick()
      }
      SystemClock.sleep(350)
      scenario.onActivity { activity ->
        val overlay = activity.findViewById<View>(R.id.tutorial_container)
        val input = activity.findViewById<TextView>(R.id.txt_input)
        val action = activity.findViewById<MaterialButton>(R.id.tutorial_action_button)

        assertEquals(activity.getString(R.string.tutorial_done), action.text)
        assertTrue(input.text.isEmpty())

        action.performClick()

        assertEquals(View.GONE, overlay.visibility)
      }
    }

    assertTrue(TutorialProgressStore(ApplicationProvider.getApplicationContext()).isComplete())
  }

  @Test
  fun skipMarksTutorialComplete() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.findViewById<MaterialButton>(R.id.tutorial_action_button).performClick()
        assertEquals(View.GONE, activity.findViewById<View>(R.id.tutorial_container).visibility)
      }
    }

    assertTrue(TutorialProgressStore(ApplicationProvider.getApplicationContext()).isComplete())

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assertEquals(View.GONE, activity.findViewById<View>(R.id.tutorial_container).visibility)
      }
    }
  }

  private fun preferences() = ApplicationProvider.getApplicationContext<android.content.Context>()
    .getSharedPreferences(TutorialProgressStore.PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
}
