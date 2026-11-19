package io.github.ctznsniiips.lobby

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.ctznsniiips.lobby.device.AdbPairingBridge

/**
 * Recaptures the remote's Home button on devices that keep it wired to the stock launcher even after Lobby is
 * chosen as the home app (most Google TV boxes). Two mechanisms, because no single one works everywhere:
 *
 *  1. Key filtering: where the platform delivers KEYCODE_HOME to accessibility services, we consume it and bring
 *     Lobby forward directly — no flicker.
 *  2. Window bounce: where it doesn't (the common case on Google TV), we watch for the stock launcher's Home screen
 *     coming to the foreground — which only happens when Home is pressed or at boot — and immediately relaunch Lobby
 *     in front of it. This is the same approach Projectivy's Home-button override uses.
 *
 * The bounce matches the launcher's exact Home *activity* (package + class), never just its package. On Google TV
 * the system Settings surface, notification shade and other panels are hosted by the same launcher package, so
 * bouncing on the package alone would also yank the user out of Settings the instant it opened.
 *
 * The service only runs while the user has enabled it under Settings -> Accessibility, so its presence is the
 * opt-in; there's nothing to toggle here.
 */
class HomeOverrideService : AccessibilityService() {

    /** Flattened "package/class" of every stock launcher Home activity (minus Lobby's). Cached, refreshed lazily. */
    @Volatile private var homeActivities: Set<String> = emptySet()
    private var lastBounce = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshHomeActivities()
    }

    private fun refreshHomeActivities() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        homeActivities = packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { it.activityInfo }
            .filter { it.packageName != packageName } // not Lobby itself
            .map { "${it.packageName}/${it.name}" }
            .toSet()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_HOME && event.action == KeyEvent.ACTION_UP) {
            launchLobby()
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Experimental auto-pair: while armed, read the 6-digit code off the live Wireless Debugging pairing dialog
        // (any non-Lobby window) so Lobby can pair without the user leaving the dialog. Runs on content changes too,
        // since the code can render a beat after the window opens.
        if (AdbPairingBridge.listening) {
            val evtPkg = event.packageName?.toString()
            if (evtPkg != null && evtPkg != packageName) {
                scrapePairing(rootInActiveWindow)?.let { AdbPairingBridge.submit(it) }
            }
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        if (pkg == packageName) return
        val component = "$pkg/$cls"
        if (component !in homeActivities) {
            // A launcher we haven't seen yet may have been installed/updated; re-check once before ignoring.
            if (homeActivities.isEmpty()) {
                refreshHomeActivities()
                if (component !in homeActivities) return
            } else {
                return
            }
        }
        // Debounce: a single Home press can emit several window events.
        val now = SystemClock.uptimeMillis()
        if (now - lastBounce < 600L) return
        lastBounce = now
        launchLobby()
    }

    /**
     * Read the pairing dialog in one DFS: a text node that is exactly six digits is the code; a node matching
     * "IP:port" is the pairing endpoint. Returns null if neither was found (not the pairing dialog, or not rendered yet).
     */
    private fun scrapePairing(root: AccessibilityNodeInfo?): AdbPairingBridge.Scraped? {
        root ?: return null
        var code: String? = null
        var host: String? = null
        var port = 0
        fun consider(raw: String?) {
            val t = raw?.trim() ?: return
            if (code == null && SIX_DIGITS.matches(t)) code = t
            if (host == null) IP_PORT.find(t)?.let { m ->
                host = m.groupValues[1]
                port = m.groupValues[2].toIntOrNull() ?: 0
            }
        }
        fun visit(node: AccessibilityNodeInfo) {
            // The value can live in text or contentDescription depending on the view, so check both.
            consider(node.text?.toString())
            consider(node.contentDescription?.toString())
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { visit(it) }
            }
        }
        visit(root)
        return if (code != null || host != null) AdbPairingBridge.Scraped(code, host, port) else null
    }

    private fun launchLobby() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {}

    companion object {
        private val SIX_DIGITS = Regex("^\\d{6}$")
        private val IP_PORT = Regex("(\\d{1,3}(?:\\.\\d{1,3}){3})\\s*:\\s*(\\d{2,5})")
    }
}
