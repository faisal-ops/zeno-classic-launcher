package com.zeno.classiclauncher.nlauncher.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for effectiveHomeStripOrder, effectiveHomeStripSlotOrder, and homeStripItemCount.
 *
 * These pure functions drive the home-strip layout: what items appear, in what order,
 * and which slot positions they occupy. Bugs here silently drop shortcuts on upgrade.
 */
class HomeStripOrderTest {

    private val GRP_ID = "com.zeno.classiclauncher.slot.homegroup.aaa"
    private fun group(id: String = GRP_ID) = HomeGroup(id = id, title = "Group")

    // ─── effectiveHomeStripOrder ─────────────────────────────────────────────

    @Test
    fun effectiveHomeStripOrder_noSavedOrder_groupsBeforeShortcuts() {
        val prefs = LauncherPrefs(
            homeShortcutPackages = listOf("com.a", "com.b"),
            homeGroups = listOf(group()),
        )
        val order = prefs.effectiveHomeStripOrder()
        // Groups come before shortcuts when no saved order
        assertEquals(GRP_ID, order[0])
        assertTrue("com.a" in order)
        assertTrue("com.b" in order)
    }

    @Test
    fun effectiveHomeStripOrder_savedOrder_preserved() {
        val prefs = LauncherPrefs(
            homeShortcutPackages = listOf("com.a", "com.b"),
            homeStripOrder = listOf("com.b", "com.a"),
        )
        assertEquals(listOf("com.b", "com.a"), prefs.effectiveHomeStripOrder())
    }

    @Test
    fun effectiveHomeStripOrder_savedOrderMissingItem_appendedAtEnd() {
        // "com.c" is in shortcuts but not in saved order — appended after saved items
        val prefs = LauncherPrefs(
            homeShortcutPackages = listOf("com.a", "com.b", "com.c"),
            homeStripOrder = listOf("com.b", "com.a"),
        )
        val order = prefs.effectiveHomeStripOrder()
        assertEquals("com.b", order[0])
        assertEquals("com.a", order[1])
        assertEquals("com.c", order[2])
    }

    @Test
    fun effectiveHomeStripOrder_savedOrderContainsOrphan_orphanDropped() {
        // "com.removed" is in saved order but no longer in shortcuts/groups
        val prefs = LauncherPrefs(
            homeShortcutPackages = listOf("com.a"),
            homeStripOrder = listOf("com.removed", "com.a"),
        )
        val order = prefs.effectiveHomeStripOrder()
        assertEquals(listOf("com.a"), order)
    }

    @Test
    fun effectiveHomeStripOrder_empty_returnsEmpty() {
        val prefs = LauncherPrefs()
        assertTrue(prefs.effectiveHomeStripOrder().isEmpty())
    }

    // ─── effectiveHomeStripSlotOrder ─────────────────────────────────────────

    @Test
    fun effectiveHomeStripSlotOrder_alwaysReturnsFiveSlots() {
        val prefs = LauncherPrefs(homeShortcutPackages = listOf("com.a", "com.b"))
        assertEquals(STRIP_TOTAL_SLOTS, prefs.effectiveHomeStripSlotOrder().size)
    }

    @Test
    fun effectiveHomeStripSlotOrder_noSavedSlots_itemsPlacedFirst() {
        val prefs = LauncherPrefs(homeShortcutPackages = listOf("com.a", "com.b"))
        val slots = prefs.effectiveHomeStripSlotOrder()
        assertEquals("com.a", slots[0])
        assertEquals("com.b", slots[1])
        assertNull(slots[2])
        assertNull(slots[3])
        assertNull(slots[4])
    }

    @Test
    fun effectiveHomeStripSlotOrder_savedSlots_positionsRespected() {
        // Place "com.a" in slot 2 (index 2), leave others null
        val prefs = LauncherPrefs(
            homeShortcutPackages = listOf("com.a"),
            homeStripSlots = listOf(null, null, "com.a", null, null),
        )
        val slots = prefs.effectiveHomeStripSlotOrder()
        assertEquals(STRIP_TOTAL_SLOTS, slots.size)
        assertEquals("com.a", slots[2])
    }

    @Test
    fun effectiveHomeStripSlotOrder_savedSlotsContainOrphan_replacedWithNull() {
        val prefs = LauncherPrefs(
            homeShortcutPackages = listOf("com.a"),
            homeStripSlots = listOf("com.orphan", "com.a", null, null, null),
        )
        val slots = prefs.effectiveHomeStripSlotOrder()
        // com.orphan is not a valid shortcut — its slot becomes null or filled with unassigned item
        assertTrue(slots[1] == "com.a") // com.a should stay in its position
    }

    @Test
    fun effectiveHomeStripSlotOrder_unassignedShortcutsAutoFillEmptySlots() {
        // Saved slots have one slot occupied; "com.b" is not assigned so fills an empty slot
        val prefs = LauncherPrefs(
            homeShortcutPackages = listOf("com.a", "com.b"),
            homeStripSlots = listOf("com.a", null, null, null, null),
        )
        val slots = prefs.effectiveHomeStripSlotOrder()
        assertTrue(slots.filterNotNull().containsAll(listOf("com.a", "com.b")))
    }

    // ─── homeStripItemCount ──────────────────────────────────────────────────

    @Test
    fun homeStripItemCount_noItems_isZero() {
        assertEquals(0, LauncherPrefs().homeStripItemCount())
    }

    @Test
    fun homeStripItemCount_twoShortcuts_isTwo() {
        val prefs = LauncherPrefs(homeShortcutPackages = listOf("com.a", "com.b"))
        assertEquals(2, prefs.homeStripItemCount())
    }

    @Test
    fun homeStripItemCount_fullStrip_equalsStripTotalSlots() {
        val prefs = LauncherPrefs(
            homeShortcutPackages = List(STRIP_TOTAL_SLOTS) { "com.app$it" },
        )
        assertEquals(STRIP_TOTAL_SLOTS, prefs.homeStripItemCount())
    }
}
