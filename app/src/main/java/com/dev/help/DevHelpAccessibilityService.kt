package com.dev.help

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class DevHelpAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastMenuTapAt = 0L
    private var lastScrollAt = 0L

    override fun onServiceConnected() {
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != SETTINGS_PACKAGE) return
        if (!isSpecialAccessPending()) return

        scheduleSpecialAccessScans(250L, 700L, 1_200L)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (activeService === this) {
            activeService = null
        }
        super.onDestroy()
    }

    private fun scheduleSpecialAccessScans(vararg delays: Long) {
        delays.forEach { delay ->
            handler.postDelayed({ scanSpecialAccessWindow() }, delay)
        }
    }

    private fun scanSpecialAccessWindow() {
        if (!isSpecialAccessPending()) return

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != SETTINGS_PACKAGE) return

        if (containsAny(root, SPECIAL_ACCESS_OPEN_LABELS)) {
            clearSpecialAccessPending()
            return
        }

        if (clickAny(root, SPECIAL_ACCESS_MENU_LABELS)) return

        if (clickAny(root, MORE_OPTIONS_LABELS)) {
            scheduleSpecialAccessScans(350L, 900L)
            return
        }

        if (tapTopRightMenu(root)) {
            scheduleSpecialAccessScans(350L, 900L)
            return
        }

        if (scrollSettings(root)) {
            scheduleSpecialAccessScans(650L)
            return
        }

        if (SystemClock.elapsedRealtime() - specialAccessPendingStartedAt() > PENDING_TIMEOUT_MS) {
            clearSpecialAccessPending()
        }
    }

    private fun isSpecialAccessPending(): Boolean =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SPECIAL_ACCESS_PENDING, false)

    private fun specialAccessPendingStartedAt(): Long =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_SPECIAL_ACCESS_STARTED_AT, 0L)

    private fun clearSpecialAccessPending() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SPECIAL_ACCESS_PENDING, false)
            .apply()
    }

    private fun clickAny(root: AccessibilityNodeInfo, labels: List<String>): Boolean {
        for (label in labels) {
            val clicked = root.findAccessibilityNodeInfosByText(label).any { node ->
                node.visibleText().contains(label, ignoreCase = true) && clickNode(node)
            }
            if (clicked) return true
        }
        return false
    }

    private fun containsAny(root: AccessibilityNodeInfo, labels: List<String>): Boolean =
        labels.any { label ->
            root.findAccessibilityNodeInfosByText(label).any { node ->
                node.visibleText().contains(label, ignoreCase = true)
            }
        }

    private fun tapTopRightMenu(root: AccessibilityNodeInfo): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastMenuTapAt < MENU_TAP_THROTTLE_MS) return false

        val bounds = Rect()
        root.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return false

        lastMenuTapAt = now
        return tap(bounds.right - 90f, bounds.top + 190f)
    }

    private fun scrollSettings(root: AccessibilityNodeInfo): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastScrollAt < SCROLL_THROTTLE_MS) return false

        lastScrollAt = now
        return scrollForward(root) || swipeUp(root)
    }

    private fun scrollForward(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isScrollable &&
            node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        ) {
            return true
        }

        for (index in 0 until node.childCount) {
            if (scrollForward(node.getChild(index))) return true
        }

        return false
    }

    private fun swipeUp(root: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        root.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return false

        val x = bounds.centerX().toFloat()
        val startY = bounds.bottom - bounds.height() * 0.22f
        val endY = bounds.top + bounds.height() * 0.38f
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable && current.isEnabled) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    private fun AccessibilityNodeInfo.visibleText(): String =
        listOfNotNull(text, contentDescription)
            .joinToString(" ")
            .trim()

    companion object {
        private var activeService: DevHelpAccessibilityService? = null

        private const val SETTINGS_PACKAGE = "com.android.settings"
        private const val PREFS = "dev_help_accessibility"
        private const val KEY_SPECIAL_ACCESS_PENDING = "special_access_pending"
        private const val KEY_SPECIAL_ACCESS_STARTED_AT = "special_access_started_at"
        private const val PENDING_TIMEOUT_MS = 20_000L
        private const val MENU_TAP_THROTTLE_MS = 1_000L
        private const val SCROLL_THROTTLE_MS = 700L

        private val MORE_OPTIONS_LABELS = listOf(
            "More options",
            "More",
            "Menu"
        )
        private val SPECIAL_ACCESS_MENU_LABELS = listOf(
            "Special access",
            "Special app access"
        )
        private val SPECIAL_ACCESS_OPEN_LABELS = listOf(
            "Appear on top",
            "Change system settings",
            "Install unknown apps",
            "Usage data access",
            "Notification access",
            "Picture-in-picture"
        )

        fun requestSpecialAccess(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SPECIAL_ACCESS_PENDING, true)
                .putLong(KEY_SPECIAL_ACCESS_STARTED_AT, SystemClock.elapsedRealtime())
                .apply()
            activeService?.scheduleSpecialAccessScans(
                250L,
                700L,
                1_200L,
                2_000L,
                3_000L,
                4_500L,
                6_000L,
                8_000L,
                10_000L,
                13_000L,
                16_000L
            )
        }
    }
}
