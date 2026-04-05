package com.zeno.classiclauncher.nlauncher.power

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.zeno.classiclauncher.nlauncher.R

/**
 * Locks the device via [DevicePolicyManager.lockNow] when device admin is active.
 * No WakeLock usage for sleep.
 */
object SleepManager {

    fun adminComponent(context: Context): ComponentName =
        ComponentName(context, LauncherDeviceAdminReceiver::class.java)

    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(adminComponent(context))
    }

    /**
     * @return true if lock was requested, false if admin not granted (shows Toast on main thread).
     */
    fun lockNow(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val comp = adminComponent(context)
        if (!dpm.isAdminActive(comp)) {
            showToast(context, "Turn on “Double tap to lock” and grant device admin")
            return false
        }
        dpm.lockNow()
        return true
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

    private fun showToast(context: Context, msg: String) {
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
