package dev.starpad.calculatorforwear

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isEmpty
import dev.starpad.calculatorforwear.haptics.CalculatorHaptics
import dev.starpad.calculatorforwear.haptics.HapticCue
import dev.starpad.calculatorforwear.tutorial.TutorialCarousel

/**
 * Owns the tutorial overlay without coupling it to calculator input state: decides when it shows,
 * hosts the pager and records how far the user got.
 */
class TutorialController(
  context: Context,
  private val container: FrameLayout,
  private val haptics: CalculatorHaptics,
) {
  private val progressStore = TutorialProgressStore(context)
  private val composeView = ComposeView(context)

  /** The showing of the tutorial in progress, or null while it is hidden. */
  private var session: Session? by mutableStateOf(null)
  private var step = TutorialStep.TAP_NUMBER

  /** One showing of the tutorial; a new instance composes a fresh pager at [initialStep]. */
  private class Session(val initialStep: TutorialStep, val replay: Boolean)

  /** Shows unfinished tutorial, if any. */
  fun showIfNeeded() {
    if (progressStore.isComplete()) {
      container.visibility = View.GONE
      return
    }
    show(progressStore.currentStep(), replay = false)
  }

  /** Shows tutorial from its first scene even after first-run tutorial was completed. */
  fun showReplay() = show(TutorialStep.TAP_NUMBER, replay = true)

  private fun show(initialStep: TutorialStep, replay: Boolean) {
    if (container.isEmpty()) {
      container.addView(composeView, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
      ))
      composeView.setContent {
        session?.let { session ->
          key(session) { TutorialCarousel(session.initialStep, ::onStepSettled, ::finishTutorial) }
        }
      }
    }
    step = initialStep
    session = Session(initialStep, replay)
    container.visibility = View.VISIBLE
  }

  private fun onStepSettled(settled: TutorialStep) {
    val session = session ?: return
    if (settled == step) return
    step = settled
    if (!session.replay) progressStore.saveStep(settled)
    haptics.play(HapticCue.Tick)
  }

  private fun finishTutorial() {
    progressStore.markComplete()
    session = null
    container.visibility = View.GONE
  }
}
