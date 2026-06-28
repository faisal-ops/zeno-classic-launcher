package com.zeno.classiclauncher.nlauncher.minimalmode

import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalModeLogicTest {

    // Minimal AppEntry builder — icon and componentName are unused by the logic under test.
    private fun app(pkg: String, label: String = pkg) = AppEntry(
        packageName = pkg,
        label = label,
        icon = null,
    )

    // ─── resolveMinimalModeApps ───────────────────────────────────────────────

    @Test
    fun resolveMinimalModeApps_noPinned_usesDefaults() {
        val allApps = listOf(
            app("com.android.dialer", "Phone"),
            app("com.google.android.apps.messaging", "Messages"),
            app("com.other.app", "Other"),
        )
        val result = resolveMinimalModeApps(emptyList(), allApps)
        // Default list includes com.android.dialer and com.google.android.apps.messaging
        assertTrue(result.any { it.packageName == "com.android.dialer" })
        assertTrue(result.any { it.packageName == "com.google.android.apps.messaging" })
        // com.other.app is not in the DEFAULT_PACKAGES list
        assertTrue(result.none { it.packageName == "com.other.app" })
    }

    @Test
    fun resolveMinimalModeApps_noPinned_skipsNotInstalledDefaults() {
        // Only one of the default packages is "installed"
        val allApps = listOf(app("com.android.dialer", "Phone"))
        val result = resolveMinimalModeApps(emptyList(), allApps)
        assertEquals(1, result.size)
        assertEquals("com.android.dialer", result[0].packageName)
    }

    @Test
    fun resolveMinimalModeApps_withPinned_usesPinnedOrder() {
        val allApps = listOf(
            app("com.android.dialer"),
            app("com.example.browser"),
            app("com.example.mail"),
        )
        val pinned = listOf("com.example.mail", "com.android.dialer", "com.example.browser")
        val result = resolveMinimalModeApps(pinned, allApps)
        assertEquals(listOf("com.example.mail", "com.android.dialer", "com.example.browser"),
            result.map { it.packageName })
    }

    @Test
    fun resolveMinimalModeApps_pinnedContainsUninstalledApp_skipsIt() {
        val allApps = listOf(app("com.android.dialer"))
        val pinned = listOf("com.uninstalled.app", "com.android.dialer")
        val result = resolveMinimalModeApps(pinned, allApps)
        assertEquals(1, result.size)
        assertEquals("com.android.dialer", result[0].packageName)
    }

    @Test
    fun resolveMinimalModeApps_emptyApps_returnsEmpty() {
        val result = resolveMinimalModeApps(emptyList(), emptyList())
        assertTrue(result.isEmpty())
    }

    // ─── parseAppLimits ──────────────────────────────────────────────────────

    @Test
    fun parseAppLimits_validEntries_parsed() {
        val result = parseAppLimits("com.android.dialer:3600000,com.whatsapp:1800000")
        assertEquals(2, result.size)
        assertEquals(3_600_000L, result["com.android.dialer"])
        assertEquals(1_800_000L, result["com.whatsapp"])
    }

    @Test
    fun parseAppLimits_emptyString_returnsEmpty() {
        assertTrue(parseAppLimits("").isEmpty())
    }

    @Test
    fun parseAppLimits_malformedEntry_skipped() {
        val result = parseAppLimits("com.android.dialer:3600000,badentry,com.whatsapp:1800000")
        assertEquals(2, result.size)
        assertTrue("com.android.dialer" in result)
        assertTrue("com.whatsapp" in result)
    }

    @Test
    fun parseAppLimits_nonNumericValue_skipped() {
        val result = parseAppLimits("com.android.dialer:notanumber,com.whatsapp:1800000")
        assertEquals(1, result.size)
        assertEquals(1_800_000L, result["com.whatsapp"])
    }

}
