package com.zeno.classiclauncher.nlauncher.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

enum class GridPreset(val rows: Int, val cols: Int) {
    R3C5(3, 5),
    R3C4(3, 4),
    R4C4(4, 4),
    R5C5(5, 5),
}

enum class SecondShortcutTarget {
    MESSAGES,
    WHATSAPP,
}

enum class AppIconShape {
    ROUNDED,
    SQUIRCLE,
    CIRCLE,
    SOFT_SQUARE,
}

enum class DockIconStyle {
    MONOCHROME,
    APP,
}

enum class GlanceCalendarRange {
    DAY,
    WEEK,
}

enum class GlanceWeatherUnit {
    CELSIUS,
    FAHRENHEIT,
}

enum class GlanceWeatherLocationMode {
    DEVICE,
    MANUAL,
}

data class LauncherPrefs(
    val gridPreset: GridPreset = GridPreset.R3C5,
    val secondShortcutTarget: SecondShortcutTarget = SecondShortcutTarget.MESSAGES,
    /** Empty = auto (heuristic). Otherwise badge count for this package only. */
    val mailBadgePackage: String = "",
    /** Empty = launch default mail app resolution; else launch this package. */
    val dockMailPackage: String = "",
    /** Empty = launch default messaging app resolution; else launch this package. */
    val dockSecondPackage: String = "",
    /** Empty = default [android.provider.MediaStore.ACTION_IMAGE_CAPTURE] resolution; else launch this package. */
    val dockCameraPackage: String = "",
    /** Custom labels for dock shortcuts shown in settings/accessibility. */
    val dockMailTitle: String = "Mail",
    val dockSecondTitle: String = "Messages",
    val dockThirdTitle: String = "Camera",
    val dockIconStyle: DockIconStyle = DockIconStyle.MONOCHROME,
    val orderedPackages: List<String> = emptyList(),
    /** Folder slot id → member package names (order preserved). */
    val folderContents: Map<String, List<String>> = emptyMap(),
    /** Folder slot id → user-visible title. */
    val folderNames: Map<String, String> = emptyMap(),
    val hiddenPackages: Set<String> = emptySet(),
    /**
     * Up to 3 entries shown between home groups on the home screen above the dock.
     * Each string is either a launchable package name or `package#shortcutId` for a pinned dynamic shortcut.
     */
    val homeShortcutPackages: List<String> = emptyList(),
    /** Haptic feedback when navigating with keyboard / trackpad (dock scroll, grid focus, settings list). */
    val hapticsEnabled: Boolean = true,
    /** Haptic intensity 1–5 (1 = lightest, 5 = strongest). Only used when hapticsEnabled = true. */
    val hapticIntensity: Int = 3,
    /** When false, mail/shortcut unread badges are off and notification listener access is not required. */
    val notificationBadgesEnabled: Boolean = true,
    val themeJson: String = DEFAULT_THEME_JSON,
    /** Master switch: when false, the home glance strip is not shown. */
    val glanceEnabled: Boolean = true,
    /** Glance carousel items (calendar implemented; others reserved for smartspace-style alerts). */
    val glanceShowFlashlight: Boolean = true,
    val glanceShowBattery: Boolean = true,
    val glanceShowCalendar: Boolean = true,
    val glanceShowAlarm: Boolean = true,
    val glanceCalendarRange: GlanceCalendarRange = GlanceCalendarRange.DAY,
    val glanceWeatherUnit: GlanceWeatherUnit = GlanceWeatherUnit.CELSIUS,
    val glanceWeatherLocationMode: GlanceWeatherLocationMode = GlanceWeatherLocationMode.DEVICE,
    val glanceWeatherManualLatitude: String = "",
    val glanceWeatherManualLongitude: String = "",
    /** At most two home groups (left / right of centre shortcuts). */
    val homeGroups: List<HomeGroup> = emptyList(),
    /** Double-tap home workspace to lock (requires device admin). Default on. */
    val doubleTapToSleepEnabled: Boolean = true,
    /** Package to launch on swipe-up gesture on home. Empty = disabled. */
    val swipeUpPackage: String = "",
    /** Package to launch on double-tap on home (only when doubleTapToSleepEnabled is false). */
    val doubleTapPackage: String = "",
    /** Show today's screen-time badge (e.g. "2h") on app icons in the drawer. */
    val showUsageStatsBadge: Boolean = true,
    /** Show a red dot badge on app icons in the drawer when they have unread notifications. */
    val showIconNotifBadge: Boolean = true,
    /** Whether shortcut apps are visible on the home strip. */
    val showShortcutApps: Boolean = true,
    /** Whether home groups are visible on the home strip. */
    val showHomeGroups: Boolean = true,
    /**
     * Classic mode: app drawer only (no separate home page). Dock is mail + page dots + camera (no home
     * button, no Messages/WhatsApp shortcut). Home glance strip is not composed — no glance coroutines or
     * receivers while this is on.
     */
    val classicMode: Boolean = false,
    /** Global installed-app icon mask used across launcher surfaces. */
    val appIconShape: AppIconShape = AppIconShape.ROUNDED,
)

/** Full nested JSON (Flutter-shaped); legacy flat-only strings still load via [LauncherThemePalette.fromJson]. */
val DEFAULT_THEME_JSON: String =
    LauncherThemePalette.toExportJson(LauncherThemePalette.fromJson("{}"))

private val DEFAULT_PREFS = LauncherPrefs()

class LauncherPrefsRepository(private val context: Context) {
    private object Keys {
        val GRID = stringPreferencesKey("gridPreset")
        val SHORTCUT = stringPreferencesKey("secondShortcut")
        val MAIL_BADGE = stringPreferencesKey("mailBadgePackage")
        val DOCK_MAIL = stringPreferencesKey("dockMailPackage")
        val DOCK_SECOND = stringPreferencesKey("dockSecondPackage")
        val DOCK_CAMERA = stringPreferencesKey("dockCameraPackage")
        val DOCK_MAIL_TITLE = stringPreferencesKey("dockMailTitle")
        val DOCK_SECOND_TITLE = stringPreferencesKey("dockSecondTitle")
        val DOCK_THIRD_TITLE = stringPreferencesKey("dockThirdTitle")
        val DOCK_ICON_STYLE = stringPreferencesKey("dockIconStyle")
        val ORDER = stringPreferencesKey("orderedPackagesCsv")
        val FOLDERS = stringPreferencesKey("folderContentsJson")
        val FOLDER_NAMES = stringPreferencesKey("folderNamesJson")
        val HIDDEN = stringPreferencesKey("hiddenPackagesCsv")
        val HOME_SHORTCUTS = stringPreferencesKey("homeShortcutsCsv")
        val HAPTICS = booleanPreferencesKey("hapticsEnabled")
        val HAPTIC_INTENSITY = intPreferencesKey("hapticIntensity")
        val NOTIFICATION_BADGES = booleanPreferencesKey("notificationBadgesEnabled")
        val THEME = stringPreferencesKey("themeJson")
        val GLANCE_ENABLED = booleanPreferencesKey("glanceEnabled")
        val GLANCE_FLASHLIGHT = booleanPreferencesKey("glanceShowFlashlight")
        val GLANCE_BATTERY = booleanPreferencesKey("glanceShowBattery")
        val GLANCE_CALENDAR = booleanPreferencesKey("glanceShowCalendar")
        val GLANCE_ALARM = booleanPreferencesKey("glanceShowAlarm")
        val GLANCE_CALENDAR_RANGE = stringPreferencesKey("glanceCalendarRange")
        val GLANCE_WEATHER_UNIT = stringPreferencesKey("glanceWeatherUnit")
        val GLANCE_WEATHER_LOCATION_MODE = stringPreferencesKey("glanceWeatherLocationMode")
        val GLANCE_WEATHER_MANUAL_LAT = stringPreferencesKey("glanceWeatherManualLatitude")
        val GLANCE_WEATHER_MANUAL_LON = stringPreferencesKey("glanceWeatherManualLongitude")
        val HOME_GROUPS = stringPreferencesKey("homeGroupsJson")
        val DOUBLE_TAP_SLEEP = booleanPreferencesKey("doubleTapToSleepEnabled")
        val SWIPE_UP_PKG = stringPreferencesKey("swipeUpPackage")
        val DOUBLE_TAP_PKG = stringPreferencesKey("doubleTapPackage")
        val SHOW_USAGE_STATS_BADGE = booleanPreferencesKey("showUsageStatsBadge")
        val SHOW_ICON_NOTIF_BADGE = booleanPreferencesKey("showIconNotifBadge")
        val SHOW_SHORTCUT_APPS = booleanPreferencesKey("showShortcutApps")
        val SHOW_HOME_GROUPS = booleanPreferencesKey("showHomeGroups")
        val CLASSIC_MODE = booleanPreferencesKey("classicMode")
        /** Legacy key from earlier builds; read only for migration. */
        val CLASSIC_MODE_LEGACY = booleanPreferencesKey("drawerOnlyMode")
        val APP_ICON_SHAPE = stringPreferencesKey("appIconShape")
    }

    val prefsFlow: Flow<LauncherPrefs> = context.dataStore.data.map { p ->
        val grid = p[Keys.GRID]?.let { v -> GridPreset.entries.firstOrNull { it.name == v } } ?: DEFAULT_PREFS.gridPreset
        val shortcut =
            p[Keys.SHORTCUT]?.let { v -> SecondShortcutTarget.entries.firstOrNull { it.name == v } } ?: DEFAULT_PREFS.secondShortcutTarget
        val mailBadge = p[Keys.MAIL_BADGE]?.trim() ?: ""
        val dockMail = p[Keys.DOCK_MAIL]?.trim() ?: ""
        val dockSecond = (p[Keys.DOCK_SECOND]?.trim() ?: "").ifEmpty {
            if (shortcut == SecondShortcutTarget.WHATSAPP) "com.whatsapp" else ""
        }
        val dockCamera = p[Keys.DOCK_CAMERA]?.trim() ?: ""
        val dockMailTitle = p[Keys.DOCK_MAIL_TITLE]?.trim().orEmpty().ifEmpty { DEFAULT_PREFS.dockMailTitle }
        val dockSecondTitle = p[Keys.DOCK_SECOND_TITLE]?.trim().orEmpty().ifEmpty { DEFAULT_PREFS.dockSecondTitle }
        val dockThirdTitle = p[Keys.DOCK_THIRD_TITLE]?.trim().orEmpty().ifEmpty { DEFAULT_PREFS.dockThirdTitle }
        val dockIconStyle =
            p[Keys.DOCK_ICON_STYLE]?.let { v -> DockIconStyle.entries.firstOrNull { it.name == v } }
                ?: DEFAULT_PREFS.dockIconStyle
        val order = parseCsvList(p[Keys.ORDER])
        val folders = parseFolderContentsJson(p[Keys.FOLDERS])
        val folderNames = parseFolderNamesJson(p[Keys.FOLDER_NAMES])
        val hidden = parseCsvSet(p[Keys.HIDDEN])
        val homeShortcuts = parseCsvList(p[Keys.HOME_SHORTCUTS]).take(3)
        val haptics = p[Keys.HAPTICS] ?: DEFAULT_PREFS.hapticsEnabled
        val hapticIntensity = (p[Keys.HAPTIC_INTENSITY] ?: DEFAULT_PREFS.hapticIntensity).coerceIn(1, 5)
        val notifBadges = p[Keys.NOTIFICATION_BADGES] ?: DEFAULT_PREFS.notificationBadgesEnabled
        val theme = p[Keys.THEME] ?: DEFAULT_THEME_JSON
        val glanceOn = p[Keys.GLANCE_ENABLED] ?: DEFAULT_PREFS.glanceEnabled
        val glanceFlash = p[Keys.GLANCE_FLASHLIGHT] ?: DEFAULT_PREFS.glanceShowFlashlight
        val glanceBat = p[Keys.GLANCE_BATTERY] ?: DEFAULT_PREFS.glanceShowBattery
        val glanceCal = p[Keys.GLANCE_CALENDAR] ?: DEFAULT_PREFS.glanceShowCalendar
        val glanceAlm = p[Keys.GLANCE_ALARM] ?: DEFAULT_PREFS.glanceShowAlarm
        val glanceCalendarRange =
            p[Keys.GLANCE_CALENDAR_RANGE]?.let { v -> GlanceCalendarRange.entries.firstOrNull { it.name == v } }
                ?: DEFAULT_PREFS.glanceCalendarRange
        val glanceWeatherUnit =
            p[Keys.GLANCE_WEATHER_UNIT]?.let { v -> GlanceWeatherUnit.entries.firstOrNull { it.name == v } }
                ?: DEFAULT_PREFS.glanceWeatherUnit
        val glanceWeatherLocationMode =
            p[Keys.GLANCE_WEATHER_LOCATION_MODE]?.let { v -> GlanceWeatherLocationMode.entries.firstOrNull { it.name == v } }
                ?: DEFAULT_PREFS.glanceWeatherLocationMode
        val glanceWeatherManualLat = p[Keys.GLANCE_WEATHER_MANUAL_LAT]?.trim() ?: ""
        val glanceWeatherManualLon = p[Keys.GLANCE_WEATHER_MANUAL_LON]?.trim() ?: ""
        val homeGroups = parseHomeGroupsJson(p[Keys.HOME_GROUPS]).normalizedAtMostTwo()
        val doubleTapSleep = p[Keys.DOUBLE_TAP_SLEEP] ?: DEFAULT_PREFS.doubleTapToSleepEnabled
        val swipeUpPkg = p[Keys.SWIPE_UP_PKG]?.trim() ?: ""
        val doubleTapPkg = p[Keys.DOUBLE_TAP_PKG]?.trim() ?: ""
        val showUsageStatsBadge = p[Keys.SHOW_USAGE_STATS_BADGE] ?: DEFAULT_PREFS.showUsageStatsBadge
        val showIconNotifBadge = p[Keys.SHOW_ICON_NOTIF_BADGE] ?: DEFAULT_PREFS.showIconNotifBadge
        val showShortcutApps = p[Keys.SHOW_SHORTCUT_APPS] ?: DEFAULT_PREFS.showShortcutApps
        val showHomeGroups = p[Keys.SHOW_HOME_GROUPS] ?: DEFAULT_PREFS.showHomeGroups
        val classicMode = p[Keys.CLASSIC_MODE] ?: p[Keys.CLASSIC_MODE_LEGACY] ?: DEFAULT_PREFS.classicMode
        val appIconShape =
            p[Keys.APP_ICON_SHAPE]?.let { v -> AppIconShape.entries.firstOrNull { it.name == v } }
                ?: DEFAULT_PREFS.appIconShape
        LauncherPrefs(
            gridPreset = grid,
            secondShortcutTarget = shortcut,
            mailBadgePackage = mailBadge,
            dockMailPackage = dockMail,
            dockSecondPackage = dockSecond,
            dockCameraPackage = dockCamera,
            dockMailTitle = dockMailTitle,
            dockSecondTitle = dockSecondTitle,
            dockThirdTitle = dockThirdTitle,
            dockIconStyle = dockIconStyle,
            orderedPackages = order,
            folderContents = folders,
            folderNames = folderNames,
            hiddenPackages = hidden,
            homeShortcutPackages = homeShortcuts,
            hapticsEnabled = haptics,
            hapticIntensity = hapticIntensity,
            notificationBadgesEnabled = notifBadges,
            themeJson = theme,
            glanceEnabled = glanceOn,
            glanceShowFlashlight = glanceFlash,
            glanceShowBattery = glanceBat,
            glanceShowCalendar = glanceCal,
            glanceShowAlarm = glanceAlm,
            glanceCalendarRange = glanceCalendarRange,
            glanceWeatherUnit = glanceWeatherUnit,
            glanceWeatherLocationMode = glanceWeatherLocationMode,
            glanceWeatherManualLatitude = glanceWeatherManualLat,
            glanceWeatherManualLongitude = glanceWeatherManualLon,
            homeGroups = homeGroups,
            doubleTapToSleepEnabled = doubleTapSleep,
            swipeUpPackage = swipeUpPkg,
            doubleTapPackage = doubleTapPkg,
            showUsageStatsBadge = showUsageStatsBadge,
            showIconNotifBadge = showIconNotifBadge,
            showShortcutApps = showShortcutApps,
            showHomeGroups = showHomeGroups,
            classicMode = classicMode,
            appIconShape = appIconShape,
        )
    }.distinctUntilChanged()

    suspend fun setGridPreset(preset: GridPreset) {
        context.dataStore.edit { it[Keys.GRID] = preset.name }
    }

    suspend fun setSecondShortcutTarget(target: SecondShortcutTarget) {
        context.dataStore.edit { it[Keys.SHORTCUT] = target.name }
    }

    suspend fun setMailBadgePackage(packageName: String) {
        context.dataStore.edit { it[Keys.MAIL_BADGE] = packageName.trim() }
    }

    suspend fun setDockMailPackage(packageName: String) {
        context.dataStore.edit { it[Keys.DOCK_MAIL] = packageName.trim() }
    }

    suspend fun setDockSecondPackage(packageName: String) {
        context.dataStore.edit { it[Keys.DOCK_SECOND] = packageName.trim() }
    }

    suspend fun setDockCameraPackage(packageName: String) {
        context.dataStore.edit { it[Keys.DOCK_CAMERA] = packageName.trim() }
    }

    suspend fun setDockMailTitle(title: String) {
        context.dataStore.edit { it[Keys.DOCK_MAIL_TITLE] = title.trim().ifEmpty { DEFAULT_PREFS.dockMailTitle } }
    }

    suspend fun setDockSecondTitle(title: String) {
        context.dataStore.edit { it[Keys.DOCK_SECOND_TITLE] = title.trim().ifEmpty { DEFAULT_PREFS.dockSecondTitle } }
    }

    suspend fun setDockThirdTitle(title: String) {
        context.dataStore.edit { it[Keys.DOCK_THIRD_TITLE] = title.trim().ifEmpty { DEFAULT_PREFS.dockThirdTitle } }
    }

    suspend fun setDockIconStyle(style: DockIconStyle) {
        context.dataStore.edit { it[Keys.DOCK_ICON_STYLE] = style.name }
    }

    suspend fun setHomeShortcutPackages(packages: List<String>) {
        val trimmed = packages.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(3)
        context.dataStore.edit { it[Keys.HOME_SHORTCUTS] = trimmed.joinToString(",") }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setHapticIntensity(intensity: Int) {
        context.dataStore.edit { it[Keys.HAPTIC_INTENSITY] = intensity.coerceIn(1, 5) }
    }

    suspend fun setNotificationBadgesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_BADGES] = enabled }
    }

    suspend fun setOrderedPackages(packages: List<String>) {
        context.dataStore.edit { it[Keys.ORDER] = packages.joinToString(",") }
    }

    suspend fun setFolderContents(folderContents: Map<String, List<String>>) {
        context.dataStore.edit { s ->
            s[Keys.FOLDERS] = folderContentsToJson(folderContents)
        }
    }

    suspend fun setOrderedPackagesAndFolders(
        orderedPackages: List<String>,
        folderContents: Map<String, List<String>>,
        folderNames: Map<String, String>,
    ) {
        writeGridState(orderedPackages, folderContents, folderNames)
    }

    suspend fun writeGridState(
        orderedPackages: List<String>,
        folderContents: Map<String, List<String>>,
        folderNames: Map<String, String>,
    ) {
        val prunedNames = folderNames.filterKeys { folderContents.containsKey(it) }
        context.dataStore.edit { s ->
            s[Keys.ORDER] = orderedPackages.joinToString(",")
            s[Keys.FOLDERS] = folderContentsToJson(folderContents)
            s[Keys.FOLDER_NAMES] = folderNamesToJson(prunedNames)
        }
    }

    suspend fun writeGridAndHomeGroupsState(
        orderedPackages: List<String>,
        folderContents: Map<String, List<String>>,
        folderNames: Map<String, String>,
        homeGroups: List<HomeGroup>,
    ) {
        val prunedNames = folderNames.filterKeys { folderContents.containsKey(it) }
        context.dataStore.edit { s ->
            s[Keys.ORDER] = orderedPackages.joinToString(",")
            s[Keys.FOLDERS] = folderContentsToJson(folderContents)
            s[Keys.FOLDER_NAMES] = folderNamesToJson(prunedNames)
            s[Keys.HOME_GROUPS] = homeGroupsToJson(homeGroups.normalizedAtMostTwo())
        }
    }

    suspend fun setHiddenPackages(packages: Set<String>) {
        context.dataStore.edit { it[Keys.HIDDEN] = packages.joinToString(",") }
    }

    suspend fun setHidden(packageName: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val current = parseCsvMutableSet(prefs[Keys.HIDDEN])
            if (hidden) current.add(packageName) else current.remove(packageName)
            prefs[Keys.HIDDEN] = current.joinToString(",")
        }
    }

    suspend fun setThemeJson(themeJson: String) {
        context.dataStore.edit { it[Keys.THEME] = themeJson }
    }

    suspend fun setGlanceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GLANCE_ENABLED] = enabled }
    }

    suspend fun setGlanceShowFlashlight(show: Boolean) {
        context.dataStore.edit { it[Keys.GLANCE_FLASHLIGHT] = show }
    }

    suspend fun setGlanceShowBattery(show: Boolean) {
        context.dataStore.edit { it[Keys.GLANCE_BATTERY] = show }
    }

    suspend fun setGlanceShowCalendar(show: Boolean) {
        context.dataStore.edit { it[Keys.GLANCE_CALENDAR] = show }
    }

    suspend fun setGlanceShowAlarm(show: Boolean) {
        context.dataStore.edit { it[Keys.GLANCE_ALARM] = show }
    }

    suspend fun setGlanceCalendarRange(range: GlanceCalendarRange) {
        context.dataStore.edit { it[Keys.GLANCE_CALENDAR_RANGE] = range.name }
    }

    suspend fun setGlanceWeatherUnit(unit: GlanceWeatherUnit) {
        context.dataStore.edit { it[Keys.GLANCE_WEATHER_UNIT] = unit.name }
    }

    suspend fun setGlanceWeatherLocationMode(mode: GlanceWeatherLocationMode) {
        context.dataStore.edit { it[Keys.GLANCE_WEATHER_LOCATION_MODE] = mode.name }
    }

    suspend fun setGlanceWeatherManualLatitude(latitude: String) {
        context.dataStore.edit { it[Keys.GLANCE_WEATHER_MANUAL_LAT] = latitude.trim() }
    }

    suspend fun setGlanceWeatherManualLongitude(longitude: String) {
        context.dataStore.edit { it[Keys.GLANCE_WEATHER_MANUAL_LON] = longitude.trim() }
    }

    suspend fun resetThemeJson() {
        context.dataStore.edit { it[Keys.THEME] = DEFAULT_THEME_JSON }
    }

    suspend fun setHomeGroups(groups: List<HomeGroup>) {
        context.dataStore.edit { s ->
            s[Keys.HOME_GROUPS] = homeGroupsToJson(groups.normalizedAtMostTwo())
        }
    }

    suspend fun setDoubleTapToSleepEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DOUBLE_TAP_SLEEP] = enabled }
    }

    suspend fun setSwipeUpPackage(pkg: String) {
        context.dataStore.edit { it[Keys.SWIPE_UP_PKG] = pkg.trim() }
    }

    suspend fun setDoubleTapPackage(pkg: String) {
        context.dataStore.edit { it[Keys.DOUBLE_TAP_PKG] = pkg.trim() }
    }

    suspend fun setShowUsageStatsBadge(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_USAGE_STATS_BADGE] = enabled }
    }

    suspend fun setShowIconNotifBadge(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_ICON_NOTIF_BADGE] = enabled }
    }

    suspend fun setShowShortcutApps(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SHORTCUT_APPS] = enabled }
    }

    suspend fun setShowHomeGroups(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_HOME_GROUPS] = enabled }
    }

    suspend fun setClassicMode(enabled: Boolean) {
        context.dataStore.edit { s ->
            s[Keys.CLASSIC_MODE] = enabled
            s.remove(Keys.CLASSIC_MODE_LEGACY)
        }
    }

    suspend fun setAppIconShape(shape: AppIconShape) {
        context.dataStore.edit { it[Keys.APP_ICON_SHAPE] = shape.name }
    }

    /** Replaces all launcher preferences in one atomic write (full backup restore). */
    suspend fun applyFullBackup(prefs: LauncherPrefs) {
        context.dataStore.edit { s ->
            s[Keys.GRID] = prefs.gridPreset.name
            s[Keys.SHORTCUT] = prefs.secondShortcutTarget.name
            s[Keys.MAIL_BADGE] = prefs.mailBadgePackage.trim()
            s[Keys.DOCK_MAIL] = prefs.dockMailPackage.trim()
            s[Keys.DOCK_SECOND] = prefs.dockSecondPackage.trim()
            s[Keys.DOCK_CAMERA] = prefs.dockCameraPackage.trim()
            s[Keys.DOCK_MAIL_TITLE] = prefs.dockMailTitle.trim().ifEmpty { DEFAULT_PREFS.dockMailTitle }
            s[Keys.DOCK_SECOND_TITLE] = prefs.dockSecondTitle.trim().ifEmpty { DEFAULT_PREFS.dockSecondTitle }
            s[Keys.DOCK_THIRD_TITLE] = prefs.dockThirdTitle.trim().ifEmpty { DEFAULT_PREFS.dockThirdTitle }
            s[Keys.DOCK_ICON_STYLE] = prefs.dockIconStyle.name
            s[Keys.ORDER] = prefs.orderedPackages.joinToString(",")
            s[Keys.FOLDERS] = folderContentsToJson(prefs.folderContents)
            s[Keys.FOLDER_NAMES] = folderNamesToJson(prefs.folderNames.filterKeys { prefs.folderContents.containsKey(it) })
            s[Keys.HIDDEN] = prefs.hiddenPackages.joinToString(",")
            s[Keys.HOME_SHORTCUTS] = prefs.homeShortcutPackages.take(3).joinToString(",")
            s[Keys.HAPTICS] = prefs.hapticsEnabled
            s[Keys.HAPTIC_INTENSITY] = prefs.hapticIntensity.coerceIn(1, 5)
            s[Keys.NOTIFICATION_BADGES] = prefs.notificationBadgesEnabled
            s[Keys.THEME] = prefs.themeJson
            s[Keys.GLANCE_ENABLED] = prefs.glanceEnabled
            s[Keys.GLANCE_FLASHLIGHT] = prefs.glanceShowFlashlight
            s[Keys.GLANCE_BATTERY] = prefs.glanceShowBattery
            s[Keys.GLANCE_CALENDAR] = prefs.glanceShowCalendar
            s[Keys.GLANCE_ALARM] = prefs.glanceShowAlarm
            s[Keys.GLANCE_CALENDAR_RANGE] = prefs.glanceCalendarRange.name
            s[Keys.GLANCE_WEATHER_UNIT] = prefs.glanceWeatherUnit.name
            s[Keys.GLANCE_WEATHER_LOCATION_MODE] = prefs.glanceWeatherLocationMode.name
            s[Keys.GLANCE_WEATHER_MANUAL_LAT] = prefs.glanceWeatherManualLatitude.trim()
            s[Keys.GLANCE_WEATHER_MANUAL_LON] = prefs.glanceWeatherManualLongitude.trim()
            s[Keys.HOME_GROUPS] = homeGroupsToJson(prefs.homeGroups)
            s[Keys.DOUBLE_TAP_SLEEP] = prefs.doubleTapToSleepEnabled
            s[Keys.SWIPE_UP_PKG] = prefs.swipeUpPackage
            s[Keys.DOUBLE_TAP_PKG] = prefs.doubleTapPackage
            s[Keys.SHOW_USAGE_STATS_BADGE] = prefs.showUsageStatsBadge
            s[Keys.SHOW_ICON_NOTIF_BADGE] = prefs.showIconNotifBadge
            s[Keys.SHOW_SHORTCUT_APPS] = prefs.showShortcutApps
            s[Keys.SHOW_HOME_GROUPS] = prefs.showHomeGroups
            s[Keys.CLASSIC_MODE] = prefs.classicMode
            s.remove(Keys.CLASSIC_MODE_LEGACY)
            s[Keys.APP_ICON_SHAPE] = prefs.appIconShape.name
        }
    }
}

private fun parseCsvList(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    val result = ArrayList<String>()
    var start = 0
    val len = raw.length
    while (start < len) {
        val end = raw.indexOf(',', start)
        val token = if (end < 0) raw.substring(start).trim() else raw.substring(start, end).trim()
        if (token.isNotEmpty()) result.add(token)
        if (end < 0) break
        start = end + 1
    }
    return result
}

private fun parseCsvSet(raw: String?): Set<String> {
    if (raw.isNullOrBlank()) return emptySet()
    val result = HashSet<String>()
    var start = 0
    val len = raw.length
    while (start < len) {
        val end = raw.indexOf(',', start)
        val token = if (end < 0) raw.substring(start).trim() else raw.substring(start, end).trim()
        if (token.isNotEmpty()) result.add(token)
        if (end < 0) break
        start = end + 1
    }
    return result
}

private fun parseCsvMutableSet(raw: String?): MutableSet<String> {
    if (raw.isNullOrBlank()) return mutableSetOf()
    val result = HashSet<String>()
    var start = 0
    val len = raw.length
    while (start < len) {
        val end = raw.indexOf(',', start)
        val token = if (end < 0) raw.substring(start).trim() else raw.substring(start, end).trim()
        if (token.isNotEmpty()) result.add(token)
        if (end < 0) break
        start = end + 1
    }
    return result
}

private fun parseFolderContentsJson(raw: String?): Map<String, List<String>> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
        val o = JSONObject(raw.trim())
        buildMap {
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = o.getJSONArray(k)
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        add(arr.getString(i).trim())
                    }
                }.filter { it.isNotEmpty() }
                if (list.isNotEmpty()) put(k, list)
            }
        }
    }.getOrDefault(emptyMap())
}

private fun folderContentsToJson(folderContents: Map<String, List<String>>): String {
    val o = JSONObject()
    folderContents.forEach { (k, v) ->
        if (v.isNotEmpty()) o.put(k, JSONArray(v))
    }
    return o.toString()
}

private fun parseFolderNamesJson(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
        val o = JSONObject(raw.trim())
        buildMap {
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = o.optString(k, "").trim()
                if (v.isNotEmpty()) put(k, v)
            }
        }
    }.getOrDefault(emptyMap())
}

private fun folderNamesToJson(folderNames: Map<String, String>): String {
    val o = JSONObject()
    folderNames.forEach { (k, v) ->
        if (v.isNotEmpty()) o.put(k, v)
    }
    return o.toString()
}

private fun parseHomeGroupsJson(raw: String?): List<HomeGroup> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw.trim())
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id", "").trim()
                val title = o.optString("title", "").trim()
                if (id.isEmpty() || title.isEmpty()) continue
                val pkgs = o.optJSONArray("packages") ?: JSONArray()
                val names = buildList {
                    for (j in 0 until pkgs.length()) add(pkgs.getString(j).trim())
                }.filter { it.isNotEmpty() }.distinct()
                val side = HomeGroupSide.fromStored(o.optString("side", "").trim())
                add(HomeGroup(id = id, title = title, packageNames = names, side = side))
            }
        }
    }.getOrDefault(emptyList())
}

private fun homeGroupsToJson(groups: List<HomeGroup>): String {
    val arr = JSONArray()
    groups.normalizedAtMostTwo().forEach { g ->
        val o = JSONObject()
        o.put("id", g.id)
        o.put("title", g.title)
        o.put("packages", JSONArray(g.packageNames))
        o.put("side", g.side.name)
        arr.put(o)
    }
    return arr.toString()
}

