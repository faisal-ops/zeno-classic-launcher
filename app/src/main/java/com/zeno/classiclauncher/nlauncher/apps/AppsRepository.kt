package com.zeno.classiclauncher.nlauncher.apps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import java.util.Calendar
import com.zeno.classiclauncher.nlauncher.R
import kotlinx.coroutines.channels.BufferOverflow
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

    /** Emitting to this triggers a full app list reload (e.g. after custom icon change).
     *  DROP_OLDEST ensures tryEmit never silently fails — two rapid invalidations both
     *  result in a reload, just coalesced into one. */
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

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

    /**
     * Synthesizes a Google-Calendar-style icon (white card, colored header strip, big day
     * number) for calendar apps whose installed build doesn't ship its own date-adaptive
     * icon resources. Cached under the package name like any other icon, and evicted at
     * midnight by the same [appsFlow] date-change listener that clears real dynamic icons.
     */
    private fun buildCalendarBadgeIcon(day: Int): Drawable {
        val size = ICON_SIZE_PX
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = size * 0.22f
        val outline = Path().apply {
            addRoundRect(0f, 0f, size.toFloat(), size.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(outline)
        canvas.drawColor(Color.WHITE)

        val headerHeight = size * 0.24f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EA4335") }
        canvas.drawRect(0f, 0f, size.toFloat(), headerHeight, headerPaint)

        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3C4043")
            textAlign = Paint.Align.CENTER
            textSize = size * 0.46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val text = day.toString()
        val textBounds = Rect()
        numberPaint.getTextBounds(text, 0, text.length, textBounds)
        val cx = size / 2f
        val cy = headerHeight + (size - headerHeight) / 2f - (textBounds.top + textBounds.bottom) / 2f
        canvas.drawText(text, cx, cy, numberPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun getCachedCalendarBadgeIcon(pkg: String): Drawable {
        iconCache[pkg]?.let { return it }
        val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        return buildCalendarBadgeIcon(day).also { iconCache[pkg] = it }
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
        // Standard Android signal an app uses to mark itself as a calendar app (same category
        // launchers use to find "the" calendar app for shortcuts) — no package names hardcoded.
        val calendarPackages = runCatching {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR), 0)
                .mapNotNull { it.activityInfo?.applicationInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet())
        val installed = ArrayList<AppEntry>(resolveInfos.size + 1)
        // Maps package name → index in `installed`. Using a map (not a set) so that a later
        // activity alias with a distinct date-specific icon (Google Calendar, BB Hub+ Calendar)
        // can upgrade an already-inserted entry that had only the generic app icon.
        val seenPackages = HashMap<String, Int>(resolveInfos.size)

        for (ri in resolveInfos) {
            val ai = ri.activityInfo?.applicationInfo ?: continue
            val pkg = ai.packageName ?: continue
            if (pkg == context.packageName) continue
            val component = ComponentName(pkg, ri.activityInfo.name)
            // True when the activity defines its OWN icon distinct from the application icon —
            // the Google Calendar / BB Hub+ Calendar pattern: 31 day aliases, one enabled per day.
            val activityHasOwnIcon = ri.activityInfo.icon != 0 &&
                ri.activityInfo.icon != ri.activityInfo.applicationInfo.icon
            // Declares itself a calendar app but its installed build has no per-day icon
            // resources of its own — synthesize a day-number badge instead.
            val needsSynthesizedBadge = !activityHasOwnIcon && pkg in calendarPackages

            val existingIdx = seenPackages[pkg]
            if (existingIdx != null) {
                // Duplicate package. Upgrade the stored entry only if this alias has a
                // distinct icon and the stored entry did not (base activity came first).
                if (activityHasOwnIcon) {
                    iconCache.remove(pkg)  // evict base-activity icon so we re-fetch the alias
                    installed[existingIdx] = installed[existingIdx].copy(
                        icon = getCachedIcon(pkg, component),
                        componentName = component,
                        hasDynamicIcon = true,
                    )
                }
                continue
            }

            val label = pm.getApplicationLabel(ai)?.toString() ?: pkg
            seenPackages[pkg] = installed.size
            installed.add(AppEntry(
                packageName = pkg,
                label = label,
                icon = when {
                    activityHasOwnIcon -> getCachedIcon(pkg, component)
                    needsSynthesizedBadge -> getCachedCalendarBadgeIcon(pkg)
                    else -> getCachedIcon(pkg, null)
                },
                componentName = component,
                hasDynamicIcon = activityHasOwnIcon || needsSynthesizedBadge,
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
