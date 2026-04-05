package com.zeno.classiclauncher.nlauncher.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * Native launcher theme: parses legacy flat keys plus Flutter-shaped nested JSON
 * (`launcherTheme`-style: `settingsTheme`, `navBarTheme`, `pageIndicatorTheme`, `appGridTheme`, `selectorTheme`, …).
 *
 * **LOCKED UI defaults (Mar 2026):** Do not tweak spacing, font sizes, or dock/grid metrics in drive-by refactors.
 * Change these only when deliberately revisiting layout/UX. Canonical numbers: grid icon 52, label 15sp, gaps 9,
 * card radius 7, dock height 74 / dock icons 43 / spacing 14, selector radius 5. Search strip: DrawerSearchBar (~45dp, #202020).
 */
data class LauncherThemePalette(
    val longPressActionDurationMs: Long = 800L,
    // Legacy flat (still supported)
    val settingsBg: Color = Color(0xFF0E131B),
    val settingsSelected: Color = Color(0xFF2B6CB0),
    val dockSelected: Color = Color(0x664FC3F7),
    val appCardTop: Color = Color(0x332A3648),
    val appCardBottom: Color = Color(0x22131A24),
    // settingsTheme
    val settingsMenuTitleFontSp: Float = 28f,
    val settingsMenuBodyFontSp: Float = 22f,
    val settingsMenuTitle: Color = Color(0xFFE8EEF7),
    val settingsMenuBody: Color = Color(0xFFB4C0CF),
    val settingsMenuTitleSelected: Color = Color(0xFFF2F7FF),
    val settingsMenuBodySelected: Color = Color(0xFFE3EDF9),
    val settingsMenuBorder: Color = Color(0x553D4B60),
    // navBarTheme
    val navBarHeightDp: Float = 74f,
    val navBarSpacingDp: Float = 14f,
    val navBarIconSizeDp: Float = 52f,
    val dockIconTint: Color = Color(0xFFE6E6E6),
    // pageIndicatorTheme
    val pageIndicatorInactiveDp: Float = 12f,
    val pageIndicatorActiveDp: Float = 22f,
    val pageIndicatorSpacingDp: Float = 28f,
    val pageIndicatorFontSp: Float = 14f,
    val pageIndicatorColour: Color = Color(0xFFE6E6E6),
    /** Matches Flutter `PageIndicatorTheme.indicatorShape`: `squircle` uses ~10dp corners; `circle` uses [CircleShape]. */
    val pageIndicatorShapeSquircle: Boolean = false,
    // app grid (subset)
    val appGridIconSizeDp: Float = 52f,
    val appGridColumnSpacingDp: Float = 9f,
    val appGridRowSpacingDp: Float = 9f,
    /** Flutter `appGridTheme.appGridEdgeHoverZoneWidth` (px in logical coords). */
    val appGridEdgeHoverZoneWidthDp: Float = 70f,
    /** Flutter stores duration in microseconds in JSON; we keep ms here. */
    val appGridEdgeHoverDurationMs: Long = 2500L,
    val appCardFontSp: Float = 15f,
    val appCardTextColour: Color = Color(0xFFE6E6E6),
    val appCardTextOutlineColour: Color = Color.Black,
    val appCardCornerRadiusDp: Float = 7f,
    // selectorTheme (grid focus / edit selection)
    val selectorBackgroundColour: Color = Color(0xBE42A7CF),
    val selectorBorderColour: Color = Color(0x9470B9D6),
    val selectorBorderRadiusDp: Float = 5f,
    // dock badges
    val badgeBackground: Color = Color(0xFFD32F2F),
    val badgeText: Color = Color.White,
    val badgeFontSp: Float = 11f,
) {
    fun navBarHeight(): Dp = navBarHeightDp.dp
    fun navBarSpacing(): Dp = navBarSpacingDp.dp
    fun navIconSize(): Dp = navBarIconSizeDp.dp

    companion object {
        fun fromJson(json: String): LauncherThemePalette {
            val root = runCatching { JSONObject(json) }.getOrNull() ?: JSONObject()
            val legacy = root
            val nested = root.optJSONObject("launcherTheme")
                ?: root.optJSONObject("theme")
                ?: JSONObject()

            fun obj(vararg names: String): JSONObject {
                var o: JSONObject = root
                for (n in names) {
                    o = o.optJSONObject(n) ?: JSONObject()
                }
                return o
            }

            val settingsTheme = obj("settingsTheme")
            val navBar = obj("navBarTheme")
            val pageInd = obj("pageIndicatorTheme")
            val appGrid = obj("appGridTheme")
            val selector = appGrid.optJSONObject("selectorTheme")
                ?: obj("selectorTheme")

            fun c(o: JSONObject, key: String, d: Color): Color =
                parseColor(o.optString(key, ""), d)

            fun cFlat(key: String, d: Color): Color =
                parseColor(legacy.optString(key, nested.optString(key, "")), d)

            fun f(o: JSONObject, key: String, def: Float): Float {
                if (!o.has(key)) return def
                return o.optDouble(key, def.toDouble()).toFloat()
            }

            return LauncherThemePalette(
                longPressActionDurationMs = run {
                    val raw = legacy.optLong("longPressActionDuration", nested.optLong("longPressActionDuration", -1L))
                    when {
                        raw < 0L -> 800L
                        raw > 100_000L -> raw / 1000 // Flutter JSON: microseconds → ms
                        else -> raw // already ms
                    }
                }.coerceIn(50L, 5000L),
                settingsBg = cFlat("settingsBg", c(settingsTheme, "backgroundColour", Color(0xFF0E131B))),
                settingsSelected = cFlat("settingsSelected", Color(0x2A66D6FF)),
                dockSelected = cFlat("dockSelected", Color(0x664FC3F7)),
                appCardTop = cFlat("appCardTop", Color(0x332A3648)),
                appCardBottom = cFlat("appCardBottom", Color(0x22131A24)),
                settingsMenuTitleFontSp = f(settingsTheme, "menuItemTitleFontSize", 28f),
                settingsMenuBodyFontSp = f(settingsTheme, "menuItemBodyFontSize", 22f),
                settingsMenuTitle = c(settingsTheme, "menuItemTitleTextColour", Color(0xFFE8EEF7)),
                settingsMenuBody = c(settingsTheme, "menuItemBodyTextColour", Color(0xFFB4C0CF)),
                settingsMenuTitleSelected = c(settingsTheme, "menuItemTitleSelectedTextColour", Color(0xFFF2F7FF)),
                settingsMenuBodySelected = c(settingsTheme, "menuItemBodySelectedTextColour", Color(0xFFE3EDF9)),
                settingsMenuBorder = c(settingsTheme, "menuItemBorderColour", Color(0x553D4B60)),
                navBarHeightDp = f(navBar, "navBarHeight", 74f),
                navBarSpacingDp = f(navBar, "navBarSpacing", 14f),
                navBarIconSizeDp = f(navBar, "navBarIconSize", 43f),
                dockIconTint = c(navBar, "iconColour", Color(0xFFE6E6E6)),
                pageIndicatorInactiveDp = f(pageInd, "pageIndicatorInactiveSize", 12f),
                pageIndicatorActiveDp = f(pageInd, "pageIndicatorActiveSize", 22f),
                pageIndicatorSpacingDp = f(pageInd, "pageIndicatorSpacing", 28f),
                pageIndicatorFontSp = f(pageInd, "pageIndicatorFontSize", 14f),
                pageIndicatorColour = c(pageInd, "pageIndicatorColour", Color(0xFFE6E6E6)),
                pageIndicatorShapeSquircle = pageInd.optString("indicatorShape", "circle")
                    .equals("squircle", ignoreCase = true),
                appGridIconSizeDp = f(appGrid, "iconSize", 52f),
                appGridColumnSpacingDp = f(appGrid, "columnSpacing", 9f),
                appGridRowSpacingDp = f(appGrid, "rowSpacing", 9f),
                appCardFontSp = f(appGrid, "appCardFontSize", 15f),
                appCardTextColour = c(appGrid, "appCardTextColour", Color(0xFFE6E6E6)),
                appCardTextOutlineColour = c(appGrid, "appCardTextOutlineColour", Color.Black),
                appCardCornerRadiusDp = f(appGrid, "cornerRadius", 7f).let { if (it <= 0f) 7f else it },
                appGridEdgeHoverZoneWidthDp = f(appGrid, "appGridEdgeHoverZoneWidth", 70f),
                appGridEdgeHoverDurationMs = run {
                    if (!appGrid.has("appGridEdgeHoverDuration")) return@run 2500L
                    val raw = appGrid.optLong("appGridEdgeHoverDuration", 2_500_000L)
                    when {
                        raw > 500_000L -> raw / 1000 // Flutter JSON: microseconds → ms
                        else -> raw.coerceAtLeast(50L)
                    }
                }.coerceIn(50L, 60_000L),
                selectorBackgroundColour = c(selector, "selectorBackgroundColour", Color(0xBE42A7CF)),
                selectorBorderColour = c(selector, "selectorBorderColour", Color(0x9470B9D6)),
                selectorBorderRadiusDp = f(selector, "selectorBorderRadius", 5f),
                badgeBackground = cFlat("badgeBackground", Color(0xFFD32F2F)),
                badgeText = cFlat("badgeText", Color.White),
                badgeFontSp = legacy.optDouble("badgeFontSp", 11.0).toFloat(),
            )
        }

        fun defaultPalette(): LauncherThemePalette = fromJson("{}")

        fun toExportJson(p: LauncherThemePalette): String {
            val root = JSONObject()
            root.put("longPressActionDuration", p.longPressActionDurationMs * 1000)
            root.put("settingsBg", colorToHex8(p.settingsBg))
            root.put("settingsSelected", colorToHex8(p.settingsSelected))
            root.put("dockSelected", colorToHex8(p.dockSelected))
            root.put("appCardTop", colorToHex8(p.appCardTop))
            root.put("appCardBottom", colorToHex8(p.appCardBottom))
            root.put("badgeBackground", colorToHex8(p.badgeBackground))
            root.put("badgeText", colorToHex8(p.badgeText))
            root.put("badgeFontSp", p.badgeFontSp.toDouble())

            val settingsTheme = JSONObject()
            settingsTheme.put("menuItemTitleFontSize", p.settingsMenuTitleFontSp.toDouble())
            settingsTheme.put("menuItemBodyFontSize", p.settingsMenuBodyFontSp.toDouble())
            settingsTheme.put("menuItemTitleTextColour", colorToHex8(p.settingsMenuTitle))
            settingsTheme.put("menuItemBodyTextColour", colorToHex8(p.settingsMenuBody))
            settingsTheme.put("menuItemTitleSelectedTextColour", colorToHex8(p.settingsMenuTitleSelected))
            settingsTheme.put("menuItemBodySelectedTextColour", colorToHex8(p.settingsMenuBodySelected))
            settingsTheme.put("menuItemBorderColour", colorToHex8(p.settingsMenuBorder))
            settingsTheme.put("backgroundColour", colorToHex8(p.settingsBg))
            root.put("settingsTheme", settingsTheme)

            val navBar = JSONObject()
            navBar.put("navBarHeight", p.navBarHeightDp.toDouble())
            navBar.put("navBarSpacing", p.navBarSpacingDp.toDouble())
            navBar.put("navBarIconSize", p.navBarIconSizeDp.toDouble())
            navBar.put("iconColour", colorToHex8(p.dockIconTint))
            root.put("navBarTheme", navBar)

            val pageInd = JSONObject()
            pageInd.put("pageIndicatorInactiveSize", p.pageIndicatorInactiveDp.toDouble())
            pageInd.put("pageIndicatorActiveSize", p.pageIndicatorActiveDp.toDouble())
            pageInd.put("pageIndicatorSpacing", p.pageIndicatorSpacingDp.toDouble())
            pageInd.put("pageIndicatorFontSize", p.pageIndicatorFontSp.toDouble())
            pageInd.put("pageIndicatorColour", colorToHex8(p.pageIndicatorColour))
            pageInd.put("indicatorShape", if (p.pageIndicatorShapeSquircle) "squircle" else "circle")
            root.put("pageIndicatorTheme", pageInd)

            val selector = JSONObject()
            selector.put("selectorBackgroundColour", colorToHex8(p.selectorBackgroundColour))
            selector.put("selectorBorderColour", colorToHex8(p.selectorBorderColour))
            selector.put("selectorBorderRadius", p.selectorBorderRadiusDp.toDouble())

            val appGrid = JSONObject()
            appGrid.put("iconSize", p.appGridIconSizeDp.toDouble())
            appGrid.put("columnSpacing", p.appGridColumnSpacingDp.toDouble())
            appGrid.put("rowSpacing", p.appGridRowSpacingDp.toDouble())
            appGrid.put("appCardFontSize", p.appCardFontSp.toDouble())
            appGrid.put("appCardTextColour", colorToHex8(p.appCardTextColour))
            appGrid.put("appCardTextOutlineColour", colorToHex8(p.appCardTextOutlineColour))
            appGrid.put("cornerRadius", p.appCardCornerRadiusDp.toDouble())
            appGrid.put("appGridEdgeHoverZoneWidth", p.appGridEdgeHoverZoneWidthDp.toDouble())
            appGrid.put("appGridEdgeHoverDuration", p.appGridEdgeHoverDurationMs * 1000)
            appGrid.put("selectorTheme", selector)
            root.put("appGridTheme", appGrid)

            root.put("selectorTheme", selector)
            return root.toString(2)
        }
    }
}

private fun parseColor(hex: String, fallback: Color): Color {
    val clean = hex.removePrefix("#").trim()
    if (clean.isEmpty()) return fallback
    return runCatching {
        val value = clean.toLong(16)
        when (clean.length) {
            8 -> Color(value)
            6 -> Color(0xFF000000 or value)
            else -> fallback
        }
    }.getOrDefault(fallback)
}

private fun colorToHex8(c: Color): String {
    val a = (c.alpha * 255f + 0.5f).toInt().coerceIn(0, 255)
    val r = (c.red * 255f + 0.5f).toInt().coerceIn(0, 255)
    val g = (c.green * 255f + 0.5f).toInt().coerceIn(0, 255)
    val b = (c.blue * 255f + 0.5f).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}
