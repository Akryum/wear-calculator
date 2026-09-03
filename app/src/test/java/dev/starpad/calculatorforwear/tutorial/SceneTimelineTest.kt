package dev.starpad.calculatorforwear.tutorial

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks in the keyframe math every tutorial scene is paced with: a window that leaked outside its
 * bounds would make a finger move during the hold that is meant to teach a long press.
 */
class SceneTimelineTest {
  private val tolerance = 1e-6f

  @Test
  fun `segment clamps outside its window and is linear inside`() {
    assertEquals(0f, SceneTimeline.segment(0.1f, 0.2f, 0.6f), tolerance)
    assertEquals(0.5f, SceneTimeline.segment(0.4f, 0.2f, 0.6f), tolerance)
    assertEquals(1f, SceneTimeline.segment(0.9f, 0.2f, 0.6f), tolerance)
  }

  @Test
  fun `easings keep their endpoints`() {
    assertEquals(0f, SceneTimeline.easeInOut(0f), tolerance)
    assertEquals(0.5f, SceneTimeline.easeInOut(0.5f), tolerance)
    assertEquals(1f, SceneTimeline.easeInOut(1f), tolerance)
    assertEquals(0f, SceneTimeline.easeOut(0f), tolerance)
    assertEquals(1f, SceneTimeline.easeOut(1f), tolerance)
  }

  @Test
  fun `loop fade hides both ends of the loop`() {
    assertEquals(0f, SceneTimeline.loopFade(0f), tolerance)
    assertEquals(0.5f, SceneTimeline.loopFade(0.04f, edge = 0.08f), tolerance)
    assertEquals(1f, SceneTimeline.loopFade(0.5f), tolerance)
    assertEquals(0f, SceneTimeline.loopFade(1f), tolerance)
  }
}
