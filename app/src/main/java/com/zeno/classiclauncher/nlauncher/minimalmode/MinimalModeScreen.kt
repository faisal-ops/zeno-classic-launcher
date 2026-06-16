package com.zeno.classiclauncher.nlauncher.minimalmode

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.app.Notification
import android.app.RemoteInput
import android.os.BatteryManager
import android.os.Bundle
import com.zeno.classiclauncher.nlauncher.badges.BadgeNotificationListener
import com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.zeno.classiclauncher.nlauncher.power.LockScreenAccessibilityService
import com.zeno.classiclauncher.nlauncher.usage.UsageStatsRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Battery0Bar
import androidx.compose.material.icons.outlined.Battery2Bar
import androidx.compose.material.icons.outlined.Battery4Bar
import androidx.compose.material.icons.outlined.Battery6Bar
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.SignalCellularAlt
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.zeno.classiclauncher.nlauncher.apps.SoundProfileMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions
import com.zeno.classiclauncher.nlauncher.prefs.GlanceWeatherUnit
import androidx.compose.foundation.layout.widthIn
import com.zeno.classiclauncher.nlauncher.ui.LauncherViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import com.zeno.classiclauncher.nlauncher.R
import java.util.Locale

// ─── Focus bar colors (matching prototype) ───────────────────────────────────
private val FOCUS_BG = Color.White
private val FOCUS_TEXT = Color.Black
private val FOCUS_ACCENT = Color(0xFF4DA3FF)
private val NORMAL_TEXT = Color.White
private val MUTED_TEXT = Color(0xFF8B8B8B)
private val DIVIDER_COLOR = Color(0xFF1A1A1A)
private val SCREEN_BG = Color.Black

private val LONG_PRESS_MS = 700L
private val FOOTER_CONTENT_HEIGHT = 80.dp

// Default apps shown when user hasn't configured Minimal Mode list yet.
// Ordered by typical BB-style priority. We'll match against installed apps.
private val DEFAULT_PACKAGES = listOf(
    "com.android.dialer",
    "com.google.android.dialer",
    "com.android.mms",
    "com.google.android.apps.messaging",
    "com.android.camera2",
    "com.google.android.camera",
    "com.android.music",
    "com.google.android.music",
    "com.android.chrome",
    "com.android.browser",
    "com.android.email",
    "com.google.android.gm",
    "com.android.settings",
    "com.android.documentsui",
    "com.google.android.maps",
)

@Composable
internal fun MinimalModeScreen(vm: LauncherViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val allApps by vm.apps.collectAsStateWithLifecycle()
    val forecast by vm.minimalModeForecast.collectAsStateWithLifecycle()
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
    val hasUnreadMail by vm.hasUnreadMail.collectAsStateWithLifecycle()
    val hasUnreadSms by vm.hasUnreadSms.collectAsStateWithLifecycle()
    val hasUnreadWhatsApp by vm.hasUnreadWhatsApp.collectAsStateWithLifecycle()
    val packagesWithUnread by vm.packagesWithUnread.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val actions = remember(context) { LauncherActions(context) }
    val is24h = remember { DateFormat.is24HourFormat(context) }
    val visibleApps = remember(prefs.minimalModeApps, allApps) {
        resolveMinimalModeApps(prefs.minimalModeApps, allApps).take(7)
    }

    var focusedIndex by rememberSaveable(visibleApps.joinToString(",") { it.packageName }) {
        mutableIntStateOf(0)
    }

    var clockTime by remember { mutableStateOf(currentTimeString(is24h)) }
    var clockAmPm by remember { mutableStateOf(if (is24h) "" else currentAmPmString()) }
    var clockDate by remember { mutableStateOf(currentDateString()) }
    var batteryPct by remember { mutableIntStateOf(readBatteryPct(context)) }
    var batteryCharging by remember { mutableStateOf(readBatteryCharging(context)) }
    var wifiOn by remember { mutableStateOf(actions.isWifiEnabled() == true) }
    var soundProfile by remember { mutableStateOf(actions.currentSoundProfile()) }
    var topBarBottomPx by remember { mutableIntStateOf(0) }

    val connectivityManager = remember { context.getSystemService(ConnectivityManager::class.java) }
    fun hasCellular(): Boolean = connectivityManager.allNetworks.any {
        connectivityManager.getNetworkCapabilities(it)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }
    var mobileConnected by remember { mutableStateOf(hasCellular()) }

    fun isHeadsetConnected(audioManager: AudioManager): Boolean {
        val wiredTypes = intArrayOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { it.type in wiredTypes }
    }
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    var headsetConnected by remember { mutableStateOf(isHeadsetConnected(audioManager)) }

    // Battery: update immediately from the sticky broadcast, then listen for changes.
    // BroadcastReceiver is far cheaper than polling — ACTION_BATTERY_CHANGED is a sticky
    // broadcast so registerReceiver() returns the current state instantly on registration.
    androidx.compose.runtime.DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                batteryPct = readBatteryPct(context)
                batteryCharging = readBatteryCharging(context)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Sound profile: react to ringer mode and DND filter changes instantly
    androidx.compose.runtime.DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                soundProfile = actions.currentSoundProfile()
            }
        }
        val filter = IntentFilter().apply {
            addAction(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // WiFi: react to enable/disable and connection changes instantly
    androidx.compose.runtime.DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                wifiOn = actions.isWifiEnabled() == true
            }
        }
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Mobile signal: poll every 30s (fast enough for airplane-mode or SIM changes).
    LaunchedEffect(Unit) {
        while (true) {
            mobileConnected = hasCellular()
            delay(30_000L)
        }
    }

    // Headset: react to wired headset/headphone plug and unplug events.
    androidx.compose.runtime.DisposableEffect(Unit) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                headsetConnected = isHeadsetConnected(audioManager)
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                headsetConnected = isHeadsetConnected(audioManager)
            }
        }
        audioManager.registerAudioDeviceCallback(callback, null)
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }

    // Clock: tick every second, synced to the wall-clock second boundary to avoid drift.
    LaunchedEffect(Unit) {
        while (true) {
            clockTime = currentTimeString(is24h)
            clockAmPm = if (is24h) "" else currentAmPmString()
            clockDate = currentDateString()
            val now = System.currentTimeMillis()
            delay(1_000L - (now % 1_000L))
        }
    }

    // Screen time: fetch once on entry, then refresh every 60 seconds.
    var totalScreenTimeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                totalScreenTimeMs = UsageStatsRepository.getTodayScreenOnTime(context)
            }
            delay(60_000L)
        }
    }

    // Per-app usage today: refresh every 60s alongside total screen time
    var appUsageMap by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                appUsageMap = UsageStatsRepository.getLast24hUsage(context)
            }
            delay(60_000L)
        }
    }

    // Parse per-app limits from prefs: "pkg:ms,pkg:ms" → Map
    val appLimitsMap = remember(prefs.minimalModeAppLimits) {
        prefs.minimalModeAppLimits.split(",").filter { it.contains(":") }
            .mapNotNull { entry ->
                val k = entry.substringBefore(":")
                val v = entry.substringAfter(":").toLongOrNull()
                if (v != null) k to v else null
            }.toMap()
    }

    // App challenge countdown state: non-null pkg = countdown in progress
    var challengeTargetPkg by remember { mutableStateOf<String?>(null) }

    val notifCount = packagesWithUnread.size +
        (if (hasUnreadMail) 1 else 0) +
        (if (hasUnreadSms) 1 else 0) +
        (if (hasUnreadWhatsApp) 1 else 0)
    val notifSummary = remember(packagesWithUnread, hasUnreadMail, hasUnreadSms, hasUnreadWhatsApp) {
        buildNotifSummary(packagesWithUnread, hasUnreadMail, hasUnreadSms, hasUnreadWhatsApp)
    }
    val hasAnyNotif = packagesWithUnread.isNotEmpty() || hasUnreadMail || hasUnreadSms || hasUnreadWhatsApp
    val unreadApps = remember(packagesWithUnread, allApps) {
        packagesWithUnread.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }.take(7)
    }

    var showSearchOverlay by rememberSaveable { mutableStateOf(false) }
    var showSwipeRightSelector by rememberSaveable { mutableStateOf(false) }
    var enterPressedAt by remember { mutableLongStateOf(0L) }
    var showAppsEditor by rememberSaveable { mutableStateOf(false) }
    var showMinimalModeSettings by rememberSaveable { mutableStateOf(false) }
    var replacingIndex by rememberSaveable { mutableIntStateOf(-1) }
    var showQuickSettings by rememberSaveable { mutableStateOf(false) }
    var quickReplyPackage by rememberSaveable { mutableStateOf<String?>(null) }
    // (index, app) of the app row that was long-pressed — drives context menu
    var contextMenuEntry by remember { mutableStateOf<Pair<Int, AppEntry>?>(null) }
    // Package blocked by a daily limit — shows the limit-reached overlay
    var limitBlockedPkg by remember { mutableStateOf<String?>(null) }
    val limitExtensions by vm.limitExtensions.collectAsStateWithLifecycle()

    // Consume back press silently — minimal mode is a home screen, back should do nothing
    BackHandler {}

    // 0 = weather, 1 = player
    var footerPage by rememberSaveable { mutableIntStateOf(0) }
    // Auto-jump to player when music starts; jump back when it stops
    LaunchedEffect(nowPlaying != null) {
        if (nowPlaying != null) footerPage = 1 else footerPage = 0
    }

    val screenFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { screenFocus.requestFocus() } }
    // Central launch helper — enforces daily limit, then challenge, then direct launch
    fun launchOrChallenge(pkg: String) {
        val limitMs = appLimitsMap[pkg]
        if (limitMs != null && (appUsageMap[pkg] ?: 0L) >= limitMs && !vm.isLimitExtended(pkg)) {
            limitBlockedPkg = pkg
            return
        }
        if (pkg in prefs.minimalModeChallengeApps) {
            challengeTargetPkg = pkg
        } else {
            vm.launchApp(pkg)
        }
    }

    val anyOverlayOpen = showAppsEditor || showMinimalModeSettings || replacingIndex >= 0 || showSearchOverlay || showQuickSettings || quickReplyPackage != null || challengeTargetPkg != null || contextMenuEntry != null || limitBlockedPkg != null || showSwipeRightSelector
    LaunchedEffect(anyOverlayOpen) {
        if (!anyOverlayOpen) runCatching { screenFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SCREEN_BG)
            .then(
                if (prefs.minimalModeGreyscale)
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(color = Color.Black, blendMode = androidx.compose.ui.graphics.BlendMode.Saturation)
                    }
                else Modifier
            )
            .focusRequester(screenFocus)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!LockScreenAccessibilityService.tryLockScreen()) {
                            LockScreenAccessibilityService.sendLockRequestBroadcast(context)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                var dx = 0f; var dy = 0f
                detectDragGestures(
                    onDragEnd = { dx = 0f; dy = 0f },
                    onDragCancel = { dx = 0f; dy = 0f },
                    onDrag = { _, dragAmount ->
                        dx += dragAmount.x
                        dy += dragAmount.y
                        // Swipe down → QS (only when vertical dominates)
                        if (dy > 60f && dy > kotlin.math.abs(dx) * 1.5f) {
                            showQuickSettings = true
                            dx = 0f; dy = 0f
                        }
                        // Swipe right → BB Hub / configured app (only when horizontal dominates)
                        if (dx > 80f && dx > kotlin.math.abs(dy) * 1.5f) {
                            val savedPkg = prefs.minimalModeSwipeRightApp
                            val target = resolveSwipeRightApp(context, savedPkg)
                            if (target != null) vm.launchApp(target)
                            else showSwipeRightSelector = true
                            dx = 0f; dy = 0f
                        }
                    },
                )
            }
            .onPreviewKeyEvent { event ->
                if (anyOverlayOpen) return@onPreviewKeyEvent false
                val appCount = visibleApps.size
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                        if (focusedIndex + 1 < appCount) focusedIndex++
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                        if (focusedIndex - 1 >= 0) focusedIndex--
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.Enter -> {
                        if (enterPressedAt == 0L) enterPressedAt = System.currentTimeMillis()
                        true
                    }
                    event.type == KeyEventType.KeyUp && event.key == Key.Enter -> {
                        val held = System.currentTimeMillis() - enterPressedAt
                        enterPressedAt = 0L
                        if (held >= LONG_PRESS_MS) {
                            visibleApps.getOrNull(focusedIndex)?.let { app ->
                                contextMenuEntry = Pair(focusedIndex, app)
                            }
                        } else {
                            visibleApps.getOrNull(focusedIndex)?.let { app ->
                                launchOrChallenge(app.packageName)
                            }
                        }
                        true
                    }
                    else -> false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp)
                .padding(bottom = 10.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            MinimalModeHeader(
                time = clockTime,
                amPm = clockAmPm,
                date = clockDate,
                unreadApps = if (prefs.minimalModeShowNotifSummary) unreadApps else emptyList(),
                hasAnyNotif = hasAnyNotif,
                conditionEmoji = forecast.firstOrNull()?.conditionEmoji,
                currentTemp = forecast.firstOrNull()?.let {
                    formatTemp(it.tempMaxC, prefs.glanceWeatherUnit == GlanceWeatherUnit.CELSIUS)
                },
                batteryPct = if (batteryPct >= 0) batteryPct else null,
                batteryCharging = batteryCharging,
                greyscale = prefs.minimalModeGreyscale,
                screenTimeMs = if (totalScreenTimeMs > 0L) totalScreenTimeMs else null,
                wifiEnabled = wifiOn,
                soundProfile = soundProfile,
                headsetConnected = headsetConnected,
                mobileConnected = mobileConnected,
                onClockTap = {
                    resolveClockPackage(context)?.let { vm.launchApp(it) }
                },
                onWeatherTap = {
                    resolveWeatherPackage(context)?.let { vm.launchApp(it) }
                },
                onWifiTap = { actions.openInternetPanel(); wifiOn = actions.isWifiEnabled() == true },
                onUnreadAppTap = { pkg ->
                    val hasReply = buildQuickReplyItems(pkg).any { it.action != null }
                    if (hasReply) {
                        quickReplyPackage = pkg
                    } else {
                        BadgeNotificationListener.cancelForPackage(pkg)
                        vm.launchApp(pkg)
                    }
                },
                onTopBarPositioned = { topBarBottomPx = it },
            )

            Spacer(Modifier.height(8.dp))

            // ── App list ──────────────────────────────────────────────────────
            MinimalModeListView(
                modifier = Modifier.weight(1f),
                apps = visibleApps,
                focusedIndex = if (showQuickSettings) -1 else focusedIndex,
                appUsageMap = appUsageMap,
                appLimitsMap = appLimitsMap,
                onTap = { idx ->
                    focusedIndex = idx
                    visibleApps.getOrNull(idx)?.let { launchOrChallenge(it.packageName) }
                },
                onLongPress = { idx ->
                    visibleApps.getOrNull(idx)?.let { app -> contextMenuEntry = Pair(idx, app) }
                },
            )

            // ── Footer: swipeable weather ↔ player ───────────────────────────
            // hasWeatherPref = weather pref is on (footer space always reserved to avoid layout shift)
            // hasWeather     = data actually arrived
            val hasWeatherPref = prefs.minimalModeShowWeather
            val hasWeather = hasWeatherPref && forecast.isNotEmpty()
            val hasPlayer = nowPlaying != null
            if (hasWeatherPref || hasPlayer) {
                var dragAccum by remember { mutableStateOf(0f) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(hasWeather, hasPlayer) {
                            detectHorizontalDragGestures(
                                onDragEnd = { dragAccum = 0f },
                                onDragCancel = { dragAccum = 0f },
                                onHorizontalDrag = { _, delta ->
                                    dragAccum += delta
                                    if (dragAccum < -40f && hasPlayer) {
                                        footerPage = 1; dragAccum = 0f
                                    } else if (dragAccum > 40f && hasWeather) {
                                        footerPage = 0; dragAccum = 0f
                                    }
                                },
                            )
                        },
                ) {
                    when {
                        footerPage == 1 && hasPlayer -> MinimalModeNowPlayingBar(
                            state = nowPlaying!!,
                            onPrevious = { vm.mediaPreviousTrack() },
                            onPlayPause = { vm.mediaPlayPause() },
                            onNext = { vm.mediaSkipNext() },
                        )
                        hasWeather -> MinimalModeWeatherRow(
                            forecast = forecast,
                            useCelsius = prefs.glanceWeatherUnit == GlanceWeatherUnit.CELSIUS,
                        )
                        hasPlayer -> MinimalModeNowPlayingBar(
                            state = nowPlaying!!,
                            onPrevious = { vm.mediaPreviousTrack() },
                            onPlayPause = { vm.mediaPlayPause() },
                            onNext = { vm.mediaSkipNext() },
                        )
                        hasWeatherPref -> MinimalModeWeatherPlaceholder()
                    }
                }
            }
        }

        if (showAppsEditor) {
            MinimalModeAppsEditor(
                allApps = allApps,
                selectedPackages = prefs.minimalModeApps,
                onSave = { vm.setMinimalModeApps(it) },
                onDismiss = { showAppsEditor = false },
            )
        }

        if (showMinimalModeSettings) {
            MinimalModeSettingsOverlay(
                vm = vm,
                onDismiss = { showMinimalModeSettings = false },
            )
        }

        if (replacingIndex >= 0) {
            MinimalModeAppSelector(
                allApps = allApps,
                onSelect = { selectedPkg ->
                    val current = prefs.minimalModeApps.ifEmpty {
                        resolveMinimalModeApps(emptyList(), allApps).take(9).map { it.packageName }
                    }.toMutableList()
                    if (replacingIndex < current.size) {
                        current[replacingIndex] = selectedPkg
                    } else {
                        current.add(selectedPkg)
                    }
                    vm.setMinimalModeApps(current)
                    replacingIndex = -1
                },
                onDismiss = { replacingIndex = -1 },
            )
        }

        if (showSwipeRightSelector) {
            MinimalModeAppSelector(
                allApps = allApps,
                onSelect = { selectedPkg ->
                    vm.setMinimalModeSwipeRightApp(selectedPkg)
                    showSwipeRightSelector = false
                    vm.launchApp(selectedPkg)
                },
                onDismiss = { showSwipeRightSelector = false },
            )
        }

        contextMenuEntry?.let { (idx, app) ->
            AppRowContextMenu(
                app = app,
                isChallengeApp = app.packageName in prefs.minimalModeChallengeApps,
                limitMs = appLimitsMap[app.packageName],
                onReplace = { contextMenuEntry = null; replacingIndex = idx },
                onToggleChallenge = { add -> vm.toggleMinimalModeChallengeApp(app.packageName, add) },
                onSetLimit = { ms -> vm.setMinimalModeAppLimit(app.packageName, ms) },
                onDismiss = { contextMenuEntry = null },
            )
        }

        // Search overlay — type any letter or tap the search icon to open
        if (showSearchOverlay) {
            MinimalModeSearchOverlay(
                allApps = allApps,
                onLaunch = { pkg -> vm.launchApp(pkg); showSearchOverlay = false },
                onDismiss = { showSearchOverlay = false },
            )
        }
        if (showQuickSettings) {
            MinimalModeQsOverlay(
                topBarBottomPx = topBarBottomPx,
                qrScannerPackage = prefs.quickSettingsQrScannerPackage,
                allApps = allApps,
                onSetQrScannerPackage = vm::setQuickSettingsQrScannerPackage,
                onOpenSettings = { showQuickSettings = false; showMinimalModeSettings = true },
                onDismiss = { showQuickSettings = false },
            )
        }
        quickReplyPackage?.let { pkg ->
            val qrApp = allApps.find { it.packageName == pkg }
            if (qrApp != null) {
                QuickReplyOverlay(
                    app = qrApp,
                    context = context,
                    packagesWithUnread = packagesWithUnread,
                    onDismiss = {
                        BadgeNotificationListener.cancelForPackage(pkg)
                        quickReplyPackage = null
                    },
                )
            }
        }
        limitBlockedPkg?.let { pkg ->
            val app = allApps.find { it.packageName == pkg }
            if (app != null) {
                AppLimitReachedOverlay(
                    app = app,
                    usageMs = appUsageMap[pkg] ?: 0L,
                    limitMs = appLimitsMap[pkg] ?: 0L,
                    onExtend = {
                        vm.extendAppLimit(pkg)
                        limitBlockedPkg = null
                        launchOrChallenge(pkg)
                    },
                    onDismiss = { limitBlockedPkg = null },
                )
            }
        }

        challengeTargetPkg?.let { pkg ->
            val appLabel = allApps.find { it.packageName == pkg }?.label ?: pkg
            AppChallengeOverlay(
                appLabel = appLabel,
                onLaunch = { vm.launchApp(pkg); challengeTargetPkg = null },
                onCancel = { challengeTargetPkg = null },
            )
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
internal fun MinimalModeTopBar(
    time: String,
    amPm: String,
    batteryPct: Int?,
    batteryCharging: Boolean,
    greyscale: Boolean,
    screenTimeMs: Long?,
    wifiEnabled: Boolean,
    soundProfile: SoundProfileMode,
    headsetConnected: Boolean,
    mobileConnected: Boolean,
    onClockTap: () -> Unit,
    onWifiTap: () -> Unit,
) {
    // 3-column Row: weight(1f) on both sides guarantees clock is truly centered.
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left side — battery + screen time
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (batteryPct != null) {
                BatteryPill(pct = batteryPct, charging = batteryCharging, greyscale = greyscale)
            }
            if (screenTimeMs != null) {
                Text(
                    text = "  ·  ${UsageStatsRepository.formatUsageShort(screenTimeMs)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9AA0A8),
                )
            }
        }
        // Center — time + AM/PM inline
        val clockCd = stringResource(R.string.cd_simple_clock, time)
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = clockCd
                    onClick(label = "Open clock") { onClockTap(); true }
                }
                .pointerInput(Unit) { detectTapGestures(onTap = { onClockTap() }) },
        ) {
            Text(
                text = time,
                fontSize = 24.sp,
                fontWeight = FontWeight.W400,
                color = NORMAL_TEXT,
                letterSpacing = (-0.5).sp,
            )
            if (amPm.isNotEmpty()) {
                Text(
                    text = amPm,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W300,
                    color = MUTED_TEXT,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                )
            }
        }
        // Right side — DND/Vibrate indicator + wifi icon
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (soundProfile == SoundProfileMode.DND) {
                Icon(
                    imageVector = Icons.Rounded.DoNotDisturb,
                    contentDescription = stringResource(R.string.quick_settings_dnd),
                    tint = Color(0xFFEAF0F6),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
            } else if (soundProfile == SoundProfileMode.VIBRATE) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = stringResource(R.string.sound_profile_vibrate),
                    tint = Color(0xFFEAF0F6),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            if (headsetConnected) {
                Icon(
                    imageVector = Icons.Rounded.Headset,
                    contentDescription = "Headset connected",
                    tint = Color(0xFFEAF0F6),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            if (mobileConnected) {
                Icon(
                    imageVector = Icons.Rounded.SignalCellularAlt,
                    contentDescription = "Mobile network",
                    tint = Color(0xFFEAF0F6),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = if (wifiEnabled) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                contentDescription = if (wifiEnabled) "WiFi on" else "WiFi off",
                tint = Color(0xFFEAF0F6),
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onWifiTap() }) },
            )
        }
    }
}

@Composable
private fun MinimalModeHeader(
    time: String,
    amPm: String,
    date: String,
    unreadApps: List<AppEntry>,
    hasAnyNotif: Boolean,
    onUnreadAppTap: (String) -> Unit,
    conditionEmoji: String?,
    currentTemp: String?,
    batteryPct: Int?,
    batteryCharging: Boolean,
    greyscale: Boolean,
    screenTimeMs: Long?,
    wifiEnabled: Boolean,
    soundProfile: SoundProfileMode,
    headsetConnected: Boolean,
    mobileConnected: Boolean,
    onClockTap: () -> Unit,
    onWeatherTap: () -> Unit,
    onWifiTap: () -> Unit,
    onTopBarPositioned: (Int) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Box(modifier = Modifier.onGloballyPositioned { coords ->
            onTopBarPositioned((coords.positionInRoot().y + coords.size.height).toInt())
        }) {
        MinimalModeTopBar(
            time = time,
            amPm = amPm,
            batteryPct = batteryPct,
            batteryCharging = batteryCharging,
            greyscale = greyscale,
            screenTimeMs = screenTimeMs,
            wifiEnabled = wifiEnabled,
            soundProfile = soundProfile,
            headsetConnected = headsetConnected,
            mobileConnected = mobileConnected,
            onClockTap = onClockTap,
            onWifiTap = onWifiTap,
        )
        } // end measurement Box

        // ── Date/weather · notifications — centered ───────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = date, fontSize = 13.sp, color = MUTED_TEXT)
                if (currentTemp != null) {
                    val tempLabel = if (conditionEmoji != null) "$conditionEmoji $currentTemp" else currentTemp
                    Text(
                        text = "  ·  $tempLabel",
                        fontSize = 13.sp,
                        color = MUTED_TEXT,
                        modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onWeatherTap() }) },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            if (unreadApps.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    unreadApps.forEach { app ->
                        val bmp = remember(app.packageName) { app.icon?.toBitmap(64, 64) }
                        if (bmp != null) {
                            androidx.compose.foundation.Image(
                                painter = BitmapPainter(bmp.asImageBitmap()),
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .pointerInput(app.packageName) {
                                        detectTapGestures(onTap = { onUnreadAppTap(app.packageName) })
                                    },
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.minimal_mode_no_notifications),
                    fontSize = 12.sp,
                    color = MUTED_TEXT,
                )
            }
        }
    }
}

// ─── List view ────────────────────────────────────────────────────────────────

@Composable
private fun MinimalModeListView(
    modifier: Modifier,
    apps: List<AppEntry>,
    focusedIndex: Int,
    appUsageMap: Map<String, Long>,
    appLimitsMap: Map<String, Long>,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(7) { index ->
            val app = apps.getOrNull(index)
            if (app != null) {
                val usageMs = appUsageMap[app.packageName] ?: 0L
                val limitMs = appLimitsMap[app.packageName]
                MinimalModeListRow(
                    app = app,
                    selected = index == focusedIndex,
                    distanceFromFocus = kotlin.math.abs(index - focusedIndex),
                    usageMs = usageMs,
                    limitMs = limitMs,
                    onClick = { onTap(index) },
                    onLongPress = { onLongPress(index) },
                )
            } else {
                val emptySlotCd = stringResource(R.string.cd_simple_empty_slot)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = emptySlotCd
                            onClick(label = "Add app") { onLongPress(index); true }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onLongPress(index) })
                        }
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(R.string.minimal_mode_add_app_slot),
                        fontSize = 18.sp,
                        color = Color(0xFF2C3540),
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalModeListRow(
    app: AppEntry,
    selected: Boolean,
    distanceFromFocus: Int,
    usageMs: Long,
    limitMs: Long?,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val overLimit = limitMs != null && usageMs >= limitMs
    val density = LocalDensity.current
    val tx = remember { Animatable(with(density) { if (selected) 16.dp.toPx() else 8.dp.toPx() }) }
    LaunchedEffect(selected) {
        tx.animateTo(
            targetValue = with(density) { if (selected) 16.dp.toPx() else 8.dp.toPx() },
            animationSpec = tween(durationMillis = 200),
        )
    }
    val targetAlpha = when (distanceFromFocus) {
        0 -> 1f
        1 -> 0.55f
        2 -> 0.30f
        else -> 0.18f
    }
    val rowAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 180),
        label = "rowAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer { alpha = rowAlpha }
            .background(if (selected) FOCUS_BG else Color.Transparent)
            .drawBehind {
                if (selected) drawRect(color = FOCUS_ACCENT, size = Size(4.dp.toPx(), size.height))
            }
            .semantics {
                role = Role.Button
                contentDescription = app.label
                onClick(label = "Open ${app.label}") { onClick(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (selected) {
            Text(
                text = "▌",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = tx.value }
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val nameColor = when {
                selected -> FOCUS_TEXT
                overLimit -> Color(0xFFFFB340)
                else -> NORMAL_TEXT
            }
            Text(
                text = app.label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (usageMs >= 60_000L) {
                Text(
                    text = UsageStatsRepository.formatUsageShort(usageMs),
                    fontSize = 11.sp,
                    color = if (overLimit) Color(0xFFFFB340) else Color(0xFF555555),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}


// ─── Weather row ──────────────────────────────────────────────────────────────

@Composable
private fun MinimalModeWeatherPlaceholder() {
    Column(modifier = Modifier.fillMaxWidth().height(FOOTER_CONTENT_HEIGHT)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DIVIDER_COLOR))
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "—", fontSize = 13.sp, color = Color(0xFF333333))
        }
    }
}

@Composable
internal fun MinimalModeWeatherRow(
    forecast: List<MinimalModeWeatherDay>,
    useCelsius: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth().height(FOOTER_CONTENT_HEIGHT)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DIVIDER_COLOR))
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            forecast.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = day.label, fontSize = 11.sp, color = MUTED_TEXT, lineHeight = 13.sp)
                    Text(text = day.conditionEmoji, fontSize = 20.sp, lineHeight = 24.sp)
                    Text(
                        text = "${formatTemp(day.tempMaxC, useCelsius)}/${formatTemp(day.tempMinC, useCelsius)}",
                        fontSize = 11.sp,
                        color = NORMAL_TEXT,
                        lineHeight = 13.sp,
                    )
                }
            }
        }
    }
}

// ─── Now-playing bar ─────────────────────────────────────────────────────────

@Composable
private fun MinimalModeNowPlayingBar(
    state: NowPlayingState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().height(FOOTER_CONTENT_HEIGHT)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DIVIDER_COLOR))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.albumArt != null) {
                androidx.compose.foundation.Image(
                    painter = BitmapPainter(state.albumArt.asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NORMAL_TEXT,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.artist.isNotEmpty()) {
                    Text(
                        text = state.artist,
                        fontSize = 13.sp,
                        color = MUTED_TEXT,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.action_previous),
                tint = NORMAL_TEXT,
                modifier = Modifier.size(40.dp).clickable { onPrevious() },
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (state.isPlaying) R.string.action_pause else R.string.action_play),
                tint = NORMAL_TEXT,
                modifier = Modifier.size(46.dp).clickable { onPlayPause() },
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.action_next),
                tint = NORMAL_TEXT,
                modifier = Modifier.size(40.dp).clickable { onNext() },
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun readBatteryPct(context: android.content.Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: return -1
    return if (scale > 0) level * 100 / scale else -1
}

private fun readBatteryCharging(context: android.content.Context): Boolean {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return false
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
}

// ─── App row context menu (long-press) ───────────────────────────────────────

@Composable
private fun AppRowContextMenu(
    app: AppEntry,
    isChallengeApp: Boolean,
    limitMs: Long?,
    onReplace: () -> Unit,
    onToggleChallenge: (Boolean) -> Unit,
    onSetLimit: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val limitOptions = listOf<Long?>(null, 30 * 60_000L, 60 * 60_000L, 2 * 60 * 60_000L)
    val limitLabels = listOf("Off", "30m", "1h", "2h")
    // 0 = Replace, 1 = Challenge, 2 = Limit row
    var menuFocusIdx by remember { mutableIntStateOf(0) }
    var limitFocusIdx by remember { mutableIntStateOf(limitOptions.indexOf(limitMs).coerceAtLeast(0)) }

    val menuFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { menuFocusRequester.requestFocus() } }

    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .focusRequester(menuFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                        if (menuFocusIdx < 2) menuFocusIdx++
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                        if (menuFocusIdx > 0) menuFocusIdx--
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> {
                        if (menuFocusIdx == 2 && limitFocusIdx > 0) limitFocusIdx--
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight -> {
                        if (menuFocusIdx == 2 && limitFocusIdx < limitOptions.lastIndex) limitFocusIdx++
                        true
                    }
                    event.type == KeyEventType.KeyUp && event.key == Key.Enter -> {
                        when (menuFocusIdx) {
                            0 -> onReplace()
                            1 -> onToggleChallenge(!isChallengeApp)
                            2 -> onSetLimit(limitOptions[limitFocusIdx])
                        }
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFF111111))
                .navigationBarsPadding()
                .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        ) {
            // App name header
            Text(
                text = app.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF9AA0A8),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))

            // Replace slot
            ContextMenuRow(
                label = "Replace app",
                value = null,
                keyboardFocused = menuFocusIdx == 0,
                onClick = { onReplace() },
            )
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))

            // Challenge toggle
            ContextMenuRow(
                label = "5s challenge",
                value = if (isChallengeApp) "On" else "Off",
                valueColor = if (isChallengeApp) Color(0xFF4DA3FF) else Color(0xFF555555),
                keyboardFocused = menuFocusIdx == 1,
                onClick = { onToggleChallenge(!isChallengeApp) },
            )
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))

            // Daily limit picker
            val limitHeaderColor = if (menuFocusIdx == 2) NORMAL_TEXT else Color(0xFF9AA0A8)
            Text(
                text = "Daily limit",
                fontSize = 15.sp,
                color = limitHeaderColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                limitOptions.forEachIndexed { i, ms ->
                    val active = limitMs == ms
                    val kbFocused = menuFocusIdx == 2 && limitFocusIdx == i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) Color(0xFF4DA3FF) else Color(0xFF1C1C1E))
                            .then(
                                if (kbFocused && !active)
                                    Modifier.drawBehind {
                                        drawRoundRect(
                                            color = Color(0xFF4DA3FF),
                                            cornerRadius = CornerRadius(8.dp.toPx()),
                                            style = Stroke(width = 1.5.dp.toPx()),
                                        )
                                    }
                                else Modifier
                            )
                            .pointerInput(ms) { detectTapGestures(onTap = { onSetLimit(ms) }) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = limitLabels[i],
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (active) Color.White else if (kbFocused) Color(0xFF4DA3FF) else Color(0xFF9AA0A8),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    label: String,
    value: String?,
    valueColor: Color = MUTED_TEXT,
    keyboardFocused: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (keyboardFocused) Color(0xFF1A2030) else Color.Transparent)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (keyboardFocused) Color(0xFF4DA3FF) else NORMAL_TEXT,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(text = value, fontSize = 14.sp, color = valueColor)
        }
    }
}

@Composable
private fun BatteryPill(pct: Int, charging: Boolean, greyscale: Boolean = false) {
    val fillColor   = if (greyscale) Color(0xFF888888) else Color(0xFFE8EDF2)
    val outlineColor = if (greyscale) Color(0xFF888888) else Color(0xFF9EA8B3)
    val textColor   = if (greyscale) Color(0xFF888888) else Color(0xFFE8EDF2)

    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Battery $pct percent${if (charging) ", charging" else ""}"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(modifier = Modifier.width(38.dp).height(17.dp)) {
            val nubW  = 4.dp.toPx()
            val nubH  = size.height * 0.45f
            val bodyW = size.width - nubW
            val bodyH = size.height
            val sw    = 2.dp.toPx()
            val r     = CornerRadius(3.dp.toPx())

            drawRoundRect(
                color = outlineColor,
                size = Size(bodyW, bodyH),
                cornerRadius = r,
                style = Stroke(width = sw),
            )

            val maxFillW = bodyW - sw * 2
            val fillW = (maxFillW * (pct / 100f)).coerceAtLeast(0f)
            if (fillW > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(sw, sw),
                    size = Size(fillW, bodyH - sw * 2),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }

            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(bodyW, (bodyH - nubH) / 2f),
                size = Size(nubW, nubH),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
        }

        Text(
            text = if (charging) "$pct⚡" else "$pct%",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            lineHeight = 14.sp,
        )
    }
}

private val timeFormatter12 = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
private val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
private val amPmFormatter = DateTimeFormatter.ofPattern("a", Locale.getDefault())

private fun currentTimeString(is24h: Boolean = false): String =
    LocalDateTime.now().format(if (is24h) timeFormatter24 else timeFormatter12)
private fun currentDateString(): String = LocalDateTime.now().format(dateFormatter)
private fun currentAmPmString(): String = LocalDateTime.now().format(amPmFormatter)

private fun batteryIconVector(pct: Int) = when {
    pct >= 90 -> Icons.Outlined.BatteryFull
    pct >= 65 -> Icons.Outlined.Battery6Bar
    pct >= 45 -> Icons.Outlined.Battery4Bar
    pct >= 20 -> Icons.Outlined.Battery2Bar
    else      -> Icons.Outlined.Battery0Bar
}

internal fun resolveMinimalModeApps(
    pinned: List<String>,
    allApps: List<AppEntry>,
): List<AppEntry> {
    val appMap = allApps.associateBy { it.packageName }
    return if (pinned.isEmpty()) {
        DEFAULT_PACKAGES.mapNotNull { appMap[it] }
    } else {
        pinned.mapNotNull { appMap[it] }
    }
}

private val PHONE_PACKAGES = setOf(
    "com.android.dialer", "com.google.android.dialer",
    "com.samsung.android.dialer", "com.blackberry.phone",
    "com.motorola.incallui",
)

internal fun buildNotifSummary(
    packagesWithUnread: Set<String>,
    hasUnreadMail: Boolean,
    hasUnreadSms: Boolean,
    hasUnreadWhatsApp: Boolean,
): String {
    val hasPhone = packagesWithUnread.any { it in PHONE_PACKAGES }
    return when {
        hasPhone -> "Missed Calls"
        hasUnreadSms -> "New Messages"
        hasUnreadWhatsApp -> "WhatsApp Messages"
        hasUnreadMail -> "New Emails"
        packagesWithUnread.isNotEmpty() -> "New Notifications"
        else -> "No New Notifications"
    }
}

// ─── Search overlay ───────────────────────────────────────────────────────────

@Composable
private fun MinimalModeSearchOverlay(
    allApps: List<AppEntry>,
    onLaunch: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val filtered = remember(query.text, allApps) {
        if (query.text.isBlank()) allApps.sortedBy { it.label }
        else allApps.filter { it.label.contains(query.text, ignoreCase = true) }.sortedBy { it.label }
    }
    val fieldFocus = remember { FocusRequester() }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0000000))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .pointerInput(Unit) { /* consume touches so column doesn't dismiss */ },
        ) {
            Spacer(Modifier.height(12.dp))

            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MUTED_TEXT,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = NORMAL_TEXT, fontSize = 16.sp),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(fieldFocus),
                    decorationBox = { inner ->
                        if (query.text.isEmpty()) {
                            Text(stringResource(R.string.search_apps_ellipsis), fontSize = 16.sp, color = MUTED_TEXT)
                        }
                        inner()
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(app.packageName) {
                                detectTapGestures(onTap = { onLaunch(app.packageName) })
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val bmp = remember(app.packageName) { app.icon?.toBitmap(64, 64) }
                        if (bmp != null) {
                            androidx.compose.foundation.Image(
                                painter = BitmapPainter(bmp.asImageBitmap()),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            text = app.label,
                            fontSize = 18.sp,
                            color = NORMAL_TEXT,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DIVIDER_COLOR))
                }
            }
        }

        LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }
    }
}

// ─── Limit reached overlay ────────────────────────────────────────────────────

@Composable
private fun AppLimitReachedOverlay(
    app: AppEntry,
    usageMs: Long,
    limitMs: Long,
    onExtend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var authError by remember { mutableStateOf<String?>(null) }
    var authenticating by remember { mutableStateOf(false) }

    fun requestAuth() {
        val activity = context as? androidx.fragment.app.FragmentActivity ?: return
        authError = null
        authenticating = true
        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        val prompt = androidx.biometric.BiometricPrompt(
            activity, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    authenticating = false
                    onExtend()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authenticating = false
                    if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) authError = errString.toString()
                }
                override fun onAuthenticationFailed() {
                    authenticating = false
                    authError = "Authentication failed"
                }
            },
        )
        val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Extend limit for ${app.label}")
            .setSubtitle("Authenticate to open for 30 more minutes")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
        prompt.authenticate(info)
    }

    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0000000))
            .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = app.label,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = "Daily limit reached",
                fontSize = 14.sp,
                color = Color(0xFFFFB340),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${UsageStatsRepository.formatUsage(usageMs)} used · limit ${UsageStatsRepository.formatUsage(limitMs)}",
                fontSize = 12.sp,
                color = Color(0xFF8B8B8B),
            )
            Spacer(Modifier.height(8.dp))
            // Extend button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (authenticating) Color(0xFF1C1C1E) else Color(0xFF4DA3FF))
                    .pointerInput(authenticating) {
                        detectTapGestures(onTap = { if (!authenticating) requestAuth() })
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (authenticating) "Authenticating…" else "Open for 30 more minutes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }
            if (authError != null) {
                Text(
                    text = authError!!,
                    fontSize = 12.sp,
                    color = Color(0xFFFF453A),
                )
            }
            // Cancel
            Text(
                text = "Cancel",
                fontSize = 14.sp,
                color = Color(0xFF8B8B8B),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
    }
}

// ─── Quick Reply overlay ──────────────────────────────────────────────────────

@Composable
private fun AppChallengeOverlay(
    appLabel: String,
    onLaunch: () -> Unit,
    onCancel: () -> Unit,
) {
    var secondsLeft by remember { mutableIntStateOf(5) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
        onLaunch()
    }
    BackHandler(onBack = onCancel)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0000000))
            .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Opening $appLabel",
                fontSize = 16.sp,
                color = Color(0xFF8B8B8B),
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = "$secondsLeft",
                fontSize = 72.sp,
                fontWeight = FontWeight.W300,
                color = Color.White,
                letterSpacing = (-2).sp,
            )
            Text(
                text = "Cancel",
                fontSize = 14.sp,
                color = Color(0xFF4DA3FF),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) { detectTapGestures(onTap = { onCancel() }) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
    }
}

private data class QuickReplyItem(
    val notifKey: String,
    val title: String,
    val text: String,
    val action: Notification.Action?,
    val remoteInputs: Array<RemoteInput>?,
    val remoteInputResultKey: String?,
)

private fun buildQuickReplyItems(pkg: String): List<QuickReplyItem> =
    NotificationRepository.snapshotActiveMap()
        .values
        .filter { it.packageName == pkg }
        .sortedByDescending { it.postTime }
        .take(5)
        .map { sbn ->
            val n = sbn.notification
            val title = (n.extras?.getCharSequence(Notification.EXTRA_TITLE) ?: "").toString()
            val text = (n.extras?.getCharSequence(Notification.EXTRA_TEXT) ?: "").toString()
            val replyAction = n.actions?.firstOrNull { a -> a.remoteInputs?.isNotEmpty() == true }
            QuickReplyItem(
                notifKey = sbn.key,
                title = title,
                text = text,
                action = replyAction,
                remoteInputs = replyAction?.remoteInputs,
                remoteInputResultKey = replyAction?.remoteInputs?.firstOrNull()?.resultKey,
            )
        }

@Composable
private fun QuickReplyOverlay(
    app: AppEntry,
    context: android.content.Context,
    packagesWithUnread: Set<String>,
    onDismiss: () -> Unit,
) {
    val items = remember(app.packageName, packagesWithUnread) {
        buildQuickReplyItems(app.packageName)
    }
    val replyItem = remember(items) { items.firstOrNull { it.action != null } }
    var replyText by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }

    val doSend = send@{
        val item = replyItem ?: return@send
        val action = item.action ?: return@send
        val inputs = item.remoteInputs ?: return@send
        val ri = inputs.firstOrNull() ?: return@send
        if (replyText.isBlank()) return@send
        val bundle = Bundle()
        bundle.putCharSequence(ri.resultKey, replyText)
        val fillIntent = Intent()
        RemoteInput.addResultsToIntent(inputs, fillIntent, bundle)
        val ok = runCatching { action.actionIntent.send(context, 0, fillIntent); true }.getOrDefault(false)
        if (ok) { sent = true; replyText = "" }
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE5000000))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111))
                .pointerInput(Unit) { detectTapGestures { /* consume to block background dismiss */ } },
        ) {
            // App header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bmp = remember(app.packageName) { app.icon?.toBitmap(48, 48) }
                if (bmp != null) {
                    androidx.compose.foundation.Image(
                        painter = BitmapPainter(bmp.asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = app.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NORMAL_TEXT,
                    modifier = Modifier.weight(1f),
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DIVIDER_COLOR))

            // Notification list
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (item.title.isNotEmpty()) {
                        Text(
                            text = item.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NORMAL_TEXT,
                        )
                    }
                    if (item.text.isNotEmpty()) {
                        Text(
                            text = item.text,
                            fontSize = 13.sp,
                            color = MUTED_TEXT,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DIVIDER_COLOR))
            }

            // Reply field — only shown when a notification exposes a RemoteInput action
            if (replyItem != null) {
                if (sent) {
                    Text(
                        text = "Sent ✓",
                        fontSize = 13.sp,
                        color = Color(0xFF34C759),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    event.key == Key.Enter &&
                                    replyText.isNotBlank()
                                ) {
                                    doSend()
                                    true
                                } else false
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            singleLine = true,
                            textStyle = TextStyle(color = NORMAL_TEXT, fontSize = 15.sp),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(fieldFocus),
                            decorationBox = { inner ->
                                if (replyText.isEmpty()) {
                                    Text("Reply…", fontSize = 15.sp, color = MUTED_TEXT)
                                }
                                inner()
                            },
                        )
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (replyText.isNotBlank()) FOCUS_ACCENT else Color(0xFF222222))
                                .pointerInput(replyText) {
                                    detectTapGestures(onTap = { if (replyText.isNotBlank()) doSend() })
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Send",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (replyText.isNotBlank()) Color.White else MUTED_TEXT,
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DIVIDER_COLOR))
            }

        }
    }

    LaunchedEffect(replyItem) {
        if (replyItem != null) runCatching { fieldFocus.requestFocus() }
    }
}

// ─── Clock / weather app resolvers ───────────────────────────────────────────

private val BB_HUB_PACKAGES = listOf(
    "com.blackberry.hub",
    "com.blackberry.bb.bbhub",
)

private fun resolveSwipeRightApp(context: android.content.Context, savedPkg: String): String? {
    val pm = context.packageManager
    if (savedPkg.isNotBlank() && runCatching { pm.getPackageInfo(savedPkg, 0) }.isSuccess) return savedPkg
    return BB_HUB_PACKAGES.firstOrNull { runCatching { pm.getPackageInfo(it, 0) }.isSuccess }
}

private fun resolveClockPackage(context: android.content.Context): String? {
    val pm = context.packageManager
    return listOf(
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.samsung.android.app.clockpackage",
        "com.htc.android.worldclock",
        "com.motorola.blur.alarmclock",
    ).firstOrNull { pkg -> runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess }
}

private fun resolveWeatherPackage(context: android.content.Context): String? {
    val pm = context.packageManager
    return listOf(
        "com.google.android.apps.weather",
        "com.weather.Weather",
        "com.yahoo.mobile.client.android.weather",
        "ru.yandex.weatherplugin",
        "com.samsung.android.weather",
    ).firstOrNull { pkg -> runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess }
}
