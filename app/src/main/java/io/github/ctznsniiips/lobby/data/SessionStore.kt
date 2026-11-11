package io.github.ctznsniiips.lobby.data

import android.content.Context

/** A saved pairing: where the server is and the token it issued to this TV. */
data class PairedServer(val address: ServerAddress, val token: String, val tvName: String)

/** Persists the pairing. A TV launcher has no user account to protect it, so plain private prefs are used. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("lobby", Context.MODE_PRIVATE)

    fun load(): PairedServer? {
        val origin = prefs.getString(KEY_ORIGIN, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        if (token.length < 16) return null
        return PairedServer(
            ServerAddress(origin, prefs.getString(KEY_PREFIX, "") ?: ""),
            token,
            prefs.getString(KEY_NAME, "") ?: "",
        )
    }

    fun save(server: PairedServer) {
        prefs.edit()
            .putString(KEY_ORIGIN, server.address.origin)
            .putString(KEY_PREFIX, server.address.prefix)
            .putString(KEY_TOKEN, server.token)
            .putString(KEY_NAME, server.tvName)
            .apply()
    }

    /** Idle minutes before Lobby starts the system screen saver; 0 (the default) is off. This is a per-device choice, kept locally. */
    fun screenSaverMinutes(): Int = prefs.getInt(KEY_SCREEN_SAVER, 0)

    fun setScreenSaverMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SCREEN_SAVER, minutes).apply()
    }

    /** True once the user has said "Not now" to the make-Lobby-your-home-screen prompt on Home. */
    fun homePromptDismissed(): Boolean = prefs.getBoolean(KEY_HOME_PROMPT_DISMISSED, false)

    fun setHomePromptDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_PROMPT_DISMISSED, dismissed).apply()
    }

    /** Packages the user has hidden from the Home app dock. */
    fun hiddenApps(): Set<String> = prefs.getStringSet(KEY_HIDDEN_APPS, emptySet())?.toSet() ?: emptySet()

    fun setHiddenApps(values: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, values).apply()
    }

    /** The chosen home font id (manrope, geist, newsreader). */
    fun homeFont(): String = prefs.getString(KEY_HOME_FONT, "manrope") ?: "manrope"

    fun setHomeFont(id: String) {
        prefs.edit().putString(KEY_HOME_FONT, id).apply()
    }

    /** The Background mode: dark, light or photo. */
    fun bgMode(): String = prefs.getString(KEY_BG_MODE, "dark") ?: "dark"

    fun setBgMode(mode: String) {
        prefs.edit().putString(KEY_BG_MODE, mode).apply()
    }

    /** The chosen wallpaper id for Photo mode. */
    fun wallpaper(): String = prefs.getString(KEY_WALLPAPER, "misty") ?: "misty"

    fun setWallpaper(id: String) {
        prefs.edit().putString(KEY_WALLPAPER, id).apply()
    }

    /** Cached copy of the server's "combine Continue Watching + Next Up" TV setting, so Home can apply it at launch. */
    fun combineContinueNextUp(): Boolean = prefs.getBoolean(KEY_COMBINE_CONTINUE, false)

    fun setCombineContinueNextUp(value: Boolean) {
        prefs.edit().putBoolean(KEY_COMBINE_CONTINUE, value).apply()
    }

    /** The Focus style: "type" (solid fill) or "glass" (translucent plate). */
    fun focusStyle(): String = prefs.getString(KEY_FOCUS_STYLE, "type") ?: "type"

    fun setFocusStyle(id: String) {
        prefs.edit().putString(KEY_FOCUS_STYLE, id).apply()
    }

    /** The glass Material (only meaningful over a photo background): "soft", "frost" or "tint". */
    fun material(): String = prefs.getString(KEY_MATERIAL, "soft") ?: "soft"

    fun setMaterial(id: String) {
        prefs.edit().putString(KEY_MATERIAL, id).apply()
    }

    /** The user's preferred order of dock apps, by package name. Apps not listed keep their detected order after these. */
    fun appOrder(): List<String> = prefs.getString(KEY_APP_ORDER, null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

    fun setAppOrder(order: List<String>) {
        prefs.edit().putString(KEY_APP_ORDER, order.joinToString("\n")).apply()
    }

    /** TV input ids the user has hidden from the Home inputs row. */
    fun hiddenInputs(): Set<String> = prefs.getStringSet(KEY_HIDDEN_INPUTS, emptySet())?.toSet() ?: emptySet()

    fun setHiddenInputs(values: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN_INPUTS, values).apply()
    }

    fun lastOrigin(): String? = prefs.getString(KEY_ORIGIN, null)

    fun forget() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_NAME).apply()
    }

    private companion object {
        const val KEY_ORIGIN = "origin"
        const val KEY_PREFIX = "prefix"
        const val KEY_TOKEN = "token"
        const val KEY_NAME = "tvName"
        const val KEY_SCREEN_SAVER = "screenSaverMinutes"
        const val KEY_HOME_PROMPT_DISMISSED = "homePromptDismissed"
        const val KEY_HIDDEN_APPS = "hiddenApps"
        const val KEY_HIDDEN_INPUTS = "hiddenInputs"
        const val KEY_APP_ORDER = "appOrder"
        const val KEY_HOME_FONT = "homeFont"
        const val KEY_BG_MODE = "bgMode"
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_FOCUS_STYLE = "focusStyle"
        const val KEY_MATERIAL = "material"
        const val KEY_COMBINE_CONTINUE = "combineContinueNextUp"
    }
}
