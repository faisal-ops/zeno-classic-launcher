package com.zeno.classiclauncher.nlauncher.minimalmode

import android.Manifest
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.location.LocationManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions
import com.zeno.classiclauncher.nlauncher.apps.SoundProfileMode
import com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
import kotlinx.coroutines.delay

/** Everything [ZenoStatusBar] needs to render, gathered from platform receivers rather than
 *  polling — see [rememberZenoStatusBarState]. */
internal data class ZenoStatusBarState(
    val time: String,
    val amPm: String,
    val batteryPct: Int?,
    val batteryCharging: Boolean,
    /** Wi-Fi is the active/default network right now — i.e. actually carrying internet
     *  traffic, not merely "adapter switched on". */
    val wifiConnected: Boolean,
    /** Wi-Fi radio is switched on (independent of whether it's the active transport). */
    val wifiEnabled: Boolean,
    /** Cellular is the active/default network right now — mobile data is carrying traffic. */
    val cellularActive: Boolean,
    val soundProfile: SoundProfileMode,
    val headsetConnected: Boolean,
    /** Some app is actively pulling a location fix right now (API 29+: AppOps location-in-use
     *  signal, the same one powers the real Android status bar's location dot) — not merely
     *  "location services are switched on" in Settings. Falls back to the services-on reading
     *  below API 29, where no per-app "in use" signal exists. */
    val locationEnabled: Boolean,
    val nfcEnabled: Boolean,
    val bluetoothEnabled: Boolean,
    val carrierName: String,
    val signalLevel: Int?,
    /** Greyscale icons of up to the 3 most recently posted unread apps, paired with their
     *  package name so a tap can launch the right app — empty when there are none. */
    val notifyIcons: List<ZenoStatusBarNotifyIcon>,
    val powerSaveActive: Boolean,
    val hotspotEnabled: Boolean,
    val airplaneModeEnabled: Boolean,
)

internal data class ZenoStatusBarNotifyIcon(val packageName: String, val bitmap: android.graphics.Bitmap)

/**
 * Shared source of truth for [ZenoStatusBar], used by both Minimal Mode and the Zeno/Classic
 * Mode home screen (which share [com.zeno.classiclauncher.nlauncher.ui.LauncherScreen]) so
 * neither duplicates the ~7 platform receivers this pulls together. Each field updates via its
 * own targeted broadcast/callback — no polling.
 */
@Composable
internal fun rememberZenoStatusBarState(): ZenoStatusBarState {
    val context = LocalContext.current
    val actions = remember(context) { LauncherActions(context) }
    val is24h = remember { DateFormat.is24HourFormat(context) }

    var time by remember { mutableStateOf(currentTimeString(is24h)) }
    var amPm by remember { mutableStateOf(if (is24h) "" else currentAmPmString()) }
    var batteryPct by remember { mutableIntStateOf(readBatteryPct(context)) }
    var batteryCharging by remember { mutableStateOf(readBatteryCharging(context)) }
    var powerSaveActive by remember {
        mutableStateOf(context.getSystemService(PowerManager::class.java)?.isPowerSaveMode ?: false)
    }
    var hotspotEnabled by remember { mutableStateOf(actions.isHotspotEnabled()) }
    var airplaneModeEnabled by remember { mutableStateOf(actions.isAirplaneModeEnabled()) }
    var wifiConnected by remember { mutableStateOf(actions.isWifiConnected()) }
    var wifiEnabled by remember { mutableStateOf(actions.isWifiEnabled() ?: false) }
    var cellularActive by remember { mutableStateOf(actions.isCellularActive()) }
    var soundProfile by remember { mutableStateOf(actions.currentSoundProfile()) }
    // Seeded false on API 29+ (no sync "who's using location right now" query exists — the
    // AppOps watcher below reports the real state within one frame); below API 29 the initial
    // read is the same "services enabled" fallback the ongoing effect uses.
    var locationOn by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) false else actions.isLocationEnabled())
    }
    var nfcOn by remember { mutableStateOf(actions.isNfcEnabled()) }
    val notifyPackages by NotificationRepository.recentUnreadPackages.collectAsStateWithLifecycle()
    val notifyIcons = remember(notifyPackages) {
        notifyPackages.mapNotNull { entry ->
            resolveNotifyIconBitmap(context, entry.packageName, entry.smallIcon)
                ?.let { ZenoStatusBarNotifyIcon(entry.packageName, it) }
        }
    }
    var carrierName by remember { mutableStateOf(actions.networkOperatorNameOrEmpty()) }
    var phoneStateGranted by remember { mutableStateOf(hasPhoneStatePermission(context)) }
    val signalLevel = rememberCellSignalLevel(permissionGranted = phoneStateGranted)
    var bluetoothConnectGranted by remember {
        mutableStateOf(
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    var bluetoothOn by remember { mutableStateOf(actions.isBluetoothEnabled() ?: false) }

    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> phoneStateGranted = granted }
    val bluetoothConnectPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        bluetoothConnectGranted = granted
        bluetoothOn = actions.isBluetoothEnabled() ?: false
    }
    LaunchedEffect(Unit) {
        if (!phoneStateGranted) {
            runCatching { phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
        }
        if (!bluetoothConnectGranted) {
            runCatching { bluetoothConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
        }
    }

    fun isHeadsetConnected(audioManager: AudioManager): Boolean {
        val wiredTypes = intArrayOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in wiredTypes }
    }
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    var headsetConnected by remember { mutableStateOf(isHeadsetConnected(audioManager)) }

    // Battery: sticky broadcast returns current state instantly on registration.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                batteryPct = readBatteryPct(context)
                batteryCharging = readBatteryCharging(context)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Battery saver toggle.
    DisposableEffect(Unit) {
        val pm = context.getSystemService(PowerManager::class.java)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                powerSaveActive = pm?.isPowerSaveMode ?: false
            }
        }
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Airplane mode toggle — standard public broadcast.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                airplaneModeEnabled = actions.isAirplaneModeEnabled()
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Hotspot toggle. WIFI_AP_STATE_CHANGED is a hidden platform broadcast (no public constant),
    // so it's registered by its literal action string — same tradeoff already accepted for
    // NFC's ACTION_ADAPTER_STATE_CHANGED handling above. RECEIVER_EXPORTED (not NOT_EXPORTED) for
    // the same reason established there too: on this device/ROM, NOT_EXPORTED silently drops
    // system-sent broadcasts that are documented as exempt from that restriction — confirmed for
    // NFC's broadcast via dumpsys, not independently re-verified here, but the failure mode
    // (receiver registers, broadcast dispatches, onReceive never fires) is silent enough that
    // defaulting to EXPORTED preemptively is safer than risking the same bug unverified.
    // A ContentObserver on the Settings.Global keys isHotspotEnabled() itself reads
    // ("tethering_on"/"hotspot_on") is kept as a fallback for ROMs that flip those without
    // broadcasting, mirroring the NFC nfc_on precedent.
    DisposableEffect(Unit) {
        val refresh = { hotspotEnabled = actions.isHotspotEnabled() }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) = refresh()
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"),
            ContextCompat.RECEIVER_EXPORTED,
        )
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        runCatching {
            resolver.registerContentObserver(Settings.Global.getUriFor("tethering_on"), false, observer)
            resolver.registerContentObserver(Settings.Global.getUriFor("hotspot_on"), false, observer)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }

    // Sound profile: ringer mode + DND filter changes.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                soundProfile = actions.currentSoundProfile()
            }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Location: "in use by some app right now", not "services switched on" — same distinction
    // BB10's arrow-icon makes and the real Android status bar's location dot uses. AppOpsManager's
    // active-op watcher (API 29+) is the only public API for this; it fires per (op, package)
    // start/stop, so a running set of active packages is tracked rather than a single boolean —
    // two concurrent consumers means the icon must survive the first one finishing.
    DisposableEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val appOps = context.getSystemService(AppOpsManager::class.java)
            if (appOps == null) {
                locationOn = false
                return@DisposableEffect onDispose { }
            }
            val activePackages = mutableSetOf<String>()
            val ops = arrayOf(AppOpsManager.OPSTR_FINE_LOCATION, AppOpsManager.OPSTR_COARSE_LOCATION)
            val listener = AppOpsManager.OnOpActiveChangedListener { _, _, packageName, active ->
                if (active) activePackages += packageName else activePackages -= packageName
                locationOn = activePackages.isNotEmpty()
            }
            val registered = runCatching {
                appOps.startWatchingActive(ops, context.mainExecutor, listener)
            }.isSuccess
            onDispose {
                if (registered) runCatching { appOps.stopWatchingActive(listener) }
            }
        } else {
            // No per-app "in use" signal below API 29 — closest available approximation is
            // whether location services are switched on at all.
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    locationOn = actions.isLocationEnabled()
                }
            }
            val filter = IntentFilter(LocationManager.MODE_CHANGED_ACTION)
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            onDispose { runCatching { context.unregisterReceiver(receiver) } }
        }
    }

    // Bluetooth: hidden entirely when the radio is off, mirroring the GPS/NFC/Wi-Fi rule.
    DisposableEffect(bluetoothConnectGranted) {
        if (!bluetoothConnectGranted) {
            bluetoothOn = false
            return@DisposableEffect onDispose { }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                bluetoothOn = actions.isBluetoothEnabled() ?: false
            }
        }
        val filter = IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // NFC toggle. Two non-obvious findings here, both verified on-device — do not "tidy" either
    // one away without re-testing an actual NFC toggle:
    //
    //  1. RECEIVER_EXPORTED is REQUIRED, even though ADAPTER_STATE_CHANGED *is* on this device's
    //     protected-broadcast list (`adb shell dumpsys package protected-broadcasts`) and the
    //     docs imply NOT_EXPORTED is exempt for protected broadcasts. With NOT_EXPORTED the
    //     receiver registers fine and the broadcast is genuinely dispatched by com.android.nfc
    //     (both confirmed via `dumpsys activity broadcasts`), but onReceive is never invoked —
    //     it is silently dropped. Switching to EXPORTED made it fire immediately. Harmless to
    //     export: the payload is ignored and the state is re-read from NfcAdapter regardless of
    //     who sends the intent, so a spoofed broadcast can only trigger a redundant true re-read.
    //
    //  2. The ContentObserver is a fallback for ROMs that flip Settings.Global "nfc_on" without
    //     broadcasting. It is NOT sufficient alone on the Q25: `settings get global nfc_on`
    //     returns null there even while `dumpsys nfc` reports mState=on, so it never fires.
    //
    // isNfcEnabled() reads NfcAdapter.isEnabled first and only falls back to the Settings key, so
    // whichever signal arrives, the value read back is correct.
    DisposableEffect(Unit) {
        val refresh = { nfcOn = actions.isNfcEnabled() }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) = refresh()
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )

        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        runCatching {
            resolver.registerContentObserver(Settings.Global.getUriFor("nfc_on"), false, observer)
        }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }

    // Carrier name: a plain remember{} only ever reads this once at first composition, so
    // turning the SIM back on later (or swapping it) never refreshed the displayed name. Service
    // state changes (SIM inserted/removed, registered/deregistered, airplane mode) are the same
    // signal that already fixed the stuck signal bars in CellSignalState.kt — reuse it here too.
    DisposableEffect(phoneStateGranted) {
        val tm = context.getSystemService(TelephonyManager::class.java)
        if (tm == null || !phoneStateGranted) {
            return@DisposableEffect onDispose { }
        }
        val refresh = { carrierName = actions.networkOperatorNameOrEmpty() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                override fun onServiceStateChanged(serviceState: ServiceState) = refresh()
            }
            val registered = runCatching {
                tm.registerTelephonyCallback(context.mainExecutor, callback)
            }.isSuccess
            onDispose {
                if (registered) runCatching { tm.unregisterTelephonyCallback(callback) }
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                override fun onServiceStateChanged(serviceState: ServiceState) = refresh()
            }
            @Suppress("DEPRECATION")
            val registered = runCatching {
                tm.listen(listener, PhoneStateListener.LISTEN_SERVICE_STATE)
            }.isSuccess
            onDispose {
                if (registered) {
                    @Suppress("DEPRECATION")
                    runCatching { tm.listen(listener, PhoneStateListener.LISTEN_NONE) }
                }
            }
        }
    }

    // Active/default transport (not just the Wi-Fi adapter being switched on): a
    // NetworkCallback fires the instant the default network changes, which the
    // WIFI_STATE_CHANGED broadcast never reports — that only covers the radio toggling.
    // Both wifiConnected and cellularActive derive from "which transport currently carries
    // traffic", per the network-indicator spec. Callbacks are delivered on the main Handler so
    // Compose state writes stay on the UI thread. Needs ACCESS_NETWORK_STATE, already declared.
    DisposableEffect(Unit) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
            ?: return@DisposableEffect onDispose { }
        val callback = object : ConnectivityManager.NetworkCallback() {
            // Read transport off the caps the callback itself hands us — re-querying
            // cm.activeNetwork/getNetworkCapabilities() asynchronously here is a known race:
            // right after onLost fires, activeNetwork can still briefly report the network
            // that was just dropped, so 4GLTE/Wi-Fi stayed stuck on after disabling mobile
            // data or the SIM. onLost has no caps parameter, so it explicitly clears both —
            // there's no other default network to ask about at that point anyway.
            override fun onLost(network: Network) {
                wifiConnected = false
                cellularActive = false
            }
            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities,
            ) {
                // NET_CAPABILITY_VALIDATED is Android's own "this network actually reaches the
                // internet" check. Without it, a cellular network that's still registered but
                // has no data left (plan exhausted) kept reporting TRANSPORT_CELLULAR and 4GLTE
                // stayed lit even though nothing was actually loading.
                val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                wifiConnected = validated && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                cellularActive = validated && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }
        }
        val registered = runCatching {
            cm.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        }.isSuccess
        onDispose {
            if (registered) runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }

    // Wi-Fi radio on/off — independent of whether it's the active transport. Determines
    // whether the Wi-Fi glyph is shown at all (spec: hidden entirely when disabled).
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                wifiEnabled = actions.isWifiEnabled() ?: false
            }
        }
        val filter = IntentFilter(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Wired headset plug/unplug.
    DisposableEffect(Unit) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                headsetConnected = isHeadsetConnected(audioManager)
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                headsetConnected = isHeadsetConnected(audioManager)
            }
        }
        audioManager.registerAudioDeviceCallback(callback, null)
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }

    // Clock: ticks only while RESUMED, so screen-off stops the loop rather than wasting wakeups.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                time = currentTimeString(is24h)
                amPm = if (is24h) "" else currentAmPmString()
                val now = System.currentTimeMillis()
                delay(1_000L - (now % 1_000L))
            }
        }
    }

    return ZenoStatusBarState(
        time = time,
        amPm = amPm,
        batteryPct = if (batteryPct >= 0) batteryPct else null,
        batteryCharging = batteryCharging,
        wifiConnected = wifiConnected,
        wifiEnabled = wifiEnabled,
        cellularActive = cellularActive,
        soundProfile = soundProfile,
        headsetConnected = headsetConnected,
        locationEnabled = locationOn,
        nfcEnabled = nfcOn,
        bluetoothEnabled = bluetoothOn,
        carrierName = carrierName,
        signalLevel = signalLevel,
        notifyIcons = notifyIcons,
        powerSaveActive = powerSaveActive,
        hotspotEnabled = hotspotEnabled,
        airplaneModeEnabled = airplaneModeEnabled,
    )
}

/** Prefers the notification's own small icon — the monochrome glyph each app builds
 *  specifically for the status bar, the same one the real Android status bar shows — tinted
 *  solid white to match the rest of ZenoStatusBar's icon cluster. Falls back to a greyscale
 *  launcher icon (matching Minimal Mode's own notification-summary row) only when the small
 *  icon can't be loaded, e.g. a stale/GC'd Icon reference. */
private fun resolveNotifyIconBitmap(
    context: Context,
    pkg: String,
    smallIcon: android.graphics.drawable.Icon?,
): android.graphics.Bitmap? {
    val whiteTinted = smallIcon?.let { icon ->
        runCatching {
            val drawable = icon.loadDrawable(context) ?: return@runCatching null
            drawable.mutate()
            drawable.setTint(android.graphics.Color.WHITE)
            drawable.toBitmap(48, 48)
        }.getOrNull()
    }
    if (whiteTinted != null) return whiteTinted

    return runCatching {
        val src = context.packageManager.getApplicationIcon(pkg).toBitmap(48, 48)
        val grey = android.graphics.Bitmap.createBitmap(
            src.width,
            src.height,
            src.config ?: android.graphics.Bitmap.Config.ARGB_8888,
        )
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().also { it.setSaturation(0f) })
        }
        android.graphics.Canvas(grey).drawBitmap(src, 0f, 0f, paint)
        grey
    }.getOrNull()
}
