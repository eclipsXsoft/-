package com.example.clickdoubler

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.*

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        prefs = Cfg.p(this)
        setContentView(build())
    }

    private fun build(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }
        root.addView(Button(this).apply {
            text = "1. Включить службу (Специальные возможности)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(sw("Показывать кнопку", "show", true))
        root.addView(sw("Режим настройки (перетаскивание, без кликов)", "edit", true))

        root.addView(TextView(this).apply { text = "Текст на кнопке" })
        root.addView(EditText(this).apply {
            setText(prefs.getString("text", "x2"))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    prefs.edit().putString("text", s.toString()).apply()
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        })

        seek(root, "Размер кнопки (dp)", "size", 30, 200, 70)
        seek(root, "Размер текста (sp)", "tsize", 8, 40, 18)
        seek(root, "Прозрачность (%)", "alpha", 10, 100, 70)
        seek(root, "Толщина рамки (dp)", "border", 0, 10, 2)
        seek(root, "Кликов за одно нажатие", "count", 2, 10, 2)
        seek(root, "Пауза между кликами (мс)", "gap", 10, 300, 30)

        root.addView(TextView(this).apply {
            setPadding(0, dp(16), 0, 0)
            text = "Как пользоваться: включите службу, включите «Режим настройки», " +
                "перетащите кнопку на нужное место в игре (размер меняется кнопками − / + " +
                "на панели сверху экрана). Нажмите «OK» на панели — режим настройки " +
                "выключится, и каждое нажатие на кнопку будет делать несколько кликов " +
                "в центре кнопки."
        })
        return ScrollView(this).apply { addView(root) }
    }

    private fun sw(title: String, key: String, def: Boolean) = Switch(this).apply {
        text = title
        isChecked = prefs.getBoolean(key, def)
        setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean(key, v).apply() }
    }

    private fun seek(root: LinearLayout, title: String, key: String, min: Int, max: Int, def: Int) {
        val cur = prefs.getInt(key, def)
        val label = TextView(this).apply { text = "$title: $cur"; setPadding(0, dp(12), 0, 0) }
        val bar = SeekBar(this).apply { this.max = max - min; progress = cur - min }
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val v = p + min
                label.text = "$title: $v"
                if (fromUser) prefs.edit().putInt(key, v).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        root.addView(label)
        root.addView(bar)
    }
}
