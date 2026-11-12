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
 *  2. Window bounce: where it doesn't (the common case on Google TV), we watch for a stock launcher coming to the
 *     foreground — which only happens when Home is pressed or at boot — and immediately relaunch Lobby in front of
 *     it. This is the same approach Projectivy's Home-button override uses.
 *
 * The service only runs while the user has enabled it under Settings -> Accessibility, so its presence is the
 * opt-in; there's nothing to toggle here.
 */
class HomeOverrideService : AccessibilityService() {

    /** Packages that answer the HOME intent (the launchers to bounce off), minus Lobby itself. Cached, refreshed lazily. */
    @Volatile private var launcherPackages: Set<String> = emptySet()
    private var lastBounce = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshLaunchers()
    }

    private fun refreshLaunchers() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val found = packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { it.activityInfo?.packageName }
            .toMutableSet()
        // Common stock launchers, in case the query misses one on a given build.
        found += KNOWN_LAUNCHERS
        found -= packageName
        launcherPackages = found
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
        if (pkg == packageName) return
        if (pkg !in launcherPackages) {
            // A launcher we haven't seen yet may have been installed/updated; re-check once before ignoring.
            if (launcherPackages.isEmpty()) refreshLaunchers()
            if (pkg !in launcherPackages) return
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

    private companion object {
        val KNOWN_LAUNCHERS = setOf(
            "com.google.android.apps.tv.launcherx",
            "com.google.android.tvlauncher",
            "com.google.android.tungsten.setupwraith",
        )
    }
}
