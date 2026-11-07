package io.github.ctznsniiips.lobby.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** The current reading shown next to the clock. [code] is a WMO weather code; [day] drives sun vs moon. */
data class WeatherNow(val temp: Int, val code: Int, val day: Boolean) {
    val text: String get() = "$temp°"
}

/**
 * Fetches the current temperature and condition from Open-Meteo (the same source the Samsung app uses — free,
 * no key). The plugin supplies the location and unit; this just reads the number. Returns null on any failure.
 */
object WeatherService {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun current(config: WeatherConfig): WeatherNow? {
        if (!config.valid) return null
        val unit = if (config.fahrenheit) "fahrenheit" else "celsius"
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${config.latitude}&longitude=${config.longitude}" +
            "&current=temperature_2m,weather_code,is_day&temperature_unit=$unit"
        return runCatching {
            withContext(Dispatchers.IO) {
                http.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful || body.isBlank()) return@withContext null
                    val parsed = LobbyJson.json.decodeFromString(OpenMeteoResponse.serializer(), body)
                    val cur = parsed.current ?: return@withContext null
                    val temp = cur.temperature ?: return@withContext null
                    WeatherNow(temp = Math.round(temp).toInt(), code = cur.code ?: 3, day = cur.isDay != 0)
                }
            }
        }.getOrNull()
    }
}

@Serializable
private data class OpenMeteoResponse(@SerialName("current") val current: Current? = null) {
    @Serializable
    data class Current(
        @SerialName("temperature_2m") val temperature: Double? = null,
        @SerialName("weather_code") val code: Int? = null,
        @SerialName("is_day") val isDay: Int = 1,
    )
}
