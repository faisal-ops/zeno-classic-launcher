package com.zeno.classiclauncher.nlauncher.search

/**
 * Whether Zeno Classic itself (home screen, app drawer, or any in-launcher overlay) is currently
 * the foreground/resumed activity. Wired from MainActivity's onResume/onPause.
 *
 * The global search overlay must not trigger while this is true — home and the app drawer
 * already have their own working search, so firing the global overlay on top of it would just
 * be a confusing duplicate.
 */
object LauncherForegroundState {
    @Volatile
    var isForeground: Boolean = false
}
