package com.zeno.classiclauncher.nlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized adaptive layout engine for Zeno Classic Launcher.
 *
 * All screens must read sizing decisions from here rather than scattering
 * LocalConfiguration calls or hardcoding dp values.  Nothing in this file
 * is device-specific (no "if Zinwa", no "if Pixel").  Every decision is
 * driven purely by available screen space.
 *
 * ## Device classes (based on available screen width, matching M3/WC guidance)
 *
 *   Compact  — width <  600 dp  (phones, BB-style keyboard phones like Zinwa Q25)
 *   Medium   — width  600–839 dp  (large phones, small tablets, foldables half-open)
 *   Expanded — width ≥ 840 dp  (tablets, foldables fully open, desktop windows)
 *
 * ## Usage
 *
 *   val layout = rememberAdaptiveLayout()
 *   Icon(modifier = Modifier.size(layout.dockIconSize))
 *   LazyVerticalGrid(columns = GridCells.Fixed(layout.drawerCols))
 */
@Stable
data class AdaptiveLayout(
    // ── Device class ─────────────────────────────────────────────────────────
    val widthClass: WidthClass,
    val screenWidthDp: Int,
    val screenHeightDp: Int,

    // ── Drawer grid ───────────────────────────────────────────────────────────
    /** Maximum safe grid column count for the current screen width. */
    val maxDrawerCols: Int,
    /** Maximum safe grid row count for the current usable height. */
    val maxDrawerRows: Int,

    // ── Home widget grid ──────────────────────────────────────────────────────
    val homeGridCols: Int,
    val homeGridRows: Int,

    // ── Home strip ────────────────────────────────────────────────────────────
    /** Maximum strip slot count that fits without per-slot overflow. Always ≤ 5 (STRIP_TOTAL_SLOTS). */
    val maxStripCols: Int,

    // ── Folder overlay ────────────────────────────────────────────────────────
    /** Maximum folder overlay grid height as screen fraction. */
    val folderOverlayMaxHeightDp: Dp,

    // ── Search bar ────────────────────────────────────────────────────────────
    /** Minimum search bar height — can grow taller at large font scales. */
    val searchBarMinHeightDp: Dp,

    // ── Heights / fractions used in sheet content ─────────────────────────────
    /** Maximum height of a scrollable list inside a bottom sheet (e.g., language picker, pin-to-home). */
    val sheetListMaxHeightDp: Dp,

    /** Maximum height of the home-search overlay results list. */
    val homeSearchResultsMaxHeightDp: Dp,

    /** Maximum height of settings menu body. */
    val settingsMenuMaxHeightDp: Dp,
) {
    enum class WidthClass { Compact, Medium, Expanded }

    companion object {
        /**
         * Minimum cell width per column in the drawer grid (icon + horizontal padding).
         * Phones: icon=52dp + 14dp pad = 66dp minimum.
         */
        private const val DRAWER_MIN_CELL_WIDTH_DP = 66

        /**
         * Minimum cell height per row in the drawer grid.
         * 3dp top + 52dp icon + 14dp pad + label ≈ 94dp at default settings.
         */
        private const val DRAWER_MIN_CELL_HEIGHT_DP = 94

        /** Outer horizontal padding applied to the drawer column. */
        private const val DRAWER_OUTER_PAD_H_DP = 12

        /** Column spacing between drawer grid cells. */
        private const val DRAWER_COL_SPACING_DP = 9

        /** Row spacing between drawer grid rows. */
        private const val DRAWER_ROW_SPACING_DP = 5

        /** Approximate fixed overhead above the drawer grid (status bar ~24dp + search bar ~45dp + sort row ~22dp + top padding ~11dp + small safety). */
        private const val DRAWER_OVERHEAD_DP = 110

        /** Approximate dock height (default theme value). */
        private const val DOCK_HEIGHT_DP = 74

        fun from(screenWidthDp: Int, screenHeightDp: Int): AdaptiveLayout {
            val widthClass = when {
                screenWidthDp >= 840 -> WidthClass.Expanded
                screenWidthDp >= 600 -> WidthClass.Medium
                else -> WidthClass.Compact
            }

            // ── Drawer columns ────────────────────────────────────────────────
            // Available width for grid cells after outer padding on both sides
            val availableWidthDp = screenWidthDp - DRAWER_OUTER_PAD_H_DP * 2
            // Max cols = how many min-width cells fit with their spacing
            val maxCols = ((availableWidthDp + DRAWER_COL_SPACING_DP) /
                    (DRAWER_MIN_CELL_WIDTH_DP + DRAWER_COL_SPACING_DP)).coerceIn(3, 8)

            // ── Drawer rows ───────────────────────────────────────────────────
            // Usable height for the grid pager (full screen minus fixed overhead and dock)
            val gridAvailableHeightDp = screenHeightDp - DRAWER_OVERHEAD_DP - DOCK_HEIGHT_DP
            val maxRows = ((gridAvailableHeightDp + DRAWER_ROW_SPACING_DP) /
                    (DRAWER_MIN_CELL_HEIGHT_DP + DRAWER_ROW_SPACING_DP)).coerceIn(1, 6)

            // ── Home grid ─────────────────────────────────────────────────────
            val homeGridCols = when (widthClass) {
                WidthClass.Expanded -> 6
                WidthClass.Medium -> 5
                WidthClass.Compact -> 4
            }
            val homeGridRows = when (widthClass) {
                WidthClass.Expanded -> 5
                WidthClass.Medium -> 5
                WidthClass.Compact -> 4
            }

            // ── Home strip ────────────────────────────────────────────────────
            // Strip is always 5 slots max (STRIP_TOTAL_SLOTS), but on very narrow
            // screens (< 320dp) 5 slots could overflow. Guard for future hardware.
            val stripMinCellWidth = 56 // icon(46dp) + 10dp padding
            val stripColSpacing = 4
            val stripOuterPad = 44 // 22dp each side
            val maxStripCols = ((screenWidthDp - stripOuterPad + stripColSpacing) /
                    (stripMinCellWidth + stripColSpacing)).coerceIn(3, 5)

            // ── Sheet / overlay heights ───────────────────────────────────────
            val folderOverlayMaxHeightDp = (screenHeightDp * 0.55f).toInt().dp
            val sheetListMaxHeightDp = (screenHeightDp * 0.50f).toInt().dp
            val homeSearchResultsMaxHeightDp = (screenHeightDp * 0.52f).toInt().dp
            val settingsMenuMaxHeightDp = (screenHeightDp * 0.58f).toInt().dp

            return AdaptiveLayout(
                widthClass = widthClass,
                screenWidthDp = screenWidthDp,
                screenHeightDp = screenHeightDp,
                maxDrawerCols = maxCols,
                maxDrawerRows = maxRows,
                homeGridCols = homeGridCols,
                homeGridRows = homeGridRows,
                maxStripCols = maxStripCols,
                folderOverlayMaxHeightDp = folderOverlayMaxHeightDp,
                searchBarMinHeightDp = 45.dp,
                sheetListMaxHeightDp = sheetListMaxHeightDp,
                homeSearchResultsMaxHeightDp = homeSearchResultsMaxHeightDp,
                settingsMenuMaxHeightDp = settingsMenuMaxHeightDp,
            )
        }
    }
}

/**
 * Remember an [AdaptiveLayout] that recomputes when screen configuration changes
 * (rotation, fold/unfold, display-size change, DPI change).
 */
@Composable
fun rememberAdaptiveLayout(): AdaptiveLayout {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp
    val heightDp = config.screenHeightDp
    return remember(widthDp, heightDp) {
        AdaptiveLayout.from(widthDp, heightDp)
    }
}

/**
 * Validate whether a user-selected drawer preset is safe for the current device.
 *
 * Returns true if the preset's row and column counts fit within the available
 * screen space without requiring cells to exceed their minimum dimensions.
 *
 * Call this on prefs load and in settings UI to warn the user before they
 * save a preset that would clip content on their device.
 */
fun AdaptiveLayout.isDrawerPresetSafe(cols: Int, rows: Int): Boolean =
    cols <= maxDrawerCols && rows <= maxDrawerRows

/**
 * Clamp a user-selected column count to the safe maximum for this device.
 * Preserves intent (keeps as close to requested as possible).
 */
fun AdaptiveLayout.clampDrawerCols(requested: Int): Int = requested.coerceAtMost(maxDrawerCols)

/**
 * Clamp a user-selected row count to the safe maximum for this device.
 */
fun AdaptiveLayout.clampDrawerRows(requested: Int): Int = requested.coerceAtMost(maxDrawerRows)
