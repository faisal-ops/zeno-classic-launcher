package com.zeno.classiclauncher.nlauncher.ui

import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for drawer ordering algorithms: alphabetical sort, insert-index.
 * AppEntry.icon is null throughout — it is not used by any ordering logic.
 * The internal Settings entry is deliberately NOT special-cased anywhere here — it sorts and
 * inserts exactly like any other app, by its label.
 */
class DrawerOrderTest {

    private fun app(pkg: String, label: String = pkg) = AppEntry(
        packageName = pkg,
        label = label,
        icon = null,
    )

    private val SETTINGS = AppsRepository.INTERNAL_SETTINGS_PACKAGE

    // ─── strictAlphabeticalDrawerOrder ──────────────────────────────────────

    @Test
    fun alphabeticalOrder_threeApps_sortedByLabel() {
        val installed = listOf(app("com.z", "Zebra"), app("com.a", "Apple"), app("com.m", "Mango"))
        val result = strictAlphabeticalDrawerOrder(installed, emptyMap(), emptyMap())
        assertEquals(listOf("com.a", "com.m", "com.z"), result)
    }

    @Test
    fun alphabeticalOrder_internalSettings_sortsByLabelLikeAnyApp() {
        val installed = listOf(
            app("com.z", "Zebra"),
            app(SETTINGS, "Settings"),
            app("com.a", "Apple"),
        )
        val result = strictAlphabeticalDrawerOrder(installed, emptyMap(), emptyMap())
        // "Settings" sorts between Apple and Zebra — no special pinning.
        assertEquals(listOf("com.a", SETTINGS, "com.z"), result)
    }

    @Test
    fun alphabeticalOrder_folderInList_sortedByFolderTitle() {
        val installed = listOf(app("com.z", "Zebra"), app("com.a", "Apple"))
        val folderContents = mapOf("folder1" to listOf("com.a"))
        val folderNames = mapOf("folder1" to "Banana")
        val result = strictAlphabeticalDrawerOrder(installed, folderContents, folderNames)
        // "Banana" sorts between Apple and Zebra; com.a is a folder member so excluded from top-level
        assertEquals("folder1", result[0]) // Banana folder
        assertEquals("com.z", result[1])   // Zebra
    }

    @Test
    fun alphabeticalOrder_folderMember_excludedFromTopLevel() {
        val installed = listOf(app("com.a", "Apple"), app("com.b", "Banana"))
        val folderContents = mapOf("folder1" to listOf("com.b"))
        val result = strictAlphabeticalDrawerOrder(installed, folderContents, emptyMap())
        // com.b is a folder member, should not appear as top-level item
        assertTrue("com.b" !in result || result.indexOf("com.b") < 0)
        assertTrue("folder1" in result)
    }

    @Test
    fun alphabeticalOrder_caseInsensitive_mixedCase() {
        val installed = listOf(app("com.a", "apple"), app("com.b", "Banana"), app("com.c", "CHERRY"))
        val result = strictAlphabeticalDrawerOrder(installed, emptyMap(), emptyMap())
        assertEquals(listOf("com.a", "com.b", "com.c"), result)
    }

    @Test
    fun alphabeticalOrder_emptyInstalled_returnsEmpty() {
        val result = strictAlphabeticalDrawerOrder(emptyList(), emptyMap(), emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun alphabeticalOrder_sameLabel_tieBreaksByPackageName() {
        val installed = listOf(app("com.z.app", "Same"), app("com.a.app", "Same"))
        val result = strictAlphabeticalDrawerOrder(installed, emptyMap(), emptyMap())
        assertEquals("com.a.app", result[0])
        assertEquals("com.z.app", result[1])
    }

    // ─── alphabeticalDrawerInsertIndex ───────────────────────────────────────

    @Test
    fun insertIndex_appNotInInstalled_returnsMinusOne() {
        val installed = listOf(app("com.a", "Apple"))
        val idx = alphabeticalDrawerInsertIndex(
            orderWithoutApp = listOf("com.a"),
            packageName = "com.notinstalled",
            installed = installed,
            folderContents = emptyMap(),
            folderNames = emptyMap(),
            hiddenPackages = emptySet(),
        )
        assertEquals(-1, idx)
    }

    @Test
    fun insertIndex_newAppSortsFirst_returnsZero() {
        val installed = listOf(app("com.a", "Zebra"), app("com.b", "Apple"))
        val idx = alphabeticalDrawerInsertIndex(
            orderWithoutApp = listOf("com.a"),
            packageName = "com.b",
            installed = installed,
            folderContents = emptyMap(),
            folderNames = emptyMap(),
            hiddenPackages = emptySet(),
        )
        assertEquals(0, idx)
    }

    @Test
    fun insertIndex_newAppSortsLast_returnsEndOfList() {
        val installed = listOf(app("com.a", "Apple"), app("com.b", "Zebra"))
        val idx = alphabeticalDrawerInsertIndex(
            orderWithoutApp = listOf("com.a"),
            packageName = "com.b",
            installed = installed,
            folderContents = emptyMap(),
            folderNames = emptyMap(),
            hiddenPackages = emptySet(),
        )
        assertEquals(1, idx)
    }
}
