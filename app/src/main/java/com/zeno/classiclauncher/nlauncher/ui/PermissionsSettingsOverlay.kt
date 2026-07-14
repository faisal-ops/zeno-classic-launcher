package com.zeno.classiclauncher.nlauncher.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zeno.classiclauncher.nlauncher.R
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefs
import com.zeno.classiclauncher.nlauncher.power.SleepManager
import com.zeno.classiclauncher.nlauncher.search.CaptureStatus
import com.zeno.classiclauncher.nlauncher.search.SearchOverlayCaptureState
import com.zeno.classiclauncher.nlauncher.search.SearchOverlayPermissions
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette

/** null when no key has been recorded yet. */
@Composable
private fun searchOverlayKeysLabel(customKey1: Int, customKey2: Int): String? = when {
    customKey1 == 0 -> null
    customKey2 != 0 -> stringResource(
        R.string.perm_search_overlay_gesture_custom_hold,
        keyCodeLabel(customKey1),
        keyCodeLabel(customKey2),
    )
    else -> stringResource(R.string.perm_search_overlay_gesture_custom_tap, keyCodeLabel(customKey1))
}

private fun keyCodeLabel(code: Int): String =
    AndroidKeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")

private data class RuntimePerms(
    val notificationAccess: Boolean,
    val lockAccessibility: Boolean,
    val location: Boolean,
    val calendar: Boolean,
    val searchOverlayAccessibility: Boolean,
)

private fun computeRuntimePerms(context: android.content.Context): RuntimePerms =
    RuntimePerms(
        notificationAccess = isNotificationListenerEnabled(context),
        lockAccessibility = SleepManager.isLockAccessibilityEnabled(context),
        location = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        calendar = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        searchOverlayAccessibility = SearchOverlayPermissions.isAccessibilityServiceEnabled(context),
    )

/**
 * Lists optional launcher features that need system access. Each row can be **turned off** so the
 * permission is not required, or left on with **Grant** to satisfy the requirement.
 */
@Composable
fun PermissionsSettingsOverlay(
    prefs: LauncherPrefs,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onNotificationBadgesEnabled: (Boolean) -> Unit,
    onDoubleTapSleepEnabled: (Boolean) -> Unit,
    onAutoUnlockEnabled: (Boolean) -> Unit,
    onAutoUnlockPinDigits: (Int) -> Unit,
    onGlanceEnabled: (Boolean) -> Unit,
    onGlanceShowCalendar: (Boolean) -> Unit,
    onSearchOverlayEnabled: (Boolean) -> Unit,
    onSearchOverlayCustomKeys: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    var runtime by remember { mutableStateOf(computeRuntimePerms(context)) }
    var showCustomCapture by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                runtime = computeRuntimePerms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        runtime = computeRuntimePerms(context)
    }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        runtime = computeRuntimePerms(context)
    }

    val subtitleMuted = Color(0xFF8E95A3)
    val focusRequester = remember { FocusRequester() }
    var focusedItem by remember { mutableIntStateOf(0) }

    BackHandler(enabled = true, onBack = onDismiss)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(406f)
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
                when {
                    up    -> { focusedItem = (focusedItem - 1).coerceAtLeast(0); true }
                    down  -> { focusedItem = (focusedItem + 1).coerceAtMost(5); true }
                    enter -> {
                        when (focusedItem) {
                            0 -> onNotificationBadgesEnabled(!prefs.notificationBadgesEnabled)
                            1 -> {
                                onDoubleTapSleepEnabled(!prefs.doubleTapToSleepEnabled)
                                runtime = computeRuntimePerms(context)
                            }
                            2 -> onGlanceEnabled(!prefs.glanceEnabled)
                            3 -> onGlanceShowCalendar(!prefs.glanceShowCalendar)
                            5 -> onSearchOverlayEnabled(!prefs.searchOverlayEnabled)
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
                    stringResource(R.string.permissions_title),
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.permissions_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleMuted,
                )

                PermissionSwitchCard(
                    title = stringResource(R.string.perm_badges_title),
                    subtitleOff = stringResource(R.string.perm_badges_off),
                    subtitleOnOk = stringResource(R.string.perm_badges_on_ok),
                    subtitleOnMissing = stringResource(R.string.perm_badges_on_missing),
                    featureOn = prefs.notificationBadgesEnabled,
                    permissionOk = runtime.notificationAccess,
                    focused = focusedItem == 0,
                    themePalette = themePalette,
                    onFeatureChange = onNotificationBadgesEnabled,
                    showGrant = prefs.notificationBadgesEnabled && !runtime.notificationAccess,
                    onGrant = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    grantLabel = stringResource(R.string.perm_badges_grant_label),
                    openSystemSettingsWhenTurningOff =
                        if (prefs.notificationBadgesEnabled && runtime.notificationAccess) {
                            {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.perm_badges_turn_off_toast),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        } else {
                            null
                        },
                )

                PermissionSwitchCard(
                    title = stringResource(R.string.perm_lock_title),
                    subtitleOff = stringResource(R.string.perm_lock_off),
                    subtitleOnOk = when {
                        runtime.lockAccessibility -> stringResource(R.string.perm_lock_on_helper)
                        else -> stringResource(R.string.perm_lock_on_builtin)
                    },
                    subtitleOnMissing = stringResource(R.string.perm_lock_on_builtin),
                    featureOn = prefs.doubleTapToSleepEnabled,
                    permissionOk = true,
                    focused = focusedItem == 1,
                    themePalette = themePalette,
                    onFeatureChange = { on ->
                        onDoubleTapSleepEnabled(on)
                        runtime = computeRuntimePerms(context)
                    },
                    showGrant = false,
                    onGrant = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    grantLabel = stringResource(R.string.perm_lock_grant_label),
                    openSystemSettingsWhenTurningOff = null,
                )

                PermissionSwitchCard(
                    title = stringResource(R.string.perm_auto_unlock_title),
                    subtitleOff = stringResource(R.string.perm_auto_unlock_off),
                    subtitleOnOk = stringResource(R.string.perm_auto_unlock_on, prefs.autoUnlockPinDigits),
                    subtitleOnMissing = stringResource(R.string.perm_auto_unlock_on, prefs.autoUnlockPinDigits),
                    featureOn = prefs.autoUnlockEnabled,
                    permissionOk = true,
                    focused = focusedItem == 2,
                    themePalette = themePalette,
                    onFeatureChange = { on -> onAutoUnlockEnabled(on) },
                    showGrant = prefs.autoUnlockEnabled,
                    onGrant = {
                        onAutoUnlockPinDigits(if (prefs.autoUnlockPinDigits == 4) 6 else 4)
                    },
                    grantLabel = stringResource(
                        if (prefs.autoUnlockPinDigits == 4) R.string.perm_auto_unlock_switch_to_6 else R.string.perm_auto_unlock_switch_to_4,
                    ),
                )

                PermissionSwitchCard(
                    title = stringResource(R.string.perm_glance_title),
                    subtitleOff = stringResource(R.string.perm_glance_off),
                    subtitleOnOk = stringResource(R.string.perm_glance_on_ok),
                    subtitleOnMissing = stringResource(R.string.perm_glance_on_missing),
                    featureOn = prefs.glanceEnabled,
                    permissionOk = runtime.location,
                    focused = focusedItem == 3,
                    themePalette = themePalette,
                    onFeatureChange = onGlanceEnabled,
                    showGrant = prefs.glanceEnabled && !runtime.location,
                    onGrant = {
                        locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    grantLabel = stringResource(R.string.perm_glance_grant_label),
                    openSystemSettingsWhenTurningOff =
                        if (prefs.glanceEnabled && runtime.location) {
                            {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        },
                                    )
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.perm_glance_turn_off_toast),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        } else {
                            null
                        },
                )

                PermissionSwitchCard(
                    title = stringResource(R.string.perm_calendar_title),
                    subtitleOff = stringResource(R.string.perm_calendar_off),
                    subtitleOnOk = stringResource(R.string.perm_calendar_on_ok),
                    subtitleOnMissing = when {
                        !prefs.glanceEnabled -> stringResource(R.string.perm_calendar_on_missing_glance_off)
                        else -> stringResource(R.string.perm_calendar_on_missing)
                    },
                    featureOn = prefs.glanceShowCalendar,
                    permissionOk = runtime.calendar,
                    focused = focusedItem == 4,
                    themePalette = themePalette,
                    enabled = true,
                    onFeatureChange = onGlanceShowCalendar,
                    showGrant = prefs.glanceEnabled && prefs.glanceShowCalendar && !runtime.calendar,
                    onGrant = {
                        calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    grantLabel = stringResource(R.string.perm_calendar_grant_label),
                    openSystemSettingsWhenTurningOff =
                        if (prefs.glanceEnabled && prefs.glanceShowCalendar && runtime.calendar) {
                            {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        },
                                    )
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.perm_calendar_turn_off_toast),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        } else {
                            null
                        },
                )

                val searchOverlayKeys = searchOverlayKeysLabel(
                    prefs.searchOverlayCustomKeyCode1,
                    prefs.searchOverlayCustomKeyCode2,
                )
                val searchOverlayReady = searchOverlayKeys != null && runtime.searchOverlayAccessibility
                PermissionSwitchCard(
                    title = stringResource(R.string.perm_search_overlay_title),
                    subtitleOff = stringResource(R.string.perm_search_overlay_off),
                    subtitleOnOk = stringResource(R.string.perm_search_overlay_on_ok, searchOverlayKeys.orEmpty()),
                    subtitleOnMissing = if (searchOverlayKeys == null) {
                        stringResource(R.string.perm_search_overlay_on_ok_unset)
                    } else {
                        stringResource(R.string.perm_search_overlay_on_missing, searchOverlayKeys)
                    },
                    featureOn = prefs.searchOverlayEnabled,
                    permissionOk = searchOverlayReady,
                    focused = focusedItem == 5,
                    themePalette = themePalette,
                    onFeatureChange = onSearchOverlayEnabled,
                    showGrant = prefs.searchOverlayEnabled && !searchOverlayReady,
                    onGrant = {
                        if (searchOverlayKeys == null) {
                            showCustomCapture = true
                        } else {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        }
                    },
                    grantLabel = if (searchOverlayKeys == null) {
                        stringResource(R.string.perm_search_overlay_set_combination)
                    } else {
                        stringResource(R.string.perm_search_overlay_grant_label)
                    },
                    openSystemSettingsWhenTurningOff = null,
                )
                if (prefs.searchOverlayEnabled && searchOverlayKeys != null) {
                    TextButton(
                        onClick = { showCustomCapture = true },
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    ) {
                        Text(stringResource(R.string.perm_search_overlay_change_combination), color = themePalette.settingsMenuBody)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showCustomCapture) {
        SearchOverlayCustomCaptureDialog(
            onCaptured = { key1, key2 ->
                onSearchOverlayCustomKeys(key1, key2)
                showCustomCapture = false
            },
            onDismiss = { showCustomCapture = false },
        )
    }
}

/**
 * Capture happens inside [SearchOverlayAccessibilityService], not this composable — bare Alt/Sym
 * key-down events aren't reliably delivered to a normal focused View's key dispatch on this
 * hardware (they're consumed as modifier state before reaching app-level input handling), but
 * the accessibility service's key filter reliably sees them, since that's the same path the
 * runtime trigger detection already relies on. This dialog just starts/stops recording and
 * reflects [SearchOverlayCaptureState] back to the user.
 */
@Composable
private fun SearchOverlayCustomCaptureDialog(
    onCaptured: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val status by SearchOverlayCaptureState.status.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        SearchOverlayCaptureState.start()
        onDispose { SearchOverlayCaptureState.stop() }
    }

    LaunchedEffect(status) {
        val captured = status as? CaptureStatus.Captured ?: return@LaunchedEffect
        onCaptured(captured.keyCode1, captured.keyCode2)
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(500f)
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2430),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.search_overlay_capture_title),
                    color = Color(0xFFEAF2F8),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    when (val s = status) {
                        is CaptureStatus.WaitingForSecondTap ->
                            stringResource(R.string.search_overlay_capture_waiting_second_tap, keyCodeLabel(s.keyCode))
                        is CaptureStatus.Captured, CaptureStatus.Idle ->
                            stringResource(R.string.search_overlay_capture_instructions)
                        CaptureStatus.Listening ->
                            stringResource(R.string.search_overlay_capture_instructions)
                    },
                    color = Color(0xFF9EADB8),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = Color(0xFF9EADB8))
                }
            }
        }
    }
}

@Composable
private fun PermissionSwitchCard(
    title: String,
    subtitleOff: String,
    subtitleOnOk: String,
    subtitleOnMissing: String,
    featureOn: Boolean,
    permissionOk: Boolean,
    focused: Boolean = false,
    themePalette: LauncherThemePalette,
    enabled: Boolean = true,
    onFeatureChange: (Boolean) -> Unit,
    showGrant: Boolean,
    onGrant: () -> Unit,
    grantLabel: String,
    secondaryGrantLabel: String? = null,
    onSecondaryGrant: (() -> Unit)? = null,
    /** When non-null and the user turns the switch off while the feature was on, run before updating prefs (e.g. open system settings to revoke access). */
    openSystemSettingsWhenTurningOff: (() -> Unit)? = null,
) {
    val sub = when {
        !featureOn -> subtitleOff
        permissionOk -> subtitleOnOk
        else -> subtitleOnMissing
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (focused) Color(0xFF28303F) else Color(0xFF1E2430),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (enabled) themePalette.settingsMenuTitle else Color(0xFF8E95A3),
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (!featureOn) Color(0xFF8E95A3) else if (featureOn && !permissionOk && enabled) Color(0xFFFFB4A8) else Color(0xFF8E95A3),
                            fontSize = 13.sp,
                        ),
                    )
                }
                Switch(
                    checked = featureOn,
                    onCheckedChange = if (enabled) {
                        { checked ->
                            if (!checked && featureOn && openSystemSettingsWhenTurningOff != null) {
                                openSystemSettingsWhenTurningOff()
                            }
                            onFeatureChange(checked)
                        }
                    } else {
                        null
                    },
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4A90D9),
                        uncheckedThumbColor = Color(0xFF9AA0A8),
                        uncheckedTrackColor = Color(0xFF3A3F4A),
                    ),
                )
            }
            if (showGrant && grantLabel.isNotEmpty()) {
                TextButton(onClick = onGrant, modifier = Modifier.padding(top = 4.dp)) {
                    Text(grantLabel, color = themePalette.settingsMenuBody)
                }
            }
            if (showGrant && secondaryGrantLabel != null && onSecondaryGrant != null) {
                TextButton(onClick = onSecondaryGrant, modifier = Modifier.padding(top = 0.dp)) {
                    Text(secondaryGrantLabel, color = themePalette.settingsMenuBody)
                }
            }
        }
    }
}
