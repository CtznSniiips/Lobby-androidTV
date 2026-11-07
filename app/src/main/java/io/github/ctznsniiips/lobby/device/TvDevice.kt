package io.github.ctznsniiips.lobby.device

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings

/** One launchable app in the dock. */
data class DockApp(val packageName: String, val label: String, val icon: Drawable?)

/**
 * Everything platform-specific, the Android TV counterpart of the Tizen app's device.js. Nothing outside
 * this class touches PackageManager, intents or system settings.
 */
class TvDevice(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    /** A stable id for this box, sent as the pairing "Duid" so the server can match it to the Jellyfin/Emby app's session. */
    fun deviceId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()

    fun deviceName(): String = listOf(Build.MANUFACTURER, Build.MODEL)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Android TV" }

    /**
     * Apps that declare themselves launchable on TV (LEANBACK_LAUNCHER), the documented mechanism, so no
     * hand-written allow/deny lists. Lobby itself is left out; the playback apps are listed first.
     */
    fun dockApps(): List<DockApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo.packageName to it }
            .filter { (pkg, _) -> pkg != context.packageName }
            .distinctBy { it.first }
            .map { (pkg, info) -> DockApp(pkg, info.loadLabel(pm).toString(), runCatching { info.activityInfo.loadBanner(pm) ?: info.loadIcon(pm) }.getOrNull()) }
            .sortedWith(compareBy<DockApp> { it.packageName !in PLAYBACK_PACKAGES }.thenBy { it.label.lowercase() })
            .toList()
    }

    fun launchApp(packageName: String): Boolean {
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName) ?: pm.getLaunchIntentForPackage(packageName) ?: return false
        return start(intent)
    }

    fun openSystemSettings(): Boolean = start(Intent(Settings.ACTION_SETTINGS))

    /**
     * Starts the screen saver the user picked in Android's own settings, the same as Settings -> Screen saver ->
     * Start now. Apps may not call the dream API directly (it needs a signature permission), so this opens the
     * SystemUI activity that does. It exists on most Android TV builds but not all, hence the Boolean.
     */
    fun startSystemScreensaver(): Boolean =
        start(Intent(Intent.ACTION_MAIN).setClassName("com.android.systemui", "com.android.systemui.Somnambulator"))

    /**
     * Whether Lobby is what the remote's Home button opens. Uses the Home role on Android 10+, and otherwise asks
     * which activity currently answers the HOME intent (the chooser, "android", counts as not set).
     */
    fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) return roles.isRoleHeld(RoleManager.ROLE_HOME)
        }
        val home = pm.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return home?.activityInfo?.packageName == context.packageName
    }

    /** Android's "default home app" picker, where the TV has one. */
    fun openHomeSettings(): Boolean = start(Intent(Settings.ACTION_HOME_SETTINGS))

    /** Where the user chooses the screen saver and Android's own "When to start" delay. */
    fun openScreenSaverSettings(): Boolean = start(Intent(Settings.ACTION_DREAM_SETTINGS)) || openSystemSettings()

    /** The installed official Jellyfin or Emby app that playback is handed to, if any. */
    fun playbackApp(): String? = PLAYBACK_PACKAGES.firstOrNull { isInstalled(it) }

    /** Opens the playback app after /Lobby/Play has queued the command on the server. */
    fun launchPlaybackApp(): Boolean = playbackApp()?.let(::launchApp) ?: false

    private fun isInstalled(packageName: String): Boolean =
        runCatching { pm.getPackageInfo(packageName, 0) }.isSuccess

    private fun start(intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    companion object {
        /** Official Android TV clients, in preference order. */
        val PLAYBACK_PACKAGES = listOf("org.jellyfin.androidtv", "tv.emby.embyatv", "org.jellyfin.androidtv.debug")
    }
}
