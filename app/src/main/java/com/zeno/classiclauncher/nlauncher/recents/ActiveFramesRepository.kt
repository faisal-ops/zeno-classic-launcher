package com.zeno.classiclauncher.nlauncher.recents

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.core.content.getSystemService
import com.zeno.classiclauncher.nlauncher.root.RootManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** One tile in the classic-style "Active Frames" recent-apps screen. [thumbnail] is null only if
 *  reading the task's snapshot cache fails for that specific task. */
internal data class ActiveFrameTask(
    val taskId: Int,
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val lastActiveTime: Long,
    val thumbnail: Bitmap?,
)

/**
 * Root-only: reads `dumpsys activity recents` for the live task list (id, package, last-active
 * time), and each task's last-known-state screenshot straight from the system's own task-
 * snapshot cache at /data/system_ce/<user>/snapshots/<taskId>_reduced.jpg — the same cache that
 * powers Android's real Recents/Overview screen, normally locked to the system launcher only.
 * Verified on-device before writing any of this.
 *
 * There's no non-root fallback: without root there's no access to that snapshot cache or to live
 * task state for a third-party app, so a UsageStatsManager-based approximation was tried and
 * dropped — it can't tell "task evicted from real recents" from "just not opened recently", and
 * showed stale entries the real Recents screen no longer had. Root-only avoids presenting
 * approximate data as if it were the real list.
 */
internal object ActiveFramesRepository {

    // Force-stopping a process, or even removing its task via `am stack remove`, doesn't always
    // stop it from being re-listed in a mid-air race with the very next getRecentTasks() call —
    // this object remembers the close ourselves and filters it out until the app is genuinely
    // reopened (a fresh MOVE_TO_FOREGROUND event after the close time), as a belt-and-suspenders
    // on top of the `am stack remove` call in closeTask.
    private val closedAt = mutableMapOf<String, Long>()

    suspend fun getRecentTasks(context: Context): List<ActiveFrameTask> {
        val raw = getRecentTasksRooted(context)
        return withContext(Dispatchers.IO) { filterClosed(context, raw) }
    }

    /** Removes the task from Android's own recents list (`am stack remove`, so it's gone from
     *  the system Overview too, not just this screen) and force-stops the process. */
    suspend fun closeTask(packageName: String, taskId: Int) {
        closedAt[packageName] = System.currentTimeMillis()
        RootManager.execute("am stack remove $taskId")
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
}
