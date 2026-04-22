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
    const val CURRENT_VERSION = 19

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
        p.put("dockIconStyle", prefs.dockIconStyle.name)
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
        p.put("glanceCalendarRange", prefs.glanceCalendarRange.name)
        p.put("glanceWeatherUnit", prefs.glanceWeatherUnit.name)
        p.put("glanceWeatherLocationMode", prefs.glanceWeatherLocationMode.name)
        p.put("glanceWeatherManualLatitude", prefs.glanceWeatherManualLatitude)
        p.put("glanceWeatherManualLongitude", prefs.glanceWeatherManualLongitude)
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
        p.put("doubleTapToSleepEnabled", prefs.doubleTapToSleepEnabled)
        p.put("swipeUpPackage", prefs.swipeUpPackage)
        p.put("doubleTapPackage", prefs.doubleTapPackage)
        p.put("showUsageStatsBadge", prefs.showUsageStatsBadge)
        p.put("showIconNotifBadge", prefs.showIconNotifBadge)
        p.put("showShortcutApps", prefs.showShortcutApps)
        p.put("showHomeGroups", prefs.showHomeGroups)
        p.put("customQuickSettingsEnabled", prefs.customQuickSettingsEnabled)
        p.put("classicMode", prefs.classicMode)
        p.put("appIconShape", prefs.appIconShape.name)
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
        val dockIconStyle = p.optString("dockIconStyle", "MONOCHROME").let { name ->
            DockIconStyle.entries.firstOrNull { it.name == name } ?: DockIconStyle.MONOCHROME
        }
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
        val theme = p.getString("themeJson").trim().ifEmpty { DEFAULT_THEME_JSON }
        val haptics = p.optBoolean("hapticsEnabled", true)
        val hapticIntensity = p.optInt("hapticIntensity", 3).coerceIn(1, 5)
        val notifBadges = p.optBoolean("notificationBadgesEnabled", true)
        val glanceEnabled = p.optBoolean("glanceEnabled", true)
        val glanceFlash = p.optBoolean("glanceShowFlashlight", true)
        val glanceBat = p.optBoolean("glanceShowBattery", true)
        val glanceCal = p.optBoolean("glanceShowCalendar", true)
        val glanceAlm = p.optBoolean("glanceShowAlarm", true)
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
        val doubleTapSleep = p.optBoolean("doubleTapToSleepEnabled", true)
        val swipeUpPackage = p.optString("swipeUpPackage", "").trim()
        val doubleTapPackage = p.optString("doubleTapPackage", "").trim()
        val showUsageStatsBadge = p.optBoolean("showUsageStatsBadge", true)
        val showIconNotifBadge = p.optBoolean("showIconNotifBadge", true)
        // Legacy backup compat: if old "showHomeStrip" exists and is false, default both new toggles to false
        val legacyShowHomeStrip = if (p.has("showHomeStrip")) p.optBoolean("showHomeStrip", true) else null
        val showShortcutApps = p.optBoolean("showShortcutApps", legacyShowHomeStrip ?: true)
        val showHomeGroups = p.optBoolean("showHomeGroups", legacyShowHomeStrip ?: true)
        val customQuickSettingsEnabled = p.optBoolean("customQuickSettingsEnabled", false)
        val classicMode = when {
            p.has("classicMode") -> p.optBoolean("classicMode", false)
            else -> p.optBoolean("drawerOnlyMode", false)
        }
        val appIconShape =
            p.optString("appIconShape", "").let { name ->
                AppIconShape.entries.firstOrNull { it.name == name } ?: AppIconShape.ROUNDED
            }
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
            dockIconStyle = dockIconStyle,
            orderedPackages = ordered,
            folderContents = folderContents,
            folderNames = folderNames.filterKeys { folderContents.containsKey(it) },
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
            glanceCalendarRange = glanceCalendarRange,
            glanceWeatherUnit = glanceWeatherUnit,
            glanceWeatherLocationMode = glanceWeatherLocationMode,
            glanceWeatherManualLatitude = glanceWeatherManualLatitude,
            glanceWeatherManualLongitude = glanceWeatherManualLongitude,
            homeGroups = homeGroups,
            doubleTapToSleepEnabled = doubleTapSleep,
            swipeUpPackage = swipeUpPackage,
            doubleTapPackage = doubleTapPackage,
            showUsageStatsBadge = showUsageStatsBadge,
            showIconNotifBadge = showIconNotifBadge,
            showShortcutApps = showShortcutApps,
            showHomeGroups = showHomeGroups,
            customQuickSettingsEnabled = customQuickSettingsEnabled,
            classicMode = classicMode,
            appIconShape = appIconShape,
        )
    }
}
