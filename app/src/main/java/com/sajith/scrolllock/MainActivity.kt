package com.sajith.scrolllock

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(ScrollLockAccessibilityService.PREFS, MODE_PRIVATE) }
    private val selected = mutableSetOf<String>()
    private val bg = Color.rgb(30, 28, 26)       // Reference APK: #1E1C1A
    private val surface = Color.rgb(40, 37, 34)
    private val surface2 = Color.rgb(48, 44, 40)
    private val accent = Color.rgb(245, 83, 20)  // Reference APK: #F55314
    private val primary = Color.WHITE
    private val secondary = Color.rgb(110, 104, 98) // Reference APK: #6E6862
    private lateinit var content: LinearLayout
    private lateinit var statusLine: TextView
    private lateinit var timerLine: TextView
    private var activeTab = 0
    private var durationMinutes = 45
    private var selectedAppsCount = 0

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refreshSessionText()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected.addAll(prefs.getStringSet(ScrollLockAccessibilityService.KEY_SELECTED, emptySet()) ?: emptySet())
        window.statusBarColor = bg
        window.navigationBarColor = bg
        if (Build.VERSION.SDK_INT >= 23) window.decorView.systemUiVisibility = 0
        buildShell()
        showTab(0)
        handler.post(ticker)
    }

    override fun onResume() {
        super.onResume()
        if (::statusLine.isInitialized) updateStatusLine()
        if (::content.isInitialized) showTab(activeTab)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }


    private var onboardingShowing = false
    private var onboardingStep = 0
    private var onboardingRoot: LinearLayout? = null
    private val onboardingBg = Color.rgb(224, 224, 224)
    private val onboardingInk = Color.rgb(79, 73, 73)
    private val onboardingMuted = Color.rgb(119, 112, 112)
    private val onboardingOrange = Color.rgb(249, 76, 17)

    private fun showOnboarding() {
        onboardingShowing = true
        window.statusBarColor = onboardingBg
        window.navigationBarColor = onboardingBg
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        renderOnboarding()
    }

    private fun renderOnboarding() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
            setBackgroundColor(onboardingBg)
        }
        val brand = TextView(this).apply {
            text = "scrolllock"
            textSize = 29f
            letterSpacing = 0.03f
            setTextColor(onboardingInk)
            typeface = Typeface.create("monospace", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        root.addView(brand, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(76)
        ))
        val hero = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 26)
        }
        val heroText = TextView(this).apply {
            text = when (onboardingStep) {
                0 -> "01"
                1 -> "▪   ▪   ▪"
                2 -> "08"
                else -> "■"
            }
            textSize = if (onboardingStep == 0 || onboardingStep == 2) 76f else 48f
            setTextColor(if (onboardingStep == 3) onboardingOrange else onboardingInk)
            typeface = Typeface.create("monospace", Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = if (onboardingStep == 1) 0.12f else 0f
        }
        hero.addView(heroText)
        root.addView(hero, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.34f
        ).apply { topMargin = dp(18) })

        val middle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(24), dp(12), 0)
        }
        val titles = listOf("ACCESSIBILITY", "BEFORE BOARDING", "THE BLOCK", "ANALOG CONTROLS")
        val descriptions = listOf(
            "ScrollLock uses Accessibility to notice when you open an app you chose to pause. It does not read what is on your screen.",
            "A focus session needs the required system access to work reliably. Review each permission before you begin.",
            "Choose how long to focus. Your phone, messages, camera and the apps you keep available stay at hand.",
            "ScrollLock is designed like a small machine: wheels, levers and handles. Pull the lever to move forward."
        )
        val footnotes = listOf(
            "ScrollLock never reads what is on your screen; it only brings you back here.",
            "Nothing is enabled until you grant the permission in Android settings.",
            "Calls and essential phone functions remain available.",
            "The lever is a simple gesture to guide you through setup."
        )
        val heading = TextView(this).apply {
            text = (if (onboardingStep == 0) "🔧  " else if (onboardingStep == 2) "◷  " else "▸  ") + titles[onboardingStep]
            textSize = 15f
            letterSpacing = 0.10f
            setTextColor(onboardingInk)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), 0, 0, dp(18))
        }
        left.addView(heading)
        val copy = TextView(this).apply {
            text = descriptions[onboardingStep]
            textSize = 17f
            setTextColor(onboardingMuted)
            setLineSpacing(dp(5).toFloat(), 1f)
            background = rounded(Color.rgb(218, 218, 218), 22)
            setPadding(dp(14), dp(16), dp(14), dp(18))
        }
        left.addView(copy, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        val foot = TextView(this).apply {
            text = "■  " + footnotes[onboardingStep]
            textSize = 12f
            setTextColor(onboardingMuted)
            setPadding(dp(12), dp(14), dp(8), dp(8))
        }
        left.addView(foot)
        middle.addView(left, LinearLayout.LayoutParams(0, dp(390), 1f))

        val rail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            background = rounded(Color.rgb(216, 216, 216), 30)
            setPadding(dp(12), dp(18), dp(12), dp(18))
            isClickable = true
        }
        val handle = TextView(this).apply {
            text = "☰"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(onboardingInk, 12)
            setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    if (onboardingStep == 0) grantAccessibility() else advanceOnboarding()
                    true
                } else true
            }
        }
        rail.addView(handle, LinearLayout.LayoutParams(dp(68), dp(68)))
        val dots = TextView(this).apply {
            text = "•\n•\n•\n•\n•\n•\n•\n•\n•"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(184, 184, 184))
        }
        rail.addView(dots, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        middle.addView(rail, LinearLayout.LayoutParams(dp(94), dp(390)).apply {
            topMargin = dp(18)
        })
        root.addView(middle, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.53f
        ).apply { topMargin = dp(10) })

        if (onboardingStep == 0) {
            val grant = TextView(this).apply {
                text = "Grant"
                textSize = 16f
                letterSpacing = 0.08f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = rounded(onboardingInk, 24)
                setOnClickListener { grantAccessibility() }
            }
            root.addView(grant, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(78)
            ).apply { topMargin = dp(14) })
        } else {
            val pull = TextView(this).apply {
                text = "PULL THE LEVER"
                textSize = 14f
                letterSpacing = 0.12f
                gravity = Gravity.CENTER
                setTextColor(onboardingMuted)
                setOnClickListener { advanceOnboarding() }
            }
            root.addView(pull, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(74)
            ))
        }
        val footer = TextView(this).apply {
            text = "Privacy Policy     ·     Terms of Service"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(onboardingMuted)
        }
        root.addView(footer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(62)
        ))
        onboardingRoot = root
        setContentView(root)
    }

    private fun grantAccessibility() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun advanceOnboarding() {
        if (onboardingStep < 3) {
            onboardingStep++
            renderOnboarding()
        } else {
            prefs.edit().putBoolean("onboarding_complete", true).apply()
            onboardingShowing = false
            window.statusBarColor = bg
            window.navigationBarColor = bg
            if (Build.VERSION.SDK_INT >= 23) window.decorView.systemUiVisibility = 0
            buildShell()
            showTab(0)
        }
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), dp(14), dp(22), dp(12))
        }
        val brandMark = TextView(this).apply {
            text = "◈"
            textSize = 25f
            setTextColor(accent)
            gravity = Gravity.CENTER
        }
        header.addView(brandMark, LinearLayout.LayoutParams(dp(34), dp(34)))
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(label("SCROLLLOCK", 13, primary, true, 1f))
        brand.addView(label("FOCUS, ON PURPOSE", 9, secondary, true, 1f))
        header.addView(brand, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val state = label("●", 13, if (accessibilityEnabled()) accent else secondary, true, 1f)
        state.gravity = Gravity.CENTER
        header.addView(state, LinearLayout.LayoutParams(dp(28), dp(28)))
        root.addView(header)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(4), dp(18), dp(12))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(content)
        }
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(buildNavigation())
        setContentView(root)
    }

    private fun buildNavigation(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(12))
            background = rounded(surface, 22)
        }
        val names = listOf("Bag", "Control", "Session", "Stats", "Settings")
        val icons = listOf("▣", "◉", "◷", "▥", "⚙")
        names.forEachIndexed { index, name ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setPadding(dp(2), dp(7), dp(2), dp(5))
                setOnClickListener { showTab(index) }
            }
            val chosen = index == activeTab
            item.addView(label(icons[index], 19, if (chosen) accent else secondary, true, 1f).apply {
                gravity = Gravity.CENTER
            })
            item.addView(label(name, 10, if (chosen) primary else secondary, chosen, 1f).apply {
                gravity = Gravity.CENTER
            })
            bar.addView(item, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        return bar
    }

    private fun showTab(tab: Int) {
        activeTab = tab
        content.removeAllViews()
        when (tab) {
            0 -> buildBag()
            1 -> buildControl()
            2 -> buildSession()
            3 -> buildStats()
            else -> buildSettings()
        }
        // Rebuild the navigation so the active destination is highlighted.
        val root = content.parent as? ScrollView ?: return
        val shell = root.parent as? LinearLayout ?: return
        shell.removeViewAt(shell.childCount - 1)
        shell.addView(buildNavigation())
    }

    private fun buildBag() {
        heading("Your focus bag", "Keep the apps you want to put aside in one place.")
        val card = panel()
        card.addView(label("TODAY'S FOCUS", 10, secondary, true, 1f))
        card.addView(label(if (sessionActive()) "Your session is active" else "Make room for what matters.", 22, primary, true, 1f).apply {
            setPadding(0, dp(9), 0, dp(5))
        })
        card.addView(label("${selected.size} apps selected  ·  ${durationMinutes} min session", 12, secondary, false, 1f))
        card.addView(action("START FOCUS", true) { startSession() })
        content.addView(card)
        gap(18)
        sectionTitle("YOUR DISTRACTIONS")
        sectionSubtitle("Choose which apps ScrollLock should interrupt.")
        addAppPicker()
        gap(8)
        statusLine = label("", 12, secondary, false, 1f)
        content.addView(statusLine)
        updateStatusLine()
    }

    private fun buildControl() {
        heading("Control", "Set up the guardrails before you begin.")
        val permission = panel()
        permission.addView(label("ACCESSIBILITY SERVICE", 10, secondary, true, 1f))
        permission.addView(label(if (accessibilityEnabled()) "ScrollLock is connected" else "Permission needed", 20, primary, true, 1f).apply {
            setPadding(0, dp(8), 0, dp(5))
        })
        permission.addView(label(
            if (accessibilityEnabled()) "App-change detection is ready. ScrollLock does not read screen contents."
            else "Allow the service so ScrollLock can detect when a selected app opens.",
            12, secondary, false, 1f
        ))
        permission.addView(action(if (accessibilityEnabled()) "OPEN ACCESSIBILITY SETTINGS" else "ENABLE ACCESSIBILITY", true) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(permission)
        gap(14)
        val session = panel()
        session.addView(label("BLOCKING STATUS", 10, secondary, true, 1f))
        session.addView(label(if (sessionActive()) "Focus session running" else "No active session", 20, primary, true, 1f).apply {
            setPadding(0, dp(8), 0, dp(4))
        })
        timerLine = label("", 15, accent, true, 1f)
        session.addView(timerLine)
        session.addView(action("END SESSION", false) { endSession(); showTab(1) })
        content.addView(session)
        refreshSessionText()
    }

    private fun buildSession() {
        heading("Session", "Choose a time window for uninterrupted focus.")
        val timerCard = panel()
        timerCard.addView(label("FOCUS DURATION", 10, secondary, true, 1f))
        timerCard.addView(label("${durationMinutes}", 42, primary, true, 1f).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        })
        timerCard.addView(label("MINUTES", 10, secondary, true, 1f).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        })
        content.addView(timerCard)
        gap(14)
        sectionTitle("QUICK PRESETS")
        val values = listOf(15, 25, 30, 45, 60, 90)
        values.chunked(3).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { minutes ->
                val chosen = durationMinutes == minutes
                val tile = panel().apply {
                    background = rounded(if (chosen) Color.rgb(73, 39, 26) else surface, 16)
                    isClickable = true
                    setOnClickListener { durationMinutes = minutes; showTab(2) }
                }
                tile.addView(label("$minutes", 23, if (chosen) accent else primary, true, 1f).apply {
                    gravity = Gravity.CENTER
                })
                tile.addView(label("MIN", 9, secondary, true, 1f).apply { gravity = Gravity.CENTER })
                row.addView(tile, LinearLayout.LayoutParams(0, dp(82), 1f).apply {
                    marginEnd = dp(7)
                })
            }
            content.addView(row)
            gap(8)
        }
        gap(10)
        content.addView(action("START THIS SESSION", true) { startSession() })
        content.addView(label("You can end a session manually from Control. Android may still allow access if the Accessibility service is disabled.", 11, secondary, false, 1f).apply {
            setPadding(2, dp(12), 2, 0)
        })
    }

    private fun buildStats() {
        heading("Your progress", "A small step away from endless scrolling.")
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val left = metricCard("0", "SESSIONS")
        val right = metricCard("0", "FOCUS MINUTES")
        row.addView(left, LinearLayout.LayoutParams(0, dp(112), 1f).apply { marginEnd = dp(8) })
        row.addView(right, LinearLayout.LayoutParams(0, dp(112), 1f))
        content.addView(row)
        gap(12)
        val graph = panel()
        graph.addView(label("FOCUS ACTIVITY", 10, secondary, true, 1f))
        graph.addView(label("Your focus history will appear here.", 14, primary, false, 1f).apply {
            setPadding(0, dp(18), 0, dp(18))
        })
        val bars = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
        }
        repeat(7) { i ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            val bar = View(this).apply { background = rounded(if (i == 6) accent else surface2, 8) }
            col.addView(bar, LinearLayout.LayoutParams(dp(17), dp(if (i == 6) 38 else 12 + (i % 3) * 7)))
            col.addView(label(listOf("M","T","W","T","F","S","S")[i], 9, secondary, false, 1f).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            })
            bars.addView(col, LinearLayout.LayoutParams(0, dp(68), 1f))
        }
        graph.addView(bars)
        content.addView(graph)
        content.addView(label("Statistics are currently local to this device. Detailed session history is not implemented yet.", 11, secondary, false, 1f).apply {
            setPadding(2, dp(12), 2, 0)
        })
    }

    private fun buildSettings() {
        heading("Settings", "Make ScrollLock work the way you want.")
        val permission = panel()
        permission.addView(label("ACCESSIBILITY", 10, secondary, true, 1f))
        permission.addView(label(if (accessibilityEnabled()) "Enabled" else "Not enabled", 18, primary, true, 1f).apply {
            setPadding(0, dp(8), 0, dp(4))
        })
        permission.addView(action("MANAGE PERMISSION", false) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(permission)
        gap(12)
        val privacy = panel()
        privacy.addView(label("PRIVACY", 10, secondary, true, 1f))
        privacy.addView(label("Your selected apps and session timer are stored on this device. The Accessibility service observes app/window changes only; it does not inspect screen text or send usage data to a server.", 13, primary, false, 1f).apply {
            setPadding(0, dp(8), 0, 0)
        })
        content.addView(privacy)
        gap(12)
        val about = panel()
        about.addView(label("SCROLLLOCK", 10, secondary, true, 1f))
        about.addView(label("A calmer relationship with your screen.", 15, primary, true, 1f).apply {
            setPadding(0, dp(7), 0, 0)
        })
        content.addView(about)
    }

    private fun addAppPicker() {
        val list = panel()
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(launchIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.queryIntentActivities(launchIntent, 0)
        }.filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        selectedAppsCount = selected.size
        if (matches.isEmpty()) {
            list.addView(label("No launchable apps were found.", 13, secondary, false, 1f))
        } else {
            matches.forEach { info ->
                val pkg = info.activityInfo.packageName
                val check = CheckBox(this).apply {
                    text = info.loadLabel(packageManager).toString()
                    textSize = 14f
                    setTextColor(primary)
                    buttonTintList = ColorStateList.valueOf(accent)
                    isChecked = selected.contains(pkg)
                    setPadding(0, dp(3), 0, dp(3))
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) selected.add(pkg) else selected.remove(pkg)
                        prefs.edit().putStringSet(ScrollLockAccessibilityService.KEY_SELECTED, selected.toSet()).apply()
                        selectedAppsCount = selected.size
                    }
                }
                list.addView(check)
                if (info != matches.last()) list.addView(separator())
            }
        }
        content.addView(list)
    }

    private fun startSession() {
        if (selected.isEmpty()) {
            statusLineSafe("Choose at least one app in Bag first.")
            showTab(0)
            return
        }
        if (!accessibilityEnabled()) {
            statusLineSafe("Enable Accessibility before starting.")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        prefs.edit()
            .putStringSet(ScrollLockAccessibilityService.KEY_SELECTED, selected.toSet())
            .putLong(ScrollLockAccessibilityService.KEY_END, System.currentTimeMillis() + durationMinutes * 60_000L)
            .putBoolean(ScrollLockAccessibilityService.KEY_ACTIVE, true)
            .apply()
        showTab(1)
    }

    private fun endSession() {
        prefs.edit().putBoolean(ScrollLockAccessibilityService.KEY_ACTIVE, false)
            .putLong(ScrollLockAccessibilityService.KEY_END, 0L).apply()
    }

    private fun sessionActive(): Boolean =
        prefs.getBoolean(ScrollLockAccessibilityService.KEY_ACTIVE, false) &&
            prefs.getLong(ScrollLockAccessibilityService.KEY_END, 0L) > System.currentTimeMillis()

    private fun refreshSessionText() {
        if (!::timerLine.isInitialized) return
        val end = prefs.getLong(ScrollLockAccessibilityService.KEY_END, 0L)
        val left = end - System.currentTimeMillis()
        timerLine.text = if (!sessionActive()) "No active session" else {
            val seconds = left / 1000
            "TIME LEFT  ·  %02d:%02d".format(seconds / 60, seconds % 60)
        }
    }

    private fun updateStatusLine() {
        if (!::statusLine.isInitialized) return
        statusLine.text = if (accessibilityEnabled()) "● Accessibility service enabled" else "● Accessibility service not enabled"
        statusLine.setTextColor(if (accessibilityEnabled()) accent else secondary)
    }

    private fun statusLineSafe(message: String) {
        if (::statusLine.isInitialized) statusLine.text = message
    }

    private fun accessibilityEnabled(): Boolean {
        val expected = ComponentName(this, ScrollLockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabled.split(':').any { it.equals(expected, true) }
    }

    private fun heading(title: String, subtitle: String) {
        content.addView(label(title, 27, primary, true, 1f).apply {
            setPadding(2, dp(9), 2, dp(4))
        })
        content.addView(label(subtitle, 13, secondary, false, 1f).apply {
            setPadding(2, 0, 2, dp(16))
        })
    }

    private fun sectionTitle(value: String) {
        content.addView(label(value, 10, secondary, true, 1f).apply {
            setPadding(2, 0, 2, dp(5))
        })
    }

    private fun sectionSubtitle(value: String = "Choose which apps ScrollLock should interrupt.") {
        content.addView(label(value, 12, secondary, false, 1f))
    }

    private fun metricCard(value: String, caption: String): View {
        val box = panel()
        box.addView(label(value, 29, primary, true, 1f))
        box.addView(label(caption, 9, secondary, true, 1f))
        return box
    }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(surface, 18)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun action(text: String, strong: Boolean, click: () -> Unit): TextView =
        label(text, 12, if (strong) Color.WHITE else accent, true, 1f).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(15), dp(12), dp(15))
            background = rounded(if (strong) accent else surface2, 14)
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dp(14)
            layoutParams = params
        }

    private fun label(
        value: String, size: Int, color: Int, bold: Boolean, weight: Float
    ) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        includeFontPadding = true
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, weight
        )
    }

    private fun separator() = View(this).apply {
        setBackgroundColor(Color.rgb(57, 52, 48))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ).apply { topMargin = dp(2); bottomMargin = dp(2) }
    }

    private fun gap(size: Int) {
        content.addView(View(this), LinearLayout.LayoutParams(1, dp(size)))
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
