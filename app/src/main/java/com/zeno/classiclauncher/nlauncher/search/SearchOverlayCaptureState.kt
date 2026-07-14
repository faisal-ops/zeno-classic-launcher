package com.zeno.classiclauncher.nlauncher.search

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges the custom-trigger capture dialog (in [com.zeno.classiclauncher.nlauncher.ui.PermissionsSettingsOverlay])
 * to [com.zeno.classiclauncher.nlauncher.power.LockScreenAccessibilityService]. Capture must happen through the accessibility service,
 * not the app's own window — on this hardware, bare Alt/Sym key-down events are not reliably
 * delivered to a normal focused View's key dispatch (they're swallowed as modifier state), but
 * they ARE reliably delivered to an accessibility service's [android.view.KeyEvent] filter, which
 * is exactly how the runtime trigger detection already works.
 */
object SearchOverlayCaptureState {

    @Volatile
    var isRecording = false
        private set

    private val _status = MutableStateFlow<CaptureStatus>(CaptureStatus.Idle)
    val status: StateFlow<CaptureStatus> = _status.asStateFlow()

    fun start() {
        isRecording = true
        _status.value = CaptureStatus.Listening
    }

    fun stop() {
        isRecording = false
        _status.value = CaptureStatus.Idle
    }

    /** Called by the accessibility service after the first tap of a single key, while it waits
     *  to see if a confirming second tap follows within the double-tap window. */
    fun reportWaitingForSecondTap(keyCode: Int) {
        _status.value = CaptureStatus.WaitingForSecondTap(keyCode)
    }

    /** Called by the accessibility service once a gesture is fully resolved — either a two-key
     *  hold-combo or a confirmed double-tap of one key. */
    fun reportCaptured(keyCode1: Int, keyCode2: Int) {
        isRecording = false
        _status.value = CaptureStatus.Captured(keyCode1, keyCode2)
    }
}

sealed interface CaptureStatus {
    data object Idle : CaptureStatus
    data object Listening : CaptureStatus
    data class WaitingForSecondTap(val keyCode: Int) : CaptureStatus
    data class Captured(val keyCode1: Int, val keyCode2: Int) : CaptureStatus
}
