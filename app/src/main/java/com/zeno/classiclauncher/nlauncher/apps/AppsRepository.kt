package com.zeno.classiclauncher.nlauncher.apps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.zeno.classiclauncher.nlauncher.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AppsRepository(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    /** Warm size for typical device app counts; reduces map resize churn when listing the grid. */
    private val iconCache = ConcurrentHashMap<String, Drawable>(256)

    /** Emitting to this triggers a full app list reload (e.g. after custom icon change). */
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Evicts [pkg] from the icon cache and signals [appsFlow] to re-emit. */
    fun invalidateAndRefresh(pkg: String) {
        iconCache.remove(pkg)
        refreshTrigger.tryEmit(Unit)
    }

    companion object {
        const val INTERNAL_SETTINGS_PACKAGE = "classiclauncher.internal.settings"

        /**
         * Icon raster size in pixels. 192px gives sharp icons on xxhdpi (3×) and higher.
         * Keep this a power-of-two multiple of 48dp so downscaling stays clean.
         */
        private const val ICON_SIZE_PX = 192
    }

    /**
     * Flatten an [AdaptiveIconDrawable] into a plain [BitmapDrawable] by compositing
     * the background and foreground layers directly — without applying the system's icon
     * mask path. This lets our own Compose `.clip()` control the shape instead of the
     * ROM's adaptive-icon mask (which on LineageOS / Android 16+ is baked in during
     * [Drawable.draw] and would override our per-preference shape).
     *
     * For non-adaptive drawables the input is returned unchanged.
     */
    @Suppress("DEPRECATION")
    private fun flattenAdaptiveIcon(drawable: Drawable): Drawable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return drawable
        if (drawable !is AdaptiveIconDrawable) return drawable

        val size = ICON_SIZE_PX
        // AdaptiveIconDrawable assets are designed for a 108×108 canvas where only the
        // centre 72×72 is the "safe zone". To avoid showing bleed content, we draw the
        // layers into a canvas that is slightly larger and let the bitmap bounds crop it.
        // inset = size × (1/8) / (1 + 2/8) ≈ size × 0.1
        val inset = (size * 0.1f).toInt()

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.background?.let { bg ->
            bg.setBounds(-inset, -inset, size + inset, size + inset)
            bg.draw(canvas)
        }
        drawable.foreground?.let { fg ->
            fg.setBounds(-inset, -inset, size + inset, size + inset)
            fg.draw(canvas)
        }

        return BitmapDrawable(context.resources, bitmap)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getCachedIcon(pkg: String, component: ComponentName? = null): Drawable? {
        iconCache[pkg]?.let { return it }
        // Custom icon takes priority over PackageManager icon
        CustomIconStore.load(context, pkg)?.also { iconCache[pkg] = it; return it }
        // Use getActivityIcon only when the activity has a DISTINCT icon from the application —
        // this is the Google Calendar pattern (31 day-specific aliases, each with its own icon).
        // For apps like Etar whose AllInOneActivity has its own static resource but it's NOT
        // date-adaptive, getApplicationIcon is correct and avoids loading the wrong resource.
        val raw = if (component != null) {
            runCatching { pm.getActivityIcon(component) }.getOrNull()
                ?: runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
        } else {
            runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
        }
        return raw?.let { flattenAdaptiveIcon(it) }?.also { iconCache[pkg] = it }
    }

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
            val component = ComponentName(pkg, ri.activityInfo.name)
            // Pass component only when the activity defines its OWN icon distinct from the app icon.
            // This covers Google Calendar-style day aliases (each alias has a date-specific icon).
            // For Etar and normal apps whose activity inherits the app icon, pass null so we use
            // getApplicationIcon — avoids loading an incorrect activity-specific static resource.
            val activityHasOwnIcon = ri.activityInfo.icon != 0 &&
                ri.activityInfo.icon != ri.activityInfo.applicationInfo.icon
            installed.add(AppEntry(
                packageName = pkg,
                label = label,
                icon = getCachedIcon(pkg, if (activityHasOwnIcon) component else null),
                componentName = component,
            ))
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
                when (i?.action) {
                    Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED -> {
                        // Date changed — evict all icons so apps like Google Calendar
                        // (which show today's date via a per-day activity alias) refresh.
                        iconCache.clear()
                        launch { emitNow() }
                    }
                    else -> {
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
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        // DATE_CHANGED / TIME_CHANGED cannot have a data scheme — register separately
        val dateFilter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            // DATE_CHANGED / TIME_CHANGED are system broadcasts — must be RECEIVER_EXPORTED
            context.registerReceiver(receiver, dateFilter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, dateFilter)
        }

        launch { emitNow() }

        // Re-emit when a custom icon is saved or cleared
        launch { refreshTrigger.collect { emitNow() } }

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
