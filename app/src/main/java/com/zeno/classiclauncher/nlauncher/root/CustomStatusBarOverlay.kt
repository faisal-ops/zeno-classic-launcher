package com.zeno.classiclauncher.nlauncher.root

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars

private val TIME_COLOR   = Color(0xFFEAF0F6)
private val MUTED_COLOR  = Color(0xFF9AA0A8)
private val FILL_COLOR   = Color(0xFFE8EDF2)
private val STROKE_COLOR = Color(0xFF9EA8B3)

private val timeFormatter12 = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
private val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val amPmFormatter   = DateTimeFormatter.ofPattern("a", Locale.getDefault())

// ── Public composable — inserted at the top of LauncherScreen ─────────────────
// Styled to match MinimalModeTopBar exactly:
//   Left: BatteryPill  |  Centre: Clock  |  Right: Wifi icon

@Composable
internal fun NormalModeTopBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val is24h = DateFormat.is24HourFormat(context)

    var timeText   by remember { mutableStateOf(currentTime(is24h)) }
    var amPmText   by remember { mutableStateOf(if (is24h) "" else currentAmPm()) }
    var batteryPct by remember { mutableIntStateOf(readBattery(context)) }
    var charging   by remember { mutableStateOf(readCharging(context)) }
    var wifiOn     by remember { mutableStateOf(isWifiOn(context)) }

    // Clock: tick every second
    DisposableEffect(Unit) {
        val thread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                timeText  = currentTime(is24h)
                amPmText  = if (is24h) "" else currentAmPm()
                try { Thread.sleep(1_000L) } catch (_: InterruptedException) { break }
            }
        }.also { it.isDaemon = true; it.start() }
        onDispose { thread.interrupt() }
    }

    // Battery + wifi broadcasts
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        batteryPct = readBattery(ctx)
                        charging   = readCharging(ctx)
                    }
                    WifiManager.WIFI_STATE_CHANGED_ACTION,
                    WifiManager.NETWORK_STATE_CHANGED_ACTION,
                    -> wifiOn = isWifiOn(ctx)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // 3-column Row — mirrors MinimalModeTopBar layout exactly
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left — battery pill
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            TopBarBatteryPill(pct = batteryPct, charging = charging)
        }

        // Centre — time + AM/PM
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = timeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.W300,
                color = TIME_COLOR,
                letterSpacing = (-0.5).sp,
            )
            if (amPmText.isNotEmpty()) {
                Text(
                    text = " $amPmText",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.W300,
                    color = MUTED_COLOR,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Right — wifi icon
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (wifiOn) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                contentDescription = if (wifiOn) "Wi-Fi on" else "Wi-Fi off",
                tint = if (wifiOn) Color(0xFFEAF0F6) else Color(0x2EFFFFFF),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── BatteryPill — canvas-drawn pill matching MinimalMode ──────────────────────

@Composable
private fun TopBarBatteryPill(pct: Int, charging: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(modifier = Modifier.width(28.dp).height(16.dp)) {
            val nubW  = 4.dp.toPx()
            val nubH  = size.height * 0.45f
            val bodyW = size.width - nubW
            val bodyH = size.height
            val sw    = 1.5.dp.toPx()
            val r     = CornerRadius(3.dp.toPx())

            drawRoundRect(color = STROKE_COLOR, size = Size(bodyW, bodyH), cornerRadius = r, style = Stroke(width = sw))

            val maxFillW = bodyW - sw * 2
            val fillW = (maxFillW * ((pct.coerceIn(0, 100)) / 100f)).coerceAtLeast(0f)
            if (fillW > 0f) {
                drawRoundRect(
                    color = FILL_COLOR,
                    topLeft = Offset(sw, sw),
                    size = Size(fillW, bodyH - sw * 2),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
            drawRoundRect(
                color = STROKE_COLOR,
                topLeft = Offset(bodyW, (bodyH - nubH) / 2f),
                size = Size(nubW, nubH),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
        }
        if (pct >= 0) {
            Text(
                text = if (charging) "$pct⚡" else "$pct%",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = FILL_COLOR,
                lineHeight = 13.sp,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun currentTime(is24h: Boolean): String =
    LocalDateTime.now().format(if (is24h) timeFormatter24 else timeFormatter12)

private fun currentAmPm(): String = LocalDateTime.now().format(amPmFormatter)

private fun readBattery(context: Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return -1
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (scale > 0) level * 100 / scale else -1
}

private fun readCharging(context: Context): Boolean {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return false
    return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
}

private fun isWifiOn(context: Context): Boolean {
    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    return wm?.isWifiEnabled == true
}
