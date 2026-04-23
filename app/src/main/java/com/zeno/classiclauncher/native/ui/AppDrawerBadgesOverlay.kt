package com.zeno.classiclauncher.nlauncher.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
                    "App icon badges",
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
                    "Choose what small badges appear on app icons in the drawer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )

                BadgeSwitchRow(
                    title = "Notification badge",
                    subtitle = when {
                        !notificationAccessReady ->
                            "Requires notification access — enable Unread badges in Permissions first"
                        showIconNotifBadge ->
                            "On — red star appears when an app has unread notifications"
                        else -> "Off"
                    },
                    checked = showIconNotifBadge && notificationAccessReady,
                    enabled = notificationAccessReady,
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
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
