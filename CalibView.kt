package com.example.bbsolver

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

/** Four-point calibration. Touch coordinates are local to this full-screen overlay. */
class CalibView(ctx: Context, private val onDone: (List<PointF>) -> Unit) : View(ctx) {
    private val pts = ArrayList<PointF>()
    private val steps = listOf(
        "Góc trên-trái của bảng 8 × 8",
        "Góc dưới-phải của bảng 8 × 8",
        "Mép trên của khay 3 mảnh",
        "Mép dưới của khay 3 mảnh"
    )
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP && pts.size < 4) {
            // IMPORTANT: use local coordinates; rawX/rawY include window/screen offsets.
            pts.add(PointF(e.x, e.y))
            invalidate()
            if (pts.size == 4) onDone(pts.toList())
        }
        return true
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        cv.drawColor(0x66070B18)
        p.style = Paint.Style.FILL
        p.color = 0xE51A2340.toInt()
        cv.drawRoundRect(dp(16f), dp(38f), w - dp(16f), dp(124f), dp(24f), dp(24f), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = dp(1.5f)
        p.color = 0x99FFFFFF.toInt()
        cv.drawRoundRect(dp(16f), dp(38f), w - dp(16f), dp(124f), dp(24f), dp(24f), p)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.color = Color.WHITE
        p.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        p.textSize = dp(16f)
        cv.drawText("CĂN CHỈNH CHÍNH XÁC", w / 2, dp(68f), p)
        p.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        p.textSize = dp(13f)
        val msg = if (pts.size < 4) "BƯỚC ${pts.size + 1}/4  ·  ${steps[pts.size]}" else "Đã hoàn tất"
        cv.drawText(msg, w / 2, dp(98f), p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = dp(2f)
        p.color = 0xFF8BD8FF.toInt()
        pts.forEach { q ->
            cv.drawCircle(q.x, q.y, dp(15f), p)
            cv.drawLine(q.x - dp(25f), q.y, q.x + dp(25f), q.y, p)
            cv.drawLine(q.x, q.y - dp(25f), q.x, q.y + dp(25f), p)
        }
        if (pts.size >= 2) cv.drawRect(pts[0].x, pts[0].y, pts[1].x, pts[1].y, p)
        if (pts.size >= 3) cv.drawLine(0f, pts[2].y, w, pts[2].y, p)
        if (pts.size >= 4) cv.drawLine(0f, pts[3].y, w, pts[3].y, p)

        // Progress rail
        val left = dp(28f); val right = w - dp(28f); val y = dp(145f)
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(4f); p.strokeCap = Paint.Cap.ROUND
        p.color = 0x55FFFFFF
        cv.drawLine(left, y, right, y, p)
        p.color = 0xFF65D6C5.toInt()
        cv.drawLine(left, y, left + (right - left) * (pts.size / 4f), y, p)
    }
}
