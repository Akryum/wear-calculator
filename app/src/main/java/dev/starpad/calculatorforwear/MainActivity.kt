package dev.starpad.calculatorforwear

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import dev.starpad.calculatorforwear.databinding.ActivityMainBinding
import net.objecthunter.exp4j.ExpressionBuilder
import java.time.Instant
import java.util.Timer
import kotlin.concurrent.schedule
import java.util.TimerTask
import kotlin.math.*

class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding

  private var activity = this

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
  private var equalView: View? = null
  private var addView: View? = null
  private var subtractView: View? = null
  private var multiplyView: View? = null
  private var divideView: View? = null
  private var backspaceView: View? = null
  private var dotView: View? = null
  private var powView: View? = null
  private var percentView: View? = null
  private var centerMenuActive: Boolean = false
  private var centerMenuLayout: RelativeLayout? = null
  private var choice: String = ""
  private var previousChoiceView: View? = null
  private var choiceStartTime: Long = 0
  private var longPressTimer: TimerTask? = null
  private val longPressDelay: Long = 800

  // Animation
  private var shortAnimationDuration: Int = 0

  @SuppressLint("SetTextI18n")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    mainTextLayout = findViewById(R.id.main_text_layout)
    centerMenuLayout = findViewById(R.id.center_menu_layout)

    currentInputTextView = findViewById(R.id.txt_input)
    resultTextView = findViewById(R.id.txt_result)

    // Screen size
    screenWidth = resources.displayMetrics.widthPixels
    screenHeight = resources.displayMetrics.heightPixels

    // Central menu
    equalView = findViewById(R.id.txt_equal)
    addView = findViewById(R.id.txt_add)
    subtractView = findViewById(R.id.txt_subtract)
    multiplyView = findViewById(R.id.txt_multiply)
    divideView = findViewById(R.id.txt_divide)
    backspaceView = findViewById(R.id.img_backspace)
    dotView = findViewById(R.id.txt_dot)
    powView = findViewById(R.id.txt_pow)
    percentView = findViewById(R.id.txt_percent)

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
      }

      digitBtnsLayout!!.addView(button)
    }

    // Animation
    shortAnimationDuration = resources.getInteger(com.google.android.material.R.integer.abc_config_activityShortDur)
  }

  @SuppressLint("SetTextI18n")
  override fun onTouchEvent(event: MotionEvent): Boolean {
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
          equalView?.scaleX = 1.3f
          equalView?.scaleY = 1.3f
          equalView?.alpha = 1f
          choice = "="
          choiceStartTime = Instant.now().toEpochMilli()
          resetLongPressButtons()
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
          val view: View?
          if (distance <= 36) {
            choice = "="
            view = equalView
          } else if (angle >= - PI * 1 / 8 && angle <= PI / 8) {
            choice = "×"
            view = multiplyView
          } else if (angle >= PI / 8 && angle <= PI * 3 / 8) {
            choice = "."
            view = dotView
          } else if (angle >= PI * 3 / 8 && angle <= PI * 5 / 8) {
            choice = "+"
            view = addView
          } else if (angle >= PI * 5 / 8 && angle <= PI * 7 / 8) {
            choice = "%"
            view = percentView
          } else if (angle >= PI * 7 / 8 || angle <= - PI * 7 / 8) {
            choice = "÷"
            view = divideView
          } else if (angle >= - PI * 7 / 8 && angle <= - PI * 5 / 8) {
            choice = "backspace"
            view = backspaceView
          } else if (angle >= - PI * 3 / 8  && angle <= - PI * 1 / 8) {
            choice = "^"
            view = powView
          } else {
            choice = "-"
            view = subtractView
          }
          if (previousChoiceView != view) {
            choiceStartTime = Instant.now().toEpochMilli()

            // Reset long press buttons
            longPressTimer?.cancel()
            resetLongPressButtons()
            if (view == backspaceView) {
              longPressTimer = Timer("longPressBackspace", false).schedule(longPressDelay) {
                activity.runOnUiThread {
                  (backspaceView as ImageView).setImageResource(R.drawable.ic_baseline_clear_24)
                }
              }
            }

            if (previousChoiceView != null) {
              animateViewSize(previousChoiceView!!, 1f)
            }
            if (view != null) {
              animateViewSize(view, 1.3f)
            }
            previousChoiceView = view
          }
          Log.d("TOUCH", "Action was MOVE: ${event.x};${event.y} angle:${angle} distance:${distance} => ${choice} ${view}")
        }
        true
      }
      MotionEvent.ACTION_UP -> {
        Log.d("TOUCH", "Action was UP: ${event.x};${event.y}")
        centerMenuActive = false
        mainTextLayout?.let { animateLayoutAlpha(it, 1f) }
        digitBtnsLayout?.let { animateLayoutAlpha(it, 1f) }
        centerMenuLayout?.let { animateLayoutAlpha(it, 0f) }
        previousChoiceView?.scaleX = 1f
        previousChoiceView?.scaleY = 1f
        previousChoiceView?.alpha = 0.7f

        // Handle long press
        val now = Instant.now().toEpochMilli()
        val isLongPress = now - choiceStartTime > longPressDelay

        if (choice == "=") {
          try {
            var input = currentInputTextView!!.text as String
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

            if (result.toFloat().rem(1.0) == 0.0) {
              resultTextView?.text = result.toInt().toString()
            } else {
              resultTextView?.text = "%.2f".format(result)
            }

            currentInputTextView?.alpha = 0f
            resultTextView?.alpha = 1f
            currentInputTextView?.text = resultTextView?.text
          } catch (e: Exception) {
            Log.d("INPUT", "Input error ${e}")
          }
        } else if (choice == "backspace") {
          if (currentInputTextView?.text == resultTextView?.text || isLongPress) {
            currentInputTextView?.text = ""
            resultTextView?.alpha = 0.5f
          } else if (currentInputTextView?.text!!.isNotEmpty()) {
            resultTextView?.text = ""
            currentInputTextView?.text =
              currentInputTextView?.text?.substring(0, currentInputTextView?.text!!.length - 1)
          }
        } else {
          currentInputTextView?.text = "${currentInputTextView?.text}$choice"
          currentInputTextView?.alpha = 1f
          resultTextView?.alpha = 0f
        }

        true
      }
      else -> super.onTouchEvent(event)
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
    (backspaceView as ImageView).setImageResource(R.drawable.ic_baseline_backspace_24)
  }
}