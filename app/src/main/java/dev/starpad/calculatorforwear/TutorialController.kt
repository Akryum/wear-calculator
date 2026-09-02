package dev.starpad.calculatorforwear

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton

/** Owns tutorial overlay presentation without coupling it to calculator input state. */
class TutorialController(
  private val context: Context,
  private val container: FrameLayout,
) {
  private val progressStore = TutorialProgressStore(context)
  private lateinit var illustration: TutorialIllustrationView
  private lateinit var progress: TextView
  private lateinit var title: TextView
  private lateinit var body: TextView
  private lateinit var action: MaterialButton
  private lateinit var copy: LinearLayout
  private var step = TutorialStep.TAP_NUMBER
  private var isTransitioning = false

  /** Shows unfinished tutorial, if any. */
  fun showIfNeeded() {
    if (progressStore.isComplete()) {
      container.visibility = View.GONE
      return
    }

    step = progressStore.currentStep()
    buildOverlay()
    renderStep()
    container.visibility = View.VISIBLE
    illustration.resumeAnimation()
  }

  /** Pauses decorative animation while activity is backgrounded. */
  fun onStop() {
    if (container.isVisible) illustration.pauseAnimation()
  }

  /** Resumes decorative animation when activity returns. */
  fun onStart() {
    if (container.isVisible) illustration.resumeAnimation()
  }

  private fun buildOverlay() {
    if (container.childCount > 0) return

    container.setBackgroundColor(Color.BLACK)
    container.isClickable = true
    container.isFocusable = true
    container.setOnClickListener { advance() }

    illustration = TutorialIllustrationView(context).apply {
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    container.addView(illustration, FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT,
    ))

    progress = textView(12f).apply {
      id = R.id.tutorial_progress
      gravity = Gravity.CENTER
    }
    container.addView(progress, frameParams(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 12))

    copy = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
      setPadding(dp(14))
    }
    title = textView(19f).apply {
      id = R.id.tutorial_title
      typeface = android.graphics.Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
    }
    body = textView(14f).apply {
      id = R.id.tutorial_body
      gravity = Gravity.CENTER
      maxLines = 2
      setPadding(0, dp(3), 0, dp(7))
    }
    copy.addView(title, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.WRAP_CONTENT,
    ))
    copy.addView(body, LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.WRAP_CONTENT,
    ))
    action = MaterialButton(context, null, R.attr.MaterialTextButton).apply {
      id = R.id.tutorial_action_button
      minHeight = dp(42)
      minWidth = dp(82)
      insetTop = 0
      insetBottom = 0
      setOnClickListener { finishTutorial() }
    }
    copy.addView(action)
    container.addView(copy, frameParams(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 2))
  }

  private fun renderStep() {
    val content = when (step) {
      TutorialStep.TAP_NUMBER -> TutorialContent(
        R.string.tutorial_tap_number_title,
        R.string.tutorial_tap_number_body,
      )
      TutorialStep.DRAG_ACTION -> TutorialContent(
        R.string.tutorial_drag_action_title,
        R.string.tutorial_drag_action_body,
      )
      TutorialStep.CLEAR_INPUT -> TutorialContent(
        R.string.tutorial_clear_input_title,
        R.string.tutorial_clear_input_body,
      )
    }
    progress.text = context.getString(R.string.tutorial_progress, step.ordinal + 1, TutorialStep.entries.size)
    title.setText(content.title)
    body.setText(content.body)
    val isFinalStep = step == TutorialStep.CLEAR_INPUT
    action.setText(if (isFinalStep) R.string.tutorial_done else R.string.tutorial_skip)
    action.contentDescription = context.getString(
      if (isFinalStep) R.string.tutorial_done_description else R.string.tutorial_skip_description,
    )
    container.contentDescription = if (isFinalStep) null else {
      context.getString(R.string.tutorial_next_description)
    }
    illustration.showStep(step)
  }

  private fun advance() {
    val next = step.next() ?: return
    if (isTransitioning) return

    isTransitioning = true
    action.isEnabled = false
    val distance = dp(STEP_TRANSITION_DISTANCE_DP).toFloat()

    progress.animate().alpha(0f).translationY(-distance).setDuration(STEP_EXIT_DURATION_MS).start()
    illustration.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).setDuration(STEP_EXIT_DURATION_MS).start()
    copy.animate()
      .alpha(0f)
      .translationY(-distance)
      .setDuration(STEP_EXIT_DURATION_MS)
      .withEndAction {
        step = next
        progressStore.saveStep(step)
        resetIncomingScene(distance)
        renderStep()
        animateIncomingScene()
      }
      .start()
  }

  /** Places new scene below its settled position before fade-and-lift entrance. */
  private fun resetIncomingScene(distance: Float) {
    progress.translationY = distance
    illustration.scaleX = 0.94f
    illustration.scaleY = 0.94f
    copy.translationY = distance
  }

  /** Reveals next scene after previous scene has fully left screen. */
  private fun animateIncomingScene() {
    progress.animate().alpha(1f).translationY(0f).setDuration(STEP_ENTER_DURATION_MS).start()
    illustration.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(STEP_ENTER_DURATION_MS).start()
    copy.animate()
      .alpha(1f)
      .translationY(0f)
      .setDuration(STEP_ENTER_DURATION_MS)
      .withEndAction {
        isTransitioning = false
        action.isEnabled = true
      }
      .start()
  }

  private fun finishTutorial() {
    progressStore.markComplete()
    illustration.pauseAnimation()
    container.visibility = View.GONE
  }

  private fun textView(size: Float): TextView = TextView(context).apply {
    setTextColor(Color.WHITE)
    textSize = size
    ViewCompat.setAccessibilityHeading(this, false)
  }

  private fun frameParams(gravity: Int, horizontalMargin: Int, verticalMargin: Int) = FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.WRAP_CONTENT,
    FrameLayout.LayoutParams.WRAP_CONTENT,
    gravity,
  ).apply {
    leftMargin = dp(horizontalMargin)
    rightMargin = dp(horizontalMargin)
    topMargin = dp(verticalMargin)
    bottomMargin = dp(verticalMargin)
  }

  private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

  /** String resources used by one tutorial scene. */
  private data class TutorialContent(val title: Int, val body: Int)

  private companion object {
    const val STEP_EXIT_DURATION_MS = 130L
    const val STEP_ENTER_DURATION_MS = 180L
    const val STEP_TRANSITION_DISTANCE_DP = 10
  }
}
