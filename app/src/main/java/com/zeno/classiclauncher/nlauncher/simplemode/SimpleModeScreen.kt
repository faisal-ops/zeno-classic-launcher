package com.zeno.classiclauncher.nlauncher.simplemode

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
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
private val FOOTER_CONTENT_HEIGHT = 56.dp

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
    val visibleApps = remember(prefs.simpleModeApps, allApps) {
        resolveSimpleModeApps(prefs.simpleModeApps, allApps).take(6)
    }

    var focusedIndex by rememberSaveable(visibleApps.size) {
        mutableIntStateOf(0)
    }

    var clockTime by remember { mutableStateOf(currentTimeString()) }
    var clockDate by remember { mutableStateOf(currentDateString()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            clockTime = currentTimeString()
            clockDate = currentDateString()
        }
    }
    // Tick immediately and every minute on-the-minute
    LaunchedEffect(Unit) {
        clockTime = currentTimeString()
        clockDate = currentDateString()
    }

    val notifSummary = remember(packagesWithUnread, hasUnreadMail, hasUnreadSms, hasUnreadWhatsApp) {
        buildNotifSummary(packagesWithUnread, hasUnreadMail, hasUnreadSms, hasUnreadWhatsApp)
    }

    var enterPressedAt by remember { mutableLongStateOf(0L) }
    var showAppsEditor by rememberSaveable { mutableStateOf(false) }
    var showSimpleModeSettings by rememberSaveable { mutableStateOf(false) }
    var replacingIndex by rememberSaveable { mutableIntStateOf(-1) }

    // 0 = weather, 1 = player
    var footerPage by rememberSaveable { mutableIntStateOf(0) }
    // Auto-jump to player when music starts; jump back when it stops
    LaunchedEffect(nowPlaying != null) {
        if (nowPlaying != null) footerPage = 1 else footerPage = 0
    }

    val screenFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { screenFocus.requestFocus() } }
    val anyOverlayOpen = showAppsEditor || showSimpleModeSettings || replacingIndex >= 0
    LaunchedEffect(anyOverlayOpen) {
        if (!anyOverlayOpen) runCatching { screenFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SCREEN_BG)
            .focusRequester(screenFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                val appCount = visibleApps.size
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                        if (focusedIndex < appCount - 1) focusedIndex++
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                        if (focusedIndex > 0) focusedIndex--
                        true
                    }
                    // Grid: left/right arrow moves across columns
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight &&
                        prefs.simpleModeLayout == SimpleModeLayout.GRID -> {
                        if (focusedIndex < appCount - 1) focusedIndex++
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft &&
                        prefs.simpleModeLayout == SimpleModeLayout.GRID -> {
                        if (focusedIndex > 0) focusedIndex--
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
                .padding(top = 12.dp, bottom = 10.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            SimpleModeHeader(
                time = clockTime,
                date = clockDate,
                notifSummary = if (prefs.simpleModeShowNotifSummary) notifSummary else null,
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
                    apps = visibleApps,
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
                    onTap = { idx ->
                        focusedIndex = idx
                        visibleApps.getOrNull(idx)?.let { vm.launchApp(it.packageName) }
                    },
                    onLongPress = { idx -> replacingIndex = idx },
                )
            }

            // ── Footer: swipeable weather ↔ player ───────────────────────────
            val hasWeather = prefs.simpleModeShowWeather && forecast.isNotEmpty()
            val hasPlayer = nowPlaying != null
            if (hasWeather || hasPlayer) {
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
                            onPlayPause = { vm.mediaPlayPause() },
                            onNext = { vm.mediaSkipNext() },
                        )
                        hasWeather -> SimpleModeWeatherRow(
                            forecast = forecast,
                            useCelsius = prefs.glanceWeatherUnit == GlanceWeatherUnit.CELSIUS,
                        )
                        hasPlayer -> SimpleModeNowPlayingBar(
                            state = nowPlaying!!,
                            onPlayPause = { vm.mediaPlayPause() },
                            onNext = { vm.mediaSkipNext() },
                        )
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
                    // Materialise defaults if list was empty, then swap the slot
                    val appMap = allApps.associateBy { it.packageName }
                    val current = prefs.simpleModeApps.ifEmpty {
                        resolveSimpleModeApps(emptyList(), allApps)
                            .take(6)
                            .map { it.packageName }
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
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun SimpleModeHeader(
    time: String,
    date: String,
    notifSummary: String?,
    onGearTap: () -> Unit,
    onGearLongPress: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Clock + date + summary — centred in the full width
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = time,
                fontSize = 58.sp,
                fontWeight = FontWeight.W300,
                color = NORMAL_TEXT,
                letterSpacing = (-2).sp,
                lineHeight = 60.sp,
            )
            Text(
                text = date,
                fontSize = 14.sp,
                color = MUTED_TEXT,
            )
            if (notifSummary != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = notifSummary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD1D5DB),
                )
            }
        }

        // Gear icon — top-right corner, overlaid on the header
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Settings",
            tint = Color(0xFF555555),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp)
                .padding(4.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onGearTap() },
                        onLongPress = { onGearLongPress() },
                    )
                },
        )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
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

@Composable
private fun SimpleModeGridView(
    modifier: Modifier,
    apps: List<AppEntry>,
    focusedIndex: Int,
    showIcons: Boolean,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    val columns = 3
    val slotCount = maxOf(apps.size, 6)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false,
    ) {
        items(slotCount) { index ->
            val app = apps.getOrNull(index)
            if (app != null) {
                SimpleModeGridCell(
                    app = app,
                    selected = index == focusedIndex,
                    showIcon = showIcons,
                    onClick = { onTap(index) },
                    onLongPress = { onLongPress(index) },
                )
            } else {
                // Empty cell — tap to pick an app
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D0D0D))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onLongPress(index) })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "+", fontSize = 20.sp, color = Color(0xFF333333))
                }
            }
        }
    }
}

@Composable
private fun SimpleModeGridCell(
    app: AppEntry,
    selected: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) FOCUS_BG else Color(0xFF111111))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (showIcon) {
            val context = LocalContext.current
            val bmp = remember(app.packageName) {
                // Load system icon directly — bypasses icon packs that may supply monochrome icons
                runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap(128, 128) }
                    .getOrElse { app.icon?.toBitmap(128, 128) }
            }
            if (bmp != null) {
                androidx.compose.foundation.Image(
                    painter = BitmapPainter(bmp.asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        Text(
            text = app.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) FOCUS_TEXT else NORMAL_TEXT,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Weather row ──────────────────────────────────────────────────────────────

@Composable
internal fun SimpleModeWeatherRow(
    forecast: List<SimpleModeWeatherDay>,
    useCelsius: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DIVIDER_COLOR),
        )
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            forecast.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = day.label, fontSize = 11.sp, color = MUTED_TEXT, lineHeight = 14.sp)
                    Text(text = day.conditionEmoji, fontSize = 14.sp, lineHeight = 18.sp)
                    Text(
                        text = "${formatTemp(day.tempMaxC, useCelsius)}/${formatTemp(day.tempMinC, useCelsius)}",
                        fontSize = 11.sp,
                        color = NORMAL_TEXT,
                        lineHeight = 14.sp,
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
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DIVIDER_COLOR),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NORMAL_TEXT,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.artist.isNotEmpty()) {
                    Text(
                        text = state.artist,
                        fontSize = 12.sp,
                        color = MUTED_TEXT,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = NORMAL_TEXT,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onPlayPause() }
                    .padding(2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = NORMAL_TEXT,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onNext() }
                    .padding(2.dp),
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

private fun currentTimeString(): String = LocalDateTime.now().format(timeFormatter)
private fun currentDateString(): String = LocalDateTime.now().format(dateFormatter)

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
