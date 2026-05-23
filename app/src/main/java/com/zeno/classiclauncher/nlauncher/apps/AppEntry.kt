package com.zeno.classiclauncher.nlauncher.apps

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val componentName: ComponentName? = null,
    val internal: Boolean = false,
)
