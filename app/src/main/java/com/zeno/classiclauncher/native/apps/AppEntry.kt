package com.zeno.classiclauncher.nlauncher.apps

import android.graphics.drawable.Drawable

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val internal: Boolean = false,
)

