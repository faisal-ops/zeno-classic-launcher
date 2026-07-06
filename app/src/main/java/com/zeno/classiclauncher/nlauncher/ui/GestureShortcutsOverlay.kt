package com.zeno.classiclauncher.nlauncher.ui

import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.TouchApp
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.power.SleepManager
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette

private enum class GesturePicker { SwipeUp, SwipeRight, None }

@Composable
fun GestureShortcutsOverlay(
    allApps: List<AppEntry>,
    swipeUpPackage: String,
    swipeRightPackage: String,
    doubleTapPackage: String,
    doubleTapToSleepEnabled: Boolean,
    customQuickSettingsEnabled: Boolean,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onSetSwipeUp: (String) -> Unit,
    onSetSwipeRight: (String) -> Unit,
    onSetDoubleTap: (String) -> Unit,
    onDoubleTapSleepChange: (Boolean) -> Unit,
    onCustomQuickSettingsChange: (Boolean) -> Unit,
) {
    var activePicker by remember { mutableStateOf(GesturePicker.None) }
    val subtitleColor = Color(0xFF8E95A3)
    val cardBg = Color(0xFF1E2430)
    val cardFocusedBg = Color(0xFF252D3E)
    val cardFocusedBorder = BorderStroke(1.dp, Color(0x6684D5F6))
    val cardShape = RoundedCornerShape(16.dp)
    val context = LocalContext.current

    val focusRequester = remember { FocusRequester() }
    var focusedMain by remember { mutableIntStateOf(0) }

    fun toggleDoubleTapSleep() {
        val turningOn = !doubleTapToSleepEnabled
        onDoubleTapSleepChange(turningOn)
        onSetDoubleTap("")
        if (turningOn && !SleepManager.isDoubleTapLockReady()) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            Toast.makeText(context, context.getString(R.string.gesture_lock_helper_toast), Toast.LENGTH_LONG).show()
        }
    }

    BackHandler(enabled = true) {
        if (activePicker != GesturePicker.None) activePicker = GesturePicker.None else onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(400f)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                // The picker is a separate full-screen overlay with its own focus/key handling
                // (AppSelectorOverlay) — let its events through instead of consuming them here.
                if (activePicker != GesturePicker.None) return@onPreviewKeyEvent false
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                // NOTE: Key.Back intentionally NOT handled here — the BackHandler handles it.
                // Handling Back in onPreviewKeyEvent AND BackHandler causes a double-fire on
                // physical BlackBerry back key (LineageOS), closing both the overlay AND settings.
                when {
                    up    -> { focusedMain = (focusedMain - 1).coerceAtLeast(0); true }
                    down  -> { focusedMain = (focusedMain + 1).coerceAtMost(3); true }
                    enter -> {
                        when (focusedMain) {
                            0 -> onCustomQuickSettingsChange(!customQuickSettingsEnabled)
                            1 -> activePicker = GesturePicker.SwipeUp
                            2 -> activePicker = GesturePicker.SwipeRight
                            3 -> toggleDoubleTapSleep()
                        }
                        true
                    }
                    else -> false
                }
            },
        color = themePalette.settingsBg,
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        // Main gesture settings screen
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = themePalette.settingsMenuTitle)
                    }
                    Text(
                        stringResource(R.string.settings_home_gestures_title),
                        style = MaterialTheme.typography.titleMedium.copy(color = themePalette.settingsMenuTitle, fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Swipe down — quick settings toggle
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        color = if (focusedMain == 0) cardFocusedBg else cardBg,
                        border = if (focusedMain == 0) cardFocusedBorder else null,
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
                        border = if (focusedMain == 1) cardFocusedBorder else null,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { activePicker = GesturePicker.SwipeUp }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowUp,
                                contentDescription = null,
                                tint = subtitleColor,
                                modifier = Modifier.size(40.dp).padding(8.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.gesture_swipe_up), color = themePalette.settingsMenuTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (swipeUpPackage.isEmpty()) stringResource(R.string.settings_not_configured)
                                    else allApps.find { it.packageName == swipeUpPackage }?.label ?: swipeUpPackage,
                                    color = subtitleColor, fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Swipe right row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        color = if (focusedMain == 2) cardFocusedBg else cardBg,
                        border = if (focusedMain == 2) cardFocusedBorder else null,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { activePicker = GesturePicker.SwipeRight }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = subtitleColor,
                                modifier = Modifier.size(40.dp).padding(8.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.gesture_swipe_right_label), color = themePalette.settingsMenuTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (swipeRightPackage.isEmpty()) stringResource(R.string.settings_not_configured)
                                    else allApps.find { it.packageName == swipeRightPackage }?.label ?: swipeRightPackage,
                                    color = subtitleColor, fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Double Tap row — simple lock on/off toggle, no app selection.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        color = if (focusedMain == 3) cardFocusedBg else cardBg,
                        border = if (focusedMain == 3) cardFocusedBorder else null,
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
                                    Icons.Rounded.TouchApp,
                                    contentDescription = null,
                                    tint = subtitleColor,
                                    modifier = Modifier.size(40.dp).padding(8.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.gesture_double_tap), color = themePalette.settingsMenuTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        stringResource(R.string.gesture_double_tap_subtitle),
                                        color = subtitleColor,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                            Switch(
                                checked = doubleTapToSleepEnabled,
                                onCheckedChange = { toggleDoubleTapSleep() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4A90D9),
                                    uncheckedThumbColor = Color(0xFF9AA0A8),
                                    uncheckedTrackColor = Color(0xFF3A3F4A),
                                ),
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

    if (activePicker != GesturePicker.None) {
        val title = when (activePicker) {
            GesturePicker.SwipeUp -> stringResource(R.string.gesture_swipe_up_app)
            GesturePicker.SwipeRight -> stringResource(R.string.gesture_swipe_right_app)
            GesturePicker.None -> ""
        }
        val current = when (activePicker) {
            GesturePicker.SwipeUp -> swipeUpPackage
            GesturePicker.SwipeRight -> swipeRightPackage
            GesturePicker.None -> ""
        }
        AppSelectorOverlay(
            title = title,
            apps = allApps,
            themePalette = themePalette,
            selectedPackage = current,
            unsetLabel = stringResource(R.string.gesture_none_disabled),
            onSelect = { pkg ->
                when (activePicker) {
                    GesturePicker.SwipeUp -> onSetSwipeUp(pkg)
                    GesturePicker.SwipeRight -> onSetSwipeRight(pkg)
                    GesturePicker.None -> {}
                }
                activePicker = GesturePicker.None
            },
            onDismiss = { activePicker = GesturePicker.None },
        )
    }
    }
}
