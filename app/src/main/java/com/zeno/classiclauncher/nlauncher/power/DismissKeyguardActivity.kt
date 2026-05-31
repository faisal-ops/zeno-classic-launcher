package com.zeno.classiclauncher.nlauncher.power

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager

/**
 * Invisible activity that calls [KeyguardManager.requestDismissKeyguard] — the official API
 * to skip the "tap to unlock" overlay and go straight to PIN entry.
 * Launched by [LockScreenAccessibilityService] on screen-on when the keyguard is locked.
 */
class DismissKeyguardActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        // Required flags: show this activity above the lock screen and turn the screen on
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isKeyguardLocked) {
            Log.d(TAG, "Keyguard not locked — finishing immediately")
            finish()
            return
        }

        // Don't call requestDismissKeyguard here — window focus isn't ready yet.
        // We call it in onWindowFocusChanged once the activity is actually visible.
        Log.d(TAG, "onCreate — waiting for window focus")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isKeyguardLocked) {
            Log.d(TAG, "onWindowFocusChanged — keyguard not locked, finishing")
            finish()
            return
        }
        Log.d(TAG, "onWindowFocusChanged — calling requestDismissKeyguard")
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                Log.d(TAG, "Keyguard dismiss succeeded")
                finish()
            }
            override fun onDismissCancelled() {
                Log.d(TAG, "Keyguard dismiss cancelled")
                finish()
            }
            override fun onDismissError() {
                Log.d(TAG, "Keyguard dismiss error")
                finish()
            }
        })
    }

    companion object {
        private const val TAG = "ZenoUnlock"

        fun launch(context: Context) {
            val intent = Intent(context, DismissKeyguardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
        }
    }
}
