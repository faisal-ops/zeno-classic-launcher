package com.zeno.classiclauncher.nlauncher.glance

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import com.zeno.classiclauncher.nlauncher.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import org.json.JSONObject

/**
 * Home strip modeled after Lawnchair [BcSmartspaceView]: date + weather on the right,
 * rotating “glance” carousel on the left (flashlight, battery, next calendar event, next alarm).
 *
 * Heavy IO (weather network, calendar query, etc.) runs while the view is attached (home strip
 * visible): on attach, on screen on (if last heavy IO was ≥25 min ago), when strip prefs change,
 * torch changes, and on a ~35 min cadence. A shared 25 min debounce spaces weather + carousel
 * rebuilds so rapid unlocks do not each trigger a full fetch. Carousel rotation timing is unchanged.
 */
class GlanceDateWeatherEventsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var attachedJob: Job? = null

    private lateinit var carouselText: TextView
    private lateinit var dotsText: TextView
    private lateinit var dateView: TextView
    private lateinit var weatherView: TextView

    private data class GlanceItem(
        val text: String,
        val action: (() -> Unit)? = null,
    )

    private data class CalendarEvent(
        val id: Long,
        val title: String,
        val startTime: Long,
    )

    private data class WeatherData(
        val tempC: Float,
        val condition: String,
        val tempMinC: Float,
        val tempMaxC: Float,
    )

    @Volatile
    private var stripPrefs: GlanceStripPreferences = GlanceStripPreferences()

    @Volatile
    private var glanceForceRebuild = true

    private var glanceJob: Job? = null
    private var glanceItems: List<GlanceItem> = emptyList()
    private var currentGlanceIndex = 0
    @Volatile
    private var torchActive = false
    private var glanceBuiltAt = 0L
    /**
     * Min time between heavy glance IO (calendar query, alarm read, battery snapshot, etc.) while
     * the strip is visible. Carousel still advances on its existing short delays; this only gates
     * [buildGlanceItems] on the IO path.
     */
    private val heavyGlanceIoIntervalMs = 35 * 60_000L

    private var cachedWeather: WeatherData? = null
    private var weatherFetchedAt = 0L
    /** When a fetch fails, show cached weather if younger than this. */
    private val weatherCacheMs = 2 * 60 * 60 * 1000L // 2h display staleness cap
    /** Network weather fetch interval while strip is attached and screen is on. */
    private val weatherNetworkIntervalMs = 35 * 60_000L

    /**
     * Minimum spacing between heavy IO (weather network + [buildGlanceItems]). Prevents every
     * screen-on / unlock from refetching when the user wakes the device often; still allows refresh
     * after this window or on first use ([lastHeavyIoAtMs] == 0).
     */
    private val heavyIoDebounceMs = 25 * 60_000L

    @Volatile
    private var lastHeavyIoAtMs = 0L

    private var cachedLocation: Location? = null
    private var locationFetchedAt = 0L
    private val locationCacheMs = 2 * 60 * 60 * 1000L // re-use location for 2h

    private var cachedCalendarEvent: CalendarEvent? = null
    private var calendarFetchedAt = 0L
    // Battery optimization: recompute calendar info only hourly.
    private val calendarCacheMs = 60 * 60 * 1000L

    private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val shadowColor = 0xCC000000.toInt()

    @Volatile private var screenOn = true
    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    attachedJob?.cancel()
                    attachedJob = null
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    val now = System.currentTimeMillis()
                    if (lastHeavyIoAtMs == 0L || now - lastHeavyIoAtMs >= heavyIoDebounceMs) {
                        glanceForceRebuild = true
                        glanceBuiltAt = 0L
                    }
                    if (attachedJob == null) restartLoops()
                }
            }
        }
    }
    private var screenReceiverRegistered = false

    private val torchHandler = Handler(Looper.getMainLooper())
    private var cameraManager: CameraManager? = null
    private var torchCallbackRegistered = false
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchActive = enabled
            glanceForceRebuild = true
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            torchActive = false
            glanceForceRebuild = true
        }
    }

    fun applyStripPreferences(p: GlanceStripPreferences) {
        stripPrefs = p
        glanceForceRebuild = true
        post {
            if (isAttachedToWindow) {
                glanceBuiltAt = 0L
            }
        }
        if (isAttachedToWindow) {
            if (stripPrefs.showFlashlight) {
                registerTorchCallback()
            } else {
                // When disabled, ensure we don't keep torch callbacks registered.
                torchActive = false
                unregisterTorchCallback()
            }
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.glance_date_weather_events_strip, this, true)
        carouselText = findViewById(R.id.glance_carousel_text)
        dotsText = findViewById(R.id.glance_dots_text)
        dateView = findViewById(R.id.glance_date)
        weatherView = findViewById(R.id.glance_weather)
        for (tv in listOf(carouselText, dotsText, dateView, weatherView)) {
            tv.setShadowLayer(3f, 1f, 1f, shadowColor)
        }
    }

    private fun restartLoops() {
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }
        attachedJob?.cancel()
        attachedJob = scope.launch {
            launch { runDateLoop() }
            launch { runWeatherLoop() }
            glanceJob?.cancel()
            glanceJob = launch { runGlanceCarousel() }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (stripPrefs.showFlashlight) {
            registerTorchCallback()
        } else {
            torchActive = false
            unregisterTorchCallback()
        }
        if (!screenReceiverRegistered) {
            context.registerReceiver(
                screenReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                },
            )
            screenReceiverRegistered = true
        }
        screenOn = true
        restartLoops()
    }

    override fun onDetachedFromWindow() {
        glanceJob?.cancel()
        glanceJob = null
        attachedJob?.cancel()
        attachedJob = null
        if (screenReceiverRegistered) {
            runCatching { context.unregisterReceiver(screenReceiver) }
            screenReceiverRegistered = false
        }
        unregisterTorchCallback()
        super.onDetachedFromWindow()
    }

    fun dispose() {
        unregisterTorchCallback()
        if (screenReceiverRegistered) {
            runCatching { context.unregisterReceiver(screenReceiver) }
            screenReceiverRegistered = false
        }
        glanceJob?.cancel()
        glanceJob = null
        attachedJob?.cancel()
        attachedJob = null
        scope.cancel()
    }

    private fun registerTorchCallback() {
        if (torchCallbackRegistered) return
        val cm = context.getSystemService<CameraManager>() ?: return
        cameraManager = cm
        runCatching {
            cm.registerTorchCallback(torchCallback, torchHandler)
            torchCallbackRegistered = true
        }
    }

    private fun unregisterTorchCallback() {
        if (!torchCallbackRegistered) return
        runCatching { cameraManager?.unregisterTorchCallback(torchCallback) }
        torchCallbackRegistered = false
        cameraManager = null
    }

    private suspend fun awaitHeavyIoDebounce() {
        val last = lastHeavyIoAtMs
        if (last <= 0L) return
        val elapsed = System.currentTimeMillis() - last
        if (elapsed < heavyIoDebounceMs) {
            delay(heavyIoDebounceMs - elapsed)
        }
    }

    private fun markHeavyIoDone() {
        lastHeavyIoAtMs = System.currentTimeMillis()
    }

    private suspend fun runDateLoop() {
        while (coroutineContext.isActive) {
            dateView.text = dateFormat.format(Date())
            val now = System.currentTimeMillis()
            val nextMinute = (now / 60_000 + 1) * 60_000
            delay(nextMinute - now)
        }
    }

    private suspend fun runWeatherLoop() {
        while (coroutineContext.isActive) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                weatherView.text = context.getString(R.string.glance_tap_weather)
                weatherView.isVisible = true
                weatherView.setOnClickListener { requestLocationPermission() }
                delay(5 * 60_000L) // re-check every 5min; no need to spin at 10s
                continue
            }
            weatherView.setOnClickListener(null)
            awaitHeavyIoDebounce()
            val weather = withContext(Dispatchers.IO) { fetchWeather() }
            markHeavyIoDone()
            if (weather != null) {
                cachedWeather = weather
                weatherFetchedAt = System.currentTimeMillis()
                val t = "%.0f\u00B0".format(weather.tempC)
                val lo = "%.0f".format(weather.tempMinC)
                val hi = "%.0f".format(weather.tempMaxC)
                val weatherLine = "$t ${weather.condition}  ($lo\u2013$hi\u00B0)"
                weatherView.text = weatherLine
                weatherView.isVisible = true
            } else {
                val cached = cachedWeather
                if (cached != null &&
                    System.currentTimeMillis() - weatherFetchedAt < weatherCacheMs
                ) {
                    val t = "%.0f\u00B0".format(cached.tempC)
                    weatherView.text = "$t ${cached.condition}"
                    weatherView.isVisible = true
                } else {
                    weatherView.isVisible = false
                }
            }
            // Heavy network: on a fixed cadence while visible, not tied to carousel ticks.
            delay(if (weather != null) weatherNetworkIntervalMs else 5 * 60_000L)
        }
    }

    private suspend fun runGlanceCarousel() {
        while (coroutineContext.isActive) {
            val now = System.currentTimeMillis()
            val needHeavyIo = glanceForceRebuild ||
                glanceBuiltAt == 0L ||
                now - glanceBuiltAt >= heavyGlanceIoIntervalMs
            if (needHeavyIo) {
                // Periodic rebuild: allow calendar provider query (see [getNextCalendarEvent] cache).
                if (!glanceForceRebuild && glanceBuiltAt > 0L) {
                    calendarFetchedAt = 0L
                }
                // Prefs / torch / screen-on (when allowed) skip wait so UI updates stay responsive.
                if (!glanceForceRebuild) {
                    awaitHeavyIoDebounce()
                }
                val newItems = withContext(Dispatchers.IO) { buildGlanceItems() }
                glanceForceRebuild = false
                glanceItems = newItems
                if (currentGlanceIndex >= glanceItems.size) currentGlanceIndex = 0
                glanceBuiltAt = System.currentTimeMillis()
                markHeavyIoDone()
            }

            val items = glanceItems
            val permStr = context.getString(R.string.glance_grant_calendar)
            val hasPermPrompt = items.any { it.text == permStr }
            val showFor = when {
                hasPermPrompt -> 5_000L
                items.size <= 1 -> 60_000L
                else -> 8_000L
            }

            showCurrentGlanceItem()
            delay(showFor)

            if (glanceItems.size > 1) {
                currentGlanceIndex = (currentGlanceIndex + 1) % glanceItems.size
            }
        }
    }

    private fun showCurrentGlanceItem() {
        val items = glanceItems
        val text = items.getOrNull(currentGlanceIndex)?.text
            ?: context.getString(R.string.glance_no_alerts)
        val isNoAlerts = items.isEmpty()

        if (carouselText.text == text) {
            updateDots(items)
            return
        }

        carouselText.animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    carouselText.text = text
                    val action = items.getOrNull(currentGlanceIndex)?.action
                    carouselText.setOnClickListener(
                        if (action != null) View.OnClickListener { action() } else null,
                    )
                    carouselText.animate()
                        .alpha(if (isNoAlerts) 0.5f else 1f)
                        .setDuration(200)
                        .setListener(null)
                        .start()
                }
            }).start()

        updateDots(items)
    }

    private fun updateDots(items: List<GlanceItem>) {
        if (items.size > 1) {
            dotsText.text = items.indices.joinToString(" ") { i ->
                if (i == currentGlanceIndex) "\u25CF" else "\u25CB"
            }
            dotsText.isVisible = true
        } else {
            dotsText.isVisible = false
        }
    }

    private fun buildGlanceItems(): List<GlanceItem> {
        val items = mutableListOf<GlanceItem>()
        val prefs = stripPrefs

        if (prefs.showFlashlight && torchActive) {
            items.add(GlanceItem(text = "\uD83D\uDD26 Flashlight on"))
        }

        if (prefs.showBattery) {
            getBatteryGlanceItem()?.let { items.add(it) }
        }

        if (prefs.showCalendar) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALENDAR,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                items.add(
                    GlanceItem(
                        text = context.getString(R.string.glance_grant_calendar),
                        action = { requestCalendarPermission() },
                    ),
                )
            } else {
                getNextCalendarEvent()?.let { event ->
                    val timeStr = formatEventTime(event.startTime)
                    items.add(
                        GlanceItem(
                            text = "\uD83D\uDCC5 ${event.title} · $timeStr",
                            action = {
                                try {
                                    val uri = ContentUris.withAppendedId(
                                        CalendarContract.Events.CONTENT_URI,
                                        event.id,
                                    )
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, uri)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                } catch (_: Exception) {
                                }
                            },
                        ),
                    )
                }
            }
        }

        if (prefs.showAlarm) {
            getNextAlarmInfo()?.let { info ->
                items.add(
                    GlanceItem(
                        text = "\u23F0 $info",
                        action = {
                            try {
                                context.startActivity(
                                    Intent(AlarmClock.ACTION_SHOW_ALARMS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            } catch (_: Exception) {
                            }
                        },
                    ),
                )
            }
        }

        return items
    }

    private fun getBatteryGlanceItem(): GlanceItem? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val level = (100f *
            intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) /
            intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)).toInt()
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val full = status == BatteryManager.BATTERY_STATUS_FULL || level == 100
        return when {
            full -> null
            charging -> {
                val remainMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    runCatching {
                        context.getSystemService(BatteryManager::class.java)
                            ?.computeChargeTimeRemaining() ?: -1L
                    }.getOrDefault(-1L)
                } else {
                    -1L
                }
                val suffix = if (remainMs > 0) {
                    val m = (remainMs / 60_000).toInt()
                    if (m >= 60) " · Full in ${m / 60}h ${m % 60}m" else " · Full in ${m}m"
                } else {
                    ""
                }
                GlanceItem(text = "\u26A1 Charging $level%$suffix")
            }
            level <= 15 -> GlanceItem(text = "\uD83D\uDD0B Battery low · $level%")
            else -> null
        }
    }

    private fun getNextCalendarEvent(): CalendarEvent? {
        val now = System.currentTimeMillis()
        if (cachedCalendarEvent != null && now - calendarFetchedAt < calendarCacheMs) {
            return cachedCalendarEvent
        }
        val end = now + 24L * 60 * 60 * 1000
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, end)
            it.build()
        }
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.ALL_DAY,
                ),
                "${CalendarContract.Instances.BEGIN} >= ? AND ${CalendarContract.Instances.ALL_DAY} = 0",
                arrayOf(now.toString()),
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val event = CalendarEvent(
                    id = cursor.getLong(0),
                    title = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: return null,
                    startTime = cursor.getLong(2),
                )
                cachedCalendarEvent = event
                calendarFetchedAt = now
                event
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatEventTime(startTime: Long): String {
        val diff = startTime - System.currentTimeMillis()
        return when {
            diff <= 0 -> context.getString(R.string.glance_event_now)
            diff < 60 * 60 * 1000 -> context.getString(
                R.string.glance_event_in_minutes,
                (diff / (60 * 1000)).toInt(),
            )
            else -> timeFormat.format(Date(startTime))
        }
    }

    private fun getNextAlarmInfo(): String? {
        val am = context.getSystemService<AlarmManager>() ?: return null
        val next = am.nextAlarmClock ?: return null
        val diff = next.triggerTime - System.currentTimeMillis()
        if (diff < 0 || diff > 12L * 60 * 60 * 1000) return null
        val h = diff / (60 * 60 * 1000)
        val m = (diff % (60 * 60 * 1000)) / (60 * 1000)
        return if (h > 0) "Alarm in ${h}h ${m}m" else "Alarm in ${m}m"
    }

    private fun fetchWeather(): WeatherData? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val loc = getLastLocation() ?: return null
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
            WeatherData(
                tempC = current.getDouble("temperature_2m").toFloat(),
                condition = weatherCodeToCondition(current.getInt("weathercode")),
                tempMinC = daily.getJSONArray("temperature_2m_min").getDouble(0).toFloat(),
                tempMaxC = daily.getJSONArray("temperature_2m_max").getDouble(0).toFloat(),
            )
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun getLastLocation(): Location? {
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

    private fun hostActivity(): Activity? =
        generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()

    private fun requestLocationPermission() {
        val a = hostActivity() ?: return
        ActivityCompat.requestPermissions(
            a,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQ_LOCATION,
        )
    }

    private fun requestCalendarPermission() {
        val a = hostActivity() ?: return
        ActivityCompat.requestPermissions(
            a,
            arrayOf(Manifest.permission.READ_CALENDAR),
            REQ_CALENDAR,
        )
    }

    companion object {
        const val REQ_LOCATION = 4301
        const val REQ_CALENDAR = 4302
    }
}
