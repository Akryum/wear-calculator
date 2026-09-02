package dev.starpad.calculatorforwear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TutorialStepTest {
  @Test
  fun advancesThroughEachTutorialStep() {
    assertEquals(TutorialStep.DRAG_ACTION, TutorialStep.TAP_NUMBER.next())
    assertEquals(TutorialStep.CLEAR_INPUT, TutorialStep.DRAG_ACTION.next())
    assertNull(TutorialStep.CLEAR_INPUT.next())
  }
}
