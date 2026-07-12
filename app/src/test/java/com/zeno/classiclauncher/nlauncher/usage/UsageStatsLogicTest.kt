package com.zeno.classiclauncher.nlauncher.usage

import android.app.usage.UsageEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIDNIGHT = 1_000_000L
private const val NOW = MIDNIGHT + 8 * 3_600_000L  // 8 h after midnight

private fun resumed(pkg: String, t: Long) = RawUsageEvent(UsageEvents.Event.ACTIVITY_RESUMED, pkg, t)
private fun paused(pkg: String, t: Long) = RawUsageEvent(UsageEvents.Event.ACTIVITY_PAUSED, pkg, t)
private fun stopped(pkg: String, t: Long) = RawUsageEvent(UsageEvents.Event.ACTIVITY_STOPPED, pkg, t)
private fun screenOff(t: Long) = RawUsageEvent(UsageEvents.Event.SCREEN_NON_INTERACTIVE, "android", t)
private fun screenOn(t: Long) = RawUsageEvent(UsageEvents.Event.SCREEN_INTERACTIVE, "android", t)

private fun accumulate(vararg events: RawUsageEvent) =
    accumulateUsageEvents(events.toList(), MIDNIGHT, NOW)

class UsageStatsLogicTest {

    // ─── accumulateUsageEvents ───────────────────────────────────────────────

    @Test
    fun simpleSession_correctDuration() {
        val result = accumulate(
            resumed("com.app", MIDNIGHT + 1_000L),
            paused("com.app", MIDNIGHT + 61_000L),
        )
        assertEquals(60_000L, result["com.app"])
    }

    @Test
    fun multipleSessionsSameApp_summed() {
        val result = accumulate(
            resumed("com.app", MIDNIGHT + 0L),
            paused("com.app", MIDNIGHT + 30_000L),
            resumed("com.app", MIDNIGHT + 60_000L),
            paused("com.app", MIDNIGHT + 90_000L),
        )
        assertEquals(60_000L, result["com.app"])
    }

    @Test
    fun pauseBeforeResume_midnightCreditApplied() {
        // App was already foregrounded before midnight — first event is a PAUSED with no prior RESUMED.
        val result = accumulate(
            paused("com.app", MIDNIGHT + 10_000L),
        )
        // Credit = MIDNIGHT, so duration = paused - midnight = 10_000
        assertEquals(10_000L, result["com.app"])
    }

    @Test
    fun midnightCredit_notAppliedTwice() {
        // Two consecutive PAUSEs without a RESUMED between them — second one must yield null, not another credit.
        val result = accumulate(
            paused("com.app", MIDNIGHT + 10_000L),   // gets midnight credit
            paused("com.app", MIDNIGHT + 20_000L),   // must be ignored (credit already used)
        )
        // Only the first PAUSED contributes
        assertEquals(10_000L, result["com.app"])
    }

    @Test
    fun activityStopped_excluded() {
        // STOPPED fires after every PAUSED — must NOT be accumulated, otherwise duration doubles.
        val result = accumulate(
            resumed("com.app", MIDNIGHT + 0L),
            paused("com.app", MIDNIGHT + 30_000L),
            stopped("com.app", MIDNIGHT + 30_100L),  // must be ignored
        )
        assertEquals(30_000L, result["com.app"])
    }

    @Test
    fun activityStopped_onlyEvents_noCredit() {
        // Only STOPPED events — must produce zero (no credit, no session).
        val result = accumulate(
            stopped("com.app", MIDNIGHT + 5_000L),
        )
        assertTrue(result["com.app"] == null || result["com.app"] == 0L)
    }

    @Test
    fun openSessionAtNow_accumulated() {
        // App is still in the foreground at query time — open session should be closed at NOW.
        val result = accumulate(
            resumed("com.app", MIDNIGHT + 0L),
        )
        assertEquals(NOW - MIDNIGHT, result["com.app"])
    }

    @Test
    fun multipleApps_trackedIndependently() {
        val result = accumulate(
            resumed("com.a", MIDNIGHT + 0L),
            paused("com.a", MIDNIGHT + 10_000L),
            resumed("com.b", MIDNIGHT + 20_000L),
            paused("com.b", MIDNIGHT + 50_000L),
        )
        assertEquals(10_000L, result["com.a"])
        assertEquals(30_000L, result["com.b"])
    }

    @Test
    fun emptyEvents_returnsEmptyMap() {
        val result = accumulate()
        assertTrue(result.isEmpty())
    }

    @Test
    fun screenOffWhileResumed_closesSessionAtScreenOff_notAtNow() {
        // App resumed, screen turns off 30s later but no PAUSED ever arrives, then hours pass
        // before the query window ends. Must NOT count the screen-off dead time.
        val result = accumulate(
            resumed("com.app", MIDNIGHT + 0L),
            screenOff(MIDNIGHT + 30_000L),
        )
        assertEquals(30_000L, result["com.app"])
    }

    @Test
    fun screenOffThenBackOn_newSessionCountedSeparately() {
        val result = accumulate(
            resumed("com.app", MIDNIGHT + 0L),
            screenOff(MIDNIGHT + 30_000L),
            screenOn(MIDNIGHT + 100_000L),
            resumed("com.app", MIDNIGHT + 100_000L),
            paused("com.app", MIDNIGHT + 130_000L),
        )
        assertEquals(60_000L, result["com.app"])
    }

    @Test
    fun screenOff_closesMultipleOpenSessions() {
        val result = accumulate(
            resumed("com.a", MIDNIGHT + 0L),
            resumed("com.b", MIDNIGHT + 10_000L),
            screenOff(MIDNIGHT + 40_000L),
        )
        assertEquals(40_000L, result["com.a"])
        assertEquals(30_000L, result["com.b"])
    }

    @Test
    fun staleP_afterScreenOff_notDoubleCredited() {
        // A PAUSED that arrives after SCREEN_NON_INTERACTIVE already closed the session
        // must not re-apply the midnight-carryover credit and double-count.
        val result = accumulate(
            resumed("com.app", MIDNIGHT + 0L),
            screenOff(MIDNIGHT + 30_000L),
            paused("com.app", MIDNIGHT + 35_000L),
        )
        assertEquals(30_000L, result["com.app"])
    }

    // ─── mergeUsageMaps ──────────────────────────────────────────────────────

    @Test
    fun merge_takesMaxPerPackage() {
        val daily = mapOf("com.app" to 30 * 60_000L)
        val event = mapOf("com.app" to 45 * 60_000L)
        val result = mergeUsageMaps(daily, event)
        assertEquals(45 * 60_000L, result["com.app"])
    }

    @Test
    fun merge_packageOnlyInDaily_included() {
        val daily = mapOf("com.music" to 60 * 60_000L)
        val event = emptyMap<String, Long>()
        val result = mergeUsageMaps(daily, event)
        assertEquals(60 * 60_000L, result["com.music"])
    }

    @Test
    fun merge_packageOnlyInEvent_included() {
        val daily = emptyMap<String, Long>()
        val event = mapOf("com.video" to 20 * 60_000L)
        val result = mergeUsageMaps(daily, event)
        assertEquals(20 * 60_000L, result["com.video"])
    }

    @Test
    fun merge_emptyBoth_returnsEmpty() {
        val result = mergeUsageMaps(emptyMap(), emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun merge_dailyHigher_takesDailyValue() {
        val daily = mapOf("com.app" to 90 * 60_000L)
        val event = mapOf("com.app" to 45 * 60_000L)
        val result = mergeUsageMaps(daily, event)
        assertEquals(90 * 60_000L, result["com.app"])
    }

    // ─── computeForegroundUnionMs ───────────────────────────────────────────

    private fun union(vararg events: RawUsageEvent) =
        computeForegroundUnionMs(events.toList(), MIDNIGHT, NOW)

    @Test
    fun union_singleSession_matchesDuration() {
        val result = union(
            resumed("com.app", MIDNIGHT + 0L),
            paused("com.app", MIDNIGHT + 60_000L),
        )
        assertEquals(60_000L, result)
    }

    @Test
    fun union_sequentialApps_summed() {
        val result = union(
            resumed("com.a", MIDNIGHT + 0L),
            paused("com.a", MIDNIGHT + 10_000L),
            resumed("com.b", MIDNIGHT + 10_000L),
            paused("com.b", MIDNIGHT + 30_000L),
        )
        assertEquals(30_000L, result)
    }

    @Test
    fun union_overlappingPip_notDoubleCounted() {
        // com.video stays resumed (PiP) the whole time; com.browser opens on top of it.
        // Naive per-package summation would give 30s(video) + 20s(browser) = 50s;
        // the real screen-on time is only the 30s union.
        val result = union(
            resumed("com.video", MIDNIGHT + 0L),
            resumed("com.browser", MIDNIGHT + 5_000L),
            paused("com.browser", MIDNIGHT + 25_000L),
            paused("com.video", MIDNIGHT + 30_000L),
        )
        assertEquals(30_000L, result)
    }

    @Test
    fun union_screenOff_closesOpenSession() {
        val result = union(
            resumed("com.app", MIDNIGHT + 0L),
            screenOff(MIDNIGHT + 30_000L),
        )
        assertEquals(30_000L, result)
    }

    @Test
    fun union_openSessionAtNow_closedAtNow() {
        val result = union(resumed("com.app", MIDNIGHT + 0L))
        assertEquals(NOW - MIDNIGHT, result)
    }

    @Test
    fun union_emptyEvents_returnsZero() {
        assertEquals(0L, union())
    }
}
