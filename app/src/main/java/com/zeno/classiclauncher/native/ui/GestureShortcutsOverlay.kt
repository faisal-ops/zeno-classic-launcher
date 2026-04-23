package com.zeno.classiclauncher.nlauncher.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
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
    val cardShape = RoundedCornerShape(16.dp)
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize().zIndex(400f),
        color = themePalette.settingsBg,
    ) {
        if (activePicker != GesturePicker.None) {
            val isDoubleTap = activePicker == GesturePicker.DoubleTap
            val title = if (!isDoubleTap) "Swipe-up app" else "Double-tap action"
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
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (!isDoubleTap) {
                            onSetSwipeUp("")
                        } else {
                            onDoubleTapSleepChange(false)
                            onSetDoubleTap("")
                        }
                        activePicker = GesturePicker.None
                    }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(40.dp).padding(8.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("None (disabled)", color = subtitleColor, fontSize = 15.sp)
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
                        modifier = Modifier.fillMaxWidth().clickable {
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
                                    "If lock does not work on this device, enable “Zeno Classic lock helper” in Accessibility.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                            activePicker = GesturePicker.None
                        }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(40.dp).padding(8.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Sleep (lock screen)", color = themePalette.settingsMenuTitle, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (doubleTapToSleepEnabled) {
                            Text("✓", color = themePalette.settingsMenuBody, fontSize = 15.sp)
                        }
                    }
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
                LazyColumn {
                    items(allApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (!isDoubleTap) {
                                    onSetSwipeUp(app.packageName)
                                } else {
                                    onDoubleTapSleepChange(false)
                                    onSetDoubleTap(app.packageName)
                                }
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
                        "Home Gestures",
                        style = MaterialTheme.typography.titleMedium.copy(color = themePalette.settingsMenuTitle, fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = cardShape, color = cardBg) {
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
                                        "Launcher Quick Settings",
                                        color = themePalette.settingsMenuTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Swipe down on the home wallpaper to open Zeno’s quick settings panel",
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
                    Surface(modifier = Modifier.fillMaxWidth(), shape = cardShape, color = cardBg) {
                        Column(modifier = Modifier.fillMaxWidth().clickable { activePicker = GesturePicker.SwipeUp }.padding(16.dp)) {
                            Text("Swipe up", color = themePalette.settingsMenuTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (swipeUpPackage.isEmpty()) "Not set"
                                else allApps.find { it.packageName == swipeUpPackage }?.label ?: swipeUpPackage,
                                color = subtitleColor, fontSize = 13.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Double Tap row
                    Surface(modifier = Modifier.fillMaxWidth(), shape = cardShape, color = cardBg) {
                        Column(modifier = Modifier.fillMaxWidth().clickable { activePicker = GesturePicker.DoubleTap }.padding(16.dp)) {
                            Text("Double Tap", color = themePalette.settingsMenuTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                when {
                                    doubleTapToSleepEnabled -> "Sleep (lock screen)"
                                    doubleTapPackage.isNotEmpty() -> allApps.find { it.packageName == doubleTapPackage }?.label ?: doubleTapPackage
                                    else -> "Not set"
                                },
                                color = subtitleColor, fontSize = 13.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        buildString {
                            append("Swipe up on the home wallpaper to open the selected app.\n")
                            append("Double Tap to Lock uses the built-in lock path; the lock helper (Accessibility) is optional for devices that need the power-button style lock path for face unlock.")
                            if (customQuickSettingsEnabled) {
                                append("\nWhen the launcher panel is open, opening the system shade closes it.")
                            }
                        },
                        color = subtitleColor, fontSize = 12.sp, lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}
