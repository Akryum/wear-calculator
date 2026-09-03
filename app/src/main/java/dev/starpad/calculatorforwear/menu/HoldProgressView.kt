package dev.starpad.calculatorforwear.menu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors

/**
 * Ring that fills up around a glyph of the radial menu while the finger rests on it, showing how
 * far a long press is from arming its larger action (backspace turning into clear-all).
 *
 * Fills its parent and draws around whichever [start] anchored it to, so the menu layout that
 * positions the glyphs stays untouched. Progress follows the real clock rather than an animator,
 * because it reports a timer and must stay truthful whatever the animation settings are.
 */
class HoldProgressView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
) : View(context, attrs) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
  }
  private val anchorBounds = Rect()
  private val ringBounds = RectF()
  private val trackColor = ColorUtils.setAlphaComponent(MaterialColors.getColor(this, MaterialR.attr.colorOnSurface), TRACK_ALPHA)
  private val progressColor = MaterialColors.getColor(this, AppCompatR.attr.colorPrimary)
  private var anchor: View? = null
  private var startedAt = 0L
  private var durationMs = 1L

  /** Fraction of the hold elapsed, 0 to 1; stays at 1 once the action has armed. */
  var progress: Float = 0f
    private set(value) {
      field = value
      invalidate()
    }

  private val tick = object : Runnable {
    override fun run() {
      val elapsed = (SystemClock.uptimeMillis() - startedAt).toFloat() / durationMs
      progress = elapsed.coerceIn(0f, 1f)
      if (elapsed < 1f) postOnAnimation(this)
    }
  }

  /** Starts filling the ring around [anchor], completing after [durationMs]. */
  fun start(anchor: View, durationMs: Long) {
    removeCallbacks(tick)
    this.anchor = anchor
    this.durationMs = durationMs.coerceAtLeast(1L)
    startedAt = SystemClock.uptimeMillis()
    tick.run()
  }

  /** Hides the ring, whether the hold completed or was abandoned. */
  fun cancel() {
    removeCallbacks(tick)
    anchor = null
    progress = 0f
  }

  override fun onDetachedFromWindow() {
    removeCallbacks(tick)
    super.onDetachedFromWindow()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val anchor = anchor ?: return
    if (progress <= 0f) return
    val parent = parent as? ViewGroup ?: return
    // The anchor sits deeper in the menu hierarchy; its rect is brought into this view's space.
    anchor.getDrawingRect(anchorBounds)
    parent.offsetDescendantRectToMyCoords(anchor, anchorBounds)
    val centerX = anchorBounds.exactCenterX() - left
    val centerY = anchorBounds.exactCenterY() - top
    val radius = RADIUS_DP * resources.displayMetrics.density
    ringBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

    paint.strokeWidth = STROKE_DP * resources.displayMetrics.density
    paint.color = trackColor
    canvas.drawOval(ringBounds, paint)
    paint.color = progressColor
    canvas.drawArc(ringBounds, -90f, 360f * progress, false, paint)
  }

  private companion object {
    /** Large enough to peek out from under a fingertip, small enough to clear the neighbouring glyphs. */
    const val RADIUS_DP = 20f
    const val STROKE_DP = 2.5f
    const val TRACK_ALPHA = 0x33
  }
}
