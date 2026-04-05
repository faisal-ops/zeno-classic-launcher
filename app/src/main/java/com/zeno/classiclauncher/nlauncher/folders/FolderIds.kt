package com.zeno.classiclauncher.nlauncher.folders

import java.util.UUID

object FolderIds {
    /** Synthetic "package" slot id stored in [orderedPackages][com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefs.orderedPackages]. */
    const val PREFIX: String = "com.zeno.classiclauncher.slot.folder."

    @Suppress("NOTHING_TO_INLINE")
    inline fun isFolderId(token: String): Boolean = token.startsWith(PREFIX)

    fun newId(): String = PREFIX + UUID.randomUUID().toString().replace("-", "").take(12)
}
