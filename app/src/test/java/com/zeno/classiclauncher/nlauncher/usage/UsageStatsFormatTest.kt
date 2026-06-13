package com.zeno.classiclauncher.nlauncher.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageStatsFormatTest {

    // ─── formatUsage ────────────────────────────────────────────────────────

    @Test
    fun formatUsage_zeroMs_showsOneMinute() {
        // coerceAtLeast(1) floors everything below 1 minute to "1m"
        assertEquals("1m", UsageStatsRepository.formatUsage(0L))
    }

    @Test
    fun formatUsage_30Seconds_showsOneMinute() {
        assertEquals("1m", UsageStatsRepository.formatUsage(30_000L))
    }

    @Test
    fun formatUsage_exactly45Minutes_shows45m() {
        assertEquals("45m", UsageStatsRepository.formatUsage(45 * 60_000L))
    }

    @Test
    fun formatUsage_exactly60Minutes_shows1h0m() {
        assertEquals("1h 0m", UsageStatsRepository.formatUsage(60 * 60_000L))
    }

    @Test
    fun formatUsage_90Minutes_shows1h30m() {
        assertEquals("1h 30m", UsageStatsRepository.formatUsage(90 * 60_000L))
    }

    @Test
    fun formatUsage_119Minutes_shows1h59m() {
        assertEquals("1h 59m", UsageStatsRepository.formatUsage(119 * 60_000L))
    }

    @Test
    fun formatUsage_120Minutes_shows2h0m() {
        assertEquals("2h 0m", UsageStatsRepository.formatUsage(120 * 60_000L))
    }

    @Test
    fun formatUsage_59Minutes_shows59m() {
        assertEquals("59m", UsageStatsRepository.formatUsage(59 * 60_000L))
    }

    // ─── formatUsageShort ───────────────────────────────────────────────────

    @Test
    fun formatUsageShort_zeroMs_showsOneMinute() {
        assertEquals("1m", UsageStatsRepository.formatUsageShort(0L))
    }

    @Test
    fun formatUsageShort_45Minutes_shows45m() {
        assertEquals("45m", UsageStatsRepository.formatUsageShort(45 * 60_000L))
    }

    @Test
    fun formatUsageShort_60Minutes_shows1h() {
        assertEquals("1h", UsageStatsRepository.formatUsageShort(60 * 60_000L))
    }

    @Test
    fun formatUsageShort_90Minutes_shows1h() {
        // Short format drops the minutes for anything >= 1h
        assertEquals("1h", UsageStatsRepository.formatUsageShort(90 * 60_000L))
    }

    @Test
    fun formatUsageShort_120Minutes_shows2h() {
        assertEquals("2h", UsageStatsRepository.formatUsageShort(120 * 60_000L))
    }

    @Test
    fun formatUsageShort_59Minutes_shows59m() {
        assertEquals("59m", UsageStatsRepository.formatUsageShort(59 * 60_000L))
    }
}
