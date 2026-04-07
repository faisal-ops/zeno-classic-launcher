package com.zeno.classiclauncher.nlauncher.glance

/**
 * Toggles for the Lawnchair-style glance carousel (left): flashlight, battery, next calendar event, alarm.
 * Master on/off is handled in Compose by not composing [GlanceDateWeatherEventsView].
 */
data class GlanceStripPreferences(
    val showFlashlight: Boolean = false,
    val showBattery: Boolean = true,
    val showCalendar: Boolean = true,
    val showAlarm: Boolean = true,
    val calendarLookAheadDays: Int = 1,
    val weatherUseDeviceLocation: Boolean = true,
    val weatherLatitude: Double? = null,
    val weatherLongitude: Double? = null,
    val weatherUseFahrenheit: Boolean = false,
)
