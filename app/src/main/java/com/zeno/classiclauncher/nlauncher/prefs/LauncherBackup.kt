package com.zeno.classiclauncher.nlauncher.prefs

import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Single-file backup of all launcher DataStore prefs (grid, shortcuts, order, hidden, theme).
 * Not the same as separate app-order-only or theme-only exports.
 */
object LauncherBackup {
    const val FORMAT_KEY = "format"
    const val FORMAT_VALUE = "classiclauncher_backup"
    const val VERSION_KEY = "version"
    const val CURRENT_VERSION = 25

    private object PrefKey {
        const val CUSTOM_QUICK_SETTINGS_ENABLED = "customQuickSettingsEnabled"
        const val QUICK_SETTINGS_QR_SCANNER_PACKAGE = "quickSettingsQrScannerPackage"
        const val QUICK_SETTINGS_TILE_ORDER = "quickSettingsTileOrder"
    }

    fun toJson(prefs: LauncherPrefs): String {
        val root = JSONObject()
        root.put(FORMAT_KEY, FORMAT_VALUE)
        root.put(VERSION_KEY, CURRENT_VERSION)
        root.put("exportedAt", Instant.now().toString())
        val p = JSONObject()
        p.put("gridPreset", prefs.gridPreset.name)
        p.put("secondShortcut", prefs.secondShortcutTarget.name)
        p.put("mailBadgePackage", prefs.mailBadgePackage)
        p.put("dockMailPackage", prefs.dockMailPackage)
        p.put("dockSecondPackage", prefs.dockSecondPackage)
        p.put("dockCameraPackage", prefs.dockCameraPackage)
        p.put("dockMailTitle", prefs.dockMailTitle)
        p.put("dockSecondTitle", prefs.dockSecondTitle)
        p.put("dockThirdTitle", prefs.dockThirdTitle)
        p.put("dockSecondEnabled", prefs.dockSecondEnabled)
        p.put("orderedPackages", JSONArray(prefs.orderedPackages))
        val folders = JSONObject()
        prefs.folderContents.forEach { (id, pkgs) ->
            if (pkgs.isNotEmpty()) folders.put(id, JSONArray(pkgs))
        }
        p.put("folderContents", folders)
        val fn = JSONObject()
        prefs.folderNames.forEach { (id, name) ->
            if (name.isNotEmpty()) fn.put(id, name)
        }
        p.put("folderNames", fn)
        p.put("homePinnedFolderIds", JSONArray(prefs.homePinnedFolderIds))
        p.put("hiddenPackages", JSONArray(prefs.hiddenPackages.toList()))
        p.put("homeShortcutPackages", JSONArray(prefs.homeShortcutPackages))
        p.put("hapticsEnabled", prefs.hapticsEnabled)
        p.put("hapticIntensity", prefs.hapticIntensity)
        p.put("notificationBadgesEnabled", prefs.notificationBadgesEnabled)
        p.put("themeJson", prefs.themeJson)
        p.put("glanceEnabled", prefs.glanceEnabled)
        p.put("glanceShowFlashlight", prefs.glanceShowFlashlight)
        p.put("glanceShowBattery", prefs.glanceShowBattery)
        p.put("glanceShowCalendar", prefs.glanceShowCalendar)
        p.put("glanceShowAlarm", prefs.glanceShowAlarm)
        p.put("glanceShowSoundProfile", prefs.glanceShowSoundProfile)
        p.put("glanceCalendarRange", prefs.glanceCalendarRange.name)
        p.put("glanceWeatherUnit", prefs.glanceWeatherUnit.name)
        p.put("glanceWeatherLocationMode", prefs.glanceWeatherLocationMode.name)
        p.put("glanceWeatherManualLatitude", prefs.glanceWeatherManualLatitude)
        p.put("glanceWeatherManualLongitude", prefs.glanceWeatherManualLongitude)
        p.put("glanceWeatherManualCityName", prefs.glanceWeatherManualCityName)
        val homeGroupsArr = JSONArray()
        prefs.homeGroups.forEach { g ->
            val o = JSONObject()
            o.put("id", g.id)
            o.put("title", g.title)
            o.put("packages", JSONArray(g.packageNames))
            o.put("side", g.side.name)
            homeGroupsArr.put(o)
        }
        p.put("homeGroups", homeGroupsArr)
        p.put("homeStripOrder", JSONArray(prefs.homeStripOrder))
        p.put("homeStripSlots", JSONArray(prefs.homeStripSlots.map { it ?: "" }))
        JSONObject().also { w ->
            w.put("providerPackage", prefs.homeWidget.providerPackage)
            w.put("providerClass", prefs.homeWidget.providerClass)
            w.put("row", prefs.homeWidget.row)
            w.put("col", prefs.homeWidget.col)
            w.put("cols", prefs.homeWidget.cols)
            w.put("rows", prefs.homeWidget.rows)
            p.put("homeWidget", w)
        }
        p.put("doubleTapToSleepEnabled", prefs.doubleTapToSleepEnabled)
        p.put("swipeUpPackage", prefs.swipeUpPackage)
        p.put("swipeRightPackage", prefs.swipeRightPackage)
        p.put("doubleTapPackage", prefs.doubleTapPackage)
        p.put("showUsageStatsBadge", prefs.showUsageStatsBadge)
        p.put("showIconNotifBadge", prefs.showIconNotifBadge)
        p.put("showShortcutApps", prefs.showShortcutApps)
        p.put("showHomeGroups", prefs.showHomeGroups)
        p.put(PrefKey.CUSTOM_QUICK_SETTINGS_ENABLED, prefs.customQuickSettingsEnabled)
        p.put(PrefKey.QUICK_SETTINGS_QR_SCANNER_PACKAGE, prefs.quickSettingsQrScannerPackage)
        p.put(PrefKey.QUICK_SETTINGS_TILE_ORDER, JSONArray(prefs.quickSettingsTileOrder))
        p.put("classicMode", prefs.classicMode)
        p.put("minimalModeEnabled", prefs.minimalModeEnabled)
        p.put("minimalModeLayout", prefs.minimalModeLayout.name)
        p.put("minimalModeMaxApps", prefs.minimalModeMaxApps.name)
        p.put("minimalModeShowIcons", prefs.minimalModeShowIcons)
        p.put("minimalModeShowWeather", prefs.minimalModeShowWeather)
        p.put("minimalModeShowNotifSummary", prefs.minimalModeShowNotifSummary)
        p.put("minimalModeApps", JSONArray(prefs.minimalModeApps))
        p.put("minimalModeGreyscale", prefs.minimalModeGreyscale)
        p.put("minimalModeChallengeApps", JSONArray(prefs.minimalModeChallengeApps.toList()))
        p.put("minimalModeAppLimits", prefs.minimalModeAppLimits)
        p.put("minimalModeSwipeRightApp", prefs.minimalModeSwipeRightApp)
        p.put("autoUnlockEnabled", prefs.autoUnlockEnabled)
        p.put("autoUnlockPinDigits", prefs.autoUnlockPinDigits)
        p.put("drawerSortMode", prefs.drawerSortMode)
        p.put("appIconShape", prefs.appIconShape.name)
        p.put("iconPackPackage", prefs.iconPackPackage)
        p.put("showAppCardBackground", prefs.showAppCardBackground)
        p.put("swipeDownAppSpotlight", prefs.swipeDownAppSpotlight)
        p.put("languageCode", prefs.languageCode)
        root.put("prefs", p)
        return root.toString(2)
    }

    /**
     * Parses and validates backup JSON. On success, [LauncherPrefs.themeJson] is validated via [LauncherThemePalette.fromJson].
     */
    fun fromJson(json: String): Result<LauncherPrefs> = runCatching {
        val root = JSONObject(json.trim())
        require(root.optString(FORMAT_KEY) == FORMAT_VALUE) { "Not a Zeno Classic backup file" }
        val ver = root.optInt(VERSION_KEY, 1)
        require(ver in 1..CURRENT_VERSION) { "Unsupported backup version" }
        val p = root.getJSONObject("prefs")
        val grid = p.getString("gridPreset").let { name ->
            GridPreset.entries.firstOrNull { it.name == name }
                ?: throw IllegalArgumentException("Invalid gridPreset: $name")
        }
        val shortcut = p.getString("secondShortcut").let { name ->
            SecondShortcutTarget.entries.firstOrNull { it.name == name }
                ?: throw IllegalArgumentException("Invalid secondShortcut: $name")
        }
        val mail = p.optString("mailBadgePackage", "").trim()
        val dockMail = p.optString("dockMailPackage", "").trim()
        val dockSecond = p.optString("dockSecondPackage", "").trim()
        val dockCam = p.optString("dockCameraPackage", "").trim()
        val dockMailTitle = p.optString("dockMailTitle", "Mail").trim().ifEmpty { "Mail" }
        val dockSecondTitle = p.optString("dockSecondTitle", "Messages").trim().ifEmpty { "Messages" }
        val dockThirdTitle = p.optString("dockThirdTitle", "Camera").trim().ifEmpty { "Camera" }
        val dockSecondEnabled = p.optBoolean("dockSecondEnabled", true)
        val orderArr = p.getJSONArray("orderedPackages")
        val ordered = buildList {
            for (i in 0 until orderArr.length()) add(orderArr.getString(i).trim())
        }.filter { it.isNotEmpty() }
        val folderContents = buildMap<String, List<String>> {
            val fo = p.optJSONObject("folderContents") ?: return@buildMap
            val keys = fo.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val arr = fo.getJSONArray(id)
                val members = buildList {
                    for (i in 0 until arr.length()) add(arr.getString(i).trim())
                }.filter { it.isNotEmpty() }
                if (members.isNotEmpty()) put(id, members)
            }
        }
        val folderNames = runCatching {
            val fo = p.optJSONObject("folderNames") ?: return@runCatching emptyMap()
            buildMap {
                val keys = fo.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val n = fo.optString(id, "").trim()
                    if (n.isNotEmpty()) put(id, n)
                }
            }
        }.getOrDefault(emptyMap())
        val hiddenArr = p.optJSONArray("hiddenPackages") ?: JSONArray()
        val hidden = buildList {
            for (i in 0 until hiddenArr.length()) add(hiddenArr.getString(i).trim())
        }.filter { it.isNotEmpty() }.toSet()
        val homeShortcutArr = p.optJSONArray("homeShortcutPackages")
        val homeShortcuts = if (homeShortcutArr != null) {
            buildList {
                for (i in 0 until homeShortcutArr.length()) add(homeShortcutArr.getString(i).trim())
            }.filter { it.isNotEmpty() }.distinct()
        } else {
            emptyList()
        }
        val homePinnedFolderArr = p.optJSONArray("homePinnedFolderIds")
        val homePinnedFolderIds = if (homePinnedFolderArr != null) {
            buildList {
                for (i in 0 until homePinnedFolderArr.length()) add(homePinnedFolderArr.getString(i).trim())
            }.filter { it.isNotEmpty() }.distinct()
        } else {
            emptyList()
        }
        val theme = p.getString("themeJson").trim().ifEmpty { DEFAULT_THEME_JSON }
        val haptics = p.optBoolean("hapticsEnabled", true)
        val hapticIntensity = p.optInt("hapticIntensity", 3).coerceIn(1, 5)
        val notifBadges = p.optBoolean("notificationBadgesEnabled", true)
        val glanceEnabled = p.optBoolean("glanceEnabled", true)
        val glanceFlash = p.optBoolean("glanceShowFlashlight", true)
        val glanceBat = p.optBoolean("glanceShowBattery", true)
        val glanceCal = p.optBoolean("glanceShowCalendar", true)
        val glanceAlm = p.optBoolean("glanceShowAlarm", true)
        val glanceSound = p.optBoolean("glanceShowSoundProfile", true)
        val glanceCalendarRange = p.optString("glanceCalendarRange", "DAY").let { name ->
            GlanceCalendarRange.entries.firstOrNull { it.name == name } ?: GlanceCalendarRange.DAY
        }
        val glanceWeatherUnit = p.optString("glanceWeatherUnit", "CELSIUS").let { name ->
            GlanceWeatherUnit.entries.firstOrNull { it.name == name } ?: GlanceWeatherUnit.CELSIUS
        }
        val glanceWeatherLocationMode = p.optString("glanceWeatherLocationMode", "DEVICE").let { name ->
            GlanceWeatherLocationMode.entries.firstOrNull { it.name == name } ?: GlanceWeatherLocationMode.DEVICE
        }
        val glanceWeatherManualLatitude = p.optString("glanceWeatherManualLatitude", "").trim()
        val glanceWeatherManualLongitude = p.optString("glanceWeatherManualLongitude", "").trim()
        val glanceWeatherManualCityName = p.optString("glanceWeatherManualCityName", "").trim()
        val doubleTapSleep = p.optBoolean("doubleTapToSleepEnabled", true)
        val swipeUpPackage = p.optString("swipeUpPackage", "").trim()
        val swipeRightPackage = p.optString("swipeRightPackage", "").trim()
        val doubleTapPackage = p.optString("doubleTapPackage", "").trim()
        val showUsageStatsBadge = p.optBoolean("showUsageStatsBadge", true)
        val showIconNotifBadge = p.optBoolean("showIconNotifBadge", true)
        // Legacy backup compat: if old "showHomeStrip" exists and is false, default both new toggles to false
        val legacyShowHomeStrip = if (p.has("showHomeStrip")) p.optBoolean("showHomeStrip", true) else null
        val showShortcutApps = p.optBoolean("showShortcutApps", legacyShowHomeStrip ?: true)
        val showHomeGroups = p.optBoolean("showHomeGroups", legacyShowHomeStrip ?: true)
        val customQuickSettingsEnabled = p.optBoolean(PrefKey.CUSTOM_QUICK_SETTINGS_ENABLED, false)
        val quickSettingsQrScannerPackage = p.optString(PrefKey.QUICK_SETTINGS_QR_SCANNER_PACKAGE, "").trim()
        val quickSettingsTileOrder = p.optJSONArray(PrefKey.QUICK_SETTINGS_TILE_ORDER)?.let { arr ->
            buildList {
                for (i in 0 until arr.length()) add(arr.optString(i, "").trim())
            }.filter { it.isNotEmpty() }.distinct()
        } ?: emptyList()
        val classicMode = when {
            p.has("classicMode") -> p.optBoolean("classicMode", false)
            else -> p.optBoolean("drawerOnlyMode", false)
        }
        val minimalModeEnabled = p.optBoolean("minimalModeEnabled", false)
        val minimalModeLayout = p.optString("minimalModeLayout", "LIST").let { name ->
            MinimalModeLayout.entries.firstOrNull { it.name == name } ?: MinimalModeLayout.LIST
        }
        val minimalModeMaxApps = p.optString("minimalModeMaxApps", "AUTO").let { name ->
            MinimalModeMaxApps.entries.firstOrNull { it.name == name } ?: MinimalModeMaxApps.AUTO
        }
        val minimalModeShowIcons = p.optBoolean("minimalModeShowIcons", true)
        val minimalModeShowWeather = p.optBoolean("minimalModeShowWeather", true)
        val minimalModeShowNotifSummary = p.optBoolean("minimalModeShowNotifSummary", true)
        val minimalModeApps = p.optJSONArray("minimalModeApps")?.let { arr ->
            buildList { for (i in 0 until arr.length()) add(arr.optString(i, "").trim()) }
                .filter { it.isNotEmpty() }
        } ?: emptyList()
        val minimalModeGreyscale = p.optBoolean("minimalModeGreyscale", true)
        val minimalModeChallengeApps = p.optJSONArray("minimalModeChallengeApps")?.let { arr ->
            buildList { for (i in 0 until arr.length()) add(arr.optString(i, "").trim()) }
                .filter { it.isNotEmpty() }.toSet()
        } ?: emptySet()
        val minimalModeAppLimits = p.optString("minimalModeAppLimits", "").trim()
        val minimalModeSwipeRightApp = p.optString("minimalModeSwipeRightApp", "").trim()
        val autoUnlockEnabled = p.optBoolean("autoUnlockEnabled", true)
        val autoUnlockPinDigits = p.optInt("autoUnlockPinDigits", 4).coerceIn(4, 8)
        val drawerSortMode = p.optString("drawerSortMode", "ALPHABETICAL").trim()
        val appIconShape =
            p.optString("appIconShape", "").let { name ->
                AppIconShape.entries.firstOrNull { it.name == name } ?: AppIconShape.SOFT_SQUARE
            }
        val iconPackPackage = p.optString("iconPackPackage", "").trim()
        val showAppCardBackground = p.optBoolean("showAppCardBackground", false)
        val swipeDownAppSpotlight = p.optBoolean("swipeDownAppSpotlight", false)
        val languageCode = p.optString("languageCode", "").trim()
        val homeGroups = run {
            val arr = p.optJSONArray("homeGroups") ?: return@run emptyList<HomeGroup>()
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
                    add(
                        HomeGroup(
                            id = id,
                            title = title,
                            packageNames = names,
                            side = side,
                        ),
                    )
                }
            }
        }
        val homeStripOrder = p.optJSONArray("homeStripOrder")?.let { arr ->
            buildList {
                for (i in 0 until arr.length()) add(arr.optString(i, "").trim())
            }.filter { it.isNotEmpty() }.distinct()
        } ?: emptyList()
        val homeStripSlots = p.optJSONArray("homeStripSlots")?.let { arr ->
            buildList {
                for (i in 0 until arr.length()) {
                    val token = arr.optString(i, "").trim()
                    add(token.ifEmpty { null })
                }
            }
        } ?: emptyList()
        val homeWidget = p.optJSONObject("homeWidget")?.let { w ->
            HomeWidgetConfig(
                appWidgetId = -1,
                providerPackage = w.optString("providerPackage", "").trim(),
                providerClass = w.optString("providerClass", "").trim(),
                row = w.optInt("row", 1).coerceIn(0, 3),
                col = w.optInt("col", 0).coerceIn(0, 3),
                cols = w.optInt("cols", 4).coerceIn(1, 4),
                rows = w.optInt("rows", 2).coerceIn(1, 4),
            ).let { cfg ->
                cfg.copy(
                    row = cfg.row.coerceAtMost(4 - cfg.rows),
                    col = cfg.col.coerceAtMost(4 - cfg.cols),
                )
            }
        } ?: HomeWidgetConfig()
        LauncherThemePalette.fromJson(theme)
        LauncherPrefs(
            gridPreset = grid,
            secondShortcutTarget = shortcut,
            mailBadgePackage = mail,
            dockMailPackage = dockMail,
            dockSecondPackage = dockSecond,
            dockCameraPackage = dockCam,
            dockMailTitle = dockMailTitle,
            dockSecondTitle = dockSecondTitle,
            dockThirdTitle = dockThirdTitle,
            dockSecondEnabled = dockSecondEnabled,
            orderedPackages = ordered,
            folderContents = folderContents,
            folderNames = folderNames.filterKeys { folderContents.containsKey(it) },
            homePinnedFolderIds = homePinnedFolderIds,
            hiddenPackages = hidden,
            homeShortcutPackages = homeShortcuts,
            hapticsEnabled = haptics,
            hapticIntensity = hapticIntensity,
            notificationBadgesEnabled = notifBadges,
            themeJson = theme,
            glanceEnabled = glanceEnabled,
            glanceShowFlashlight = glanceFlash,
            glanceShowBattery = glanceBat,
            glanceShowCalendar = glanceCal,
            glanceShowAlarm = glanceAlm,
            glanceShowSoundProfile = glanceSound,
            glanceCalendarRange = glanceCalendarRange,
            glanceWeatherUnit = glanceWeatherUnit,
            glanceWeatherLocationMode = glanceWeatherLocationMode,
            glanceWeatherManualLatitude = glanceWeatherManualLatitude,
            glanceWeatherManualLongitude = glanceWeatherManualLongitude,
            glanceWeatherManualCityName = glanceWeatherManualCityName,
            homeGroups = homeGroups,
            homeStripOrder = homeStripOrder,
            homeStripSlots = homeStripSlots,
            homeWidget = homeWidget,
            doubleTapToSleepEnabled = doubleTapSleep,
            swipeUpPackage = swipeUpPackage,
            swipeRightPackage = swipeRightPackage,
            doubleTapPackage = doubleTapPackage,
            showUsageStatsBadge = showUsageStatsBadge,
            showIconNotifBadge = showIconNotifBadge,
            showShortcutApps = showShortcutApps,
            showHomeGroups = showHomeGroups,
            customQuickSettingsEnabled = customQuickSettingsEnabled,
            quickSettingsQrScannerPackage = quickSettingsQrScannerPackage,
            quickSettingsTileOrder = quickSettingsTileOrder,
            classicMode = classicMode,
            minimalModeEnabled = minimalModeEnabled,
            minimalModeLayout = minimalModeLayout,
            minimalModeMaxApps = minimalModeMaxApps,
            minimalModeShowIcons = minimalModeShowIcons,
            minimalModeShowWeather = minimalModeShowWeather,
            minimalModeShowNotifSummary = minimalModeShowNotifSummary,
            minimalModeApps = minimalModeApps,
            minimalModeGreyscale = minimalModeGreyscale,
            minimalModeChallengeApps = minimalModeChallengeApps,
            minimalModeAppLimits = minimalModeAppLimits,
            minimalModeSwipeRightApp = minimalModeSwipeRightApp,
            autoUnlockEnabled = autoUnlockEnabled,
            autoUnlockPinDigits = autoUnlockPinDigits,
            drawerSortMode = drawerSortMode,
            appIconShape = appIconShape,
            iconPackPackage = iconPackPackage,
            showAppCardBackground = showAppCardBackground,
            swipeDownAppSpotlight = swipeDownAppSpotlight,
            languageCode = languageCode,
        )
    }
}
