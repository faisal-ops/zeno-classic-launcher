package com.zeno.classiclauncher.nlauncher.minimalmode

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions
import com.zeno.classiclauncher.nlauncher.apps.SoundProfileMode
import com.zeno.classiclauncher.nlauncher.apps.ToggleResult
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun MinimalModeQsOverlay(
    topBarBottomPx: Int,
    qrScannerPackage: String,
    allApps: List<AppEntry>,
    onSetQrScannerPackage: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val actions = remember(context) { LauncherActions(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val dateFormatter = remember {
        // Locale-correct field order (e.g. ko "M월 d일 EEEE") — never hardcode "MMMM d".
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd")
        SimpleDateFormat(pattern, Locale.getDefault())
    }
    var dateText by remember { mutableStateOf(dateFormatter.format(Date())) }

    var showQrPicker by remember { mutableStateOf(false) }

    val btAdapter = remember {
        (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
    }
    var wifiOn       by remember { mutableStateOf(actions.isWifiEnabled() == true) }
    var mobileDataOn by remember { mutableStateOf(actions.isMobileDataEnabled() == true) }
    var torchOn      by remember { mutableStateOf(actions.isTorchEnabled()) }
    var hotspotOn    by remember { mutableStateOf(actions.isHotspotEnabled()) }
    var bluetoothOn  by remember { mutableStateOf(runCatching { btAdapter?.isEnabled == true }.getOrDefault(false)) }
    var soundProfile by remember { mutableStateOf(actions.currentSoundProfile()) }
    var wifiSubtitle by remember { mutableStateOf(actions.currentWifiSsidLabel()) }
    var carrierName  by remember { mutableStateOf(actions.currentCarrierName()) }
    // Timestamp of the last user-initiated sound profile change. refresh() skips the
    // soundProfile re-read for 2 s to prevent Android's async DND restoration (which
    // reverts the ringer to pre-DND mode) from racing with our ON_RESUME refresh.
    var soundProfileSetAt by remember { mutableLongStateOf(0L) }

    fun refresh() {
        wifiOn       = actions.isWifiEnabled() == true
        mobileDataOn = actions.isMobileDataEnabled() == true
        torchOn      = actions.isTorchEnabled()
        hotspotOn    = actions.isHotspotEnabled()
        bluetoothOn  = runCatching { btAdapter?.isEnabled == true }.getOrDefault(false)
        if (android.os.SystemClock.elapsedRealtime() - soundProfileSetAt > 2_000L) {
            soundProfile = actions.currentSoundProfile()
        }
        wifiSubtitle = actions.currentWifiSsidLabel()
        carrierName  = actions.currentCarrierName()
        dateText     = dateFormatter.format(Date())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val strRing       = stringResource(R.string.sound_profile_ring)
    val strVibrate    = stringResource(R.string.sound_profile_vibrate)
    val strDnd        = stringResource(R.string.quick_settings_dnd)
    val strOn         = stringResource(R.string.settings_on)
    val strOff        = stringResource(R.string.settings_off)
    val soundSubtitle = when (soundProfile) {
        SoundProfileMode.RING    -> strRing
        SoundProfileMode.VIBRATE -> strVibrate
        SoundProfileMode.DND     -> strDnd
    }
    val soundIcon = when (soundProfile) {
        SoundProfileMode.RING    -> Icons.Rounded.Notifications
        SoundProfileMode.VIBRATE -> Icons.Rounded.Vibration
        SoundProfileMode.DND     -> Icons.Rounded.NotificationsOff
    }
    val strSettings     = stringResource(R.string.quick_settings_system_settings)
    val strMobileNet    = stringResource(R.string.quick_settings_mobile_network)
    val strWifi         = stringResource(R.string.quick_settings_wifi)
    val strFlashlight   = stringResource(R.string.quick_settings_torch)
    val strNotifications = stringResource(R.string.quick_settings_notifications_tile)
    val strBluetooth    = stringResource(R.string.quick_settings_bluetooth)
    val strQrScanner    = stringResource(R.string.quick_settings_qr_tile)
    val strHotspot      = stringResource(R.string.quick_settings_hotspot)
    val tiles = listOf(
        SmQsTile("settings",     Icons.Rounded.Settings,       strSettings,      "",            active = false) { actions.openSystemSettings() },
        SmQsTile("mobile_data",  Icons.Rounded.CellTower,      strMobileNet,     carrierName,   active = mobileDataOn) { actions.openMobileNetworkSettings(); refresh() },
        SmQsTile("wifi",         Icons.Rounded.Wifi,           strWifi,          wifiSubtitle,  active = wifiOn) { actions.openInternetPanel(); refresh() },
        SmQsTile("torch",        Icons.Rounded.Lightbulb,      strFlashlight,    "",            active = torchOn) {
            when (val r = actions.toggleTorch()) {
                is ToggleResult.Changed -> torchOn = r.enabled
                else -> Toast.makeText(context, context.getString(R.string.quick_settings_torch_toggle_failed), Toast.LENGTH_SHORT).show()
            }
        },
        SmQsTile("notifications", soundIcon,                   strNotifications, soundSubtitle, active = soundProfile == SoundProfileMode.RING) {
            val next = when (soundProfile) {
                SoundProfileMode.RING    -> SoundProfileMode.VIBRATE
                SoundProfileMode.VIBRATE -> SoundProfileMode.DND
                SoundProfileMode.DND     -> SoundProfileMode.RING
            }
            val ok = actions.applySoundProfile(next)
            if (ok) {
                soundProfile = next
                soundProfileSetAt = android.os.SystemClock.elapsedRealtime()
            } else if (next == SoundProfileMode.DND && !actions.hasDoNotDisturbAccess()) {
                actions.openDoNotDisturbSettings()
            }
        },
        SmQsTile("bluetooth",    Icons.Rounded.Bluetooth,      strBluetooth,     if (bluetoothOn) strOn else strOff, active = bluetoothOn) {
            val enabling = !bluetoothOn
            bluetoothOn = enabling
            scope.launch {
                runCatching {
                    val arg = if (enabling) "enable" else "disable"
                    val exit = withContext(Dispatchers.IO) {
                        ProcessBuilder("cmd", "bluetooth_manager", arg).start().waitFor()
                    }
                    if (exit != 0) {
                        bluetoothOn = !enabling
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }.onFailure {
                    bluetoothOn = !enabling
                }
                delay(1200L)
                bluetoothOn = runCatching { btAdapter?.isEnabled == true }.getOrDefault(enabling)
            }
        },
        SmQsTile("qr",           Icons.Outlined.QrCodeScanner, strQrScanner,     "",            active = false) {
            val pkg = qrScannerPackage.trim()
            val launched = if (pkg.isNotEmpty()) actions.launchApp(pkg) else actions.openQrScanner()
            if (!launched) showQrPicker = true
        },
        SmQsTile("hotspot",      Icons.Rounded.WifiTethering,  strHotspot,       "",            active = hotspotOn) { actions.openHotspotSettings(); refresh() },
    )

    BackHandler(enabled = showQrPicker, onBack = { showQrPicker = false })
    BackHandler(enabled = !showQrPicker, onBack = onDismiss)

    if (showQrPicker) {
        SmQrAppPicker(
            apps = allApps.filter { !it.internal },
            onSelect = { pkg ->
                onSetQrScannerPackage(pkg)
                showQrPicker = false
                actions.launchApp(pkg)
            },
            onDismiss = { showQrPicker = false },
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Content column — transparent spacer on top, dark scrim for everything below it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            // Transparent spacer — real top bar from main screen shows through unobstructed
            Spacer(modifier = Modifier.height(with(density) { topBarBottomPx.toDp() }))

            // Dark scrim fills the rest of the screen (not just the tiles' own intrinsic
            // height) — otherwise the home screen's app list shows through, undimmed, in the
            // gap below the last tile row.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF000000)),
            ) {

            // Date row + gear icon + divider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFFE6EBF2),
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                            lineHeight = 18.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_minimal_mode_settings),
                        tint = Color(0x66FFFFFF),
                        modifier = Modifier
                            .size(22.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onOpenSettings() })
                            },
                    )
                }
                HorizontalDivider(
                    color = Color(0xFF47515D),
                    thickness = 1.dp,
                )
            }

            // Tile grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                userScrollEnabled = false,
            ) {
                items(tiles, key = { it.id }) { tile ->
                    SmQsTileCard(tile = tile)
                }
            }
            } // end dark background Column
        }
    }
}

@Composable
private fun SmQrAppPicker(
    apps: List<AppEntry>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val bg = Color(0xFF0F1318)
    val cardBg = Color(0xFF191D22)
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps.sortedBy { it.label }
        else apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }.sortedBy { it.label }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.IconButton(onClick = onDismiss) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFFE6EBF2),
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.qr_scanner_picker_title), color = Color(0xFFE6EBF2), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.qr_scanner_picker_subtitle), color = Color(0xFF7A8899), fontSize = 13.sp)
            }
        }
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_apps_ellipsis), color = Color(0xFF7A8899), fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE6EBF2),
                unfocusedTextColor = Color(0xFFE6EBF2),
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = Color(0xFF00B7FF),
                unfocusedBorderColor = Color(0xFF2E3A4A),
            ),
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(filtered, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardBg)
                        .clickable { onSelect(app.packageName) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app.label, color = Color(0xFFE6EBF2), fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class SmQsTile(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val active: Boolean,
    val onTap: () -> Unit,
)

@Composable
private fun SmQsTileCard(tile: SmQsTile) {
    val darkCell  = Color(0xFF191D22)
    val activeBg  = Color(0xFF145A77)
    val tileBg    = if (tile.active) activeBg else darkCell
    val iconBoxBg = if (tile.active) Color(0x33000000) else Color(0xFF1A2530)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(tileBg)
            .pointerInput(tile.id) {
                detectTapGestures(onTap = { tile.onTap() })
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(iconBoxBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = Color(0xFFEAF0F6),
                modifier = Modifier.size(32.dp),
            )
            if (tile.active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF00B7FF)),
                )
            }
        }

        // Text
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = tile.title,
                color = Color(0xFFEAF0F6),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tile.subtitle.isNotBlank()) {
                Text(
                    text = tile.subtitle,
                    color = Color(0xFFAEB8C5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
