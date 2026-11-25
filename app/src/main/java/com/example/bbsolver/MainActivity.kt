package com.example.bbsolver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val BG = 0xFF14141A.toInt(); private val CARD = 0xFF202028.toInt(); private val ORANGE = 0xFFFF6B2C.toInt(); private val GRAY = 0xFF9A9AA6.toInt()
    private lateinit var status: TextView
    private lateinit var go: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun round(c: Int, r: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(r).toFloat() }
    private fun text(t: String, size: Float, col: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(col); if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun card(vararg v: View) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = round(CARD, 16); setPadding(dp(18), dp(16), dp(18), dp(16))
        v.forEach { addView(it) }
    }
    private fun running() = getSharedPreferences("cfg", 0).getBoolean("running", false)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG; window.navigationBarColor = BG
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(36), dp(20), dp(28)); setBackgroundColor(BG) }
        root.addView(text("Block Blast Solver", 28f, Color.WHITE, true))
        root.addView(text("Tự quét màn hình · giải thông minh", 14f, GRAY).apply { setPadding(0, dp(4), 0, dp(20)) })

        status = text("", 14f, Color.WHITE).apply { setLineSpacing(dp(5).toFloat(), 1f) }
        root.addView(card(status), LinearLayout.LayoutParams(-1, -2))

        go = Button(this).apply { textSize = 17f; setTextColor(Color.WHITE); isAllCaps = false; stateListAnimator = null; setOnClickListener { toggle() } }
        root.addView(go, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(16) })

        root.addView(card(
            text("Cách dùng", 16f, Color.WHITE, true),
            text("1. Bấm Bắt đầu và cho phép quay màn hình.\n2. Mở game, mỗi lượt có 3 mảnh mới thì bấm ▶ GIẢI. App tự dò bảng, đọc 3 mảnh và hiện cả 3 nước đi.\n3. Đọc sai? Bấm ✎ để sửa ô rồi giải lại. Lớp phủ lệch? Bấm ⌖ căn chỉnh tay 4 điểm.\n\nChạm “BB” để thu gọn thanh, kéo “BB” để di chuyển.", 13.5f, GRAY).apply { setPadding(0, dp(8), 0, 0); setLineSpacing(dp(3).toFloat(), 1f) }
        ), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        val adv = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        adv.addView(Switch(this).apply {
            text = "Tự động dò vị trí bảng"; setTextColor(Color.WHITE)
            isChecked = getSharedPreferences("cfg", 0).getFloat("auto", 1f) >= 0.5f
            setOnCheckedChangeListener { _, on -> getSharedPreferences("cfg", 0).edit().putFloat("auto", if (on) 1f else 0f).apply(); onResume() }
        })
        slider(adv, "bias", "Độ nhạy nhận khối (↑ khó nhận hơn)", -0.2f, 0.2f)
        slider(adv, "vt", "Ngưỡng dự phòng khi bảng trống/đầy", 0.2f, 0.8f)
        slider(adv, "ts", "Tỉ lệ ô khay / ô bảng", 0.4f, 0.95f)
        adv.addView(Button(this).apply {
            text = "Đặt lại toàn bộ cài đặt"; isAllCaps = false; setTextColor(Color.WHITE); background = round(0xFF3A3A46.toInt(), 12); stateListAnimator = null
            setOnClickListener { Cfg.reset(this@MainActivity); recreate() }
        }, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(18) })
        val toggleAdv = text("Nâng cao ▾", 15f, ORANGE, true).apply { setPadding(0, dp(22), 0, dp(6)) }
        toggleAdv.setOnClickListener { adv.visibility = if (adv.visibility == View.GONE) View.VISIBLE else View.GONE }
        root.addView(toggleAdv); root.addView(adv)
        setContentView(ScrollView(this).apply { setBackgroundColor(BG); isFillViewport = true; addView(root, ViewGroup.LayoutParams(-1, -2)) })
    }

    override fun onResume() {
        super.onResume()
        val ov = Settings.canDrawOverlays(this)
        status.text = (if (ov) "✓" else "✗") + "  Quyền hiển thị trên ứng dụng khác\n" +
            (if (running()) "✓" else "○") + "  Dịch vụ quét " + (if (running()) "đang chạy" else "chưa chạy") + "\n" +
            "✓  Vị trí bảng: " + (if (getSharedPreferences("cfg", 0).getFloat("auto", 1f) >= 0.5f) "tự động dò" else "căn chỉnh tay")
        go.text = if (running()) "Dừng dịch vụ" else "Bắt đầu"
        go.background = round(if (running()) 0xFF3A3A46.toInt() else ORANGE, 14)
    }

    private fun slider(parent: LinearLayout, key: String, label: String, min: Float, max: Float) {
        val sp = getSharedPreferences("cfg", 0)
        val cur = sp.getFloat(key, Cfg.DEF[key]!!)
        val t = text("", 13f, GRAY).apply { setPadding(0, dp(14), 0, 0) }
        val s = SeekBar(this).apply { this.max = 100; progress = ((cur - min) / (max - min) * 100).toInt().coerceIn(0, 100) }
        fun upd() { t.text = "$label: " + "%.2f".format(min + (max - min) * s.progress / 100f) }
        upd()
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) { upd(); sp.edit().putFloat(key, min + (max - min) * p / 100f).apply() }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        parent.addView(t); parent.addView(s)
    }

    private fun toggle() {
        if (running()) { stopService(Intent(this, CaptureService::class.java)); getSharedPreferences("cfg", 0).edit().putBoolean("running", false).apply(); onResume(); return }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Bật quyền rồi quay lại bấm Bắt đầu", Toast.LENGTH_LONG).show(); return
        }
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
        startActivityForResult((getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent(), 1)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == 1 && res == RESULT_OK && data != null) {
            startForegroundService(Intent(this, CaptureService::class.java).putExtra("code", res).putExtra("data", data))
            moveTaskToBack(true)
        }
    }
}
