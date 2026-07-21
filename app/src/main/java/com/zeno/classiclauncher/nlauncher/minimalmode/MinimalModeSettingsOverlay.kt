@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.zeno.classiclauncher.nlauncher.minimalmode

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.zeno.classiclauncher.nlauncher.ui.isDpadDown
import com.zeno.classiclauncher.nlauncher.ui.isDpadEnter
import com.zeno.classiclauncher.nlauncher.ui.isDpadUp
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
private val CARD_FOCUSED_BG = Color(0xFF1E2A3E)
private val CARD_FOCUSED_BORDER = Color(0x6684D5F6)
private val TITLE_COLOR = Color(0xFFE8EEF7)
private val ACCENT_COLOR = Color(0xFF4A90D9)
private val CARD_SHAPE = RoundedCornerShape(12.dp)

@Composable
internal fun MinimalModeSettingsOverlay(
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fr = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(0) }

    val itemCount = if (prefs.minimalModeEnabled) 5 else 3
    val bringers = remember { List(5) { BringIntoViewRequester() } }
    LaunchedEffect(focusedIndex) { bringers[focusedIndex].bringIntoView() }

    val authAndSwitch = remember(context) { { onAuthenticated: () -> Unit ->
        val activity = context as? FragmentActivity
        if (activity == null) {
            onAuthenticated()
            return@remember
        }
        val canAuth = BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
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

    fun activate(index: Int) {
        when (index) {
            0 -> {
                val zenoModeOn = !prefs.classicMode && !prefs.minimalModeEnabled
                if (!zenoModeOn) {
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
            }
            1 -> {
                val on = !prefs.classicMode
                if (on && prefs.minimalModeEnabled) {
                    authAndSwitch {
                        vm.setClassicMode(true)
                        vm.setMinimalModeEnabled(false)
                    }
                } else {
                    vm.setClassicMode(on)
                    if (on) vm.setMinimalModeEnabled(false)
                }
            }
            2 -> {
                // Turning Minimal Mode OFF from this row would exit with no auth check at all —
                // only the (auth-gated) Zeno/Classic rows above are allowed to leave Minimal
                // Mode. Tapping this row while already in Minimal Mode is a no-op.
                if (!prefs.minimalModeEnabled) {
                    vm.setMinimalModeEnabled(true)
                    vm.setClassicMode(false)
                }
            }
            3 -> vm.setMinimalModeShowWeather(!prefs.minimalModeShowWeather)
            // Notification summary is always on now — no user-facing toggle, so no case 4 here.
            4 -> vm.setMinimalModeGreyscale(!prefs.minimalModeGreyscale)
        }
    }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(fr)
            .focusable()
            .onPreviewKeyEvent { ev ->
                when {
                    ev.isDpadUp    -> { focusedIndex = (focusedIndex - 1).coerceAtLeast(0); true }
                    ev.isDpadDown  -> { focusedIndex = (focusedIndex + 1).coerceAtMost(itemCount - 1); true }
                    ev.isDpadEnter -> { activate(focusedIndex); true }
                    else           -> false
                }
            },
        color = SETTINGS_BG,
    ) {
        LaunchedEffect(Unit) { fr.requestFocus() }
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    contentDescription = stringResource(R.string.action_back),
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

            val zenoModeOn = !prefs.classicMode && !prefs.minimalModeEnabled
            SettingsCard(focused = focusedIndex == 0, bringer = bringers[0]) {
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

            SettingsCard(focused = focusedIndex == 1, bringer = bringers[1]) {
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

            SettingsCard(focused = focusedIndex == 2, bringer = bringers[2]) {
                SettingsToggleRow(
                    title = stringResource(R.string.minimal_mode_enable),
                    checked = prefs.minimalModeEnabled,
                    onCheckedChange = { on ->
                        // Turning Minimal Mode OFF from this row would exit with no auth check
                        // at all — only the (auth-gated) Zeno/Classic rows above are allowed to
                        // leave Minimal Mode. Tapping this row while already in Minimal Mode
                        // (on == false) is a no-op.
                        if (on) {
                            vm.setMinimalModeEnabled(true)
                            vm.setClassicMode(false)
                        }
                    },
                )
            }

            if (prefs.minimalModeEnabled) {
                Spacer(Modifier.height(12.dp))

                SettingsCard(
                    focused = focusedIndex == 3,
                    bringer = bringers[3],
                ) {
                    SettingsToggleRow(
                        title = stringResource(R.string.minimal_mode_show_weather_title),
                        checked = prefs.minimalModeShowWeather,
                        focused = focusedIndex == 3,
                        onCheckedChange = { vm.setMinimalModeShowWeather(it) },
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Notification summary has no toggle — it's always on (see MinimalModeScreen.kt,
                // where the unreadApps gate on this pref was removed entirely).
                SettingsCard(
                    focused = focusedIndex == 4,
                    bringer = bringers[4],
                ) {
                    SettingsToggleRow(
                        title = stringResource(R.string.minimal_mode_greyscale_title),
                        checked = prefs.minimalModeGreyscale,
                        focused = focusedIndex == 4,
                        onCheckedChange = { vm.setMinimalModeGreyscale(it) },
                    )
                }
            }
            // Zeno Status Bar's toggle now lives in the main Settings menu (Display section),
            // shown only when Zeno Mode is active — moved out of this Modes screen entirely.

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Reusable settings widgets ────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    focused: Boolean = false,
    bringer: BringIntoViewRequester? = null,
    content: @Composable () -> Unit,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .clip(CARD_SHAPE)
        .then(if (bringer != null) Modifier.bringIntoViewRequester(bringer) else Modifier)
        .background(if (focused) CARD_FOCUSED_BG else CARD_BG)
        .then(
            if (focused) Modifier.border(1.dp, CARD_FOCUSED_BORDER, CARD_SHAPE) else Modifier,
        )
    Column(modifier = modifier) { content() }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    focused: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (focused) Modifier.background(CARD_FOCUSED_BG) else Modifier,
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (focused) Color(0xFF84D5F6) else TITLE_COLOR,
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
