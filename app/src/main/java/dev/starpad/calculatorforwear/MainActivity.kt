package dev.starpad.calculatorforwear

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import dev.starpad.calculatorforwear.databinding.ActivityMainBinding
import dev.starpad.calculatorforwear.haptics.CalculatorHaptics
import dev.starpad.calculatorforwear.haptics.HapticCue
import dev.starpad.calculatorforwear.menu.RadialChoice
import net.objecthunter.exp4j.ExpressionBuilder
import java.time.Instant
import kotlin.math.*

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding
  private lateinit var tutorialController: TutorialController
  private lateinit var historyStore: CalculationHistoryStore
  private lateinit var settingsStore: CalculatorSettingsStore
  private lateinit var haptics: CalculatorHaptics

  // Screen size
  private var screenWidth: Int = 0
  private var screenHeight: Int = 0

  // Value
  private var mainTextLayout: FrameLayout? = null
  private var currentInputTextView: TextView? = null
  private var resultTextView: TextView? = null

  // Digit buttons
  private var digitBtnWidth: Int = 0
  private var digitBtnHeight: Int = 0
  private var digitBtnsLayout: RelativeLayout? = null

  // Center menu
  private val choiceViews = mutableMapOf<RadialChoice, View>()
  private var centerMenuActive: Boolean = false
  private var centerMenuLayout: RelativeLayout? = null
  private var choice: RadialChoice = RadialChoice.EQUALS
  private var previousChoice: RadialChoice? = null
  private var choiceStartTime: Long = 0
  private val longPressDelay: Long = 800

  /** The backspace glyph, the only one whose icon changes while the menu is open. */
  private val backspaceIcon: ImageView?
    get() = choiceViews[RadialChoice.BACKSPACE] as? ImageView

  /** Turns the backspace choice into a clear-all once the finger has rested on it long enough. */
  private val armClearAll = Runnable {
    backspaceIcon?.setImageResource(R.drawable.ic_baseline_clear_24)
    binding.choiceLabel.text = getString(R.string.action_clear_all)
    haptics.play(HapticCue.Threshold)
  }

  // Animation
  private var shortAnimationDuration: Int = 0

  // The calculator page is a drag surface for the radial menu, never a click target: the digits
  // around it are real buttons, so there is no click for performClick to report to accessibility.
  @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Wallpaper-derived Material You palette, applied before inflation and scoped to this
    // activity so only the calculator screen follows the system colors. Devices without dynamic
    // color support keep the static palette declared by Theme.MyApp.
    DynamicColors.applyToActivityIfAvailable(this)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    historyStore = CalculationHistoryStore(this)
    settingsStore = CalculatorSettingsStore(this)
    // Feedback bubbles up to the window, so one view anchors the cues for the whole screen.
    haptics = CalculatorHaptics(binding.root, settingsStore)

    mainTextLayout = findViewById(R.id.main_text_layout)
    centerMenuLayout = findViewById(R.id.center_menu_layout)

    currentInputTextView = findViewById(R.id.txt_input)
    resultTextView = findViewById(R.id.txt_result)

    // Screen size
    screenWidth = resources.displayMetrics.widthPixels
    screenHeight = resources.displayMetrics.heightPixels

    // Central menu
    RadialChoice.entries.forEach { choiceViews[it] = findViewById(it.viewId) }

    // Button size
    digitBtnWidth = screenWidth / 384 * 90
    digitBtnHeight = screenHeight / 384 * 90

    // Digit buttons

    digitBtnsLayout = findViewById(R.id.digits_layout)

    for (i in 0..9) {
      val button = MaterialButton(this, null, R.attr.MaterialTextButton)
      button.text = i.toString()

      val layoutParams = RelativeLayout.LayoutParams(digitBtnWidth, digitBtnHeight)

      // Position
      val angle = PI * 2 * i / 10 + PI / 2
      val x = cos(angle) * (screenWidth / 2 - layoutParams.width / 2) + screenWidth / 2
      val y = sin(angle) * (screenHeight / 2 - layoutParams.height / 2) + screenHeight / 2

      Log.d("DIGIT_BTN", "i=$i, angle=$angle, x=$x, y=$y, ${layoutParams.width}, ${screenWidth}x${screenHeight}")

      layoutParams.leftMargin = x.toInt() - layoutParams.width / 2
      layoutParams.topMargin = y.toInt() - layoutParams.height / 2
      button.layoutParams = layoutParams
      button.insetTop = 0
      button.insetBottom = 0
      button.cornerRadius = layoutParams.width
      button.textSize = (digitBtnHeight * 1 / 5).toFloat()
      button.setPadding(0)

      button.setOnClickListener {
        currentInputTextView?.text = "${currentInputTextView?.text}$i"
        currentInputTextView?.alpha = 1f
        resultTextView?.alpha = 0f
        haptics.play(HapticCue.Tick)
      }

      digitBtnsLayout!!.addView(button)
    }

    // Animation
    shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

    binding.calculatorScroll.doOnLayout { scroll ->
      binding.calculatorPage.layoutParams = binding.calculatorPage.layoutParams.apply {
        height = scroll.height
      }
    }
    binding.calculatorPage.setOnTouchListener { _, event -> handleCalculatorPageTouch(event) }
    binding.settingsButton.setOnClickListener {
      haptics.play(HapticCue.Tick)
      startActivity(Intent(this, SettingsActivity::class.java))
    }

    tutorialController = TutorialController(this, binding.tutorialContainer)
    tutorialController.showIfNeeded()
  }

  override fun onResume() {
    super.onResume()
    renderHistory()
    // Settings only stores the replay request and finishes, so the calculator picks it up here.
    if (TutorialReplay.consume(this)) tutorialController.showReplay()
  }

  override fun onStart() {
    super.onStart()
    tutorialController.onStart()
  }

  override fun onStop() {
    tutorialController.onStop()
    super.onStop()
  }

  @SuppressLint("SetTextI18n")
  private fun handleCalculatorPageTouch(event: MotionEvent): Boolean {
    if (event.action == MotionEvent.ACTION_DOWN) {
      val dX = event.x - screenWidth / 2
      val dY = event.y - screenHeight / 2
      val threshold = (digitBtnWidth + digitBtnHeight) / 2
      if (sqrt(dX.pow(2) + dY.pow(2)) >= threshold) return false
      binding.calculatorScroll.requestDisallowInterceptTouchEvent(true)
    }
    return onCalculatorTouch(event)
  }

  @SuppressLint("SetTextI18n")
  private fun onCalculatorTouch(event: MotionEvent): Boolean {
    return when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        Log.d("TOUCH", "Action was DOWN: ${event.x};${event.y}")
        val dX = event.x - screenWidth / 2
        val dY = event.y - screenHeight / 2
        val threshold = (digitBtnWidth + digitBtnHeight) / 2
        if (sqrt(dX.pow(2) + dY.pow(2)) < threshold) {
          centerMenuActive = true
          mainTextLayout?.let { animateLayoutAlpha(it, 0f) }
          digitBtnsLayout?.let { animateLayoutAlpha(it, 0f) }
          centerMenuLayout?.let { animateLayoutAlpha(it, 1f) }
          choice = RadialChoice.EQUALS
          choiceViews[choice]?.apply {
            scaleX = 1.3f
            scaleY = 1.3f
            alpha = 1f
          }
          updateChoiceLabel()
          // Equal is already highlighted, so the first move only ticks once the finger leaves it.
          previousChoice = choice
          choiceStartTime = Instant.now().toEpochMilli()
          resetLongPressButtons()
          haptics.play(HapticCue.GestureStart)
          Log.d("TOUCH", "Center menu active!")
        }
        true
      }
      MotionEvent.ACTION_MOVE -> {
        if (centerMenuActive) {
          val dX = event.x - screenWidth / 2
          val dY = event.y - screenHeight / 2
          val distance = sqrt(dX.pow(2) + dY.pow(2))
          val angle = atan2(dY, dX)
          choice = RadialChoice.at(distance, angle)
          if (previousChoice != choice) {
            choiceStartTime = Instant.now().toEpochMilli()
            haptics.play(HapticCue.Selection)
            updateChoiceLabel()

            // Reset long press buttons
            cancelLongPress()
            resetLongPressButtons()
            if (choice == RadialChoice.BACKSPACE) {
              backspaceIcon?.postDelayed(armClearAll, longPressDelay)
            }

            previousChoice?.let { choiceViews[it] }?.let { animateViewSize(it, 1f) }
            choiceViews[choice]?.let { animateViewSize(it, 1.3f) }
            previousChoice = choice
          }
          Log.d("TOUCH", "Action was MOVE: ${event.x};${event.y} => ${choice}")
        }
        true
      }
      MotionEvent.ACTION_UP -> {
        Log.d("TOUCH", "Action was UP: ${event.x};${event.y}")
        closeCenterMenu()

        // Handle long press
        val now = Instant.now().toEpochMilli()
        val isLongPress = now - choiceStartTime > longPressDelay

        if (choice == RadialChoice.EQUALS) {
          try {
            val displayExpression = currentInputTextView!!.text.toString()
            var input = displayExpression
            input = input.replace("×", "*")
            input = input.replace("÷", "/")

            // Special % handling, example: 100+99-30% => (100+99) * (1-30%)
            val specialPercentReg = Regex("[-+][\\d.]+%$")
            input = input.replace(specialPercentReg) {
              "*(1${it.value})"
            }

            input = input.replace("%", "/100")
            Log.d("INPUT", "Input: ${input}")

            val result = ExpressionBuilder(input).build().evaluate() as Number

            val formattedResult = if (result.toFloat().rem(1.0) == 0.0) {
              result.toInt().toString()
            } else {
              "%.2f".format(result)
            }

            resultTextView?.text = formattedResult
            currentInputTextView?.alpha = 0f
            resultTextView?.alpha = 1f
            currentInputTextView?.text = formattedResult
            historyStore.append(displayExpression, formattedResult)
            renderHistory()
            haptics.play(HapticCue.Confirm)
          } catch (e: Exception) {
            Log.d("INPUT", "Input error ${e}")
            // The expression is left untouched on screen, so the cue is the only sign of failure.
            haptics.play(HapticCue.Reject)
          }
        } else if (choice == RadialChoice.BACKSPACE) {
          if (currentInputTextView?.text == resultTextView?.text || isLongPress) {
            val hadInput = !currentInputTextView?.text.isNullOrEmpty()
            currentInputTextView?.text = ""
            resultTextView?.alpha = 0.5f
            haptics.play(if (hadInput) HapticCue.Confirm else HapticCue.Reject)
          } else if (currentInputTextView?.text!!.isNotEmpty()) {
            resultTextView?.text = ""
            currentInputTextView?.text =
              currentInputTextView?.text?.substring(0, currentInputTextView?.text!!.length - 1)
            haptics.play(HapticCue.Tick)
          } else {
            haptics.play(HapticCue.Reject)
          }
        } else {
          currentInputTextView?.text = "${currentInputTextView?.text}${choice.symbol}"
          currentInputTextView?.alpha = 1f
          resultTextView?.alpha = 0f
          haptics.play(HapticCue.Tick)
        }

        true
      }
      // A notification or a pause steals the gesture: put the screen back without committing.
      MotionEvent.ACTION_CANCEL -> {
        Log.d("TOUCH", "Action was CANCEL")
        closeCenterMenu()
        true
      }
      else -> false
    }
  }

  private fun animateViewSize (view: View, scale: Float) {
    view.animate().scaleX(scale)
      .scaleY(scale)
      .alpha(scale - 0.3f)
      .setDuration(shortAnimationDuration.toLong())
  }

  private fun animateLayoutAlpha (layout: ViewGroup, alpha: Float) {
    layout.animate().alpha(alpha)
      .setDuration(shortAnimationDuration.toLong())
  }

  private fun resetLongPressButtons () {
    backspaceIcon?.setImageResource(R.drawable.ic_baseline_backspace_24)
  }

  /** Drops a pending clear-all so it cannot arm after the finger moved on or left the screen. */
  private fun cancelLongPress() {
    backspaceIcon?.removeCallbacks(armClearAll)
  }

  /** Names the hovered action on the bezel, where the dragging finger cannot hide it. */
  private fun updateChoiceLabel() {
    binding.choiceLabel.text = getString(choice.labelRes)
  }

  /** Hides the radial menu and restores the calculator, without committing the current choice. */
  private fun closeCenterMenu() {
    centerMenuActive = false
    cancelLongPress()
    mainTextLayout?.let { animateLayoutAlpha(it, 1f) }
    digitBtnsLayout?.let { animateLayoutAlpha(it, 1f) }
    centerMenuLayout?.let { animateLayoutAlpha(it, 0f) }
    previousChoice?.let { choiceViews[it] }?.apply {
      scaleX = 1f
      scaleY = 1f
      alpha = 0.7f
    }
    previousChoice = null
  }

  private fun renderHistory() {
    val entries = historyStore.load()
    binding.historyEntries.removeAllViews()
    binding.historyEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    entries.forEach { entry ->
      val row = MaterialButton(this, null, R.attr.MaterialTextButton).apply {
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        text = getString(R.string.history_entry, entry.expression, entry.result)
        isAllCaps = false
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
        contentDescription = text
        setOnClickListener { restoreHistoryResult(entry.result) }
      }
      binding.historyEntries.addView(row)
    }
  }

  private fun restoreHistoryResult(result: String) {
    currentInputTextView?.text = result
    currentInputTextView?.alpha = 1f
    resultTextView?.alpha = 0f
    binding.calculatorScroll.post { binding.calculatorScroll.smoothScrollTo(0, 0) }
    haptics.play(HapticCue.Confirm)
  }
}
