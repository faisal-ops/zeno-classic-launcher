package com.zeno.classiclauncher.nlauncher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zeno.classiclauncher.nlauncher.power.SleepManager
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette

@Composable
fun LockWakeSettingsOverlay(
    doubleTapEnabled: Boolean,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onDoubleTapChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lockHelperOn by remember { mutableStateOf(SleepManager.isLockAccessibilityEnabled(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lockHelperOn = SleepManager.isLockAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun openAccessibilitySettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    val subtitleColor = Color(0xFF8E95A3)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(405f),
        color = themePalette.settingsBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = themePalette.settingsMenuTitle,
                    )
                }
                Text(
                    "Lock & wake",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = themePalette.settingsMenuTitle,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Double tap uses the built-in lock path. “Zeno Classic lock helper” in Accessibility remains optional for devices that need the power-button style lock path for face unlock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )

                SettingsSwitchRow(
                    title = "Double tap to lock",
                    subtitle = when {
                        !doubleTapEnabled -> "Off"
                        lockHelperOn -> "On — lock helper (face unlock)"
                        else -> "On — built-in lock path"
                    },
                    checked = doubleTapEnabled,
                    themePalette = themePalette,
                    onCheckedChange = { want ->
                        if (want) {
                            onDoubleTapChange(true)
                            lockHelperOn = SleepManager.isLockAccessibilityEnabled(context)
                        } else {
                            onDoubleTapChange(false)
                            lockHelperOn = SleepManager.isLockAccessibilityEnabled(context)
                        }
                    },
                )

                if (doubleTapEnabled && !lockHelperOn) {
                    TextButton(onClick = { openAccessibilitySettings() }) {
                        Text("Enable optional lock helper (Accessibility)", color = themePalette.settingsMenuBody)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    themePalette: LauncherThemePalette,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E2430),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = themePalette.settingsMenuTitle,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF8E95A3),
                        fontSize = 13.sp,
                    ),
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4A90D9),
                    uncheckedThumbColor = Color(0xFF9AA0A8),
                    uncheckedTrackColor = Color(0xFF3A3F4A),
                ),
            )
        }
    }
}
