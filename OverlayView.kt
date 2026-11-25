package com.example.bbsolver

import android.content.Context
import android.graphics.*
import android.view.View

/**
 * High-contrast Liquid Glass guidance layer.
 * Geometry is normalized against the captured frame, then mapped independently
 * to this overlay window so differing display/window dimensions do not shift moves.
 */
class OverlayView(ctx: Context) : View(ctx) {
    var moves: List<Move>? = null
    var cfg: Map<String, Float>? = null
    var detected: IntArray? = null
    var title = ""
    var sub = ""
    var lines: List<String> = emptyList()
    var pieceIds: List<Int> = emptyList()

    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val d = resources.displayMetrics.density
    private val colors = intArrayOf(0xFF62A8FF.toInt(), 0xFF55D6B0.toInt(), 0xFFFF9A70.toInt())
    private fun dp(v: Float) = v * d

    override fun onDraw(cv: Canvas) {
        val c = cfg ?: return
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return

        val sourceW = (c["sw"] ?: viewW).coerceAtLeast(1f)
        val sourceH = (c["sh"] ?: viewH).coerceAtLeast(1f)
        val sx = viewW / sourceW
        val sy = viewH / sourceH

        val cell = c["bs"]!! * sourceW / 8f * sx
        val cellY = c["bs"]!! * sourceW / 8f * sy
        val x0 = c["bx"]!! * sourceW * sx
        val y0 = c["by"]!! * sourceH * sy
        val radius = cell * 0.17f
        val inset = dp(2f)

        // Fine guide grid gives the eye a stable reference without obscuring the game.
        p.style = Paint.Style.STROKE
        p.strokeWidth = dp(0.8f)
        p.color = 0x45FFFFFF
        for (i in 0..8) {
            val x = x0 + i * cell
            val y = y0 + i * cellY
            cv.drawLine(x, y0, x, y0 + 8 * cellY, p)
            cv.drawLine(x0, y, x0 + 8 * cell, y, p)
        }

        // Highlight every destination cell; sequence numbers are shown as separate badges.
        moves?.forEachIndexed { index, move ->
            val color = colors[move.pi % colors.size]
            move.cells.forEach { q ->
                val l = x0 + (move.c + q[1]) * cell
                val t = y0 + (move.r + q[0]) * cellY
                val rect = RectF(l + inset, t + inset, l + cell - inset, t + cellY - inset)
                p.style = Paint.Style.FILL
                p.color = 0x493B9BFF
                p.setShadowLayer(dp(8f), 0f, dp(2f), color)
                cv.drawRoundRect(rect, radius, radius, p)
                p.clearShadowLayer()
                p.color = (0xA0000000.toInt() or (color and 0x00FFFFFF))
                cv.drawRoundRect(rect, radius, radius, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = dp(1.8f)
                p.color = 0xFFF7FBFF.toInt()
                cv.drawRoundRect(rect, radius, radius, p)
                p.style = Paint.Style.FILL
            }

            // Number badge is anchored to the first occupied cell, never lost in the fill.
            val first = move.cells.minWithOrNull(compareBy<IntArray> { it[0] }.thenBy { it[1] })
            if (first != null) {
                val cx = x0 + (move.c + first[1] + 0.5f) * cell
                val cy = y0 + (move.r + first[0] + 0.5f) * cellY
                val br = minOf(cell, cellY) * 0.29f
                p.color = color
                cv.drawCircle(cx, cy, br, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = dp(2f)
                p.color = Color.WHITE
                cv.drawCircle(cx, cy, br, p)
                p.style = Paint.Style.FILL
                p.textAlign = Paint.Align.CENTER
                p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                p.textSize = br * 1.25f
                p.color = Color.WHITE
                cv.drawText("${index + 1}", cx, cy - (p.ascent() + p.descent()) / 2f, p)
            }
        }

        drawGlassPanel(cv, viewW, y0, cell, cellY)
    }

    private fun drawGlassPanel(cv: Canvas, viewW: Float, boardTop: Float, cell: Float, cellY: Float) {
        val count = maxOf(1, lines.size)
        val panelW = minOf(viewW - dp(24f), cell * 8f)
        val left = (viewW - panelW) / 2f
        val pad = dp(13f)
        val titleSize = dp(14f)
        val rowSize = dp(12.5f)
        val rowH = dp(21f)
        val panelH = pad * 2 + titleSize + dp(7f) + count * rowH + dp(3f)
        val top = (boardTop - panelH - dp(10f)).coerceAtLeast(dp(10f))
        val rect = RectF(left, top, left + panelW, top + panelH)

        p.style = Paint.Style.FILL
        p.color = 0xE8172038.toInt()
        p.setShadowLayer(dp(18f), 0f, dp(5f), 0x66000000)
        cv.drawRoundRect(rect, dp(20f), dp(20f), p)
        p.clearShadowLayer()
        p.color = 0xB82D4263.toInt()
        cv.drawRoundRect(rect, dp(20f), dp(20f), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = dp(1f)
        p.color = 0x77FFFFFF
        cv.drawRoundRect(rect, dp(20f), dp(20f), p)

        // Small glass highlight at the top edge.
        p.style = Paint.Style.STROKE
        p.strokeWidth = dp(1.2f)
        p.color = 0x66FFFFFF
        cv.drawLine(left + dp(22f), top + dp(1f), left + panelW - dp(22f), top + dp(1f), p)

        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        p.textSize = titleSize
        p.color = Color.WHITE
        cv.drawText(title.ifBlank { "GỢI Ý NƯỚC ĐI" }, left + pad, top + pad + titleSize, p)

        var y = top + pad + titleSize + dp(7f)
        p.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        p.textSize = rowSize
        lines.forEachIndexed { i, line ->
            val id = pieceIds.getOrElse(i) { 0 }.coerceIn(0, 2)
            p.color = colors[id]
            cv.drawCircle(left + pad + dp(4f), y - dp(4f), dp(3.5f), p)
            p.color = 0xFFF1F5FF.toInt()
            cv.drawText(line, left + pad + dp(15f), y, p)
            y += rowH
        }
        p.textAlign = Paint.Align.RIGHT
        p.textSize = dp(10.5f)
        p.color = 0xFFB8C7DF.toInt()
        cv.drawText(sub, left + panelW - pad, top + panelH - dp(8f), p)
    }
}
