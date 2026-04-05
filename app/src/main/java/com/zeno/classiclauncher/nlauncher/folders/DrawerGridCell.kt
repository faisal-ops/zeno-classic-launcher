package com.zeno.classiclauncher.nlauncher.folders

import com.zeno.classiclauncher.nlauncher.apps.AppEntry

sealed class DrawerGridCell {
    abstract val slotId: String

    data class App(val entry: AppEntry) : DrawerGridCell() {
        override val slotId: String get() = entry.packageName
    }

    data class Folder(val id: String, val members: List<AppEntry>, val displayTitle: String) : DrawerGridCell() {
        override val slotId: String get() = id
    }
}
