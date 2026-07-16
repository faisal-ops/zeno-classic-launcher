package com.zeno.classiclauncher.nlauncher.minimalmode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Bars drawn at full strength. [SignalStrength.getLevel] is already reported as 0..4. */
internal const val CELL_SIGNAL_MAX_LEVEL = 4

internal fun hasPhoneStatePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Live cellular signal level (0..[CELL_SIGNAL_MAX_LEVEL]), or null when it genuinely can't be
 * read — permission denied, no SIM, or airplane mode. Callers hide the bars on null rather than
 * drawing a fabricated level.
 *
 * Event-driven, mirroring the BroadcastReceiver pattern the rest of Minimal Mode uses — the
 * platform pushes updates, we never poll. Re-registers when [permissionGranted] flips so the
 * bars appear as soon as the user accepts the prompt, without needing a re-entry.
 *
 * [TelephonyCallback] is API 31+; [PhoneStateListener] covers minSdk 26..30.
 */
@Composable
internal fun rememberCellSignalLevel(permissionGranted: Boolean): Int? {
    val context = LocalContext.current
    var level by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(permissionGranted) {
        val tm = context.getSystemService(TelephonyManager::class.java)
        if (tm == null || !permissionGranted || !hasPhoneStatePermission(context)) {
            level = null
            return@DisposableEffect onDispose { }
        }
        val publish: (SignalStrength) -> Unit = { ss ->
            level = ss.level.coerceIn(0, CELL_SIGNAL_MAX_LEVEL)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) =
                    publish(signalStrength)
            }
            val registered = runCatching {
                tm.registerTelephonyCallback(context.mainExecutor, callback)
            }.isSuccess
            onDispose {
                if (registered) runCatching { tm.unregisterTelephonyCallback(callback) }
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) =
                    publish(signalStrength)
            }
            @Suppress("DEPRECATION")
            val registered = runCatching {
                tm.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            }.isSuccess
            onDispose {
                if (registered) {
                    @Suppress("DEPRECATION")
                    runCatching { tm.listen(listener, PhoneStateListener.LISTEN_NONE) }
                }
            }
        }
    }
    return level
}
