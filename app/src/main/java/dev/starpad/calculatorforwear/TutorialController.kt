package dev.starpad.calculatorforwear

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

/** Owns tutorial overlay presentation without coupling it to calculator input state. */
class TutorialController(
  private val context: Context,
  private val container: FrameLayout,
) {
  private val progressStore = TutorialProgressStore(context)
  private lateinit var carousel: RecyclerView
  private lateinit var carouselLayoutManager: LinearLayoutManager
  private lateinit var indicator: LinearLayout
  private lateinit var dots: List<View>
  private val snapHelper = PagerSnapHelper()
  private var step = TutorialStep.TAP_NUMBER
  private var isReplay = false

  /** Shows unfinished tutorial, if any. */
  fun showIfNeeded() {
    if (progressStore.isComplete()) {
      container.visibility = View.GONE
      return
    }

    isReplay = false
    step = progressStore.currentStep()
    show(step)
  }

  /** Shows tutorial from its first scene even after first-run tutorial was completed. */
  fun showReplay() {
    isReplay = true
    step = TutorialStep.TAP_NUMBER
    show(step)
  }

  /** Pauses decorative animation while activity is backgrounded. */
  fun onStop() {
    if (container.isVisible) pauseVisibleIllustrations()
  }

  /** Resumes current page animation when activity returns. */
  fun onStart() {
    if (container.isVisible) resumeSelectedIllustration()
  }

  private fun show(initialStep: TutorialStep) {
    buildOverlay()
    container.visibility = View.VISIBLE
    carousel.scrollToPosition(initialStep.ordinal)
    updateIndicator(initialStep.ordinal)
    carousel.post { selectPage(initialStep.ordinal, persist = false) }
  }

  private fun buildOverlay() {
    if (container.isNotEmpty()) return

    container.setBackgroundColor(Color.BLACK)
    container.isClickable = true
    container.isFocusable = true

    carouselLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    carousel = RecyclerView(context).apply {
      id = R.id.tutorial_carousel
      layoutManager = carouselLayoutManager
      adapter = TutorialCarouselAdapter(context, ::finishTutorial)
      itemAnimator = null
      isNestedScrollingEnabled = false
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
      addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
          if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            selectedSnapPosition()?.let { selectPage(it, persist = true) }
          } else {
            pauseVisibleIllustrations()
          }
        }
      })
    }
    snapHelper.attachToRecyclerView(carousel)
    container.addView(carousel, FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT,
    ))

    indicator = LinearLayout(context).apply {
      id = R.id.tutorial_page_indicator
      gravity = Gravity.CENTER
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
      isFocusable = true
      orientation = LinearLayout.HORIZONTAL
    }
    dots = TutorialStep.entries.map { createDot() }
    dots.forEach(indicator::addView)
    container.addView(indicator, FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT,
      Gravity.TOP or Gravity.CENTER_HORIZONTAL,
    ).apply { topMargin = dp(12) })
  }

  private fun selectedSnapPosition(): Int? {
    val snapView = snapHelper.findSnapView(carouselLayoutManager) ?: return null
    return carousel.getChildAdapterPosition(snapView).takeIf { it != RecyclerView.NO_POSITION }
  }

  private fun selectPage(position: Int, persist: Boolean) {
    val selectedStep = TutorialStep.entries.getOrNull(position) ?: return
    step = selectedStep
    updateIndicator(position)
    if (persist && !isReplay) progressStore.saveStep(selectedStep)
    resumeSelectedIllustration()
  }

  private fun updateIndicator(position: Int) {
    dots.forEachIndexed { index, dot ->
      dot.isSelected = index == position
      val diameter = dp(if (dot.isSelected) ACTIVE_DOT_DIAMETER_DP else DOT_DIAMETER_DP)
      dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).apply {
        width = diameter
        height = diameter
      }
      dot.background = dotDrawable(dot.isSelected)
    }
    indicator.contentDescription = context.getString(
      R.string.tutorial_page_description,
      position + 1,
      TutorialStep.entries.size,
    )
  }

  private fun createDot(): View = View(context).apply {
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    layoutParams = LinearLayout.LayoutParams(dp(DOT_DIAMETER_DP), dp(DOT_DIAMETER_DP)).apply {
      marginStart = dp(DOT_MARGIN_DP)
      marginEnd = dp(DOT_MARGIN_DP)
    }
  }

  private fun dotDrawable(active: Boolean) = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(if (active) ORANGE else INACTIVE_DOT)
  }

  private fun pauseVisibleIllustrations() {
    repeat(carousel.childCount) { index -> pageAtChild(index)?.pauseAnimation() }
  }

  private fun resumeSelectedIllustration() {
    repeat(carousel.childCount) { index ->
      val page = pageAtChild(index) ?: return@repeat
      if (carousel.getChildAdapterPosition(page) == step.ordinal) page.resumeAnimation() else page.pauseAnimation()
    }
  }

  private fun pageAtChild(index: Int): TutorialPageView? = carousel.getChildAt(index) as? TutorialPageView

  private fun finishTutorial() {
    progressStore.markComplete()
    pauseVisibleIllustrations()
    container.visibility = View.GONE
  }

  private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

  private companion object {
    const val ACTIVE_DOT_DIAMETER_DP = 8
    const val DOT_DIAMETER_DP = 6
    const val DOT_MARGIN_DP = 3
    val INACTIVE_DOT: Int = Color.rgb(96, 96, 96)
    val ORANGE: Int = Color.rgb(255, 152, 46)
  }
}
