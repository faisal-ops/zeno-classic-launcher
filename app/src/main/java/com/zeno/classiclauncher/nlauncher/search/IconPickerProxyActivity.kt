package com.zeno.classiclauncher.nlauncher.search

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

/**
 * Invisible proxy activity that exists solely to host the image picker's
 * [Activity.onActivityResult] callback — same trick as [VoiceSearchProxyActivity], applied to
 * "Change icon" from Quick Switch's "⋮" menu. The picker itself is a separate system UI, not our
 * launcher's home screen, so this never has to bring the launcher to the foreground.
 */
class IconPickerProxyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openDocument = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        val launched = runCatching {
            @Suppress("DEPRECATION")
            startActivityForResult(openDocument, REQUEST_CODE)
        }.isSuccess
        if (!launched) {
            val getContent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            val fallbackLaunched = runCatching {
                @Suppress("DEPRECATION")
                startActivityForResult(getContent, REQUEST_CODE)
            }.isSuccess
            if (!fallbackLaunched) {
                Log.w(TAG, "No image picker available on this device")
                deliverResult(null)
                finish()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        deliverResult(data?.data)
        finish()
    }

    private fun deliverResult(uri: Uri?) {
        val callback = resultCallback
        resultCallback = null
        callback?.invoke(uri)
    }

    companion object {
        private const val TAG = "ZenoIconPickerProxy"
        private const val REQUEST_CODE = 9422

        private var resultCallback: ((Uri?) -> Unit)? = null

        /** [onResult] is invoked on the main thread with the picked image's URI, or null if cancelled/unavailable. */
        fun launch(context: Context, onResult: (Uri?) -> Unit) {
            resultCallback = onResult
            val intent = Intent(context, IconPickerProxyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
    }
}
