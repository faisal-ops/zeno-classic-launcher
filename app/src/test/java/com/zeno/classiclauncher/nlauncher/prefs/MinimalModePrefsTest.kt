package com.zeno.classiclauncher.nlauncher.prefs

import com.zeno.classiclauncher.nlauncher.apps.homeShortcutStorageToken
import com.zeno.classiclauncher.nlauncher.apps.parseHomeShortcutToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for:
 *  - canAddHomeStripItem (slot limit enforcement used in consumePinShortcutRequest)
 *  - homeShortcutPackages CSV edge cases (dedup, blank filtering)
 *  - parseHomeShortcutToken with # in shortcut ID
 *  - minimalModeChallengeApps CSV parsing
 *  - minimalModeAppLimits "pkg:ms" format parsing (as used in MinimalModeScreen)
 *  - LauncherBackup round-trips for all three new fields
 */
class MinimalModePrefsTest {

    // ─── canAddHomeStripItem ──────────────────────────────────────────────────

    @Test
    fun canAddHomeStripItem_noItems_returnsTrue() {
        val prefs = LauncherPrefs()
        assertTrue(prefs.canAddHomeStripItem())
    }

    @Test
    fun canAddHomeStripItem_belowLimit_returnsTrue() {
        val prefs = LauncherPrefs(
            homeShortcutPackages = listOf("com.a", "com.b", "com.c"),
        )
        assertTrue(prefs.canAddHomeStripItem())
    }

    @Test
    fun canAddHomeStripItem_atLimit_returnsFalse() {
        // STRIP_TOTAL_SLOTS = 5; fill it with shortcuts so count == 5
        val prefs = LauncherPrefs(
            homeShortcutPackages = List(STRIP_TOTAL_SLOTS) { "com.app$it" },
        )
        assertFalse(prefs.canAddHomeStripItem())
    }

    @Test
    fun canAddHomeStripItem_overLimit_returnsFalse() {
        // More than STRIP_TOTAL_SLOTS entries — effectiveHomeStripSlotOrder caps at STRIP_TOTAL_SLOTS
        val prefs = LauncherPrefs(
            homeShortcutPackages = List(STRIP_TOTAL_SLOTS + 2) { "com.app$it" },
        )
        assertFalse(prefs.canAddHomeStripItem())
    }

    // ─── homeShortcutPackages CSV edge cases ──────────────────────────────────

    @Test
    fun homeShortcutToken_duplicateInList_deduplicatedBySetHomeShortcutPackages() {
        // The serialise → distinct() call removes duplicates silently.
        // Verify via backup round-trip (which uses the same CSV path).
        val prefs = LauncherPrefs(
            homeShortcutPackages = listOf("com.a", "com.b", "com.a"),
        )
        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()
        // After round-trip the CSV has already deduped; "com.a" appears once.
        assertEquals(listOf("com.a", "com.b"), restored.homeShortcutPackages)
    }

    @Test
    fun homeShortcutToken_blankEntry_filtered() {
        // Blank tokens produced by split on trailing comma are dropped.
        val prefs = LauncherPrefs(homeShortcutPackages = listOf("com.a", "", "com.b"))
        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()
        assertEquals(listOf("com.a", "com.b"), restored.homeShortcutPackages)
    }

    // ─── parseHomeShortcutToken — # in shortcut ID ───────────────────────────

    @Test
    fun parseHomeShortcutToken_shortcutIdContainsHash_splitsOnFirstHashOnly() {
        // Shortcut ID itself may contain '#', e.g. "profile#42".
        // Token format: "pkg#id", split on first '#' only.
        val token = homeShortcutStorageToken("com.example.app", "profile#42")
        val (pkg, sid) = parseHomeShortcutToken(token)
        assertEquals("com.example.app", pkg)
        assertEquals("profile#42", sid)
    }

    @Test
    fun parseHomeShortcutToken_noHash_returnsNullShortcutId() {
        val (pkg, sid) = parseHomeShortcutToken("com.example.app")
        assertEquals("com.example.app", pkg)
        assertNull(sid)
    }

    // ─── minimalModeChallengeApps CSV parsing ─────────────────────────────────

    @Test
    fun challengeApps_singleEntry_parsedCorrectly() {
        val prefs = LauncherPrefs(minimalModeChallengeApps = setOf("com.instagram.android"))
        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()
        assertEquals(setOf("com.instagram.android"), restored.minimalModeChallengeApps)
    }

    @Test
    fun challengeApps_multipleEntries_allPreserved() {
        val apps = setOf("com.instagram.android", "com.twitter.android", "com.reddit.android")
        val prefs = LauncherPrefs(minimalModeChallengeApps = apps)
        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()
        assertEquals(apps, restored.minimalModeChallengeApps)
    }

    @Test
    fun challengeApps_emptySet_restoredAsEmpty() {
        val prefs = LauncherPrefs(minimalModeChallengeApps = emptySet())
        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()
        assertTrue(restored.minimalModeChallengeApps.isEmpty())
    }

    // ─── minimalModeAppLimits "pkg:ms" parsing (MinimalModeScreen inline parser) ──

    // The parsing logic from MinimalModeScreen.kt line 350-353:
    // prefs.minimalModeAppLimits.split(",")
    //     .filter { it.contains(":") }
    //     .associate { entry -> entry.split(":").let { it[0].trim() to it[1].trim().toLong() } }
    private fun parseAppLimits(raw: String): Map<String, Long> =
        raw.split(",")
            .filter { it.contains(":") }
            .associate { entry ->
                val parts = entry.split(":")
                parts[0].trim() to parts[1].trim().toLong()
            }

    // The serialisation from LauncherPrefsRepository.setMinimalModeAppLimits:
    // limits.entries.joinToString(",") { (k, v) -> "$k:$v" }
    private fun serializeAppLimits(limits: Map<String, Long>): String =
        limits.entries.joinToString(",") { (k, v) -> "$k:$v" }

    @Test
    fun appLimits_singleEntry_roundTrips() {
        val limits = mapOf("com.instagram.android" to 3_600_000L)
        val raw = serializeAppLimits(limits)
        assertEquals(limits, parseAppLimits(raw))
    }

    @Test
    fun appLimits_multipleEntries_allPreserved() {
        val limits = mapOf(
            "com.instagram.android" to 3_600_000L,
            "com.twitter.android" to 1_800_000L,
            "com.reddit.android" to 7_200_000L,
        )
        val raw = serializeAppLimits(limits)
        assertEquals(limits, parseAppLimits(raw))
    }

    @Test
    fun appLimits_malformedEntry_skipped() {
        // An entry without ':' (e.g. from manual DataStore corruption) is silently dropped.
        val raw = "com.instagram.android:3600000,CORRUPTED,com.twitter.android:1800000"
        val result = parseAppLimits(raw)
        assertEquals(2, result.size)
        assertEquals(3_600_000L, result["com.instagram.android"])
        assertEquals(1_800_000L, result["com.twitter.android"])
    }

    @Test
    fun appLimits_emptyString_returnsEmpty() {
        val result = parseAppLimits("")
        assertTrue(result.isEmpty())
    }

    // ─── LauncherBackup round-trips — new fields ─────────────────────────────

    @Test
    fun backup_roundTrip_challengeApps_preserved() {
        val apps = setOf("com.instagram.android", "com.twitter.android")
        val prefs = LauncherPrefs(minimalModeChallengeApps = apps)
        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()
        assertEquals(apps, restored.minimalModeChallengeApps)
    }

    @Test
    fun backup_roundTrip_appLimits_preserved() {
        val raw = "com.instagram.android:3600000,com.twitter.android:1800000"
        val prefs = LauncherPrefs(minimalModeAppLimits = raw)
        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()
        assertEquals(raw, restored.minimalModeAppLimits)
    }

    @Test
    fun backup_roundTrip_swipeRightApp_preserved() {
        val prefs = LauncherPrefs(minimalModeSwipeRightApp = "com.blackberry.hub")
        val restored = LauncherBackup.fromJson(LauncherBackup.toJson(prefs)).getOrThrow()
        assertEquals("com.blackberry.hub", restored.minimalModeSwipeRightApp)
    }

    @Test
    fun backup_missingChallengeApps_defaultsToEmpty() {
        val json = LauncherBackup.toJson(LauncherPrefs())
            .replace("\"minimalModeChallengeApps\"", "\"_removed\"")
        val restored = LauncherBackup.fromJson(json).getOrThrow()
        assertTrue(restored.minimalModeChallengeApps.isEmpty())
    }

    @Test
    fun backup_missingSwipeRightApp_defaultsToEmpty() {
        val json = LauncherBackup.toJson(LauncherPrefs())
            .replace("\"minimalModeSwipeRightApp\"", "\"_removed\"")
        val restored = LauncherBackup.fromJson(json).getOrThrow()
        assertEquals("", restored.minimalModeSwipeRightApp)
    }
}
