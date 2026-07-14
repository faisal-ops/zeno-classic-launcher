package com.zeno.classiclauncher.nlauncher.search

import android.content.Context
import com.zeno.classiclauncher.nlauncher.power.SleepManager

/**
 * Permission status for the global search overlay. Only one grant is needed —
 * [com.zeno.classiclauncher.nlauncher.power.LockScreenAccessibilityService] enabled in system
 * Accessibility settings (the same service that already handles auto-unlock; the search-overlay
 * trigger is merged into it so the user only grants Accessibility once) — which covers both key
 * detection and the floating window itself: the overlay is added with
 * [android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY], a window type reserved
 * for bound accessibility services that does **not** require the separate "Display over other
 * apps" (SYSTEM_ALERT_WINDOW) permission. No root involved either way.
 */
object SearchOverlayPermissions {
    fun isAccessibilityServiceEnabled(context: Context): Boolean =
        SleepManager.isLockAccessibilityEnabled(context)
}
