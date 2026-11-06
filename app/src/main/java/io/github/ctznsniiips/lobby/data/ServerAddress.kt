package io.github.ctznsniiips.lobby.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * A reachable Emby/Jellyfin server. [origin] is "scheme://host:port"; [prefix] is "" (Jellyfin, or Emby
 * answering bare) or "/emby" (an Emby server that only answered under that alias).
 */
data class ServerAddress(val origin: String, val prefix: String) {
    /** Retrofit base URL; always ends with a slash. */
    val baseUrl: String get() = "$origin$prefix/"

    /** Image paths from the plugin are bare, server-relative paths; both servers answer them without a prefix. */
    fun imageUrl(path: String?, width: Int = 400): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("https://image.tmdb.org/")) return path
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        if (!path.startsWith("/") || path.startsWith("//")) return null
        val sep = if (path.contains('?')) '&' else '?'
        return "$origin$path${sep}maxWidth=$width&quality=85"
    }
}

object ServerProbe {
    private const val DEFAULT_PORT = 8096
    private const val DEFAULT_TLS_PORT = 443

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Same rules as the Tizen app: a bare IPv4 address means http on 8096; a hostname means https on 443;
     * an explicit scheme and/or port is respected. Returns null for anything that isn't an address.
     */
    fun normalise(input: String): String? {
        var s = input.trim().trimEnd('/')
        var scheme = "http"
        Regex("^(https?)://", RegexOption.IGNORE_CASE).find(s)?.let {
            scheme = it.groupValues[1].lowercase()
            s = s.substring(it.value.length)
        }
        val m = Regex("^([A-Za-z0-9](?:[A-Za-z0-9.\\-]{0,251}[A-Za-z0-9])?)(?::(\\d{1,5}))?$").find(s) ?: return null
        var host = m.groupValues[1]
        val isIp = host.all { it.isDigit() || it == '.' }
        if (isIp && !Regex("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$").matches(host)) return null
        if (!isIp) {
            scheme = "https"
            host = host.lowercase()
        }
        val port = m.groupValues[2].toIntOrNull() ?: if (isIp) DEFAULT_PORT else DEFAULT_TLS_PORT
        if (port !in 1..65535) return null
        return "$scheme://$host:$port"
    }

    class ProbeResult(val address: ServerAddress, val serverName: String)

    /** Tries the bare path first (Jellyfin, and Emby too), then Emby's "/emby" alias. Blocking; call off the main thread. */
    fun probe(origin: String): Result<ProbeResult> {
        val bare = probeOnce(origin, "")
        if (bare.isSuccess) return bare
        val aliased = probeOnce(origin, "/emby")
        return if (aliased.isSuccess) aliased else bare
    }

    private fun probeOnce(origin: String, prefix: String): Result<ProbeResult> = runCatching {
        val request = Request.Builder().url("$origin$prefix/System/Info/Public").header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            val info = LobbyJson.json.decodeFromString(PublicSystemInfo.serializer(), body)
            if (info.version.isNullOrBlank()) error("That address answered, but it isn't an Emby or Jellyfin server.")
            ProbeResult(ServerAddress(origin, prefix), info.serverName ?: "Server")
        }
    }
}
