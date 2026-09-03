package dev.starpad.calculatorforwear

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalculationHistoryStoreTest {
  private lateinit var store: CalculationHistoryStore

  @Before
  fun setUp() {
    store = CalculationHistoryStore(ApplicationProvider.getApplicationContext())
    store.clear()
  }

  @After
  fun tearDown() {
    store.clear()
  }

  @Test
  fun persistsNewestFifteenEntries() {
    repeat(CalculationHistoryStore.MAX_ENTRIES + 1) { index ->
      store.append("$index+1", (index + 1).toString())
    }

    val reloaded = CalculationHistoryStore(ApplicationProvider.getApplicationContext()).load()

    assertEquals(CalculationHistoryStore.MAX_ENTRIES, reloaded.size)
    assertEquals(CalculationHistoryEntry("15+1", "16"), reloaded.first())
    assertEquals(CalculationHistoryEntry("1+1", "2"), reloaded.last())
  }

  @Test
  fun clearRemovesSavedEntries() {
    store.append("2+2", "4")
    store.clear()

    assertTrue(store.load().isEmpty())
  }
}
