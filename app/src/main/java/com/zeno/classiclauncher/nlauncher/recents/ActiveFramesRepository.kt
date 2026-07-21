package com.zeno.classiclauncher.nlauncher.recents

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.core.content.getSystemService
import com.zeno.classiclauncher.nlauncher.root.RootManager
import com.zeno.classiclauncher.nlauncher.usage.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** One tile in the BB10-style "Active Frames" recent-apps screen. [thumbnail] is only ever
 *  non-null on the rooted path — the non-root path has no access to another app's rendered
 *  content, so tiles there fall back to just the app's own icon (see [ActiveFramesOverlay]). */
internal data class ActiveFrameTask(
    val taskId: Int,
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val lastActiveTime: Long,
    val thumbnail: Bitmap?,
)

/**
 * Two independent sources for the same feature, chosen by [rootGranted]:
 *
 * ROOTED: reads `dumpsys activity recents` for the live task list (id, package, last-active
 * time), and each task's last-known-state screenshot straight from the system's own task-
 * snapshot cache at /data/system_ce/<user>/snapshots/<taskId>_reduced.jpg — the same cache
 * that powers Android's real Recents/Overview screen, normally locked to the system launcher
 * only. Verified on-device (see conversation) before writing any of this.
 *
 * NON-ROOT: no access to that snapshot cache or to live task state for a third-party app, so
 * this falls back to UsageStatsManager's MOVE_TO_FOREGROUND event log (the same permission
 * this app's own usage-stats screen already asks for) to build a "recently used" ordering,
 * with no thumbnail — just each app's own icon.
 */
internal object ActiveFramesRepository {

    // Belt-and-suspenders against the same task reappearing: closeTask (below) now also runs
    // `am stack remove <taskId>` for the rooted path, which — confirmed on-device — deletes the
    // task from Android's own recents list outright (every task here has rootTaskId == taskId,
    // i.e. its own single-task stack, so this is safe/precise; `am help` only documents this at
    // stack granularity, but that's exactly this ROM's task/stack relationship in practice). Kept
    // as a fallback for the non-root path (or if that command ever silently no-ops on some
    // device/ROM): this object remembers the close ourselves and filters it out until the app is
    // genuinely reopened (a fresh MOVE_TO_FOREGROUND event after the close time).
    private val closedAt = mutableMapOf<String, Long>()

    suspend fun getRecentTasks(context: Context, rootGranted: Boolean): List<ActiveFrameTask> {
        val raw = if (rootGranted) getRecentTasksRooted(context) else withContext(Dispatchers.IO) {
            getRecentTasksNonRoot(context)
        }
        return withContext(Dispatchers.IO) { filterClosed(context, raw) }
    }

    /** Root-only: removes the task from Android's own recents list (`am stack remove`, so it's
     *  gone from the system Overview too, not just this screen) and force-stops the process.
     *  No-op (caller just removes the tile) when not rooted. [taskId] is null on the non-root
     *  path, where there's no real task list access anyway. */
    suspend fun closeTask(packageName: String, taskId: Int?, rootGranted: Boolean) {
        closedAt[packageName] = System.currentTimeMillis()
        if (!rootGranted) return
        if (taskId != null) RootManager.execute("am stack remove $taskId")
        RootManager.execute("am force-stop $packageName")
    }

    private fun filterClosed(context: Context, tasks: List<ActiveFrameTask>): List<ActiveFrameTask> {
        if (closedAt.isEmpty()) return tasks
        val usm = context.getSystemService<UsageStatsManager>()
        return tasks.filterNot { task ->
            val closedTime = closedAt[task.packageName] ?: return@filterNot false
            val reopened = usm != null && wasForegroundSince(usm, task.packageName, closedTime)
            if (reopened) closedAt.remove(task.packageName)
            !reopened
        }
    }

    private fun wasForegroundSince(usm: UsageStatsManager, packageName: String, since: Long): Boolean {
        val events = usm.queryEvents(since, System.currentTimeMillis()) ?: return false
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND && event.packageName == packageName) {
                return true
            }
        }
        return false
    }

    // ── Rooted path ──────────────────────────────────────────────────────────

    // The [AI]= capture excludes '}' — on-device output has no space before the closing brace
    // (e.g. "type=standard A=10241:com.instagram.lite}"), so a plain \S+ swallows it into the
    // package string and breaks getApplicationInfo() for every affinity-form ("A=") task.
    // Confirmed via `adb shell dumpsys activity recents` against this exact device.
    private val recentTaskLineRegex =
        Regex("""Task\{[0-9a-f]+ #(\d+) type=(\S+)(?: [AI]=([^\s}]+))?""")

    private suspend fun getRecentTasksRooted(context: Context): List<ActiveFrameTask> = coroutineScope {
        val dump = RootManager.readFull("dumpsys activity recents") ?: return@coroutineScope emptyList()
        val ownPackage = context.packageName
        val pm = context.packageManager

        data class ParsedTask(val taskId: Int, val type: String, val componentOrAffinity: String?)

        val parsed = mutableListOf<ParsedTask>()
        for (line in dump.lineSequence()) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("* Recent") && !trimmed.startsWith("Recent")) continue
            val match = recentTaskLineRegex.find(trimmed) ?: continue
            val taskId = match.groupValues[1].toIntOrNull() ?: continue
            val type = match.groupValues[2]
            val componentOrAffinity = match.groupValues[3].takeIf { it.isNotEmpty() }
            parsed += ParsedTask(taskId, type, componentOrAffinity)
        }

        val candidates = parsed
            .asSequence()
            .filter { it.type == "standard" }
            .mapNotNull { task ->
                val raw = task.componentOrAffinity ?: return@mapNotNull null
                // "pkg/.Activity" (I=) or "uid:pkg/.Activity" (A=, affinity form) — package is
                // whichever segment sits right before the first '/'.
                val withoutUid = raw.substringAfter(':', raw)
                val pkg = withoutUid.substringBefore('/')
                if (pkg.isEmpty() || pkg == ownPackage) null else task.taskId to pkg
            }
            .distinctBy { it.second } // one tile per package — keep the most-recent task for it
            .toList()

        val deferred = candidates.map { (taskId, pkg) ->
            async(Dispatchers.IO) {
                val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                    ?: return@async null
                val label = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkg)
                val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
                val thumbBytes = RootManager.readBytes(
                    "cat /data/system_ce/0/snapshots/${taskId}_reduced.jpg 2>/dev/null || " +
                        "cat /data/system_ce/0/snapshots/$taskId.jpg 2>/dev/null"
                )
                val thumb = thumbBytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() }
                ActiveFrameTask(
                    taskId = taskId,
                    packageName = pkg,
                    label = label,
                    icon = icon,
                    lastActiveTime = 0L,
                    thumbnail = thumb,
                )
            }
        }
        deferred.awaitAll().filterNotNull()
    }

    // ── Non-root path ────────────────────────────────────────────────────────

    private fun getRecentTasksNonRoot(context: Context): List<ActiveFrameTask> {
        if (!UsageStatsRepository.hasPermission(context)) return emptyList()
        val usm = context.getSystemService<UsageStatsManager>() ?: return emptyList()
        val ownPackage = context.packageName
        val pm = context.packageManager
        val now = System.currentTimeMillis()
        val windowStart = now - 24L * 60 * 60 * 1000
        val events = usm.queryEvents(windowStart, now) ?: return emptyList()

        val lastForeground = LinkedHashMap<String, Long>() // insertion order = discovery order
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
            val pkg = event.packageName ?: continue
            if (pkg == ownPackage) continue
            lastForeground[pkg] = event.timeStamp
        }

        return lastForeground.entries
            .sortedByDescending { it.value }
            .mapNotNull { (pkg, lastActive) ->
                val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
                // Skip packages with no launcher entry — background/service-only components that
                // still emit foreground events (IME, System UI, etc.) aren't "apps" the user
                // would recognize in this list.
                if (pm.getLaunchIntentForPackage(pkg) == null) return@mapNotNull null
                val label = runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrDefault(pkg)
                val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
                ActiveFrameTask(
                    taskId = -1,
                    packageName = pkg,
                    label = label,
                    icon = icon,
                    lastActiveTime = lastActive,
                    thumbnail = null,
                )
            }
    }
}
