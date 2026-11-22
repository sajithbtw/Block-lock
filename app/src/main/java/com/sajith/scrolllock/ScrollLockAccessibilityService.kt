package com.sajith.scrolllock

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView

class ScrollLockAccessibilityService : AccessibilityService() {
    companion object {
        const val PREFS = "scrolllock_prefs"
        const val KEY_SELECTED = "selected_packages"
        const val KEY_ACTIVE = "session_active"
        const val KEY_END = "session_end"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var blocker: LinearLayout? = null
    private lateinit var windows: WindowManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        windows = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED)) return
        val openedPackage = event.packageName?.toString() ?: return
        if (openedPackage == packageName) return

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val end = prefs.getLong(KEY_END, 0L)
        val active = prefs.getBoolean(KEY_ACTIVE, false) && System.currentTimeMillis() < end
        if (!active) {
            prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
            hideBlocker()
            return
        }
        val selected = prefs.getStringSet(KEY_SELECTED, emptySet()) ?: emptySet()
        if (openedPackage in selected) showBlocker(end) else hideBlocker()
    }

    private fun showBlocker(end: Long) {
        if (blocker != null) {
            updateCountdown(end)
            return
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(30), dp(30), dp(30), dp(30))
            setBackgroundColor(Color.rgb(18, 19, 22))
            isClickable = true
            isFocusable = true
        }
        root.addView(TextView(this).apply {
            text = "SCROLLLOCK"
            textSize = 13f
            setTextColor(Color.rgb(255, 151, 75))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Stay focused."
            textSize = 30f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(12))
        })
        root.addView(TextView(this).apply {
            text = "This app is paused until your focus session ends."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            tag = "remaining"
            textSize = 25f
            setTextColor(Color.rgb(255, 151, 75))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, dp(10))
        })
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        try {
            windows.addView(root, params)
            blocker = root
            updateCountdown(end)
            handler.removeCallbacksAndMessages(null)
            handler.post(object : Runnable {
                override fun run() {
                    val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
                    val currentEnd = prefs.getLong(KEY_END, 0L)
                    if (!prefs.getBoolean(KEY_ACTIVE, false) || System.currentTimeMillis() >= currentEnd) {
                        prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
                        hideBlocker()
                    } else {
                        updateCountdown(currentEnd)
                        handler.postDelayed(this, 1000L)
                    }
                }
            })
        } catch (_: Exception) {
            blocker = null
        }
    }

    private fun updateCountdown(end: Long) {
        val millis = (end - System.currentTimeMillis()).coerceAtLeast(0L)
        val seconds = millis / 1000L
        blocker?.findViewWithTag<TextView>("remaining")?.text =
            "%02d:%02d".format(seconds / 60, seconds % 60)
    }

    private fun hideBlocker() {
        handler.removeCallbacksAndMessages(null)
        blocker?.let {
            try { windows.removeView(it) } catch (_: Exception) {}
        }
        blocker = null
    }

    override fun onInterrupt() { hideBlocker() }
    override fun onDestroy() {
        hideBlocker()
        super.onDestroy()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
