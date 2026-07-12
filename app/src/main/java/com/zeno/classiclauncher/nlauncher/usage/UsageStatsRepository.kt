package com.zeno.classiclauncher.nlauncher.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
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
        val allowedPkgs = launchableAppPackages(context)
        val event = UsageEvents.Event()
        val eventList = mutableListOf<RawUsageEvent>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isScreenEvent = event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE ||
                event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE
            if (isScreenEvent || (event.packageName != launcherPkg && event.packageName in allowedPkgs)) {
                eventList.add(RawUsageEvent(event.eventType, event.packageName, event.timeStamp))
            }
        }
        return computeForegroundUnionMs(eventList, startOfDay, now)
    }

    /**
     * Packages with a launcher-visible activity — mirrors what Digital Wellbeing counts as
     * "apps". Excludes System UI, IME, permission controller, and other system components
     * that fire foreground RESUMED/PAUSED events but aren't apps the user intentionally opened.
     */
    private fun launchableAppPackages(context: Context): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return context.packageManager.queryIntentActivities(intent, 0)
            .mapTo(mutableSetOf()) { it.activityInfo.packageName }
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
        return mergeUsageMaps(daily, eventBased)
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
        val eventList = mutableListOf<RawUsageEvent>()
        val events = usm.queryEvents(startOfDay, now) ?: return emptyMap()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != launcherPkg) {
                eventList.add(RawUsageEvent(event.eventType, event.packageName, event.timeStamp))
            }
        }
        return accumulateUsageEvents(eventList, startOfDay, now)
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

/** Minimal event record extracted from [UsageEvents.Event] for pure accumulation logic. */
internal data class RawUsageEvent(val eventType: Int, val packageName: String, val timeStamp: Long)

/**
 * Pure accumulation of RESUMED/PAUSED events into per-package foreground milliseconds.
 *
 * Rules enforced here (tested independently of Android framework):
 * - ACTIVITY_STOPPED is ignored — it fires after every PAUSED and would double-count.
 * - A PAUSED with no prior RESUMED gets a one-time midnight carry-over credit (the app was
 *   already in the foreground when the query window started).
 * - [midnightCreditUsed] ensures that credit is applied at most once per package.
 * - SCREEN_NON_INTERACTIVE closes every currently open session at the screen-off timestamp,
 *   so a missed/delayed PAUSED (or an app still "resumed" while the screen is off) never
 *   accrues dead time — otherwise an open session would run all the way to [now].
 * - Open sessions at [now] are closed and counted.
 */
internal fun accumulateUsageEvents(
    events: List<RawUsageEvent>,
    startOfDay: Long,
    now: Long,
): Map<String, Long> {
    val resumedAt = mutableMapOf<String, Long>()
    val totals = mutableMapOf<String, Long>()
    val midnightCreditUsed = mutableSetOf<String>()
    for (e in events) {
        val pkg = e.packageName
        when (e.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> resumedAt[pkg] = e.timeStamp
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                val start = if (pkg in resumedAt) resumedAt.remove(pkg)!!
                else if (midnightCreditUsed.add(pkg)) startOfDay
                else null
                if (start != null) totals[pkg] = (totals[pkg] ?: 0L) + e.timeStamp - start
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                resumedAt.forEach { (openPkg, start) ->
                    totals[openPkg] = (totals[openPkg] ?: 0L) + e.timeStamp - start
                    midnightCreditUsed.add(openPkg)
                }
                resumedAt.clear()
            }
        }
    }
    resumedAt.forEach { (pkg, start) ->
        totals[pkg] = (totals[pkg] ?: 0L) + now - start
    }
    return totals
}

/**
 * Merges two per-package usage maps by taking the max value per package.
 * Packages present in only one map are included at their full value.
 */
internal fun mergeUsageMaps(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> {
    val allPkgs = a.keys + b.keys
    return allPkgs.associateWith { pkg -> maxOf(a[pkg] ?: 0L, b[pkg] ?: 0L) }
}

/**
 * Total "screen was actively in use" duration — the union of foreground intervals across all
 * packages, not their sum. Unlike [accumulateUsageEvents] (per-package totals, used for "Most
 * Used" ranking), this must not double-count overlapping foreground sessions: e.g. a video app
 * left resumed in Picture-in-Picture while the user browses a second app would otherwise be
 * counted twice, inflating "Screen time" past what Digital Wellbeing reports for the same day.
 */
internal fun computeForegroundUnionMs(
    events: List<RawUsageEvent>,
    startOfDay: Long,
    now: Long,
): Long {
    val sorted = events.sortedBy { it.timeStamp }
    val openPkgs = mutableSetOf<String>()
    val midnightCreditUsed = mutableSetOf<String>()
    var sessionStart: Long? = null
    var total = 0L

    fun closeSession(at: Long) {
        val start = sessionStart ?: return
        total += at - start
        sessionStart = null
    }

    for (e in sorted) {
        when (e.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                if (openPkgs.isEmpty()) sessionStart = e.timeStamp
                openPkgs.add(e.packageName)
            }
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                if (e.packageName !in openPkgs) {
                    if (!midnightCreditUsed.add(e.packageName)) continue
                    if (openPkgs.isEmpty()) sessionStart = startOfDay
                    openPkgs.add(e.packageName)
                }
                openPkgs.remove(e.packageName)
                if (openPkgs.isEmpty()) closeSession(e.timeStamp)
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                closeSession(e.timeStamp)
                openPkgs.clear()
            }
        }
    }
    closeSession(now)
    return total
}
