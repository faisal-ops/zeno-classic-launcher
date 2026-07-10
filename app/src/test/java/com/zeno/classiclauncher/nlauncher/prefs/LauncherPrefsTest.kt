package com.zeno.classiclauncher.nlauncher.prefs

import com.zeno.classiclauncher.nlauncher.apps.homeShortcutStorageToken
import com.zeno.classiclauncher.nlauncher.apps.parseHomeShortcutToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPrefsTest {
    @Test
    fun moveHomeStripSlot_shiftsItemsForward() {
        val slots = mutableListOf<String?>("mail", "tools", "browser", null, "music")

        slots.moveHomeStripSlot(fromIdx = 0, toIdx = 3)

        assertEquals(listOf("tools", "browser", null, "mail", "music"), slots)
    }

    @Test
    fun moveHomeStripSlot_shiftsItemsBackward() {
        val slots = mutableListOf<String?>("mail", "tools", "browser", null, "music")

        slots.moveHomeStripSlot(fromIdx = 4, toIdx = 1)

        assertEquals(listOf("mail", "music", "tools", "browser", null), slots)
    }

    @Test
    fun launcherBackup_roundTripsWidgetProviderButRequiresRebinding() {
        val prefs = LauncherPrefs(
            iconPackPackage = "com.example.iconpack",
            homeWidgets = listOf(
                HomeWidgetConfig(
                    appWidgetId = 42,
                    providerPackage = "com.example",
                    providerClass = "com.example.Widget",
                    row = 1,
                    col = 1,
                    cols = 2,
                    rows = 2,
                ),
                HomeWidgetConfig(
                    appWidgetId = 43,
                    providerPackage = "com.example.two",
                    providerClass = "com.example.two.Widget",
                    row = 0,
                    col = 0,
                    cols = 1,
                    rows = 1,
                ),
            ),
            quickSettingsTileOrder = listOf("battery", "internet"),
        )

        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()

        assertEquals(2, restored.homeWidgets.size)
        assertFalse(restored.homeWidgets[0].hasWidget)
        assertEquals("com.example", restored.homeWidgets[0].providerPackage)
        assertEquals("com.example.Widget", restored.homeWidgets[0].providerClass)
        assertEquals(1, restored.homeWidgets[0].row)
        assertEquals(1, restored.homeWidgets[0].col)
        assertEquals(2, restored.homeWidgets[0].cols)
        assertEquals(2, restored.homeWidgets[0].rows)
        assertEquals("com.example.two", restored.homeWidgets[1].providerPackage)
        assertEquals("com.example.iconpack", restored.iconPackPackage)
        assertEquals(listOf("battery", "internet"), restored.quickSettingsTileOrder)
    }

    @Test
    fun homeWidgetsJson_roundTripsMultipleWidgets() {
        val widgets = listOf(
            HomeWidgetConfig(appWidgetId = 1, providerPackage = "a.pkg", providerClass = "a.Cls", row = 0, col = 0, cols = 2, rows = 1),
            HomeWidgetConfig(appWidgetId = 2, providerPackage = "b.pkg", providerClass = "b.Cls", row = 2, col = 1, cols = 1, rows = 2),
        )

        val restored = parseHomeWidgetsJson(homeWidgetsToJson(widgets))

        assertEquals(widgets, restored)
    }

    @Test
    fun parseHomeWidgetsJson_dropsEntriesWithoutAppWidgetId() {
        val restored = parseHomeWidgetsJson("""[{"providerPackage":"a.pkg"},{"appWidgetId":5,"providerPackage":"b.pkg"}]""")

        assertEquals(1, restored.size)
        assertEquals(5, restored[0].appWidgetId)
    }

    @Test
    fun launcherBackup_rejectsUnknownFutureVersions() {
        val json = LauncherBackup.toJson(LauncherPrefs())
            .replace(
                "\"${LauncherBackup.VERSION_KEY}\": ${LauncherBackup.CURRENT_VERSION}",
                "\"${LauncherBackup.VERSION_KEY}\": ${LauncherBackup.CURRENT_VERSION + 1}",
            )

        assertTrue(LauncherBackup.fromJson(json).isFailure)
    }

    @Test
    fun launcherBackup_roundTripsMinimalModePrefs() {
        val prefs = LauncherPrefs(
            minimalModeEnabled = true,
            minimalModeShowWeather = false,
            minimalModeGreyscale = true,
            minimalModeApps = listOf("com.android.dialer", "com.android.mms"),
        )

        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()

        assertTrue(restored.minimalModeEnabled)
        assertFalse(restored.minimalModeShowWeather)
        assertTrue(restored.minimalModeGreyscale)
        assertEquals(listOf("com.android.dialer", "com.android.mms"), restored.minimalModeApps)
    }

    @Test
    fun launcherBackup_roundTripsGpsCoordinates() {
        val prefs = LauncherPrefs(
            glanceWeatherManualLatitude = "51.5074",
            glanceWeatherManualLongitude = "-0.1278",
        )

        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()

        assertEquals("51.5074", restored.glanceWeatherManualLatitude)
        assertEquals("-0.1278", restored.glanceWeatherManualLongitude)
    }

    @Test
    fun parseHomeShortcutToken_plainPackage() {
        val (pkg, sid) = parseHomeShortcutToken("com.example.app")

        assertEquals("com.example.app", pkg)
        assertNull(sid)
    }

    @Test
    fun parseHomeShortcutToken_withShortcutId() {
        val (pkg, sid) = parseHomeShortcutToken("com.example.app#shortcut_42")

        assertEquals("com.example.app", pkg)
        assertEquals("shortcut_42", sid)
    }

    @Test
    fun parseHomeShortcutToken_trimsWhitespace() {
        val (pkg, sid) = parseHomeShortcutToken("  com.example.app  #  my_shortcut  ")

        assertEquals("com.example.app", pkg)
        assertEquals("my_shortcut", sid)
    }

    @Test
    fun homeShortcutStorageToken_plain() {
        assertEquals("com.example.app", homeShortcutStorageToken("com.example.app", null))
    }

    @Test
    fun homeShortcutStorageToken_withShortcutId() {
        assertEquals("com.example.app#sid123", homeShortcutStorageToken("com.example.app", "sid123"))
    }

    @Test
    fun parseHomeShortcutToken_roundTrip() {
        val token = homeShortcutStorageToken("com.example.app", "my_sid")
        val (pkg, sid) = parseHomeShortcutToken(token)

        assertEquals("com.example.app", pkg)
        assertEquals("my_sid", sid)
    }
}
