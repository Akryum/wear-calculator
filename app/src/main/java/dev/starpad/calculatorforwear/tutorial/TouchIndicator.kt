package dev.starpad.calculatorforwear.tutorial

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import dev.starpad.calculatorforwear.tutorial.SceneTimeline.easeOut
import kotlin.math.atan2
import kotlin.math.hypot

/** One frame of the touch indicator: where the fingertip is and what it is doing. */
internal class Touch(
  val x: Float,
  val y: Float,
  /** Overall visibility, 0 to 1. */
  val alpha: Float,
  /** Whether the finger is down; the disc shrinks a little and darkens. */
  val pressed: Boolean = false,
  /** Fraction of a long press completed, drawn as an arc around the disc. */
  val hold: Float = 0f,
  /** Progress of the finger lifting off, the disc growing as it fades. */
  val lift: Float = 0f,
)

/**
 * Draws the fingertip the scenes move around: a plain translucent disc, an arc around it while
 * it holds, and a faint trail back towards where it landed while it drags.
 */
internal class TouchIndicator(private val accent: Int) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val trailMatrix = Matrix()
  private val arcBounds = RectF()

  /** Trail brightness from its origin (0) to the fingertip (1), laid along the drag by [trailMatrix]. */
  private val trail = LinearGradient(
    0f, 0f, 1f, 0f,
    withAlpha(Color.WHITE, 0.03f), withAlpha(Color.WHITE, 0.2f),
    Shader.TileMode.CLAMP,
  )

  /** Draws [touch] in [frame]. */
  fun draw(canvas: Canvas, frame: SceneFrame, touch: Touch) {
    if (touch.alpha <= 0f) return
    val base = frame.r(RADIUS)
    val radius = base * (if (touch.pressed) 0.9f else 1f) * (1f + 0.25f * easeOut(touch.lift))
    paint.style = Paint.Style.FILL
    paint.color = withAlpha(Color.WHITE, touch.alpha * if (touch.pressed) 0.42f else 0.32f)
    canvas.drawCircle(touch.x, touch.y, radius, paint)

    if (touch.hold > 0f) {
      val ring = radius + base * 0.16f
      arcBounds.set(touch.x - ring, touch.y - ring, touch.x + ring, touch.y + ring)
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = base * 0.12f
      paint.strokeCap = Paint.Cap.ROUND
      paint.color = withAlpha(accent, touch.alpha)
      canvas.drawArc(arcBounds, -90f, 360f * touch.hold.coerceAtMost(1f), false, paint)
      paint.strokeCap = Paint.Cap.BUTT
    }
  }

  /** Path from where the finger landed to where it is now, brightest at the fingertip. */
  fun drawTrail(canvas: Canvas, frame: SceneFrame, fromX: Float, fromY: Float, toX: Float, toY: Float, alpha: Float) {
    val length = hypot(toX - fromX, toY - fromY)
    if (alpha <= 0f || length <= 0f) return
    trailMatrix.setScale(length, length)
    trailMatrix.postRotate(Math.toDegrees(atan2(toY - fromY, toX - fromX).toDouble()).toFloat())
    trailMatrix.postTranslate(fromX, fromY)
    trail.setLocalMatrix(trailMatrix)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = frame.r(0.07f)
    paint.strokeCap = Paint.Cap.ROUND
    paint.shader = trail
    paint.color = withAlpha(Color.WHITE, alpha)
    canvas.drawLine(fromX, fromY, toX, toY, paint)
    paint.shader = null
    paint.strokeCap = Paint.Cap.BUTT
  }

  private companion object {
    /** Disc radius as a fraction of the frame radius. */
    const val RADIUS = 0.22f
  }
}
