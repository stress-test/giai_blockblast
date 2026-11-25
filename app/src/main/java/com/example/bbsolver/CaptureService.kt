package com.example.bbsolver

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CaptureService : Service() {
    private lateinit var wm: WindowManager
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vd: VirtualDisplay? = null
    private var overlay: OverlayView? = null
    private var bar: LinearLayout? = null
    private var btns: LinearLayout? = null
    private lateinit var goBtn: TextView
    private var calib: CalibView? = null
    private var editor: EditorView? = null
    private val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    private val barLp = WindowManager.LayoutParams(-2, -2, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
    private val h = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    private var W = 0; private var H = 0
    private var lastFrame: Frame? = null
    private var lastScan: Scan? = null

    override fun onBind(i: Intent?): IBinder? = null

    private fun fullLp(touch: Boolean) = WindowManager.LayoutParams(-1, -1, type,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or (if (touch) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT).apply {
        if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("bb", "BB Solver", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "bb").setContentTitle("Block Blast Solver đang chạy").setContentText("Mở game và bấm GIẢI").setSmallIcon(android.R.drawable.ic_menu_search).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(1, n)
        val data = i?.getParcelableExtra<Intent>("data")
        if (data == null) { stopSelf(); return START_NOT_STICKY }
        projection = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(i.getIntExtra("code", 0), data)
        projection!!.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, h)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics(); wm.defaultDisplay.getRealMetrics(dm)
        W = dm.widthPixels; H = dm.heightPixels
        reader = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 2)
        vd = projection!!.createVirtualDisplay("bb", W, H, dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, null)
        overlay = OverlayView(this).also { wm.addView(it, fullLp(false)) }
        buildBar()
        getSharedPreferences("cfg", 0).edit().putBoolean("running", true).apply()
        toast(if (Cfg.calibrated(this)) "Sẵn sàng · mở game và bấm GIẢI" else "Lần đầu: bấm ⌖ để căn chỉnh bảng")
        return START_NOT_STICKY
    }

    private fun pill(c: Int, r: Float) = GradientDrawable().apply { setColor(c); cornerRadius = r }

    private fun buildBar() {
        val b = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = pill(0xE0181820.toInt(), 90f); setPadding(10, 8, 10, 8) }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun btn(t: String, col: Int, act: () -> Unit) = TextView(this).apply {
            text = t; textSize = 15f; setTextColor(Color.WHITE); setPadding(30, 16, 30, 16); background = pill(col, 60f)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(5, 0, 5, 0) }; setOnClickListener { act() }
        }
        goBtn = btn("▶ GIẢI", 0xFFFF6B2C.toInt()) { scan() }
        row.addView(goBtn); row.addView(btn("✎", 0xFF3A3A46.toInt()) { edit() })
        row.addView(btn("⌖", 0xFF3A3A46.toInt()) { calibrate() }); row.addView(btn("✕", 0xFF3A3A46.toInt()) { stopSelf() })
        val handle = TextView(this).apply { text = "BB"; textSize = 14f; setTextColor(0xFFFF6B2C.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(22, 12, 22, 12) }
        handle.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = barLp.x; oy = barLp.y; moved = false }
                    MotionEvent.ACTION_MOVE -> {
                        if (Math.abs(e.rawX - sx) + Math.abs(e.rawY - sy) > 14) moved = true
                        if (moved) { barLp.x = ox + (e.rawX - sx).toInt(); barLp.y = oy + (e.rawY - sy).toInt(); wm.updateViewLayout(b, barLp) }
                    }
                    MotionEvent.ACTION_UP -> if (!moved) row.visibility = if (row.visibility == View.VISIBLE) View.GONE else View.VISIBLE   // chạm BB để thu gọn
                }
                return true
            }
        })
        b.addView(handle); b.addView(row)
        barLp.gravity = Gravity.TOP or Gravity.START; barLp.x = 16; barLp.y = H / 2
        bar = b; btns = row; wm.addView(b, barLp)
    }

    private fun idle() { busy.set(false); goBtn.text = "▶ GIẢI"; overlay?.visibility = View.VISIBLE; bar?.visibility = View.VISIBLE }

    private fun grabFrame(): Frame? {
        val img = reader?.acquireLatestImage() ?: return lastFrame
        try {
            val pl = img.planes[0]; val stride = pl.rowStride; val bb = pl.buffer
            val arr = ByteArray(stride * H); bb.get(arr, 0, minOf(bb.remaining(), arr.size))
            return Frame(arr, stride, W, H).also { lastFrame = it }
        } finally { img.close() }
    }

    /** Quét tối đa 3 lần, dừng khi hai lần liên tiếp cho cùng kết quả (tránh hoạt ảnh / hiệu ứng xóa hàng). */
    private fun capture(cfg: Map<String, Float>): Scan? {
        Thread.sleep(170)   // chờ lớp phủ biến mất khỏi khung hình
        var prev: Scan? = null
        repeat(3) {
            val f = grabFrame()
            if (f != null) {
                val cur = Scan(Detector.board(f, cfg), Detector.pieces(f, cfg))
                if (prev != null && prev!!.key() == cur.key()) return cur
                prev = cur
            }
            Thread.sleep(130)
        }
        return prev
    }

    private fun scan() {
        if (!busy.compareAndSet(false, true)) return
        goBtn.text = "…"; overlay?.visibility = View.INVISIBLE; bar?.visibility = View.INVISIBLE
        val cfg = Cfg.load(this)
        worker.execute {
            try {
                val sc = capture(cfg)
                if (sc == null) { h.post { toast("Chưa chụp được màn hình, thử lại"); idle() }; return@execute }
                solveAndShow(sc, cfg)
            } catch (e: Throwable) { h.post { toast("Lỗi: ${e.message}"); idle() } }
        }
    }

    private fun solveAndShow(sc: Scan, cfg: Map<String, Float>) {
        val t = SystemClock.elapsedRealtime()
        val sol = Solver.solve(sc.board, sc.pieces)
        val ms = SystemClock.elapsedRealtime() - t
        h.post {
            lastScan = sc
            overlay?.let { ov ->
                ov.cfg = cfg; ov.moves = sol?.moves; ov.detected = sc.board
                when {
                    sol == null -> { ov.title = "Không có nước đi"; ov.sub = "Chưa đọc được mảnh · bấm ✎ để sửa hoặc ⌖ căn chỉnh" }
                    !sol.complete -> { ov.title = "Chỉ đặt được ${sol.moves.size}/${sol.total} mảnh"; ov.sub = "Sắp hết chỗ · đây là nước kéo dài nhất" }
                    else -> { ov.title = "Thứ tự: " + sol.moves.joinToString(" → ") { "mảnh ${it.pi + 1}" }; ov.sub = "Xóa ${sol.moves.sumOf { it.lines }} đường · ${ms} ms" }
                }
                ov.invalidate()
            }
            idle()
        }
    }

    private fun edit() {
        val s = lastScan
        if (s == null) { toast("Hãy bấm GIẢI trước"); return }
        if (busy.get() || editor != null) return
        overlay?.visibility = View.INVISIBLE; bar?.visibility = View.GONE
        val v = EditorView(this, s.board, s.pieces, { rows, ps ->
            h.post { closeEditor(); busy.set(true); goBtn.text = "…"; val cfg = Cfg.load(this)
                worker.execute { try { solveAndShow(Scan(rows, ps), cfg) } catch (e: Throwable) { h.post { toast("Lỗi: ${e.message}"); idle() } } } }
        }, { h.post { closeEditor(); idle() } })
        editor = v; wm.addView(v, fullLp(true))
    }

    private fun closeEditor() { editor?.let { runCatching { wm.removeView(it) } }; editor = null }

    private fun calibrate() {
        if (busy.get() || calib != null) return
        bar?.visibility = View.GONE; overlay?.visibility = View.INVISIBLE
        val v = CalibView(this) { pts ->
            h.post {
                calib?.let { runCatching { wm.removeView(it) } }; calib = null; idle()
                val a = pts[0]; val b = pts[1]; val c = pts[2]; val d = pts[3]
                if (b.x <= a.x || b.y <= a.y || d.y <= c.y) toast("Căn chỉnh sai thứ tự, thử lại")
                else {
                    getSharedPreferences("cfg", 0).edit().putFloat("bx", a.x / W).putFloat("by", a.y / H).putFloat("bs", (b.x - a.x) / W)
                        .putFloat("ty", c.y / H).putFloat("th", (d.y - c.y) / H).putBoolean("calib", true).apply()
                    toast("Đã lưu căn chỉnh ✓")
                }
            }
        }
        calib = v; wm.addView(v, fullLp(true))
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        getSharedPreferences("cfg", 0).edit().putBoolean("running", false).apply()
        worker.shutdownNow()
        listOfNotNull(overlay, bar, calib, editor).forEach { runCatching { wm.removeView(it) } }
        vd?.release(); reader?.close(); projection?.stop()
        super.onDestroy()
    }
}
