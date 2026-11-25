package com.example.clickdoubler

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView

class ClickService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences
    private val h = Handler(Looper.getMainLooper())
    private var btn: TextView? = null
    private var panel: LinearLayout? = null
    private lateinit var lp: WindowManager.LayoutParams
    private var busy = false

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        h.post { refresh() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = Cfg.p(this)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        refresh()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(listener)
        removeAll()
        super.onDestroy()
    }

    private fun removeAll() {
        try { btn?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { panel?.let { wm.removeView(it) } } catch (_: Exception) {}
        btn = null
        panel = null
    }

    private fun refresh() {
        if (!prefs.getBoolean("show", true)) { removeAll(); return }
        val edit = prefs.getBoolean("edit", true)
        val size = dp(prefs.getInt("size", 70))

        if (btn == null) createButton(size)
        val b = btn!!
        lp.width = size
        lp.height = size
        b.text = prefs.getString("text", "x2")
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.getInt("tsize", 18).toFloat())
        b.alpha = prefs.getInt("alpha", 70) / 100f
        b.background = GradientDrawable().apply {
            setColor(Color.BLACK)
            setStroke(dp(prefs.getInt("border", 2)), if (edit) Color.YELLOW else Color.WHITE)
        }
        try { wm.updateViewLayout(b, lp) } catch (_: Exception) {}

        if (edit && panel == null) createPanel()
        if (!edit) {
            try { panel?.let { wm.removeView(it) } } catch (_: Exception) {}
            panel = null
        }
    }

    private fun createButton(size: Int) {
        val b = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", 200)
            y = prefs.getInt("y", 500)
        }

        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        b.setOnTouchListener { v, e ->
            if (prefs.getBoolean("edit", true)) {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        lp.x = startX + (e.rawX - downX).toInt()
                        lp.y = startY + (e.rawY - downY).toInt()
                        try { wm.updateViewLayout(v, lp) } catch (_: Exception) {}
                    }
                    MotionEvent.ACTION_UP ->
                        prefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                }
            } else if (e.action == MotionEvent.ACTION_UP) {
                fire()
            }
            true
        }
        wm.addView(b, lp)
        btn = b
    }

    // Панель "− + OK" сверху экрана, только в режиме настройки
    private fun createPanel() {
        fun item(label: String, action: () -> Unit) = TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(10), dp(20), dp(10))
            setOnClickListener { action() }
        }
        fun changeSize(d: Int) {
            val s = (prefs.getInt("size", 70) + d).coerceIn(30, 200)
            prefs.edit().putInt("size", s).apply()
        }
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(0xDD222222.toInt()); cornerRadius = dp(12).toFloat()
            }
            addView(item("−") { changeSize(-8) })
            addView(item("+") { changeSize(8) })
            addView(item("OK") { prefs.edit().putBoolean("edit", false).apply() })
        }
        val plp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(40)
        }
        wm.addView(p, plp)
        panel = p
    }

    private fun fire() {
        val b = btn ?: return
        if (busy) return
        busy = true
        val loc = IntArray(2)
        b.getLocationOnScreen(loc)
        val cx = loc[0] + b.width / 2f
        val cy = loc[1] + b.height / 2f
        val n = prefs.getInt("count", 2).coerceIn(1, 10)
        val gap = prefs.getInt("gap", 30).toLong()
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        try { wm.updateViewLayout(b, lp) } catch (_: Exception) {}
        h.postDelayed({
            val tapMs = 8L
            val builder = GestureDescription.Builder()
            for (i in 0 until n) {
                val path = Path().apply { moveTo(cx, cy) }
                builder.addStroke(
                    GestureDescription.StrokeDescription(path, i * (tapMs + gap), tapMs)
                )
            }
            val ok = dispatchGesture(builder.build(), object : GestureResultCallback() {
                override fun onCompleted(d: GestureDescription?) { finishFire() }
                override fun onCancelled(d: GestureDescription?) { finishFire() }
            }, null)
            if (!ok) finishFire()
        }, 20)
    }

    private fun finishFire() {
        busy = false
        val b = btn ?: return
        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        try { wm.updateViewLayout(b, lp) } catch (_: Exception) {}
    }
}
