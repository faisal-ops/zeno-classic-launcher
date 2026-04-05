package com.zeno.classiclauncher.nlauncher.apps

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import android.provider.Settings

class LauncherActions(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    fun launchApp(packageName: String): Boolean {
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    fun startShortcut(packageName: String, shortcutId: String): Boolean {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return false
        return try {
            launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openAppInfo(packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    fun launchCamera(): Boolean {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolved = pm.resolveActivity(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolved != null) {
            val pkg = resolved.activityInfo.packageName
            if (launchApp(pkg)) return true
            // getLaunchIntentForPackage failed — start capture intent directly
            return try { context.startActivity(captureIntent); true } catch (_: Exception) { false }
        }
        // No camera app resolves IMAGE_CAPTURE — try common camera packages
        val fallbacks = listOf(
            "com.android.camera2",
            "com.android.camera",
            "com.google.android.GoogleCamera",
            "com.sec.android.app.camera",
        )
        for (pkg in fallbacks) {
            if (launchApp(pkg)) return true
        }
        return false
    }

    fun launchMail(): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return false
        return launchApp(resolved.activityInfo.packageName)
    }

    fun launchMessages(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolved != null) return launchApp(resolved.activityInfo.packageName)

        val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolvedSms = pm.resolveActivity(sms, PackageManager.MATCH_DEFAULT_ONLY) ?: return false
        return launchApp(resolvedSms.activityInfo.packageName)
    }

    /** System static wallpaper / “Wallpapers” entry (OEM chooser). */
    fun openSystemWallpaperChooser(): Boolean =
        startActivityNewTask(Intent(Intent.ACTION_SET_WALLPAPER))

    fun openLiveWallpaperChooser(): Boolean =
        startActivityNewTask(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))

    fun openWallpaperStyleSettings(): Boolean =
        startActivityNewTask(Intent("android.settings.WALLPAPER_SETTINGS"))

    /** Classic gallery-style image pick. */
    fun openGalleryForWallpaper(): Boolean =
        startActivityNewTask(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))

    /** Photo picker (API 33+) or [Intent.ACTION_GET_CONTENT] on older devices. */
    fun openPhotosForWallpaper(): Boolean {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES)
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        }
        return startActivityNewTask(intent)
    }

    private fun startActivityNewTask(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
