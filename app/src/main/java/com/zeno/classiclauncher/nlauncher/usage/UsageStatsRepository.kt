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
     * Event-based: precise, launcher-excluded, no interval-bucket rounding.
     */
    fun getTodayScreenOnTime(context: Context): Long {
        if (!hasPermission(context)) return 0L
        val usm = context.getSystemService<UsageStatsManager>() ?: return 0L
        val now = System.currentTimeMillis()
        val startOfDay = midnightToday()
        val events = usm.queryEvents(startOfDay, now) ?: return 0L
        val launcherPkg = context.packageName
        val event = UsageEvents.Event()
        val resumedAt = mutableMapOf<String, Long>()
        val midnightCreditUsed = mutableSetOf<String>()
        var total = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            if (pkg == launcherPkg) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> resumedAt[pkg] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = if (pkg in resumedAt) resumedAt.remove(pkg)!!
                    else if (midnightCreditUsed.add(pkg)) startOfDay
                    else null
                    if (start != null) total += event.timeStamp - start
                }
            }
        }
        resumedAt.values.forEach { start -> total += now - start }
        return total
    }

    /**
     * Returns foreground usage per package since midnight today.
     *
     * Hybrid of two independent sources — takes max(daily, event) per package:
     *
     * SOURCE 1 — INTERVAL_DAILY queryUsageStats (midnight-bounded bucket):
     *   On API 29+, totalTimeInForeground includes foreground-service time, so music apps
     *   (YouTube Music), video apps (YouTube PiP), and on some OEMs notification interaction
     *   time (WhatsApp) are captured even when the activity itself isn't visible.
     *   Guard: filter firstTimeStamp >= startOfDay to prevent yesterday's bucket bleeding in.
     *
     * SOURCE 2 — ACTIVITY_RESUMED/PAUSED events (event log):
     *   Exact per-session timing. Correct midnight carry-over via one-time startOfDay credit.
     *   Zero risk of bucket rounding or yesterday contamination.
     *
     * Taking max() ensures no app is ever worse than either source alone.
     */
    fun getLast24hUsage(context: Context): Map<String, Long> {
        if (!hasPermission(context)) return emptyMap()
        val daily = dailyBucketUsage(context)
        val eventBased = eventBasedUsage(context)
        val allPkgs = daily.keys + eventBased.keys
        return allPkgs.associateWith { pkg -> maxOf(daily[pkg] ?: 0L, eventBased[pkg] ?: 0L) }
    }

    /**
     * SOURCE 1: INTERVAL_DAILY queryUsageStats.
     * Bucket starts at midnight — totalTimeInForeground represents usage since midnight.
     * Includes foreground-service time on API 29+ (audio, PiP, etc.).
     */
    private fun dailyBucketUsage(context: Context): Map<String, Long> {
        val usm = context.getSystemService<UsageStatsManager>() ?: return emptyMap()
        val startOfDay = midnightToday()
        val now = System.currentTimeMillis()
        val launcherPkg = context.packageName
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
            ?.filter { stats ->
                stats.packageName != launcherPkg &&
                    stats.totalTimeInForeground > 0 &&
                    // Require bucket to start within 60s of midnight to exclude yesterday's bucket
                    stats.firstTimeStamp >= startOfDay - 60_000L
            }
            ?.groupBy { it.packageName }
            ?.mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }
            ?: emptyMap()
    }

    /**
     * SOURCE 2: ACTIVITY_RESUMED / ACTIVITY_PAUSED event log.
     * Precise session boundaries. One-time midnight carry-over credit per package.
     * ACTIVITY_STOPPED excluded — PAUSED already marks end of foreground; STOPPED fires
     * after every PAUSED and would multiply-count each screen transition as hours of fake time.
     */
    private fun eventBasedUsage(context: Context): Map<String, Long> {
        val usm = context.getSystemService<UsageStatsManager>() ?: return emptyMap()
        val now = System.currentTimeMillis()
        val startOfDay = midnightToday()
        val launcherPkg = context.packageName
        val event = UsageEvents.Event()
        val resumedAt = mutableMapOf<String, Long>()
        val totals = mutableMapOf<String, Long>()
        val midnightCreditUsed = mutableSetOf<String>()
        val events = usm.queryEvents(startOfDay, now) ?: return emptyMap()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            if (pkg == launcherPkg) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> resumedAt[pkg] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = if (pkg in resumedAt) resumedAt.remove(pkg)!!
                    else if (midnightCreditUsed.add(pkg)) startOfDay
                    else null
                    if (start != null) totals[pkg] = (totals[pkg] ?: 0L) + event.timeStamp - start
                }
            }
        }
        resumedAt.forEach { (pkg, start) ->
            totals[pkg] = (totals[pkg] ?: 0L) + now - start
        }
        return totals
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
