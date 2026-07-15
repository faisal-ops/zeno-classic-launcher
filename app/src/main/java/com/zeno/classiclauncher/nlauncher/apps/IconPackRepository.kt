package com.zeno.classiclauncher.nlauncher.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

data class IconPackEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

/** A pack's parsed appfilter.xml: static per-component icons, plus any `<calendar>` entries
 *  (component → drawable-name prefix, e.g. "ic_calendar_") for packs that ship their own
 *  day-of-month-aware calendar icon set. */
private data class ParsedAppFilter(
    val items: Map<ComponentName, String>,
    val calendarPrefixes: Map<ComponentName, String>,
)

class IconPackRepository(private val context: Context) {
    private val pm = context.packageManager
    private val parsedPacks = ConcurrentHashMap<String, ParsedAppFilter>()
    private val drawableCache = ConcurrentHashMap<String, Drawable>()
    /** "$packageName:$prefix" → true only if all 31 numbered drawables actually resolve. An
     *  incomplete set (missing even one day) is never trusted — falls back to our own
     *  synthesized badge for every day rather than mixing pack icons with the badge per-day. */
    private val completeCalendarSets = ConcurrentHashMap<String, Boolean>()

    @Volatile private var cachedIconPacks: List<IconPackEntry>? = null

    fun invalidateIconPackListCache() { cachedIconPacks = null }

    suspend fun installedIconPacks(): List<IconPackEntry> {
        cachedIconPacks?.let { return it }
        return withContext(Dispatchers.IO) { queryInstalledIconPacks().also { cachedIconPacks = it } }
    }

    private fun queryInstalledIconPacks(): List<IconPackEntry> {
        // Collect ApplicationInfo from every known icon-pack discovery intent.
        // Many icon packs don't register a CATEGORY_LAUNCHER activity, so querying
        // only that intent misses them. We also query the standard icon pack action
        // strings that packs declare in their manifests.
        val discoveryIntents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            Intent("org.adw.launcher.THEMES"),
            Intent("com.novalauncher.THEME"),
            Intent("com.teslacoilsw.launcher.THEME"),
            Intent("com.gau.go.launcherex.theme"),
            Intent("org.adw.launcher.icons.ACTION_PICK_ICON"),
        )

        return discoveryIntents
            .flatMap { pm.queryIntentActivities(it, 0) }
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .filter { hasAppFilter(it.packageName) }
            .map { app ->
                IconPackEntry(
                    packageName = app.packageName,
                    label = runCatching { pm.getApplicationLabel(app).toString() }.getOrDefault(app.packageName),
                    icon = runCatching { pm.getApplicationIcon(app.packageName) }.getOrNull(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun applyIconPack(
        apps: List<AppEntry>,
        iconPackPackage: String,
        customIconPackages: Set<String> = emptySet(),
    ): List<AppEntry> = withContext(Dispatchers.IO) {
        val pack = iconPackPackage.trim()
        if (pack.isEmpty()) return@withContext apps
        val parsed = loadMappings(pack)
        if (parsed.items.isEmpty() && parsed.calendarPrefixes.isEmpty()) return@withContext apps
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        apps.map { app ->
            if (app.packageName in customIconPackages) {
                app
            } else if (app.hasDynamicIcon) {
                // Dynamic date alias (real OS per-day icon, or our own synthesized day badge) —
                // a *static* icon-pack `<item>` icon must never replace it (would freeze the
                // calendar icon at the wrong day forever). A pack's own `<calendar>` entry is
                // itself day-aware though, so it's allowed to win over both the OS icon and our
                // synthesized fallback — that's exactly what the pack author built it for. Only
                // trusted if the pack's 31-day set is actually complete; an incomplete set falls
                // back to whatever was already resolved (OS icon or our badge) for every day.
                val prefix = app.componentName?.let { parsed.calendarPrefixes[it] }
                    ?: parsed.calendarPrefixes.entries.firstOrNull { it.key.packageName == app.packageName }?.value
                val dayIcon = if (prefix != null && hasComplete31DaySet(pack, prefix)) {
                    loadPackDrawable(pack, "$prefix$today")
                } else {
                    null
                }
                if (dayIcon != null) app.copy(icon = dayIcon) else app
            } else {
                val drawableName = app.componentName?.let { parsed.items[it] }
                    ?: parsed.items.entries.firstOrNull { it.key.packageName == app.packageName }?.value
                val themedIcon = drawableName?.let { loadPackDrawable(pack, it) }
                if (themedIcon != null) app.copy(icon = themedIcon) else app
            }
        }
    }

    private fun hasComplete31DaySet(packageName: String, prefix: String): Boolean =
        completeCalendarSets.getOrPut("$packageName:$prefix") {
            runCatching {
                val res = pm.getResourcesForApplication(packageName)
                (1..31).all { day ->
                    val name = "$prefix$day"
                    res.getIdentifier(name, "drawable", packageName) != 0 ||
                        res.getIdentifier(name, "mipmap", packageName) != 0
                }
            }.getOrDefault(false)
        }

    private fun hasAppFilter(packageName: String): Boolean =
        runCatching {
            val res = pm.getResourcesForApplication(packageName)
            findAppFilterResourceId(res, packageName) != 0
        }.getOrDefault(false)

    private fun loadMappings(packageName: String): ParsedAppFilter =
        parsedPacks.getOrPut(packageName) {
            runCatching {
                val res = pm.getResourcesForApplication(packageName)
                val appFilterId = findAppFilterResourceId(res, packageName)
                if (appFilterId == 0) return@runCatching ParsedAppFilter(emptyMap(), emptyMap())
                parseAppFilter(res.getXml(appFilterId))
            }.getOrDefault(ParsedAppFilter(emptyMap(), emptyMap()))
        }

    private fun findAppFilterResourceId(res: Resources, packageName: String): Int {
        val names = listOf("appfilter", "drawable", "icon_pack")
        for (name in names) {
            val id = res.getIdentifier(name, "xml", packageName)
            if (id != 0) return id
        }
        return 0
    }

    private fun parseAppFilter(parser: XmlPullParser): ParsedAppFilter {
        val items = LinkedHashMap<ComponentName, String>()
        val calendarPrefixes = LinkedHashMap<ComponentName, String>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "item" -> {
                        val componentRaw = parser.getAttributeValue(null, "component").orEmpty()
                        val drawableName = parser.getAttributeValue(null, "drawable").orEmpty()
                        val component = parseIconPackComponent(componentRaw)
                        if (component != null && drawableName.isNotBlank()) {
                            items[component] = drawableName.trim()
                        }
                    }
                    "calendar" -> {
                        val componentRaw = parser.getAttributeValue(null, "component").orEmpty()
                        // Some packs put a stray space before the value ("prefix= \"...\"") — the
                        // attribute name itself is still exactly "prefix", so this parses fine.
                        val prefix = parser.getAttributeValue(null, "prefix").orEmpty()
                        val component = parseIconPackComponent(componentRaw)
                        if (component != null && prefix.isNotBlank()) {
                            calendarPrefixes[component] = prefix.trim()
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return ParsedAppFilter(items, calendarPrefixes)
    }

    private fun parseIconPackComponent(raw: String): ComponentName? {
        val inner = raw
            .trim()
            .removePrefix("ComponentInfo{")
            .removeSuffix("}")
        val slash = inner.indexOf('/')
        if (slash <= 0 || slash >= inner.lastIndex) return null
        val pkg = inner.take(slash)
        val clsRaw = inner.substring(slash + 1)
        val cls = if (clsRaw.startsWith(".")) pkg + clsRaw else clsRaw
        return ComponentName(pkg, cls)
    }

    private fun loadPackDrawable(packageName: String, drawableName: String): Drawable? {
        val cacheKey = "$packageName:$drawableName"
        drawableCache[cacheKey]?.let { return it.constantState?.newDrawable()?.mutate() ?: it }
        val loaded = runCatching {
            val res = pm.getResourcesForApplication(packageName)
            val id = res.getIdentifier(drawableName, "drawable", packageName)
                .takeIf { it != 0 }
                ?: res.getIdentifier(drawableName, "mipmap", packageName).takeIf { it != 0 }
                ?: return@runCatching null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                res.getDrawable(id, null)
            } else {
                @Suppress("DEPRECATION")
                res.getDrawable(id)
            }
        }.getOrNull()
        if (loaded != null) drawableCache[cacheKey] = loaded
        return loaded?.constantState?.newDrawable()?.mutate() ?: loaded
    }
}
