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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.prefs.AppIconShape
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AppsRepository(private val context: Context, private val prefsRepository: LauncherPrefsRepository) {
    private val pm: PackageManager = context.packageManager

    /** Current icon shape, kept in sync by [appsFlow]'s prefs collector so synthesized
     *  icons (e.g. the calendar badge) can mirror whatever shape the user has chosen
     *  instead of baking in a fixed corner style. */
    @Volatile private var currentIconShape: AppIconShape = AppIconShape.SOFT_SQUARE

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

        private const val CALENDAR_ICON_ARRAY_META_KEY = "com.teslacoilsw.launcher.calendarIconArray"
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
     * Outline for [shape] at [size]px, mirroring the corner styles Compose applies via
     * `iconMaskShape()` in LauncherScreen.kt. There's no shared Canvas-usable definition of
     * those shapes (that helper returns a Compose `Shape`, usable only via `Modifier.clip`),
     * so the corner-radius fractions here are a plain-Canvas approximation of the same look.
     */
    private fun shapedIconOutline(shape: AppIconShape, size: Float): Path = Path().apply {
        when (shape) {
            AppIconShape.SQUARE -> addRect(0f, 0f, size, size, Path.Direction.CW)
            AppIconShape.CIRCLE -> addOval(0f, 0f, size, size, Path.Direction.CW)
            AppIconShape.SQUIRCLE -> {
                val r = size * 0.35f
                addRoundRect(0f, 0f, size, size, r, r, Path.Direction.CW)
            }
            AppIconShape.CUT_CORNER -> {
                val cut = size * 0.18f
                moveTo(cut, 0f)
                lineTo(size - cut, 0f)
                lineTo(size, cut)
                lineTo(size, size - cut)
                lineTo(size - cut, size)
                lineTo(cut, size)
                lineTo(0f, size - cut)
                lineTo(0f, cut)
                close()
            }
            AppIconShape.ROUNDED, AppIconShape.SOFT_SQUARE -> {
                val r = size * 0.17f
                addRoundRect(0f, 0f, size, size, r, r, Path.Direction.CW)
            }
        }
    }

    /**
     * Synthesizes a Google-Calendar-style icon (white card, colored header strip with the
     * month, big day number) for calendar apps whose installed build doesn't ship its own
     * date-adaptive icon resources. Shape mirrors [currentIconShape] so it stays consistent
     * with the rest of the grid. Cached under the package name like any other icon, and
     * evicted at midnight by the same [appsFlow] date-change listener that clears real
     * dynamic icons, and whenever the icon shape preference changes.
     */
    private fun buildCalendarBadgeIcon(day: Int, dayOfWeek: String, shape: AppIconShape): Drawable {
        val size = ICON_SIZE_PX
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.clipPath(shapedIconOutline(shape, size.toFloat()))
        canvas.drawColor(Color.WHITE)

        val headerHeight = size * 0.26f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EA4335") }
        canvas.drawRect(0f, 0f, size.toFloat(), headerHeight, headerPaint)

        // Full day-of-week names ("Wednesday") vary a lot in width vs. the old fixed 3-letter
        // month abbreviation — shrink-to-fit so longer names (Wednesday/Saturday) don't overflow
        // the header while short ones (Sunday) still read at a decent size. Uses getTextBounds
        // (actual rendered glyph width) rather than measureText, which over-reports width when
        // combined with a custom letterSpacing and was shrinking every name — even short ones —
        // down toward the floor.
        val headerMaxWidth = size * 0.9f
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        var dayTextSize = size * 0.135f
        val minDayTextSize = size * 0.09f
        val dayBounds = Rect()
        dayPaint.textSize = dayTextSize
        dayPaint.getTextBounds(dayOfWeek, 0, dayOfWeek.length, dayBounds)
        while (dayBounds.width() > headerMaxWidth && dayTextSize > minDayTextSize) {
            dayTextSize -= size * 0.004f
            dayPaint.textSize = dayTextSize
            dayPaint.getTextBounds(dayOfWeek, 0, dayOfWeek.length, dayBounds)
        }
        canvas.drawText(dayOfWeek, size / 2f, headerHeight * 0.68f, dayPaint)

        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3C4043")
            textAlign = Paint.Align.CENTER
            textSize = size * 0.42f
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
        val now = Calendar.getInstance()
        val day = now.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(now.time)
        return buildCalendarBadgeIcon(day, dayOfWeek, currentIconShape).also { iconCache[pkg] = it }
    }

    /**
     * Third-party-launcher convention (originated by Nova Launcher, also honoured by Action
     * Launcher/Lawnchair) for apps that don't use the AOSP 31-activity-alias trick: the
     * launcher activity declares a `<meta-data>` pointing at an `<array>` resource of 31 (or
     * 366, for day-of-year) drawable references, one per day, and the launcher itself is
     * expected to pick the right one. BB Calendar ships this instead of per-day aliases.
     */
    private fun getCachedCalendarArrayIcon(pkg: String, metaData: android.os.Bundle?): Drawable? {
        iconCache[pkg]?.let { return it }
        val arrayResId = metaData?.getInt(CALENDAR_ICON_ARRAY_META_KEY, 0)?.takeIf { it != 0 } ?: return null
        return runCatching {
            val res = pm.getResourcesForApplication(pkg)
            val typedArray = res.obtainTypedArray(arrayResId)
            val count = typedArray.length()
            val now = Calendar.getInstance()
            val index = when (count) {
                365, 366 -> now.get(Calendar.DAY_OF_YEAR) - 1
                else -> now.get(Calendar.DAY_OF_MONTH) - 1
            }.coerceIn(0, count - 1)
            val dayResId = typedArray.getResourceId(index, 0)
            typedArray.recycle()
            if (dayResId == 0) null else {
                val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    res.getDrawable(dayResId, null)
                } else {
                    @Suppress("DEPRECATION") res.getDrawable(dayResId)
                }
                raw?.let { flattenAdaptiveIcon(it) }
            }
        }.getOrNull()?.also { iconCache[pkg] = it }
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
        // GET_META_DATA is required to see the calendarIconArray meta-data some calendar apps
        // (e.g. BB Calendar) declare instead of the AOSP per-day-activity-alias trick.
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
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
            // Nova/Teslacoil-style calendarIconArray meta-data: a single activity exposing a
            // resource array of 31/366 day-specific drawables instead of per-day aliases (BB
            // Calendar uses this).
            val calendarArrayIcon = if (!activityHasOwnIcon && pkg in calendarPackages) {
                getCachedCalendarArrayIcon(pkg, ri.activityInfo.metaData)
            } else null
            // Declares itself a calendar app but its installed build has no per-day icon
            // resources of its own — synthesize a day-number badge instead.
            val needsSynthesizedBadge = !activityHasOwnIcon && calendarArrayIcon == null && pkg in calendarPackages
            val hasRealDynamicIcon = activityHasOwnIcon || calendarArrayIcon != null

            val existingIdx = seenPackages[pkg]
            if (existingIdx != null) {
                // Duplicate package. Upgrade the stored entry only if this alias has a
                // distinct icon and the stored entry did not (base activity came first).
                if (hasRealDynamicIcon) {
                    iconCache.remove(pkg)  // evict base-activity icon so we re-fetch the alias
                    installed[existingIdx] = installed[existingIdx].copy(
                        icon = if (activityHasOwnIcon) getCachedIcon(pkg, component) else calendarArrayIcon,
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
                    calendarArrayIcon != null -> calendarArrayIcon
                    needsSynthesizedBadge -> getCachedCalendarBadgeIcon(pkg)
                    else -> getCachedIcon(pkg, null)
                },
                componentName = component,
                hasDynamicIcon = hasRealDynamicIcon || needsSynthesizedBadge,
            ))
        }

        installed.sortWith(APP_LABEL_COMPARATOR)


        // User-set custom icon (see the app-drawer "Change Icon" menu, available for every app
        // including this internal entry) takes priority over the default gear icon.
        val settingsIcon = CustomIconStore.load(context, INTERNAL_SETTINGS_PACKAGE)
            ?: runCatching { context.getDrawable(R.drawable.ic_dock_settings) }.getOrNull()
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

    /**
     * Every LAUNCHER activity as its own entry, unlike [loadLaunchableApps] which collapses
     * same-package aliases (Google Calendar's day icons) into one entry for the drawer/dock.
     * Gesture pickers need per-activity granularity so a second entry point in the same app
     * (e.g. a companion "quick view" activity) is individually selectable. The first activity
     * found per package keeps a bare package token (backward-compatible with existing stored
     * gesture prefs); later activities with a genuinely different label get
     * "pkg#component.class.Name" — the exact shape [homeShortcutStorageToken] already produces
     * for pinned shortcuts, so [parseHomeShortcutToken] recovers (pkg, component) at launch
     * time (see [LauncherActions.launchApp]). Same-label aliases (icon-only variants) are still
     * collapsed, so calendar-style apps don't explode into dozens of entries here either.
     */
    suspend fun loadGestureTargetApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val seenLabelsByPackage = HashMap<String, MutableSet<String>>()
        val firstTokenIssued = HashSet<String>()

        resolveInfos.mapNotNull { ri ->
            val ai = ri.activityInfo?.applicationInfo ?: return@mapNotNull null
            val pkg = ai.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null

            val label = runCatching { ri.loadLabel(pm)?.toString() }.getOrNull()
                ?: pm.getApplicationLabel(ai)?.toString() ?: pkg

            // Same label as an already-emitted entry for this package — an icon-only alias
            // (e.g. Google Calendar's day icons), not a genuinely distinct entry point. Skip,
            // matching loadLaunchableApps' collapse behavior.
            val labelsSeen = seenLabelsByPackage.getOrPut(pkg) { mutableSetOf() }
            if (!labelsSeen.add(label)) return@mapNotNull null

            val component = ComponentName(pkg, ri.activityInfo.name)
            val token = if (firstTokenIssued.add(pkg)) pkg else homeShortcutStorageToken(pkg, ri.activityInfo.name)

            AppEntry(
                packageName = token,
                label = label,
                icon = getCachedIcon(pkg, component),
                componentName = component,
            )
        }.sortedWith(APP_LABEL_COMPARATOR)
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

        val shapeFlow = prefsRepository.prefsFlow.map { it.appIconShape }.distinctUntilChanged()
        launch {
            currentIconShape = shapeFlow.first()
            emitNow()
            // Re-emit (with a cleared cache) whenever the user changes the icon shape, so
            // synthesized icons like the calendar badge pick up the new shape immediately.
            shapeFlow.drop(1).collect { shape ->
                currentIconShape = shape
                iconCache.clear()
                emitNow()
            }
        }

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
