package com.zeno.classiclauncher.nlauncher.apps

import android.Manifest
import android.bluetooth.BluetoothManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.Uri
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.provider.MediaStore
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.core.content.ContextCompat

sealed class ToggleResult {
    data class Changed(val enabled: Boolean) : ToggleResult()
    data object PermissionRequired : ToggleResult()
    data object Unsupported : ToggleResult()
}

class LauncherActions(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private val bitwardenPackage = "com.x8bit.bitwarden"
    private val wellbeingPackage = "com.google.android.apps.wellbeing"
    private val gmsPackage = "com.google.android.gms"
    /**
     * GMS barcode UI is registered on intent actions; starting by raw [ComponentName] alone often
     * finishes immediately (internal caller checks). See `adb shell dumpsys package com.google.android.gms`.
     */
    private val gmsBarcodeComponentStr = "com.google.android.gms/.mlkit.barcode.ui.PlatformBarcodeScanningActivityProxy"
    private val gmsBarcodeLaunchFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    private val keyboardSettingsPackage = "com.android.settings"
    private val keyboardSettingsActivity = "com.android.settings.Settings\$KeyboardFunctionSettingsActivity"
    private val keyboardModeBroadcastAction = "com.android.settings.keyboard.KeyboardFunctionSettings"
    private val keyboardModeKey = "KEYBOARD_MODE"
    private val launcherPrefs by lazy { context.getSharedPreferences("launcher_actions", Context.MODE_PRIVATE) }
    private val keyboardModeUiKey = "keyboard_mode_ui"

    fun launchApp(packageName: String): Boolean {
        // Some dialer apps expose a launcher activity that only shows "set default phone app".
        // If this package is the current default dialer, route through ACTION_DIAL first.
        if (launchDefaultDialerTarget(packageName)) return true
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    fun startShortcut(packageName: String, shortcutId: String): Boolean {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return false
        return try {
            launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openAppInfo(packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    fun launchCamera(): Boolean {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolved = pm.resolveActivity(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolved != null) {
            val pkg = resolved.activityInfo.packageName
            if (launchApp(pkg)) return true
            // getLaunchIntentForPackage failed — start capture intent directly
            return try { context.startActivity(captureIntent); true } catch (_: Exception) { false }
        }
        // No camera app resolves IMAGE_CAPTURE — try common camera packages
        val fallbacks = listOf(
            "com.android.camera2",
            "com.android.camera",
            "com.google.android.GoogleCamera",
            "com.sec.android.app.camera",
        )
        for (pkg in fallbacks) {
            if (launchApp(pkg)) return true
        }
        return false
    }

    fun launchMail(): Boolean {
        // Prefer Zeno Hub when installed — it provides the unified inbox experience.
        if (launchApp("com.zeno.hub")) return true
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return false
        return launchApp(resolved.activityInfo.packageName)
    }

    fun launchMessages(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolved != null) return launchApp(resolved.activityInfo.packageName)

        val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolvedSms = pm.resolveActivity(sms, PackageManager.MATCH_DEFAULT_ONLY) ?: return false
        return launchApp(resolvedSms.activityInfo.packageName)
    }

    /** System static wallpaper / “Wallpapers” entry (OEM chooser). */
    fun openSystemWallpaperChooser(): Boolean =
        startActivityNewTask(Intent(Intent.ACTION_SET_WALLPAPER))

    fun openLiveWallpaperChooser(): Boolean =
        startActivityNewTask(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))

    fun openWallpaperStyleSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.WALLPAPER_SETTINGS"))

    /** Classic gallery-style image pick. */
    fun openGalleryForWallpaper(): Boolean =
        startActivityNewTask(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))

    /** Photo picker (API 33+) or [Intent.ACTION_GET_CONTENT] on older devices. */
    fun openPhotosForWallpaper(): Boolean {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES)
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        }
        return startActivityNewTask(intent)
    }

    fun openSystemSettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_SETTINGS))

    fun openWifiSettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_WIFI_SETTINGS)) ||
            openSystemSettings()

    fun openBluetoothSettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) ||
            openSystemSettings()

    fun openMobileNetworkSettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)) ||
            startActivityNewTask(Intent(Settings.ACTION_WIRELESS_SETTINGS)) ||
            openSystemSettings()

    fun isWifiEnabled(): Boolean? {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        @Suppress("DEPRECATION")
        return wifi.isWifiEnabled
    }

    fun toggleWifi(): ToggleResult {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return ToggleResult.Unsupported
        @Suppress("DEPRECATION")
        val targetEnabled = !wifi.isWifiEnabled
        @Suppress("DEPRECATION")
        var changed = runCatching { wifi.setWifiEnabled(targetEnabled) }.getOrDefault(false)
        if (!changed) {
            @Suppress("DEPRECATION")
            changed = runCatching { wifi.setWifiEnabled(targetEnabled) }.getOrDefault(false)
        }
        if (changed) return ToggleResult.Changed(targetEnabled)
        val globalOk = runCatching {
            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.WIFI_ON,
                if (targetEnabled) 1 else 0,
            )
        }.getOrDefault(false)
        return if (globalOk) ToggleResult.Changed(targetEnabled) else ToggleResult.Unsupported
    }

    fun isMobileDataEnabled(): Boolean? {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val dataEnabled = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tm?.isDataEnabled else null
        }.getOrNull()
        if (dataEnabled != null) return dataEnabled
        return runCatching { Settings.Global.getInt(context.contentResolver, "mobile_data") > 0 }.getOrNull()
    }

    fun toggleMobileData(): ToggleResult {
        val tm = context.getSystemService(TelephonyManager::class.java) ?: return ToggleResult.Unsupported
        val current = isMobileDataEnabled() ?: return ToggleResult.Unsupported
        val targetEnabled = !current

        val reflectionChanged = runCatching {
            val method = TelephonyManager::class.java.getDeclaredMethod("setDataEnabled", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(tm, targetEnabled)
            true
        }.getOrDefault(false)
        if (reflectionChanged) return ToggleResult.Changed(targetEnabled)

        val globalOk = runCatching {
            Settings.Global.putInt(
                context.contentResolver,
                "mobile_data",
                if (targetEnabled) 1 else 0,
            )
        }.getOrDefault(false)
        return if (globalOk) ToggleResult.Changed(targetEnabled) else ToggleResult.Unsupported
    }

    fun openAlarmSettings(): Boolean =
        startActivityNewTask(Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun openNotificationSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.NOTIFICATION_SETTINGS"))

    fun openInternetSettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_WIFI_SETTINGS)) ||
            startActivityNewTask(Intent(Settings.ACTION_WIRELESS_SETTINGS))

    fun openInternetPanel(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startActivityNewTask(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)) || openInternetSettings()
        } else {
            openInternetSettings()
        }

    fun openQrScanner(): Boolean {
        fun tryGmsBarcode(intent: Intent): Boolean {
            intent.setPackage(gmsPackage)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addFlags(gmsBarcodeLaunchFlags)
            return startActivityNewTask(intent)
        }
        // V2 activity sometimes survives where PlatformBarcodeScanningActivityProxy bails for non-SystemUI callers.
        if (tryGmsBarcode(Intent("com.google.android.gms.mlkit_barcode_ui.SCAN_QR_CODE_V2"))) return true
        if (tryGmsBarcode(Intent("com.google.android.gms.mlkit_barcode_ui.SCAN_QR_CODE"))) return true
        if (tryGmsBarcode(Intent("com.google.android.gms.mlkit.ACTION_SCAN_BARCODE"))) return true

        val fromUnflatten = ComponentName.unflattenFromString(gmsBarcodeComponentStr)?.let { cn ->
            Intent().apply {
                component = cn
                addFlags(gmsBarcodeLaunchFlags)
            }
        }
        if (fromUnflatten != null && startActivityNewTask(fromUnflatten)) return true

        if (startActivityNewTask(Intent("android.settings.QR_CODE_SCANNER_SETTINGS"))) return true
        if (startActivityNewTask(Intent("com.android.settings.QR_CODE_SCANNER_SETTINGS"))) return true
        return false
    }

    fun openWirelessDebuggingSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS")) ||
            startActivityNewTask(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) ||
            openSystemSettings()

    fun openKeyboardSettings(): Boolean =
        startActivityNewTask(
            Intent().setClassName(keyboardSettingsPackage, keyboardSettingsActivity),
        ) || startActivityNewTask(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))

    fun canWriteSystemSettings(): Boolean =
        Settings.System.canWrite(context)

    fun requestWriteSettingsPermission(): Boolean =
        startActivityNewTask(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        )

    fun currentKeyboardMode(): String? {
        val mode = runCatching {
            Settings.System.getString(context.contentResolver, keyboardModeKey)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: runCatching {
            Settings.System.getString(context.contentResolver, "keyboard_mode")
        }.getOrNull()
        return when (mode?.trim()?.lowercase()) {
            "keyboard" -> "keyboard"
            "0" -> "keyboard"
            "mouse" -> "mouse"
            "1" -> "mouse"
            else -> null
        }
    }

    fun lastKnownKeyboardMode(): String {
        val saved = launcherPrefs.getString(keyboardModeUiKey, null)?.lowercase()
        if (saved == "keyboard" || saved == "mouse") return saved
        return currentKeyboardMode() ?: "keyboard"
    }

    fun persistKeyboardModeLabel(mode: String) {
        val normalized = if (mode.lowercase() == "mouse") "mouse" else "keyboard"
        launcherPrefs.edit().putString(keyboardModeUiKey, normalized).apply()
    }

    fun setKeyboardMode(targetMode: String): Boolean {
        val nextMode = if (targetMode.lowercase() == "mouse") "mouse" else "keyboard"
        val writeUpper = runCatching {
            Settings.System.putString(context.contentResolver, keyboardModeKey, nextMode)
        }.getOrDefault(false)
        val writeLower = runCatching {
            Settings.System.putString(context.contentResolver, "keyboard_mode", nextMode)
        }.getOrDefault(false)

        // Mirror OEM tile: this broadcast is consumed by KeyboardModeTileService when active.
        val broadcastSent = runCatching {
            val modeIndex = if (nextMode == "keyboard") 0 else 1
            val intent = Intent(keyboardModeBroadcastAction).apply {
                setPackage(keyboardSettingsPackage)
                putExtra("keyboard_mode", modeIndex)
            }
            context.sendBroadcast(intent)
            // Some ROMs only respond without package scoping.
            context.sendBroadcast(
                Intent(keyboardModeBroadcastAction).putExtra("keyboard_mode", modeIndex),
            )
            true
        }.getOrDefault(false)

        if (!(writeUpper || writeLower || broadcastSent)) return false
        val readBack = currentKeyboardMode()
        persistKeyboardModeLabel(readBack ?: nextMode)
        return readBack == null || readBack == nextMode
    }

    fun toggleKeyboardMode(): Boolean {
        val nextMode = if (currentKeyboardMode() == "keyboard") "mouse" else "keyboard"
        return setKeyboardMode(nextMode)
    }

    fun openBatterySaverSettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) ||
            startActivityNewTask(Intent("android.settings.BATTERY_SAVER_SETTINGS"))

    /** Matches system QS “battery” tile: usage / power summary. */
    fun openBatteryUsageSummary(): Boolean =
        startActivityNewTask(Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) ||
            openBatterySaverSettings()

    fun batteryPercent(): Int? {
        val bm = context.getSystemService(BatteryManager::class.java) ?: return null
        val p = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (p in 0..100) p else null
    }

    fun isDigitalWellbeingInstalled(): Boolean =
        pm.getLaunchIntentForPackage(wellbeingPackage) != null

    /** Opens Digital Wellbeing (system Grayscale / Wind Down live here on Pixel-style builds). */
    fun openDigitalWellbeingHome(): Boolean {
        val intent = pm.getLaunchIntentForPackage(wellbeingPackage) ?: return false
        return startActivityNewTask(intent)
    }

    fun openSecuritySettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_SECURITY_SETTINGS))

    fun openAirplaneModeSettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)) ||
            openSystemSettings()

    fun openStorageSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.INTERNAL_STORAGE_SETTINGS")) ||
            startActivityNewTask(Intent(Settings.ACTION_MEMORY_CARD_SETTINGS))

    fun openHotspotSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.TETHER_SETTINGS"))

    fun openNightLightSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.NIGHT_DISPLAY_SETTINGS")) ||
            startActivityNewTask(Intent(Settings.ACTION_DISPLAY_SETTINGS))

    fun openDisplaySettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_DISPLAY_SETTINGS))

    fun openNfcSettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_NFC_SETTINGS)) ||
            openSystemSettings()

    fun openExtraDimSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.REDUCE_BRIGHT_COLORS_SETTINGS")) ||
            startActivityNewTask(Intent(Settings.ACTION_DISPLAY_SETTINGS))

    fun openScreenRecordSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.SYSTEMUI_QS_TILES_SETTINGS")) ||
            startActivityNewTask(Intent(Settings.ACTION_SETTINGS))

    fun openCastSettings(): Boolean =
        startActivityNewTask(Intent(Settings.ACTION_CAST_SETTINGS)) ||
            startActivityNewTask(Intent(Settings.ACTION_DISPLAY_SETTINGS))

    fun openBedtimeSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.BEDTIME_SETTINGS")) ||
            startActivityNewTask(Intent("android.settings.ZEN_MODE_SETTINGS"))

    fun openDoNotDisturbSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.ZEN_MODE_SETTINGS")) ||
            openSystemSettings()

    fun openBitwardenVault(): Boolean =
        launchApp(bitwardenPackage)

    fun isBitwardenInstalled(): Boolean =
        pm.getLaunchIntentForPackage(bitwardenPackage) != null

    @Suppress("DEPRECATION")
    private fun bluetoothAdapter(): android.bluetooth.BluetoothAdapter? {
        val fromManager = context.applicationContext.getSystemService(BluetoothManager::class.java)?.adapter
        if (fromManager != null) return fromManager
        return android.bluetooth.BluetoothAdapter.getDefaultAdapter()
    }

    fun isBluetoothEnabled(): Boolean? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val adapter = bluetoothAdapter() ?: return null
        @Suppress("DEPRECATION")
        return adapter.isEnabled
    }

    /**
     * Same strategy as [setKeyboardMode]: direct API, retry, then [Settings.Global] mirror some OEMs honour.
     * Keyboard tile also relies on Settings.System + a Settings-scoped broadcast; Bluetooth has no stable public
     * broadcast, so Global [Settings.Global.BLUETOOTH_ON] is the main extra lever.
     *
     * **Platform note:** On API 31+, [android.bluetooth.BluetoothAdapter.enable]/[disable] usually fail for
     * third-party apps, and writing [Settings.Global.BLUETOOTH_ON] is not permitted. [ToggleResult.Unsupported]
     * is expected on many devices even when [Manifest.permission.BLUETOOTH_CONNECT] is granted. See `docs/NOTES.md`.
     */
    fun toggleBluetooth(): ToggleResult {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return ToggleResult.PermissionRequired
        }
        val adapter = bluetoothAdapter() ?: return ToggleResult.Unsupported
        @Suppress("DEPRECATION")
        val targetEnabled = !adapter.isEnabled
        @Suppress("DEPRECATION")
        fun tryAdapter(): Boolean =
            if (targetEnabled) adapter.enable() else adapter.disable()

        var changed = tryAdapter()
        if (!changed) changed = tryAdapter()
        if (changed) return ToggleResult.Changed(targetEnabled)

        val globalOk = runCatching {
            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.BLUETOOTH_ON,
                if (targetEnabled) 1 else 0,
            )
        }.getOrDefault(false)
        if (globalOk) return ToggleResult.Changed(targetEnabled)

        @Suppress("DEPRECATION")
        return if (adapter.isEnabled == targetEnabled) {
            ToggleResult.Changed(targetEnabled)
        } else {
            ToggleResult.Unsupported
        }
    }

    fun toggleNfc(): ToggleResult {
        val adapter = context.getSystemService(NfcManager::class.java)?.defaultAdapter ?: return ToggleResult.Unsupported
        val wasEnabled = adapter.isEnabled
        val targetEnabled = !wasEnabled
        val methodName = if (targetEnabled) "enable" else "disable"
        val invoked = runCatching {
            val m = NfcAdapter::class.java.getMethod(methodName)
            m.invoke(adapter)
        }
        if (invoked.isFailure) return ToggleResult.Unsupported
        return if (adapter.isEnabled != wasEnabled) {
            ToggleResult.Changed(adapter.isEnabled)
        } else {
            ToggleResult.Unsupported
        }
    }

    fun currentCarrierName(): String {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val name = tm?.networkOperatorName?.trim().orEmpty()
        return if (name.isNotEmpty()) name else "Mobile data"
    }

    fun hasWifiNamePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasPreciseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isWifiConnected(): Boolean {
        val app = context.applicationContext
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun currentWifiSsidLabel(): String {
        val app = context.applicationContext
        val cm = app.getSystemService(ConnectivityManager::class.java)
        val active = cm?.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (!onWifi) return "Disconnected"

        // Prefer transportInfo on newer APIs; it aligns with current active network.
        val fromTransport = (caps?.transportInfo as? WifiInfo)?.ssid?.trim().orEmpty()

        val wifi = app.getSystemService(WifiManager::class.java)
        @Suppress("DEPRECATION")
        val fromManager = wifi?.connectionInfo?.ssid?.trim().orEmpty()

        val raw = sequenceOf(fromTransport, fromManager)
            .map { it.removePrefix("\"").removeSuffix("\"").trim() }
            .firstOrNull { candidate ->
                candidate.isNotBlank() &&
                    !candidate.equals("<unknown ssid>", ignoreCase = true) &&
                    !candidate.equals("unknown ssid", ignoreCase = true)
            }
            ?: "Connected"

        val ssid = raw.removePrefix("\"").removeSuffix("\"")
        return when {
            ssid.isBlank() -> "Connected"
            ssid.equals("<unknown ssid>", ignoreCase = true) -> "Connected"
            ssid.equals("unknown ssid", ignoreCase = true) -> "Connected"
            else -> ssid
        }
    }

    fun isWirelessDebuggingEnabled(): Boolean =
        readGlobalInt("adb_wifi_enabled") > 0

    fun isBatterySaverEnabled(): Boolean =
        readGlobalInt("low_power") > 0

    fun isAirplaneModeEnabled(): Boolean =
        readGlobalInt("airplane_mode_on") > 0

    fun isDoNotDisturbEnabled(): Boolean =
        readGlobalInt("zen_mode") > 0

    fun isNightLightEnabled(): Boolean =
        readSecureInt("night_display_activated") > 0

    fun isAutoRotateEnabled(): Boolean =
        readSystemInt("accelerometer_rotation") > 0

    fun isExtraDimEnabled(): Boolean =
        readSecureInt("reduce_bright_colors_activated") > 0

    fun isTorchEnabled(): Boolean =
        readSecureInt("flashlight_enabled") > 0

    private fun firstBackCameraIdWithFlash(): String? {
        val cm = context.getSystemService(CameraManager::class.java) ?: return null
        return cm.cameraIdList.firstOrNull { id ->
            val ch = cm.getCameraCharacteristics(id)
            val facing = ch.get(CameraCharacteristics.LENS_FACING)
            val flash = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            flash && facing == CameraCharacteristics.LENS_FACING_BACK
        } ?: cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    /** Toggle torch via Camera2 (needs [Manifest.permission.CAMERA] at runtime). */
    fun toggleTorch(): ToggleResult {
        val cm = context.getSystemService(CameraManager::class.java) ?: return ToggleResult.Unsupported
        val id = firstBackCameraIdWithFlash() ?: return ToggleResult.Unsupported
        val wantOn = !isTorchEnabled()
        return try {
            cm.setTorchMode(id, wantOn)
            ToggleResult.Changed(wantOn)
        } catch (_: Exception) {
            ToggleResult.Unsupported
        }
    }

    fun hasTorchHardware(): Boolean = firstBackCameraIdWithFlash() != null

    /** Toggle auto-rotate when [Settings.System.canWrite] is true ([WRITE_SETTINGS]). */
    fun toggleAutoRotate(): ToggleResult {
        if (!Settings.System.canWrite(context)) return ToggleResult.PermissionRequired
        val cur = isAutoRotateEnabled() ?: return ToggleResult.Unsupported
        val next = !cur
        val v = if (next) 1 else 0
        return try {
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, v)
            ToggleResult.Changed(next)
        } catch (_: Exception) {
            ToggleResult.Unsupported
        }
    }

    fun isHotspotEnabled(): Boolean =
        readGlobalInt("tethering_on") > 0 || readGlobalInt("hotspot_on") > 0

    fun isNfcEnabled(): Boolean {
        val adapter = context.getSystemService<android.nfc.NfcManager>()?.defaultAdapter
        return adapter?.isEnabled == true || readGlobalInt("nfc_on") > 0
    }

    private fun readGlobalInt(key: String): Int =
        runCatching { Settings.Global.getInt(context.contentResolver, key) }.getOrDefault(0)

    private fun readSecureInt(key: String): Int =
        runCatching { Settings.Secure.getInt(context.contentResolver, key) }.getOrDefault(0)

    private fun readSystemInt(key: String): Int =
        runCatching { Settings.System.getInt(context.contentResolver, key) }.getOrDefault(0)

    private fun startActivityNewTask(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun launchDefaultDialerTarget(packageName: String): Boolean {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return false
        val defaultDialer = telecom.defaultDialerPackage ?: return false
        if (defaultDialer != packageName) return false

        val dialWithPackage = Intent(Intent.ACTION_DIAL).apply {
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (pm.resolveActivity(dialWithPackage, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            return try {
                context.startActivity(dialWithPackage)
                true
            } catch (_: Exception) {
                false
            }
        }

        return false
    }
}
