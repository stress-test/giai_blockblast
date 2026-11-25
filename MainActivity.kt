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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val bg = 0xFF090E1B.toInt()
    private val cardTop = 0xD9273552.toInt()
    private val cardBottom = 0xC9162138.toInt()
    private val white = 0xFFF4F7FF.toInt()
    private val muted = 0xFF9BAAC4.toInt()
    private val cyan = 0xFF8BD8FF.toInt()
    private val mint = 0xFF63DEC4.toInt()
    private val orange = 0xFFFFA477.toInt()
    private lateinit var status: TextView
    private lateinit var go: TextView
    private lateinit var statusDot: View
    private lateinit var advanced: LinearLayout
    private lateinit var advancedToggle: TextView

    private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Float, stroke: Int = 0x55FFFFFF): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(color, if (color == cardTop) cardBottom else color)
        ).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1f), stroke)
        }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(Typeface.create("sans-serif", Typeface.BOLD))
            includeFontPadding = false
        }

    private fun glassCard(pad: Int = 18): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(cardTop, 24f, 0x66D9ECFF)
            setPadding(dp(pad.toFloat()), dp(pad.toFloat()), dp(pad.toFloat()), dp(pad.toFloat()))
            elevation = dp(8f).toFloat()
        }

    private fun running() = getSharedPreferences("cfg", 0).getBoolean("running", false)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(24f))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(24f), dp(20f), dp(20f))
        }
        scroll.addView(root, ViewGroup.LayoutParams(-1, -2))

        // Ambient glass header
        val hero = glassCard(20)
        hero.background = shape(0xE52C4568.toInt(), 28f, 0x99B7E8FF)
        hero.addView(label("BLOCK BLAST", 12f, cyan, true))
        hero.addView(label("SOLVER", 32f, white, true).apply {
            setPadding(0, dp(5f), 0, dp(3f))
            letterSpacing = 0.08f
        })
        hero.addView(label("Smart moves · Liquid Glass edition", 13f, 0xFFD1DDF0.toInt()))
        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(17f), 0, 0)
        }
        chips.addView(chip("8 × 8 BOARD", 0x334FBAFF, cyan))
        chips.addView(space(8))
        chips.addView(chip("3 PIECES", 0x335BDCC0, mint))
        hero.addView(chips)
        root.addView(hero, marginBottom(18))

        // Live status card
        val statusCard = glassCard(18)
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        statusDot = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(mint) }
            layoutParams = LinearLayout.LayoutParams(dp(9f), dp(9f))
        }
        head.addView(statusDot)
        head.addView(label("  SYSTEM STATUS", 12f, cyan, true))
        head.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        head.addView(label("LIVE", 10f, mint, true).apply {
            background = shape(0x333DDBB8, 30f, 0x664FE2C2)
            setPadding(dp(10f), dp(5f), dp(10f), dp(5f))
        })
        statusCard.addView(head)
        status = label("", 13f, white).apply {
            setLineSpacing(dp(7f).toFloat(), 1f)
            setPadding(0, dp(14f), 0, 0)
        }
        statusCard.addView(status)
        root.addView(statusCard, marginBottom(14))

        // Primary action
        go = label("✦   START SOLVER", 16f, 0xFF101A2A.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xFF9BE6FF.toInt(), 0xFF70E2CE.toInt())
            ).apply { cornerRadius = dp(18f).toFloat() }
            elevation = dp(8f).toFloat()
            setPadding(0, dp(17f), 0, dp(17f))
            setOnClickListener { toggle() }
        }
        root.addView(go, marginBottom(18))

        val how = glassCard()
        how.addView(label("QUICK GUIDE", 12f, cyan, true))
        how.addView(label("Giải trong 3 bước", 20f, white, true).apply { setPadding(0, dp(7f), 0, dp(12f)) })
        how.addView(step("01", "Bắt đầu", "Cấp quyền hiển thị và quay màn hình."))
        how.addView(step("02", "Mở Block Blast", "Nhấn GIẢI trên thanh nổi khi có đủ 3 mảnh."))
        how.addView(step("03", "Làm theo gợi ý", "Đặt theo số 1 → 2 → 3. Có thể sửa nhận diện nếu cần."))
        root.addView(how, marginBottom(14))

        val toolsCard = glassCard()
        toolsCard.addView(label("PRECISION TOOLS", 12f, cyan, true))
        toolsCard.addView(label("Tinh chỉnh độ chính xác", 18f, white, true).apply { setPadding(0, dp(6f), 0, dp(4f)) })
        toolsCard.addView(label("Tự dò bảng hoặc căn chỉnh thủ công theo 4 điểm để khớp lớp phủ với màn hình game.", 13f, muted))
        advancedToggle = label("CÀI ĐẶT NÂNG CAO     ＋", 13f, mint, true).apply {
            setPadding(0, dp(18f), 0, dp(6f))
            setOnClickListener {
                advanced.visibility = if (advanced.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                text = if (advanced.visibility == View.VISIBLE) "CÀI ĐẶT NÂNG CAO     －" else "CÀI ĐẶT NÂNG CAO     ＋"
            }
        }
        toolsCard.addView(advancedToggle)
        advanced = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        advanced.addView(Switch(this).apply {
            text = "Tự động dò vị trí bảng"
            setTextColor(white)
            isChecked = getSharedPreferences("cfg", 0).getFloat("auto", 1f) >= 0.5f
            setOnCheckedChangeListener { _, on ->
                getSharedPreferences("cfg", 0).edit().putFloat("auto", if (on) 1f else 0f).apply()
                refreshStatus()
            }
        })
        slider(advanced, "bias", "Độ nhạy nhận diện", -0.2f, 0.2f)
        slider(advanced, "vt", "Ngưỡng dự phòng", 0.2f, 0.8f)
        slider(advanced, "ts", "Tỉ lệ kích thước mảnh", 0.4f, 0.95f)
        val reset = label("ĐẶT LẠI CÀI ĐẶT", 12f, orange, true).apply {
            gravity = Gravity.CENTER
            background = shape(0x333C526E, 14f)
            setPadding(0, dp(13f), 0, dp(13f))
            setOnClickListener { Cfg.reset(this@MainActivity); recreate() }
        }
        advanced.addView(reset, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12f) })
        toolsCard.addView(advanced)
        root.addView(toolsCard, marginBottom(18))

        root.addView(label("Designed for clarity  ·  BB Solver", 11f, 0xFF65758F.toInt()).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(5f), 0, 0)
        })
        setContentView(scroll)
        refreshStatus()
    }

    private fun marginBottom(v: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(v.toFloat()) }

    private fun space(w: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(w.toFloat()), 1)
    }

    private fun chip(text: String, color: Int, fg: Int) = label(text, 10f, fg, true).apply {
        background = shape(color, 30f, 0x55FFFFFF)
        setPadding(dp(10f), dp(7f), dp(10f), dp(7f))
    }

    private fun step(no: String, title: String, body: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, dp(7f), 0, dp(7f))
        }
        row.addView(label(no, 12f, mint, true).apply {
            gravity = Gravity.CENTER
            background = shape(0x334FE2C2, 12f, 0x665FE3C5)
            setPadding(dp(10f), dp(9f), dp(10f), dp(9f))
        })
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12f), 0, 0, 0) }
        col.addView(label(title, 14f, white, true))
        col.addView(label(body, 12f, muted).apply { setPadding(0, dp(4f), 0, 0) })
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        return row
    }

    private fun refreshStatus() {
        val overlayPermission = Settings.canDrawOverlays(this)
        val isRunning = running()
        statusDot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (isRunning) mint else orange)
        }
        status.text = (if (overlayPermission) "●  Overlay permission ready" else "○  Overlay permission required") +
            "\n" + (if (isRunning) "●  Solver service is running" else "○  Solver service is stopped") +
            "\n" + "●  Board alignment: " +
            if (getSharedPreferences("cfg", 0).getFloat("auto", 1f) >= 0.5f) "Auto detect" else "Manual calibration"
        go.text = if (isRunning) "■   STOP SOLVER" else "✦   START SOLVER"
        go.background = if (isRunning) shape(0xFF34435A.toInt(), 18f, 0x88FFFFFF) else
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFF9BE6FF.toInt(), 0xFF70E2CE.toInt())).apply {
                cornerRadius = dp(18f).toFloat()
            }
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun slider(parent: LinearLayout, key: String, title: String, min: Float, max: Float) {
        val prefs = getSharedPreferences("cfg", 0)
        val current = prefs.getFloat(key, Cfg.DEF[key]!!)
        val value = label("", 12f, muted).apply { setPadding(0, dp(12f), 0, 0) }
        val seek = SeekBar(this).apply {
            this.max = 100
            progress = ((current - min) / (max - min) * 100).toInt().coerceIn(0, 100)
        }
        fun update() {
            value.text = "$title  ·  %.2f".format(min + (max - min) * seek.progress / 100f)
        }
        update()
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                update()
                if (fromUser) prefs.edit().putFloat(key, min + (max - min) * progress / 100f).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        parent.addView(value)
        parent.addView(seek)
    }

    private fun toggle() {
        if (running()) {
            stopService(Intent(this, CaptureService::class.java))
            getSharedPreferences("cfg", 0).edit().putBoolean("running", false).apply()
            refreshStatus()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Hãy bật quyền hiển thị trên ứng dụng khác rồi quay lại.", Toast.LENGTH_LONG).show()
            return
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
