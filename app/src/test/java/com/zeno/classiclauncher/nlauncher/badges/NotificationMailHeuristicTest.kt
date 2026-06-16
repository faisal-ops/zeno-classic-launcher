package com.zeno.classiclauncher.nlauncher.badges

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [NotificationRepository.isMailLikePackage].
 *
 * This is pure string logic — no Android SDK objects needed.
 * New mail clients must be verified here before adding to the heuristic.
 */
class NotificationMailHeuristicTest {

    // ─── Known mail apps (in MAIL_PACKAGES) ─────────────────────────────────

    @Test
    fun gmail_isMailLike() = assertTrue(NotificationRepository.isMailLikePackage("com.google.android.gm"))

    @Test
    fun outlook_isMailLike() = assertTrue(NotificationRepository.isMailLikePackage("com.microsoft.office.outlook"))

    @Test
    fun yahoo_isMailLike() = assertTrue(NotificationRepository.isMailLikePackage("com.yahoo.mobile.client.android.mail"))

    // ─── Heuristic matches ───────────────────────────────────────────────────

    @Test
    fun samsungEmail_isMailLike() =
        assertTrue(NotificationRepository.isMailLikePackage("com.samsung.android.email.provider"))

    @Test
    fun protonMail_isMailLike() =
        assertTrue(NotificationRepository.isMailLikePackage("me.proton.mail.android"))

    @Test
    fun k9Mail_isMailLike() =
        assertTrue(NotificationRepository.isMailLikePackage("com.fsck.k9"))  // contains "k9"

    @Test
    fun aquaMail_isMailLike() =
        assertTrue(NotificationRepository.isMailLikePackage("org.kman.AquaMail"))

    @Test
    fun blueMail_isMailLike() =
        assertTrue(NotificationRepository.isMailLikePackage("me.bluemail.mail"))

    @Test
    fun inboxApp_isMailLike() =
        assertTrue(NotificationRepository.isMailLikePackage("com.google.android.apps.inbox"))

    @Test
    fun thunderbird_isMailLike() =
        assertTrue(NotificationRepository.isMailLikePackage("net.thunderbird.android"))

    @Test
    fun readdle_isMailLike() =
        assertTrue(NotificationRepository.isMailLikePackage("com.readdle.spark"))

    // ─── Packages that must NOT match ────────────────────────────────────────

    @Test
    fun whatsapp_isNotMailLike() =
        assertFalse(NotificationRepository.isMailLikePackage("com.whatsapp"))

    @Test
    fun googleMessaging_isNotMailLike() =
        assertFalse(NotificationRepository.isMailLikePackage("com.google.android.apps.messaging"))

    @Test
    fun androidMessaging_isNotMailLike() =
        assertFalse(NotificationRepository.isMailLikePackage("com.android.messaging"))

    @Test
    fun telegramMessenger_isNotMailLike() =
        assertFalse(NotificationRepository.isMailLikePackage("org.telegram.messenger"))

    @Test
    fun dialer_isNotMailLike() =
        assertFalse(NotificationRepository.isMailLikePackage("com.android.dialer"))

    @Test
    fun calculator_isNotMailLike() =
        assertFalse(NotificationRepository.isMailLikePackage("com.google.android.calculator"))

    @Test
    fun chrome_isNotMailLike() =
        assertFalse(NotificationRepository.isMailLikePackage("com.android.chrome"))

    // ─── Edge cases ──────────────────────────────────────────────────────────

    @Test
    fun packageContainingMessagingAndMail_messagingWins() {
        // "messaging" keyword blocks the "mail" keyword
        assertFalse(NotificationRepository.isMailLikePackage("com.example.messaging.mail"))
    }

    @Test
    fun hubPackage_isMailLike() =
        // BB Hub-style apps should count as mail
        assertTrue(NotificationRepository.isMailLikePackage("com.blackberry.hub"))
}
