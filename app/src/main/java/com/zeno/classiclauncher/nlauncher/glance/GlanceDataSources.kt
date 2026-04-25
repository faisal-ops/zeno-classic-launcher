package com.zeno.classiclauncher.nlauncher.glance

import android.Manifest
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONObject

internal data class GlanceWeatherData(
    val tempC: Float,
    val condition: String,
    val tempMinC: Float,
    val tempMaxC: Float,
)

internal data class GlanceCalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean = false,
    val calendarId: Long = 0L,
    val accountType: String = "",
)

/**
 * Collects the data-heavy pieces of the glance strip: weather, calendar, alarm and battery state.
 * The view keeps the animation/lifecycle shell; this helper owns the fetch/cache logic.
 */
internal class GlanceDataSources(private val context: Context) {
    private companion object {
        const val MAX_CALENDAR_EVENTS = 5
    }

    private var cachedWeather: GlanceWeatherData? = null
    private var weatherFetchedAt = 0L
    private var cachedLocation: Location? = null
    private var locationFetchedAt = 0L
    private var cachedCalendarEvents: List<GlanceCalendarEvent> = emptyList()
    private var calendarFetchedAt = 0L
    private val calendarAppPrefs by lazy {
        context.getSharedPreferences("zeno_glance_cal_app", Context.MODE_PRIVATE)
    }
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val weatherCacheMs = 2 * 60 * 60 * 1000L
    private val locationCacheMs = 2 * 60 * 60 * 1000L
    private val calendarCacheMs = 60 * 60 * 1000L

    fun cachedWeather(): GlanceWeatherData? = cachedWeather
    fun setCachedWeather(weather: GlanceWeatherData?) {
        cachedWeather = weather
        weatherFetchedAt = System.currentTimeMillis()
    }

    @Suppress("DEPRECATION")
    fun getLastLocation(): Location? {
        val now = System.currentTimeMillis()
        cachedLocation?.let { if (now - locationFetchedAt < locationCacheMs) return it }
        val lm = context.getSystemService<LocationManager>() ?: return null
        return try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
                ?.also { cachedLocation = it; locationFetchedAt = now }
        } catch (_: Exception) {
            null
        }
    }

    fun fetchWeather(useDeviceLocation: Boolean, latitude: Float?, longitude: Float?): GlanceWeatherData? {
        if (useDeviceLocation &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val now = System.currentTimeMillis()
        cachedWeather?.let { if (now - weatherFetchedAt < weatherCacheMs) return it }
        val loc = if (useDeviceLocation) {
            getLastLocation()
        } else {
            if (latitude == null || longitude == null) null else Location("manual").apply {
                this.latitude = latitude.toDouble()
                this.longitude = longitude.toDouble()
                time = now
            }
        } ?: return null
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=%.4f&longitude=%.4f".format(loc.latitude, loc.longitude) +
                    "&current=temperature_2m,weathercode" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto&forecast_days=1",
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode != 200) return null
            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            val json = JSONObject(body)
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            GlanceWeatherData(
                tempC = current.getDouble("temperature_2m").toFloat(),
                condition = weatherCodeToCondition(current.getInt("weathercode")),
                tempMinC = daily.getJSONArray("temperature_2m_min").getDouble(0).toFloat(),
                tempMaxC = daily.getJSONArray("temperature_2m_max").getDouble(0).toFloat(),
            ).also {
                cachedWeather = it
                weatherFetchedAt = now
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun getUpcomingCalendarEvents(lookAheadDays: Int): List<GlanceCalendarEvent> {
        val now = System.currentTimeMillis()
        if (cachedCalendarEvents.isNotEmpty() && now - calendarFetchedAt < calendarCacheMs) {
            return cachedCalendarEvents
        }
        val end = now + lookAheadDays.toLong() * 24L * 60 * 60 * 1000
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, end)
            it.build()
        }
        return try {
            val events = mutableListOf<GlanceCalendarEvent>()
            val calendarIds = mutableSetOf<Long>()
            context.contentResolver.query(
                uri,
                arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.CALENDAR_ID,
                    CalendarContract.Instances.ALL_DAY,
                ),
                "${CalendarContract.Instances.END} >= ?",
                arrayOf(now.toString()),
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                while (cursor.moveToNext() && events.size < MAX_CALENDAR_EVENTS) {
                    val title = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: return@use
                    val calId = cursor.getLong(4)
                    events.add(
                        GlanceCalendarEvent(
                            id = cursor.getLong(0),
                            title = title,
                            startTime = cursor.getLong(2),
                            endTime = cursor.getLong(3),
                            isAllDay = cursor.getInt(5) != 0,
                            calendarId = calId,
                        ),
                    )
                    calendarIds.add(calId)
                }
            }
            val accountTypeMap = mutableMapOf<Long, String>()
            if (calendarIds.isNotEmpty()) {
                val selection = "${CalendarContract.Calendars._ID} IN (${calendarIds.joinToString(",")})"
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.ACCOUNT_TYPE),
                    selection,
                    null,
                    null,
                )?.use { cal ->
                    while (cal.moveToNext()) {
                        accountTypeMap[cal.getLong(0)] = cal.getString(1) ?: ""
                    }
                }
            }
            events.map { it.copy(accountType = accountTypeMap[it.calendarId] ?: "") }.also {
                cachedCalendarEvents = it
                calendarFetchedAt = now
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getNextAlarmInfo(): String? {
        val am = context.getSystemService<AlarmManager>() ?: return null
        val next = am.nextAlarmClock ?: return null
        val diff = next.triggerTime - System.currentTimeMillis()
        if (diff < 0 || diff > 12L * 60 * 60 * 1000) return null
        val h = diff / (60 * 60 * 1000)
        val m = (diff % (60 * 60 * 1000)) / (60 * 1000)
        return if (h > 0) "Alarm in ${h}h ${m}m" else "Alarm in ${m}m"
    }

    fun formatEventTime(startTime: Long): String {
        val diff = startTime - System.currentTimeMillis()
        return when {
            diff <= 0 -> context.getString(com.zeno.classiclauncher.nlauncher.R.string.glance_event_now)
            diff < 60 * 60 * 1000 -> context.getString(
                com.zeno.classiclauncher.nlauncher.R.string.glance_event_in_minutes,
                (diff / (60 * 1000)).toInt(),
            )
            else -> timeFormat.format(Date(startTime))
        }
    }

    fun formatCalendarEvent(event: GlanceCalendarEvent): String =
        buildString {
            append(trimCalendarTitle(event.title))
            append(" \u00B7 ")
            append(if (event.isAllDay) "All day" else formatEventTime(event.startTime).uppercase(Locale.getDefault()))
        }

    private fun trimCalendarTitle(title: String, maxChars: Int = 24): String {
        val clean = title.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= maxChars) return clean
        return clean.take(maxChars).trimEnd() + "\u2026"
    }

    fun selectRelevantCalendarEvents(
        events: List<GlanceCalendarEvent>,
        nowMs: Long = System.currentTimeMillis(),
    ): List<GlanceCalendarEvent> {
        if (events.isEmpty()) return emptyList()

        fun isSameLocalDay(a: Long, b: Long): Boolean {
            val calA = Calendar.getInstance().apply { timeInMillis = a }
            val calB = Calendar.getInstance().apply { timeInMillis = b }
            return calA.get(Calendar.ERA) == calB.get(Calendar.ERA) &&
                calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
                calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
        }

        val tomorrowMs = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val ongoing = events.firstOrNull { !it.isAllDay && it.startTime <= nowMs && it.endTime > nowMs }
        val nextTimedToday = events.firstOrNull { !it.isAllDay && it.startTime > nowMs && isSameLocalDay(it.startTime, nowMs) }
        val allDayToday = events.firstOrNull { it.isAllDay && isSameLocalDay(it.startTime, nowMs) }
        val tomorrowFirst = events.firstOrNull { it.startTime > nowMs && isSameLocalDay(it.startTime, tomorrowMs) }

        val ordered = buildList {
            add(ongoing ?: nextTimedToday ?: allDayToday ?: tomorrowFirst ?: return@buildList)
            when (firstOrNull()) {
                ongoing -> add(nextTimedToday ?: allDayToday ?: tomorrowFirst)
                nextTimedToday -> add(allDayToday ?: tomorrowFirst)
                allDayToday -> add(tomorrowFirst)
                else -> Unit
            }
        }

        return ordered
            .filterNotNull()
            .distinctBy { it.id }
            .take(2)
    }

    private fun weatherCodeToCondition(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Storm"
        else -> "\u2014"
    }
}
