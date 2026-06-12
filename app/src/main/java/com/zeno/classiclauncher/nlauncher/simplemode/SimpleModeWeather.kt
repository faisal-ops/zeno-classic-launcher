package com.zeno.classiclauncher.nlauncher.simplemode

import android.content.Context
import android.location.Location
import com.zeno.classiclauncher.nlauncher.glance.GlanceDataSources
import com.zeno.classiclauncher.nlauncher.prefs.GlanceWeatherLocationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class NowPlayingState(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
)

data class SimpleModeWeatherDay(
    val label: String,
    val conditionEmoji: String,
    val tempMaxC: Float,
    val tempMinC: Float,
)

internal suspend fun fetchSimpleModeWeather(
    context: Context,
    locationMode: GlanceWeatherLocationMode,
    manualLatitude: String,
    manualLongitude: String,
): List<SimpleModeWeatherDay> = withContext(Dispatchers.IO) {
    val dataSources = GlanceDataSources(context)
    val loc: Location? = if (locationMode == GlanceWeatherLocationMode.DEVICE) {
        dataSources.getLastLocation()
    } else {
        val lat = manualLatitude.toFloatOrNull() ?: return@withContext emptyList()
        val lon = manualLongitude.toFloatOrNull() ?: return@withContext emptyList()
        Location("manual").apply {
            latitude = lat.toDouble()
            longitude = lon.toDouble()
        }
    }
    if (loc == null) return@withContext emptyList()

    var conn: HttpURLConnection? = null
    try {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=%.4f&longitude=%.4f".format(loc.latitude, loc.longitude) +
                "&daily=weathercode,temperature_2m_max,temperature_2m_min" +
                "&timezone=auto&forecast_days=5",
        )
        conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        if (conn.responseCode != 200) return@withContext emptyList()
        val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        val daily = JSONObject(body).getJSONObject("daily")
        val times = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weathercode")
        val maxTemps = daily.getJSONArray("temperature_2m_max")
        val minTemps = daily.getJSONArray("temperature_2m_min")

        val today = LocalDate.now()
        (0 until times.length()).map { i ->
            val date = LocalDate.parse(times.getString(i))
            val label = if (i == 0) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            SimpleModeWeatherDay(
                label = label,
                conditionEmoji = wmoCodeToEmoji(codes.getInt(i)),
                tempMaxC = maxTemps.getDouble(i).toFloat(),
                tempMinC = minTemps.getDouble(i).toFloat(),
            )
        }
    } catch (_: Exception) {
        emptyList()
    } finally {
        conn?.disconnect()
    }
}

private fun wmoCodeToEmoji(code: Int): String = when (code) {
    0 -> "☀"
    1 -> "☀"
    2 -> "⛅"
    3 -> "☁"
    45, 48 -> "🌫"
    51, 53, 55 -> "🌦"
    61, 63, 65 -> "🌧"
    71, 73, 75 -> "❄"
    80, 81, 82 -> "🌦"
    95, 96, 99 -> "⛈"
    else -> "☁"
}

internal fun formatTemp(tempC: Float, useCelsius: Boolean): String =
    if (useCelsius) "${tempC.toInt()}°" else "${(tempC * 9f / 5f + 32f).toInt()}°"
