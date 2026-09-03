package dev.starpad.calculatorforwear.tutorial

/** Pure keyframe math shared by the tutorial scenes, so every scene paces itself the same way. */
internal object SceneTimeline {
  /**
   * Progress of one keyframe window inside the loop: 0 before [start], 1 after [end] and linear
   * in between. Scenes chain these windows to sequence approach, press, drag and release.
   */
  fun segment(phase: Float, start: Float, end: Float): Float =
    ((phase - start) / (end - start)).coerceIn(0f, 1f)

  /** Smooth start and stop, for a finger travelling between two points. */
  fun easeInOut(t: Float): Float = t * t * (3f - 2f * t)

  /** Fast start that settles gently, for presses and highlights. */
  fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)

  /** 0 at both ends of the loop and 1 in between, so restarting the loop never pops. */
  fun loopFade(phase: Float, edge: Float = 0.08f): Float =
    minOf(segment(phase, 0f, edge), 1f - segment(phase, 1f - edge, 1f))

  /** Linear interpolation from [from] to [to]. */
  fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
}
