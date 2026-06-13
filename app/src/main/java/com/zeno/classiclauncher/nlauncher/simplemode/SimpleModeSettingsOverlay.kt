package com.zeno.classiclauncher.nlauncher.simplemode

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.prefs.SimpleModeLayout
import com.zeno.classiclauncher.nlauncher.ui.LauncherViewModel

private val SETTINGS_BG = Color(0xFF0E131B)
private val CARD_BG = Color(0xFF161D2A)
private val TITLE_COLOR = Color(0xFFE8EEF7)
private val SUBTITLE_COLOR = Color(0xFF6B7280)
private val ACCENT_COLOR = Color(0xFF4A90D9)

@Composable
internal fun SimpleModeSettingsOverlay(
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SETTINGS_BG)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.simple_mode_settings_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = TITLE_COLOR,
        )
        Spacer(Modifier.height(20.dp))

        // Classic Mode (always visible, at top) — mutually exclusive with Minimal Mode
        SettingsCard {
            SettingsToggleRow(
                title = stringResource(R.string.settings_classic_mode_title),
                checked = prefs.classicMode,
                onCheckedChange = { on ->
                    vm.setClassicMode(on)
                    if (on) vm.setSimpleModeEnabled(false)
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Minimal Mode toggle — mutually exclusive with Classic Mode
        SettingsCard {
            SettingsToggleRow(
                title = stringResource(R.string.simple_mode_enable),
                checked = prefs.simpleModeEnabled,
                onCheckedChange = { on ->
                    vm.setSimpleModeEnabled(on)
                    if (on) vm.setClassicMode(false)
                },
            )
        }

        // Sub-options: only shown when Minimal Mode is on
        if (prefs.simpleModeEnabled) {
            Spacer(Modifier.height(12.dp))

            // Layout
            SettingsCard {
                SettingsLabelRow(title = stringResource(R.string.simple_mode_layout_title))
                SegmentedChoice(
                    options = listOf(
                        stringResource(R.string.simple_mode_layout_list) to SimpleModeLayout.LIST,
                        stringResource(R.string.simple_mode_layout_grid) to SimpleModeLayout.GRID,
                    ),
                    selected = prefs.simpleModeLayout,
                    onSelect = { vm.setSimpleModeLayout(it) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // Weather + notification summary + greyscale toggles
            SettingsCard {
                SettingsToggleRow(
                    title = stringResource(R.string.simple_mode_show_weather_title),
                    checked = prefs.simpleModeShowWeather,
                    onCheckedChange = { vm.setSimpleModeShowWeather(it) },
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = stringResource(R.string.simple_mode_show_summary_title),
                    checked = prefs.simpleModeShowNotifSummary,
                    onCheckedChange = { vm.setSimpleModeShowNotifSummary(it) },
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Greyscale Mode",
                    checked = prefs.simpleModeGreyscale,
                    onCheckedChange = { vm.setSimpleModeGreyscale(it) },
                )
            }

        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Reusable settings widgets ────────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CARD_BG),
    ) { content() }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TITLE_COLOR,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ACCENT_COLOR,
                uncheckedThumbColor = Color(0xFF9AA0A8),
                uncheckedTrackColor = Color(0xFF3A3F4A),
            ),
        )
    }
}

@Composable
private fun SettingsLabelRow(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = SUBTITLE_COLOR,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun SettingsDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(Color(0xFF1F2937)),
    )
}

@Composable
private fun <T> SegmentedChoice(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
    ) {
        options.forEachIndexed { i, (label, value) ->
            val isSelected = value == selected
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) ACCENT_COLOR else Color(0xFF6B7280),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFF1A2A3A) else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (i < options.size - 1) Spacer(Modifier.width(4.dp))
        }
    }
}
