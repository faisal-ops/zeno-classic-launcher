package com.zeno.classiclauncher.nlauncher.minimalmode

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint as AndroidPaint
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
private val FOCUS_BG = Color(0xFF0F0F0F)
private val FOCUS_TEXT = Color.White
private val FOCUS_ACCENT = Color(0xFF4DA3FF)
// NORMAL_TEXT is used both by Minimal Mode's own body content AND by the shared ZenoStatusBar
// composable (clock/carrier text), which also renders on top of Zeno/Classic Mode's dark
// wallpaper via LauncherScreen.kt. MINIMAL_BODY_TEXT is kept as a distinct constant (currently
// equal to NORMAL_TEXT) so Minimal Mode's own body content isn't re-coupled to NORMAL_TEXT.
private val NORMAL_TEXT = Color.White
private val MINIMAL_BODY_TEXT = Color.White
private val MUTED_TEXT = Color(0xFF8B8B8B)
private val DIVIDER_COLOR = Color(0xFF1A1A1A)
private val SCREEN_BG = Color.Black

private fun wmoCodeToIcon(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Rounded.WbSunny
    in 51..55, in 61..65, in 80..82 -> Icons.Rounded.WaterDrop
    in 71..75 -> Icons.Rounded.AcUnit
    in 95..99 -> Icons.Rounded.Bolt
    else -> Icons.Rounded.Cloud
}

private val LONG_PRESS_MS = 700L
private val FOOTER_CONTENT_HEIGHT = 80.dp

internal fun parseAppLimits(raw: String): Map<String, Long> =
    raw.split(",").filter { it.contains(":") }
        .mapNotNull { entry ->
            val k = entry.substringBefore(":").trim()
            val v = entry.substringAfter(":").toLongOrNull()
            if (k.isNotEmpty() && v != null) k to v else null
        }.toMap()

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
    val maxApps = when (prefs.minimalModeMaxApps) {
        com.zeno.classiclauncher.nlauncher.prefs.MinimalModeMaxApps.SIX -> 6
        com.zeno.classiclauncher.nlauncher.prefs.MinimalModeMaxApps.NINE -> 9
        com.zeno.classiclauncher.nlauncher.prefs.MinimalModeMaxApps.TWELVE -> 12
        com.zeno.classiclauncher.nlauncher.prefs.MinimalModeMaxApps.AUTO -> 7
    }
    val visibleApps = remember(prefs.minimalModeApps, allApps, prefs.minimalModeMaxApps) {
        resolveMinimalModeApps(prefs.minimalModeApps, allApps).take(maxApps)
    }

    var focusedIndex by rememberSaveable(visibleApps.joinToString(",") { it.packageName }) {
        mutableIntStateOf(0)
    }

    val statusBar = rememberZenoStatusBarState()
    var clockDate by remember { mutableStateOf(currentDateString()) }
    var topBarBottomPx by remember { mutableIntStateOf(0) }

    // System-wide greyscale via root (daltonizer monochromacy mode).
    // Only used when rootGranted — non-rooted devices fall back to Compose saturation filter below.
    // LaunchedEffect handles reactive toggle; DisposableEffect(Unit) ensures cleanup on exit.
    var systemGreyscaleActive by remember { mutableStateOf(false) }
    LaunchedEffect(prefs.minimalModeGreyscale, prefs.rootGranted) {
        if (prefs.minimalModeGreyscale && prefs.rootGranted) {
            // Fire-and-forget: .start() without .waitFor() so the IO thread is not blocked.
            runCatching {
                ProcessBuilder("su", "-c",
                    "settings put secure accessibility_display_daltonizer_enabled 1 && " +
                    "settings put secure accessibility_display_daltonizer 0")
                    .start()
            }
            systemGreyscaleActive = true
        } else if (systemGreyscaleActive) {
            runCatching {
                ProcessBuilder("su", "-c",
                    "settings put secure accessibility_display_daltonizer_enabled 0")
                    .start()
            }
            systemGreyscaleActive = false
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            if (systemGreyscaleActive) {
                runCatching {
                    ProcessBuilder("su", "-c",
                        "settings put secure accessibility_display_daltonizer_enabled 0").start()
                }
            }
        }
    }

    // Date: ticks every second while the screen is on (time/AM-PM tick inside
    // rememberZenoStatusBarState). repeatOnLifecycle(RESUMED) suspends the loop when the
    // Activity moves to background or the screen turns off, preventing ~28 800 unnecessary
    // wakeups per screen-off hour.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                clockDate = currentDateString()
                val now = System.currentTimeMillis()
                delay(1_000L - (now % 1_000L))
            }
        }
    }

    // Screen time + per-app usage: fetched together in one IO call every 5 minutes while
    // the screen is on. Merging the two 60s loops into one 5-min loop reduces UsageStats
    // ContentProvider queries from 120/hr to 12/hr and stops all queries while screen is off.
    var totalScreenTimeMs by remember { mutableLongStateOf(0L) }
    var appUsageMap by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    totalScreenTimeMs = UsageStatsRepository.getTodayScreenOnTime(context)
                    appUsageMap = UsageStatsRepository.getLast24hUsage(context)
                }
                delay(5 * 60_000L)
            }
        }
    }

    // Parse per-app limits from prefs: "pkg:ms,pkg:ms" → Map
    val appLimitsMap = remember(prefs.minimalModeAppLimits) {
        parseAppLimits(prefs.minimalModeAppLimits)
    }

    // App challenge countdown state: non-null pkg = countdown in progress
    var challengeTargetPkg by remember { mutableStateOf<String?>(null) }

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
                if (prefs.minimalModeGreyscale && !prefs.rootGranted)
                    Modifier
                        .graphicsLayer {}
                        .drawWithContent {
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
                // Not .statusBarsPadding() — that reserves the frozen inset height as blank space
                // above this Column's content, pushing MinimalModeHeader/ZenoStatusBar below the
                // reserved strip instead of into it (invisible on the old black background, but a
                // visible gap now that SCREEN_BG is light). Zeno/Classic Mode's own ZenoStatusBar
                // avoids this by rendering directly inside the reserved height instead of padding
                // past it (see LauncherScreen.kt's own ZenoStatusBar call site) — here, simply not
                // reserving extra space at all lets the header start flush at the true top, since
                // ZenoStatusBar is already this Column's first child with nothing above it.
                .padding(horizontal = 14.dp)
                // Small top gap protects the bold 26sp date/temp header from rendering right at
                // the physical edge (cap-height/ascenders can look clipped at literal 0dp) — the
                // bottom row's own internal padding covers it fine at 0dp, so no matching gap
                // needed there.
                .padding(top = 3.dp, bottom = 0.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            MinimalModeHeader(
                time = statusBar.time,
                amPm = statusBar.amPm,
                date = clockDate,
                // Always shown now — the "Show Notification Summary" toggle was removed from
                // the Modes settings screen.
                unreadApps = unreadApps,
                conditionCode = forecast.firstOrNull()?.conditionCode,
                currentTemp = forecast.firstOrNull()?.let {
                    formatTemp(it.tempMaxC, prefs.glanceWeatherUnit == GlanceWeatherUnit.CELSIUS)
                },
                batteryPct = statusBar.batteryPct,
                batteryCharging = statusBar.batteryCharging,
                wifiConnected = statusBar.wifiConnected,
                soundProfile = statusBar.soundProfile,
                headsetConnected = statusBar.headsetConnected,
                locationEnabled = statusBar.locationEnabled,
                nfcEnabled = statusBar.nfcEnabled,
                carrierName = statusBar.carrierName,
                signalLevel = statusBar.signalLevel,
                wifiEnabled = statusBar.wifiEnabled,
                cellularActive = statusBar.cellularActive,
                bluetoothEnabled = statusBar.bluetoothEnabled,
                notifyIcons = statusBar.notifyIcons,
                powerSaveActive = statusBar.powerSaveActive,
                airplaneModeEnabled = statusBar.airplaneModeEnabled,
                hotspotEnabled = statusBar.hotspotEnabled,
                callIndicator = statusBar.callIndicator,
                micMuted = statusBar.micMuted,
                onClockTap = {
                    resolveClockPackage(context)?.let { vm.launchApp(it) }
                },
                onWeatherTap = {
                    resolveWeatherPackage(context)?.let { vm.launchApp(it) }
                },
                // The shared Wi-Fi broadcast receiver inside rememberZenoStatusBarState picks up
                // the change as soon as the system fires it — no manual refresh needed here.
                onWifiTap = { actions.openInternetPanel() },
                onAppIconTap = { pkg -> vm.launchApp(pkg) },
                onNotifyIconTap = { pkg -> vm.launchApp(pkg) },
                onTopBarPositioned = { topBarBottomPx = it },
            )

            Spacer(Modifier.height(8.dp))

            // ── App list ──────────────────────────────────────────────────────
            if (prefs.minimalModeLayout == com.zeno.classiclauncher.nlauncher.prefs.MinimalModeLayout.GRID) {
                MinimalModeGridView(
                    modifier = Modifier.weight(1f),
                    apps = visibleApps,
                    showIcons = prefs.minimalModeShowIcons,
                    packagesWithUnread = packagesWithUnread,
                    appLimitsMap = appLimitsMap,
                    appUsageMap = appUsageMap,
                    onTap = { idx -> visibleApps.getOrNull(idx)?.let { launchOrChallenge(it.packageName) } },
                    onLongPress = { idx -> visibleApps.getOrNull(idx)?.let { app -> contextMenuEntry = Pair(idx, app) } },
                )
            } else {
                MinimalModeListView(
                    modifier = Modifier.weight(1f),
                    apps = visibleApps,
                    maxApps = maxApps,
                    focusedIndex = if (showQuickSettings) -1 else focusedIndex,
                    appUsageMap = appUsageMap,
                    appLimitsMap = appLimitsMap,
                    packagesWithUnread = packagesWithUnread,
                    onTap = { idx ->
                        focusedIndex = idx
                        visibleApps.getOrNull(idx)?.let { launchOrChallenge(it.packageName) }
                    },
                    onLongPress = { idx ->
                        // Empty "+ Add app" slots route their tap through this same callback (see
                        // MinimalModeListView's null-app branch) but have no AppEntry to build a
                        // context menu around — go straight to the app picker instead of no-op'ing.
                        val app = visibleApps.getOrNull(idx)
                        if (app != null) contextMenuEntry = Pair(idx, app) else replacingIndex = idx
                    },
                )
            }

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
internal fun ZenoStatusBar(
    time: String,
    amPm: String,
    batteryPct: Int?,
    batteryCharging: Boolean,
    wifiConnected: Boolean,
    soundProfile: SoundProfileMode,
    headsetConnected: Boolean,
    onClockTap: () -> Unit,
    onWifiTap: () -> Unit,
    locationEnabled: Boolean = false,
    nfcEnabled: Boolean = false,
    carrierName: String = "",
    signalLevel: Int? = null,
    wifiEnabled: Boolean = wifiConnected,
    cellularActive: Boolean = false,
    bluetoothEnabled: Boolean = false,
    notifyIcons: List<ZenoStatusBarNotifyIcon> = emptyList(),
    powerSaveActive: Boolean = false,
    onNotifyIconTap: (String) -> Unit = {},
    airplaneModeEnabled: Boolean = false,
    hotspotEnabled: Boolean = false,
    /** Classic Mode only — a moderately larger clock, since it's the only home-screen clock there. */
    bigClock: Boolean = false,
    /** Minimal Mode's own status bar sits directly on its light SCREEN_BG, unlike Zeno/Classic
     *  Mode's dark wallpaper — set true there so text/icons switch to dark variants instead of
     *  the default light ones (which would be nearly invisible on a light background). */
    lightBackground: Boolean = false,
    /** Ongoing/just-missed call — replaces [carrierName] with a call/speaker/missed-call icon. */
    callIndicator: ZenoCallIndicator = ZenoCallIndicator.NONE,
    /** Mic muted during an active/speaker call — a separate icon shown on the left cluster. */
    micMuted: Boolean = false,
) {
    val textColor = if (lightBackground) MINIMAL_BODY_TEXT else NORMAL_TEXT
    val iconTint = if (lightBackground) SB_ICON_TINT_DARK else SB_ICON_TINT
    // 3-column Row: weight(1f) on both sides guarantees clock is truly centered on the display
    // rather than centered between the icon clusters — BB10 centres on the display.
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left — battery, then up to 2 notification icons (always shown — see below), then only
        // system-state indicators, then the notification icons — grouping "device state" (battery
        // + mute/Bluetooth/GPS/NFC) together right after battery, with the more time-sensitive
        // "alerts" (notifications) as a visually separate cluster after it. BB10 omits inactive
        // indicators entirely rather than greying them out; system icons are capped independently
        // of notifications (fixed slot budgets below) rather than shrinking icons to fit.
        //
        // System-state icons fill their fixed budget in priority order — mute/DND (explains
        // silence, most behaviourally relevant) > Bluetooth (usually a relied-on paired device) >
        // GPS (already transient — only shown while actively in use) > NFC (momentary-use, least
        // useful to see persistently) — dropping from the end of that list first when there isn't
        // room for all four.
        val systemIconCandidates = buildList {
            if (soundProfile == SoundProfileMode.DND || soundProfile == SoundProfileMode.VIBRATE) add(SbSystemIcon.MUTE)
            if (bluetoothEnabled) add(SbSystemIcon.BLUETOOTH)
            if (locationEnabled) add(SbSystemIcon.GPS)
            if (nfcEnabled) add(SbSystemIcon.NFC)
        }
        val shownSystemIcons = systemIconCandidates.take(SB_SYSTEM_ICON_SLOTS).toSet()
        val shownNotifyIcons = notifyIcons.take(SB_NOTIFY_ICON_SLOTS)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(SB_GAP_LEFT, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (batteryPct != null) {
                ZenoStatusBarBattery(pct = batteryPct, charging = batteryCharging, powerSaveActive = powerSaveActive, tint = iconTint)
            }
            if (SbSystemIcon.MUTE in shownSystemIcons) {
                when (soundProfile) {
                    SoundProfileMode.DND -> ZenoStatusBarGlyph(
                        icon = Icons.Rounded.NotificationsOff,
                        contentDescription = stringResource(R.string.quick_settings_dnd),
                        size = SB_MUTE,
                        tint = iconTint,
                    )
                    SoundProfileMode.VIBRATE -> ZenoStatusBarGlyph(
                        icon = Icons.Rounded.Vibration,
                        contentDescription = stringResource(R.string.sound_profile_vibrate),
                        size = SB_MUTE,
                        tint = iconTint,
                    )
                    else -> Unit
                }
            }
            if (SbSystemIcon.BLUETOOTH in shownSystemIcons) {
                Icon(
                    painter = painterResource(R.drawable.ic_sb_bluetooth),
                    contentDescription = stringResource(R.string.quick_settings_bluetooth),
                    tint = iconTint,
                    modifier = Modifier.size(width = SB_BLUETOOTH_W, height = SB_GLYPH),
                )
            }
            if (SbSystemIcon.GPS in shownSystemIcons) {
                Icon(
                    painter = painterResource(R.drawable.ic_sb_gps),
                    contentDescription = stringResource(R.string.quick_settings_location),
                    tint = iconTint,
                    modifier = Modifier.size(width = SB_GPS_W, height = SB_GLYPH),
                )
            }
            if (SbSystemIcon.NFC in shownSystemIcons) {
                Icon(
                    painter = painterResource(R.drawable.ic_sb_nfc),
                    contentDescription = stringResource(R.string.quick_settings_nfc),
                    tint = iconTint,
                    modifier = Modifier.size(width = SB_NFC_W, height = SB_GLYPH),
                )
            }
            shownNotifyIcons.forEach { entry ->
                androidx.compose.foundation.Image(
                    painter = BitmapPainter(entry.bitmap.asImageBitmap()),
                    contentDescription = stringResource(R.string.cd_notification_icon),
                    modifier = Modifier
                        .size(SB_GLYPH)
                        .pointerInput(entry.packageName) {
                            detectTapGestures(onTap = { onNotifyIconTap(entry.packageName) })
                        },
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
                fontSize = if (bigClock) SB_CLOCK_SIZE_BIG else SB_CLOCK_SIZE,
                fontWeight = FontWeight.W400, // TEST: was W300 (reference spec) — trying W400
                color = textColor,
                letterSpacing = (-0.5).sp,
            )
            if (amPm.isNotEmpty()) {
                Text(
                    // BB10 always renders AM/PM uppercase; the shared formatter is
                    // locale-cased (lowercase under e.g. en_IN), so uppercase only here.
                    text = amPm.uppercase(),
                    fontSize = if (bigClock) SB_AMPM_SIZE_BIG else SB_AMPM_SIZE,
                    fontWeight = FontWeight.W400,
                    color = textColor,
                    modifier = Modifier.padding(top = SB_AMPM_BASELINE_OFFSET, start = 1.dp),
                )
            }
        }
        // Right — carrier, headset, Wi-Fi, BlackBerry mark, signal bars.
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(SB_GAP_RIGHT, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Call state takes over this slot entirely — carrier name is hidden while a call is
            // active/just-missed, matching the reference BB10 status bar.
            when (callIndicator) {
                // Material's Call/VolumeUp/CallMissed glyphs have less internal padding than the
                // other status-bar icons at the same nominal SB_GLYPH box, so they rendered
                // visibly chunkier/larger than their neighbors — sized down to match.
                ZenoCallIndicator.ACTIVE -> ZenoStatusBarGlyph(
                    icon = Icons.Rounded.Call,
                    contentDescription = stringResource(R.string.cd_call_active),
                    size = SB_CALL_ICON_SIZE,
                    tint = iconTint,
                )
                ZenoCallIndicator.SPEAKER -> ZenoStatusBarGlyph(
                    icon = Icons.Rounded.VolumeUp,
                    contentDescription = stringResource(R.string.cd_call_speaker),
                    size = SB_CALL_ICON_SIZE,
                    tint = iconTint,
                )
                ZenoCallIndicator.MISSED -> ZenoStatusBarGlyph(
                    icon = Icons.Rounded.CallMissed,
                    contentDescription = stringResource(R.string.cd_call_missed),
                    size = SB_CALL_ICON_SIZE,
                    tint = iconTint,
                )
                ZenoCallIndicator.NONE -> if (carrierName.isNotEmpty()) {
                    Text(
                        // BB10 title-cases the carrier name (e.g. "airtel" -> "Airtel") regardless
                        // of how the SIM reports it.
                        text = carrierName.replaceFirstChar { it.titlecase() },
                        fontSize = SB_CARRIER_SIZE,
                        fontWeight = FontWeight.W500,
                        color = textColor,
                    )
                }
            }
            // Mic muted during an active/speaker call — kept on the same (right) side as the
            // call/speaker icon above, not the left cluster.
            if (micMuted) {
                ZenoStatusBarGlyph(
                    icon = Icons.Rounded.MicOff,
                    contentDescription = stringResource(R.string.cd_mic_muted),
                    size = SB_MUTE,
                    tint = iconTint,
                )
            }
            if (airplaneModeEnabled) {
                Icon(
                    painter = painterResource(R.drawable.ic_sb_airplane),
                    contentDescription = stringResource(R.string.quick_settings_aeroplane_mode),
                    tint = iconTint,
                    modifier = Modifier
                        .size(SB_AIRPLANE_SIZE)
                        .offset(y = SB_RIGHT_ICON_VISUAL_OFFSET),
                )
            }
            if (hotspotEnabled) {
                Icon(
                    painter = painterResource(R.drawable.ic_sb_hotspot),
                    contentDescription = stringResource(R.string.quick_settings_hotspot),
                    tint = iconTint,
                    modifier = Modifier
                        .size(SB_GLYPH)
                        .offset(y = SB_RIGHT_ICON_VISUAL_OFFSET),
                )
            }
            if (headsetConnected) {
                Box(modifier = Modifier.offset(y = SB_RIGHT_ICON_VISUAL_OFFSET)) {
                    ZenoStatusBarGlyph(
                        icon = Icons.Rounded.Headset,
                        contentDescription = stringResource(R.string.cd_headset_connected),
                        size = SB_GLYPH,
                        tint = iconTint,
                    )
                }
            }
            // 4GLTE: shown only while cellular is the active/default transport — i.e. mobile
            // data is actually carrying traffic, not merely "mobile data enabled".
            if (cellularActive) {
                Text(
                    text = "4GLTE",
                    fontSize = SB_4GLTE_SIZE,
                    fontWeight = FontWeight.W400,
                    letterSpacing = (-0.3).sp,
                    color = iconTint,
                )
            }
            // Wi-Fi: hidden entirely when the radio is off. While on, full-tint means it's the
            // active transport (real internet); dimmed means enabled but idle — e.g. cellular is
            // carrying traffic instead. Never struck-through, matching the GPS/NFC/mute rule.
            if (wifiEnabled) {
                Icon(
                    painter = painterResource(R.drawable.ic_sb_wifi),
                    contentDescription = "WiFi on",
                    tint = if (wifiConnected) iconTint else iconTint.copy(alpha = SB_WIFI_DIM_ALPHA),
                    modifier = Modifier
                        .size(SB_WIFI_SIZE)
                        .offset(y = SB_RIGHT_ICON_VISUAL_OFFSET)
                        .pointerInput(Unit) { detectTapGestures(onTap = { onWifiTap() }) },
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_sb_blackberry),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(width = SB_BB_MARK_W, height = SB_BB_MARK_H)
                    .offset(y = SB_RIGHT_ICON_VISUAL_OFFSET),
            )
            if (signalLevel != null) {
                Box(modifier = Modifier.offset(y = SB_RIGHT_ICON_VISUAL_OFFSET)) {
                    ZenoStatusBarSignalBars(level = signalLevel, tint = iconTint)
                }
            }
        }
    }
}

/** Uniformly tinted/sized status-bar vector glyph — collapses four identical [Icon] call sites. */
@Composable
private fun ZenoStatusBarGlyph(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    tint: Color = SB_ICON_TINT,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size),
    )
}

/**
 * Cellular bars, drawn rather than blitted: the BB10 pack ships no signal asset because the
 * bars are OS-rendered on real hardware. Ascending 5-bar ramp; bars above [level] stay visible
 * but dimmed, which is how BB10 shows headroom.
 */
@Composable
private fun ZenoStatusBarSignalBars(level: Int, tint: Color = SB_ICON_TINT) {
    Canvas(modifier = Modifier.size(width = SB_BARS_W, height = SB_BARS_H)) {
        val gap = size.width * SB_BARS_GAP_FRAC
        val barW = (size.width - gap * (SB_BARS_COUNT - 1)) / SB_BARS_COUNT
        val lit = (level + 1).coerceIn(0, SB_BARS_COUNT)
        val radius = CornerRadius(0.5.dp.toPx())
        for (i in 0 until SB_BARS_COUNT) {
            val barH = size.height * (SB_BARS_MIN_FRAC + SB_BARS_STEP_FRAC * i)
            drawRoundRect(
                color = tint.copy(alpha = if (i < lit) 1f else SB_BARS_DIM_ALPHA),
                topLeft = Offset(i * (barW + gap), size.height - barH),
                size = Size(barW, barH),
                cornerRadius = radius,
            )
        }
    }
}

// ─── ZenoStatusBar geometry ───────────────────────────────────────────────────
// Derived from the approved BB10 reference rendered at the Q25's native panel: 720x720 at
// 208dpi, i.e. a density scale of 1.3, so every reference pixel divides by 1.3 into dp. The
// source px is kept alongside each value so re-measuring the reference only ever touches this
// block — nothing below hardcodes a magic number.
// No horizontal padding here: MinimalModeHeader's outer Column already applies
// `.padding(horizontal = 14.dp)`, shared with the date/weather row below — adding another
// inset here would either double it or desync the status bar from that row.
// Real system status bar measured at exactly 23.8dp (31px @ 208dpi/scale 1.3 — confirmed via
// `adb shell dumpsys window displays`, InsetsSource type=statusBars frame=[0,0][720,31]). When
// ZenoStatusBar replaces that bar (Zeno/Classic Mode's toggle) it renders inside that exact
// height, so icons are sized to fill most of it — the reference photos show icons occupying
// nearly the full row height, not floating in a mostly-empty strip. Minimal Mode's own row has
// no fixed height and simply wraps this same content.
// Left/right icon gaps tightened — the reference reads as tightly packed, not spaced out.
private val SB_GAP_LEFT     = 4.dp
private val SB_GAP_RIGHT    = 5.dp
// The right cluster mixes Icon/Canvas glyphs (edge-to-edge within their own box) with Text
// (carrier name, 4GLTE — has built-in font-metric padding above the visible glyph). Centering
// both kinds via the same CenterVertically centers their boxes, not their visible pixels, so
// the icons/bars read as floating higher than the text. Nudges the non-text elements down to
// match the text's visual glyph center instead.
private val SB_RIGHT_ICON_VISUAL_OFFSET = 2.dp
// Left-cluster crowding control (battery not counted — it never drops). System-state icons and
// notifications each get their own fixed slot budget, independent of each other, priority order
// mute > Bluetooth > GPS > NFC for system icons, most-recent-first for notifications — dropped
// from the end of each list when there isn't room for everything active at once.
private const val SB_SYSTEM_ICON_SLOTS = 2
private const val SB_NOTIFY_ICON_SLOTS = 3
private enum class SbSystemIcon { MUTE, BLUETOOTH, GPS, NFC }
private val SB_GLYPH        = 19.dp   // Wi-Fi / headset / notification icons (square-ish source art)
// Material's Call/VolumeUp/CallMissed glyphs read visually larger than SB_GLYPH's other icons at
// the same nominal size (less internal padding baked into the source art) — sized down to match.
private val SB_CALL_ICON_SIZE = 15.dp
private val SB_MUTE         = 20.dp
// ic_sb_airplane.png is tightly cropped (near-zero internal padding), unlike Material's Headset
// vector or Wi-Fi's own crop, both of which carry more built-in margin — at the same nominal
// SB_GLYPH box it read as visibly larger than its neighbors. Sized down to compensate.
private val SB_AIRPLANE_SIZE = 15.dp
// Bluetooth/GPS/NFC assets are naturally tall/narrow (cropped bbox aspect, measured directly from
// the pack's PNGs), NOT square like the other glyphs. Forcing them into a square SB_GLYPH box
// left each one letterboxed by a different amount, so their actual glyph pixels ended up
// different visual widths — reading as inconsistent gaps between icons despite uniform 4dp
// Compose spacing between the (equal-size) boxes. Sized to each one's real aspect ratio at the
// shared SB_GLYPH height instead: Bluetooth 48x86px, GPS 50x72px, NFC 39x69px post-crop.
private val SB_BLUETOOTH_W  = 10.6.dp
private val SB_GPS_W        = 13.2.dp
private val SB_NFC_W        = 10.7.dp
// Berry mark kept deliberately smaller than the other glyphs — at equal height its wide 1.404
// aspect made it visually dominate the right cluster. Aspect preserved (20/14.2 ≈ 1.404).
private val SB_BB_MARK_W    = 20.dp
private val SB_BB_MARK_H    = 14.dp
// Clock text was clipped against the 23.8dp reserved row at the previous, larger size (21sp
// clock + 7dp AM/PM offset overflowed it) — pulled back down to fit with margin.
private val SB_CLOCK_SIZE   = 18.sp
private val SB_CLOCK_SIZE_BIG = 22.sp
private val SB_AMPM_SIZE_BIG = 13.sp
// Reference (real BB10 device photo, "11:35AM") reads AM/PM at nearly the same weight/brightness
// as the time digits, not as a small dimmed subscript — bumped up from 9sp/MUTED_TEXT to match.
private val SB_AMPM_SIZE    = 12.sp
// Reference (AT&T screenshot) reads the carrier name notably larger than the other right-cluster
// glyphs, and the Wi-Fi mark closer to the signal bars' height than the other 19dp glyphs.
private val SB_CARRIER_SIZE = 15.sp
private val SB_WIFI_SIZE    = 22.dp
// Tuned for the previous 9sp size — at the current 12sp, that much downward push sinks PM
// below the clock's own baseline instead of sitting on it. Reduced to compensate.
/** Nudges AM/PM down so its cap-height sits on the clock's baseline, as BB10 sets it. */
private val SB_AMPM_BASELINE_OFFSET = 1.dp
private val SB_ICON_TINT = Color(0xFFEAF0F6)
/** ZenoStatusBar's icon tint when rendered with lightBackground=true (Minimal Mode's own light
 *  SCREEN_BG) — SB_ICON_TINT is unreadable there, same reasoning as MINIMAL_BODY_TEXT. */
private val SB_ICON_TINT_DARK = Color(0xFF2E2E2E)
/** "4GLTE" network-type glyph — bold/condensed to read as a compact mark, not prose. */
private val SB_4GLTE_SIZE = 11.sp
/** Wi-Fi tint when the radio is on but not the active transport (enabled-but-idle). */
private const val SB_WIFI_DIM_ALPHA = 0.45f

// Signal bars — no asset exists for these (OS-rendered on real BB10), so they are drawn.
private val SB_BARS_W = 22.dp
private val SB_BARS_H = 19.dp
private const val SB_BARS_COUNT = 5
private const val SB_BARS_GAP_FRAC = 0.10f
private const val SB_BARS_MIN_FRAC = 0.30f   // shortest bar, as a fraction of full height
private const val SB_BARS_STEP_FRAC = 0.175f // per-bar rise; bar 5 lands exactly on 1.0
private const val SB_BARS_DIM_ALPHA = 0.30f

// Battery proportions measured directly from the pack's own ic_battery.png (glyph occupies
// 74x40 inside a 96px canvas): total aspect 1.85, nub is 8.1% of total width and 60% of body
// height. Drawn rather than blitted because the asset is a *static* glyph — using it would
// throw away the live charge level.
private val SB_BATTERY_W = 33.dp
private val SB_BATTERY_H = 18.dp
// Widened from the pack's literal 8.1%/1dp — read as too thin against the other status-bar
// glyphs' stroke weights once placed next to them at this size.
private const val SB_BATTERY_NUB_W_FRAC = 0.13f
private const val SB_BATTERY_NUB_H_FRAC = 0.60f
private val SB_CHARGE_COLOR = Color(0xFFFFC400)
private val SB_BATTERY_LOW_COLOR = Color(0xFFEE3123)
private val SB_BATTERY_SAVER_COLOR = Color(0xFFFFE9A6)
private const val SB_BATTERY_LOW_PCT = 15

/** Lightning bolt in unit space, drawn inside the body while charging (BB10 does the same). */
private val SB_BOLT_POINTS = listOf(
    0.58f to 0.00f, 0.28f to 0.56f, 0.48f to 0.56f,
    0.42f to 1.00f, 0.74f to 0.42f, 0.54f to 0.42f,
)

@Composable
private fun ZenoStatusBarBattery(pct: Int, charging: Boolean, powerSaveActive: Boolean = false, tint: Color = SB_ICON_TINT) {
    // Border/nub color: charging and battery saver both keep the neutral outline color (the
    // yellow bolt / light-yellow fill already signal their states); a low, non-charging,
    // non-saver battery reads as red.
    val color = when {
        charging -> tint
        powerSaveActive -> tint
        pct <= SB_BATTERY_LOW_PCT -> SB_BATTERY_LOW_COLOR
        else -> tint
    }
    Canvas(
        modifier = Modifier.size(width = SB_BATTERY_W, height = SB_BATTERY_H),
    ) {
        val nubW = size.width * SB_BATTERY_NUB_W_FRAC
        val bodyW = size.width - nubW
        val bodyH = size.height
        // Widened from 1dp — the outline nearly vanished against the icon cluster around it.
        val stroke = 1.6.dp.toPx()
        val radius = CornerRadius(1.5.dp.toPx())

        drawRoundRect(
            color = color,
            size = Size(bodyW, bodyH),
            cornerRadius = radius,
            style = Stroke(width = stroke),
        )

        val inset = stroke * 1.6f
        if (powerSaveActive && !charging) {
            // Battery saver: same body/nub silhouette and white border as the normal gauge, with
            // a "+" mark matching Android's own battery-saver iconography since Lollipop — but
            // the yellow fill is proportional to the actual charge level, same as the normal
            // gauge below. It was previously always drawn full regardless of charge, which read
            // as "always full" even well below 50%.
            val fillW = (bodyW - inset * 2) * (pct.coerceIn(0, 100) / 100f)
            if (fillW > 0f) {
                drawRoundRect(
                    color = SB_BATTERY_SAVER_COLOR,
                    topLeft = Offset(inset, inset),
                    size = Size(fillW, bodyH - inset * 2),
                    cornerRadius = CornerRadius(0.5.dp.toPx()),
                )
            }
            // Centered on the whole body (not the fill) and drawn in the icon tint rather than
            // the fill-contrast color — it needs to read clearly whether it lands over the
            // yellow fill (low charge) or the plain dark background (high charge) behind it.
            val plusLen = (bodyH - inset * 2) * 0.55f
            val plusThickness = plusLen * 0.28f
            val pcx = inset + (bodyW - inset * 2) / 2f
            val pcy = inset + (bodyH - inset * 2) / 2f
            val plusRadius = CornerRadius(plusThickness / 4f)
            drawRoundRect(
                color = tint,
                topLeft = Offset(pcx - plusThickness / 2f, pcy - plusLen / 2f),
                size = Size(plusThickness, plusLen),
                cornerRadius = plusRadius,
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(pcx - plusLen / 2f, pcy - plusThickness / 2f),
                size = Size(plusLen, plusThickness),
                cornerRadius = plusRadius,
            )
        } else {
            val fillW = (bodyW - inset * 2) * (pct.coerceIn(0, 100) / 100f)
            if (fillW > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(inset, inset),
                    size = Size(fillW, bodyH - inset * 2),
                    cornerRadius = CornerRadius(0.5.dp.toPx()),
                )
            }
        }

        val nubH = bodyH * SB_BATTERY_NUB_H_FRAC
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyW, (bodyH - nubH) / 2f),
            size = Size(nubW, nubH),
            cornerRadius = CornerRadius(0.5.dp.toPx()),
        )

        if (charging) {
            val boltH = bodyH * 0.82f
            val boltW = boltH * 0.55f
            val left = (bodyW - boltW) / 2f
            val top = (bodyH - boltH) / 2f
            val path = Path().apply {
                SB_BOLT_POINTS.forEachIndexed { i, (ux, uy) ->
                    val x = left + ux * boltW
                    val y = top + uy * boltH
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            // Solid yellow fill (BB10's charging color) reads clearly at any charge level or
            // background — the previous clear-cutout + thin white outline nearly disappeared.
            drawPath(path, color = SB_CHARGE_COLOR)
        }
    }
}

@Composable
private fun MinimalModeHeader(
    time: String,
    amPm: String,
    date: String,
    unreadApps: List<AppEntry>,
    conditionCode: Int?,
    currentTemp: String?,
    batteryPct: Int?,
    batteryCharging: Boolean,
    wifiConnected: Boolean,
    soundProfile: SoundProfileMode,
    headsetConnected: Boolean,
    onClockTap: () -> Unit,
    onWeatherTap: () -> Unit,
    onWifiTap: () -> Unit,
    onAppIconTap: (String) -> Unit,
    onTopBarPositioned: (Int) -> Unit = {},
    locationEnabled: Boolean = false,
    nfcEnabled: Boolean = false,
    carrierName: String = "",
    signalLevel: Int? = null,
    wifiEnabled: Boolean = wifiConnected,
    cellularActive: Boolean = false,
    bluetoothEnabled: Boolean = false,
    notifyIcons: List<ZenoStatusBarNotifyIcon> = emptyList(),
    powerSaveActive: Boolean = false,
    onNotifyIconTap: (String) -> Unit = {},
    airplaneModeEnabled: Boolean = false,
    hotspotEnabled: Boolean = false,
    callIndicator: ZenoCallIndicator = ZenoCallIndicator.NONE,
    micMuted: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Box(modifier = Modifier.onGloballyPositioned { coords ->
            onTopBarPositioned((coords.positionInRoot().y + coords.size.height).toInt())
        }) {
        ZenoStatusBar(
            time = time,
            amPm = amPm,
            batteryPct = batteryPct,
            batteryCharging = batteryCharging,
            wifiConnected = wifiConnected,
            soundProfile = soundProfile,
            headsetConnected = headsetConnected,
            onClockTap = onClockTap,
            onWifiTap = onWifiTap,
            locationEnabled = locationEnabled,
            nfcEnabled = nfcEnabled,
            carrierName = carrierName,
            signalLevel = signalLevel,
            wifiEnabled = wifiEnabled,
            cellularActive = cellularActive,
            bluetoothEnabled = bluetoothEnabled,
            notifyIcons = notifyIcons,
            powerSaveActive = powerSaveActive,
            onNotifyIconTap = onNotifyIconTap,
            airplaneModeEnabled = airplaneModeEnabled,
            hotspotEnabled = hotspotEnabled,
            callIndicator = callIndicator,
            micMuted = micMuted,
            // Minimal Mode's own SCREEN_BG is dark again — lightBackground defaults to false.
            // Matches Classic Mode's clock size/style — same ZenoStatusBar look across all modes.
            bigClock = true,
        )
        } // end measurement Box

        // ── Date/weather · notifications — left-aligned, matching the app list below ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = { onWeatherTap() }, onDoubleTap = {})
                },
            ) {
                // Bigger than the clock itself (SB_CLOCK_SIZE_BIG = 22.sp) and full-bright with
                // bold weight, so this row reads as more prominent than the clock rather than a
                // secondary caption under it.
                val dateTempColor = Color.White
                Text(text = date, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = dateTempColor)
                if (currentTemp != null) {
                    Text(text = "  ·  ", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = dateTempColor)
                    Text(text = currentTemp, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = dateTempColor)
                }
            }

            // Always reserved, regardless of unreadApps — collapsing/re-appearing this row made
            // the app list below jump up and down every time a notification arrived or got read.
            // A stable empty strip here reads better than that layout shift.
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.CenterStart) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    unreadApps.forEach { app ->
                        val bmp = remember(app.packageName) {
                            val src = app.icon?.toBitmap(48, 48) ?: return@remember null
                            val grey = android.graphics.Bitmap.createBitmap(src.width, src.height, src.config ?: android.graphics.Bitmap.Config.ARGB_8888)
                            val paint = AndroidPaint().apply {
                                colorFilter = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0f) })
                            }
                            Canvas(grey).drawBitmap(src, 0f, 0f, paint)
                            grey
                        }
                        if (bmp != null) {
                            androidx.compose.foundation.Image(
                                painter = BitmapPainter(bmp.asImageBitmap()),
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .pointerInput(app.packageName) {
                                        detectTapGestures(onTap = { onAppIconTap(app.packageName) })
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Grid view ────────────────────────────────────────────────────────────────

@Composable
private fun MinimalModeGridView(
    modifier: Modifier,
    apps: List<AppEntry>,
    showIcons: Boolean,
    packagesWithUnread: Set<String>,
    appLimitsMap: Map<String, Long>,
    appUsageMap: Map<String, Long>,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        gridItems(apps, key = { it.packageName }) { app ->
            val index = apps.indexOf(app)
            val usageMs = appUsageMap[app.packageName] ?: 0L
            val limitMs = appLimitsMap[app.packageName]
            val overLimit = limitMs != null && usageMs >= limitMs
            val hasNotif = app.packageName in packagesWithUnread
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                    .pointerInput(app.packageName) {
                        detectTapGestures(onTap = { onTap(index) }, onLongPress = { onLongPress(index) })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (showIcons) {
                        val bmp = remember(app.packageName) { app.icon?.toBitmap(48, 48) }
                        if (bmp != null) {
                            androidx.compose.foundation.Image(
                                painter = BitmapPainter(bmp.asImageBitmap()),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                    Text(
                        text = app.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W400,
                        color = if (overLimit) Color(0xFFFFB340) else MINIMAL_BODY_TEXT,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (hasNotif || overLimit) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(6.dp)
                            .background(
                                if (overLimit) Color(0xFFFFB340) else FOCUS_ACCENT,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

// ─── List view ────────────────────────────────────────────────────────────────

@Composable
private fun MinimalModeListView(
    modifier: Modifier,
    apps: List<AppEntry>,
    maxApps: Int,
    focusedIndex: Int,
    appUsageMap: Map<String, Long>,
    appLimitsMap: Map<String, Long>,
    packagesWithUnread: Set<String>,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(maxApps) { index ->
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
                    hasNotification = app.packageName in packagesWithUnread,
                    onClick = { onTap(index) },
                    onLongPress = { onLongPress(index) },
                )
            } else {
                val emptySlotCd = stringResource(R.string.cd_simple_empty_slot)
                val addAppLabel = stringResource(R.string.action_add_app)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = emptySlotCd
                            onClick(label = addAppLabel) { onLongPress(index); true }
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
    hasNotification: Boolean,
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
        2 -> 0.28f
        else -> 0.14f
    }
    val rowAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 180),
        label = "rowAlpha",
    )
    val appNameWeight = when (distanceFromFocus) {
        0 -> FontWeight.W500
        1 -> FontWeight.W400
        2 -> FontWeight.W300
        else -> FontWeight.W200
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer { alpha = rowAlpha }
            .drawBehind {
                if (selected) drawRect(color = FOCUS_ACCENT, size = Size(3.dp.toPx(), size.height))
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = tx.value }
                .drawBehind { if (selected) drawRect(color = FOCUS_BG) }
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val nameColor = when {
                overLimit -> Color(0xFFFFB340)
                else -> MINIMAL_BODY_TEXT
            }
            Text(
                text = app.label,
                fontSize = 22.sp,
                fontWeight = appNameWeight,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // Right side: notification dot, or over-limit warning
            Box(
                modifier = Modifier.size(width = 14.dp, height = 14.dp).padding(start = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (overLimit) {
                    Box(modifier = Modifier.size(5.dp).background(Color(0xFFFFB340), CircleShape))
                } else if (hasNotification) {
                    val dotColor = if (selected) FOCUS_ACCENT else Color(0xFF606060)
                    Box(modifier = Modifier.size(5.dp).background(dotColor, CircleShape))
                }
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
                    Text(text = day.label, fontSize = 11.sp, color = Color(0xFF343434), lineHeight = 13.sp)
                    Icon(
                        imageVector = wmoCodeToIcon(day.conditionCode),
                        contentDescription = null,
                        tint = Color(0xFF505050),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "${formatTemp(day.tempMaxC, useCelsius)}/${formatTemp(day.tempMinC, useCelsius)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF505050),
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
                    color = MINIMAL_BODY_TEXT,
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
                tint = MINIMAL_BODY_TEXT,
                modifier = Modifier.size(40.dp).clickable { onPrevious() },
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (state.isPlaying) R.string.action_pause else R.string.action_play),
                tint = MINIMAL_BODY_TEXT,
                modifier = Modifier.size(46.dp).clickable { onPlayPause() },
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.action_next),
                tint = MINIMAL_BODY_TEXT,
                modifier = Modifier.size(40.dp).clickable { onNext() },
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

internal fun readBatteryPct(context: android.content.Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: return -1
    return if (scale > 0) level * 100 / scale else -1
}

internal fun readBatteryCharging(context: android.content.Context): Boolean {
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
            val limitHeaderColor = if (menuFocusIdx == 2) MINIMAL_BODY_TEXT else Color(0xFF9AA0A8)
            Text(
                text = stringResource(R.string.minimal_mode_daily_limit),
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
            color = if (keyboardFocused) Color(0xFF4DA3FF) else MINIMAL_BODY_TEXT,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(text = value, fontSize = 14.sp, color = valueColor)
        }
    }
}

private val timeFormatter12 = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
private val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
private val amPmFormatter = DateTimeFormatter.ofPattern("a", Locale.getDefault())

internal fun currentTimeString(is24h: Boolean = false): String =
    LocalDateTime.now().format(if (is24h) timeFormatter24 else timeFormatter12)
private fun currentDateString(): String = LocalDateTime.now().format(dateFormatter)
internal fun currentAmPmString(): String = LocalDateTime.now().format(amPmFormatter)


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
                    textStyle = TextStyle(color = MINIMAL_BODY_TEXT, fontSize = 16.sp),
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
                            color = MINIMAL_BODY_TEXT,
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
                text = stringResource(R.string.minimal_mode_daily_limit_reached),
                fontSize = 14.sp,
                color = Color(0xFFFFB340),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.minimal_mode_usage_summary, UsageStatsRepository.formatUsage(usageMs), UsageStatsRepository.formatUsage(limitMs)),
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
                text = stringResource(R.string.minimal_mode_challenge_cancel),
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
                text = stringResource(R.string.minimal_mode_opening_app, appLabel),
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
                text = stringResource(R.string.minimal_mode_challenge_cancel),
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
    val remoteInputs: List<RemoteInput>?,
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
                remoteInputs = replyAction?.remoteInputs?.toList(),
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
        RemoteInput.addResultsToIntent(inputs.toTypedArray(), fillIntent, bundle)
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
                    color = MINIMAL_BODY_TEXT,
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
                            color = MINIMAL_BODY_TEXT,
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
                        text = stringResource(R.string.minimal_mode_quick_reply_sent),
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
                            textStyle = TextStyle(color = MINIMAL_BODY_TEXT, fontSize = 15.sp),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(fieldFocus),
                            decorationBox = { inner ->
                                if (replyText.isEmpty()) {
                                    Text(stringResource(R.string.minimal_mode_reply_hint), fontSize = 15.sp, color = MUTED_TEXT)
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
                                text = stringResource(R.string.minimal_mode_quick_reply_send),
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
