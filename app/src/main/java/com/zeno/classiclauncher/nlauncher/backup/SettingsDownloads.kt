package com.zeno.classiclauncher.nlauncher.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

object SettingsDownloads {

    /**
     * Opens the system file picker with the Downloads folder as the initial location (when supported).
     */
    fun openBackupJsonPickerIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "text/*"))
            runCatching {
                val initial = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial)
            }
        }

    fun isPickerOk(resultCode: Int): Boolean = resultCode == Activity.RESULT_OK
}
