package com.zeno.classiclauncher.nlauncher.minimalmode

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.ui.LauncherViewModel

private val SETTINGS_BG = Color(0xFF0E131B)
private val CARD_BG = Color(0xFF161D2A)
private val TITLE_COLOR = Color(0xFFE8EEF7)
private val ACCENT_COLOR = Color(0xFF4A90D9)

@Composable
internal fun MinimalModeSettingsOverlay(
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val authAndSwitch = remember(context) { { onAuthenticated: () -> Unit ->
        val activity = context as? FragmentActivity
        if (activity == null) {
            onAuthenticated()
            return@remember
        }
        val canAuth = BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // No lock screen set up — switch without auth
            onAuthenticated()
            return@remember
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthenticated()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Switch mode")
            .setSubtitle("Verify to exit Minimal Mode")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }}

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
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = TITLE_COLOR,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onDismiss() },
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.minimal_mode_settings_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TITLE_COLOR,
            )
        }
        Spacer(Modifier.height(20.dp))

        // Zeno Mode — default mode; on when neither Classic nor Minimal is active
        val zenoModeOn = !prefs.classicMode && !prefs.minimalModeEnabled
        SettingsCard {
            SettingsToggleRow(
                title = stringResource(R.string.settings_zeno_mode_title),
                checked = zenoModeOn,
                onCheckedChange = { on ->
                    if (on) {
                        if (prefs.minimalModeEnabled) {
                            authAndSwitch {
                                vm.setClassicMode(false)
                                vm.setMinimalModeEnabled(false)
                            }
                        } else {
                            vm.setClassicMode(false)
                            vm.setMinimalModeEnabled(false)
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Classic Mode — mutually exclusive with Zeno and Minimal
        SettingsCard {
            SettingsToggleRow(
                title = stringResource(R.string.settings_classic_mode_title),
                checked = prefs.classicMode,
                onCheckedChange = { on ->
                    if (on && prefs.minimalModeEnabled) {
                        authAndSwitch {
                            vm.setClassicMode(true)
                            vm.setMinimalModeEnabled(false)
                        }
                    } else {
                        vm.setClassicMode(on)
                        if (on) vm.setMinimalModeEnabled(false)
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Minimal Mode toggle — mutually exclusive with Classic and Zeno
        SettingsCard {
            SettingsToggleRow(
                title = stringResource(R.string.minimal_mode_enable),
                checked = prefs.minimalModeEnabled,
                onCheckedChange = { on ->
                    vm.setMinimalModeEnabled(on)
                    if (on) vm.setClassicMode(false)
                },
            )
        }

        // Sub-options: only shown when Minimal Mode is on
        if (prefs.minimalModeEnabled) {
            Spacer(Modifier.height(12.dp))

            SettingsCard {
                SettingsToggleRow(
                    title = stringResource(R.string.minimal_mode_show_weather_title),
                    checked = prefs.minimalModeShowWeather,
                    onCheckedChange = { vm.setMinimalModeShowWeather(it) },
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = stringResource(R.string.minimal_mode_show_summary_title),
                    checked = prefs.minimalModeShowNotifSummary,
                    onCheckedChange = { vm.setMinimalModeShowNotifSummary(it) },
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = stringResource(R.string.minimal_mode_greyscale_title),
                    checked = prefs.minimalModeGreyscale,
                    onCheckedChange = { vm.setMinimalModeGreyscale(it) },
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
private fun SettingsDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(Color(0xFF1F2937)),
    )
}
