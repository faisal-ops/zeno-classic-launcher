package com.zeno.classiclauncher.nlauncher.badges

import android.app.Notification
import android.os.Process
import android.service.notification.StatusBarNotification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests for [NotificationRepository.shouldTrack].
 *
 * Uses Robolectric so [Notification.Builder] and [StatusBarNotification] work on JVM.
 * The deprecated 10-param constructor is used to disambiguate from the overload that
 * swaps `int score` for `String overrideGroupKey` (both have 10 params; the deprecated
 * one has score=Int at position 7, which the Kotlin compiler can resolve unambiguously).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationFilterTest {

    private val ctx get() = RuntimeEnvironment.getApplication()

    @Suppress("DEPRECATION")
    private fun makeSbn(pkg: String, notification: Notification): StatusBarNotification =
        StatusBarNotification(
            pkg, pkg, 1, null,
            Process.myUid(), 0,
            0, // score (deprecated param — used to select this overload unambiguously)
            notification,
            Process.myUserHandle(),
            System.currentTimeMillis(),
        )

    // ─── SMS / messaging ─────────────────────────────────────────────────────

    @Test
    fun sms_messageCategoryNotification_isTracked() {
        val n = Notification.Builder(ctx, "ch")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .build()
        assertTrue(NotificationRepository.shouldTrack(makeSbn("com.android.messaging", n)))
    }

    @Test
    fun sms_lowPriorityNotification_isNotTracked() {
        @Suppress("DEPRECATION")
        val n = Notification.Builder(ctx, "ch")
            .setPriority(Notification.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .build()
        assertFalse(NotificationRepository.shouldTrack(makeSbn("com.android.messaging", n)))
    }

    @Test
    fun sms_groupSummary_isNotTracked() {
        val n = Notification.Builder(ctx, "ch")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setGroupSummary(true)
            .build()
        assertFalse(NotificationRepository.shouldTrack(makeSbn("com.android.messaging", n)))
    }

    // ─── WhatsApp ─────────────────────────────────────────────────────────────

    @Test
    fun whatsapp_normalMessageNotification_isTracked() {
        val n = Notification.Builder(ctx, "ch")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .build()
        assertTrue(NotificationRepository.shouldTrack(makeSbn("com.whatsapp", n)))
    }

    @Test
    fun whatsapp_groupSummary_isNotTracked() {
        val n = Notification.Builder(ctx, "ch")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setGroupSummary(true)
            .build()
        assertFalse(NotificationRepository.shouldTrack(makeSbn("com.whatsapp", n)))
    }

    // ─── Mail ────────────────────────────────────────────────────────────────

    @Test
    fun gmail_normalNotification_isTracked() {
        val n = Notification.Builder(ctx, "ch").build()
        assertTrue(NotificationRepository.shouldTrack(makeSbn("com.google.android.gm", n)))
    }

    @Test
    fun gmail_groupSummary_isNotTracked() {
        // Group summaries from Gmail are silent bookkeeping — must be excluded so the badge
        // clears after the user reads all visible mail notifications.
        val n = Notification.Builder(ctx, "ch")
            .setGroupSummary(true)
            .build()
        assertFalse(NotificationRepository.shouldTrack(makeSbn("com.google.android.gm", n)))
    }

    @Test
    fun mail_lowPriorityNotification_isStillTracked() {
        // Mail apps: Primary / Social / Promotions tabs all count — do NOT drop by priority.
        @Suppress("DEPRECATION")
        val n = Notification.Builder(ctx, "ch")
            .setPriority(Notification.PRIORITY_LOW)
            .build()
        assertTrue(NotificationRepository.shouldTrack(makeSbn("com.google.android.gm", n)))
    }

    // ─── Non-badge packages ───────────────────────────────────────────────────

    @Test
    fun unrelatedApp_messageCategory_isNotTracked() {
        val n = Notification.Builder(ctx, "ch")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .build()
        assertFalse(NotificationRepository.shouldTrack(makeSbn("com.example.randomapp", n)))
    }

    // ─── Category filter for SMS ─────────────────────────────────────────────

    @Test
    fun sms_nullCategory_isTracked() {
        // null category is a pass-through for SMS packages
        val n = Notification.Builder(ctx, "ch").build()
        assertTrue(NotificationRepository.shouldTrack(makeSbn("com.android.messaging", n)))
    }

    @Test
    fun sms_emailCategory_isTracked() {
        val n = Notification.Builder(ctx, "ch")
            .setCategory(Notification.CATEGORY_EMAIL)
            .build()
        assertTrue(NotificationRepository.shouldTrack(makeSbn("com.android.messaging", n)))
    }

    @Test
    fun sms_callCategory_isTracked() {
        val n = Notification.Builder(ctx, "ch")
            .setCategory(Notification.CATEGORY_CALL)
            .build()
        assertTrue(NotificationRepository.shouldTrack(makeSbn("com.android.messaging", n)))
    }

    @Test
    fun sms_alarmCategory_isNotTracked() {
        // ALARM is not in the allowed category set for SMS packages
        val n = Notification.Builder(ctx, "ch")
            .setCategory(Notification.CATEGORY_ALARM)
            .build()
        assertFalse(NotificationRepository.shouldTrack(makeSbn("com.android.messaging", n)))
    }
}
