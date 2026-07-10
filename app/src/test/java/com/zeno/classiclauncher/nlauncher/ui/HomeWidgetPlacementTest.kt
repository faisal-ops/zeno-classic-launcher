package com.zeno.classiclauncher.nlauncher.ui

import com.zeno.classiclauncher.nlauncher.prefs.HomeWidgetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeWidgetPlacementTest {

    private fun widget(col: Int, row: Int, cols: Int, rows: Int) =
        HomeWidgetConfig(appWidgetId = 1, col = col, row = row, cols = cols, rows = rows)

    // ─── widgetSlotsOverlap ───────────────────────────────────────────────

    @Test
    fun widgetSlotsOverlap_identicalRects_overlap() {
        assertTrue(widgetSlotsOverlap(0, 0, 2, 2, 0, 0, 2, 2))
    }

    @Test
    fun widgetSlotsOverlap_adjacentRects_doNotOverlap() {
        assertFalse(widgetSlotsOverlap(0, 0, 2, 2, 2, 0, 2, 2))
        assertFalse(widgetSlotsOverlap(0, 0, 2, 2, 0, 2, 2, 2))
    }

    @Test
    fun widgetSlotsOverlap_partialOverlap_detected() {
        assertTrue(widgetSlotsOverlap(0, 0, 2, 2, 1, 1, 2, 2))
    }

    @Test
    fun widgetSlotsOverlap_farApart_doNotOverlap() {
        assertFalse(widgetSlotsOverlap(0, 0, 1, 1, 3, 3, 1, 1))
    }

    // ─── overlapsAnyWidget ───────────────────────────────────────────────

    @Test
    fun overlapsAnyWidget_emptyList_neverOverlaps() {
        assertFalse(overlapsAnyWidget(emptyList(), 0, 0, 4, 4))
    }

    @Test
    fun overlapsAnyWidget_detectsOverlapWithAnyEntry() {
        val others = listOf(widget(col = 0, row = 0, cols = 1, rows = 1), widget(col = 3, row = 3, cols = 1, rows = 1))
        assertTrue(overlapsAnyWidget(others, 3, 3, 1, 1))
        assertFalse(overlapsAnyWidget(others, 1, 1, 1, 1))
    }

    // ─── findFreeWidgetSlot ───────────────────────────────────────────────

    @Test
    fun findFreeWidgetSlot_emptyGrid_usesPreferredSpot() {
        val (col, row) = findFreeWidgetSlot(
            placed = emptyList(), cols = 2, rows = 2, gridCols = 4, gridRows = 4, preferredCol = 1, preferredRow = 1,
        )
        assertEquals(1 to 1, col to row)
    }

    @Test
    fun findFreeWidgetSlot_preferredSpotTaken_scansForFreeSlot() {
        // A 2x2 widget occupying the top-left quadrant leaves the rest of a 4x4 grid free for
        // another 1x1 widget, so a non-overlapping slot must exist even though (0, 0) is taken.
        val placed = listOf(widget(col = 0, row = 0, cols = 2, rows = 2))
        val (col, row) = findFreeWidgetSlot(
            placed = placed, cols = 1, rows = 1, gridCols = 4, gridRows = 4, preferredCol = 0, preferredRow = 0,
        )
        assertFalse(overlapsAnyWidget(placed, col, row, 1, 1))
    }

    @Test
    fun findFreeWidgetSlot_fullyPacked_fallsBackToOrigin() {
        val placed = listOf(widget(col = 0, row = 0, cols = 4, rows = 4))
        val (col, row) = findFreeWidgetSlot(
            placed = placed, cols = 4, rows = 4, gridCols = 4, gridRows = 4, preferredCol = 0, preferredRow = 0,
        )
        assertEquals(0 to 0, col to row)
    }
}
