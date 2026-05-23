package com.zeno.classiclauncher.nlauncher.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent

/** Volume (and mute) must not be consumed by the launcher so SystemUI can show the on-screen panel. */
fun KeyEvent.isVolumePanelKey(): Boolean {
    val code = nativeKeyEvent?.keyCode ?: return false
    return code == AndroidKeyEvent.KEYCODE_VOLUME_UP ||
        code == AndroidKeyEvent.KEYCODE_VOLUME_DOWN ||
        code == AndroidKeyEvent.KEYCODE_VOLUME_MUTE
}

/** BB Classic red/end-call button — always returns to home screen from anywhere. */
fun KeyEvent.isEndCallKey(): Boolean =
    nativeKeyEvent?.keyCode == AndroidKeyEvent.KEYCODE_ENDCALL

enum class FocusArea { DrawerGrid, Dock }

data class NavState(
    val area: FocusArea = FocusArea.DrawerGrid,
    val gridIndex: Int = 0,
    val dockIndex: Int = 0,
)

/**
 * Move the grid cursor. Page changes and home exits are handled in the caller (LauncherScreen).
 * DirectionDown at the bottom row transitions focus to the Dock.
 */
fun NavState.onGridKey(key: Key, cols: Int, itemCount: Int): NavState {
    if (itemCount <= 0) return this
    val lastIndex = (itemCount - 1).coerceAtLeast(0)
    val i = gridIndex.coerceIn(0, lastIndex)
    return when (key) {
        Key.DirectionLeft -> {
            val col = i % cols
            if (col == 0) this else copy(gridIndex = i - 1)
        }
        Key.DirectionRight -> {
            val col = i % cols
            if (col == cols - 1 || i == lastIndex) this else copy(gridIndex = i + 1)
        }
        Key.DirectionUp -> {
            val up = i - cols
            if (up < 0) this else copy(gridIndex = up)
        }
        Key.DirectionDown -> {
            val next = i + cols
            if (next > lastIndex) {
                // Bottom row — drop into dock (start at Mail, index 0)
                copy(area = FocusArea.Dock, dockIndex = 0)
            } else {
                copy(gridIndex = next)
            }
        }
        else -> this
    }
}

/**
 * Dock navigation: left/right move between 4 items (stop at edges), up exits to grid.
 * No isHard requirement — single deliberate swipe is enough for any dock action.
 * dockSize = 4 (Mail=0, Home=1, Shortcut=2, Camera=3).
 */
fun NavState.onDockKey(key: Key, dockSize: Int = 4): NavState {
    return when (key) {
        Key.DirectionLeft -> copy(dockIndex = (dockIndex - 1).coerceAtLeast(0))
        Key.DirectionRight -> copy(dockIndex = (dockIndex + 1).coerceAtMost(dockSize - 1))
        Key.DirectionUp -> copy(area = FocusArea.DrawerGrid)
        else -> this
    }
}

