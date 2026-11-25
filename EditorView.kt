package com.example.bbsolver

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

/** Bảng sửa nhanh: chạm ô để bật/tắt nếu app đọc sai, rồi giải lại. */
class EditorView(ctx: Context, board: IntArray, pieces: List<List<IntArray>>,
                 private val onSolve: (IntArray, List<List<IntArray>>) -> Unit, private val onCancel: () -> Unit) : View(ctx) {
    private val b = Array(8) { r -> BooleanArray(8) { k -> ((board[r] shr k) and 1) == 1 } }
    private val pg = Array(3) { i -> Array(5) { BooleanArray(5) }.also { g -> pieces[i].forEach { if (it[0] < 5 && it[1] < 5) g[it[0]][it[1]] = true } } }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pc = intArrayOf(0xFF2F80ED.toInt(), 0xFF27AE60.toInt(), 0xFFFF6B2C.toInt())
    private var cell = 0f; private var ox = 0f; private var oy = 0f; private var qc = 0f; private var qy = 0f
    private val bSolve = RectF(); private val bCancel = RectF()

    private fun layout() {
        val W = width.toFloat(); val H = height.toFloat()
        cell = minOf(W * 0.9f / 8, H * 0.36f / 8); ox = (W - 8 * cell) / 2; oy = H * 0.12f
        qc = W / 3 * 0.8f / 5; qy = oy + 8 * cell + cell * 1.3f
        val by = qy + 5 * qc + cell * 0.8f; val bh = cell * 1.1f
        bCancel.set(W * 0.06f, by, W * 0.48f, by + bh); bSolve.set(W * 0.52f, by, W * 0.94f, by + bh)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_DOWN) return true
        layout()
        val x = e.x; val y = e.y; val W = width.toFloat()
        if (bSolve.contains(x, y)) {
            val rows = IntArray(8) { r -> var m = 0; for (k in 0 until 8) if (b[r][k]) m = m or (1 shl k); m }
            val ps = pg.map { g ->
                val a = ArrayList<IntArray>(); for (r in 0 until 5) for (k in 0 until 5) if (g[r][k]) a.add(intArrayOf(r, k))
                if (a.isEmpty()) a else { val mr = a.minOf { it[0] }; val mk = a.minOf { it[1] }; a.map { intArrayOf(it[0] - mr, it[1] - mk) } }
            }
            onSolve(rows, ps)
        } else if (bCancel.contains(x, y)) onCancel()
        else {
            val c = ((x - ox) / cell).toInt(); val r = ((y - oy) / cell).toInt()
            if (x >= ox && y >= oy && c in 0..7 && r in 0..7) b[r][c] = !b[r][c]
            for (i in 0 until 3) {
                val px = W * (i + .5f) / 3 - 2.5f * qc
                val k = ((x - px) / qc).toInt(); val rr = ((y - qy) / qc).toInt()
                if (x >= px && y >= qy && k in 0..4 && rr in 0..4) pg[i][rr][k] = !pg[i][rr][k]
            }
        }
        invalidate(); return true
    }

    override fun onDraw(cv: Canvas) {
        layout(); val W = width.toFloat()
        cv.drawColor(0xF2101016.toInt())
        p.textAlign = Paint.Align.CENTER; p.style = Paint.Style.FILL; p.color = Color.WHITE; p.textSize = cell * 0.5f
        cv.drawText("Chạm ô để sửa nhận diện", W / 2, oy - cell * 0.35f, p)
        for (r in 0 until 8) for (k in 0 until 8) {
            p.color = if (b[r][k]) 0xFF8A8F98.toInt() else 0xFF2A2A34.toInt()
            cv.drawRoundRect(ox + k * cell + 2, oy + r * cell + 2, ox + (k + 1) * cell - 2, oy + (r + 1) * cell - 2, 8f, 8f, p)
        }
        for (i in 0 until 3) {
            val px = W * (i + .5f) / 3 - 2.5f * qc
            p.color = pc[i]; p.textSize = qc * 0.9f; cv.drawText("${i + 1}", W * (i + .5f) / 3, qy - 8f, p)
            for (r in 0 until 5) for (k in 0 until 5) {
                p.color = if (pg[i][r][k]) pc[i] else 0xFF2A2A34.toInt()
                cv.drawRoundRect(px + k * qc + 1.5f, qy + r * qc + 1.5f, px + (k + 1) * qc - 1.5f, qy + (r + 1) * qc - 1.5f, 6f, 6f, p)
            }
        }
        p.color = 0xFF3A3A46.toInt(); cv.drawRoundRect(bCancel, 30f, 30f, p)
        p.color = 0xFFFF6B2C.toInt(); cv.drawRoundRect(bSolve, 30f, 30f, p)
        p.color = Color.WHITE; p.textSize = cell * 0.45f
        cv.drawText("Hủy", bCancel.centerX(), bCancel.centerY() + p.textSize / 3, p)
        cv.drawText("Giải lại", bSolve.centerX(), bSolve.centerY() + p.textSize / 3, p)
    }
}
