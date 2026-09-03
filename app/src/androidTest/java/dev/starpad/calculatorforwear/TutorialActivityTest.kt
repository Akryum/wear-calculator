package dev.starpad.calculatorforwear

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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
  fun carouselSelectsPageUpdatesDotsAndPreservesCalculatorInput() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val carousel = activity.findViewById<RecyclerView>(R.id.tutorial_carousel)
        val indicator = activity.findViewById<LinearLayout>(R.id.tutorial_page_indicator)

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.tutorial_container).visibility)
        assertEquals(TutorialStep.entries.size, indicator.childCount)
        assertTrue(indicator.getChildAt(TutorialStep.TAP_NUMBER.ordinal).isSelected)
        assertTrue(carousel.canScrollHorizontally(1))
        carousel.smoothScrollToPosition(TutorialStep.CLEAR_INPUT.ordinal)
      }
      waitForCarousel()
      scenario.onActivity { activity ->
        val indicator = activity.findViewById<LinearLayout>(R.id.tutorial_page_indicator)
        val page = pageFor(activity, TutorialStep.CLEAR_INPUT)

        assertEquals(
          activity.getString(R.string.tutorial_clear_input_title),
          page.findViewById<TextView>(R.id.tutorial_title).text,
        )
        assertTrue(indicator.getChildAt(TutorialStep.CLEAR_INPUT.ordinal).isSelected)
        assertEquals(
          activity.getString(
            R.string.tutorial_page_description,
            TutorialStep.CLEAR_INPUT.ordinal + 1,
            TutorialStep.entries.size,
          ),
          indicator.contentDescription,
        )
        assertTrue(activity.findViewById<TextView>(R.id.txt_input).text.isEmpty())
      }
    }

    assertEquals(
      TutorialStep.CLEAR_INPUT,
      TutorialProgressStore(ApplicationProvider.getApplicationContext()).currentStep(),
    )
  }

  @Test
  fun carouselRestoreShowsPersistedPageAfterActivityRecreation() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.findViewById<RecyclerView>(R.id.tutorial_carousel)
          .smoothScrollToPosition(TutorialStep.DRAG_ACTION.ordinal)
      }
      waitForCarousel()
    }

    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      waitForCarousel()
      scenario.onActivity { activity ->
        val page = pageFor(activity, TutorialStep.DRAG_ACTION)

        assertEquals(
          activity.getString(R.string.tutorial_drag_action_title),
          page.findViewById<TextView>(R.id.tutorial_title).text,
        )
      }
    }
  }

  @Test
  fun skipAndDoneHideTutorialAndMarkComplete() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        val firstPage = pageFor(activity, TutorialStep.TAP_NUMBER)
        val action = firstPage.findViewById<MaterialButton>(R.id.tutorial_action_button)

        assertEquals(activity.getString(R.string.tutorial_skip), action.text)
        action.performClick()
        assertEquals(View.GONE, activity.findViewById<View>(R.id.tutorial_container).visibility)
      }
    }
    assertTrue(TutorialProgressStore(ApplicationProvider.getApplicationContext()).isComplete())

    preferences().edit().clear().commit()
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        activity.findViewById<RecyclerView>(R.id.tutorial_carousel)
          .smoothScrollToPosition(TutorialStep.SCROLL_HISTORY.ordinal)
      }
      waitForCarousel()
      scenario.onActivity { activity ->
        val finalPage = pageFor(activity, TutorialStep.SCROLL_HISTORY)
        val action = finalPage.findViewById<MaterialButton>(R.id.tutorial_action_button)

        assertEquals(activity.getString(R.string.tutorial_done), action.text)
        action.performClick()
        assertEquals(View.GONE, activity.findViewById<View>(R.id.tutorial_container).visibility)
      }
    }
    assertTrue(TutorialProgressStore(ApplicationProvider.getApplicationContext()).isComplete())
  }

  private fun pageFor(activity: MainActivity, step: TutorialStep): View {
    val carousel = activity.findViewById<RecyclerView>(R.id.tutorial_carousel)
    return requireNotNull(carousel.findViewHolderForAdapterPosition(step.ordinal)).itemView
  }

  private fun waitForCarousel() {
    SystemClock.sleep(CAROUSEL_SETTLE_WAIT_MS)
  }

  private fun preferences() = ApplicationProvider.getApplicationContext<Context>()
    .getSharedPreferences(TutorialProgressStore.PREFERENCES_NAME, Context.MODE_PRIVATE)

  private companion object {
    const val CAROUSEL_SETTLE_WAIT_MS = 700L
  }
}
