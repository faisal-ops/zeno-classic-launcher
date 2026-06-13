package com.zeno.classiclauncher.nlauncher.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette

@Composable
fun AppDrawerBadgesOverlay(
    showUsageStatsBadge: Boolean,
    showIconNotifBadge: Boolean,
    /** True when the "Unread badges" master switch is on AND notification listener permission is granted. */
    notificationAccessReady: Boolean,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onShowUsageStatsBadgeChange: (Boolean) -> Unit,
    onShowIconNotifBadgeChange: (Boolean) -> Unit,
) {
    val subtitleColor = Color(0xFF8E95A3)
    val focusRequester = remember { FocusRequester() }
    var focusedItem by remember { mutableIntStateOf(0) }

    BackHandler(enabled = true, onBack = onDismiss)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(405f)
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
                when {
                    back  -> { onDismiss(); true }
                    up    -> { focusedItem = (focusedItem - 1).coerceAtLeast(0); true }
                    down  -> { focusedItem = (focusedItem + 1).coerceAtMost(1); true }
                    enter -> {
                        when (focusedItem) {
                            0 -> onShowUsageStatsBadgeChange(!showUsageStatsBadge)
                            1 -> if (notificationAccessReady) onShowIconNotifBadgeChange(!showIconNotifBadge)
                        }
                        true
                    }
                    else -> false
                }
            },
        color = themePalette.settingsBg,
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
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
                        contentDescription = stringResource(R.string.action_back),
                        tint = themePalette.settingsMenuTitle,
                    )
                }
                Text(
                    stringResource(R.string.icon_badges_title),
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
                    stringResource(R.string.badge_overlay_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )

                BadgeSwitchRow(
                    title = stringResource(R.string.badge_usage_stats_title),
                    subtitle = if (showUsageStatsBadge) {
                        stringResource(R.string.badge_usage_stats_on)
                    } else {
                        stringResource(R.string.settings_off)
                    },
                    checked = showUsageStatsBadge,
                    focused = focusedItem == 0,
                    themePalette = themePalette,
                    onCheckedChange = onShowUsageStatsBadgeChange,
                )

                BadgeSwitchRow(
                    title = stringResource(R.string.icon_badges_title),
                    subtitle = when {
                        !notificationAccessReady ->
                            stringResource(R.string.badge_notif_permission_required)
                        showIconNotifBadge ->
                            stringResource(R.string.badge_notif_on)
                        else -> stringResource(R.string.settings_off)
                    },
                    checked = showIconNotifBadge && notificationAccessReady,
                    enabled = notificationAccessReady,
                    focused = focusedItem == 1,
                    themePalette = themePalette,
                    onCheckedChange = onShowIconNotifBadgeChange,
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BadgeSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    themePalette: LauncherThemePalette,
    focused: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                color = if (enabled) themePalette.settingsMenuTitle else themePalette.settingsMenuBody,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            Text(
                subtitle,
                color = themePalette.settingsMenuBody,
                fontSize = 12.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4A90D9),
                uncheckedThumbColor = Color(0xFF8E95A3),
                uncheckedTrackColor = Color(0xFF2E3545),
            ),
        )
    }
}
