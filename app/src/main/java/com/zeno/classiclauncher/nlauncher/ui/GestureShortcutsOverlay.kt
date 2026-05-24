package com.zeno.classiclauncher.nlauncher.ui

import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.power.SleepManager
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette

private enum class GesturePicker { SwipeUp, DoubleTap, None }

@Composable
fun GestureShortcutsOverlay(
    allApps: List<AppEntry>,
    swipeUpPackage: String,
    doubleTapPackage: String,
    doubleTapToSleepEnabled: Boolean,
    customQuickSettingsEnabled: Boolean,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onSetSwipeUp: (String) -> Unit,
    onSetDoubleTap: (String) -> Unit,
    onDoubleTapSleepChange: (Boolean) -> Unit,
    onCustomQuickSettingsChange: (Boolean) -> Unit,
) {
    var activePicker by remember { mutableStateOf(GesturePicker.None) }
    val subtitleColor = Color(0xFF8E95A3)
    val cardBg = Color(0xFF1E2430)
    val cardFocusedBg = Color(0xFF28303F)
    val cardShape = RoundedCornerShape(16.dp)
    val context = LocalContext.current

    val focusRequester = remember { FocusRequester() }
    var focusedMain by remember { mutableIntStateOf(0) }
    var focusedPicker by remember { mutableIntStateOf(0) }
    val pickerListState = rememberLazyListState()

    // Initialise focusedPicker to current selection when picker opens
    LaunchedEffect(activePicker) {
        if (activePicker == GesturePicker.None) return@LaunchedEffect
        val isDoubleTap = activePicker == GesturePicker.DoubleTap
        focusedPicker = when {
            isDoubleTap && doubleTapToSleepEnabled -> 1
            isDoubleTap && doubleTapPackage.isNotEmpty() -> {
                val idx = allApps.indexOfFirst { it.packageName == doubleTapPackage }
                if (idx >= 0) idx + 2 else 0
            }
            !isDoubleTap && swipeUpPackage.isNotEmpty() -> {
                val idx = allApps.indexOfFirst { it.packageName == swipeUpPackage }
                if (idx >= 0) idx + 1 else 0
            }
            else -> 0
        }
    }

    // Scroll LazyColumn to keep focused app visible
    LaunchedEffect(focusedPicker, activePicker) {
        if (activePicker == GesturePicker.None) return@LaunchedEffect
        val lazyOffset = if (activePicker == GesturePicker.DoubleTap) 2 else 1
        val lazyIdx = focusedPicker - lazyOffset
        if (lazyIdx >= 0) pickerListState.animateScrollToItem(lazyIdx)
    }

    BackHandler(enabled = true) {
        if (activePicker != GesturePicker.None) activePicker = GesturePicker.None else onDismiss()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(400f)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                val back  = ev.key == Key.Back || nk?.keyCode == AndroidKeyEvent.KEYCODE_BACK

                if (activePicker != GesturePicker.None) {
                    val isDoubleTap = activePicker == GesturePicker.DoubleTap
                    val lazyOffset = if (isDoubleTap) 2 else 1
                    val maxPicker = allApps.size + lazyOffset - 1
                    when {
                        back -> { activePicker = GesturePicker.None; true }
                        up   -> { focusedPicker = (focusedPicker - 1).coerceAtLeast(0); true }
                        down -> { focusedPicker = (focusedPicker + 1).coerceAtMost(maxPicker); true }
                        enter -> {
                            when {
                                focusedPicker == 0 -> {
                                    if (!isDoubleTap) onSetSwipeUp("")
                                    else { onDoubleTapSleepChange(false); onSetDoubleTap("") }
                                    activePicker = GesturePicker.None
                                }
                                isDoubleTap && focusedPicker == 1 -> {
                                    onDoubleTapSleepChange(true)
                                    onSetDoubleTap("")
                                    if (!SleepManager.isDoubleTapLockReady()) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                            )
                                        }
                                        Toast.makeText(context, context.getString(R.string.gesture_lock_helper_toast), Toast.LENGTH_LONG).show()
                                    }
                                    activePicker = GesturePicker.None
                                }
                                else -> {
                                    val app = allApps.getOrNull(focusedPicker - lazyOffset)
                                    if (app != null) {
                                        if (!isDoubleTap) onSetSwipeUp(app.packageName)
                                        else { onDoubleTapSleepChange(false); onSetDoubleTap(app.packageName) }
                                        activePicker = GesturePicker.None
                                    }
                                }
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    when {
                        back  -> { onDismiss(); true }
                        up    -> { focusedMain = (focusedMain - 1).coerceAtLeast(0); true }
                        down  -> { focusedMain = (focusedMain + 1).coerceAtMost(2); true }
                        enter -> {
                            when (focusedMain) {
                                0 -> onCustomQuickSettingsChange(!customQuickSettingsEnabled)
                                1 -> activePicker = GesturePicker.SwipeUp
                                2 -> activePicker = GesturePicker.DoubleTap
                            }
                            true
                        }
                        else -> false
                    }
                }
            },
        color = themePalette.settingsBg,
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        if (activePicker != GesturePicker.None) {
            val isDoubleTap = activePicker == GesturePicker.DoubleTap
            val lazyOffset = if (isDoubleTap) 2 else 1
            val title = if (!isDoubleTap) stringResource(R.string.gesture_swipe_up_app) else stringResource(R.string.gesture_double_tap_action)
            val current = if (!isDoubleTap) swipeUpPackage else doubleTapPackage
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { activePicker = GesturePicker.None }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = themePalette.settingsMenuTitle)
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(color = themePalette.settingsMenuTitle, fontWeight = FontWeight.SemiBold))
                }
                // None option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusedPicker == 0) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
                        .clickable {
                            if (!isDoubleTap) onSetSwipeUp("") else { onDoubleTapSleepChange(false); onSetDoubleTap("") }
                            activePicker = GesturePicker.None
                        }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(40.dp).padding(8.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.gesture_none_disabled), color = subtitleColor, fontSize = 15.sp)
                    val isNone = if (!isDoubleTap) current.isEmpty() else !doubleTapToSleepEnabled && current.isEmpty()
                    if (isNone) {
                        Spacer(Modifier.weight(1f))
                        Text("✓", color = themePalette.settingsMenuBody, fontSize = 15.sp)
                    }
                }
                // Sleep option — double-tap only
                if (isDoubleTap) {
                    Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusedPicker == 1) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
                            .clickable {
                                onDoubleTapSleepChange(true)
                                onSetDoubleTap("")
                                if (!SleepManager.isDoubleTapLockReady()) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                        )
                                    }
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.gesture_lock_helper_toast),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                                activePicker = GesturePicker.None
                            }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(40.dp).padding(8.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.gesture_sleep_lock), color = themePalette.settingsMenuTitle, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (doubleTapToSleepEnabled) {
                            Text("✓", color = themePalette.settingsMenuBody, fontSize = 15.sp)
                        }
                    }
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
                LazyColumn(state = pickerListState) {
                    itemsIndexed(allApps, key = { _, app -> app.packageName }) { index, app ->
                        val appIdx = index + lazyOffset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (focusedPicker == appIdx) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
                                .clickable {
                                    if (!isDoubleTap) onSetSwipeUp(app.packageName)
                                    else { onDoubleTapSleepChange(false); onSetDoubleTap(app.packageName) }
                                    activePicker = GesturePicker.None
                                }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = app.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(app.label, color = themePalette.settingsMenuTitle, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            val isSelected = if (!isDoubleTap) app.packageName == current
                            else !doubleTapToSleepEnabled && app.packageName == current
                            if (isSelected) {
                                Text("✓", color = themePalette.settingsMenuBody, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Main gesture settings screen
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = themePalette.settingsMenuTitle)
                    }
                    Text(
                        stringResource(R.string.settings_home_gestures_title),
                        style = MaterialTheme.typography.titleMedium.copy(color = themePalette.settingsMenuTitle, fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        color = if (focusedMain == 0) cardFocusedBg else cardBg,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.GridView,
                                    contentDescription = null,
                                    tint = subtitleColor,
                                    modifier = Modifier.size(40.dp).padding(8.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.gesture_quick_settings_title),
                                        color = themePalette.settingsMenuTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        stringResource(R.string.gesture_quick_settings_subtitle),
                                        color = subtitleColor,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                            Switch(
                                checked = customQuickSettingsEnabled,
                                onCheckedChange = onCustomQuickSettingsChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4A90D9),
                                    uncheckedThumbColor = Color(0xFF9AA0A8),
                                    uncheckedTrackColor = Color(0xFF3A3F4A),
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Swipe up row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        color = if (focusedMain == 1) cardFocusedBg else cardBg,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().clickable { activePicker = GesturePicker.SwipeUp }.padding(16.dp)) {
                            Text(stringResource(R.string.gesture_swipe_up), color = themePalette.settingsMenuTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (swipeUpPackage.isEmpty()) stringResource(R.string.settings_not_configured)
                                else allApps.find { it.packageName == swipeUpPackage }?.label ?: swipeUpPackage,
                                color = subtitleColor, fontSize = 13.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Double Tap row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        color = if (focusedMain == 2) cardFocusedBg else cardBg,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().clickable { activePicker = GesturePicker.DoubleTap }.padding(16.dp)) {
                            Text(stringResource(R.string.gesture_double_tap), color = themePalette.settingsMenuTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                when {
                                    doubleTapToSleepEnabled -> stringResource(R.string.gesture_sleep_lock)
                                    doubleTapPackage.isNotEmpty() -> allApps.find { it.packageName == doubleTapPackage }?.label ?: doubleTapPackage
                                    else -> stringResource(R.string.settings_not_configured)
                                },
                                color = subtitleColor, fontSize = 13.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.gesture_help_short),
                        color = subtitleColor, fontSize = 12.sp, lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}
