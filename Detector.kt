package com.example.bbsolver

import android.content.Context

object Cfg {
    // bx,bs theo chiều rộng màn hình; by,ty,th theo chiều cao. ts: ô khay/ô bảng. vt: ngưỡng dự phòng. bias: độ nhạy.
    val DEF = linkedMapOf("bx" to 0.056f, "by" to 0.241f, "bs" to 0.889f, "ty" to 0.70f, "th" to 0.12f,
        "ts" to 0.45f, "vt" to 0.50f, "bias" to 0.0f, "auto" to 1.0f)
    fun load(c: Context): Map<String, Float> { val p = c.getSharedPreferences("cfg", 0); return DEF.mapValues { p.getFloat(it.key, it.value) } }
    fun calibrated(c: Context) = c.getSharedPreferences("cfg", 0).getBoolean("calib", false)
    fun reset(c: Context) = c.getSharedPreferences("cfg", 0).edit().clear().apply()
}

/** Ảnh màn hình dạng byte thô RGBA (không tạo Bitmap, không getPixel → nhanh, ít rác bộ nhớ). */
class Frame(val buf: ByteArray, val stride: Int, val W: Int, val H: Int) {
    fun rgb(x: Int, y: Int): Int {
        val i = y.coerceIn(0, H - 1) * stride + x.coerceIn(0, W - 1) * 4
        return ((buf[i].toInt() and 255) shl 16) or ((buf[i + 1].toInt() and 255) shl 8) or (buf[i + 2].toInt() and 255)
    }
}

class Scan(val board: IntArray, val pieces: List<List<IntArray>>, val geo: Map<String, Float>) {
    fun key() = board.joinToString(",") + "|" + pieces.joinToString("/") { p -> p.joinToString(";") { "${it[0]}${it[1]}" } }
}

object Detector {
    private fun score(c: Int): Float {
        val r = (c shr 16) and 255; val g = (c shr 8) and 255; val b = c and 255
        val mx = maxOf(r, g, b); val mn = minOf(r, g, b)
        val s = if (mx == 0) 0f else (mx - mn).toFloat() / mx
        return mx / 255f * (0.3f + s)
    }
    private fun avgScore(f: Frame, cx: Float, cy: Float, d: Float): Float {
        var t = 0f
        for (dy in -1..1) for (dx in -1..1) t += score(f.rgb((cx + dx * d).toInt(), (cy + dy * d).toInt()))
        return t / 9f
    }
    /** Tự tìm ngưỡng tách ô trống / ô có khối (Otsu) → chạy tốt với mọi theme. */
    private fun otsu(v: FloatArray, fallback: Float): Float {
        val s = v.sortedArray()
        if (s.last() - s.first() < 0.25f) return fallback
        val n = s.size; val tot = s.sum(); var acc = 0f; var bestV = -1f; var bestT = fallback
        for (i in 1 until n) {
            acc += s[i - 1]
            val m1 = acc / i; val m2 = (tot - acc) / (n - i)
            val vr = i.toFloat() * (n - i) * (m1 - m2) * (m1 - m2)
            if (vr > bestV) { bestV = vr; bestT = (s[i - 1] + s[i]) / 2 }
        }
        return bestT
    }

    /** Tự dò vị trí bảng 8×8: các ô trống + khe giữa các ô là màu tối, tạo thành khung vuông. Không được thì dùng căn chỉnh tay. */
    fun geometry(f: Frame, c: Map<String, Float>): Map<String, Float> {
        if (c["auto"]!! < 0.5f) return c + mapOf("sw" to f.W.toFloat(), "sh" to f.H.toFloat())
        fun dark(p: Int): Boolean { val r = (p shr 16) and 255; val g = (p shr 8) and 255; val b = p and 255; return maxOf(r, g, b) < 80 && b >= r }
        val cc = IntArray(f.W / 4 + 1); val rc = IntArray(f.H / 4 + 1)
        var y = (f.H * 0.10f).toInt(); val ye = (f.H * 0.80f).toInt()
        while (y < ye) { var x = 0; while (x < f.W) { if (dark(f.rgb(x, y))) { cc[x / 4]++; rc[y / 4]++ }; x += 4 }; y += 4 }
        val mc = cc.max(); val mr = rc.max()
        if (mc < 20 || mr < 20) return c + mapOf("sw" to f.W.toFloat(), "sh" to f.H.toFloat())
        val xs = cc.indices.filter { cc[it] >= mc * 0.5f }; val ys = rc.indices.filter { rc[it] >= mr * 0.5f }
        val left = xs.first() * 4; val right = xs.last() * 4 + 3; val top = ys.first() * 4; val bot = ys.last() * 4 + 3
        val w = right - left + 1; val h = bot - top + 1
        if (w < f.W * 0.5f || w > f.W || Math.abs(w - h) > w * 0.05f)
            return c + mapOf("sw" to f.W.toFloat(), "sh" to f.H.toFloat())
        return c + mapOf("bx" to left.toFloat() / f.W, "by" to top.toFloat() / f.H, "bs" to w.toFloat() / f.W,
            "ty" to (bot + 0.04f * f.H) / f.H, "th" to 0.20f,
            "sw" to f.W.toFloat(), "sh" to f.H.toFloat())
    }

    fun board(f: Frame, c: Map<String, Float>): IntArray {
        val cell = c["bs"]!! * f.W / 8; val x0 = c["bx"]!! * f.W; val y0 = c["by"]!! * f.H; val d = cell * 0.18f
        val sc = FloatArray(64) { avgScore(f, x0 + (it % 8 + .5f) * cell, y0 + (it / 8 + .5f) * cell, d) }
        val thr = otsu(sc, c["vt"]!!) + c["bias"]!!
        return IntArray(8) { r -> var m = 0; for (k in 0 until 8) if (sc[r * 8 + k] > thr) m = m or (1 shl k); m }
    }

    fun pieces(f: Frame, c: Map<String, Float>): List<List<IntArray>> {
        val ideal = c["bs"]!! * f.W / 8 * c["ts"]!!
        val ty = (c["ty"]!! * f.H).toInt(); val th = (c["th"]!! * f.H).toInt()
        val rs = ArrayList<Int>(); val gs = ArrayList<Int>(); val bs = ArrayList<Int>()
        var yy = ty
        while (yy < ty + th) { var xx = 0; while (xx < f.W) { val p = f.rgb(xx, yy); rs.add((p shr 16) and 255); gs.add((p shr 8) and 255); bs.add(p and 255); xx += 12 }; yy += 12 }
        rs.sort(); gs.sort(); bs.sort()
        val br = rs[rs.size / 2]; val bg = gs[gs.size / 2]; val bb = bs[bs.size / 2]   // màu nền khay = trung vị
        fun fg(p: Int): Boolean {
            val r = (p shr 16) and 255; val g = (p shr 8) and 255; val b = p and 255
            val dr = r - br; val dg = g - bg; val db = b - bb
            return dr * dr + dg * dg + db * db > 48 * 48 && maxOf(r, g, b) > 90
        }
        return (0 until 3).map { i ->
            val xa = f.W * i / 3; val xb = f.W * (i + 1) / 3
            var minX = Int.MAX_VALUE; var maxX = -1; var minY = Int.MAX_VALUE; var maxY = -1
            var y = ty
            while (y < ty + th) {
                var x = xa
                while (x < xb) { if (fg(f.rgb(x, y))) { minX = minOf(minX, x); maxX = maxOf(maxX, x); minY = minOf(minY, y); maxY = maxOf(maxY, y) }; x += 3 }
                y += 3
            }
            if (maxX < 0) return@map emptyList<IntArray>()
            val bw = (maxX - minX + 3).toFloat(); val bh = (maxY - minY + 3).toFloat()
            var bestS = 0f; var bestD = Float.MAX_VALUE
            for (k in 1..5) {
                val s = bw / k; val rr = Math.round(bh / s)
                if (rr < 1 || rr > 5 || Math.abs(bh - rr * s) > s * 0.3f) continue
                val dd = Math.abs(s - ideal)
                if (dd < bestD) { bestD = dd; bestS = s }
            }
            if (bestS == 0f) return@map emptyList<IntArray>()
            val cols = Math.round(bw / bestS); val rows = Math.round(bh / bestS)
            val out = ArrayList<IntArray>()
            for (r in 0 until rows) for (k in 0 until cols) {
                val cx = minX + (k + .5f) * bestS; val cy = minY + (r + .5f) * bestS
                var hit = 0
                for (dy in -1..1) for (dx in -1..1) if (fg(f.rgb((cx + dx * bestS * .2f).toInt(), (cy + dy * bestS * .2f).toInt()))) hit++
                if (hit >= 5) out.add(intArrayOf(r, k))
            }
            out
        }
    }
}
