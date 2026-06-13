package com.zeno.classiclauncher.nlauncher.simplemode

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.zeno.classiclauncher.nlauncher.power.LockScreenAccessibilityService
import com.zeno.classiclauncher.nlauncher.power.SleepManager
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Pause
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
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.prefs.GlanceWeatherUnit
import androidx.compose.foundation.layout.widthIn
import com.zeno.classiclauncher.nlauncher.prefs.SimpleModeLayout
import com.zeno.classiclauncher.nlauncher.ui.LauncherViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

// Default apps shown when user hasn't configured Simple Mode list yet.
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
internal fun SimpleModeScreen(vm: LauncherViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val allApps by vm.apps.collectAsStateWithLifecycle()
    val forecast by vm.simpleModeForecast.collectAsStateWithLifecycle()
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
    val hasUnreadMail by vm.hasUnreadMail.collectAsStateWithLifecycle()
    val hasUnreadSms by vm.hasUnreadSms.collectAsStateWithLifecycle()
    val hasUnreadWhatsApp by vm.hasUnreadWhatsApp.collectAsStateWithLifecycle()
    val packagesWithUnread by vm.packagesWithUnread.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val is24h = remember { DateFormat.is24HourFormat(context) }
    val visibleApps = remember(prefs.simpleModeApps, allApps) {
        resolveSimpleModeApps(prefs.simpleModeApps, allApps).take(9)
    }

    var focusedIndex by rememberSaveable(visibleApps.size) {
        mutableIntStateOf(0)
    }

    var clockTime by remember { mutableStateOf(currentTimeString(is24h)) }
    var clockAmPm by remember { mutableStateOf(if (is24h) "" else currentAmPmString()) }
    var clockDate by remember { mutableStateOf(currentDateString()) }
    var batteryPct by remember { mutableIntStateOf(readBatteryPct(context)) }
    var batteryCharging by remember { mutableStateOf(readBatteryCharging(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            clockTime = currentTimeString(is24h)
            clockAmPm = if (is24h) "" else currentAmPmString()
            clockDate = currentDateString()
            batteryPct = readBatteryPct(context)
            batteryCharging = readBatteryCharging(context)
        }
    }
    // Tick immediately on first composition
    LaunchedEffect(Unit) {
        clockTime = currentTimeString(is24h)
        clockAmPm = if (is24h) "" else currentAmPmString()
        clockDate = currentDateString()
    }

    // Fetch total screen-on time using SCREEN_INTERACTIVE/NON_INTERACTIVE events — matches
    // Digital Wellbeing exactly. Avoids per-app sum which double-counts overlapping sessions.
    var totalScreenTimeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            totalScreenTimeMs = UsageStatsRepository.getTodayScreenOnTime(context)
        }
    }

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
    var enterPressedAt by remember { mutableLongStateOf(0L) }
    var showAppsEditor by rememberSaveable { mutableStateOf(false) }
    var showSimpleModeSettings by rememberSaveable { mutableStateOf(false) }
    var replacingIndex by rememberSaveable { mutableIntStateOf(-1) }

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
    val anyOverlayOpen = showAppsEditor || showSimpleModeSettings || replacingIndex >= 0 || showSearchOverlay
    LaunchedEffect(anyOverlayOpen) {
        if (!anyOverlayOpen) runCatching { screenFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SCREEN_BG)
            .then(
                if (prefs.simpleModeGreyscale)
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
                var swipeAccum = 0f
                detectVerticalDragGestures(
                    onDragEnd = { swipeAccum = 0f },
                    onDragCancel = { swipeAccum = 0f },
                    onVerticalDrag = { _, delta ->
                        swipeAccum += delta
                        if (swipeAccum > 60f) {
                            LockScreenAccessibilityService.tryShowNotifications()
                            swipeAccum = 0f
                        }
                    },
                )
            }
            .onPreviewKeyEvent { event ->
                // List shows max 6, grid shows all 9 — cap navigation to what's visible
                val appCount = if (prefs.simpleModeLayout == SimpleModeLayout.LIST) {
                    minOf(visibleApps.size, 6)
                } else {
                    visibleApps.size
                }
                val isGrid = prefs.simpleModeLayout == SimpleModeLayout.GRID
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                        val next = if (isGrid) focusedIndex + GRID_COLS else focusedIndex + 1
                        if (next < appCount) focusedIndex = next
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                        val prev = if (isGrid) focusedIndex - GRID_COLS else focusedIndex - 1
                        if (prev >= 0) focusedIndex = prev
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight && isGrid -> {
                        // stay in same row: only move if not already on last column
                        if (focusedIndex % GRID_COLS < GRID_COLS - 1 && focusedIndex + 1 < appCount)
                            focusedIndex++
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && isGrid -> {
                        // stay in same row: only move if not already on first column
                        if (focusedIndex % GRID_COLS > 0) focusedIndex--
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.Enter -> {
                        enterPressedAt = System.currentTimeMillis()
                        true
                    }
                    event.type == KeyEventType.KeyUp && event.key == Key.Enter -> {
                        val held = System.currentTimeMillis() - enterPressedAt
                        enterPressedAt = 0L
                        if (held >= LONG_PRESS_MS) {
                            showAppsEditor = true
                        } else {
                            visibleApps.getOrNull(focusedIndex)?.let { app ->
                                vm.launchApp(app.packageName)
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
                .navigationBarsPadding()
                .padding(horizontal = 14.dp)
                .padding(top = 4.dp, bottom = 10.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            SimpleModeHeader(
                time = clockTime,
                amPm = clockAmPm,
                date = clockDate,
                unreadApps = if (prefs.simpleModeShowNotifSummary) unreadApps else emptyList(),
                hasAnyNotif = hasAnyNotif,
                conditionEmoji = forecast.firstOrNull()?.conditionEmoji,
                currentTemp = forecast.firstOrNull()?.let {
                    formatTemp(it.tempMaxC, prefs.glanceWeatherUnit == GlanceWeatherUnit.CELSIUS)
                },
                batteryPct = if (batteryPct >= 0) batteryPct else null,
                batteryCharging = batteryCharging,
                screenTimeMs = if (totalScreenTimeMs > 0L) totalScreenTimeMs else null,
                onClockTap = {
                    resolveClockPackage(context)?.let { vm.launchApp(it) }
                },
                onWeatherTap = {
                    resolveWeatherPackage(context)?.let { vm.launchApp(it) }
                },
                onUnreadAppTap = { pkg -> vm.launchApp(pkg) },
                onGearTap = { showSimpleModeSettings = true },
                onGearLongPress = {
                    context.startActivity(
                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
            )

            Spacer(Modifier.height(8.dp))

            // ── App list / grid ───────────────────────────────────────────────
            when (prefs.simpleModeLayout) {
                SimpleModeLayout.LIST -> SimpleModeListView(
                    modifier = Modifier.weight(1f),
                    apps = visibleApps.take(6),
                    focusedIndex = focusedIndex,
                    showIcons = false,
                    onTap = { idx ->
                        focusedIndex = idx
                        visibleApps.getOrNull(idx)?.let { vm.launchApp(it.packageName) }
                    },
                    onLongPress = { idx -> replacingIndex = idx },
                )
                SimpleModeLayout.GRID -> SimpleModeGridView(
                    modifier = Modifier.weight(1f),
                    apps = visibleApps,
                    focusedIndex = focusedIndex,
                    showIcons = true,
                    packagesWithUnread = packagesWithUnread,
                    onTap = { idx ->
                        focusedIndex = idx
                        visibleApps.getOrNull(idx)?.let { vm.launchApp(it.packageName) }
                    },
                    onLongPress = { idx -> replacingIndex = idx },
                )
            }

            // ── Footer: swipeable weather ↔ player ───────────────────────────
            // hasWeatherPref = weather pref is on (footer space always reserved to avoid layout shift)
            // hasWeather     = data actually arrived
            val hasWeatherPref = prefs.simpleModeShowWeather
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
                        footerPage == 1 && hasPlayer -> SimpleModeNowPlayingBar(
                            state = nowPlaying!!,
                            onPrevious = { vm.mediaPreviousTrack() },
                            onPlayPause = { vm.mediaPlayPause() },
                            onNext = { vm.mediaSkipNext() },
                        )
                        hasWeather -> SimpleModeWeatherRow(
                            forecast = forecast,
                            useCelsius = prefs.glanceWeatherUnit == GlanceWeatherUnit.CELSIUS,
                        )
                        hasPlayer -> SimpleModeNowPlayingBar(
                            state = nowPlaying!!,
                            onPrevious = { vm.mediaPreviousTrack() },
                            onPlayPause = { vm.mediaPlayPause() },
                            onNext = { vm.mediaSkipNext() },
                        )
                        hasWeatherPref -> SimpleModeWeatherPlaceholder()
                    }
                }
            }
        }

        if (showAppsEditor) {
            SimpleModeAppsEditor(
                allApps = allApps,
                selectedPackages = prefs.simpleModeApps,
                onSave = { vm.setSimpleModeApps(it) },
                onDismiss = { showAppsEditor = false },
            )
        }

        if (showSimpleModeSettings) {
            SimpleModeSettingsOverlay(
                vm = vm,
                onDismiss = { showSimpleModeSettings = false },
            )
        }

        if (replacingIndex >= 0) {
            SimpleModeAppSelector(
                allApps = allApps,
                onSelect = { selectedPkg ->
                    val current = prefs.simpleModeApps.ifEmpty {
                        resolveSimpleModeApps(emptyList(), allApps).take(9).map { it.packageName }
                    }.toMutableList()
                    if (replacingIndex < current.size) {
                        current[replacingIndex] = selectedPkg
                    } else {
                        current.add(selectedPkg)
                    }
                    vm.setSimpleModeApps(current)
                    replacingIndex = -1
                },
                onDismiss = { replacingIndex = -1 },
            )
        }

        // Search overlay — type any letter or tap the search icon to open
        if (showSearchOverlay) {
            SimpleModeSearchOverlay(
                allApps = allApps,
                onLaunch = { pkg -> vm.launchApp(pkg); showSearchOverlay = false },
                onDismiss = { showSearchOverlay = false },
            )
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun SimpleModeHeader(
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
    screenTimeMs: Long?,
    onClockTap: () -> Unit,
    onWeatherTap: () -> Unit,
    onGearTap: () -> Unit,
    onGearLongPress: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Top bar: [battery · stats] LEFT · [clock] CENTER · [gear] RIGHT ────
        // 3-column Row: weight(1f) on both sides guarantees clock is truly centered.
        // All children share the same verticalAlignment = CenterVertically baseline.
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
                    BatteryPill(pct = batteryPct, charging = batteryCharging)
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
            // Center — time digits only (no AM/PM), so the digits are pixel-perfectly centered.
            Text(
                text = time,
                fontSize = 36.sp,
                fontWeight = FontWeight.W300,
                color = NORMAL_TEXT,
                letterSpacing = (-1).sp,
                modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onClockTap() }) },
            )
            // Right side — AM/PM at start, gear at end, both inside the right weight(1f) box.
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (amPm.isNotEmpty()) {
                    Text(
                        text = " $amPm",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W300,
                        color = MUTED_TEXT,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = Color(0x2EFFFFFF),
                    modifier = Modifier
                        .size(28.dp)
                        .padding(2.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onGearTap() },
                                onLongPress = { onGearLongPress() },
                            )
                        },
                )
            }
        }

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
                    text = "No New Notifications",
                    fontSize = 12.sp,
                    color = MUTED_TEXT,
                )
            }
        }
    }
}

// ─── List view ────────────────────────────────────────────────────────────────

@Composable
private fun SimpleModeListView(
    modifier: Modifier,
    apps: List<AppEntry>,
    focusedIndex: Int,
    showIcons: Boolean,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    // Always fill at least 6 slots so rows don't stretch too tall on short lists
    val slotCount = maxOf(apps.size, 6)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(slotCount) { index ->
            val app = apps.getOrNull(index)
            if (app != null) {
                SimpleModeListRow(
                    app = app,
                    selected = index == focusedIndex,
                    distanceFromFocus = kotlin.math.abs(index - focusedIndex),
                    showIcon = showIcons,
                    onClick = { onTap(index) },
                    onLongPress = { onLongPress(index) },
                )
            } else {
                // Empty slot — tap to pick an app for this position
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onLongPress(index) })
                        }
                        .padding(start = 20.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "+ Add app",
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleModeListRow(
    app: AppEntry,
    selected: Boolean,
    distanceFromFocus: Int,
    showIcon: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val density = LocalDensity.current
    // unselected = 8dp right, selected = 16dp right → drifts further right on focus
    val tx = remember { Animatable(with(density) { if (selected) 16.dp.toPx() else 8.dp.toPx() }) }
    LaunchedEffect(selected) {
        tx.animateTo(
            targetValue = with(density) { if (selected) 16.dp.toPx() else 8.dp.toPx() },
            animationSpec = tween(durationMillis = 200),
        )
    }
    // Proximity fade: items further from focus dim out, drawing the eye to the selection
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
            .graphicsLayer { alpha = rowAlpha }
            .background(if (selected) FOCUS_BG else Color.Transparent)
            .drawBehind {
                if (selected) drawRect(color = FOCUS_ACCENT, size = Size(4.dp.toPx(), size.height))
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            },
    ) {
        // ▌ pinned 1dp after the 4dp blue accent bar, does not drift
        if (selected) {
            Text(
                text = "▌",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 7.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = tx.value }
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = app.label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) FOCUS_TEXT else NORMAL_TEXT,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Grid view ────────────────────────────────────────────────────────────────

private const val GRID_COLS = 3
private const val GRID_ROWS = 3

@Composable
private fun SimpleModeGridView(
    modifier: Modifier,
    apps: List<AppEntry>,
    focusedIndex: Int,
    showIcons: Boolean,
    packagesWithUnread: Set<String>,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        for (row in 0 until GRID_ROWS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                for (col in 0 until GRID_COLS) {
                    val index = row * GRID_COLS + col
                    val app = apps.getOrNull(index)
                    if (app != null) {
                        SimpleModeGridCell(
                            modifier = Modifier.weight(1f),
                            app = app,
                            selected = index == focusedIndex,
                            showIcon = showIcons,
                            hasUnread = app.packageName in packagesWithUnread,
                            onClick = { onTap(index) },
                            onLongPress = { onLongPress(index) },
                        )
                    } else {
                        // Empty slot — tap to pick an app for this position
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF181818))
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { onLongPress(index) })
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "+", fontSize = 20.sp, color = Color(0xFF484848))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleModeGridCell(
    modifier: Modifier = Modifier,
    app: AppEntry,
    selected: Boolean,
    showIcon: Boolean,
    hasUnread: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FOCUS_BG else Color(0xFF111111))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (showIcon) {
            val bmp = remember(app.packageName) {
                app.icon?.toBitmap(128, 128)
            }
            if (bmp != null) {
                com.zeno.classiclauncher.nlauncher.badges.AppIconWithBadge(
                    hasUnread = hasUnread,
                    badgeDiameter = 12.dp,
                    glyphSp = 7f,
                ) {
                    androidx.compose.foundation.Image(
                        painter = BitmapPainter(bmp.asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(3.dp))
            }
        }
        Text(
            text = app.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) FOCUS_TEXT else NORMAL_TEXT,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Weather row ──────────────────────────────────────────────────────────────

@Composable
private fun SimpleModeWeatherPlaceholder() {
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
internal fun SimpleModeWeatherRow(
    forecast: List<SimpleModeWeatherDay>,
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
private fun SimpleModeNowPlayingBar(
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
                contentDescription = "Previous",
                tint = NORMAL_TEXT,
                modifier = Modifier.size(40.dp).clickable { onPrevious() },
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = NORMAL_TEXT,
                modifier = Modifier.size(46.dp).clickable { onPlayPause() },
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
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

@Composable
private fun BatteryPill(pct: Int, charging: Boolean) {
    val bgColor = when {
        pct >= 50 -> Color(0xFF3DDC84) // green
        pct >= 20 -> Color(0xFFFFB300) // amber
        else      -> Color(0xFFE53935) // red
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$pct",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            lineHeight = 13.sp,
        )
        if (charging) {
            Text(text = "⚡", fontSize = 9.sp, lineHeight = 11.sp, color = Color.Black)
        }
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

internal fun resolveSimpleModeApps(
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
private fun SimpleModeSearchOverlay(
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
                            Text("Search apps…", fontSize = 16.sp, color = MUTED_TEXT)
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

// ─── Clock / weather app resolvers ───────────────────────────────────────────

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
