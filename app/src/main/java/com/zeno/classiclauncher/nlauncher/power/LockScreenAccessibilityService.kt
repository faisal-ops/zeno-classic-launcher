package com.zeno.classiclauncher.nlauncher.power

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
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
 *
 * Design notes:
 *  - Keyguard state from [KeyguardManager.isKeyguardLocked] (ground truth), not window events.
 *  - Polling is the only reliable detection path: TYPE_VIEW_TEXT_CHANGED is fired only through
 *    InputConnection (on-screen keyboard), not through physical key events.
 *  - onKeyEvent is intentionally blocked by Android on the secure keyguard.
 *  - TYPE_KEYGUARD (API 35+, value 5) requires FLAG_RETRIEVE_INTERACTIVE_WINDOWS.
 */
class LockScreenAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var keyguardManager: KeyguardManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var isScreenOn = false
    @Volatile private var autoUnlockEnabled = true
    private var dismissAttempted = false
    private var pollingActive = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_REQUEST_LOCK -> {
                    Log.d(TAG, "Lock request received")
                    lockWithAccessibilityAction()
                }
                Intent.ACTION_SCREEN_ON -> handleScreenOn()
                Intent.ACTION_SCREEN_OFF -> handleScreenOff()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        // FLAG_RETRIEVE_INTERACTIVE_WINDOWS is required for getWindows() to return keyguard window
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }

        val filter = IntentFilter().apply {
            addAction(ACTION_REQUEST_LOCK)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // Observe the autoUnlock pref so the service always reflects the latest setting
        serviceScope.launch {
            LauncherPrefsRepository(applicationContext).prefsFlow.collect { prefs ->
                autoUnlockEnabled = prefs.autoUnlockEnabled
                Log.d(TAG, "autoUnlockEnabled updated → $autoUnlockEnabled")
            }
        }
        Log.d(TAG, "Service connected — flagRetrieveInteractiveWindows set")
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        if (instance === this) instance = null
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
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
        Log.d(TAG, "Screen on — isKeyguardLocked=$locked autoUnlockEnabled=$autoUnlockEnabled")
        if (locked && autoUnlockEnabled) {
            handler.postDelayed(::attemptDismiss, DISMISS_DELAY_MS)
        }
    }

    private fun handleScreenOff() {
        isScreenOn = false
        dismissAttempted = false
        pollingActive = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Screen off — all state reset")
    }

    // ── Dismiss lock screen overlay ───────────────────────────────────────────

    private fun attemptDismiss() {
        if (!isScreenOn) return
        dismissAttempted = true
        Log.d(TAG, "Launching DismissKeyguardActivity")
        DismissKeyguardActivity.launch(this)
        // Give the activity time to call requestDismissKeyguard and PIN bouncer to appear
        handler.postDelayed(::startPinPolling, POLLING_START_DELAY_MS)
    }

    // ── PIN polling ───────────────────────────────────────────────────────────

    private fun startPinPolling() {
        if (!isScreenOn || !keyguardManager.isKeyguardLocked) {
            Log.d(TAG, "PIN polling skipped — isScreenOn=$isScreenOn locked=${keyguardManager.isKeyguardLocked}")
            return
        }
        pollingActive = true
        Log.d(TAG, "PIN polling started — interval=${POLL_INTERVAL_MS}ms")
        handler.post(pinPollRunnable)
    }

    private val pinPollRunnable = object : Runnable {
        override fun run() {
            if (!isScreenOn || !pollingActive) return
            val len = findPinFieldLength()
            Log.d(TAG, "PIN poll — length=$len")
            when {
                len < 0 -> {
                    // PIN field not found yet — keep polling
                    handler.postDelayed(this, POLL_INTERVAL_MS)
                }
                len >= PIN_LENGTH -> {
                    pollingActive = false
                    Log.d(TAG, "PIN complete ($len chars) — confirming")
                    triggerPinConfirm()
                }
                else -> handler.postDelayed(this, POLL_INTERVAL_MS)
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
     * Each non-root node is recycled after processing.
     */
    private fun bfsForPasswordLength(root: AccessibilityNodeInfo): Int {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val isTarget = node.isPassword || (node.isEditable && node.isFocused)
            if (isTarget && node.text != null) {
                val len = node.text.length
                Log.d(TAG, "  bfs PIN node cls=${node.className} len=$len isPassword=${node.isPassword}")
                // Recycle queued nodes before returning
                queue.forEach { it.recycle() }
                if (node !== root) node.recycle()
                return len
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            if (node !== root) node.recycle()
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
        private const val PIN_LENGTH = 4
        private const val DISMISS_DELAY_MS = 400L        // wait for screen to fully render
        private const val POLLING_START_DELAY_MS = 800L  // wait for PIN bouncer to appear after swipe
        private const val POLL_INTERVAL_MS = 150L

        const val ACTION_REQUEST_LOCK = "com.zeno.classiclauncher.nlauncher.action.REQUEST_LOCK"

        @Volatile
        private var instance: LockScreenAccessibilityService? = null

        fun tryLockScreen(): Boolean = instance?.let {
            it.lockWithAccessibilityAction()
        } ?: false

        fun sendLockRequestBroadcast(context: Context) {
            context.sendBroadcast(
                Intent(ACTION_REQUEST_LOCK).setPackage(context.packageName),
            )
        }
    }
}
