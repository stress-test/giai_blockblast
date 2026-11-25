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
    var lines: List<String> = emptyList()
    var pieceIds: List<Int> = emptyList()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cols = intArrayOf(0xD92F80ED.toInt(), 0xD927AE60.toInt(), 0xD9FF6B2C.toInt())

    override fun onDraw(cv: Canvas) {
        val c = cfg ?: return
        val W = width.toFloat(); val H = height.toFloat()
        val cell = c["bs"]!! * W / 8; val x0 = c["bx"]!! * W; val y0 = c["by"]!! * H
        val rad = cell * 0.18f
        p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT
        // số thứ tự mảnh trên khay
        p.style = Paint.Style.FILL; p.textSize = cell * 0.5f
        for (i in 0 until 3) { p.color = cols[i] or 0xFF000000.toInt(); cv.drawText("${i + 1}", W * (i + .5f) / 3, c["ty"]!! * H + cell * 0.55f, p) }
        // vị trí đặt: viền màu theo mảnh, số = thứ tự đặt
        moves?.forEachIndexed { i, m ->
            for (q in m.cells) {
                val l = x0 + (m.c + q[1]) * cell; val t = y0 + (m.r + q[0]) * cell
                p.style = Paint.Style.FILL; p.color = cols[m.pi % 3]
                cv.drawRoundRect(l + 3, t + 3, l + cell - 3, t + cell - 3, rad, rad, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = Color.WHITE
                cv.drawRoundRect(l + 3, t + 3, l + cell - 3, t + cell - 3, rad, rad, p)
            }
            p.style = Paint.Style.FILL; p.color = Color.WHITE; p.textSize = cell * 0.55f; p.typeface = Typeface.DEFAULT_BOLD
            val f = m.cells[0]
            cv.drawText("${i + 1}", x0 + (m.c + f[1] + .5f) * cell, y0 + (m.r + f[0] + .5f) * cell + p.textSize / 3, p)
        }
        // thẻ thông tin phía trên bảng: tiêu đề + 1 dòng cho mỗi nước đi
        val lh = cell * 0.4f; val ph = cell * 0.35f + lh * (lines.size + 1.6f)
        val top = (y0 - ph - 14f).coerceAtLeast(16f)
        p.style = Paint.Style.FILL; p.color = 0xEB181820.toInt()
        cv.drawRoundRect(x0, top, x0 + 8 * cell, top + ph, cell * 0.25f, cell * 0.25f, p)
        p.color = Color.WHITE; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = cell * 0.36f
        var ty = top + cell * 0.12f + lh * 0.9f
        cv.drawText(title, W / 2, ty, p)
        p.typeface = Typeface.DEFAULT; p.textAlign = Paint.Align.LEFT; p.textSize = cell * 0.3f
        lines.forEachIndexed { i, s ->
            ty += lh
            p.color = cols[pieceIds.getOrElse(i) { 0 } % 3] or 0xFF000000.toInt()
            cv.drawCircle(x0 + cell * 0.35f, ty - p.textSize * 0.3f, cell * 0.09f, p)
            p.color = Color.WHITE; cv.drawText(s, x0 + cell * 0.6f, ty, p)
        }
        p.textAlign = Paint.Align.CENTER; p.color = 0xFFB0B0BC.toInt(); p.textSize = cell * 0.27f
        cv.drawText(sub, W / 2, ty + lh * 0.9f, p)
    }
}
