package com.zeno.classiclauncher.nlauncher.ui

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for NavState keyboard navigation (D-pad grid + dock).
 * NavState.onGridKey / onDockKey are pure functions that drive BB Classic trackpad navigation.
 */
class KeyNavTest {

    // ─── onGridKey — 4-column grid ──────────────────────────────────────────

    @Test
    fun gridKey_right_movesOneColumn() {
        val state = NavState(area = FocusArea.DrawerGrid, gridIndex = 0)
        val next = state.onGridKey(Key.DirectionRight, cols = 4, itemCount = 12)
        assertEquals(1, next.gridIndex)
    }

    @Test
    fun gridKey_right_atEndOfRow_staysInPlace() {
        val state = NavState(area = FocusArea.DrawerGrid, gridIndex = 3) // last col in 4-col grid
        val next = state.onGridKey(Key.DirectionRight, cols = 4, itemCount = 12)
        assertEquals(3, next.gridIndex)
    }

    @Test
    fun gridKey_left_movesOneColumn() {
        val state = NavState(area = FocusArea.DrawerGrid, gridIndex = 2)
        val next = state.onGridKey(Key.DirectionLeft, cols = 4, itemCount = 12)
        assertEquals(1, next.gridIndex)
    }

    @Test
    fun gridKey_left_atStartOfRow_staysInPlace() {
        val state = NavState(area = FocusArea.DrawerGrid, gridIndex = 0)
        val next = state.onGridKey(Key.DirectionLeft, cols = 4, itemCount = 12)
        assertEquals(0, next.gridIndex)
    }

    @Test
    fun gridKey_down_movesOneRow() {
        val state = NavState(area = FocusArea.DrawerGrid, gridIndex = 1)
        val next = state.onGridKey(Key.DirectionDown, cols = 4, itemCount = 12)
        assertEquals(5, next.gridIndex)
        assertEquals(FocusArea.DrawerGrid, next.area)
    }

    @Test
    fun gridKey_down_atLastRow_transitionsToDock() {
        val state = NavState(area = FocusArea.DrawerGrid, gridIndex = 8) // row 3 of 3 in 4-col/12-item grid
        val next = state.onGridKey(Key.DirectionDown, cols = 4, itemCount = 12)
        assertEquals(FocusArea.Dock, next.area)
        assertEquals(0, next.dockIndex)
    }

    @Test
    fun gridKey_up_movesOneRow() {
        val state = NavState(area = FocusArea.DrawerGrid, gridIndex = 5)
        val next = state.onGridKey(Key.DirectionUp, cols = 4, itemCount = 12)
        assertEquals(1, next.gridIndex)
    }

    @Test
    fun gridKey_up_atFirstRow_staysInPlace() {
        val state = NavState(area = FocusArea.DrawerGrid, gridIndex = 2)
        val next = state.onGridKey(Key.DirectionUp, cols = 4, itemCount = 12)
        assertEquals(2, next.gridIndex)
    }

    @Test
    fun gridKey_emptyGrid_staysInPlace() {
        val state = NavState(area = FocusArea.DrawerGrid, gridIndex = 0)
        val next = state.onGridKey(Key.DirectionDown, cols = 4, itemCount = 0)
        assertEquals(state, next)
    }

    // ─── onDockKey ───────────────────────────────────────────────────────────

    @Test
    fun dockKey_right_movesRight() {
        val state = NavState(area = FocusArea.Dock, dockIndex = 0)
        val next = state.onDockKey(Key.DirectionRight, dockSize = 4)
        assertEquals(1, next.dockIndex)
    }

    @Test
    fun dockKey_right_atLastItem_staysInPlace() {
        val state = NavState(area = FocusArea.Dock, dockIndex = 3)
        val next = state.onDockKey(Key.DirectionRight, dockSize = 4)
        assertEquals(3, next.dockIndex)
    }

    @Test
    fun dockKey_left_movesLeft() {
        val state = NavState(area = FocusArea.Dock, dockIndex = 2)
        val next = state.onDockKey(Key.DirectionLeft, dockSize = 4)
        assertEquals(1, next.dockIndex)
    }

    @Test
    fun dockKey_left_atFirstItem_staysInPlace() {
        val state = NavState(area = FocusArea.Dock, dockIndex = 0)
        val next = state.onDockKey(Key.DirectionLeft, dockSize = 4)
        assertEquals(0, next.dockIndex)
    }

    @Test
    fun dockKey_up_transitionsToGrid() {
        val state = NavState(area = FocusArea.Dock, dockIndex = 1)
        val next = state.onDockKey(Key.DirectionUp, dockSize = 4)
        assertEquals(FocusArea.DrawerGrid, next.area)
    }

    @Test
    fun dockKey_down_wrapsBackToGrid() {
        val state = NavState(area = FocusArea.Dock, dockIndex = 2)
        val next = state.onDockKey(Key.DirectionDown, dockSize = 4)
        assertEquals(FocusArea.DrawerGrid, next.area)
    }
}
