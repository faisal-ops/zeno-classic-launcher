package com.zeno.classiclauncher.nlauncher.glance

import android.Manifest
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Looper
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.zeno.classiclauncher.nlauncher.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
        const val PERSIST_DEVICE = "device"
        const val PERSIST_IP = "ip"
        const val FRESH_FIX_TIMEOUT_MS = 30_000L // GPS cold start regularly needs >15 s
        const val DEVICE_FIX_REUSE_MS = 24L * 60 * 60 * 1000 // same city assumption for weather
        const val IP_FIX_REUSE_MS = 6L * 60 * 60 * 1000 // don't re-hit the geolocation service
        val IP_GEO_ENDPOINTS = listOf(
            "https://get.geojs.io/v1/ip/geo.json",
            "https://ipwho.is/",
        )
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
    // Skeleton-based pattern: locale-correct format and 12/24h per system setting.
    private val timeFormat = SimpleDateFormat(
        android.text.format.DateFormat.getBestDateTimePattern(
            Locale.getDefault(),
            if (android.text.format.DateFormat.is24HourFormat(context)) "Hm" else "hm",
        ),
        Locale.getDefault(),
    )
    private val weatherCacheMs = 2 * 60 * 60 * 1000L
    private val locationCacheMs = 2 * 60 * 60 * 1000L
    private val calendarCacheMs = 60 * 60 * 1000L

    fun cachedWeather(): GlanceWeatherData? = cachedWeather
    fun setCachedWeather(weather: GlanceWeatherData?) {
        cachedWeather = weather
        weatherFetchedAt = System.currentTimeMillis()
    }

    /**
     * Resolves a usable location through a chain of Google-free fallbacks so weather keeps
     * working on any ROM (stock, Lineage, degoogled) and survives process restarts:
     *
     *  1. In-memory fix under 2 h old.
     *  2. Passive [LocationManager.getLastKnownLocation] under 2 h old — a fix some other
     *     app already requested. On ROMs without a network location backend (degoogled,
     *     no microG) this may never update after boot, hence the tiers below.
     *  3. Active single fix request (GPS/network) with a 30 s timeout — GPS cold starts
     *     routinely need more than the 15 s this used to allow.
     *  4. Device fix persisted to disk by a previous session (< 24 h).
     *  5. IP-based geolocation — city-level, no permissions, works degoogled. Skipped when
     *     a VPN is active (it would resolve the exit server's city) and sanity-checked
     *     against the last real device fix.
     *  6. Any persisted fix, however old — the right city beats no weather at all.
     */
    @Suppress("DEPRECATION")
    suspend fun getLastLocation(): Location? {
        val now = System.currentTimeMillis()
        cachedLocation?.let { if (now - locationFetchedAt < locationCacheMs) return it }
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val lm = context.getSystemService<LocationManager>() ?: return null
        val passive = try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
        } catch (_: Exception) {
            null
        }
        if (passive != null && now - passive.time < locationCacheMs) {
            return passive.also { remember(it); persistFix(PERSIST_DEVICE, it) }
        }
        requestFreshLocation(lm)?.let { fresh ->
            return fresh.also { remember(it); persistFix(PERSIST_DEVICE, it) }
        }
        // Stale passive fix is still a genuine device fix — persist with its own timestamp.
        passive?.let { persistFix(PERSIST_DEVICE, it) }

        readPersistedFix(PERSIST_DEVICE)?.let { fix ->
            if (now - fix.timeMs < DEVICE_FIX_REUSE_MS) return fix.toLocation().also { remember(it) }
        }
        readPersistedFix(PERSIST_IP)?.let { fix ->
            if (now - fix.timeMs < IP_FIX_REUSE_MS) return fix.toLocation().also { remember(it) }
        }
        if (!isVpnActive()) {
            fetchIpLocation()?.let { ip ->
                val device = readPersistedFix(PERSIST_DEVICE)
                val trusted = shouldTrustIpLocation(
                    ipLatitude = ip.latitude,
                    ipLongitude = ip.longitude,
                    lastDeviceLatitude = device?.latitude,
                    lastDeviceLongitude = device?.longitude,
                    lastDeviceAgeMs = device?.let { now - it.timeMs },
                )
                if (trusted) {
                    persistFix(PERSIST_IP, ip)
                    return ip.also { remember(it) }
                }
                if (device != null) return device.toLocation().also { remember(it) }
            }
        }
        (readPersistedFix(PERSIST_DEVICE) ?: readPersistedFix(PERSIST_IP))?.let { fix ->
            return fix.toLocation().also { remember(it) }
        }
        return null
    }

    private fun remember(loc: Location) {
        cachedLocation = loc
        locationFetchedAt = System.currentTimeMillis()
    }

    private data class PersistedFix(val latitude: Double, val longitude: Double, val timeMs: Long) {
        fun toLocation(): Location = Location("cache").apply {
            latitude = this@PersistedFix.latitude
            longitude = this@PersistedFix.longitude
            time = timeMs
        }
    }

    private val locationStore by lazy {
        context.getSharedPreferences("zeno_weather_location", Context.MODE_PRIVATE)
    }

    private fun persistFix(prefix: String, loc: Location) {
        val fixTime = if (loc.time > 0) loc.time else System.currentTimeMillis()
        locationStore.edit()
            .putLong("${prefix}_lat", loc.latitude.toRawBits())
            .putLong("${prefix}_lon", loc.longitude.toRawBits())
            .putLong("${prefix}_time", fixTime)
            .apply()
    }

    private fun readPersistedFix(prefix: String): PersistedFix? {
        if (!locationStore.contains("${prefix}_time")) return null
        return PersistedFix(
            latitude = Double.fromBits(locationStore.getLong("${prefix}_lat", 0L)),
            longitude = Double.fromBits(locationStore.getLong("${prefix}_lon", 0L)),
            timeMs = locationStore.getLong("${prefix}_time", 0L),
        )
    }

    private fun isVpnActive(): Boolean = runCatching {
        val cm = context.getSystemService<ConnectivityManager>()
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }.getOrDefault(false)

    /** City-level fix from the public IP. Two providers, both keyless HTTPS. */
    private fun fetchIpLocation(): Location? {
        for (endpoint in IP_GEO_ENDPOINTS) {
            val body = httpGetJson(endpoint) ?: continue
            val (lat, lon) = parseIpLocation(body) ?: continue
            return Location("ip").apply {
                latitude = lat
                longitude = lon
                time = System.currentTimeMillis()
            }
        }
        return null
    }

    private fun httpGetJson(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode != 200) {
                null
            } else {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private suspend fun requestFreshLocation(lm: LocationManager): Location? {
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return null
        return withTimeoutOrNull(FRESH_FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { lm.removeUpdates(this) }
                        if (cont.isActive) cont.resume(location)
                    }
                }
                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
                try {
                    providers.forEach { lm.requestSingleUpdate(it, listener, Looper.getMainLooper()) }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    suspend fun fetchWeather(useDeviceLocation: Boolean, latitude: Float?, longitude: Float?): GlanceWeatherData? {
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
                    "?latitude=%.4f&longitude=%.4f".format(Locale.US, loc.latitude, loc.longitude) +
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
        val end = calendarQueryWindowEnd(now, lookAheadDays)
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

    private fun weatherCodeToCondition(code: Int): String = weatherCodeToCondition(context, code)
}

/** Great-circle distance in kilometres (haversine). */
internal fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    return 2 * earthRadiusKm * kotlin.math.asin(kotlin.math.sqrt(a))
}

/**
 * Whether an IP-derived fix is plausible. An IP fix far from a recent real device fix usually
 * means a VPN or mis-geolocated ISP range, not actual travel — prefer the device fix then.
 * With no recent device reference there is nothing to contradict, so trust the IP.
 */
internal fun shouldTrustIpLocation(
    ipLatitude: Double,
    ipLongitude: Double,
    lastDeviceLatitude: Double?,
    lastDeviceLongitude: Double?,
    lastDeviceAgeMs: Long?,
    maxDistanceKm: Double = 300.0,
    referenceMaxAgeMs: Long = 48L * 60 * 60 * 1000,
): Boolean {
    if (lastDeviceLatitude == null || lastDeviceLongitude == null || lastDeviceAgeMs == null) return true
    if (lastDeviceAgeMs > referenceMaxAgeMs) return true
    return haversineKm(ipLatitude, ipLongitude, lastDeviceLatitude, lastDeviceLongitude) <= maxDistanceKm
}

/**
 * Coordinates from an IP-geolocation response. Handles both GeoJS (string lat/long) and
 * ipwho.is (numeric lat/long, plus a `success` flag). Returns null on failure markers,
 * out-of-range values, or the (0,0) null island some services emit on lookup failure.
 */
internal fun parseIpLocation(json: String): Pair<Double, Double>? = runCatching {
    val o = JSONObject(json)
    if (o.has("success") && !o.optBoolean("success", true)) return@runCatching null
    val lat = o.optString("latitude").toDoubleOrNull()
    val lon = o.optString("longitude").toDoubleOrNull()
    when {
        lat == null || lon == null -> null
        lat == 0.0 && lon == 0.0 -> null
        kotlin.math.abs(lat) > 90.0 || kotlin.math.abs(lon) > 180.0 -> null
        else -> lat to lon
    }
}.getOrNull()

/**
 * End of the calendar query window: local midnight after [lookAheadDays] full days beyond today.
 * A rolling `now + N*24h` window would hide tomorrow-evening events whenever the current
 * time-of-day is earlier than the event's \u2014 e.g. at 19:42 a rolling 1-day window ends at
 * 19:42 tomorrow and misses a 23:00 meeting.
 */
internal fun calendarQueryWindowEnd(nowMs: Long, lookAheadDays: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = nowMs
        add(Calendar.DAY_OF_YEAR, lookAheadDays + 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** WMO weather code \u2192 localized condition label (shared by the glance strip fetchers). */
internal fun weatherCodeToCondition(context: Context, code: Int): String {
    val res = when (code) {
        0 -> R.string.weather_clear
        1 -> R.string.weather_mainly_clear
        2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_overcast
        45, 48 -> R.string.weather_fog
        51, 53, 55 -> R.string.weather_drizzle
        61, 63, 65 -> R.string.weather_rain
        71, 73, 75 -> R.string.weather_snow
        80, 81, 82 -> R.string.weather_showers
        95 -> R.string.weather_thunderstorm
        96, 99 -> R.string.weather_storm
        else -> return "\u2014"
    }
    return context.getString(res)
}
