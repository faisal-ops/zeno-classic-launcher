package com.zeno.classiclauncher.nlauncher.simplemode

import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleModeLogicTest {

    // Minimal AppEntry builder — icon and componentName are unused by the logic under test.
    private fun app(pkg: String, label: String = pkg) = AppEntry(
        packageName = pkg,
        label = label,
        icon = null,
    )

    // ─── resolveSimpleModeApps ───────────────────────────────────────────────

    @Test
    fun resolveSimpleModeApps_noPinned_usesDefaults() {
        val allApps = listOf(
            app("com.android.dialer", "Phone"),
            app("com.google.android.apps.messaging", "Messages"),
            app("com.other.app", "Other"),
        )
        val result = resolveSimpleModeApps(emptyList(), allApps)
        // Default list includes com.android.dialer and com.google.android.apps.messaging
        assertTrue(result.any { it.packageName == "com.android.dialer" })
        assertTrue(result.any { it.packageName == "com.google.android.apps.messaging" })
        // com.other.app is not in the DEFAULT_PACKAGES list
        assertTrue(result.none { it.packageName == "com.other.app" })
    }

    @Test
    fun resolveSimpleModeApps_noPinned_skipsNotInstalledDefaults() {
        // Only one of the default packages is "installed"
        val allApps = listOf(app("com.android.dialer", "Phone"))
        val result = resolveSimpleModeApps(emptyList(), allApps)
        assertEquals(1, result.size)
        assertEquals("com.android.dialer", result[0].packageName)
    }

    @Test
    fun resolveSimpleModeApps_withPinned_usesPinnedOrder() {
        val allApps = listOf(
            app("com.android.dialer"),
            app("com.example.browser"),
            app("com.example.mail"),
        )
        val pinned = listOf("com.example.mail", "com.android.dialer", "com.example.browser")
        val result = resolveSimpleModeApps(pinned, allApps)
        assertEquals(listOf("com.example.mail", "com.android.dialer", "com.example.browser"),
            result.map { it.packageName })
    }

    @Test
    fun resolveSimpleModeApps_pinnedContainsUninstalledApp_skipsIt() {
        val allApps = listOf(app("com.android.dialer"))
        val pinned = listOf("com.uninstalled.app", "com.android.dialer")
        val result = resolveSimpleModeApps(pinned, allApps)
        assertEquals(1, result.size)
        assertEquals("com.android.dialer", result[0].packageName)
    }

    @Test
    fun resolveSimpleModeApps_emptyApps_returnsEmpty() {
        val result = resolveSimpleModeApps(emptyList(), emptyList())
        assertTrue(result.isEmpty())
    }

    // ─── buildNotifSummary ───────────────────────────────────────────────────

    @Test
    fun buildNotifSummary_noNotifications_returnsNoNewNotifications() {
        val result = buildNotifSummary(
            packagesWithUnread = emptySet(),
            hasUnreadMail = false,
            hasUnreadSms = false,
            hasUnreadWhatsApp = false,
        )
        assertEquals("No New Notifications", result)
    }

    @Test
    fun buildNotifSummary_missedCall_returnsMissedCalls() {
        val result = buildNotifSummary(
            packagesWithUnread = setOf("com.android.dialer"),
            hasUnreadMail = true,
            hasUnreadSms = true,
            hasUnreadWhatsApp = true,
        )
        // Phone takes priority over everything else
        assertEquals("Missed Calls", result)
    }

    @Test
    fun buildNotifSummary_unreadSmsNoCalls_returnsNewMessages() {
        val result = buildNotifSummary(
            packagesWithUnread = setOf("com.android.messaging"),
            hasUnreadMail = false,
            hasUnreadSms = true,
            hasUnreadWhatsApp = false,
        )
        assertEquals("New Messages", result)
    }

    @Test
    fun buildNotifSummary_whatsappOnly_returnsWhatsappMessages() {
        val result = buildNotifSummary(
            packagesWithUnread = setOf("com.whatsapp"),
            hasUnreadMail = false,
            hasUnreadSms = false,
            hasUnreadWhatsApp = true,
        )
        assertEquals("WhatsApp Messages", result)
    }

    @Test
    fun buildNotifSummary_mailOnly_returnsNewEmails() {
        val result = buildNotifSummary(
            packagesWithUnread = setOf("com.google.android.gm"),
            hasUnreadMail = true,
            hasUnreadSms = false,
            hasUnreadWhatsApp = false,
        )
        assertEquals("New Emails", result)
    }

    @Test
    fun buildNotifSummary_genericAppNotification_returnsNewNotifications() {
        val result = buildNotifSummary(
            packagesWithUnread = setOf("com.example.someapp"),
            hasUnreadMail = false,
            hasUnreadSms = false,
            hasUnreadWhatsApp = false,
        )
        assertEquals("New Notifications", result)
    }

    @Test
    fun buildNotifSummary_googleDialerCountsAsPhone() {
        val result = buildNotifSummary(
            packagesWithUnread = setOf("com.google.android.dialer"),
            hasUnreadMail = false,
            hasUnreadSms = false,
            hasUnreadWhatsApp = false,
        )
        assertEquals("Missed Calls", result)
    }
}
