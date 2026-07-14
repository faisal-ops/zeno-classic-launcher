package com.zeno.classiclauncher.nlauncher.search

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
import com.zeno.classiclauncher.nlauncher.ui.SettingsSearchEntry
import com.zeno.classiclauncher.nlauncher.ui.matchSettingsEntries
import com.zeno.classiclauncher.nlauncher.ui.rankHomeSearchApps
import com.zeno.classiclauncher.nlauncher.ui.tryConsumeSearchKey
import kotlinx.coroutines.flow.first

/**
 * Content of the floating search overlay — a compact card near the top of the screen, same
 * visual language as the home screen's own search card, with the rest of the screen as a
 * dismiss-on-tap scrim so the app underneath stays visible (and resumes untouched on dismiss).
 */
@Composable
internal fun SearchOverlayContent(
    onDismiss: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onLaunchSettings: (action: String, fallbackAction: String) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var hiddenPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        val prefsRepo = LauncherPrefsRepository(context.applicationContext)
        hiddenPackages = prefsRepo.prefsFlow.first().hiddenPackages
        allApps = AppsRepository(context.applicationContext, prefsRepo).appsFlow().first()
        focusRequester.requestFocus()
    }

    val appResults = remember(query, allApps, hiddenPackages) {
        rankHomeSearchApps(query, allApps, hiddenPackages).take(6)
    }
    val settingsResults = remember(query) { matchSettingsEntries(query) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xF01A1F28))
                .clickable(enabled = false) {} // absorb taps so the scrim behind doesn't dismiss
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { ev ->
                    if (ev.key == Key.Back) {
                        onDismiss()
                        return@onPreviewKeyEvent true
                    }
                    val next = tryConsumeSearchKey(ev, query)
                    if (next != null) {
                        query = next
                        true
                    } else {
                        false
                    }
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF9EADB8), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    query.ifEmpty { "Search apps & settings" },
                    color = if (query.isEmpty()) Color(0xFF6F7D84) else Color(0xFFEAF2F8),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (query.isNotEmpty()) {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(appResults, key = { "app:${it.packageName}" }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLaunchApp(app.packageName) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, color = Color(0xFFEAF2F8), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    items(settingsResults, key = { "settings:${it.label}" }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLaunchSettings(entry.action, entry.fallbackAction) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2A3A3D)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color(0xFF9EADB8), modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(entry.label, color = Color(0xFFEAF2F8), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (appResults.isEmpty() && settingsResults.isEmpty()) {
                        item {
                            Text(
                                "No results",
                                color = Color(0xFF6F7D84),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
