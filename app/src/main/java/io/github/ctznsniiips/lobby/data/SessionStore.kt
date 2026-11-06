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

    fun lastOrigin(): String? = prefs.getString(KEY_ORIGIN, null)

    fun forget() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_NAME).apply()
    }

    private companion object {
        const val KEY_ORIGIN = "origin"
        const val KEY_PREFIX = "prefix"
        const val KEY_TOKEN = "token"
        const val KEY_NAME = "tvName"
    }
}
