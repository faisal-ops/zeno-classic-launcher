package com.zeno.classiclauncher.nlauncher.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

// D-pad / trackpad direction helpers — include KeyDown check so callers need no type guard.
val KeyEvent.isDpadUp: Boolean
    get() = type == KeyEventType.KeyDown &&
        (key == Key.DirectionUp || nativeKeyEvent?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP)

val KeyEvent.isDpadDown: Boolean
    get() = type == KeyEventType.KeyDown &&
        (key == Key.DirectionDown || nativeKeyEvent?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN)

val KeyEvent.isDpadLeft: Boolean
    get() = type == KeyEventType.KeyDown &&
        (key == Key.DirectionLeft || nativeKeyEvent?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT)

val KeyEvent.isDpadRight: Boolean
    get() = type == KeyEventType.KeyDown &&
        (key == Key.DirectionRight || nativeKeyEvent?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT)

val KeyEvent.isDpadEnter: Boolean
    get() = type == KeyEventType.KeyDown && (
        key == Key.Enter || key == Key.NumPadEnter ||
        nativeKeyEvent?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        nativeKeyEvent?.keyCode == AndroidKeyEvent.KEYCODE_ENTER)

val KeyEvent.isDpadBack: Boolean
    get() = type == KeyEventType.KeyDown &&
        (key == Key.Back || nativeKeyEvent?.keyCode == AndroidKeyEvent.KEYCODE_BACK)

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
 * Down wraps back to the grid (circular navigation — mirrors BB Classic behaviour).
 * No isHard requirement — single deliberate swipe is enough for any dock action.
 * dockSize = 4 (Mail=0, Home=1, Shortcut=2, Camera=3).
 */
fun NavState.onDockKey(key: Key, dockSize: Int = 4): NavState {
    return when (key) {
        Key.DirectionLeft  -> copy(dockIndex = (dockIndex - 1).coerceAtLeast(0))
        Key.DirectionRight -> copy(dockIndex = (dockIndex + 1).coerceAtMost(dockSize - 1))
        Key.DirectionUp    -> copy(area = FocusArea.DrawerGrid)
        // Down in dock wraps back to drawer grid (circular nav — nothing is below the dock).
        Key.DirectionDown  -> copy(area = FocusArea.DrawerGrid)
        else -> this
    }
}

