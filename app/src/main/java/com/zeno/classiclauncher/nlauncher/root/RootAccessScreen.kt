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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeno.classiclauncher.nlauncher.R
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
        Text(stringResource(R.string.root_access_title), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TITLE_COLOR)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.root_access_subtitle), fontSize = 13.sp, color = SUBTITLE_COLOR)
        Spacer(Modifier.height(24.dp))

        // ── Permission status card ────────────────────────────────────────────
        RootCard {
            StatusRow(
                icon = if (rootGranted) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                label = stringResource(R.string.root_launcher_permission_label),
                value = if (rootGranted) stringResource(R.string.root_granted) else stringResource(R.string.root_not_granted),
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
            Text(if (detecting) stringResource(R.string.root_checking) else stringResource(R.string.root_check_status_button))
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
                                        d.highConfidence -> stringResource(R.string.root_status_rooted)
                                        d.isRooted -> stringResource(R.string.root_status_rooted_low_confidence)
                                        else -> stringResource(R.string.root_status_not_rooted)
                                    },
                                    fontSize = 15.sp,
                                    color = if (d.isRooted) SUCCESS else SUBTITLE_COLOR,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    when {
                                        d.highConfidence -> stringResource(R.string.root_status_rooted_desc)
                                        d.isRooted -> stringResource(R.string.root_status_rooted_low_desc)
                                        else -> stringResource(R.string.root_status_not_rooted_desc)
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
                                            stringResource(R.string.root_detected_tap_grant),
                                            fontSize = 12.sp, color = SUBTITLE_COLOR, lineHeight = 17.sp,
                                        )
                                    } else {
                                        // Root not detected — guide user through Magisk SuperUser grant
                                        Text(
                                            stringResource(R.string.root_detection_blocked),
                                            fontSize = 12.sp, color = SUBTITLE_COLOR, lineHeight = 17.sp,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            stringResource(R.string.root_grant_instructions),
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
                                statusMessage = context.getString(R.string.root_granted_status_msg)
                            } else {
                                statusMessage = context.getString(R.string.root_perm_commands_failed)
                            }
                        } else {
                            statusMessage = context.getString(R.string.root_grant_denied)
                        }
                        loading = false
                    }
                },
                enabled = !loading && !detecting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (loading) stringResource(R.string.root_requesting) else stringResource(R.string.root_grant_button), fontWeight = FontWeight.SemiBold)
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
                        statusMessage = context.getString(R.string.root_verifying)
                        val verification = RootManager.detectRoot()
                        detection = verification
                        detecting = false
                        statusMessage = when {
                            !verification.signals.any { it.id == RootSignalId.SU_EXEC && it.detected } ->
                                context.getString(R.string.root_fully_removed)
                            !managerRevoked ->
                                context.getString(R.string.root_manager_not_auto_cleared)
                            else ->
                                context.getString(R.string.root_su_still_responds)
                        }
                    }
                },
                enabled = !loading && !detecting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DANGER),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (loading) stringResource(R.string.root_revoking) else stringResource(R.string.root_revoke_button), fontWeight = FontWeight.SemiBold)
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(statusMessage, fontSize = 12.sp, color = SUBTITLE_COLOR, modifier = Modifier.padding(horizontal = 4.dp))
        }

        // ── Root features list ────────────────────────────────────────────────
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.root_features_section), fontSize = 13.sp, color = SUBTITLE_COLOR, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        RootCard {
            RootFeatureRow(
                icon = Icons.Rounded.GridView,
                title = stringResource(R.string.root_qs_tiles_title),
                subtitle = stringResource(R.string.root_qs_tiles_subtitle),
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
