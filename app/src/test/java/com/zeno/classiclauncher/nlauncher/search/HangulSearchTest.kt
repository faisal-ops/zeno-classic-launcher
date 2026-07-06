package com.zeno.classiclauncher.nlauncher.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HangulSearchTest {

    /** Types a QWERTY key sequence through the composer, like physical-keyboard search input. */
    private fun type(keys: String, start: String = ""): String {
        var q = start
        for (c in keys) q = HangulSearch.composeKey(q, c) ?: (q + c)
        return q
    }

    // ─── Composition ─────────────────────────────────────────────────────────

    @Test
    fun compose_simpleSyllable() {
        assertEquals("가", type("rk"))
        assertEquals("안", type("dks"))
    }

    @Test
    fun compose_multiSyllableWord() {
        assertEquals("안녕", type("dkssud"))
        assertEquals("나무", type("skan"))
    }

    @Test
    fun compose_finalConsonantMigratesToNextSyllable() {
        // 각 + ㅏ → 가가 (final ㄱ becomes the next initial)
        assertEquals("가가", type("rkrk"))
        assertEquals("카카오톡", type("zkzkdhxhr"))
    }

    @Test
    fun compose_compoundVowel() {
        assertEquals("과", type("rhk"))
        assertEquals("왜", type("dho"))
        assertEquals("의", type("dml"))
    }

    @Test
    fun compose_compoundFinal() {
        assertEquals("값", type("rkqt"))
        assertEquals("앉", type("dksw"))
    }

    @Test
    fun compose_compoundFinalSplitsBeforeVowel() {
        // 값 + ㅣ → 갑시 (ㅅ of ㅄ migrates)
        assertEquals("갑시", type("rkqtl"))
    }

    @Test
    fun compose_shiftedKeysGiveTenseConsonants() {
        assertEquals("까", type("Rk"))
        assertEquals("빵", type("Qkd"))
        assertEquals("있", type("dlT"))
    }

    @Test
    fun compose_rawCompatJamoAccepted() {
        // A Korean key-layout file may deliver jamo directly instead of Latin letters.
        var q = HangulSearch.composeKey("", 'ㄱ')!!
        q = HangulSearch.composeKey(q, 'ㅏ')!!
        assertEquals("가", q)
    }

    @Test
    fun compose_nonLetterReturnsNull() {
        assertNull(HangulSearch.composeKey("가", '1'))
        assertNull(HangulSearch.composeKey("가", ' '))
        assertNull(HangulSearch.composeKey("가", '.'))
    }

    @Test
    fun compose_loneVowelsAndConsonantsAppendStandalone() {
        assertEquals("ㅏ", type("k"))
        assertEquals("ㄱㄱ", type("rr")) // repeating a consonant never combines into a tense one
    }

    // ─── Jamo-level backspace ────────────────────────────────────────────────

    @Test
    fun backspace_removesOneJamoAtATime() {
        assertEquals("갑", HangulSearch.deleteLastJamo("값"))
        assertEquals("가", HangulSearch.deleteLastJamo("갑"))
        assertEquals("ㄱ", HangulSearch.deleteLastJamo("가"))
        assertEquals("", HangulSearch.deleteLastJamo("ㄱ"))
    }

    @Test
    fun backspace_reducesCompoundVowel() {
        assertEquals("고", HangulSearch.deleteLastJamo("과"))
    }

    @Test
    fun backspace_onlyAffectsLastSyllable() {
        assertEquals("안녀", HangulSearch.deleteLastJamo("안녕"))
    }

    @Test
    fun backspace_nonHangulDropsOneChar() {
        assertEquals("ab", HangulSearch.deleteLastJamo("abc"))
        assertEquals("", HangulSearch.deleteLastJamo(""))
    }

    // ─── Matching ────────────────────────────────────────────────────────────

    @Test
    fun match_composedKoreanQueryAgainstKoreanLabel() {
        assertTrue(HangulSearch.matches("카카오톡", "카카"))
    }

    @Test
    fun match_midCompositionStateStillMatches() {
        // Typing 카카 passes through 캌 (ㅋ attaches as final before the next vowel).
        assertTrue(HangulSearch.matches("카카오톡", "캌"))
    }

    @Test
    fun match_chosungSearch() {
        assertTrue(HangulSearch.matches("카카오톡", "ㅋㅋ"))
        assertTrue(HangulSearch.matches("카카오톡", "ㅋㅋㅇㅌ"))
        assertFalse(HangulSearch.matches("카카오톡", "ㅋㅌ"))
    }

    @Test
    fun match_latinLabelViaComposedQuery() {
        // Korean-mode user types y-o-u-t on QWERTY; composer yields Hangul-ish text,
        // but the Latin-keys fallback must still find "YouTube".
        val q = type("yout")
        assertTrue(HangulSearch.matches("youtube", q.lowercase()))
    }

    @Test
    fun match_pureLatinQueryIsNotAugmented() {
        assertFalse(HangulSearch.matches("카카오톡", "kakao"))
        assertFalse(HangulSearch.matches("youtube", "you"))
    }

    @Test
    fun match_emptyQueryNeverMatches() {
        assertFalse(HangulSearch.matches("카카오톡", ""))
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    @Test
    fun toLatinKeys_roundTripsTypedWord() {
        assertEquals("dbxbqm", HangulSearch.toLatinKeys("유튜브"))
        assertEquals("rkqt", HangulSearch.toLatinKeys("값"))
    }

    @Test
    fun toJamoString_decomposesCompounds() {
        assertEquals("ㄱㅏㅂㅅㅇㅣ", HangulSearch.toJamoString("값이"))
        assertEquals("ㄱㅗㅏ", HangulSearch.toJamoString("과"))
    }

    @Test
    fun chosung_extractsInitials() {
        assertEquals("ㅋㅋㅇㅌ", HangulSearch.chosung("카카오톡"))
        assertEquals("ㄴㅇㅂ abc", HangulSearch.chosung("네이버 abc"))
    }

    @Test
    fun containsHangul_detectsSyllablesAndJamo() {
        assertTrue(HangulSearch.containsHangul("가"))
        assertTrue(HangulSearch.containsHangul("ㅋ"))
        assertFalse(HangulSearch.containsHangul("abc123"))
    }
}
