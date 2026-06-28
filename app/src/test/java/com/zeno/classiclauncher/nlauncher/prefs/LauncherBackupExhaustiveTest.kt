package com.zeno.classiclauncher.nlauncher.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive round-trip and edge-case tests for [LauncherBackup].
 *
 * Supplements [LauncherPrefsTest] which covers the basic round-trip and version rejection.
 * These tests target: enum fields, list fields, boolean toggles, and cross-version compatibility.
 */
class LauncherBackupExhaustiveTest {

    private fun roundTrip(prefs: LauncherPrefs): LauncherPrefs =
        LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()

    // ─── String fields ────────────────────────────────────────────────────────

    @Test
    fun roundTrip_swipeUpPackage_preserved() {
        val prefs = LauncherPrefs(swipeUpPackage = "com.example.browser")
        assertEquals("com.example.browser", roundTrip(prefs).swipeUpPackage)
    }

    @Test
    fun roundTrip_swipeRightPackageDefault_isEmptyString() {
        val prefs = LauncherPrefs(swipeRightPackage = "")
        assertEquals("", roundTrip(prefs).swipeRightPackage)
    }

    @Test
    fun roundTrip_iconPackPackage_preserved() {
        val prefs = LauncherPrefs(iconPackPackage = "com.example.icons")
        assertEquals("com.example.icons", roundTrip(prefs).iconPackPackage)
    }

    // ─── Boolean toggles ─────────────────────────────────────────────────────

    @Test
    fun roundTrip_doubleTapToSleepEnabled_true_preserved() {
        val prefs = LauncherPrefs(doubleTapToSleepEnabled = true)
        assertTrue(roundTrip(prefs).doubleTapToSleepEnabled)
    }

    @Test
    fun roundTrip_doubleTapToSleepEnabled_false_preserved() {
        val prefs = LauncherPrefs(doubleTapToSleepEnabled = false)
        assertFalse(roundTrip(prefs).doubleTapToSleepEnabled)
    }

    @Test
    fun roundTrip_hapticsEnabled_preserved() {
        val on = LauncherPrefs(hapticsEnabled = true)
        val off = LauncherPrefs(hapticsEnabled = false)
        assertTrue(roundTrip(on).hapticsEnabled)
        assertFalse(roundTrip(off).hapticsEnabled)
    }

    @Test
    fun roundTrip_showIconNotifBadge_preserved() {
        assertTrue(roundTrip(LauncherPrefs(showIconNotifBadge = true)).showIconNotifBadge)
        assertFalse(roundTrip(LauncherPrefs(showIconNotifBadge = false)).showIconNotifBadge)
    }

    // ─── Enum fields ─────────────────────────────────────────────────────────

    @Test
    fun roundTrip_glanceWeatherUnit_celsius_preserved() {
        val prefs = LauncherPrefs(glanceWeatherUnit = GlanceWeatherUnit.CELSIUS)
        assertEquals(GlanceWeatherUnit.CELSIUS, roundTrip(prefs).glanceWeatherUnit)
    }

    @Test
    fun roundTrip_glanceWeatherUnit_fahrenheit_preserved() {
        val prefs = LauncherPrefs(glanceWeatherUnit = GlanceWeatherUnit.FAHRENHEIT)
        assertEquals(GlanceWeatherUnit.FAHRENHEIT, roundTrip(prefs).glanceWeatherUnit)
    }

    @Test
    fun roundTrip_appIconShape_preserved() {
        AppIconShape.entries.forEach { shape ->
            val prefs = LauncherPrefs(appIconShape = shape)
            assertEquals("Shape $shape failed to round-trip", shape, roundTrip(prefs).appIconShape)
        }
    }

    @Test
    fun roundTrip_minimalModeLayout_preserved() {
        MinimalModeLayout.entries.forEach { layout ->
            val prefs = LauncherPrefs(minimalModeLayout = layout)
            assertEquals("Layout $layout failed to round-trip", layout, roundTrip(prefs).minimalModeLayout)
        }
    }

    @Test
    fun roundTrip_gridPreset_preserved() {
        GridPreset.entries.forEach { preset ->
            val prefs = LauncherPrefs(gridPreset = preset)
            assertEquals("GridPreset $preset failed to round-trip", preset, roundTrip(prefs).gridPreset)
        }
    }

    // ─── List fields ─────────────────────────────────────────────────────────

    @Test
    fun roundTrip_quickSettingsTileOrder_preserved() {
        val prefs = LauncherPrefs(quickSettingsTileOrder = listOf("internet", "bluetooth", "battery"))
        assertEquals(listOf("internet", "bluetooth", "battery"), roundTrip(prefs).quickSettingsTileOrder)
    }

    @Test
    fun roundTrip_minimalModeApps_emptyList_preserved() {
        val prefs = LauncherPrefs(minimalModeApps = emptyList())
        assertTrue(roundTrip(prefs).minimalModeApps.isEmpty())
    }

    @Test
    fun roundTrip_minimalModeApps_multipleEntries_preserved() {
        val apps = listOf("com.android.dialer", "com.android.messaging", "com.android.camera2")
        val prefs = LauncherPrefs(minimalModeApps = apps)
        assertEquals(apps, roundTrip(prefs).minimalModeApps)
    }

    @Test
    fun roundTrip_minimalModeMaxApps_preserved() {
        MinimalModeMaxApps.entries.forEach { value ->
            val prefs = LauncherPrefs(minimalModeMaxApps = value)
            assertEquals("MinimalModeMaxApps $value failed to round-trip", value, roundTrip(prefs).minimalModeMaxApps)
        }
    }

    @Test
    fun roundTrip_minimalModeShowIcons_preserved() {
        assertTrue(roundTrip(LauncherPrefs(minimalModeShowIcons = true)).minimalModeShowIcons)
        assertFalse(roundTrip(LauncherPrefs(minimalModeShowIcons = false)).minimalModeShowIcons)
    }

    @Test
    fun roundTrip_minimalModeShowWeather_preserved() {
        assertTrue(roundTrip(LauncherPrefs(minimalModeShowWeather = true)).minimalModeShowWeather)
        assertFalse(roundTrip(LauncherPrefs(minimalModeShowWeather = false)).minimalModeShowWeather)
    }

    @Test
    fun roundTrip_minimalModeShowNotifSummary_preserved() {
        assertTrue(roundTrip(LauncherPrefs(minimalModeShowNotifSummary = true)).minimalModeShowNotifSummary)
        assertFalse(roundTrip(LauncherPrefs(minimalModeShowNotifSummary = false)).minimalModeShowNotifSummary)
    }

    @Test
    fun roundTrip_minimalModeGreyscale_preserved() {
        assertTrue(roundTrip(LauncherPrefs(minimalModeGreyscale = true)).minimalModeGreyscale)
        assertFalse(roundTrip(LauncherPrefs(minimalModeGreyscale = false)).minimalModeGreyscale)
    }

    @Test
    fun roundTrip_minimalModeAppLimits_preserved() {
        val limits = "com.android.dialer:3600000,com.whatsapp:1800000"
        val prefs = LauncherPrefs(minimalModeAppLimits = limits)
        assertEquals(limits, roundTrip(prefs).minimalModeAppLimits)
    }

    // ─── Auto-unlock fields ───────────────────────────────────────────────────

    @Test
    fun roundTrip_autoUnlockEnabled_false_preserved() {
        val prefs = LauncherPrefs(autoUnlockEnabled = false)
        assertFalse(roundTrip(prefs).autoUnlockEnabled)
    }

    @Test
    fun roundTrip_autoUnlockPinDigits_6_preserved() {
        val prefs = LauncherPrefs(autoUnlockPinDigits = 6)
        assertEquals(6, roundTrip(prefs).autoUnlockPinDigits)
    }

    @Test
    fun roundTrip_autoUnlockPinDigits_clampedToRange() {
        // fromJson clamps to 4..8; simulate a future version writing 3
        val json = LauncherBackup.toJson(LauncherPrefs(autoUnlockPinDigits = 4))
            .replace("\"autoUnlockPinDigits\": 4", "\"autoUnlockPinDigits\": 3")
        val restored = LauncherBackup.fromJson(json).getOrThrow()
        assertEquals(4, restored.autoUnlockPinDigits)
    }

    // ─── Default values for missing keys ─────────────────────────────────────

    @Test
    fun fromJson_missingMinimalModeEnabled_defaultsFalse() {
        val json = LauncherBackup.toJson(LauncherPrefs())
            .replace("\"minimalModeEnabled\": false,", "")
        val restored = LauncherBackup.fromJson(json).getOrThrow()
        assertFalse(restored.minimalModeEnabled)
    }

    @Test
    fun fromJson_missingSwipeRightPackage_defaultsEmpty() {
        val json = LauncherBackup.toJson(LauncherPrefs())
            .replace("\"swipeRightPackage\": \"\",", "")
        val restored = LauncherBackup.fromJson(json).getOrThrow()
        assertEquals("", restored.swipeRightPackage)
    }

    // ─── Version handling ────────────────────────────────────────────────────

    @Test
    fun toJson_includesCorrectVersion() {
        val json = LauncherBackup.toJson(LauncherPrefs())
        assertTrue(json.contains("\"${LauncherBackup.VERSION_KEY}\": ${LauncherBackup.CURRENT_VERSION}"))
    }

    @Test
    fun fromJson_exactCurrentVersion_succeeds() {
        val json = LauncherBackup.toJson(LauncherPrefs())
        assertTrue(LauncherBackup.fromJson(json).isSuccess)
    }
}
