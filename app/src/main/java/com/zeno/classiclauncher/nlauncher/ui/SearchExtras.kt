package com.zeno.classiclauncher.nlauncher.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette
import java.util.Locale

data class SearchExtra(
    val title: String,
    val subtitle: String,
    val onOpen: () -> Unit,
)

/**
 * Keyword → system shortcuts (no DB, no contacts read permission).
 * [buildSearchExtras] stays O(keyword rows) per query.
 */
fun buildSearchExtras(
    context: Context,
    query: String,
    onMissingHandler: () -> Unit,
): List<SearchExtra> {
    val q = query.trim().lowercase(Locale.US)
    if (q.length < 2) return emptyList()

    val out = mutableListOf<SearchExtra>()
    val seen = mutableSetOf<String>()

    fun add(keys: List<String>, title: String, subtitle: String, intent: Intent) {
        if (title in seen) return
        if (!keys.any { k -> q.contains(k) }) return
        seen.add(title)
        out.add(
            SearchExtra(title, subtitle) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }.onFailure { onMissingHandler() }
            },
        )
    }

    add(listOf("wifi", "wi-fi", "wireless"), "Wi‑Fi", "Open Wi‑Fi settings", Intent(Settings.ACTION_WIFI_SETTINGS))
    add(listOf("internet", "data", "mobile data", "cellular"), "Network & internet", "Open network settings", Intent(Settings.ACTION_WIRELESS_SETTINGS))
    add(listOf("bluetooth"), "Bluetooth", "Open Bluetooth settings", Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    add(listOf("hotspot", "tether"), "Hotspot & tethering", "Open tethering settings", Intent(Settings.ACTION_WIRELESS_SETTINGS))
    add(listOf("airplane", "flight mode"), "Airplane mode", "Open wireless settings", Intent(Settings.ACTION_WIRELESS_SETTINGS))
    add(listOf("nfc"), "NFC", "Open NFC settings", Intent(Settings.ACTION_NFC_SETTINGS))
    add(listOf("display", "screen", "brightness", "dark mode", "theme"), "Display", "Open Display settings", Intent(Settings.ACTION_DISPLAY_SETTINGS))
    add(listOf("wallpaper", "background"), "Wallpaper", "Open wallpaper settings", Intent("android.settings.WALLPAPER_SETTINGS"))
    add(listOf("battery", "power", "charge"), "Battery", "Open Battery settings", Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
    add(listOf("sound", "volume", "audio", "ringtone"), "Sound", "Open Sound settings", Intent(Settings.ACTION_SOUND_SETTINGS))
    add(
        listOf("notif", "notification", "do not disturb", "dnd"),
        "Notifications",
        "Open notification settings",
        Intent("android.settings.NOTIFICATION_SETTINGS"),
    )
    add(listOf("app", "application", "manage app", "uninstall app"), "Apps", "Open Apps settings", Intent(Settings.ACTION_APPLICATION_SETTINGS))
    add(listOf("storage", "space", "free space"), "Storage", "Open Storage settings", Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
    add(listOf("security", "privacy", "lock", "fingerprint", "face unlock"), "Security", "Open Security settings", Intent(Settings.ACTION_SECURITY_SETTINGS))
    add(listOf("location", "gps", "maps location"), "Location", "Open Location settings", Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    add(listOf("date", "time", "clock", "timezone"), "Date & time", "Open Date & time settings", Intent(Settings.ACTION_DATE_SETTINGS))
    add(listOf("language", "keyboard", "input", "spell"), "Languages & input", "Open Language & input", Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    add(listOf("accessibility", "a11y", "talkback", "font size"), "Accessibility", "Open Accessibility", Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    add(listOf("developer", "usb debug", "adb"), "Developer options", "Open Developer settings", Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
    add(listOf("account", "sync", "google account"), "Accounts", "Open Account settings", Intent(Settings.ACTION_SYNC_SETTINGS))
    add(listOf("vpn"), "VPN", "Open VPN settings", Intent(Settings.ACTION_VPN_SETTINGS))
    add(listOf("print", "printer"), "Printing", "Open Print settings", Intent(Settings.ACTION_PRINT_SETTINGS))
    add(listOf("device info", "about phone", "software", "android version", "model"), "About device", "Open About phone", Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
    add(listOf("default", "default app", "browser app", "phone app"), "Default apps", "Open default apps", Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    add(listOf("permission", "app permission"), "App permissions", "Open permission manager", Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
    add(listOf("system setting", "configure", "open settings", "all settings"), "System settings", "Open Settings", Intent(Settings.ACTION_SETTINGS))

    add(
        listOf("contact", "contacts", "people", "address book", "phone book"),
        "Contacts",
        "Open Contacts",
        Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI),
    )
    add(
        listOf("dial", "dialer", "phone pad", "keypad"),
        "Phone dialer",
        "Open dialer",
        Intent(Intent.ACTION_DIAL),
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(listOf("wifi panel", "internet panel"), "Internet panel", "Quick settings panel", Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        add(listOf("volume panel", "media output"), "Media output", "Volume / output panel", Intent(Settings.Panel.ACTION_VOLUME))
    }

    return out.take(12)
}

fun privateSearchHintColor(themePalette: LauncherThemePalette): Color =
    themePalette.settingsMenuBody.copy(alpha = 0.92f)
