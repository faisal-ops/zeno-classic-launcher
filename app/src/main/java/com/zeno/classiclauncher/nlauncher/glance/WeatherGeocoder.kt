package com.zeno.classiclauncher.nlauncher.glance

import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One geocoding match for the manual weather-location picker. */
internal data class GeoCity(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** "Faridabad, Haryana, India" — parts omitted when the API leaves them blank. */
    val label: String
        get() = listOf(name, region, country).filter { it.isNotBlank() }.joinToString(", ")
}

/**
 * City-name → coordinates via Open-Meteo's geocoding API — same provider as the weather
 * fetch, keyless HTTPS, and localized results (`language` accepts the UI locale, so a
 * Hindi user can search "दिल्ली" as well as "Delhi").
 */
internal object WeatherGeocoder {

    suspend fun search(query: String, language: String): List<GeoCity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${Uri.encode(trimmed)}&count=8&language=${Uri.encode(language)}&format=json"
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode != 200) return@withContext emptyList()
            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            parseGeocodingResults(body)
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }
}

internal fun parseGeocodingResults(json: String): List<GeoCity> = runCatching {
    val arr = JSONObject(json).optJSONArray("results") ?: return@runCatching emptyList()
    (0 until arr.length()).mapNotNull { i ->
        val o = arr.getJSONObject(i)
        val name = o.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val lat = o.optDouble("latitude", Double.NaN)
        val lon = o.optDouble("longitude", Double.NaN)
        if (lat.isNaN() || lon.isNaN()) return@mapNotNull null
        GeoCity(
            name = name,
            region = o.optString("admin1"),
            country = o.optString("country"),
            latitude = lat,
            longitude = lon,
        )
    }
}.getOrDefault(emptyList())
