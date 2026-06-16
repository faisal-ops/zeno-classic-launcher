package com.zeno.classiclauncher.nlauncher.minimalmode

import org.junit.Assert.assertEquals
import org.junit.Test

class MinimalModeWeatherTest {

    @Test
    fun formatTemp_celsius_roundsDown() {
        assertEquals("22°", formatTemp(22.7f, useCelsius = true))
    }

    @Test
    fun formatTemp_celsius_zero() {
        assertEquals("0°", formatTemp(0f, useCelsius = true))
    }

    @Test
    fun formatTemp_celsius_negative() {
        assertEquals("-5°", formatTemp(-5.9f, useCelsius = true))
    }

    @Test
    fun formatTemp_fahrenheit_boilingPoint() {
        // 100°C → 212°F
        assertEquals("212°", formatTemp(100f, useCelsius = false))
    }

    @Test
    fun formatTemp_fahrenheit_freezingPoint() {
        // 0°C → 32°F
        assertEquals("32°", formatTemp(0f, useCelsius = false))
    }

    @Test
    fun formatTemp_fahrenheit_bodyTemp() {
        // 37°C → 98.6°F → truncated to 98°
        assertEquals("98°", formatTemp(37f, useCelsius = false))
    }
}
