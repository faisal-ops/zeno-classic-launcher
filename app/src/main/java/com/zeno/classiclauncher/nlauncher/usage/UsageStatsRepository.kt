package com.zeno.classiclauncher.nlauncher.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.core.content.getSystemService

object UsageStatsRepository {

    /** Returns true if the PACKAGE_USAGE_STATS special permission is granted. */
    fun hasPermission(context: Context): Boolean {
        val ops = context.getSystemService<AppOpsManager>() ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Returns total time spent in user-facing apps since midnight today.
     *
     * Uses ACTIVITY_RESUMED / ACTIVITY_PAUSED events — precise per-app intervals from exact
     * timestamps. Excludes system packages (no launch intent) and the launcher itself.
     * This matches how Digital Wellbeing calculates its "Today" figure.
     *
     * Why not queryUsageStats / queryAndAggregateUsageStats: those APIs aggregate over calendar
     * interval buckets that may span yesterday, inflating the total.
     * Why not SCREEN_INTERACTIVE events: those count lock-screen viewing time.
     * Why not KEYGUARD_HIDDEN events: unreliable across ROMs; misses sessions that started before
     * midnight when the device was already unlocked.
     */
    fun getTodayScreenOnTime(context: Context): Long {
        if (!hasPermission(context)) return 0L
        val usm = context.getSystemService<UsageStatsManager>() ?: return 0L
        val pm = context.packageManager
        val now = System.currentTimeMillis()
        val startOfDay = midnightToday()

        val events = usm.queryEvents(startOfDay, now) ?: return 0L

        // Build the user-facing package set once before iterating — getLaunchIntentForPackage
        // is an IPC call and must not be called inside the event loop (hundreds of calls).
        val launcherPkg = context.packageName
        val userFacingPkgs = mutableSetOf<String>()
        val event = UsageEvents.Event()
        val resumedAt = mutableMapOf<String, Long>()
        var total = 0L

        // First pass: collect all unique packages from events that are not the launcher itself.
        val allEventPkgs = mutableSetOf<String>()
        val eventsCopy = usm.queryEvents(startOfDay, now) ?: return 0L
        while (eventsCopy.hasNextEvent()) {
            eventsCopy.getNextEvent(event)
            if (event.packageName != launcherPkg) allEventPkgs.add(event.packageName)
        }
        for (pkg in allEventPkgs) {
            if (pm.getLaunchIntentForPackage(pkg) != null) userFacingPkgs.add(pkg)
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg in userFacingPkgs) {
                        resumedAt[pkg] = event.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                -> {
                    val start = resumedAt.remove(pkg)
                    if (start != null) total += event.timeStamp - start
                }
            }
        }
        // Apps still in foreground right now
        resumedAt.values.forEach { start -> total += now - start }
        return total
    }

    /**
     * Returns foreground usage time in milliseconds per package since midnight today.
     * Uses queryAndAggregateUsageStats (API 28+) for correct per-package aggregation.
     */
    fun getLast24hUsage(context: Context): Map<String, Long> {
        if (!hasPermission(context)) return emptyMap()
        val usm = context.getSystemService<UsageStatsManager>() ?: return emptyMap()
        val now = System.currentTimeMillis()
        val startOfDay = midnightToday()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            usm.queryAndAggregateUsageStats(startOfDay, now)
                .filterValues { it.totalTimeInForeground > 0 }
                .mapValues { (_, stats) -> stats.totalTimeInForeground }
        } else {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startOfDay, now)
                ?: return emptyMap()
            stats
                .filter { it.totalTimeInForeground > 0 }
                .groupBy { it.packageName }
                .mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }
        }
    }

    /** Formats milliseconds as a compact human string: "2h 15m", "45m", etc. */
    fun formatUsage(ms: Long): String {
        val totalMin = (ms / 60_000).toInt().coerceAtLeast(1)
        return if (totalMin >= 60) "${totalMin / 60}h ${totalMin % 60}m" else "${totalMin}m"
    }

    /** Short badge label: "2h" or "45m". */
    fun formatUsageShort(ms: Long): String {
        val totalMin = (ms / 60_000).toInt().coerceAtLeast(1)
        return if (totalMin >= 60) "${totalMin / 60}h" else "${totalMin}m"
    }

    private fun midnightToday(): Long =
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
}
