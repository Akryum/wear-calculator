package dev.starpad.calculatorforwear.tutorial

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Text
import dev.starpad.calculatorforwear.R
import dev.starpad.calculatorforwear.TutorialIllustrationView
import dev.starpad.calculatorforwear.TutorialStep

/** Test tag of the pager, so instrumentation tests can swipe it. */
const val TUTORIAL_PAGER_TAG = "tutorial_pager"

/**
 * The tutorial as a Wear pager: one page per [TutorialStep], the platform page indicator curved
 * along the bottom edge, rotary input snapping between pages, and Skip or Done to leave.
 *
 * @param initialStep page shown first: the last one reached, or the first when replaying.
 * @param onStepSettled called each time the pager comes to rest on a page, the first included.
 * @param onFinish called when the user taps Skip or Done.
 */
@Composable
internal fun TutorialCarousel(
  initialStep: TutorialStep,
  onStepSettled: (TutorialStep) -> Unit,
  onFinish: () -> Unit,
) {
  val pagerState = rememberPagerState(initialPage = initialStep.ordinal) { TutorialStep.entries.size }
  val currentOnStepSettled by rememberUpdatedState(onStepSettled)
  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.settledPage }.collect { currentOnStepSettled(TutorialStep.entries[it]) }
  }

  HorizontalPagerScaffold(
    pagerState = pagerState,
    modifier = Modifier.fillMaxSize().background(Color.Black),
  ) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize().testTag(TUTORIAL_PAGER_TAG),
      // The calculator opts out of swipe-to-dismiss, so the whole width is free to page.
      gestureInclusion = PagerDefaults.gestureInclusion(pagerState, 0f),
      rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(pagerState),
    ) { page ->
      TutorialPage(
        step = TutorialStep.entries[page],
        // Only the page at rest animates; neighbours wait until they are swiped in.
        animate = pagerState.settledPage == page && !pagerState.isScrollInProgress,
        onFinish = onFinish,
      )
    }
  }
}

/** One page: the looping illustration above the title, the two-line body and Skip or Done. */
@Composable
private fun TutorialPage(step: TutorialStep, animate: Boolean, onFinish: () -> Unit) {
  val finalStep = step.next() == null
  Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
    AndroidView(
      factory = { context ->
        TutorialIllustrationView(context).apply {
          importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
      },
      // Takes whatever the copy leaves, so it never overlaps it; kept off the bezel at the top.
      modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
      update = { view ->
        view.step = step
        view.active = animate
      },
    )
    Text(
      text = stringResource(step.titleRes),
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 6.dp, end = 14.dp),
      color = Color.White,
      fontSize = 19.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(step.bodyRes),
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 3.dp, end = 14.dp, bottom = 7.dp),
      color = Color.White,
      fontSize = 14.sp,
      textAlign = TextAlign.Center,
      // Always two lines, so title and button sit at the same height on every page, balanced so
      // a two-line body never leaves a single word on its second line.
      minLines = 2,
      maxLines = 2,
      style = TextStyle(lineBreak = LineBreak.Heading),
    )
    val actionDescription = stringResource(
      if (finalStep) R.string.tutorial_done_description else R.string.tutorial_skip_description,
    )
    CompactButton(
      onClick = onFinish,
      // Bottom padding keeps clear of the page indicator along the edge.
      modifier = Modifier.padding(bottom = 16.dp).semantics { contentDescription = actionDescription },
      colors = ButtonDefaults.childButtonColors(contentColor = colorResource(R.color.brand_orange)),
      label = { Text(stringResource(if (finalStep) R.string.tutorial_done else R.string.tutorial_skip)) },
    )
  }
}

/** Title shown above the illustration of this step. */
private val TutorialStep.titleRes: Int
  get() = when (this) {
    TutorialStep.TAP_NUMBER -> R.string.tutorial_tap_number_title
    TutorialStep.DRAG_ACTION -> R.string.tutorial_drag_action_title
    TutorialStep.CLEAR_INPUT -> R.string.tutorial_clear_input_title
    TutorialStep.SCROLL_HISTORY -> R.string.tutorial_history_title
  }

/** One or two lines explaining the gesture of this step. */
private val TutorialStep.bodyRes: Int
  get() = when (this) {
    TutorialStep.TAP_NUMBER -> R.string.tutorial_tap_number_body
    TutorialStep.DRAG_ACTION -> R.string.tutorial_drag_action_body
    TutorialStep.CLEAR_INPUT -> R.string.tutorial_clear_input_body
    TutorialStep.SCROLL_HISTORY -> R.string.tutorial_history_body
  }
