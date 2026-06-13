package com.zeno.classiclauncher.nlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {

    // ─── Width class bucketing ───────────────────────────────────────────────

    @Test
    fun from_phonePortrait_isCompact() {
        val layout = AdaptiveLayout.from(360, 800)
        assertEquals(AdaptiveLayout.WidthClass.Compact, layout.widthClass)
    }

    @Test
    fun from_largeFoldable_isMedium() {
        val layout = AdaptiveLayout.from(700, 900)
        assertEquals(AdaptiveLayout.WidthClass.Medium, layout.widthClass)
    }

    @Test
    fun from_tablet_isExpanded() {
        val layout = AdaptiveLayout.from(1024, 768)
        assertEquals(AdaptiveLayout.WidthClass.Expanded, layout.widthClass)
    }

    @Test
    fun from_exactBoundary600_isMedium() {
        val layout = AdaptiveLayout.from(600, 800)
        assertEquals(AdaptiveLayout.WidthClass.Medium, layout.widthClass)
    }

    @Test
    fun from_exactBoundary840_isExpanded() {
        val layout = AdaptiveLayout.from(840, 800)
        assertEquals(AdaptiveLayout.WidthClass.Expanded, layout.widthClass)
    }

    // ─── Drawer column calculation ───────────────────────────────────────────

    @Test
    fun from_phone360dp_drawerColsAtLeast3() {
        val layout = AdaptiveLayout.from(360, 800)
        assertTrue("Compact phone should have ≥3 cols", layout.maxDrawerCols >= 3)
    }

    @Test
    fun from_tablet1024dp_drawerColsMoreThanPhone() {
        val phone = AdaptiveLayout.from(360, 800)
        val tablet = AdaptiveLayout.from(1024, 768)
        assertTrue(tablet.maxDrawerCols > phone.maxDrawerCols)
    }

    @Test
    fun from_drawerColsNeverExceeds8() {
        // Very wide screen should still cap at 8
        val layout = AdaptiveLayout.from(2560, 1600)
        assertTrue(layout.maxDrawerCols <= 8)
    }

    // ─── Home grid ───────────────────────────────────────────────────────────

    @Test
    fun from_compactPhone_homeGrid4x4() {
        val layout = AdaptiveLayout.from(360, 800)
        assertEquals(4, layout.homeGridCols)
        assertEquals(4, layout.homeGridRows)
    }

    @Test
    fun from_expandedTablet_homeGrid6x5() {
        val layout = AdaptiveLayout.from(1024, 768)
        assertEquals(6, layout.homeGridCols)
        assertEquals(5, layout.homeGridRows)
    }

    // ─── Strip column cap ────────────────────────────────────────────────────

    @Test
    fun from_normalPhone_stripColsMax5() {
        val layout = AdaptiveLayout.from(360, 800)
        assertTrue(layout.maxStripCols <= 5)
        assertTrue(layout.maxStripCols >= 3)
    }

    // ─── Extension functions ─────────────────────────────────────────────────

    @Test
    fun isDrawerPresetSafe_withinBounds_returnsTrue() {
        val layout = AdaptiveLayout.from(360, 800)
        assertTrue(layout.isDrawerPresetSafe(layout.maxDrawerCols, layout.maxDrawerRows))
    }

    @Test
    fun isDrawerPresetSafe_exceedsCols_returnsFalse() {
        val layout = AdaptiveLayout.from(360, 800)
        assertFalse(layout.isDrawerPresetSafe(layout.maxDrawerCols + 1, layout.maxDrawerRows))
    }

    @Test
    fun isDrawerPresetSafe_exceedsRows_returnsFalse() {
        val layout = AdaptiveLayout.from(360, 800)
        assertFalse(layout.isDrawerPresetSafe(layout.maxDrawerCols, layout.maxDrawerRows + 1))
    }

    @Test
    fun clampDrawerCols_overRequest_clampsToMax() {
        val layout = AdaptiveLayout.from(360, 800)
        assertEquals(layout.maxDrawerCols, layout.clampDrawerCols(layout.maxDrawerCols + 99))
    }

    @Test
    fun clampDrawerCols_underMax_preservesRequest() {
        val layout = AdaptiveLayout.from(360, 800)
        val safeRequest = layout.maxDrawerCols - 1
        if (safeRequest >= 1) assertEquals(safeRequest, layout.clampDrawerCols(safeRequest))
    }

    @Test
    fun clampDrawerRows_overRequest_clampsToMax() {
        val layout = AdaptiveLayout.from(360, 800)
        assertEquals(layout.maxDrawerRows, layout.clampDrawerRows(layout.maxDrawerRows + 99))
    }

    // ─── Sheet height proportions ────────────────────────────────────────────

    @Test
    fun from_sheetListMaxHeight_isHalfScreen() {
        val layout = AdaptiveLayout.from(360, 800)
        val expectedDp = (800 * 0.50f).toInt()
        assertEquals(expectedDp.toFloat(), layout.sheetListMaxHeightDp.value, 0.5f)
    }

    @Test
    fun from_folderOverlayMaxHeight_is55PercentScreen() {
        val layout = AdaptiveLayout.from(360, 800)
        val expectedDp = (800 * 0.55f).toInt()
        assertEquals(expectedDp.toFloat(), layout.folderOverlayMaxHeightDp.value, 0.5f)
    }
}
