package com.zeno.classiclauncher.nlauncher.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.search.HangulSearch
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette

/**
 * A selectable row shown above the app list that isn't itself an installed app — e.g. the
 * "Sleep" option in the double-tap gesture picker, which is mutually exclusive with picking an app.
 */
data class AppSelectorSpecialOption(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

/**
 * Shared full-screen app picker used by dock shortcuts, home gestures, and the QR scanner tile.
 *
 * Search has no visible field at rest — typing on a hardware/soft keyboard while the list is
 * focused reveals a query pill above the list (same [tryConsumeSearchKey] / Hangul-composition
 * path as the home-screen drawer search), and clearing back to empty hides it again. Dpad
 * Up/Down/Enter navigate the combined [unsetLabel] + [specialOptions] + app list.
 */
@Composable
fun AppSelectorOverlay(
    title: String,
    apps: List<AppEntry>,
    themePalette: LauncherThemePalette,
    selectedPackage: String,
    unsetLabel: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    topToggle: (@Composable () -> Unit)? = null,
    specialOptions: List<AppSelectorSpecialOption> = emptyList(),
) {
    val subtitleColor = Color(0xFF8E95A3)
    val toggleLanguageDescription = stringResource(R.string.cd_toggle_korean_input)
    var query by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    // Toggleable, not a fixed locale check — every a-z key maps to a jamo in dubeolsik, so
    // composing can't be told apart from "the user wants literal English" without a switch.
    var koreanInput by remember(configuration) {
        mutableStateOf(runCatching { configuration.locales[0].language == "ko" }.getOrDefault(false))
    }
    val showLanguageToggle = remember(configuration) {
        runCatching { configuration.locales[0].language == "ko" }.getOrDefault(false)
    }

    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        apps.filter {
            if (it.internal) return@filter false
            if (q.isEmpty()) return@filter true
            it.label.lowercase().contains(q) || HangulSearch.matches(it.label.lowercase(), q)
        }
    }

    val specialCount = specialOptions.size
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var focusedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        focusedIndex = when {
            specialOptions.any { it.selected } -> 1 + specialOptions.indexOfFirst { it.selected }
            selectedPackage.isNotEmpty() -> {
                val idx = apps.indexOfFirst { it.packageName == selectedPackage }
                if (idx >= 0) idx + 1 + specialCount else 0
            }
            else -> 0
        }
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(filtered) {
        val maxIndex = specialCount + filtered.size
        if (focusedIndex > maxIndex) focusedIndex = maxIndex
    }

    LaunchedEffect(focusedIndex) {
        val appIdx = focusedIndex - 1 - specialCount
        if (appIdx >= 0 && filtered.isNotEmpty()) {
            listState.animateScrollToItem(appIdx.coerceIn(0, filtered.size - 1))
        }
    }

    BackHandler(enabled = true) {
        if (query.isNotEmpty()) query = "" else onDismiss()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(420f)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up = ev.key == Key.DirectionUp || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down = ev.key == Key.DirectionDown || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER || nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                val maxIndex = specialCount + filtered.size
                when {
                    up -> { focusedIndex = (focusedIndex - 1).coerceAtLeast(0); true }
                    down -> { focusedIndex = (focusedIndex + 1).coerceAtMost(maxIndex); true }
                    enter -> {
                        when {
                            focusedIndex == 0 -> onSelect("")
                            focusedIndex <= specialCount -> specialOptions[focusedIndex - 1].onSelect()
                            else -> filtered.getOrNull(focusedIndex - 1 - specialCount)?.let { onSelect(it.packageName) }
                        }
                        true
                    }
                    else -> {
                        val newQuery = tryConsumeSearchKey(ev, query, koreanInput)
                        if (newQuery != null) {
                            query = newQuery
                            focusedIndex = 0
                            true
                        } else {
                            false
                        }
                    }
                }
            },
        color = themePalette.settingsBg,
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
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = themePalette.settingsMenuTitle,
                    )
                }
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = themePalette.settingsMenuTitle,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                // Decorative only — typing works without tapping this; it just signals the capability.
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.app_selector_type_to_search),
                    tint = subtitleColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            topToggle?.invoke()

            AnimatedVisibility(visible = query.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0xFF1E2430), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        query,
                        color = themePalette.settingsMenuTitle,
                        fontSize = 16.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (showLanguageToggle) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2C3340))
                                .clickable { koreanInput = !koreanInput }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = if (koreanInput) "한" else "EN",
                                color = themePalette.settingsMenuTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.semantics {
                                    contentDescription = toggleLanguageDescription
                                },
                            )
                        }
                    }
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_clear_search),
                        tint = subtitleColor,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { query = "" },
                    )
                }
            }

            Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))

            LazyColumn(state = listState) {
                item {
                    val isNoneSelected = selectedPackage.isEmpty() && specialOptions.none { it.selected }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusedIndex == 0) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
                            .clickable { onSelect("") }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(unsetLabel, color = subtitleColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (isNoneSelected) {
                            Text("✓", color = themePalette.settingsMenuBody, fontSize = 15.sp)
                        }
                    }
                }
                itemsIndexed(specialOptions) { index, option ->
                    val virtualIndex = index + 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusedIndex == virtualIndex) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
                            .clickable { option.onSelect() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(option.icon, contentDescription = null, tint = subtitleColor, modifier = Modifier.size(40.dp).padding(8.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(option.label, color = themePalette.settingsMenuTitle, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (option.selected) {
                            Text("✓", color = themePalette.settingsMenuBody, fontSize = 15.sp)
                        }
                    }
                }
                itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                    val virtualIndex = index + 1 + specialCount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusedIndex == virtualIndex) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
                            .clickable { onSelect(app.packageName) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = app.icon,
                            contentDescription = app.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, color = themePalette.settingsMenuTitle, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        val isSelected = specialOptions.none { it.selected } && app.packageName == selectedPackage
                        if (isSelected) {
                            Text("✓", color = themePalette.settingsMenuBody, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppSelectorToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    themePalette: LauncherThemePalette,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = themePalette.settingsMenuTitle, fontSize = 15.sp, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
