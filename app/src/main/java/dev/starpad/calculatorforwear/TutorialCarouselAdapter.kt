package dev.starpad.calculatorforwear

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/** Supplies one full-screen tutorial page for each tutorial step. */
internal class TutorialCarouselAdapter(
  private val context: Context,
  private val onFinishTutorial: () -> Unit,
) : RecyclerView.Adapter<TutorialPageViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialPageViewHolder = TutorialPageViewHolder(
    TutorialPageView(context, onFinishTutorial).apply {
      layoutParams = RecyclerView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      )
    },
  )

  override fun onBindViewHolder(holder: TutorialPageViewHolder, position: Int) {
    holder.page.bind(TutorialStep.entries[position])
  }

  override fun getItemCount(): Int = TutorialStep.entries.size
}

/** Holds one reusable full-screen tutorial page. */
internal class TutorialPageViewHolder(val page: TutorialPageView) : RecyclerView.ViewHolder(page)

/**
 * Displays one animated gesture explanation and its action button. Built only by
 * [TutorialCarouselAdapter], never inflated from a layout, so it takes its callback directly
 * instead of the attribute-set constructors the layout editor looks for.
 */
@SuppressLint("ViewConstructor")
internal class TutorialPageView(
  context: Context,
  onFinishTutorial: () -> Unit,
) : FrameLayout(context) {
  private val illustration = TutorialIllustrationView(context).apply {
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  private val title = textView(context, 19f).apply {
    id = R.id.tutorial_title
    typeface = android.graphics.Typeface.DEFAULT_BOLD
    gravity = Gravity.CENTER
  }
  private val body = textView(context, 14f).apply {
    id = R.id.tutorial_body
    gravity = Gravity.CENTER
    maxLines = 2
    setPadding(0, dp(3), 0, dp(7))
  }
  private val action = MaterialButton(context, null, R.attr.TutorialTextButton).apply {
    id = R.id.tutorial_action_button
    minHeight = dp(42)
    minWidth = dp(82)
    insetTop = 0
    insetBottom = 0
    setOnClickListener { onFinishTutorial() }
  }

  init {
    addView(illustration, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    addView(LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
      setPadding(dp(14))
      addView(title, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
      ))
      addView(body, LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
      ))
      addView(action)
    }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
      bottomMargin = dp(2)
    })
  }

  /** Binds this reusable page to one tutorial step. */
  fun bind(step: TutorialStep) {
    val content = contentFor(step)
    title.setText(content.title)
    body.setText(content.body)
    val finalStep = step.next() == null
    action.setText(if (finalStep) R.string.tutorial_done else R.string.tutorial_skip)
    action.contentDescription = context.getString(
      if (finalStep) R.string.tutorial_done_description else R.string.tutorial_skip_description,
    )
    illustration.showStep(step)
  }

  /** Pauses page animation when it is no longer selected. */
  fun pauseAnimation() = illustration.pauseAnimation()

  /** Starts page animation when this page becomes selected. */
  fun resumeAnimation() = illustration.resumeAnimation()

  private fun contentFor(step: TutorialStep): TutorialContent = when (step) {
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
    TutorialStep.SCROLL_HISTORY -> TutorialContent(
      R.string.tutorial_history_title,
      R.string.tutorial_history_body,
    )
  }

  private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

/** Creates standard accessible tutorial copy. */
private fun textView(context: Context, size: Float): TextView = TextView(context).apply {
  setTextColor(Color.WHITE)
  textSize = size
  ViewCompat.setAccessibilityHeading(this, false)
}

/** String resources used by one tutorial scene. */
private data class TutorialContent(val title: Int, val body: Int)
