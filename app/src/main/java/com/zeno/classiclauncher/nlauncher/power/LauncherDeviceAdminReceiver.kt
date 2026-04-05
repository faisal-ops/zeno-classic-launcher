package com.zeno.classiclauncher.nlauncher.power

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Minimal device admin: [android.app.admin.DevicePolicyManager.lockNow] only (force-lock policy).
 */
class LauncherDeviceAdminReceiver : DeviceAdminReceiver()
