package com.zeno.classiclauncher.nlauncher.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGroupTest {

    private fun group(id: String, side: HomeGroupSide = HomeGroupSide.LEFT) =
        HomeGroup(id = id, title = "Group $id", side = side)

    // ─── normalizedAtMostTwo ─────────────────────────────────────────────────

    @Test
    fun normalizedAtMostTwo_emptyList_returnsEmpty() {
        assertTrue(emptyList<HomeGroup>().normalizedAtMostTwo().isEmpty())
    }

    @Test
    fun normalizedAtMostTwo_singleGroup_returnedUnchanged() {
        val g = group("g1", HomeGroupSide.RIGHT)
        val result = listOf(g).normalizedAtMostTwo()
        assertEquals(1, result.size)
        assertEquals(HomeGroupSide.RIGHT, result[0].side)
    }

    @Test
    fun normalizedAtMostTwo_twoGroupsDifferentSides_returnedUnchanged() {
        val a = group("a", HomeGroupSide.LEFT)
        val b = group("b", HomeGroupSide.RIGHT)
        val result = listOf(a, b).normalizedAtMostTwo()
        assertEquals(HomeGroupSide.LEFT, result[0].side)
        assertEquals(HomeGroupSide.RIGHT, result[1].side)
    }

    @Test
    fun normalizedAtMostTwo_twoGroupsBothLeft_assignsLeftRight() {
        val a = group("a", HomeGroupSide.LEFT)
        val b = group("b", HomeGroupSide.LEFT)
        val result = listOf(a, b).normalizedAtMostTwo()
        assertEquals(HomeGroupSide.LEFT, result[0].side)
        assertEquals(HomeGroupSide.RIGHT, result[1].side)
    }

    @Test
    fun normalizedAtMostTwo_twoGroupsBothRight_assignsLeftRight() {
        val a = group("a", HomeGroupSide.RIGHT)
        val b = group("b", HomeGroupSide.RIGHT)
        val result = listOf(a, b).normalizedAtMostTwo()
        assertEquals(HomeGroupSide.LEFT, result[0].side)
        assertEquals(HomeGroupSide.RIGHT, result[1].side)
    }

    @Test
    fun normalizedAtMostTwo_moreThanTwo_onlyFirstTwoKept() {
        val groups = listOf(group("a"), group("b"), group("c"), group("d"))
        val result = groups.normalizedAtMostTwo()
        assertEquals(2, result.size)
        assertEquals("a", result[0].id)
        assertEquals("b", result[1].id)
    }

    // ─── HomeGroupSide.fromStored ────────────────────────────────────────────

    @Test
    fun fromStored_right_returnsRight() {
        assertEquals(HomeGroupSide.RIGHT, HomeGroupSide.fromStored("RIGHT"))
    }

    @Test
    fun fromStored_rightLowercase_returnsRight() {
        assertEquals(HomeGroupSide.RIGHT, HomeGroupSide.fromStored("right"))
    }

    @Test
    fun fromStored_left_returnsLeft() {
        assertEquals(HomeGroupSide.LEFT, HomeGroupSide.fromStored("LEFT"))
    }

    @Test
    fun fromStored_null_defaultsToLeft() {
        assertEquals(HomeGroupSide.LEFT, HomeGroupSide.fromStored(null))
    }

    @Test
    fun fromStored_unknownValue_defaultsToLeft() {
        assertEquals(HomeGroupSide.LEFT, HomeGroupSide.fromStored("CENTRE"))
    }

    // ─── HomeGroupIds ────────────────────────────────────────────────────────

    @Test
    fun isHomeGroupId_validPrefix_returnsTrue() {
        assertTrue(HomeGroupIds.isHomeGroupId(HomeGroupIds.PREFIX + "abc123"))
    }

    @Test
    fun isHomeGroupId_plainPackageName_returnsFalse() {
        assertFalse(HomeGroupIds.isHomeGroupId("com.android.dialer"))
    }

    @Test
    fun isHomeGroupId_newId_returnsTrue() {
        assertTrue(HomeGroupIds.isHomeGroupId(HomeGroupIds.newId()))
    }

    @Test
    fun newId_twoCallsProduceDifferentIds() {
        assertFalse(HomeGroupIds.newId() == HomeGroupIds.newId())
    }
}
