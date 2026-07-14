package com.zeno.classiclauncher.nlauncher.power

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import com.zeno.classiclauncher.nlauncher.badges.BadgeNotificationListener
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
import com.zeno.classiclauncher.nlauncher.search.LauncherForegroundState
import com.zeno.classiclauncher.nlauncher.search.SearchOverlayCaptureState
import com.zeno.classiclauncher.nlauncher.search.SearchOverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Accessibility service that:
 *  1. Locks the screen via [GLOBAL_ACTION_LOCK_SCREEN] (existing behaviour).
 *  2. On screen-on with keyguard locked: clicks/swipes the lock screen overlay to reveal PIN entry.
 *  3. Polls the PIN field's text length every [POLL_INTERVAL_MS]ms; at [PIN_LENGTH] chars fires
 *     [GLOBAL_ACTION_DPAD_CENTER] to confirm — works for both physical keyboard and numpad.
 *  4. Watches the user's own recorded key gesture (see the "── Global search overlay ──" section
 *     below) to trigger the floating search overlay from any app. Deliberately kept in the same
 *     service as (1)-(3) so the user only grants Accessibility once — but the search-overlay
 *     state and logic live in their own fields/methods below and never touch lock-screen state,
 *     so a bug there can't affect auto-unlock.
 *
 * Design notes:
 *  - Keyguard state from [KeyguardManager.isKeyguardLocked] (ground truth), not window events.
 *  - Polling is the only reliable detection path: TYPE_VIEW_TEXT_CHANGED is fired only through
 *    InputConnection (on-screen keyboard), not through physical key events.
 *  - onKeyEvent is intentionally blocked by Android on the secure keyguard, so the search-overlay
 *    trigger below naturally never fires on the lock screen either.
 *  - TYPE_KEYGUARD (API 35+, value 5) requires FLAG_RETRIEVE_INTERACTIVE_WINDOWS.
 */
class LockScreenAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var keyguardManager: KeyguardManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var isScreenOn = false
    @Volatile private var autoUnlockEnabled = true
    @Volatile private var pinLength = PIN_LENGTH
    private var dismissAttempted = false
    private var pollingActive = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_REQUEST_LOCK -> {
                    Log.d(TAG, "Lock request received")
                    lockWithAccessibilityAction()
                }
                ACTION_REQUEST_NOTIFICATIONS -> {
                    Log.d(TAG, "Notifications shade request received")
                    performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                }
                Intent.ACTION_SCREEN_ON -> handleScreenOn()
                Intent.ACTION_SCREEN_OFF -> handleScreenOff()
                Intent.ACTION_USER_PRESENT -> handleUserPresent()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        // FLAG_RETRIEVE_INTERACTIVE_WINDOWS is required for getWindows() to return keyguard window.
        // FLAG_REQUEST_FILTER_KEY_EVENTS is for the search-overlay trigger below.
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        val filter = IntentFilter().apply {
            addAction(ACTION_REQUEST_LOCK)
            addAction(ACTION_REQUEST_NOTIFICATIONS)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // Observe the autoUnlock pref so the service always reflects the latest setting
        serviceScope.launch {
            LauncherPrefsRepository(applicationContext).prefsFlow.collect { prefs ->
                autoUnlockEnabled = prefs.autoUnlockEnabled
                pinLength = prefs.autoUnlockPinDigits
                searchOverlayEnabled = prefs.searchOverlayEnabled
                searchOverlayKeyCode1 = prefs.searchOverlayCustomKeyCode1
                searchOverlayKeyCode2 = prefs.searchOverlayCustomKeyCode2
                Log.d(TAG, "prefs updated — autoUnlockEnabled=$autoUnlockEnabled pinLength=$pinLength")
            }
        }
        Log.d(TAG, "Service connected — flagRetrieveInteractiveWindows + flagRequestFilterKeyEvents set")
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        handler.removeCallbacksAndMessages(null)
        resetSearchOverlayState()
        serviceScope.cancel()
        if (instance === this) instance = null
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    // ── Global search overlay (see com.zeno.classiclauncher.nlauncher.search) ──────────────────
    // Isolated from all lock-screen state/logic above — only shares this service instance so the
    // user grants Accessibility once. onKeyEvent is the sole integration point.

    // Hold-combo state (used when a second key is recorded).
    @Volatile private var searchKey1Down = false
    @Volatile private var searchKey2Down = false
    /** Prevents re-firing on every key-repeat event while both keys are held. */
    @Volatile private var searchTriggeredForThisHold = false

    // Double-tap state (used when only one key is recorded) — timestamp of the last UP of the
    // watched key; a DOWN arriving within [SEARCH_DOUBLE_TAP_WINDOW_MS] of it counts as the
    // second tap.
    @Volatile private var searchLastTapUpAt = 0L

    /** Mirrors prefs.searchOverlayEnabled — lets the user turn off just the trigger without
     *  disabling the whole accessibility service. */
    @Volatile private var searchOverlayEnabled = true

    /** Mirrors prefs.searchOverlayCustomKeyCode1/2. */
    @Volatile private var searchOverlayKeyCode1 = 0
    @Volatile private var searchOverlayKeyCode2 = 0

    private val searchCaptureHeldKeys = mutableSetOf<Int>()
    private val searchCaptureInvolvedKeys = mutableListOf<Int>()
    private var searchCaptureMaxSimultaneous = 0
    private var searchCaptureFirstTapKey = 0
    private var searchCaptureFirstTapUpAt = 0L

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (SearchOverlayCaptureState.isRecording) {
            handleSearchOverlayCapture(event)
            return false
        }
        val key1 = searchOverlayKeyCode1
        if (key1 != 0) {
            val key2 = searchOverlayKeyCode2
            if (key2 != 0) {
                handleSearchOverlayHoldCombo(event, key1, key2)
            } else {
                handleSearchOverlayDoubleTap(event, key1)
            }
        }
        return false
    }

    private fun handleSearchOverlayCapture(event: KeyEvent) {
        val code = event.keyCode
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (searchCaptureHeldKeys.add(code)) {
                    if (code !in searchCaptureInvolvedKeys) searchCaptureInvolvedKeys.add(code)
                    searchCaptureMaxSimultaneous = maxOf(searchCaptureMaxSimultaneous, searchCaptureHeldKeys.size)
                }
            }
            KeyEvent.ACTION_UP -> {
                searchCaptureHeldKeys.remove(code)
                if (searchCaptureHeldKeys.isNotEmpty()) return
                if (searchCaptureMaxSimultaneous >= 2 && searchCaptureInvolvedKeys.size >= 2) {
                    // Two keys were held together — hold-combo mode, finalize immediately.
                    SearchOverlayCaptureState.reportCaptured(searchCaptureInvolvedKeys[0], searchCaptureInvolvedKeys[1])
                    resetSearchOverlayCaptureState()
                } else if (searchCaptureInvolvedKeys.size == 1) {
                    val tapped = searchCaptureInvolvedKeys[0]
                    val now = android.os.SystemClock.uptimeMillis()
                    if (tapped == searchCaptureFirstTapKey && now - searchCaptureFirstTapUpAt in 0..SEARCH_DOUBLE_TAP_WINDOW_MS) {
                        // Confirming second tap of the same key — finalize as double-tap mode.
                        SearchOverlayCaptureState.reportCaptured(tapped, 0)
                        resetSearchOverlayCaptureState()
                    } else {
                        // First tap only — wait for a confirming second tap before saving anything.
                        searchCaptureFirstTapKey = tapped
                        searchCaptureFirstTapUpAt = now
                        searchCaptureInvolvedKeys.clear()
                        searchCaptureMaxSimultaneous = 0
                        SearchOverlayCaptureState.reportWaitingForSecondTap(tapped)
                    }
                }
            }
        }
    }

    private fun resetSearchOverlayCaptureState() {
        searchCaptureHeldKeys.clear()
        searchCaptureInvolvedKeys.clear()
        searchCaptureMaxSimultaneous = 0
        searchCaptureFirstTapKey = 0
        searchCaptureFirstTapUpAt = 0L
    }

    private fun handleSearchOverlayHoldCombo(event: KeyEvent, keyCodeA: Int, keyCodeB: Int) {
        when (event.keyCode) {
            keyCodeA -> {
                searchKey1Down = event.action == KeyEvent.ACTION_DOWN
                if (!searchKey1Down) searchTriggeredForThisHold = false
            }
            keyCodeB -> {
                searchKey2Down = event.action == KeyEvent.ACTION_DOWN
                if (!searchKey2Down) searchTriggeredForThisHold = false
            }
        }
        if (searchKey1Down && searchKey2Down && !searchTriggeredForThisHold) {
            searchTriggeredForThisHold = true
            fireSearchOverlayTrigger()
        }
    }

    private fun handleSearchOverlayDoubleTap(event: KeyEvent, targetKeyCode: Int) {
        if (event.keyCode != targetKeyCode) return
        val now = android.os.SystemClock.uptimeMillis()
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (now - searchLastTapUpAt in 0..SEARCH_DOUBLE_TAP_WINDOW_MS) {
                    searchLastTapUpAt = 0L
                    fireSearchOverlayTrigger()
                }
            }
            KeyEvent.ACTION_UP -> searchLastTapUpAt = now
        }
    }

    private fun fireSearchOverlayTrigger() {
        if (!searchOverlayEnabled) return
        if (LauncherForegroundState.isForeground) return
        SearchOverlayController.toggle(this)
    }

    private fun resetSearchOverlayState() {
        searchKey1Down = false
        searchKey2Down = false
        searchTriggeredForThisHold = false
        searchLastTapUpAt = 0L
        resetSearchOverlayCaptureState()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { Log.d(TAG, "Service interrupted") }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    private fun handleScreenOn() {
        isScreenOn = true
        dismissAttempted = false
        pollingActive = false
        handler.removeCallbacksAndMessages(null)
        val locked = keyguardManager.isKeyguardLocked
        val keepLock = shouldKeepLockScreen()
        Log.d(TAG, "Screen on — isKeyguardLocked=$locked autoUnlockEnabled=$autoUnlockEnabled keepLock=$keepLock")
        if (locked && autoUnlockEnabled) {
            if (keepLock) {
                Log.d(TAG, "Keep lock screen — skipping overlay dismiss")
            } else {
                handler.postDelayed(::attemptDismiss, DISMISS_DELAY_MS)
            }
            // Poll for PIN in all cases (including media/keepLock) — auto-submits manual PIN entry
            handler.postDelayed(::startPinPolling, POLLING_START_DELAY_MS)
        }
    }

    private fun handleScreenOff() {
        isScreenOn = false
        dismissAttempted = false
        pollingActive = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Screen off — all state reset")
    }

    /**
     * Fired by Android after ANY successful unlock (face, fingerprint, PIN, pattern, swipe).
     * Brings the launcher to the foreground so the user always lands on the home screen
     * instead of whatever app was last open — consistent with what auto-unlock does.
     * Only acts when [autoUnlockEnabled] is true.
     */
    private fun handleUserPresent() {
        Log.d(TAG, "USER_PRESENT — bringing launcher to front")
        runCatching {
            val intent = packageManager
                .getLaunchIntentForPackage(packageName)
                ?.apply {
                    // FLAG_ACTIVITY_NEW_TASK is required when starting an Activity from a Service.
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
            if (intent != null) startActivity(intent)
            else Log.w(TAG, "No launch intent found for $packageName")
        }.onFailure { Log.w(TAG, "Failed to bring launcher to front: $it") }
    }

    // ── Dismiss lock screen overlay ───────────────────────────────────────────

    private fun attemptDismiss() {
        if (!isScreenOn) return
        // Re-check at actual dismiss time — state may have changed since screen-on
        // (e.g. alarm fires, Spotify resumes after wake transition, BT reconnects, etc.)
        if (shouldKeepLockScreen()) {
            Log.d(TAG, "Keep lock screen at dismiss time — aborting dismiss")
            return
        }
        dismissAttempted = true
        Log.d(TAG, "Launching DismissKeyguardActivity")
        DismissKeyguardActivity.launch(this)
    }

    // ── PIN polling ───────────────────────────────────────────────────────────

    private fun startPinPolling() {
        if (!isScreenOn || !keyguardManager.isKeyguardLocked) {
            Log.d(TAG, "PIN polling skipped — isScreenOn=$isScreenOn locked=${keyguardManager.isKeyguardLocked}")
            return
        }
        pollingActive = true
        Log.d(TAG, "PIN polling started — searching=${POLL_INTERVAL_SEARCHING_MS}ms typing=${POLL_INTERVAL_TYPING_MS}ms")
        handler.post(pinPollRunnable)
    }

    private val pinPollRunnable = object : Runnable {
        override fun run() {
            if (!isScreenOn || !pollingActive) return
            if (!keyguardManager.isKeyguardLocked) {
                pollingActive = false
                Log.d(TAG, "PIN poll — keyguard unlocked, stopping")
                return
            }
            val len = findPinFieldLength()
            Log.d(TAG, "PIN poll — length=$len")
            when {
                len < 0 -> {
                    // Bouncer not visible yet — slow poll to reduce tree walks
                    handler.postDelayed(this, POLL_INTERVAL_SEARCHING_MS)
                }
                len >= pinLength -> {
                    pollingActive = false
                    Log.d(TAG, "PIN complete ($len chars, expected $pinLength) — confirming")
                    triggerPinConfirm()
                }
                else -> {
                    // Field found, user is typing — fast poll for quick completion detection
                    handler.postDelayed(this, POLL_INTERVAL_TYPING_MS)
                }
            }
        }
    }

    // ── PIN field inspection ──────────────────────────────────────────────────

    private fun findPinFieldLength(): Int {
        // Prefer the explicit keyguard window so we never inspect the wrong app
        val windows = getWindowsSafe()
        if (windows != null) {
            for (window in windows) {
                // 5 = TYPE_KEYGUARD (API 35+); 3 = TYPE_SYSTEM (covers PIN bouncer on API 34)
                val isKeyguardType = window.type == 5 ||
                    window.type == AccessibilityWindowInfo.TYPE_SYSTEM
                if (!isKeyguardType) continue
                val root = window.root ?: continue
                val len = bfsForPasswordLength(root)
                root.recycle()
                if (len >= 0) {
                    Log.d(TAG, "PIN found in window type=${window.type} len=$len")
                    return len
                }
            }
        }
        // Fallback: active window (covers cases where getWindows() is restricted)
        val root = rootInActiveWindow ?: return -1
        val len = bfsForPasswordLength(root)
        Log.d(TAG, "PIN via rootInActiveWindow — len=$len pkg=${root.packageName}")
        root.recycle()
        return len
    }

    /**
     * BFS over the node tree. Returns the character count of the first password/editable
     * node that has non-null text, or -1 if none found.
     * Each non-root node is recycled after processing; the finally block recycles any
     * remaining queued nodes if an exception interrupts the traversal.
     */
    private fun bfsForPasswordLength(root: AccessibilityNodeInfo): Int {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        try {
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                val isTarget = node.isPassword || (node.isEditable && node.isFocused)
                if (isTarget && node.text != null) {
                    val len = node.text.length
                    Log.d(TAG, "  bfs PIN node cls=${node.className} len=$len isPassword=${node.isPassword}")
                    queue.forEach { it.recycle() }
                    if (node !== root) node.recycle()
                    return len
                }
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
                if (node !== root) node.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "bfsForPasswordLength exception: $e")
            queue.forEach { runCatching { it.recycle() } }
        }
        return -1
    }

    // ── PIN confirm ───────────────────────────────────────────────────────────

    private fun triggerPinConfirm() {
        // DPAD_CENTER simulates the physical trackpad center / Enter key
        val r1 = performGlobalAction(GLOBAL_ACTION_DPAD_CENTER)
        Log.d(TAG, "GLOBAL_ACTION_DPAD_CENTER → $r1")
        if (r1) return

        // Fallback: find and click a visible confirm button in the window tree
        val root = rootInActiveWindow ?: run {
            Log.w(TAG, "confirm fallback: rootInActiveWindow=null")
            return
        }
        val clicked = clickConfirmButton(root)
        Log.d(TAG, "clickConfirmButton fallback → $clicked")
        root.recycle()
    }

    private fun clickConfirmButton(root: AccessibilityNodeInfo): Boolean {
        val labels = listOf("enter", "confirm", "ok", "done", "go")
        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                if (node.isClickable && node.isVisibleToUser) {
                    val r = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d(TAG, "  clicked '${node.contentDescription ?: node.text}' → $r")
                    node.recycle()
                    if (r) return true
                }
                node.recycle()
            }
        }
        return false
    }

    // ── Lock screen keep-alive detection ─────────────────────────────────────

    /**
     * Single gate: returns true if auto-dismiss should be suppressed.
     * Covers bedtime mode, alarm audio, and active media sessions.
     */
    private fun shouldKeepLockScreen(): Boolean {
        val bedtime = isBedtimeModeActive()
        if (bedtime) {
            Log.d(TAG, "shouldKeepLockScreen — bedtime mode active, keeping lock screen")
            return true
        }
        val systemAudio = isSystemAudioActive()
        val media = if (!systemAudio) hasActiveMediaSession() else false
        Log.d(TAG, "shouldKeepLockScreen — systemAudio=$systemAudio media=$media")
        return systemAudio || media
    }

    private fun isBedtimeModeActive(): Boolean = try {
        // Check only bedtime_mode, not zen_mode. zen_mode is active for any DND activation
        // (meetings, driving, manual DND) — using it as a proxy causes the lock screen to
        // stay visible for all DND modes, not just bedtime. Accept the rare boot-race edge
        // case (Digital Wellbeing initialising) rather than suppressing unlock for every DND.
        val active = Settings.Secure.getInt(contentResolver, "bedtime_mode", 0) == 1
        Log.d(TAG, "isBedtimeModeActive — $active")
        active
    } catch (e: Exception) {
        Log.w(TAG, "bedtime_mode check failed: $e")
        false
    }

    /**
     * Returns true if any audio playback that requires lock screen interaction is active:
     *  - USAGE_ALARM                          : alarm / timer
     *  - USAGE_NOTIFICATION_RINGTONE          : incoming phone call ringtone
     *  - USAGE_VOICE_COMMUNICATION            : active phone call audio
     *  - USAGE_NOTIFICATION_COMMUNICATION_REQUEST : VoIP incoming call (WhatsApp, Meet, etc.)
     *  - USAGE_ASSISTANCE_NAVIGATION_GUIDANCE : GPS / turn-by-turn navigation
     */
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

    // ── Media session detection ───────────────────────────────────────────────

    /**
     * Returns true if any media session has a "visible on lock screen" playback state:
     * PLAYING, PAUSED, BUFFERING, FAST_FORWARDING, or REWINDING.
     *
     * Uses MediaSessionManager.getActiveSessions() via BadgeNotificationListener's
     * NotificationListenerService token — no extra permission required.
     * Falls back to AudioManager.isMusicActive() if the listener isn't connected yet.
     *
     * This is the correct signal for "should lock screen media controls be shown?" because
     * Android SystemUI also uses MediaSession state (not audio hardware output) to decide
     * whether to render media controls — isMusicActive() misses paused sessions, Bluetooth
     * audio, and apps that briefly drop output during screen-wake transitions.
     */
    private fun hasActiveMediaSession(): Boolean {
        return try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(this, BadgeNotificationListener::class.java)
            val sessions = msm.getActiveSessions(cn)
            val activeStates = setOf(
                PlaybackState.STATE_PLAYING,
                PlaybackState.STATE_PAUSED,
                PlaybackState.STATE_BUFFERING,
                PlaybackState.STATE_FAST_FORWARDING,
                PlaybackState.STATE_REWINDING,
            )
            // Also accept null playbackState — session exists but hasn't set state yet
            // (e.g. media app just started). Android's own lock screen shows controls in this case.
            val active = sessions.any {
                val state = it.playbackState?.state
                state == null || state in activeStates
            }
            Log.d(TAG, "hasActiveMediaSession — sessions=${sessions.size} active=$active")
            active
        } catch (e: Exception) {
            // NLS not connected — fall back to activePlaybackConfigurations which works for
            // ALL audio outputs (speaker, Bluetooth, headphones) unlike isMusicActive()
            // which only checks the device speaker hardware output.
            Log.w(TAG, "MediaSessionManager check failed — falling back to activePlaybackConfigurations: $e")
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.activePlaybackConfigurations.any { config ->
                config.audioAttributes.usage == AudioAttributes.USAGE_MEDIA
            }
        }
    }

    // ── Lock screen ───────────────────────────────────────────────────────────

    private fun lockWithAccessibilityAction(): Boolean {
        val result = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        Log.d(TAG, "GLOBAL_ACTION_LOCK_SCREEN → $result")
        return result
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun getWindowsSafe(): List<AccessibilityWindowInfo>? = try {
        windows
    } catch (e: Exception) {
        Log.w(TAG, "getWindows exception: $e")
        null
    }

    companion object {
        private const val TAG = "ZenoUnlock"
        /** Default PIN digit count used before prefs are loaded. Overridden at runtime by [pinLength]. */
        private const val PIN_LENGTH = 4
        private const val DISMISS_DELAY_MS = 400L        // wait for screen to fully render
        private const val POLLING_START_DELAY_MS = 800L  // wait for PIN bouncer to appear after swipe
        // Two-speed poll: slow while waiting for the bouncer to appear, fast once the PIN
        // field is visible so we catch completion quickly without burning extra tree walks.
        private const val POLL_INTERVAL_SEARCHING_MS = 1_000L
        private const val POLL_INTERVAL_TYPING_MS = 200L
        private const val SEARCH_DOUBLE_TAP_WINDOW_MS = 400L

        const val ACTION_REQUEST_LOCK = "com.zeno.classiclauncher.nlauncher.action.REQUEST_LOCK"
        const val ACTION_REQUEST_NOTIFICATIONS = "com.zeno.classiclauncher.nlauncher.action.REQUEST_NOTIFICATIONS"

        @Volatile
        private var instance: LockScreenAccessibilityService? = null

        fun tryLockScreen(): Boolean = instance?.let {
            it.lockWithAccessibilityAction()
        } ?: false

        fun tryShowNotifications(): Boolean = instance?.let {
            it.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        } ?: false

        fun sendLockRequestBroadcast(context: Context) {
            context.sendBroadcast(
                Intent(ACTION_REQUEST_LOCK).setPackage(context.packageName),
            )
        }

        fun sendNotificationsRequestBroadcast(context: Context) {
            context.sendBroadcast(
                Intent(ACTION_REQUEST_NOTIFICATIONS).setPackage(context.packageName),
            )
        }
    }
}
