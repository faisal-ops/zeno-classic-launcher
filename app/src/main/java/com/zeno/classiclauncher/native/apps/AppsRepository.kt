package com.zeno.classiclauncher.nlauncher.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.zeno.classiclauncher.nlauncher.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AppsRepository(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    /** Warm size for typical device app counts; reduces map resize churn when listing the grid. */
    private val iconCache = ConcurrentHashMap<String, Drawable>(256)

    companion object {
        const val INTERNAL_SETTINGS_PACKAGE = "classiclauncher.internal.settings"
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getCachedIcon(pkg: String): Drawable? =
        iconCache[pkg] ?: runCatching { pm.getApplicationIcon(pkg) }.getOrNull()?.also { iconCache[pkg] = it }

    private suspend fun loadLaunchableApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val installed = ArrayList<AppEntry>(resolveInfos.size + 1)
        val seenPackages = HashSet<String>(resolveInfos.size)

        for (ri in resolveInfos) {
            val ai = ri.activityInfo?.applicationInfo ?: continue
            val pkg = ai.packageName ?: continue
            if (pkg == context.packageName) continue
            if (!seenPackages.add(pkg)) continue  // skip apps with multiple launcher activities
            val label = pm.getApplicationLabel(ai)?.toString() ?: pkg
            installed.add(AppEntry(packageName = pkg, label = label, icon = getCachedIcon(pkg)))
        }

        installed.sortWith(APP_LABEL_COMPARATOR)

        val settingsIcon = runCatching { context.getDrawable(R.drawable.ic_dock_settings) }.getOrNull()
        installed.add(
            0,
            AppEntry(
                packageName = INTERNAL_SETTINGS_PACKAGE,
                label = context.getString(R.string.home_menu_settings_title),
                icon = settingsIcon,
                internal = true,
            ),
        )
        installed
    }

    fun appsFlow(): Flow<List<AppEntry>> = callbackFlow {
        suspend fun emitNow() {
            trySend(loadLaunchableApps())
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                val pkg = i?.data?.schemeSpecificPart
                if (pkg != null) {
                    when (i.action) {
                        Intent.ACTION_PACKAGE_ADDED,
                        Intent.ACTION_PACKAGE_REPLACED,
                        Intent.ACTION_PACKAGE_REMOVED -> iconCache.remove(pkg)
                    }
                }
                launch { emitNow() }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        launch { emitNow() }

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
}

/** Splits a home-strip token into package name and optional shortcut id (after first `#`). */
fun parseHomeShortcutToken(token: String): Pair<String, String?> {
    val t = token.trim()
    val idx = t.indexOf('#')
    if (idx < 0) return t to null
    val pkg = t.take(idx).trim()
    val sid = t.substring(idx + 1).trim().takeIf { it.isNotEmpty() }
    return pkg to sid
}

/** Serialized form for prefs: plain package, or `package#shortcutId` for pinned shortcuts. */
fun homeShortcutStorageToken(packageName: String, shortcutId: String?): String {
    val pkg = packageName.trim()
    val sid = shortcutId?.trim().orEmpty()
    return if (sid.isEmpty()) pkg else "$pkg#$sid"
}

private val APP_LABEL_COMPARATOR = compareBy<AppEntry> { it.label.lowercase() }
