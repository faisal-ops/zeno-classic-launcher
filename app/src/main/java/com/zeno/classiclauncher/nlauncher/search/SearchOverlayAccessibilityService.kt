package com.zeno.classiclauncher.nlauncher.search

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Standalone accessibility service, deliberately isolated from [com.zeno.classiclauncher.nlauncher.power.LockScreenAccessibilityService]:
 * its only job is watching the user's own recorded key gesture system-wide to trigger the
 * floating search overlay. Kept as its own service (own manifest entry, own user-facing toggle)
 * so a bug in key-combo detection here can never interfere with auto-unlock.
 *
 * There is no predefined trigger — the user records their own key(s) in Settings (see
 * `PermissionsSettingsOverlay`'s capture dialog). A non-zero second key means hold-combo mode
 * (both keys held together); a zero second key means double-tap mode on the first key alone.
 * Nothing fires until a key has been recorded.
 *
 * Never consumes key events (always returns false from [onKeyEvent]) — the watched key(s) keep
 * their normal solo function in every app; only the recorded gesture is acted on here, and even
 * then nothing is swallowed from the underlying app's input stream.
 *
 * Does not fire while [LauncherForegroundState.isForeground] is true — home and the app drawer
 * already have their own search, so the global overlay is for other apps only.
 */
class SearchOverlayAccessibilityService : AccessibilityService() {

    // Hold-combo state (used when a second key is recorded).
    @Volatile private var key1Down = false
    @Volatile private var key2Down = false
    /** Prevents re-firing on every key-repeat event while both keys are held. */
    @Volatile private var triggeredForThisHold = false

    // Double-tap state (used when only one key is recorded) — timestamp of the last UP of the
    // watched key; a DOWN arriving within [DOUBLE_TAP_WINDOW_MS] of it counts as the second tap.
    @Volatile private var lastTapUpAt = 0L

    /** Mirrors prefs.searchOverlayEnabled — lets the user turn off just the trigger without
     *  disabling the whole accessibility service. */
    @Volatile private var overlayEnabled = true

    /** Mirrors prefs.searchOverlayCustomKeyCode1/2. */
    @Volatile private var keyCode1 = 0
    @Volatile private var keyCode2 = 0

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceScope.launch {
            LauncherPrefsRepository(applicationContext).prefsFlow.collect { prefs ->
                overlayEnabled = prefs.searchOverlayEnabled
                keyCode1 = prefs.searchOverlayCustomKeyCode1
                keyCode2 = prefs.searchOverlayCustomKeyCode2
            }
        }
        Log.d(TAG, "Service connected — filtering key events for search overlay trigger")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (SearchOverlayCaptureState.isRecording) {
            handleCapture(event)
            return false
        }
        val key1 = keyCode1
        if (key1 != 0) {
            val key2 = keyCode2
            if (key2 != 0) {
                handleHoldCombo(event, key1, key2)
            } else {
                handleDoubleTap(event, key1)
            }
        }
        return false
    }

    // ── Custom trigger capture (Settings → Quick Switch → Set/Change combination) ─────────────

    private val captureHeldKeys = mutableSetOf<Int>()
    private val captureInvolvedKeys = mutableListOf<Int>()
    private var captureMaxSimultaneous = 0
    private var captureFirstTapKey = 0
    private var captureFirstTapUpAt = 0L

    private fun handleCapture(event: KeyEvent) {
        val code = event.keyCode
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (captureHeldKeys.add(code)) {
                    if (code !in captureInvolvedKeys) captureInvolvedKeys.add(code)
                    captureMaxSimultaneous = maxOf(captureMaxSimultaneous, captureHeldKeys.size)
                }
            }
            KeyEvent.ACTION_UP -> {
                captureHeldKeys.remove(code)
                if (captureHeldKeys.isNotEmpty()) return
                if (captureMaxSimultaneous >= 2 && captureInvolvedKeys.size >= 2) {
                    // Two keys were held together — hold-combo mode, finalize immediately.
                    SearchOverlayCaptureState.reportCaptured(captureInvolvedKeys[0], captureInvolvedKeys[1])
                    resetCaptureState()
                } else if (captureInvolvedKeys.size == 1) {
                    val tapped = captureInvolvedKeys[0]
                    val now = SystemClock.uptimeMillis()
                    if (tapped == captureFirstTapKey && now - captureFirstTapUpAt in 0..DOUBLE_TAP_WINDOW_MS) {
                        // Confirming second tap of the same key — finalize as double-tap mode.
                        SearchOverlayCaptureState.reportCaptured(tapped, 0)
                        resetCaptureState()
                    } else {
                        // First tap only — wait for a confirming second tap before saving anything.
                        captureFirstTapKey = tapped
                        captureFirstTapUpAt = now
                        captureInvolvedKeys.clear()
                        captureMaxSimultaneous = 0
                        SearchOverlayCaptureState.reportWaitingForSecondTap(tapped)
                    }
                }
            }
        }
    }

    private fun resetCaptureState() {
        captureHeldKeys.clear()
        captureInvolvedKeys.clear()
        captureMaxSimultaneous = 0
        captureFirstTapKey = 0
        captureFirstTapUpAt = 0L
    }

    private fun handleHoldCombo(event: KeyEvent, keyCodeA: Int, keyCodeB: Int) {
        when (event.keyCode) {
            keyCodeA -> {
                key1Down = event.action == KeyEvent.ACTION_DOWN
                if (!key1Down) triggeredForThisHold = false
            }
            keyCodeB -> {
                key2Down = event.action == KeyEvent.ACTION_DOWN
                if (!key2Down) triggeredForThisHold = false
            }
        }
        if (key1Down && key2Down && !triggeredForThisHold) {
            triggeredForThisHold = true
            fireTrigger()
        }
    }

    private fun handleDoubleTap(event: KeyEvent, targetKeyCode: Int) {
        if (event.keyCode != targetKeyCode) return
        val now = SystemClock.uptimeMillis()
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (now - lastTapUpAt in 0..DOUBLE_TAP_WINDOW_MS) {
                    lastTapUpAt = 0L
                    fireTrigger()
                }
            }
            KeyEvent.ACTION_UP -> lastTapUpAt = now
        }
    }

    private fun fireTrigger() {
        if (!overlayEnabled) return
        if (LauncherForegroundState.isForeground) return
        SearchOverlayController.toggle(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        key1Down = false
        key2Down = false
        triggeredForThisHold = false
        lastTapUpAt = 0L
        resetCaptureState()
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "ZenoSearchOverlay"
        const val DOUBLE_TAP_WINDOW_MS = 400L
    }
}
