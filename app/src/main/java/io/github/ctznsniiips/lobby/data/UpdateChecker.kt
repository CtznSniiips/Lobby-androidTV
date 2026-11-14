package io.github.ctznsniiips.lobby.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Checks the project's GitHub releases for a build newer than the one installed, and can fetch its APK. */
object UpdateChecker {

    data class Release(
        val version: String,
        val url: String,
        val notes: String,
        val apkUrl: String?,
    )

    private const val LATEST = "https://api.github.com/repos/CtznSniiips/Lobby-androidTV/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** The latest published release, or null if it couldn't be reached/parsed. Runs off the main thread. */
    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(LATEST).header("Accept", "application/vnd.github+json").build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val json = JSONObject(resp.body?.string() ?: return@use null)
                val tag = json.optString("tag_name").ifBlank { return@use null }
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url").ifBlank { null }
                            break
                        }
                    }
                }
                Release(tag.removePrefix("v"), json.optString("html_url"), json.optString("body").trim(), apkUrl)
            }
        }.getOrNull()
    }

    /** Streams the APK at [url] into [dest]. Returns true on success. Runs off the main thread. */
    suspend fun download(url: String, dest: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use false
                val body = resp.body ?: return@use false
                dest.outputStream().use { out -> body.byteStream().copyTo(out) }
                true
            }
        }.getOrDefault(false)
    }

    /** True when [latest] is a higher semantic version than [current] (both like "0.3.0"). */
    fun isNewer(latest: String, current: String): Boolean {
        if (current.isBlank()) return false
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parts(v: String): List<Int> =
        v.trim().split('.', '-', '+').mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
}
