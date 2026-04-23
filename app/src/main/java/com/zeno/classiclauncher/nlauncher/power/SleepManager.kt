package com.zeno.classiclauncher.nlauncher.power

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import com.zeno.classiclauncher.nlauncher.R

/**
 * Lock order:
 * 1. Hidden [PowerManager.goToSleep] with [GO_TO_SLEEP_REASON_POWER_BUTTON] (same reason as the
 *    hardware key). **Requires [android.Manifest.permission.DEVICE_POWER]** — system_server enforces
 *    this, so normal Play Store installs almost always skip this step.
 * 2. [LockScreenAccessibilityService] global lock when enabled in settings (face-friendly on many devices).
 * 3. [DevicePolicyManager.lockNow] if device admin is active (often logs `device_admin` sleep and breaks face).
 */
object SleepManager {

    fun adminComponent(context: Context): ComponentName =
        ComponentName(context, LauncherDeviceAdminReceiver::class.java)

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(adminComponent(context))
    }

    /** True when our lock helper accessibility service is enabled in system settings. */
    fun isLockAccessibilityEnabled(context: Context): Boolean {
        if (isLockAccessibilityEnabledViaManager(context)) return true
        return isLockAccessibilityEnabledViaSecureSettings(context)
    }

    private fun isLockAccessibilityEnabledViaManager(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expected = ComponentName(context, LockScreenAccessibilityService::class.java)
        val list = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (info in list) {
            val svc = info.resolveInfo.serviceInfo ?: continue
            if (expected.packageName == svc.packageName && expected.className == svc.name) return true
        }
        return false
    }

    private fun isLockAccessibilityEnabledViaSecureSettings(context: Context): Boolean {
        val cn = ComponentName(context, LockScreenAccessibilityService::class.java)
        val flat = cn.flattenToString().lowercase()
        val className = LockScreenAccessibilityService::class.java.name.lowercase()
        val setting =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
        return setting.lowercase().split(':').any { entry ->
            val e = entry.trim()
            e == flat || e.endsWith("/$className") || e.contains(flat)
        }
    }

    /**
     * Release UI treats double-tap lock as available without requiring Device Admin.
     * Device admin remains implemented below as a future/manual fallback, but it is no longer
     * promoted as a prerequisite because the primary lock path works on the target build.
     */
    fun isDoubleTapLockReady(): Boolean = true

    /**
     * Best-effort: real power-button sleep reason. Succeeds only if the app holds [DEVICE_POWER]
     * (platform / privileged app), not for a typical store install.
     */
    @Suppress("PrivateApi", "DiscouragedPrivateApi")
    private fun tryGoToSleepPowerButtonReason(context: Context): Boolean {
        return try {
            val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val uptime = SystemClock.uptimeMillis()
            val reason =
                runCatching {
                    PowerManager::class.java.getField("GO_TO_SLEEP_REASON_POWER_BUTTON").getInt(null)
                }.getOrDefault(4)
            val m =
                PowerManager::class.java.getMethod(
                    "goToSleep",
                    Long::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                )
            m.invoke(pm, uptime, reason, 0)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * @return true if a lock was requested (including scheduled accessibility retries).
     */
    fun lockNow(context: Context): Boolean {
        if (tryGoToSleepPowerButtonReason(context)) return true

        val a11yOn = isLockAccessibilityEnabled(context)
        if (a11yOn) {
            if (LockScreenAccessibilityService.tryLockScreen()) return true
            LockScreenAccessibilityService.sendLockRequestBroadcast(context)
            scheduleAccessibilityLockRetries(context)
            return true
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val comp = adminComponent(context)
        if (!dpm.isAdminActive(comp)) {
            showToast(
                context,
                "If lock does not work on this device, enable “Zeno Classic lock helper” in Accessibility.",
            )
            return false
        }
        dpm.lockNow()
        return true
    }

    private fun scheduleAccessibilityLockRetries(context: Context) {
        val appCtx = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        var attempt = 0
        val maxAttempts = 16
        val delayMs = 50L
        val run =
            object : Runnable {
                override fun run() {
                    if (LockScreenAccessibilityService.tryLockScreen()) return
                    LockScreenAccessibilityService.sendLockRequestBroadcast(appCtx)
                    attempt++
                    if (attempt < maxAttempts) {
                        main.postDelayed(this, delayMs)
                    } else {
                        showToast(
                            appCtx,
                            "Lock helper didn’t lock the screen—open Zeno Classic once or toggle “Zeno Classic lock helper” off and on.",
                            Toast.LENGTH_LONG,
                        )
                    }
                }
            }
        main.post(run)
    }

    fun createEnableAdminIntent(context: Context): android.content.Intent {
        val comp = adminComponent(context)
        return android.content.Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.device_admin_explanation),
            )
        }
    }

    private fun showToast(context: Context, msg: String, duration: Int = Toast.LENGTH_SHORT) {
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(app, msg, duration).show()
        }
    }
}
