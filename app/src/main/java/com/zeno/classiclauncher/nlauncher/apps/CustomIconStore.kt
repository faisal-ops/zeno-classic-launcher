package com.zeno.classiclauncher.nlauncher.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Persists per-app icon overrides as 192×192 PNG files under [Context.filesDir]/custom_icons/.
 * All operations are safe to call from a background thread.
 */
object CustomIconStore {

    private const val TAG = "CustomIconStore"
    private const val DIR = "custom_icons"
    private const val ICON_SIZE = 192

    // Pseudo-package keys for dock slots — won't conflict with real package names
    const val DOCK_MAIL_KEY      = "__dock_mail__"
    const val DOCK_SHORTCUT_KEY  = "__dock_shortcut__"
    const val DOCK_CAMERA_KEY    = "__dock_camera__"

    private fun iconFile(context: Context, pkg: String): File {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        // Replace characters that are invalid in filenames
        return File(dir, "${pkg.replace('/', '_')}.png")
    }

    /** Returns true if a custom icon exists for [pkg]. */
    fun has(context: Context, pkg: String): Boolean = iconFile(context, pkg).exists()

    /** Loads the custom icon for [pkg], or null if none saved. */
    fun load(context: Context, pkg: String): Drawable? {
        val file = iconFile(context, pkg)
        if (!file.exists()) return null
        return runCatching {
            BitmapDrawable(context.resources, file.absolutePath)
        }.getOrElse {
            Log.w(TAG, "Failed to load custom icon for $pkg", it)
            null
        }
    }

    /**
     * Saves the image at [uri] as the custom icon for [pkg].
     * Decodes, scales to [ICON_SIZE]×[ICON_SIZE], and writes as PNG.
     * Returns true on success.
     */
    fun save(context: Context, pkg: String, uri: Uri): Boolean {
        return runCatching {
            val bitmap = decodeBitmap(context, uri)
            val scaled = Bitmap.createScaledBitmap(bitmap, ICON_SIZE, ICON_SIZE, true)
            iconFile(context, pkg).outputStream().buffered().use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d(TAG, "Saved custom icon for $pkg")
            true
        }.getOrElse {
            Log.e(TAG, "Failed to save custom icon for $pkg", it)
            false
        }
    }

    /** Removes the custom icon for [pkg]. Safe to call if none exists. */
    fun delete(context: Context, pkg: String) {
        val file = iconFile(context, pkg)
        if (file.delete()) Log.d(TAG, "Deleted custom icon for $pkg")
    }

    @Suppress("DEPRECATION")
    private fun decodeBitmap(context: Context, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }
}
