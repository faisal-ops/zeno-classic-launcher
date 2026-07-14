package com.zeno.classiclauncher.nlauncher.search

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Permission status for the Alt+Sym global search overlay. Only one grant is needed —
 * [SearchOverlayAccessibilityService] enabled in system Accessibility settings — which covers
 * both key detection and the floating window itself: the overlay is added with
 * [android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY], a window type reserved
 * for bound accessibility services that does **not** require the separate "Display over other
 * apps" (SYSTEM_ALERT_WINDOW) permission. No root involved either way.
 */
object SearchOverlayPermissions {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (isEnabledViaManager(context)) return true
        return isEnabledViaSecureSettings(context)
    }

    private fun isEnabledViaManager(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val expected = ComponentName(context, SearchOverlayAccessibilityService::class.java)
        val list = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return list.any { info ->
            val svc = info.resolveInfo.serviceInfo ?: return@any false
            svc.packageName == expected.packageName && svc.name == expected.className
        }
    }

    private fun isEnabledViaSecureSettings(context: Context): Boolean {
        val cn = ComponentName(context, SearchOverlayAccessibilityService::class.java)
        val flat = cn.flattenToString().lowercase()
        val className = SearchOverlayAccessibilityService::class.java.name.lowercase()
        val setting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return setting.lowercase().split(':').any { entry ->
            val e = entry.trim()
            e == flat || e.endsWith("/$className") || e.contains(flat)
        }
    }
}
