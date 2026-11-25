package com.example.bbsolver

data class Move(val pi: Int, val r: Int, val c: Int, val cells: List<IntArray>, val lines: Int)
class Solution(val moves: List<Move>, val complete: Boolean, val total: Int)

/**
 * Beam search qua mọi thứ tự + vị trí đặt, rồi chấm điểm các trạng thái cuối bằng:
 * số đường xóa (bình phương), ô trống, lỗ hổng, độ gồ ghề, và "khả năng sống sót":
 * bảng còn nhận được bao nhiêu loại mảnh phổ biến ở lượt sau.
 */
object Solver {
    private class Lib(val m: IntArray, val w: Int, val wt: Float)
    private class St(val rows: IntArray, val used: Int, val moves: List<Move>, val lines: Int, var score: Double)

    private fun rot(g: List<String>): List<String> {
        val h = g.size; val w = g[0].length
        return List(w) { c -> String(CharArray(h) { r -> g[h - 1 - r][c] }) }
    }
    private fun masks(g: List<String>) = IntArray(g.size) { r -> g[r].foldIndexed(0) { i, a, ch -> if (ch == '1') a or (1 shl i) else a } }
    private fun width(m: IntArray) = 32 - Integer.numberOfLeadingZeros(m.fold(0) { a, b -> a or b })

    private val LIB: List<Lib> by lazy {
        val base = listOf(
            listOf("11") to 1f, listOf("111") to 2f, listOf("1111") to 3f, listOf("11111") to 4f,
            listOf("11", "11") to 2f, listOf("111", "111", "111") to 4f, listOf("111", "111") to 3f,
            listOf("10", "11") to 2f, listOf("100", "100", "111") to 4f, listOf("100", "111") to 3f,
            listOf("111", "010") to 3f, listOf("011", "110") to 3f, listOf("110", "011") to 3f)
        val out = ArrayList<Lib>()
        for ((s, wt) in base) {
            var g = s; val seen = HashSet<String>()
            repeat(4) { if (seen.add(g.joinToString("/"))) { val m = masks(g); out.add(Lib(m, width(m), wt)) }; g = rot(g) }
        }
        out
    }

    private fun toMasks(cells: List<IntArray>): IntArray {
        val h = cells.maxOf { it[0] } + 1
        val m = IntArray(h)
        for (p in cells) m[p[0]] = m[p[0]] or (1 shl p[1])
        return m
    }

    private fun cheap(rows: IntArray, lines: Int): Double {
        fun g(r: Int, c: Int) = if (r < 0 || c < 0 || r >= 8 || c >= 8) 1 else (rows[r] shr c) and 1
        var empty = 0; var holes = 0; var tr = 0
        for (r in 0 until 8) for (c in 0 until 8) {
            if (g(r, c) == 0) { empty++; if (g(r - 1, c) + g(r + 1, c) + g(r, c - 1) + g(r, c + 1) == 4) holes++ }
            if (g(r, c) != g(r, c + 1)) tr++
            if (g(r, c) != g(r + 1, c)) tr++
        }
        return lines * lines * 10.0 + empty * 0.8 - holes * 6 - tr * 0.5 + (if (empty == 64) 80 else 0)
    }

    private fun count(rows: IntArray, l: Lib): Int {
        val h = l.m.size; var n = 0
        for (r in 0..8 - h) for (c in 0..8 - l.w) {
            var ok = true
            for (i in 0 until h) if ((rows[r + i] and (l.m[i] shl c)) != 0) { ok = false; break }
            if (ok) { n++; if (n >= 6) return n }
        }
        return n
    }

    private fun survival(rows: IntArray): Double {
        var s = 0.0; var tw = 0.0
        for (l in LIB) { s += l.wt * count(rows, l) / 6.0; tw += l.wt }
        return 40.0 * s / tw
    }

    fun solve(board: IntArray, pieces: List<List<IntArray>>): Solution? {
        val ids = pieces.indices.filter { pieces[it].isNotEmpty() }
        if (ids.isEmpty()) return null
        val ms = pieces.map { if (it.isEmpty()) IntArray(0) else toMasks(it) }
        var beam = listOf(St(board.copyOf(), 0, emptyList(), 0, 0.0))
        for (level in ids.indices) {
            val next = HashMap<Int, St>()
            for (s in beam) for (pi in ids) {
                if (((s.used shr pi) and 1) == 1) continue
                val m = ms[pi]; val h = m.size; val w = width(m)
                for (r in 0..8 - h) for (c in 0..8 - w) {
                    var ok = true
                    for (i in 0 until h) if ((s.rows[r + i] and (m[i] shl c)) != 0) { ok = false; break }
                    if (!ok) continue
                    val pre = s.rows.copyOf()
                    for (i in 0 until h) pre[r + i] = pre[r + i] or (m[i] shl c)
                    var colsFull = 255
                    for (x in pre) colsFull = colsFull and x
                    var nl = Integer.bitCount(colsFull)
                    val after = IntArray(8) { i -> if (pre[i] == 255) { nl++; 0 } else pre[i] and colsFull.inv() and 255 }
                    val st = St(after, s.used or (1 shl pi), s.moves + Move(pi, r, c, pieces[pi], nl), s.lines + nl, 0.0)
                    st.score = cheap(after, st.lines)
                    val key = after.contentHashCode() * 31 + st.used
                    val old = next[key]
                    if (old == null || old.score < st.score) next[key] = st
                }
            }
            if (next.isEmpty()) { if (level == 0) return null; break }
            beam = next.values.sortedByDescending { it.score }.take(if (level < ids.size - 1) 90 else 400)
        }
        val best = beam.maxByOrNull { it.score + survival(it.rows) }!!
        return Solution(best.moves, best.moves.size == ids.size, ids.size)
    }
}
