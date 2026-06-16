package com.zeno.classiclauncher.nlauncher.ui

import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import com.zeno.classiclauncher.nlauncher.folders.DrawerGridCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for drawer ordering algorithms: alphabetical sort, settings-pin, insert-index.
 * AppEntry.icon is null throughout — it is not used by any ordering logic.
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
    fun alphabeticalOrder_internalSettings_alwaysFirst() {
        val installed = listOf(
            app("com.z", "Zebra"),
            app(SETTINGS, "Settings"),
            app("com.a", "Apple"),
        )
        val result = strictAlphabeticalDrawerOrder(installed, emptyMap(), emptyMap())
        assertEquals(SETTINGS, result[0])
        assertEquals("com.a", result[1])
        assertEquals("com.z", result[2])
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

    // ─── pinInternalSettingsFirst ────────────────────────────────────────────

    @Test
    fun pinSettingsFirst_settingsNotPresent_listUnchanged() {
        val cells = listOf(
            DrawerGridCell.App(app("com.a", "Apple")),
            DrawerGridCell.App(app("com.b", "Banana")),
        )
        val result = pinInternalSettingsFirst(cells)
        assertEquals(cells, result)
    }

    @Test
    fun pinSettingsFirst_settingsAlreadyFirst_noChange() {
        val cells = listOf(
            DrawerGridCell.App(app(SETTINGS, "Settings")),
            DrawerGridCell.App(app("com.a", "Apple")),
        )
        val result = pinInternalSettingsFirst(cells)
        assertEquals(SETTINGS, (result[0] as DrawerGridCell.App).entry.packageName)
    }

    @Test
    fun pinSettingsFirst_settingsInMiddle_movedToFront() {
        val cells = listOf(
            DrawerGridCell.App(app("com.a", "Apple")),
            DrawerGridCell.App(app(SETTINGS, "Settings")),
            DrawerGridCell.App(app("com.z", "Zebra")),
        )
        val result = pinInternalSettingsFirst(cells)
        assertEquals(SETTINGS, (result[0] as DrawerGridCell.App).entry.packageName)
        assertEquals("com.a", (result[1] as DrawerGridCell.App).entry.packageName)
        assertEquals("com.z", (result[2] as DrawerGridCell.App).entry.packageName)
    }

    @Test
    fun pinSettingsFirst_settingsLast_movedToFront() {
        val cells = listOf(
            DrawerGridCell.App(app("com.a", "Apple")),
            DrawerGridCell.App(app("com.z", "Zebra")),
            DrawerGridCell.App(app(SETTINGS, "Settings")),
        )
        val result = pinInternalSettingsFirst(cells)
        assertEquals(SETTINGS, (result[0] as DrawerGridCell.App).entry.packageName)
        assertEquals(3, result.size)
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
