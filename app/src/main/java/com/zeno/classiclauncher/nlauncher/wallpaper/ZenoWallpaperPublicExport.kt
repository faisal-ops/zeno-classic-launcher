package com.zeno.classiclauncher.nlauncher.wallpaper

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

private const val ASSET_DIR = "zeno_wallpapers"
/** Same folder name as manual adb pushes — visible in Gallery / Photos / pickers. */
private const val PUBLIC_FOLDER = "ZenoClassicWallpapers"
private val RELATIVE_PATH: String
    get() = "${Environment.DIRECTORY_PICTURES}/$PUBLIC_FOLDER"

/**
 * Copies bundled [ASSET_DIR] JPEGs into **public** [Environment.DIRECTORY_PICTURES]/[PUBLIC_FOLDER].
 * OEM “Wallpapers” apps use their own catalog and cannot be fed by third-party apps; this is the
 * standard way to appear next to user photos so **Gallery**, **Photos**, and image pickers can see them.
 */
object ZenoWallpaperPublicExport {

    fun syncFromAssets(context: Context) {
        val app = context.applicationContext
        val names = runCatching { app.assets.list(ASSET_DIR) }
            .getOrNull()
            ?.filter { it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) || it.endsWith(".png", true) }
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            syncMediaStoreQ(app, names)
        } else {
            syncLegacyPublicDir(app, names)
        }
    }

    private fun syncMediaStoreQ(context: Context, names: List<String>) {
        val resolver = context.contentResolver
        for (raw in names) {
            val displayName = zenoDisplayName(raw)
            if (mediaStoreImageExistsQ(resolver, displayName)) continue
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeFor(raw))
                    put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching
                resolver.openOutputStream(uri)?.use { out ->
                    context.assets.open("$ASSET_DIR/$raw").use { it.copyTo(out) }
                } ?: run {
                    resolver.delete(uri, null, null)
                    return@runCatching
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        }
    }

    private fun mediaStoreImageExistsQ(resolver: ContentResolver, displayName: String): Boolean {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sel = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val args = arrayOf(displayName)
        resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, sel, args, null)?.use { c ->
            return c.moveToFirst()
        }
        return false
    }

    private fun syncLegacyPublicDir(context: Context, names: List<String>) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(base, PUBLIC_FOLDER).apply { mkdirs() }
        val mime = "image/jpeg"
        for (raw in names) {
            val displayName = zenoDisplayName(raw)
            val outFile = File(dir, displayName)
            if (outFile.exists() && outFile.length() > 0L) continue
            runCatching {
                FileOutputStream(outFile).use { out ->
                    context.assets.open("$ASSET_DIR/$raw").use { it.copyTo(out) }
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outFile.absolutePath),
                    arrayOf(mimeFor(raw)),
                ) { _, _ -> }
            }
        }
    }

    private fun zenoDisplayName(assetFileName: String): String = "Zeno_$assetFileName"

    private fun mimeFor(file: String): String = when {
        file.endsWith(".png", true) -> "image/png"
        else -> "image/jpeg"
    }
}
