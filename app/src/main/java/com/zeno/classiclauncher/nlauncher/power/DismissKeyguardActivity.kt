package com.zeno.classiclauncher.nlauncher.power

import android.app.Activity
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.zeno.classiclauncher.nlauncher.badges.BadgeNotificationListener

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
        // Final guard: alarm or media may have become active between screen-on and now.
        if (shouldKeepLockScreen()) {
            Log.d(TAG, "onWindowFocusChanged — alarm/media active, aborting dismiss")
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

    private fun shouldKeepLockScreen(): Boolean = isSystemAudioActive() || hasActiveMediaSession()

    private fun isSystemAudioActive(): Boolean {
        val keepUsages = setOf(
            AudioAttributes.USAGE_ALARM,                         // alarm / timer
            AudioAttributes.USAGE_NOTIFICATION_RINGTONE,         // incoming call ringtone
            AudioAttributes.USAGE_VOICE_COMMUNICATION,           // active call / VoIP audio
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE, // GPS navigation
        )
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.activePlaybackConfigurations.any { config ->
            config.audioAttributes.usage in keepUsages
        }
    }

    private fun hasActiveMediaSession(): Boolean {
        return try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(this, BadgeNotificationListener::class.java)
            val activeStates = setOf(
                PlaybackState.STATE_PLAYING,
                PlaybackState.STATE_PAUSED,
                PlaybackState.STATE_BUFFERING,
                PlaybackState.STATE_FAST_FORWARDING,
                PlaybackState.STATE_REWINDING,
            )
            msm.getActiveSessions(cn).any { it.playbackState?.state in activeStates }
        } catch (e: Exception) {
            Log.w(TAG, "MediaSessionManager check failed — falling back to isMusicActive: $e")
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).isMusicActive
        }
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
