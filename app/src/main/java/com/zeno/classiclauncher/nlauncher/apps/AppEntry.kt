package com.zeno.classiclauncher.nlauncher.apps

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val componentName: ComponentName? = null,
    val internal: Boolean = false,
    /** True when the icon comes from a dynamic activity alias (e.g. Google Calendar day icons).
     *  Icon packs must not override these — the live date icon should always win. */
    val hasDynamicIcon: Boolean = false,
)
