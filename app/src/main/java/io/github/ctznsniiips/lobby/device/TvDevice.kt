package io.github.ctznsniiips.lobby.device

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Build
import android.provider.Settings

/** One launchable app in the dock. */
data class DockApp(val packageName: String, val label: String, val icon: Drawable?)

/** One physical video input (HDMI, composite, ...) the TV can switch to. */
data class TvInput(val id: String, val label: String)

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

    /**
     * The TV's HDMI inputs, via the TV Input Framework (same approach as LtvLauncher): passthrough inputs only,
     * HDMI type first. Empty on boxes with no tuner/inputs (e.g. a streaming stick), so the UI hides the row.
     */
    fun tvInputs(): List<TvInput> = runCatching {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        manager.tvInputList
            .filter { it.isPassthroughInput && it.type == TvInputInfo.TYPE_HDMI }
            .map { TvInput(it.id, it.loadCustomLabel(context)?.toString()?.takeIf { l -> l.isNotBlank() } ?: it.loadLabel(context)?.toString() ?: it.id) }
            .distinctBy { it.id }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())

    /** Switches the TV to [inputId] by viewing its passthrough channel URI. */
    fun launchTvInput(inputId: String): Boolean = runCatching {
        start(Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId)))
    }.getOrDefault(false)

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

    /** Whether Lobby's Home-button override accessibility service is currently enabled. */
    fun isHomeOverrideEnabled(): Boolean {
        val ours = ComponentName(context.packageName, "${context.packageName}.HomeOverrideService")
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').mapNotNull { ComponentName.unflattenFromString(it) }.any { it == ours }
    }

    /** Android's Accessibility settings, where the user turns the Home-button override on or off. */
    fun openAccessibilitySettings(): Boolean = start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

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
        /**
         * Official Jellyfin/Emby clients playback is handed to, in preference order: the Android TV apps first,
         * then the general Android apps (which Google TV often installs instead of the TV-specific build), then
         * the Jellyfin debug build.
         */
        val PLAYBACK_PACKAGES = listOf(
            "org.jellyfin.androidtv",
            "tv.emby.embyatv",
            "com.mb.android",
            "org.jellyfin.mobile",
            "org.jellyfin.androidtv.debug",
        )
    }
}
