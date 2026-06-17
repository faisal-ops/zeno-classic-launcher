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

class IconPackRepository(private val context: Context) {
    private val pm = context.packageManager
    private val parsedPacks = ConcurrentHashMap<String, Map<ComponentName, String>>()
    private val drawableCache = ConcurrentHashMap<String, Drawable>()

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
        val mappings = loadMappings(pack)
        if (mappings.isEmpty()) return@withContext apps
        apps.map { app ->
            if (app.internal || app.packageName in customIconPackages || app.hasDynamicIcon) {
                // Skip: internal app, user custom icon, or dynamic date alias (e.g. calendar) —
                // the live PackageManager icon must not be replaced by a static icon pack icon.
                app
            } else {
                val drawableName = app.componentName?.let { mappings[it] }
                    ?: mappings.entries.firstOrNull { it.key.packageName == app.packageName }?.value
                val themedIcon = drawableName?.let { loadPackDrawable(pack, it) }
                if (themedIcon != null) app.copy(icon = themedIcon) else app
            }
        }
    }

    private fun hasAppFilter(packageName: String): Boolean =
        runCatching {
            val res = pm.getResourcesForApplication(packageName)
            findAppFilterResourceId(res, packageName) != 0
        }.getOrDefault(false)

    private fun loadMappings(packageName: String): Map<ComponentName, String> =
        parsedPacks.getOrPut(packageName) {
            runCatching {
                val res = pm.getResourcesForApplication(packageName)
                val appFilterId = findAppFilterResourceId(res, packageName)
                if (appFilterId == 0) return@runCatching emptyMap()
                parseAppFilter(res.getXml(appFilterId))
            }.getOrDefault(emptyMap())
        }

    private fun findAppFilterResourceId(res: Resources, packageName: String): Int {
        val names = listOf("appfilter", "drawable", "icon_pack")
        for (name in names) {
            val id = res.getIdentifier(name, "xml", packageName)
            if (id != 0) return id
        }
        return 0
    }

    private fun parseAppFilter(parser: XmlPullParser): Map<ComponentName, String> {
        val out = LinkedHashMap<ComponentName, String>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val componentRaw = parser.getAttributeValue(null, "component").orEmpty()
                val drawableName = parser.getAttributeValue(null, "drawable").orEmpty()
                val component = parseIconPackComponent(componentRaw)
                if (component != null && drawableName.isNotBlank()) {
                    out[component] = drawableName.trim()
                }
            }
            eventType = parser.next()
        }
        return out
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
