package com.example.bbsolver

import android.content.Context
import android.graphics.*
import android.view.View

class OverlayView(ctx: Context) : View(ctx) {
    var moves: List<Move>? = null
    var cfg: Map<String, Float>? = null
    var detected: IntArray? = null
    var title = ""
    var sub = ""
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cols = intArrayOf(0xD92F80ED.toInt(), 0xD927AE60.toInt(), 0xD9FF6B2C.toInt())

    override fun onDraw(cv: Canvas) {
        val c = cfg ?: return
        val W = width.toFloat(); val H = height.toFloat()
        val cell = c["bs"]!! * W / 8; val x0 = c["bx"]!! * W; val y0 = c["by"]!! * H
        val rad = cell * 0.18f
        // ô đã đọc được (để kiểm tra nhận diện)
        detected?.let { d ->
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = 0x66FFFFFF
            for (r in 0 until 8) for (k in 0 until 8) if ((d[r] shr k) and 1 == 1)
                cv.drawRoundRect(x0 + k * cell + 4, y0 + r * cell + 4, x0 + (k + 1) * cell - 4, y0 + (r + 1) * cell - 4, rad, rad, p)
        }
        p.textAlign = Paint.Align.CENTER
        p.style = Paint.Style.FILL; p.textSize = cell * 0.5f
        for (i in 0 until 3) { p.color = cols[i] or 0xFF000000.toInt(); cv.drawText("${i + 1}", W * (i + .5f) / 3, c["ty"]!! * H - 10f, p) }
        moves?.forEachIndexed { i, m ->
            for (q in m.cells) {
                val l = x0 + (m.c + q[1]) * cell; val t = y0 + (m.r + q[0]) * cell
                p.style = Paint.Style.FILL; p.color = cols[m.pi % 3]
                cv.drawRoundRect(l + 3, t + 3, l + cell - 3, t + cell - 3, rad, rad, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = Color.WHITE
                cv.drawRoundRect(l + 3, t + 3, l + cell - 3, t + cell - 3, rad, rad, p)
            }
            p.style = Paint.Style.FILL; p.color = Color.WHITE; p.textSize = cell * 0.55f
            val f = m.cells[0]
            cv.drawText("${i + 1}", x0 + (m.c + f[1] + .5f) * cell, y0 + (m.r + f[0] + .5f) * cell + p.textSize / 3, p)
        }
        // thẻ thông tin phía trên bảng
        val ph = cell * 1.25f; val top = (y0 - ph - 16f).coerceAtLeast(24f)
        p.style = Paint.Style.FILL; p.color = 0xE6181820.toInt()
        cv.drawRoundRect(x0, top, x0 + 8 * cell, top + ph, ph / 3, ph / 3, p)
        p.color = Color.WHITE; p.textSize = cell * 0.42f; p.typeface = Typeface.DEFAULT_BOLD
        cv.drawText(title, W / 2, top + ph * 0.45f, p)
        p.typeface = Typeface.DEFAULT; p.color = 0xFFB0B0BC.toInt(); p.textSize = cell * 0.33f
        cv.drawText(sub, W / 2, top + ph * 0.82f, p)
    }
}
