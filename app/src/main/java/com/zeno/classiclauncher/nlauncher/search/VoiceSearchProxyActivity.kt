package com.zeno.classiclauncher.nlauncher.search

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log

/**
 * Invisible proxy activity that exists solely to host the system speech-recognizer's
 * [Activity.onActivityResult] callback — something Quick Switch's tokenless
 * [SearchOverlayController] window cannot do on its own, since a raw `WindowManager` overlay
 * has no Activity to back an `ActivityResultLauncher`. Launched by
 * [SearchOverlayController.startVoiceSearch]; hands the recognized text back via the
 * one-shot [resultCallback] and finishes itself immediately.
 */
class VoiceSearchProxyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        val launched = runCatching {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_CODE)
        }.isSuccess
        if (!launched) {
            Log.w(TAG, "No speech recognizer available on this device")
            deliverResult(null)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val heard = data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        deliverResult(heard)
        finish()
    }

    private fun deliverResult(text: String?) {
        val callback = resultCallback
        resultCallback = null
        callback?.invoke(text)
    }

    companion object {
        private const val TAG = "ZenoVoiceProxy"
        private const val REQUEST_CODE = 9421

        private var resultCallback: ((String?) -> Unit)? = null

        /** [onResult] is invoked on the main thread with the recognized text, or null if cancelled/unavailable. */
        fun launch(context: Context, onResult: (String?) -> Unit) {
            resultCallback = onResult
            val intent = Intent(context, VoiceSearchProxyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
    }
}
