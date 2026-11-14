package io.github.ctznsniiips.lobby

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

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

    private fun launchLobby() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {}
}
