@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.zeno.classiclauncher.nlauncher.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    /** Long-press on an app result row — opens the same app context menu as long-pressing it
     *  in the drawer/home strip. Null (default) disables long-press entirely, e.g. Quick Switch's
     *  tokenless overlay window has no access to the host Activity's context-menu state. */
    onLongPressApp: ((AppEntry) -> Unit)? = null,
    /** No-op if READ_CONTACTS isn't granted (checked internally) — contacts results simply don't
     *  appear, same as how other permission-gated features stay silent until granted elsewhere. */
    onLaunchContact: (ContactResult) -> Unit = {},
    /** Tapping the call icon on a contact row. Null (default) falls back to ACTION_DIAL (opens
     *  the dialer pre-filled, no permission needed) — hosts with an Activity (home/drawer) should
     *  pass this to request CALL_PHONE and place the call directly via ACTION_CALL instead;
     *  Quick Switch's tokenless overlay window has no Activity to request permissions from, so it
     *  keeps the ACTION_DIAL fallback. */
    onCallContact: ((ContactResult) -> Unit)? = null,
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
    /** When false, this composable installs no key handling of its own — the host (e.g. home
     *  page) already owns hardware key capture and D-pad result navigation upstream, and only
     *  wants this composable for presentation. [focusedIndex] then drives the highlight. */
    captureKeys: Boolean = true,
    /** Row index the host wants highlighted (apps first, then settings), driven by the host's
     *  own D-pad Up/Down navigation when [captureKeys] is false. -1 = nothing highlighted. */
    focusedIndex: Int = -1,
    /** Must match the host's own app-result truncation (see [captureKeys]) so [focusedIndex]
     *  lines up with the same row the host computed it against. */
    maxAppResults: Int = 8,
    /** Bumped by hosts (Quick Switch) whenever a homemade overlay drawn on top of this one — e.g.
     *  [QuickSwitchAppMenu] — steals focus for itself and then closes. This composable's own
     *  focus request only fires once, on first mount, so without an explicit signal to re-request
     *  it, focus is simply lost once that covering overlay's focusable node is disposed — leaving
     *  nothing to receive Back (or any other key) afterward. */
    regainFocusKey: Any = Unit,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(regainFocusKey) { if (captureKeys) focusRequester.requestFocus() }

    val appResults = remember(query, allApps, hiddenPackages, maxAppResults) {
        rankHomeSearchApps(query, allApps, hiddenPackages).take(maxAppResults)
    }
    val settingsResults = remember(query) { matchSettingsEntries(query) }
    val trimmedQuery = query.trim().lowercase()
    val isPrivateQuery = trimmedQuery == "private" || trimmedQuery.startsWith("private ")
    var showHelp by remember { mutableStateOf(false) }

    val contactsGranted = remember { contactsPermissionGranted(context) }
    var contactResults by remember { mutableStateOf(emptyList<ContactResult>()) }
    LaunchedEffect(query, contactsGranted) {
        contactResults = if (contactsGranted) matchContacts(context, query) else emptyList()
    }

    // Self-contained D-pad result navigation for hosts that don't already own it (see
    // [captureKeys]/[focusedIndex]) — mirrors the same Up/Down/Enter behavior home page's own
    // upstream key handler has, so every entry point gets identical hardware-keyboard navigation.
    // Contacts are only included in this self-contained cycle (home page's own externally-driven
    // index doesn't know about them, so they stay tap-only there).
    var internalFocusedIndex by remember { mutableStateOf(-1) }
    val totalResultItems = appResults.size + contactResults.size + settingsResults.size
    LaunchedEffect(query, appResults, contactResults, settingsResults) {
        internalFocusedIndex = if (totalResultItems > 0) 0 else -1
    }
    val effectiveFocusedIndex = if (captureKeys) internalFocusedIndex else focusedIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE00E1116))
            .clickable(onClick = onDismiss)
            .then(
                if (captureKeys) {
                    Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { ev ->
                            // Without this, a single physical Back press that also causes a focus
                            // hand-off mid-gesture (e.g. QuickSwitchAppMenu closing, returning
                            // focus here — see regainFocusKey) delivers BOTH its KeyDown and KeyUp
                            // to this handler once this composable is the one holding focus again,
                            // and this Back branch used to fire on either — dismissing this overlay
                            // in the very same press that only meant to close the menu on top of it.
                            if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            if (ev.key == Key.Back) {
                                onDismiss()
                                return@onPreviewKeyEvent true
                            }
                            val next = tryConsumeSearchKey(ev, query, koreanInput)
                            if (next != null) {
                                onQueryChange(next)
                                return@onPreviewKeyEvent true
                            }
                            if (query.isNotEmpty() && totalResultItems > 0) {
                                val nk = ev.nativeKeyEvent
                                when {
                                    ev.key == Key.DirectionDown || nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        internalFocusedIndex = (internalFocusedIndex + 1).coerceAtMost(totalResultItems - 1)
                                        return@onPreviewKeyEvent true
                                    }
                                    ev.key == Key.DirectionUp || nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                        internalFocusedIndex = if (internalFocusedIndex > 0) internalFocusedIndex - 1 else -1
                                        return@onPreviewKeyEvent true
                                    }
                                    ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                                        nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                        nk?.keyCode == android.view.KeyEvent.KEYCODE_ENTER -> {
                                        val idx = if (internalFocusedIndex >= 0) internalFocusedIndex else 0
                                        when {
                                            idx < appResults.size ->
                                                appResults.getOrNull(idx)?.let { onLaunchApp(it.packageName) }
                                            idx < appResults.size + contactResults.size ->
                                                contactResults.getOrNull(idx - appResults.size)?.let { onLaunchContact(it) }
                                            else ->
                                                settingsResults.getOrNull(idx - appResults.size - contactResults.size)?.let {
                                                    onLaunchSettings(it.action, it.fallbackAction)
                                                }
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                }
                            }
                            false
                        }
                } else {
                    Modifier
                },
            ),
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
                        if (query.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_clear),
                                tint = Color(0xFF8E95A3),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onQueryChange("") },
                            )
                        }
                    }
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
                    appResults.forEachIndexed { idx, app ->
                        SearchResultRow(
                            leading = {
                                // Opaque placeholder behind the icon: a row that's newly appeared
                                // in the filtered list (typing narrows/widens results every
                                // keystroke) needs a fresh Coil decode, and without this the gap
                                // was fully transparent for that frame — letting the dimmed
                                // background app grid show through where the icon should be,
                                // which read as the grid itself "jumping" through the overlay.
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2C3547)),
                                ) {
                                    AsyncImage(
                                        model = app.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                            },
                            label = app.label,
                            onClick = { onLaunchApp(app.packageName) },
                            onLongClick = onLongPressApp?.let { { it(app) } },
                            isFocused = idx == effectiveFocusedIndex,
                            trailing = onLongPressApp?.let {
                                {
                                    // 46dp hit target (Android's recommended minimum, and then some)
                                    // decoupled from the glyph's own visual size via the Box wrapper
                                    // below — the previous 32dp box was both hard to hit AND hard to
                                    // see, and a near-miss tap fell through to the row's own onClick
                                    // and launched the app instead of opening the options menu.
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .clickable { onLongPressApp(app) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Rounded.MoreVert,
                                            contentDescription = stringResource(R.string.cd_search_app_options),
                                            tint = Color(0xFF8E95A3),
                                            modifier = Modifier.size(26.dp),
                                        )
                                    }
                                }
                            },
                        )
                    }
                    if (appResults.isNotEmpty() && contactResults.isNotEmpty()) ThinDivider()
                    contactResults.forEachIndexed { idx, contact ->
                        SearchResultRow(
                            leading = {
                                if (contact.photoUri != null) {
                                    AsyncImage(
                                        model = contact.photoUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(CircleShape),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2C3547)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = contact.displayName.firstOrNull()?.uppercase() ?: "?",
                                            color = Color(0xFF9EADB8),
                                            fontSize = 15.sp,
                                        )
                                    }
                                }
                            },
                            label = contact.displayName,
                            onClick = { onLaunchContact(contact) },
                            isFocused = (appResults.size + idx) == effectiveFocusedIndex,
                            trailing = if (contact.phoneNumber != null) {
                                {
                                    Icon(
                                        Icons.Rounded.Call,
                                        contentDescription = stringResource(R.string.cd_search_call_contact),
                                        tint = Color(0xFF6EDC8C),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .clickable { (onCallContact ?: { c -> callContact(context, c) })(contact) }
                                            .padding(7.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                    if ((appResults.isNotEmpty() || contactResults.isNotEmpty()) && settingsResults.isNotEmpty()) ThinDivider()
                    settingsResults.forEachIndexed { idx, entry ->
                        SearchResultRow(
                            leading = { RowIconBadge(Icons.Rounded.Settings, tint = Color(0xFF9EADB8)) },
                            label = entry.label,
                            onClick = { onLaunchSettings(entry.action, entry.fallbackAction) },
                            isFocused = (appResults.size + contactResults.size + idx) == effectiveFocusedIndex,
                        )
                    }
                    if (appResults.isEmpty() && contactResults.isEmpty() && settingsResults.isEmpty() && !isPrivateQuery) {
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

            // Bottom chrome: back (left) / mic (center, when available) / info (right) — matches
            // the reference design's layout, which has no separate bar behind this row at all: each
            // button is its own opaque chip (see CircleIconButton's own solid fill) floating
            // directly on the screen, with plain gaps between them — not a continuous toolbar.
            // All three share one size so back/info visually match mic instead of a smaller
            // default — proportional to screen width (0.0812 * refWidth ≈ 45dp on this project's
            // reference device) rather than a fixed dp, so it scales on other screen sizes.
            val bottomChromeButtonSize = LocalConfiguration.current.screenWidthDp.dp * 0.0812f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(onClick = onDismiss, size = bottomChromeButtonSize) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (showMic) {
                    CircleIconButton(onClick = onVoiceSearch, size = bottomChromeButtonSize) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = stringResource(R.string.cd_search_voice),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                CircleIconButton(onClick = { showHelp = true }, size = bottomChromeButtonSize) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = stringResource(R.string.cd_search_help),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
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
    isFocused: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isFocused) Color(0x336EA8D8) else Color.Transparent)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                },
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = if (isFocused) Color(0xFF84D5F6) else labelColor,
            fontSize = 15.sp,
            fontWeight = if (isFocused) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
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

/**
 * Quick Switch's "⋮" app-row menu. Visually matches the drawer/home AppContextMenu (same rows,
 * same bottom-sheet-style layout), but is NOT a real Compose `ModalBottomSheet` — that creates its
 * own Android Dialog window, which needs an Activity window token that Quick Switch's raw
 * WindowManager overlay doesn't have (same BadTokenException class of problem as [SearchHelpPanel]
 * below). This renders in-place inside the same window instead.
 *
 * Open/App info/Hide/Change icon all stay on top of whatever app Quick Switch was triggered from —
 * none of them need the launcher's own UI. Arrange is the one exception: it means dragging tiles
 * in the actual home/drawer grid, which only exists on the launcher, so [onArrange] switches to it.
 */
@Composable
internal fun QuickSwitchAppMenu(
    app: AppEntry,
    isHidden: Boolean,
    hasCustomIcon: Boolean,
    /** (folderId, label) pairs — same data `foldersForAddMenu()` returns for home/drawer's own menu. */
    folderChoices: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onInfo: () -> Unit,
    onHideToggle: () -> Unit,
    onArrange: () -> Unit,
    onChangeIcon: () -> Unit,
    onResetIcon: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onAddToFolder: (String) -> Unit,
) {
    // Grabs its own focus and consumes Back itself — otherwise the key event falls through to
    // UniversalSearchOverlay's own onPreviewKeyEvent underneath (still focused, since this menu
    // is just drawn on top of it, not a separate focus scope), whose own Back handling closes the
    // whole search overlay instead of just this menu.
    val menuFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { menuFocusRequester.requestFocus() }
    var newGroupInputOpen by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    val nameFieldFocusRequester = remember { FocusRequester() }
    LaunchedEffect(newGroupInputOpen) { if (newGroupInputOpen) nameFieldFocusRequester.requestFocus() }

    fun confirmNewGroup() {
        if (newGroupName.isNotBlank()) onCreateFolder(newGroupName)
        newGroupInputOpen = false
        newGroupName = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(onClick = onDismiss)
            .focusRequester(menuFocusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
                if (ev.key == Key.Back) {
                    if (newGroupInputOpen) newGroupInputOpen = false else onDismiss()
                    return@onPreviewKeyEvent true
                }
                if (newGroupInputOpen && (ev.key == Key.Enter || ev.key == Key.NumPadEnter)) {
                    confirmNewGroup()
                    return@onPreviewKeyEvent true
                }
                // Everything else — including normal typing while naming a new group — falls
                // through to whatever's actually focused underneath (e.g. the name TextField);
                // sibling composables elsewhere in the tree (the search overlay this menu sits on
                // top of) never see it, since key events only tunnel down the currently-focused
                // subtree, not unrelated siblings.
                false
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFF12161C))
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp, bottom = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x33FFFFFF)),
            )
            Text(
                app.label,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFEAF2F8),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            ThinDivider()
            Spacer(Modifier.height(4.dp))

            @Composable
            fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF8E95A3), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFEAF2F8))
                }
            }

            if (newGroupInputOpen) {
                // In-place name entry — a real AlertDialog would need an Activity window token
                // this overlay doesn't have (same class of problem as SearchHelpPanel below).
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.dialog_group_name_hint), color = Color(0xFF7A8899)) },
                        modifier = Modifier.weight(1f).focusRequester(nameFieldFocusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFEAF2F8),
                            unfocusedTextColor = Color(0xFFEAF2F8),
                            focusedBorderColor = Color(0xFF00B7FF),
                            unfocusedBorderColor = Color(0xFF2E3A4A),
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    CircleIconButton(onClick = ::confirmNewGroup) {
                        Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.action_new_group), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.action_open), onOpen)
                    if (!app.internal) {
                        MenuRow(Icons.Rounded.Info, stringResource(R.string.action_app_info), onInfo)
                    }
                    MenuRow(
                        if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        if (isHidden) stringResource(R.string.action_unhide) else stringResource(R.string.action_hide),
                        onHideToggle,
                    )
                    MenuRow(Icons.Rounded.SwapVert, stringResource(R.string.action_arrange), onArrange)
                    MenuRow(Icons.Rounded.Image, stringResource(R.string.action_change_icon), onChangeIcon)
                    if (hasCustomIcon) {
                        MenuRow(Icons.Rounded.SettingsBackupRestore, stringResource(R.string.action_reset_icon), onResetIcon)
                    }
                    if (!app.internal) {
                        MenuRow(Icons.Rounded.Folder, stringResource(R.string.action_new_group)) { newGroupInputOpen = true }
                        for ((folderId, label) in folderChoices) {
                            MenuRow(Icons.Rounded.BookmarkAdd, stringResource(R.string.action_add_to, label)) { onAddToFolder(folderId) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
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

/**
 * A dedicated, simple grid of just the user's hidden apps — reached via Universal Search's
 * "Show Hidden Apps" row when the query is "private". Deliberately plain (no search-within,
 * no reordering) since it's a rarely-visited utility view, not a primary surface.
 */
@Composable
internal fun HiddenAppsOverlay(
    allApps: List<AppEntry>,
    hiddenPackages: Set<String>,
    onDismiss: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
) {
    val hidden = remember(allApps, hiddenPackages) {
        allApps.filter { it.packageName in hiddenPackages }.sortedBy { it.label.lowercase() }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF00E1116)),
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
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.search_show_hidden_apps),
                    color = Color.White,
                    fontSize = 17.sp,
                )
            }
            if (hidden.isEmpty()) {
                Text(
                    text = stringResource(R.string.drawer_no_apps_found),
                    color = Color(0xFF8E95A3),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                ) {
                    items(hidden, key = { it.packageName }) { app ->
                        Column(
                            modifier = Modifier
                                .padding(6.dp)
                                .combinedClickable(
                                    onClick = { onLaunchApp(app.packageName) },
                                    onLongClick = { onLongPressApp(app) },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = app.label,
                                color = Color(0xFFEAF2F8),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun openContact(context: Context, contact: ContactResult) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, contact.lookupUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** ACTION_DIAL (not ACTION_CALL) — opens the default dialer pre-filled, no CALL_PHONE permission
 *  needed, and the user still has to tap the actual call button themselves. Fallback for hosts
 *  with no Activity to request CALL_PHONE from (Quick Switch). */
internal fun callContact(context: Context, contact: ContactResult) {
    val number = contact.phoneNumber ?: return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** ACTION_CALL — places the call directly (requires CALL_PHONE, already granted by the caller).
 *  Deliberately a plain implicit intent (no target package) so that if more than one app can
 *  handle it, Android shows its own "Just once / Always" chooser and remembers an "Always" pick
 *  as the default handler going forward — standard OS behavior, no extra code needed for it. */
internal fun placeCall(context: Context, number: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** For hosts with a real Activity (home page, drawer) — requests CALL_PHONE on first use if
 *  needed, then places the call directly via [placeCall]. Pass the result as [UniversalSearchOverlay]'s
 *  `onCallContact`. Not usable from Quick Switch's tokenless overlay window (no Activity to
 *  request a permission from) — that surface keeps the [callContact] ACTION_DIAL fallback instead. */
@Composable
internal fun rememberCallContactHandler(context: Context): (ContactResult) -> Unit {
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) pendingNumber?.let { placeCall(context, it) }
        pendingNumber = null
    }
    return { contact ->
        val number = contact.phoneNumber
        if (number != null) {
            if (contactsCallPermissionGranted(context)) {
                placeCall(context, number)
            } else {
                pendingNumber = number
                permissionLauncher.launch(android.Manifest.permission.CALL_PHONE)
            }
        }
    }
}

private fun contactsCallPermissionGranted(context: Context): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CALL_PHONE,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

internal data class ContactResult(
    val id: Long,
    val displayName: String,
    val photoUri: String?,
    val lookupKey: String,
    val phoneNumber: String?,
) {
    /** For `Intent(ACTION_VIEW, ...)` to open this contact's own detail page. */
    val lookupUri: Uri get() = android.provider.ContactsContract.Contacts.getLookupUri(id, lookupKey)
}

internal fun contactsPermissionGranted(context: Context): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.READ_CONTACTS,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

internal suspend fun matchContacts(context: Context, query: String): List<ContactResult> {
    val trimmed = query.trim()
    if (trimmed.length < 2) return emptyList()
    return withContext(Dispatchers.IO) {
        val results = mutableListOf<ContactResult>()
        val projection = arrayOf(
            android.provider.ContactsContract.Contacts._ID,
            android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            android.provider.ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            android.provider.ContactsContract.Contacts.LOOKUP_KEY,
        )
        runCatching {
            context.contentResolver.query(
                android.provider.ContactsContract.Contacts.CONTENT_URI,
                projection,
                "${android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?",
                arrayOf("%$trimmed%"),
                "${android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC LIMIT 5",
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIdx = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                val lookupIdx = cursor.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.LOOKUP_KEY)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    results.add(
                        ContactResult(
                            id = id,
                            displayName = cursor.getString(nameIdx) ?: continue,
                            photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null,
                            lookupKey = cursor.getString(lookupIdx) ?: continue,
                            // HAS_PHONE_NUMBER proved unreliable as a pre-check on this device/ROM
                            // (contacts that visibly have a number were still gated out) — just
                            // always attempt the lookup and let it come back null naturally.
                            phoneNumber = fetchPrimaryPhoneNumber(context, id),
                        ),
                    )
                }
            }
        }
        results
    }
}

private fun fetchPrimaryPhoneNumber(context: Context, contactId: Long): String? = runCatching {
    context.contentResolver.query(
        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null,
    )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
}.getOrNull()
