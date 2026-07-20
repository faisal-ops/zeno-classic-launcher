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
    val contacts: Boolean,
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
        contacts = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        searchOverlayAccessibility = SearchOverlayPermissions.isAccessibilityServiceEnabled(context),
    )

/**
 * Lists optional launcher features that need system access. Each row can be **turned off** so the
 * permission is not required, or left on with **Grant** to satisfy the requirement.
 */
@Composable
fun PermissionsSettingsOverlay(
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var runtime by remember { mutableStateOf(computeRuntimePerms(context)) }
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

    val contactsLauncher = rememberLauncherForActivityResult(
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
                        // Every row here is now a permission status/grant link (no feature
                        // toggles) — Enter opens the same exact permission screen tapping the
                        // row would, only when that permission isn't already granted.
                        when (focusedItem) {
                            0 -> if (!runtime.notificationAccess) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                            1 -> if (!runtime.lockAccessibility) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                            2 -> if (!runtime.location) {
                                locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                            3 -> if (!runtime.calendar) {
                                calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                            4 -> if (!runtime.contacts) {
                                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                            5 -> if (!runtime.searchOverlayAccessibility) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
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

                // Notification badges: the actual on/off toggle lives in Icon Layout's app icon
                // badge setting now — this row is just the underlying Notification Access grant.
                PermissionSwitchCard(
                    title = stringResource(R.string.perm_badges_title),
                    subtitleOff = stringResource(R.string.perm_badges_off),
                    subtitleOnOk = stringResource(R.string.perm_badges_on_ok),
                    subtitleOnMissing = stringResource(R.string.perm_badges_on_missing),
                    featureOn = true,
                    permissionOk = runtime.notificationAccess,
                    focused = focusedItem == 0,
                    themePalette = themePalette,
                    onFeatureChange = {},
                    showSwitch = false,
                    showGrant = !runtime.notificationAccess,
                    onGrant = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    grantLabel = stringResource(R.string.perm_badges_grant_label),
                    openSystemSettingsWhenTurningOff = null,
                )

                // Double Tap to Lock's own on/off toggle lives in Home Gestures now — this row is
                // just the optional Accessibility helper grant (built-in path works without it).
                PermissionSwitchCard(
                    title = stringResource(R.string.perm_lock_title),
                    subtitleOff = stringResource(R.string.perm_lock_off),
                    subtitleOnOk = stringResource(R.string.perm_lock_on_helper),
                    subtitleOnMissing = stringResource(R.string.perm_lock_on_builtin),
                    featureOn = true,
                    permissionOk = runtime.lockAccessibility,
                    focused = focusedItem == 1,
                    themePalette = themePalette,
                    onFeatureChange = {},
                    showSwitch = false,
                    showGrant = !runtime.lockAccessibility,
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

                // Glance Strip's own on/off toggle lives in its own Settings screen now — this
                // row is just the underlying Location permission grant.
                PermissionSwitchCard(
                    title = stringResource(R.string.perm_glance_title),
                    subtitleOff = stringResource(R.string.perm_glance_off),
                    subtitleOnOk = stringResource(R.string.perm_glance_on_ok),
                    subtitleOnMissing = stringResource(R.string.perm_glance_on_missing),
                    featureOn = true,
                    permissionOk = runtime.location,
                    focused = focusedItem == 2,
                    themePalette = themePalette,
                    onFeatureChange = {},
                    showSwitch = false,
                    showGrant = !runtime.location,
                    onGrant = {
                        locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    grantLabel = stringResource(R.string.perm_glance_grant_label),
                    openSystemSettingsWhenTurningOff = null,
                )

                // Calendar-on-glance's own on/off toggle lives in the Glance Strip screen now —
                // this row is just the underlying Calendar permission grant.
                PermissionSwitchCard(
                    title = stringResource(R.string.perm_calendar_title),
                    subtitleOff = stringResource(R.string.perm_calendar_off),
                    subtitleOnOk = stringResource(R.string.perm_calendar_on_ok),
                    subtitleOnMissing = stringResource(R.string.perm_calendar_on_missing),
                    featureOn = true,
                    permissionOk = runtime.calendar,
                    focused = focusedItem == 3,
                    themePalette = themePalette,
                    onFeatureChange = {},
                    showSwitch = false,
                    showGrant = !runtime.calendar,
                    onGrant = {
                        calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    grantLabel = stringResource(R.string.perm_calendar_grant_label),
                    openSystemSettingsWhenTurningOff = null,
                )

                // Universal Search's Contacts results — this is just the underlying READ_CONTACTS
                // grant; there's no separate feature toggle since contacts search is always on
                // once granted (same as apps/settings search).
                PermissionSwitchCard(
                    title = stringResource(R.string.perm_contacts_title),
                    subtitleOff = stringResource(R.string.perm_contacts_off),
                    subtitleOnOk = stringResource(R.string.perm_contacts_on_ok),
                    subtitleOnMissing = stringResource(R.string.perm_contacts_on_missing),
                    featureOn = true,
                    permissionOk = runtime.contacts,
                    focused = focusedItem == 4,
                    themePalette = themePalette,
                    onFeatureChange = {},
                    showSwitch = false,
                    showGrant = !runtime.contacts,
                    onGrant = {
                        contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    grantLabel = stringResource(R.string.perm_contacts_grant_label),
                    openSystemSettingsWhenTurningOff = null,
                )

                // Quick Switch (Any App)'s feature toggle lives in its own screen (main Settings
                // menu) now — this is just the underlying Accessibility Service grant, since
                // that's a genuine OS permission and belongs here like the others.
                PermissionSwitchCard(
                    title = stringResource(R.string.perm_search_overlay_title),
                    subtitleOff = stringResource(R.string.perm_search_overlay_accessibility_granted),
                    subtitleOnOk = stringResource(R.string.perm_search_overlay_accessibility_granted),
                    subtitleOnMissing = stringResource(R.string.perm_search_overlay_accessibility_not_granted),
                    featureOn = true,
                    permissionOk = runtime.searchOverlayAccessibility,
                    focused = focusedItem == 5,
                    themePalette = themePalette,
                    onFeatureChange = {},
                    showSwitch = false,
                    showGrant = !runtime.searchOverlayAccessibility,
                    onGrant = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    grantLabel = stringResource(R.string.perm_search_overlay_grant_label),
                    openSystemSettingsWhenTurningOff = null,
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Standalone screen for the Auto Unlock toggle — moved out of Permissions into the main Settings menu. */
@Composable
fun AutoUnlockSettingsOverlay(
    autoUnlockEnabled: Boolean,
    autoUnlockPinDigits: Int,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onAutoUnlockEnabled: (Boolean) -> Unit,
    onAutoUnlockPinDigits: (Int) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

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
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                if (enter) {
                    onAutoUnlockEnabled(!autoUnlockEnabled)
                    true
                } else {
                    false
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
                    stringResource(R.string.perm_auto_unlock_title),
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
                PermissionSwitchCard(
                    title = stringResource(R.string.perm_auto_unlock_title),
                    subtitleOff = stringResource(R.string.perm_auto_unlock_off),
                    subtitleOnOk = stringResource(R.string.perm_auto_unlock_on, autoUnlockPinDigits),
                    subtitleOnMissing = stringResource(R.string.perm_auto_unlock_on, autoUnlockPinDigits),
                    featureOn = autoUnlockEnabled,
                    permissionOk = true,
                    focused = true,
                    themePalette = themePalette,
                    onFeatureChange = onAutoUnlockEnabled,
                    showGrant = autoUnlockEnabled,
                    onGrant = {
                        onAutoUnlockPinDigits(if (autoUnlockPinDigits == 4) 6 else 4)
                    },
                    grantLabel = stringResource(
                        if (autoUnlockPinDigits == 4) R.string.perm_auto_unlock_switch_to_6 else R.string.perm_auto_unlock_switch_to_4,
                    ),
                )
            }
        }
    }
}

/** Standalone screen for the Quick Switch (Any App) toggle — moved out of Permissions into the main Settings menu. */
@Composable
fun QuickSwitchSettingsOverlay(
    prefs: LauncherPrefs,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onSearchOverlayEnabled: (Boolean) -> Unit,
    onSearchOverlayCustomKeys: (Int, Int) -> Unit,
) {
    val context = LocalContext.current
    var searchOverlayAccessibilityGranted by remember {
        mutableStateOf(SearchOverlayPermissions.isAccessibilityServiceEnabled(context))
    }
    var showCustomCapture by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                searchOverlayAccessibilityGranted = SearchOverlayPermissions.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val focusRequester = remember { FocusRequester() }

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
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                if (enter) {
                    onSearchOverlayEnabled(!prefs.searchOverlayEnabled)
                    true
                } else {
                    false
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
                    stringResource(R.string.perm_search_overlay_title),
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
                val searchOverlayKeys = searchOverlayKeysLabel(
                    prefs.searchOverlayCustomKeyCode1,
                    prefs.searchOverlayCustomKeyCode2,
                )
                val searchOverlayReady = searchOverlayKeys != null && searchOverlayAccessibilityGranted
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
                    focused = true,
                    themePalette = themePalette,
                    onFeatureChange = onSearchOverlayEnabled,
                    // The Accessibility Service grant itself lives in Permissions now (it's a
                    // genuine OS permission) — this screen only offers the feature toggle and its
                    // own key-combination setting, not a duplicate "Open Accessibility settings" link.
                    showGrant = prefs.searchOverlayEnabled && searchOverlayKeys == null,
                    onGrant = { showCustomCapture = true },
                    grantLabel = stringResource(R.string.perm_search_overlay_set_combination),
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
 * Capture happens inside [com.zeno.classiclauncher.nlauncher.power.LockScreenAccessibilityService], not this composable — bare Alt/Sym
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
    /** When false, this row is a plain OS-permission status/grant row — no feature on/off switch. */
    showSwitch: Boolean = true,
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
    // Permission-only rows (showSwitch = false) are entirely OS-permission status/grant links now
    // — the whole card opens the exact system permission screen, rather than a separate button
    // below a feature toggle.
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (focused) Color(0xFF28303F) else Color(0xFF1E2430),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!showSwitch) Modifier.clickable(onClick = onGrant) else Modifier),
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
                if (showSwitch) {
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
            }
            if (showSwitch) {
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
}
