package com.zeno.classiclauncher.nlauncher.power

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

/**
 * Minimal accessibility service used only to lock the screen via
 * [performGlobalAction] ([GLOBAL_ACTION_LOCK_SCREEN]), matching the power-button path so face
 * unlock keeps working. [DevicePolicyManager.lockNow] can skip biometrics on some devices.
 */
class LockScreenAccessibilityService : AccessibilityService() {

    private val lockReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_REQUEST_LOCK) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        ContextCompat.registerReceiver(
            this,
            lockReceiver,
            IntentFilter(ACTION_REQUEST_LOCK),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(lockReceiver)
        } catch (_: Exception) {
        }
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    companion object {
        const val ACTION_REQUEST_LOCK = "com.zeno.classiclauncher.nlauncher.action.REQUEST_LOCK"

        @Volatile
        private var instance: LockScreenAccessibilityService? = null

        fun tryLockScreen(): Boolean {
            val s = instance ?: return false
            return s.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        fun sendLockRequestBroadcast(context: Context) {
            context.sendBroadcast(
                Intent(ACTION_REQUEST_LOCK).setPackage(context.packageName),
            )
        }
    }
}
