package com.ailenezareti.nezaretv4.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class AnalyticsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    var incoming = 0
    var outgoing = 0
    var missed = 0
    var battery: List<Int> = emptyList()
    var mode = "calls"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mode == "calls") drawCalls(canvas) else drawBattery(canvas)
    }

    private fun drawCalls(canvas: Canvas) {
        val realTotal = incoming + outgoing + missed
        val drawTotal = realTotal.coerceAtLeast(1)
        val diameter = min(width, height) * 0.62f
        val cx = width / 2f
        val cy = height / 2f
        val rect = RectF(cx - diameter / 2, cy - diameter / 2, cx + diameter / 2, cy + diameter / 2)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 15f
        paint.strokeCap = Paint.Cap.BUTT
        if (realTotal == 0) {
            paint.color = Color.rgb(232, 234, 241)
            canvas.drawArc(rect, -90f, 360f, false, paint)
        } else {
            var start = -90f
            listOf(
                incoming to Color.rgb(109, 76, 246),
                outgoing to Color.rgb(24, 201, 154),
                missed to Color.rgb(242, 93, 101)
            ).forEach { (value, color) ->
                if (value > 0) {
                    val sweep = 360f * value / drawTotal
                    paint.color = color
                    canvas.drawArc(rect, start, sweep, false, paint)
                    start += sweep
                }
            }
        }

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(122, 129, 146)
        paint.textSize = 12f * resources.displayMetrics.scaledDensity
        canvas.drawText("Cəmi", cx, cy - 5f, paint)
        paint.color = Color.rgb(23, 25, 39)
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 25f * resources.displayMetrics.scaledDensity
        canvas.drawText(realTotal.toString(), cx, cy + 28f, paint)
        paint.typeface = Typeface.DEFAULT
    }

    private fun drawBattery(canvas: Canvas) {
        if (battery.isEmpty()) return
        val left = 34f
        val right = width - 14f
        val top = 18f
        val bottom = height - 26f

        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(232, 234, 241)
        for (pct in listOf(0, 50, 100)) {
            val y = bottom - (pct / 100f) * (bottom - top)
            canvas.drawLine(left, y, right, y, paint)
        }

        val path = Path()
        battery.forEachIndexed { index, value ->
            val x = if (battery.size == 1) left else left + index.toFloat() / (battery.size - 1) * (right - left)
            val y = bottom - (value.coerceIn(0, 100) / 100f) * (bottom - top)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = Color.rgb(109, 76, 246)
        canvas.drawPath(path, paint)
    }
}
