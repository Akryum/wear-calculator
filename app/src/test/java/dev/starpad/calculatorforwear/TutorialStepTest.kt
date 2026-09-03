package dev.starpad.calculatorforwear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TutorialStepTest {
  @Test
  fun advancesThroughEachTutorialStep() {
    assertEquals(TutorialStep.DRAG_ACTION, TutorialStep.TAP_NUMBER.next())
    assertEquals(TutorialStep.CLEAR_INPUT, TutorialStep.DRAG_ACTION.next())
    assertEquals(TutorialStep.SCROLL_HISTORY, TutorialStep.CLEAR_INPUT.next())
    assertNull(TutorialStep.SCROLL_HISTORY.next())
  }
}
