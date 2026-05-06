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
    private var lastQrTapAt = 0L

    override fun onServiceConnected() {
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isQrScanPending()) return
        if (event?.packageName?.toString() != SETTINGS_PACKAGE) return

        handler.postDelayed({ scanActiveSettingsWindow() }, 250)
        handler.postDelayed({ scanActiveSettingsWindow() }, 750)
        handler.postDelayed({ scanActiveSettingsWindow() }, 1_500)
    }

    private fun scanActiveSettingsWindow() {
        if (!isQrScanPending()) return

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != SETTINGS_PACKAGE) return

        if (containsAny(root, WRONG_DETAIL_LABELS)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }

        if (clickAny(root, CONFIRM_LABELS)) return

        if (containsAny(root, SCANNER_OPEN_LABELS)) {
            clearQrScanPending()
            return
        }

        if (tapQrPairingRow(root) || clickQrPairing(root)) {
            return
        }

        if (SystemClock.elapsedRealtime() - pendingStartedAt() > PENDING_TIMEOUT_MS) {
            clearQrScanPending()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (activeService === this) {
            activeService = null
        }
        super.onDestroy()
    }

    private fun scheduleScans() {
        handler.postDelayed({ scanActiveSettingsWindow() }, 250)
        handler.postDelayed({ scanActiveSettingsWindow() }, 750)
        handler.postDelayed({ scanActiveSettingsWindow() }, 1_500)
        handler.postDelayed({ scanActiveSettingsWindow() }, 2_500)
        handler.postDelayed({ scanActiveSettingsWindow() }, 4_000)
    }

    private fun isQrScanPending(): Boolean =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_QR_SCAN_PENDING, false)

    private fun pendingStartedAt(): Long =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_QR_SCAN_STARTED_AT, 0L)

    private fun clearQrScanPending() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_QR_SCAN_PENDING, false)
            .apply()
    }

    private fun clickAny(root: AccessibilityNodeInfo, labels: List<String>): Boolean {
        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label)
            val clicked = nodes.any {
                val text = it.visibleText()
                text.contains(label, ignoreCase = true) && clickNode(it)
            }
            if (clicked) return true
        }
        return false
    }

    private fun containsAny(root: AccessibilityNodeInfo, labels: List<String>): Boolean =
        labels.any { label ->
            root.findAccessibilityNodeInfosByText(label)
                .any { it.text?.toString()?.contains(label, ignoreCase = true) == true }
        }

    private fun clickQrPairing(root: AccessibilityNodeInfo): Boolean {
        val node = findNode(root) { text ->
            text.contains("pair", ignoreCase = true) &&
                text.contains("qr", ignoreCase = true) &&
                text.contains("code", ignoreCase = true)
        }
        return clickNode(node)
    }

    private fun tapQrPairingRow(root: AccessibilityNodeInfo): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastQrTapAt < TAP_THROTTLE_MS) return false

        val titleNode = findNode(root) { text ->
            text.contains("pair device", ignoreCase = true) &&
                text.contains("qr", ignoreCase = true) &&
                text.contains("code", ignoreCase = true)
        } ?: return false

        val rowNode = titleNode.findTapTarget()
        val bounds = Rect()
        rowNode.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return false

        lastQrTapAt = now
        return tap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
    }

    private fun AccessibilityNodeInfo.findTapTarget(): AccessibilityNodeInfo {
        var current: AccessibilityNodeInfo? = this
        var best: AccessibilityNodeInfo = this

        while (current != null) {
            val bounds = Rect()
            current.getBoundsInScreen(bounds)
            if (current.isEnabled && bounds.width() > 500 && bounds.height() > 80) {
                best = current
            }
            if (current.isClickable && current.isEnabled) return current
            current = current.parent
        }

        return best
    }

    private fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun findNode(
        node: AccessibilityNodeInfo?,
        matcher: (String) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null

        if (matcher(node.visibleText())) return node

        for (index in 0 until node.childCount) {
            val match = findNode(node.getChild(index), matcher)
            if (match != null) return match
        }

        return null
    }

    private fun AccessibilityNodeInfo.visibleText(): String =
        listOfNotNull(text, contentDescription)
            .joinToString(" ")
            .trim()

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

    companion object {
        private var activeService: DevHelpAccessibilityService? = null

        private const val SETTINGS_PACKAGE = "com.android.settings"
        private const val PREFS = "dev_help_accessibility"
        private const val KEY_QR_SCAN_PENDING = "qr_scan_pending"
        private const val KEY_QR_SCAN_STARTED_AT = "qr_scan_started_at"
        private const val PENDING_TIMEOUT_MS = 15_000L
        private const val TAP_THROTTLE_MS = 1_500L

        private val QR_PAIRING_LABELS = listOf(
            "Pair device with QR code",
            "Pair device over Wi-Fi by scanning a QR code",
            "Pair new devices using QR code scanner",
            "Scan QR code"
        )
        private val SCANNER_OPEN_LABELS = listOf(
            "QR code scanner",
            "Couldn't scan QR code"
        )
        private val WRONG_DETAIL_LABELS = listOf("Device details", "Device fingerprint")
        private val CONFIRM_LABELS = listOf("Allow", "OK")

        fun requestWirelessQrScan(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_QR_SCAN_PENDING, true)
                .putLong(KEY_QR_SCAN_STARTED_AT, SystemClock.elapsedRealtime())
                .apply()
            activeService?.scheduleScans()
        }
    }
}
