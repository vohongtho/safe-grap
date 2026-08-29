package com.safegrap.app.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.safegrap.app.R
import com.safegrap.app.alerts.AlertState
import kotlin.math.max

class DetectionOverlayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var detection: VehicleDetection? = null
    private var distance: Float? = null
    private var state = AlertState.SAFE
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(3f) }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun update(detection: VehicleDetection?, distance: Float?, state: AlertState) {
        this.detection = detection; this.distance = distance; this.state = state; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val item = detection ?: return
        val colour = when (state) {
            AlertState.COLLISION, AlertState.DANGER -> Color.rgb(232, 76, 79)
            AlertState.WARNING, AlertState.SPEEDING -> Color.rgb(244, 166, 42)
            else -> Color.rgb(24, 190, 133)
        }
        val scale = max(width.toFloat() / item.frameWidth, height.toFloat() / item.frameHeight)
        val dx = (width - item.frameWidth * scale) / 2f
        val dy = (height - item.frameHeight * scale) / 2f
        val box = RectF(item.box.left * scale + dx, item.box.top * scale + dy, item.box.right * scale + dx, item.box.bottom * scale + dy)
        stroke.color = colour
        canvas.drawRoundRect(box, dp(8f), dp(8f), stroke)
        text.color = Color.WHITE; text.textSize = dp(11f)
        val label = context.getString(R.string.vehicle_ahead)
        fill.color = Color.argb(220, 17, 24, 32)
        val labelWidth = text.measureText(label) + dp(18f)
        val top = (box.top - dp(28f)).coerceAtLeast(dp(6f))
        canvas.drawRoundRect(box.left, top, box.left + labelWidth, top + dp(24f), dp(6f), dp(6f), fill)
        canvas.drawText(label, box.left + dp(9f), top + dp(16.5f), text)
        distance?.let {
            val value = "%.0f m".format(it)
            text.textSize = dp(18f)
            val chipWidth = text.measureText(value) + dp(26f)
            val chipLeft = (box.centerX() - chipWidth / 2f).coerceIn(dp(6f), width - chipWidth - dp(6f))
            val chipTop = (box.bottom + dp(8f)).coerceAtMost(height - dp(44f))
            fill.color = colour
            canvas.drawRoundRect(chipLeft, chipTop, chipLeft + chipWidth, chipTop + dp(38f), dp(19f), dp(19f), fill)
            canvas.drawText(value, chipLeft + dp(13f), chipTop + dp(25f), text)
        }
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
