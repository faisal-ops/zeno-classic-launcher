package com.zeno.classiclauncher.nlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {

    @Test
    fun matchSettingsEntries_shortQuery_returnsEmpty() {
        assertEquals(emptyList<Any>(), matchSettingsEntries("a"))
    }

    @Test
    fun matchSettingsEntries_manageApps_findsUninstallKeyword() {
        val labels = matchSettingsEntries("uninstall").map { it.label }
        assertTrue(labels.contains("Manage Apps"))
    }

    @Test
    fun matchSettingsEntries_batteryOptimization_findsDozeKeyword() {
        val labels = matchSettingsEntries("doze").map { it.label }
        assertTrue(labels.contains("Battery Optimization"))
    }

    @Test
    fun matchSettingsEntries_wallpaper_findsBackgroundImageKeyword() {
        val labels = matchSettingsEntries("background image").map { it.label }
        assertTrue(labels.contains("Wallpaper"))
    }

    @Test
    fun matchSettingsEntries_changeDefaultLauncher_findsHomeAppKeyword() {
        val labels = matchSettingsEntries("home app").map { it.label }
        assertTrue(labels.contains("Change Default Launcher"))
    }

    @Test
    fun matchSettingsEntries_usageAccess_findsDigitalWellbeingKeyword() {
        val labels = matchSettingsEntries("digital wellbeing").map { it.label }
        assertTrue(labels.contains("Usage Access"))
    }

    @Test
    fun matchSettingsEntries_multipleUsers_findsGuestModeKeyword() {
        val labels = matchSettingsEntries("guest mode").map { it.label }
        assertTrue(labels.contains("Multiple Users"))
    }

    @Test
    fun matchSettingsEntries_print_findsPrinterKeyword() {
        val labels = matchSettingsEntries("printer").map { it.label }
        assertTrue(labels.contains("Print"))
    }

    @Test
    fun matchSettingsEntries_screenPinning_findsPinAppKeyword() {
        val labels = matchSettingsEntries("pin app").map { it.label }
        assertTrue(labels.contains("Screen Pinning"))
    }

    @Test
    fun matchSettingsEntries_voiceInput_findsAssistantKeyword() {
        val labels = matchSettingsEntries("assistant").map { it.label }
        assertTrue(labels.contains("Voice Input & Assistant"))
    }

    @Test
    fun matchSettingsEntries_ambientDisplay_findsAodKeyword() {
        val labels = matchSettingsEntries("aod").map { it.label }
        assertTrue(labels.contains("Ambient Display"))
    }

    @Test
    fun matchSettingsEntries_colourAndMotion_findsColorInversionKeyword() {
        val labels = matchSettingsEntries("color inversion").map { it.label }
        assertTrue(labels.contains("Colour and Motion"))
    }

    @Test
    fun matchSettingsEntries_colourAndMotion_findsRemoveAnimationsKeyword() {
        val labels = matchSettingsEntries("remove animations").map { it.label }
        assertTrue(labels.contains("Colour and Motion"))
    }
}
