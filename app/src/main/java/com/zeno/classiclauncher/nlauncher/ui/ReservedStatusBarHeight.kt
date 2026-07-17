package com.zeno.classiclauncher.nlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * The status bar's real, measured height in px — frozen at that value even while the actual
 * system status bar is hidden, via a window-insets override installed in MainActivity. Every
 * existing `.statusBarsPadding()` call site across the app keeps reserving exactly this much
 * space whether or not the real bar is currently drawn, so a custom status bar (ZenoStatusBar)
 * can occupy that same reserved strip without anything else in the layout shifting.
 *
 * Defaults to 0 (no reservation) outside MainActivity's provided scope — e.g. in @Preview.
 */
val LocalReservedStatusBarHeightPx = compositionLocalOf { 0 }

/**
 * Hides the real system status bar for the *dialog window* this is composed inside.
 *
 * ModalBottomSheet / AlertDialog / Dialog each render in their own window, which carries its own
 * WindowInsetsController and does NOT inherit the hidden state MainActivity applies to the
 * Activity window. So while ZenoStatusBar is active, opening any sheet or dialog made the real
 * system bar reappear *on top of it* — two clocks and doubled icons (confirmed on-device).
 * MainActivity cannot fix this centrally: its decorView insets listener never fires for another
 * window. Call this once at the top of every dialog/sheet's content lambda.
 *
 * Self-gating via [LocalReservedStatusBarHeightPx] (CompositionLocals do propagate into dialog
 * windows), which is non-zero exactly when we hide the real bar and draw our own — so no caller
 * needs to thread a flag through. No-op outside a dialog window, and no-op in the default
 * configuration where the system bar is left alone.
 */
@Composable
fun HideSystemStatusBarInDialog() {
    val view = LocalView.current
    val active = LocalReservedStatusBarHeightPx.current > 0
    val window = (view.parent as? DialogWindowProvider)?.window
    DisposableEffect(window, active) {
        if (active && window != null) {
            WindowCompat.getInsetsController(window, view)
                .hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose { }
    }
}
