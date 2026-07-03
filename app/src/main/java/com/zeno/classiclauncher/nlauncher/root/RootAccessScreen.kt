package com.zeno.classiclauncher.nlauncher.root

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val BG = Color(0xFF0E131B)
private val CARD_BG = Color(0xFF161D2A)
private val TITLE_COLOR = Color(0xFFE8EEF7)
private val SUBTITLE_COLOR = Color(0xFF7A8290)
private val ACCENT = Color(0xFF4A90D9)
private val SUCCESS = Color(0xFF34C759)
private val DANGER = Color(0xFFFF453A)
private val WARNING = Color(0xFFFF9F0A)

private const val PKG = "com.zeno.classiclauncher.nlauncher"

@Composable
internal fun RootAccessScreen(
    rootGranted: Boolean,
    rootedQsEnabled: Boolean,
    onDismiss: () -> Unit,
    onRootGranted: () -> Unit,
    onRootRevoked: () -> Unit,
    onRootedQsToggled: (Boolean) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var detection by remember { mutableStateOf<RootDetectionResult?>(null) }
    var detecting by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Root Access", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TITLE_COLOR)
        Spacer(Modifier.height(4.dp))
        Text("Grant root to unlock advanced launcher features", fontSize = 13.sp, color = SUBTITLE_COLOR)
        Spacer(Modifier.height(24.dp))

        // ── Permission status card ────────────────────────────────────────────
        RootCard {
            StatusRow(
                icon = if (rootGranted) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                label = "Launcher root permission",
                value = if (rootGranted) "Granted" else "Not granted",
                valueColor = if (rootGranted) SUCCESS else SUBTITLE_COLOR,
            )
        }

        // ── Root detection section ────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))

        // "Check root status" button — user-triggered, not automatic
        OutlinedButton(
            onClick = {
                detecting = true
                statusMessage = ""
                scope.launch {
                    detection = RootManager.detectRoot()
                    detecting = false
                }
            },
            enabled = !detecting && !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ACCENT),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (detecting) "Checking…" else "Check Root Status")
        }

        // Detection result — overall only
        AnimatedVisibility(
            visible = detection != null,
            enter = expandVertically() + fadeIn(),
        ) {
            val d = detection
            if (d != null) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RootCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (d.isRooted) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (d.isRooted) SUCCESS else SUBTITLE_COLOR,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    when {
                                        d.highConfidence -> "Rooted"
                                        d.isRooted -> "Rooted (low confidence)"
                                        else -> "Not rooted"
                                    },
                                    fontSize = 15.sp,
                                    color = if (d.isRooted) SUCCESS else SUBTITLE_COLOR,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    when {
                                        d.highConfidence -> "Root access confirmed on this device"
                                        d.isRooted -> "Some root indicators found"
                                        else -> "No root indicators detected"
                                    },
                                    fontSize = 12.sp,
                                    color = SUBTITLE_COLOR,
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    }

                    if (!rootGranted) {
                        Spacer(Modifier.height(8.dp))
                        RootCard {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Rounded.Error, contentDescription = null, tint = WARNING, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    if (d.isRooted) {
                                        // Device is confirmed rooted — no need to go to Magisk, just tap Grant
                                        Text(
                                            "Root detected. Tap Grant Root Access below to enable launcher features.",
                                            fontSize = 12.sp, color = SUBTITLE_COLOR, lineHeight = 17.sp,
                                        )
                                    } else {
                                        // Root not detected — guide user through Magisk SuperUser grant
                                        Text(
                                            "Detection blocked by Magisk DenyList + Shamiko (expected on well-spoofed devices).",
                                            fontSize = 12.sp, color = SUBTITLE_COLOR, lineHeight = 17.sp,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "To grant root access:\n" +
                                                "1. Open the Magisk app\n" +
                                                "2. Tap Superuser tab\n" +
                                                "3. Find Zeno Classic → Allow\n" +
                                                "4. Return here and tap Grant Root Access",
                                            fontSize = 12.sp, color = SUBTITLE_COLOR, lineHeight = 18.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Grant / revoke ────────────────────────────────────────────────────
        Spacer(Modifier.height(20.dp))
        if (!rootGranted) {
            Button(
                onClick = {
                    loading = true
                    statusMessage = ""
                    scope.launch {
                        val granted = RootManager.requestRootGrant()
                        if (granted) {
                            val ok = RootManager.execute(
                                "pm grant $PKG android.permission.STATUS_BAR",
                                "appops set $PKG SYSTEM_ALERT_WINDOW allow",
                                "pm grant $PKG android.permission.BLUETOOTH_PRIVILEGED",
                                "pm grant $PKG android.permission.BLUETOOTH_CONNECT",
                            )
                            if (ok) {
                                onRootGranted()
                                // Re-run detection to update status
                                detection = RootManager.detectRoot()
                                statusMessage = "Root access granted. Features unlocked."
                            } else {
                                statusMessage = "Permission commands failed — device may not support them."
                            }
                        } else {
                            statusMessage = "Root grant denied or timed out."
                        }
                        loading = false
                    }
                },
                enabled = !loading && !detecting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (loading) "Requesting…" else "Grant Root Access", fontWeight = FontWeight.SemiBold)
            }
        } else {
            OutlinedButton(
                onClick = {
                    loading = true
                    statusMessage = ""
                    scope.launch {
                        // Step 1: revoke app-level permissions
                        RootManager.execute(
                            "pm revoke $PKG android.permission.STATUS_BAR",
                            "appops set $PKG SYSTEM_ALERT_WINDOW default",
                            "pm revoke $PKG android.permission.BLUETOOTH_PRIVILEGED",
                            "pm revoke $PKG android.permission.BLUETOOTH_CONNECT",
                            "wm overscan reset",
                        )
                        // Step 2: clear the root manager's SuperUser entry (Magisk / KSU / APatch)
                        val managerRevoked = RootManager.revokeRootManagerGrant(PKG)
                        onRootRevoked()
                        // Step 3: verify root is actually gone — re-run detection so user can see proof
                        detecting = true
                        loading = false
                        statusMessage = "Verifying root is fully removed…"
                        val verification = RootManager.detectRoot()
                        detection = verification
                        detecting = false
                        statusMessage = when {
                            !verification.signals.any { it.id == RootSignalId.SU_EXEC && it.detected } ->
                                "Root access fully removed. su is now blocked — confirmed."
                            !managerRevoked ->
                                "Permissions revoked. Could not auto-clear the SuperUser entry — go to your root manager app and remove Zeno Classic manually."
                            else ->
                                "Permissions revoked but su still responds. Remove Zeno Classic from your root manager's SuperUser tab manually."
                        }
                    }
                },
                enabled = !loading && !detecting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DANGER),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (loading) "Revoking…" else "Revoke Root Access", fontWeight = FontWeight.SemiBold)
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(statusMessage, fontSize = 12.sp, color = SUBTITLE_COLOR, modifier = Modifier.padding(horizontal = 4.dp))
        }

        // ── Root features list ────────────────────────────────────────────────
        Spacer(Modifier.height(28.dp))
        Text("Root Features", fontSize = 13.sp, color = SUBTITLE_COLOR, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        RootCard {
            RootFeatureRow(
                icon = Icons.Rounded.GridView,
                title = "Rooted QS Tiles",
                subtitle = "Tap tiles toggle directly (Wi-Fi, mobile data, battery saver, airplane mode, night light, greyscale…)",
                active = rootGranted && rootedQsEnabled,
                enabled = rootGranted,
                onToggle = { if (rootGranted) onRootedQsToggled(it) },
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Internal widgets ──────────────────────────────────────────────────────────

@Composable
private fun RootCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CARD_BG),
    ) { content() }
}

@Composable
private fun StatusRow(icon: ImageVector, label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = SUBTITLE_COLOR, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, color = TITLE_COLOR, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RootFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (active) ACCENT.copy(alpha = 0.15f) else Color(0xFF1F2937)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) ACCENT else SUBTITLE_COLOR,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = TITLE_COLOR, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = SUBTITLE_COLOR, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = active,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ACCENT,
                uncheckedThumbColor = SUBTITLE_COLOR,
                uncheckedTrackColor = Color(0xFF1F2937),
                disabledCheckedTrackColor = SUBTITLE_COLOR.copy(alpha = 0.3f),
                disabledUncheckedTrackColor = Color(0xFF1F2937),
            ),
        )
    }
}
