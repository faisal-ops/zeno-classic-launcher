package com.zeno.classiclauncher.nlauncher.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.ui.matchSettingsEntries
import com.zeno.classiclauncher.nlauncher.ui.rankHomeSearchApps
import com.zeno.classiclauncher.nlauncher.ui.tryConsumeSearchKey

/**
 * Shared full-screen search surface used by the home page, the app drawer, and the Quick
 * Switch (any-app) accessibility overlay — one implementation instead of three near-duplicates.
 * Input is captured from hardware key events via [tryConsumeSearchKey] (this launcher targets
 * physical-keyboard devices; there's no on-screen IME field), matching how the pre-unification
 * search surfaces already worked.
 */
@Composable
internal fun UniversalSearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    allApps: List<AppEntry>,
    hiddenPackages: Set<String>,
    onLaunchApp: (String) -> Unit,
    onLaunchSettings: (action: String, fallbackAction: String) -> Unit,
    onShowHiddenApps: () -> Unit,
    koreanInput: Boolean = false,
    showKoreanToggle: Boolean = false,
    onToggleKoreanInput: () -> Unit = {},
    showMic: Boolean = false,
    onVoiceSearch: () -> Unit = {},
    /** Overrides for the Play Store / web-search fallback rows — needed by hosts (like the
     *  Quick Switch system overlay) that must also dismiss themselves after the tap, since the
     *  default behavior only launches the target app without knowing how to close its host. */
    onOpenPlayStore: ((String) -> Unit)? = null,
    onOpenWebSearch: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val appResults = remember(query, allApps, hiddenPackages) {
        rankHomeSearchApps(query, allApps, hiddenPackages).take(8)
    }
    val settingsResults = remember(query) { matchSettingsEntries(query) }
    val trimmedQuery = query.trim().lowercase()
    val isPrivateQuery = trimmedQuery == "private" || trimmedQuery.startsWith("private ")
    var showHelp by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF00E1116))
            .clickable(onClick = onDismiss)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.key == Key.Back) {
                    onDismiss()
                    return@onPreviewKeyEvent true
                }
                val next = tryConsumeSearchKey(ev, query, koreanInput)
                if (next != null) {
                    onQueryChange(next)
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E242C))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFF8E95A3),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = query.ifEmpty { stringResource(R.string.universal_search_hint) },
                            color = if (query.isEmpty()) Color(0xFF6F7D84) else Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (showKoreanToggle) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF3A3F4A))
                                    .clickable(onClick = onToggleKoreanInput)
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = if (koreanInput) "한" else "EN",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                CircleIconButton(onClick = { showHelp = true }) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = stringResource(R.string.cd_search_help),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (query.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .clickable(enabled = false) {},
                ) {
                    if (isPrivateQuery) {
                        SearchResultRow(
                            leading = { RowIconBadge(Icons.Rounded.Lock, tint = Color(0xFFE0B84A)) },
                            label = stringResource(R.string.search_show_hidden_apps),
                            onClick = onShowHiddenApps,
                        )
                        ThinDivider()
                    }
                    appResults.forEach { app ->
                        SearchResultRow(
                            leading = {
                                AsyncImage(
                                    model = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                                )
                            },
                            label = app.label,
                            onClick = { onLaunchApp(app.packageName) },
                        )
                    }
                    if (appResults.isNotEmpty() && settingsResults.isNotEmpty()) ThinDivider()
                    settingsResults.forEach { entry ->
                        SearchResultRow(
                            leading = { RowIconBadge(Icons.Rounded.Settings, tint = Color(0xFF9EADB8)) },
                            label = entry.label,
                            onClick = { onLaunchSettings(entry.action, entry.fallbackAction) },
                        )
                    }
                    if (appResults.isEmpty() && settingsResults.isEmpty() && !isPrivateQuery) {
                        Text(
                            text = stringResource(R.string.drawer_no_apps_found),
                            color = Color(0xFF8E95A3),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                    if (appResults.isEmpty()) {
                        ThinDivider()
                        SearchResultRow(
                            leading = { RowIconBadge(Icons.Rounded.Apps, tint = Color(0xFF4A90D9)) },
                            label = stringResource(R.string.search_play_store, query),
                            labelColor = Color(0xFF84D5F6),
                            onClick = { (onOpenPlayStore ?: { q -> openPlayStoreSearch(context, q) })(query) },
                        )
                        SearchResultRow(
                            leading = { RowIconBadge(Icons.Rounded.Language, tint = Color(0xFF84D5F6)) },
                            label = stringResource(R.string.search_the_internet, query),
                            labelColor = Color(0xFF84D5F6),
                            onClick = { (onOpenWebSearch ?: { q -> openWebSearch(context, q) })(query) },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            if (showMic) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircleIconButton(onClick = onVoiceSearch, size = 56.dp) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = stringResource(R.string.cd_search_voice),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showHelp) {
        // A real Compose Material3 AlertDialog creates its own Android Dialog/window, which
        // needs an Activity window token — fine for home/drawer (hosted in the real Activity)
        // but Quick Switch's overlay is a raw WindowManager window from the accessibility
        // service with no such token, so Dialog.show() throws BadTokenException there. This
        // renders the help panel in-place instead, inside the same window, so it works on both.
        SearchHelpPanel(onDismiss = { showHelp = false })
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF1E242C))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun RowIconBadge(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E242C)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SearchResultRow(
    leading: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    labelColor: Color = Color(0xFFEAF2F8),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ThinDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0x22FFFFFF)),
    )
}

@Composable
private fun SearchHelpPanel(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1F28))
                .clickable(enabled = false) {}
                .padding(20.dp),
        ) {
            Text(
                stringResource(R.string.search_help_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.search_help_body_usage), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD4DC))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.search_help_body_voice), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD4DC))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.search_help_body_hidden), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD4DC))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.search_help_body_fallback), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD4DC))
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.settings_close))
            }
        }
    }
}

internal fun openPlayStoreSearch(context: Context, query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    val encoded = Uri.encode(trimmed)
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded"))
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$encoded&c=apps"))
    runCatching { context.startActivity(marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { context.startActivity(webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

internal fun openWebSearch(context: Context, query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(SearchManager.QUERY, trimmed)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(trimmed)}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching { context.startActivity(fallback) }
    }
}
