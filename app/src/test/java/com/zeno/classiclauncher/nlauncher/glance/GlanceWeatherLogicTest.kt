package com.zeno.classiclauncher.nlauncher.glance

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic tests for the glance strip's calendar query window and the
 * weather location fallback chain helpers.
 */
class GlanceWeatherLogicTest {

    private fun localMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    // ─── Calendar query window ───────────────────────────────────────────────

    @Test
    fun calendarWindow_eveningNow_stillCoversTomorrowLateEvent() {
        // Reproduces the reported bug: at 19:42 the old rolling now+24h window ended at
        // 19:42 tomorrow, hiding a 23:00 meeting tomorrow.
        val now = localMillis(2026, Calendar.JULY, 6, 19, 42)
        val tomorrowLateMeeting = localMillis(2026, Calendar.JULY, 7, 23, 0)
        val end = calendarQueryWindowEnd(now, lookAheadDays = 1)
        assertTrue("window must include tomorrow 23:00", tomorrowLateMeeting < end)
    }

    @Test
    fun calendarWindow_endsAtMidnightBoundary() {
        val now = localMillis(2026, Calendar.JULY, 6, 19, 42)
        val end = calendarQueryWindowEnd(now, lookAheadDays = 1)
        val expected = localMillis(2026, Calendar.JULY, 8, 0, 0)
        assertEquals(expected, end)
    }

    @Test
    fun calendarWindow_morningNow_alsoEndsAtSameBoundary() {
        // The boundary depends only on the date, not the time of day.
        val morning = localMillis(2026, Calendar.JULY, 6, 6, 5)
        val evening = localMillis(2026, Calendar.JULY, 6, 23, 55)
        assertEquals(
            calendarQueryWindowEnd(morning, lookAheadDays = 1),
            calendarQueryWindowEnd(evening, lookAheadDays = 1),
        )
    }

    // ─── Haversine ───────────────────────────────────────────────────────────

    @Test
    fun haversine_samePoint_isZero() {
        assertEquals(0.0, haversineKm(28.61, 77.21, 28.61, 77.21), 0.001)
    }

    @Test
    fun haversine_delhiToMumbai_isRoughly1150Km() {
        val km = haversineKm(28.6139, 77.2090, 19.0760, 72.8777)
        assertTrue("expected ~1150 km, got $km", km in 1050.0..1250.0)
    }

    // ─── IP fix trust decision ───────────────────────────────────────────────

    @Test
    fun ipTrusted_whenNoDeviceReferenceExists() {
        assertTrue(shouldTrustIpLocation(52.52, 13.40, null, null, null))
    }

    @Test
    fun ipTrusted_whenNearRecentDeviceFix() {
        val oneHourMs = 60L * 60 * 1000
        assertTrue(shouldTrustIpLocation(28.40, 77.30, 28.61, 77.21, oneHourMs))
    }

    @Test
    fun ipRejected_whenFarFromRecentDeviceFix() {
        // VPN exit in Berlin vs. a device fix from Delhi one hour ago.
        val oneHourMs = 60L * 60 * 1000
        assertFalse(shouldTrustIpLocation(52.52, 13.40, 28.61, 77.21, oneHourMs))
    }

    @Test
    fun ipTrusted_whenDeviceReferenceIsStale() {
        // A week-old device fix shouldn't veto the IP result — the user may have travelled.
        val oneWeekMs = 7L * 24 * 60 * 60 * 1000
        assertTrue(shouldTrustIpLocation(52.52, 13.40, 28.61, 77.21, oneWeekMs))
    }

    // ─── IP geolocation response parsing ─────────────────────────────────────

    @Test
    fun parseIp_geoJsStyleStringCoordinates() {
        val json = """{"latitude":"28.3486","longitude":"77.3241","country":"India"}"""
        assertEquals(28.3486 to 77.3241, parseIpLocation(json))
    }

    @Test
    fun parseIp_ipWhoIsStyleNumericCoordinates() {
        val json = """{"success":true,"latitude":28.3486,"longitude":77.3241}"""
        assertEquals(28.3486 to 77.3241, parseIpLocation(json))
    }

    @Test
    fun parseIp_rejectsFailureFlag_nullIsland_andGarbage() {
        assertNull(parseIpLocation("""{"success":false,"message":"reserved range"}"""))
        assertNull(parseIpLocation("""{"latitude":"0","longitude":"0"}"""))
        assertNull(parseIpLocation("""{"latitude":"91.0","longitude":"10.0"}"""))
        assertNull(parseIpLocation("not json"))
        assertNull(parseIpLocation("""{"ip":"1.2.3.4"}"""))
    }

    // ─── Geocoding response parsing ──────────────────────────────────────────

    @Test
    fun parseGeocoding_extractsCities() {
        val json = """
            {"results":[
              {"name":"Faridabad","latitude":28.41124,"longitude":77.31316,"country":"India","admin1":"Haryana"},
              {"name":"Faridabad","latitude":28.3,"longitude":77.3,"country":"India"}
            ]}
        """.trimIndent()
        val cities = parseGeocodingResults(json)
        assertEquals(2, cities.size)
        assertEquals("Faridabad", cities[0].name)
        assertEquals("Faridabad, Haryana, India", cities[0].label)
        assertEquals(28.41124, cities[0].latitude, 0.00001)
        assertEquals("Faridabad, India", cities[1].label)
    }

    @Test
    fun parseGeocoding_emptyAndGarbageAreEmptyLists() {
        assertTrue(parseGeocodingResults("""{"generationtime_ms":0.5}""").isEmpty())
        assertTrue(parseGeocodingResults("not json").isEmpty())
        assertTrue(parseGeocodingResults("""{"results":[{"latitude":1.0,"longitude":2.0}]}""").isEmpty())
    }
}
