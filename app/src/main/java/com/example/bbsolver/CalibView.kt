package com.example.bbsolver

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

/** Căn chỉnh 4 chạm: góc trên-trái & dưới-phải của bảng, mép trên & dưới của khay mảnh. */
class CalibView(ctx: Context, private val onDone: (List<PointF>) -> Unit) : View(ctx) {
    private val pts = ArrayList<PointF>()
    private val steps = listOf("Chạm GÓC TRÊN-TRÁI của bảng 8×8", "Chạm GÓC DƯỚI-PHẢI của bảng 8×8",
        "Chạm MÉP TRÊN của khay 3 mảnh", "Chạm MÉP DƯỚI của khay 3 mảnh")
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP && pts.size < 4) {
            pts.add(PointF(e.rawX, e.rawY)); invalidate()
            if (pts.size == 4) onDone(pts)
        }
        return true
    }

    override fun onDraw(cv: Canvas) {
        val W = width.toFloat()
        cv.drawColor(0x55000000)
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = 0xFFFF6B2C.toInt()
        pts.forEach { cv.drawLine(it.x - 40, it.y, it.x + 40, it.y, p); cv.drawLine(it.x, it.y - 40, it.x, it.y + 40, p) }
        if (pts.size >= 2) cv.drawRect(pts[0].x, pts[0].y, pts[1].x, pts[1].y, p)
        if (pts.size >= 3) cv.drawLine(0f, pts[2].y, W, pts[2].y, p)
        if (pts.size >= 4) cv.drawLine(0f, pts[3].y, W, pts[3].y, p)
        val msg = if (pts.size < 4) "Bước ${pts.size + 1}/4 · ${steps[pts.size]}" else "Xong"
        p.style = Paint.Style.FILL; p.color = 0xEE181820.toInt()
        cv.drawRoundRect(24f, 90f, W - 24f, 230f, 40f, 40f, p)
        p.color = Color.WHITE; p.textAlign = Paint.Align.CENTER; p.textSize = 44f
        cv.drawText(msg, W / 2, 175f, p)
    }
}
