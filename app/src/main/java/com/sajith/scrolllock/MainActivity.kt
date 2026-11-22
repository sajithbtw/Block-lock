package com.sajith.scrolllock

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(ScrollLockAccessibilityService.PREFS, MODE_PRIVATE) }
    private val selected = mutableSetOf<String>()
    private lateinit var status: TextView
    private lateinit var timer: TextView
    private lateinit var duration: Spinner
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateTimer()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected.addAll(prefs.getStringSet(ScrollLockAccessibilityService.KEY_SELECTED, emptySet()) ?: emptySet())
        buildUi()
        handler.post(ticker)
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) {
            status.text = if (accessibilityEnabled()) "Accessibility service: Enabled" else "Accessibility service: Not enabled"
            status.setTextColor(if (accessibilityEnabled()) Color.rgb(120, 210, 150) else Color.rgb(255, 170, 90))
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(Color.rgb(18, 19, 22))
        }
        val scroll = ScrollView(this).apply { addView(page) }
        setContentView(scroll)

        page.addView(text("SCROLLLOCK", 13, Color.rgb(255, 151, 75), true))
        page.addView(text("Take back your focus.", 29, Color.WHITE, true).apply {
            setPadding(0, dp(12), 0, dp(6))
        })
        page.addView(text("Choose distracting apps and set a focus session.", 14, Color.LTGRAY, false))

        status = text("", 13, Color.rgb(255, 170, 90), true).apply { setPadding(0, dp(20), 0, dp(8)) }
        page.addView(status)
        status.text = if (accessibilityEnabled()) "Accessibility service: Enabled" else "Accessibility service: Not enabled"

        page.addView(button("Enable accessibility").apply {
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        page.addView(text("FOCUS DURATION", 12, Color.LTGRAY, true).apply { setPadding(0, dp(22), 0, dp(8)) })
        duration = Spinner(this)
        duration.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("15 minutes", "25 minutes", "30 minutes", "45 minutes", "60 minutes", "90 minutes"))
        page.addView(duration)

        page.addView(text("APPS TO BLOCK", 12, Color.LTGRAY, true).apply { setPadding(0, dp(22), 0, dp(8)) })
        page.addView(text("Select the apps you want interrupted during focus.", 13, Color.LTGRAY, false))
        val appList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(appList)
        loadApps(appList)

        page.addView(button("START FOCUS SESSION").apply { setOnClickListener { startSession() } })
        page.addView(button("End session").apply {
            setOnClickListener {
                prefs.edit().putBoolean(ScrollLockAccessibilityService.KEY_ACTIVE, false)
                    .putLong(ScrollLockAccessibilityService.KEY_END, 0L).apply()
                updateTimer()
            }
        })
        timer = text("No active session", 15, Color.WHITE, true).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(20), 0, dp(10))
        }
        page.addView(timer)
        page.addView(text("Privacy: Accessibility detects app/window changes only. ScrollLock does not inspect screen contents or send activity data to a server.", 12, Color.GRAY, false).apply {
            setPadding(0, dp(22), 0, 0)
        })
    }

    private fun loadApps(container: LinearLayout) {
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(launchIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.queryIntentActivities(launchIntent, 0)
        }.filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        matches.forEach { info ->
            val pkg = info.activityInfo.packageName
            container.addView(CheckBox(this).apply {
                text = info.loadLabel(packageManager).toString()
                textSize = 15f
                setTextColor(Color.WHITE)
                isChecked = selected.contains(pkg)
                buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(255, 151, 75))
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(pkg) else selected.remove(pkg)
                    prefs.edit().putStringSet(ScrollLockAccessibilityService.KEY_SELECTED, selected.toSet()).apply()
                }
            })
        }
    }

    private fun startSession() {
        if (selected.isEmpty()) {
            status.text = "Select at least one app."
            return
        }
        if (!accessibilityEnabled()) {
            status.text = "Enable Accessibility first."
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        val minutes = listOf(15L, 25L, 30L, 45L, 60L, 90L)[duration.selectedItemPosition]
        prefs.edit()
            .putStringSet(ScrollLockAccessibilityService.KEY_SELECTED, selected.toSet())
            .putLong(ScrollLockAccessibilityService.KEY_END, System.currentTimeMillis() + minutes * 60_000L)
            .putBoolean(ScrollLockAccessibilityService.KEY_ACTIVE, true)
            .apply()
        status.text = "Focus session active"
        updateTimer()
    }

    private fun accessibilityEnabled(): Boolean {
        val expected = ComponentName(this, ScrollLockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { it.equals(expected, true) }
    }

    private fun updateTimer() {
        if (!::timer.isInitialized) return
        val active = prefs.getBoolean(ScrollLockAccessibilityService.KEY_ACTIVE, false)
        val left = prefs.getLong(ScrollLockAccessibilityService.KEY_END, 0L) - System.currentTimeMillis()
        if (!active || left <= 0L) {
            prefs.edit().putBoolean(ScrollLockAccessibilityService.KEY_ACTIVE, false).apply()
            timer.text = "No active session"
        } else {
            val seconds = left / 1000
            timer.text = "FOCUS IN PROGRESS  •  %02d:%02d".format(seconds / 60, seconds % 60)
        }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun button(value: String) = Button(this).apply {
        text = value
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            setColor(Color.rgb(48, 49, 54))
            cornerRadius = dp(14).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
            topMargin = dp(10)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
