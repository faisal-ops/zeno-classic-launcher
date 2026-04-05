package com.zeno.classiclauncher.nlauncher.usage

import android.app.AppOpsManager
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
     * Returns foreground usage time in milliseconds per package for the last 24 hours.
     * Returns an empty map if the permission has not been granted.
     */
    fun getLast24hUsage(context: Context): Map<String, Long> {
        if (!hasPermission(context)) return emptyMap()
        val usm = context.getSystemService<UsageStatsManager>() ?: return emptyMap()
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 24L * 60 * 60 * 1000,
            now,
        ) ?: return emptyMap()
        return stats
            .filter { it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }
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
}
