@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.zeno.classiclauncher.nlauncher.ui

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.KeyEvent as AndroidKeyEvent
import android.view.MotionEvent
import android.widget.Toast
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.AddAlarm
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.FilterBAndW
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.zeno.classiclauncher.nlauncher.backup.SettingsDownloads
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zeno.classiclauncher.nlauncher.glance.GlanceDateWeatherEventsView
import com.zeno.classiclauncher.nlauncher.glance.GlanceStripPreferences
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions
import com.zeno.classiclauncher.nlauncher.apps.ToggleResult
import com.zeno.classiclauncher.nlauncher.apps.parseHomeShortcutToken
import com.zeno.classiclauncher.native.ui.WallpaperSourceOverlay
import com.zeno.classiclauncher.native.ui.WallpaperSourceSub
import com.zeno.classiclauncher.nlauncher.badges.AppIconWithBadge
import com.zeno.classiclauncher.nlauncher.badges.BadgeNotificationListener
import com.zeno.classiclauncher.nlauncher.power.SleepManager
import com.zeno.classiclauncher.nlauncher.folders.DrawerGridCell
import com.zeno.classiclauncher.nlauncher.folders.FolderIds
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette
import com.zeno.classiclauncher.nlauncher.prefs.GridPreset
import com.zeno.classiclauncher.nlauncher.prefs.GlanceWeatherUnit
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroup
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroupIds
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroupSide
import com.zeno.classiclauncher.nlauncher.prefs.STRIP_TOTAL_SLOTS
import com.zeno.classiclauncher.nlauncher.prefs.canAddHomeStripItem
import com.zeno.classiclauncher.nlauncher.prefs.effectiveHomeStripSlotOrder
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefs
import com.zeno.classiclauncher.nlauncher.prefs.AppIconShape
import com.zeno.classiclauncher.nlauncher.prefs.DockIconStyle
import com.zeno.classiclauncher.nlauncher.prefs.SecondShortcutTarget
import com.zeno.classiclauncher.nlauncher.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Shape

/**
 * Max ms between consecutive DPAD/trackpad steps to treat the later step as "hard"
 * (page change, dock, home). Must not swallow moves — only edge/zone logic; see AppDrawer key handler.
 */
/**
 * Q20 trackpad events at ≤120ms apart are treated as "hard" (fast intentional swipe).
 * Wider than 72ms so moderate-speed swipes still trigger page changes reliably.
 */
private const val NAV_FRAME_MS = 120L
/**
 * Minimum ms between accepted DPAD cursor steps — limits runaway cursor on fast swipes.
 * Zone/page transitions bypass this. 40ms ≈ 25 steps/sec max.
 */
private const val NAV_MOVE_MIN_MS = 40L
private fun drawerCellForHomeStripToken(
    token: String,
    gridCells: List<DrawerGridCell>,
    allApps: List<AppEntry>,
): DrawerGridCell? {
    if (FolderIds.isFolderId(token)) {
        return gridCells.filterIsInstance<DrawerGridCell.Folder>().find { it.id == token }
    }
    val (pkg, _) = parseHomeShortcutToken(token)
    gridCells.filterIsInstance<DrawerGridCell.App>().find { it.entry.packageName == pkg }?.let { return it }
    val entry = allApps.find { it.packageName == pkg } ?: return null
    return DrawerGridCell.App(entry)
}

private const val HOME_WIDGET_HOST_ID = 7777
private val HOME_SHORTCUT_ICON_DP = 52.dp
private val HOME_SHORTCUT_FALLBACK_ICON_DP = 48.dp
private val HOME_STRIP_LABEL_COLOR = Color(0xFFE8EEF7)
/** Strip captions under shortcuts and home groups (was 11sp; +10%). */
private val HOME_STRIP_LABEL_FONT_SP = 12.5.sp
private val HOME_STRIP_LABEL_LINE_SP = 16.sp
/** Space between the 48dp icon tile and the caption (was 4.dp; more readable). */
private val HOME_STRIP_ICON_LABEL_GAP = 8.dp
/** Centre shortcuts: gap between tiles (matches last shipped commit). */
private val HOME_STRIP_SHORTCUT_GAP = 8.dp

private val OUTLINE_OFFSETS = arrayOf(
    Offset(-0.8f, -0.8f),
    Offset(0.8f, -0.8f),
    Offset(0.8f, 0.8f),
    Offset(-0.8f, 0.8f),
)
private val MAIN_SHADOW_OFFSET = Offset(0f, 1.5f)

private fun iconMaskShape(shape: AppIconShape): Shape = when (shape) {
    AppIconShape.ROUNDED -> RoundedCornerShape(14.dp)
    AppIconShape.SQUIRCLE -> RoundedCornerShape(20.dp)
    AppIconShape.CIRCLE -> CircleShape
    AppIconShape.SOFT_SQUARE -> RoundedCornerShape(9.dp)
}

private fun appIconShapeLabel(shape: AppIconShape): String = when (shape) {
    AppIconShape.ROUNDED -> "Rounded"
    AppIconShape.SQUIRCLE -> "Squircle"
    AppIconShape.CIRCLE -> "Circle"
    AppIconShape.SOFT_SQUARE -> "Soft square"
}

private fun dockIconStyleLabel(style: DockIconStyle): String = when (style) {
    DockIconStyle.MONOCHROME -> "Monochrome dock style"
    DockIconStyle.APP -> "Original app icon"
}

private fun dockMonochromeModel(iconModel: Any?, tint: Color): Any? {
    if (iconModel !is Drawable) return iconModel
    val copy = iconModel.constantState?.newDrawable()?.mutate() ?: iconModel.mutate()
    copy.setTint(tint.toArgb())
    return copy
}

internal fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val flat = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ) ?: return false
    val component = ComponentName(context, BadgeNotificationListener::class.java)
    val full = component.flattenToString()
    val short = component.flattenToShortString()
    return flat.split(':').any { segment ->
        segment == full || segment == short
    }
}

private data class PermRuntime(
    val notificationAccess: Boolean,
    val deviceAdmin: Boolean,
    val location: Boolean,
    val calendar: Boolean,
)

private fun computePermRuntime(context: android.content.Context): PermRuntime =
    PermRuntime(
        notificationAccess = isNotificationListenerEnabled(context),
        deviceAdmin = SleepManager.isAdminActive(context),
        location = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED,
        calendar = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED,
    )

private fun missingPermissionCount(prefs: LauncherPrefs, r: PermRuntime): Int {
    var n = 0
    if (prefs.notificationBadgesEnabled && !r.notificationAccess) n++
    if (prefs.doubleTapToSleepEnabled && !r.deviceAdmin) n++
    if (prefs.glanceEnabled && !r.location) n++
    if (prefs.glanceEnabled && prefs.glanceShowCalendar && !r.calendar) n++
    return n
}

/** First letter uppercase; [KeyboardCapitalization] is often ignored by OEM keyboards. */
private fun capitalizeFirstLetterForGroupInput(raw: String): String =
    raw.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
    }

private data class OpenFolderState(
    val id: String,
    val members: List<AppEntry>,
    val title: String,
)

private enum class HomeNavArea { Strip, Dock }

@Composable
fun LauncherScreen(
    vm: LauncherViewModel = viewModel(),
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    val prefs by vm.prefs.collectAsState()
    val allApps by vm.apps.collectAsState()
    val gridCells by vm.filteredGridCells.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val reorderMode by vm.isReorderMode.collectAsState()
    val moving by vm.moving.collectAsState()
    val hasUnreadMail by vm.hasUnreadMail.collectAsState()
    val hasUnreadSms by vm.hasUnreadSms.collectAsState()
    val hasUnreadWhatsApp by vm.hasUnreadWhatsApp.collectAsState()
    val unreadPackages by vm.packagesWithUnread.collectAsState()
    val sortByUsage by vm.sortByUsage.collectAsState()
    val newAppAddedToast by vm.newAppAddedToast.collectAsState()
    val themePalette = remember(prefs.themeJson) { LauncherThemePalette.fromJson(prefs.themeJson) }
    fun normalizedGroupName(raw: String): String =
        raw.trim().ifBlank { "Group" }.lowercase(Locale.getDefault())
    val existingGroupNamesNormalized = remember(
        prefs.homeGroups,
        prefs.folderContents,
        prefs.folderNames,
        allApps,
    ) {
        val byPkg = allApps.associateBy { it.packageName }
        val drawerGroupNames = prefs.folderContents.mapNotNull { (id, members) ->
            if (members.isEmpty()) null
            else {
                prefs.folderNames[id]?.trim()?.takeIf { it.isNotEmpty() }
                    ?: (members.firstNotNullOfOrNull { pkg -> byPkg[pkg]?.label } ?: "Folder")
            }
        }
        (prefs.homeGroups.map { it.title } + drawerGroupNames)
            .map { normalizedGroupName(it) }
            .toSet()
    }
    fun groupNameExists(raw: String): Boolean = normalizedGroupName(raw) in existingGroupNamesNormalized
    fun appIconFor(pkg: String): Any? = allApps.find { it.packageName == pkg }?.icon
    fun keepEnvelopeForMail(pkg: String): Boolean {
        if (pkg.isBlank()) return true
        val p = pkg.lowercase()
        val knownMailPkgs = setOf(
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.blackberry.hub",
            "com.android.email",
            "com.yahoo.mobile.client.android.mail",
            "com.samsung.android.email.provider",
        )
        return p in knownMailPkgs || p.contains("mail") || p.contains("email") || p.contains("hub")
    }
    fun keepEnvelopeForSecondShortcut(pkg: String): Boolean {
        if (pkg.isBlank()) return true
        val p = pkg.lowercase()
        return p == "com.whatsapp" ||
            p.contains("message") ||
            p.contains("messaging") ||
            p.contains("sms") ||
            p.contains("mms")
    }
    val dockStartIconModel = remember(prefs.dockMailPackage, allApps) {
        val pkg = prefs.dockMailPackage.trim()
        if (keepEnvelopeForMail(pkg)) null else appIconFor(pkg)
    }
    val dockMiddleIconModel = remember(prefs.dockSecondPackage, allApps) {
        val pkg = prefs.dockSecondPackage.trim()
        if (keepEnvelopeForSecondShortcut(pkg) || pkg == "com.apple.android.music") null else appIconFor(pkg)
    }
    val secondDockFallbackResId = remember(prefs.dockSecondPackage, prefs.secondShortcutTarget) {
        val pkg = prefs.dockSecondPackage.trim()
        when {
            pkg == "com.apple.android.music" -> R.drawable.ic_dock_apple_music
            pkg == "com.whatsapp" || (pkg.isEmpty() && prefs.secondShortcutTarget == SecondShortcutTarget.WHATSAPP) -> R.drawable.ic_dock_whatsapp
            else -> null
        }
    }
    val thirdDockFallbackResId = remember(prefs.dockCameraPackage) {
        when (prefs.dockCameraPackage.trim()) {
            "com.spotify.music" -> R.drawable.ic_dock_spotify
            "us.zoom.videomeetings" -> R.drawable.ic_dock_zoom
            "com.apple.android.music" -> R.drawable.ic_dock_apple_music
            else -> null
        }
    }
    val dockEndIconModel = remember(prefs.dockCameraPackage, allApps) {
        val pkg = prefs.dockCameraPackage.trim()
        if (pkg.isEmpty() || thirdDockFallbackResId != null) null else appIconFor(pkg)
    }
    val classicMode = prefs.classicMode
    val homeStripNavEligible = !classicMode && prefs.homeStripEnabled && (
        (prefs.showHomeGroups && prefs.homeGroups.isNotEmpty()) ||
            (prefs.showShortcutApps && prefs.homeShortcutPackages.isNotEmpty())
        )
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { if (classicMode) 1 else 2 })
    var showSettings by remember { mutableStateOf(false) }
    var showPermissionsSettings by remember { mutableStateOf(false) }
    var showDockSlotPicker by remember { mutableStateOf<DockSlot?>(null) }
    var dockQuickActionSlot by remember { mutableStateOf<DockSlot?>(null) }
    var showGlanceSettings by remember { mutableStateOf(false) }
    var showHomeGroupsSettings by remember { mutableStateOf(false) }
    var showGestureSettings by remember { mutableStateOf(false) }
    var showAppDrawerBadges by remember { mutableStateOf(false) }
    var showIconAppearanceSettings by remember { mutableStateOf(false) }
    var showHomeActions by remember { mutableStateOf(false) }
    var showPinAppToHome by remember { mutableStateOf(false) }
    var showNewHomeGroupDialog by remember { mutableStateOf(false) }
    var newHomeGroupName by remember { mutableStateOf("") }
    val newHomeGroupFocusRequester = remember { FocusRequester() }
    /** In-app wallpaper source list (replaces jumping straight to system [Intent.ACTION_SET_WALLPAPER]). */
    var showWallpaperSourceOverlay by remember { mutableStateOf(false) }
    var wallpaperSourceSub by remember { mutableStateOf(WallpaperSourceSub.List) }
    var showQuickSettingsOverlay by remember { mutableStateOf(false) }
    var showSpotlightOverlay by remember { mutableStateOf(false) }
    var spotlightInitialQuery by remember { mutableStateOf("") }
    val appWidgetHost = remember(context) { AppWidgetHost(context, HOME_WIDGET_HOST_ID) }
    var homeWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(appWidgetHost) {
        appWidgetHost.startListening()
        onDispose { runCatching { appWidgetHost.stopListening() } }
    }
    val pickWidgetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val requestedId = pendingWidgetId
        pendingWidgetId = null
        val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, requestedId ?: -1)
            ?: (requestedId ?: -1)
        if (result.resultCode != Activity.RESULT_OK || widgetId == -1) {
            if (widgetId != -1) runCatching { appWidgetHost.deleteAppWidgetId(widgetId) }
            return@rememberLauncherForActivityResult
        }
        homeWidgetId?.let { oldId ->
            if (oldId != widgetId) runCatching { appWidgetHost.deleteAppWidgetId(oldId) }
        }
        homeWidgetId = widgetId
    }
    var showAppMenu by remember { mutableStateOf<AppEntry?>(null) }
    /** Storage token (`pkg` or `pkg#id`) when the app menu was opened from the home shortcut strip. */
    var homeShortcutMenuToken by remember { mutableStateOf<String?>(null) }
    var openFolder by remember { mutableStateOf<OpenFolderState?>(null) }
    /** App-drawer folder: long-press menu (Open / Rename / Reorder / Delete). */
    var drawerFolderMenu by remember { mutableStateOf<DrawerGridCell.Folder?>(null) }
    var openHomeGroup by remember { mutableStateOf<OpenFolderState?>(null) }
    /** Home-group long-press context menu target. */
    var showHomeGroupMenu by remember { mutableStateOf<HomeGroup?>(null) }
    LaunchedEffect(openFolder?.id, prefs.folderContents, prefs.folderNames, allApps) {
        val cur = openFolder ?: return@LaunchedEffect
        val pkgs = prefs.folderContents[cur.id]
        if (pkgs == null) {
            openFolder = null
            return@LaunchedEffect
        }
        val byPkg = allApps.associateBy { it.packageName }
        val newMembers = pkgs.mapNotNull { byPkg[it] }
        val custom = prefs.folderNames[cur.id]?.trim()?.takeIf { it.isNotEmpty() }
        val title = custom ?: (newMembers.firstOrNull()?.label ?: cur.title)
        val newPkgs = newMembers.map { it.packageName }
        val curPkgs = cur.members.map { it.packageName }
        if (newPkgs == curPkgs && title == cur.title) return@LaunchedEffect
        openFolder = cur.copy(members = newMembers, title = title)
    }

    LaunchedEffect(openHomeGroup?.id, prefs.homeGroups, allApps) {
        val cur = openHomeGroup ?: return@LaunchedEffect
        val g = prefs.homeGroups.find { it.id == cur.id } ?: run {
            openHomeGroup = null
            return@LaunchedEffect
        }
        val byPkg = allApps.associateBy { it.packageName }
        val newMembers = g.packageNames.mapNotNull { byPkg[it] }
        val newPkgs = newMembers.map { it.packageName }
        val curPkgs = cur.members.map { it.packageName }
        if (newPkgs == curPkgs && g.title == cur.title) return@LaunchedEffect
        openHomeGroup = cur.copy(members = newMembers, title = g.title)
    }

    LaunchedEffect(newAppAddedToast) {
        val msg = newAppAddedToast ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        vm.consumeNewAppAddedToast()
    }

    val scope = rememberCoroutineScope()
    val homeStripSnackbarHostState = remember { SnackbarHostState() }
    val homeFocusRequester = remember { FocusRequester() }
    var drawerPageIndex by remember { mutableStateOf(0) }
    var requestedDrawerPage by remember { mutableStateOf(-1) }
    var appMenuFromHomeShortcut by remember { mutableStateOf(false) }
    var dockFocused by remember { mutableStateOf(false) }
    var dockFocusIndex by remember { mutableStateOf(0) }
    var homeStripFocused by remember { mutableStateOf(false) }
    var homeStripFocusIndex by remember { mutableStateOf(0) }

    var permRuntime by remember { mutableStateOf(computePermRuntime(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permRuntime = computePermRuntime(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Pulling the system notification / QS shade while our overlay is open: dismiss launcher QS so both aren’t stacked.
    DisposableEffect(showQuickSettingsOverlay) {
        if (!showQuickSettingsOverlay) {
            return@DisposableEffect onDispose { }
        }
        val activity = context.findHostActivity() ?: return@DisposableEffect onDispose { }
        val decor = activity.window.decorView
        val slopPx = (2f * context.resources.displayMetrics.density).toInt().coerceAtLeast(8)
        var layoutPass = 0
        var baselineStatusTop: Int? = null
        val layoutListener =
            object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val wi = ViewCompat.getRootWindowInsets(decor) ?: return
                    if (!wi.isVisible(WindowInsetsCompat.Type.statusBars())) {
                        if (baselineStatusTop != null) showQuickSettingsOverlay = false
                        return
                    }
                    val top = wi.getInsets(WindowInsetsCompat.Type.statusBars()).top
                    layoutPass++
                    if (layoutPass <= 2) {
                        baselineStatusTop = top
                        return
                    }
                    val b = baselineStatusTop ?: return
                    if (abs(top - b) > slopPx) {
                        showQuickSettingsOverlay = false
                    }
                }
            }
        decor.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        onDispose {
            decor.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
    }

    // Consume back for overlays / drawer / search; on home (page 0) with nothing open, do nothing — system volume
    // and recents are handled elsewhere; no moveTaskToBack (avoids feeling like “recent apps”).
    BackHandler(enabled = true) {
        when {
            showQuickSettingsOverlay -> showQuickSettingsOverlay = false
            showWallpaperSourceOverlay -> {
                when (wallpaperSourceSub) {
                    WallpaperSourceSub.ZenoGrid -> wallpaperSourceSub = WallpaperSourceSub.List
                    WallpaperSourceSub.List -> {
                        showWallpaperSourceOverlay = false
                        wallpaperSourceSub = WallpaperSourceSub.List
                    }
                }
            }
            showAppMenu != null -> {
                showAppMenu = null
                appMenuFromHomeShortcut = false
                homeShortcutMenuToken = null
            }
            drawerFolderMenu != null -> drawerFolderMenu = null
            dockQuickActionSlot != null -> dockQuickActionSlot = null
            showDockSlotPicker != null -> showDockSlotPicker = null
            showPermissionsSettings -> showPermissionsSettings = false
            showGestureSettings -> showGestureSettings = false
            showAppDrawerBadges -> showAppDrawerBadges = false
            showIconAppearanceSettings -> showIconAppearanceSettings = false
            showHomeGroupsSettings -> showHomeGroupsSettings = false
            showGlanceSettings -> showGlanceSettings = false
            showSettings -> showSettings = false
            showHomeGroupMenu != null -> showHomeGroupMenu = null
            openHomeGroup != null -> openHomeGroup = null
            openFolder != null -> openFolder = null
            showHomeActions -> showHomeActions = false
            showPinAppToHome -> showPinAppToHome = false
            reorderMode -> vm.toggleReorderMode()
            searchQuery.isNotEmpty() -> vm.setSearchQuery("")
            !classicMode && pagerState.currentPage != 0 -> scope.launch { pagerState.animateScrollToPage(0) }
            else -> Unit
        }
    }

    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage, classicMode) {
        if (!classicMode && pagerState.currentPage == 0) {
            homeFocusRequester.requestFocus()
            // Avoid stale drawer page scrub requests when coming back to home.
            requestedDrawerPage = -1
        }
    }

    // End-call / red button: navigated via Activity dispatchKeyEvent → ViewModel → here.
    val navigateHomeEvent by vm.navigateHomeEvent.collectAsState()
    androidx.compose.runtime.LaunchedEffect(navigateHomeEvent) {
        if (navigateHomeEvent > 0) pagerState.animateScrollToPage(0)
    }

    val dismissLauncherQsEvent by vm.dismissLauncherQsEvent.collectAsState()
    androidx.compose.runtime.LaunchedEffect(dismissLauncherQsEvent) {
        if (dismissLauncherQsEvent > 0) showQuickSettingsOverlay = false
    }

    // Dock shows which app-drawer page is active; trackpad flings can move the outer home/drawer pager
    // in one motion without intermediate inner pager steps, so drawerPageIndex may still be the last
    // drawer page while we're on home — show page 0 in the indicator whenever home is visible.
    val drawerPageCountForDock by remember {
        derivedStateOf {
            val n = prefs.gridPreset.rows * prefs.gridPreset.cols
            ((gridCells.size + n - 1) / n).coerceAtLeast(1)
        }
    }
    val dockPageIndexForDisplay by remember(classicMode) {
        derivedStateOf {
            if (!classicMode && pagerState.currentPage == 0) 0
            else drawerPageIndex.coerceIn(0, (drawerPageCountForDock - 1).coerceAtLeast(0))
        }
    }

    // Wallpaper: Theme.ClassicLauncher + FLAG_SHOW_WALLPAPER (same idea as Flutter transparent MaterialApp).
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Do not add top Spacer here: HomePage uses statusBarsPadding(); an extra inset stacked a large gap under the status bar.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                // Keep the drawer composed when on home so inner pager state & nested scroll behave better
                // with trackpad (BlackBerry) — default 0 can drop the drawer subtree entirely.
                beyondViewportPageCount = 1,
            ) { page ->
                when {
                    !classicMode && page == 0 -> HomePage(
                        focusRequester = homeFocusRequester,
                        homeActive = pagerState.currentPage == 0,
                        onOpenQuickSettings = {
                            if (prefs.customQuickSettingsEnabled) showQuickSettingsOverlay = true
                        },
                        onOpenAppDrawer = {
                            if (searchQuery.isNotEmpty()) vm.setSearchQuery("")
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        homeStripCount = if (homeStripNavEligible) STRIP_TOTAL_SLOTS else 0,
                        onActivateHomeStripIndex = { idx ->
                            val tok = prefs.effectiveHomeStripSlotOrder().getOrNull(idx)
                            if (tok != null) {
                                when {
                                    HomeGroupIds.isHomeGroupId(tok) && prefs.showHomeGroups -> {
                                        prefs.homeGroups.find { it.id == tok }?.let { g ->
                                            if (g.packageNames.isEmpty()) {
                                                scope.launch {
                                                    homeStripSnackbarHostState.showSnackbar(
                                                        message = "This group is empty. Long-press an app in the drawer to add it.",
                                                    )
                                                }
                                            } else {
                                                val members =
                                                    g.packageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                                                openHomeGroup = OpenFolderState(g.id, members, g.title)
                                            }
                                        }
                                    }
                                    prefs.showShortcutApps -> vm.launchHomeShortcutFromToken(tok)
                                }
                            }
                        },
                        onHomeDockFocusChanged = { focused, idx ->
                            dockFocused = focused
                            dockFocusIndex = idx
                            if (focused) {
                                homeStripFocused = false
                                homeStripFocusIndex = -1
                            }
                        },
                        onHomeStripFocusChanged = { focused, idx ->
                            homeStripFocused = focused
                            homeStripFocusIndex = idx
                            if (focused) {
                                dockFocused = false
                                dockFocusIndex = -1
                            }
                        },
                        onHomeDockActivate = { idx ->
                            if (classicMode) {
                                when (idx) {
                                    0 -> vm.launchFromDock(DockSlot.Mail)
                                    1 -> vm.launchFromDock(DockSlot.Camera)
                                }
                            } else {
                                when (idx) {
                                    0 -> vm.launchFromDock(DockSlot.Mail)
                                    1 -> scope.launch { pagerState.animateScrollToPage(0) }
                                    2 -> vm.launchFromDock(DockSlot.Shortcut)
                                    3 -> vm.launchFromDock(DockSlot.Camera)
                                }
                            }
                        },
                        classicMode = classicMode,
                        allApps = allApps,
                        hiddenPackages = prefs.hiddenPackages,
                        onLaunchApp = { pkg -> vm.launchApp(pkg) },
                        swipeUpPackage = prefs.swipeUpPackage,
                        doubleTapPackage = prefs.doubleTapPackage,
                        hapticsEnabled = prefs.hapticsEnabled,
                        hapticIntensity = prefs.hapticIntensity,
                        homeWidgetId = homeWidgetId,
                        appWidgetHost = appWidgetHost,
                        onLongPress = { showHomeActions = true },
                        doubleTapToSleepEnabled = prefs.doubleTapToSleepEnabled,
                        searchQuery = searchQuery,
                        onSearchQueryChange = vm::setSearchQuery,
                        appIconShape = prefs.appIconShape,
                        themePalette = themePalette,
                        glanceEnabled = prefs.glanceEnabled && !prefs.classicMode,
                        glanceStripPreferences = remember(
                            prefs.glanceShowFlashlight,
                            prefs.glanceShowBattery,
                            prefs.glanceShowCalendar,
                            prefs.glanceShowAlarm,
                            prefs.glanceWeatherUnit,
                        ) {
                            GlanceStripPreferences(
                                showFlashlight = prefs.glanceShowFlashlight,
                                showBattery = prefs.glanceShowBattery,
                                showCalendar = prefs.glanceShowCalendar,
                                showAlarm = prefs.glanceShowAlarm,
                                calendarLookAheadDays = 1,
                                weatherUseDeviceLocation = true,
                                weatherLatitude = null,
                                weatherLongitude = null,
                                weatherUseFahrenheit = prefs.glanceWeatherUnit == GlanceWeatherUnit.FAHRENHEIT,
                            )
                        },
                    )
                    (classicMode && page == 0) || page == 1 -> AppDrawer(
                        isActive = if (classicMode) pagerState.currentPage == 0 else pagerState.currentPage == 1,
                        classicDock = classicMode,
                        hapticsEnabled = prefs.hapticsEnabled,
                        hapticIntensity = prefs.hapticIntensity,
                        gridPreset = prefs.gridPreset,
                        gridCells = gridCells,
                        unreadPackages = if (prefs.notificationBadgesEnabled) unreadPackages else emptySet(),
                        usageStats = emptyMap(),
                        sortByUsage = sortByUsage,
                        showIconNotifBadge = prefs.showIconNotifBadge,
                        onToggleSortByUsage = {
                            if (vm.hasUsagePermission()) {
                                vm.toggleSortByUsage()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Enable Usage Access for Zeno Classic in Settings",
                                    Toast.LENGTH_LONG,
                                ).show()
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        requestedPage = requestedDrawerPage,
                        searchQuery = searchQuery,
                        onSearchQueryChange = vm::setSearchQuery,
                        appIconShape = prefs.appIconShape,
                        showAppCardBackground = prefs.showAppCardBackground,
                        reorderMode = reorderMode,
                        movingSlotId = moving,
                        onToggleReorder = vm::toggleReorderMode,
                        onPageChange = { drawerPageIndex = it },
                        onDockFocusChanged = { focused, idx ->
                            dockFocused = focused
                            dockFocusIndex = idx
                        },
                        onDockActivate = { idx ->
                            if (classicMode) {
                                when (idx) {
                                    0 -> vm.launchFromDock(DockSlot.Mail)
                                    1 -> vm.launchFromDock(DockSlot.Camera)
                                }
                            } else {
                                when (idx) {
                                    0 -> vm.launchFromDock(DockSlot.Mail)
                                    1 -> scope.launch { pagerState.animateScrollToPage(0) }
                                    2 -> vm.launchFromDock(DockSlot.Shortcut)
                                    3 -> vm.launchFromDock(DockSlot.Camera)
                                }
                            }
                        },
                        onCellTap = { cell ->
                            when (cell) {
                                is DrawerGridCell.App -> {
                                    val app = cell.entry
                            if (app.packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) {
                                showSettings = true
                                return@AppDrawer
                            }
                            if (reorderMode) {
                                val m = moving
                                if (m == null) vm.startMove(app.packageName)
                                else {
                                    vm.moveTo(app.packageName)
                                    vm.clearMove()
                                }
                            } else {
                                if (searchQuery.isNotEmpty()) vm.setSearchQuery("")
                                vm.launchApp(app.packageName)
                                    }
                                }
                                is DrawerGridCell.Folder -> {
                                    if (reorderMode) {
                                        val m = moving
                                        if (m == null) vm.startMove(cell.id)
                                        else {
                                            vm.moveTo(cell.id)
                                            vm.clearMove()
                                        }
                                    } else {
                                        drawerFolderMenu = null
                                        openFolder = OpenFolderState(cell.id, cell.members, cell.displayTitle)
                                    }
                                }
                            }
                        },
                        onCellLongPress = { cell ->
                            when (cell) {
                                is DrawerGridCell.App -> {
                                    if (cell.entry.packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) {
                                        showSettings = true
                                    } else if (!reorderMode) {
                                        // Normal mode: show context menu
                                        drawerFolderMenu = null
                                        appMenuFromHomeShortcut = false
                                        homeShortcutMenuToken = null
                                        showAppMenu = cell.entry
                                    }
                                    // Reorder mode: drag gesture handles pickup + haptic; ignore long press here
                                }
                                is DrawerGridCell.Folder -> {
                                    if (reorderMode) {
                                        val m = moving
                                        if (m == null) vm.startMove(cell.id)
                                        else {
                                            vm.moveTo(cell.id)
                                            vm.clearMove()
                                        }
                                    } else {
                                        drawerFolderMenu = cell
                                    }
                                }
                            }
                        },
                        onStartMove = vm::startMove,
                        onReorderDrop = vm::finishReorderDrop,
                        onExitToHome = {
                            if (searchQuery.isNotEmpty()) vm.setSearchQuery("")
                            if (!classicMode) scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        themePalette = themePalette,
                    )
                }
            }

            // Flutter main.dart: search replaces the nav bar when typing (drawer page only; home page has its own overlay).
            if (searchQuery.isNotEmpty() && (classicMode || pagerState.currentPage != 0)) {
                DrawerSearchBar(
                    query = searchQuery,
                    onClear = { vm.setSearchQuery("") },
                )
            } else {
                if (pagerState.currentPage == 0 && homeStripNavEligible) {
                    val homeStripSlotTokens = remember(
                        prefs.homeStripSlots,
                        prefs.homeGroups,
                        prefs.homeShortcutPackages,
                        prefs.homeStripOrder,
                    ) {
                        prefs.effectiveHomeStripSlotOrder()
                    }
                    HomeShortcutStrip(
                        gridPreset = prefs.gridPreset,
                        gridCells = gridCells,
                        stripSlotTokens = homeStripSlotTokens,
                        showShortcutApps = prefs.showShortcutApps,
                        homeGroups = prefs.homeGroups,
                        showHomeGroups = prefs.showHomeGroups,
                        allApps = allApps,
                        unreadPackages = if (prefs.notificationBadgesEnabled) unreadPackages else emptySet(),
                        appIconShape = prefs.appIconShape,
                        showAppCardBackground = prefs.showAppCardBackground,
                        themePalette = themePalette,
                        focusedIndex = if (homeStripFocused) homeStripFocusIndex else null,
                        hapticsEnabled = prefs.hapticsEnabled,
                        hapticIntensity = prefs.hapticIntensity,
                        reorderMode = reorderMode,
                        movingSlotId = moving,
                        onEnterReorderMode = { if (!reorderMode) vm.toggleReorderMode() },
                        onExitReorderMode = { if (reorderMode) vm.toggleReorderMode() },
                        onStartStripMove = vm::startStripMove,
                        onFinishReorderDrop = vm::finishReorderDrop,
                        onClearMove = vm::clearMove,
                        onLaunchShortcut = vm::launchHomeShortcutFromToken,
                        onOpenHomeGroup = { g ->
                            if (g.packageNames.isEmpty()) {
                                scope.launch {
                                    homeStripSnackbarHostState.showSnackbar(
                                        message = "This group is empty. Long-press an app in the drawer to add it.",
                                    )
                                }
                            } else {
                                val members = g.packageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                                openHomeGroup = OpenFolderState(g.id, members, g.title)
                            }
                        },
                        onOpenDrawerFolder = { cell ->
                            openFolder = OpenFolderState(cell.id, cell.members, cell.displayTitle)
                        },
                        onShowSettings = { showSettings = true },
                        onDeleteHomeShortcut = vm::removeHomeShortcutToken,
                        onDeleteGroup = { g -> vm.deleteHomeGroup(g.id) },
                        onStripReorderTap = { token ->
                            val m = moving
                            if (m == null) vm.startStripMove(token)
                            else vm.finishReorderDrop(token)
                        },
                        onClearStripKeyboardFocus = {
                            homeStripFocused = false
                            homeStripFocusIndex = -1
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = Color(0x553D4B60),
                    )
                }
                Dock(
                    pageIndex = dockPageIndexForDisplay,
                    homeActive = !classicMode && pagerState.currentPage == 0,
                    onMail = { vm.launchFromDock(DockSlot.Mail) },
                    onShortcut = { vm.launchFromDock(DockSlot.Shortcut) },
                    onCamera = { vm.launchFromDock(DockSlot.Camera) },
                    onLongPressMail = { dockQuickActionSlot = DockSlot.Mail },
                    onLongPressShortcut = { dockQuickActionSlot = DockSlot.Shortcut },
                    onLongPressCamera = { dockQuickActionSlot = DockSlot.Camera },
                    onHome = {
                        if (!classicMode) scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    onScrubPage = { targetPage ->
                        requestedDrawerPage = targetPage
                        scope.launch {
                            if (classicMode) pagerState.animateScrollToPage(0)
                            else pagerState.animateScrollToPage(1)
                        }
                    },
                    drawerPageCount = drawerPageCountForDock,
                    mailHasUnread = hasUnreadMail && prefs.notificationBadgesEnabled,
                    shortcutHasUnread = !classicMode && prefs.notificationBadgesEnabled && when {
                        prefs.dockSecondPackage == "com.whatsapp" -> hasUnreadWhatsApp
                        prefs.dockSecondPackage.isNotEmpty() -> prefs.dockSecondPackage in unreadPackages
                        prefs.secondShortcutTarget == SecondShortcutTarget.WHATSAPP -> hasUnreadWhatsApp
                        else -> hasUnreadSms
                    },
                    showHomeButton = !classicMode,
                    showMessagesShortcut = !classicMode && prefs.dockSecondEnabled,
                    selectedTint = themePalette.dockSelected,
                    themePalette = themePalette,
                    dockStartIconModel = dockStartIconModel,
                    dockMiddleIconModel = dockMiddleIconModel,
                    secondDockFallbackResId = secondDockFallbackResId,
                    thirdDockFallbackResId = thirdDockFallbackResId,
                    dockEndIconModel = dockEndIconModel,
                    appIconShape = prefs.appIconShape,
                    dockIconStyle = prefs.dockIconStyle,
                    focused = dockFocused && (classicMode || pagerState.currentPage == 1 || pagerState.currentPage == 0),
                    focusedIndex = dockFocusIndex,
                )
            }
        }

        val selectedApp = showAppMenu
        if (selectedApp != null) {
            LaunchedEffect(selectedApp.packageName) { drawerFolderMenu = null }
            val canAddHomeShortcut =
                !appMenuFromHomeShortcut &&
                    selectedApp.packageName != AppsRepository.INTERNAL_SETTINGS_PACKAGE &&
                    prefs.canAddHomeStripItem() &&
                    prefs.homeShortcutPackages.none {
                        parseHomeShortcutToken(it).first == selectedApp.packageName
                    }
            val drawerFolderActionsEnabled =
                !appMenuFromHomeShortcut &&
                    selectedApp.packageName != AppsRepository.INTERNAL_SETTINGS_PACKAGE
            val drawerFolderChoices = vm.foldersForAddMenu()
            AppContextMenu(
                app = selectedApp,
                themePalette = themePalette,
                isHidden = prefs.hiddenPackages.contains(selectedApp.packageName),
                homeGroups = prefs.homeGroups,
                addHomeShortcutEnabled = canAddHomeShortcut,
                removeHomeShortcutEnabled = appMenuFromHomeShortcut,
                drawerFolderActionsEnabled = drawerFolderActionsEnabled,
                drawerFolders = drawerFolderChoices,
                onDismiss = {
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
                onLaunch = {
                    val t = homeShortcutMenuToken
                    if (t != null) vm.launchHomeShortcutFromToken(t) else vm.launchApp(selectedApp.packageName)
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
                onInfo = {
                    vm.openAppInfo(selectedApp.packageName)
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
                onHideToggle = {
                    vm.setHidden(selectedApp.packageName, !prefs.hiddenPackages.contains(selectedApp.packageName))
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
                onReorder = {
                    vm.toggleReorderMode()
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
                onAddHomeShortcut = {
                    vm.addHomeShortcut(selectedApp.packageName)
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
                onRemoveHomeShortcut = {
                    val t = homeShortcutMenuToken
                    if (t != null) vm.removeHomeShortcutToken(t)
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
                onAddToHomeGroup = { groupId ->
                    vm.addPackageToHomeGroup(selectedApp.packageName, groupId)
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
                onRemoveFromHomeGroup = { groupId ->
                    vm.removePackageFromHomeGroup(selectedApp.packageName, groupId)
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
                onCreateDrawerFolder = { title ->
                    if (groupNameExists(title)) {
                        Toast.makeText(context, "Groups already exist", Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        val visualIdx = gridCells.indexOfFirst { cell ->
                            cell is DrawerGridCell.App && cell.entry.packageName == selectedApp.packageName
                        }
                        vm.createFolderFromApp(selectedApp.packageName, title, visualIdx)
                        showAppMenu = null
                        appMenuFromHomeShortcut = false
                        homeShortcutMenuToken = null
                        true
                    }
                },
                onAddToDrawerFolder = { folderId ->
                    vm.addAppToFolder(selectedApp.packageName, folderId)
                    showAppMenu = null
                    appMenuFromHomeShortcut = false
                    homeShortcutMenuToken = null
                },
            )
        }

        val folderMenuCell = drawerFolderMenu
        if (folderMenuCell != null) {
            FolderDrawerContextMenu(
                folder = folderMenuCell,
                themePalette = themePalette,
                onDismiss = { drawerFolderMenu = null },
                onOpenFolder = {
                    openFolder = OpenFolderState(folderMenuCell.id, folderMenuCell.members, folderMenuCell.displayTitle)
                    drawerFolderMenu = null
                },
                onRenameFolder = { newTitle -> vm.renameFolder(folderMenuCell.id, newTitle) },
                onReorderApps = {
                    vm.toggleReorderMode()
                    drawerFolderMenu = null
                },
                onDeleteFolder = {
                    vm.dissolveFolder(folderMenuCell.id)
                    if (openFolder?.id == folderMenuCell.id) openFolder = null
                    drawerFolderMenu = null
                },
            )
        }

        val folderOpen = openFolder
        if (folderOpen != null) {
            HomeGroupFolderOverlay(
                groupTitle = folderOpen.title,
                members = folderOpen.members,
                onDismiss = { openFolder = null },
                onLaunchApp = { pkg ->
                    vm.launchApp(pkg)
                    openFolder = null
                },
                onRemoveFromGroup = { pkg ->
                    vm.removeAppFromFolder(pkg, folderOpen.id)
                    val updated = folderOpen.members.filter { it.packageName != pkg }
                    openFolder = if (updated.isEmpty()) null else folderOpen.copy(members = updated)
                },
                onRenameGroup = { newTitle ->
                    vm.renameFolder(folderOpen.id, newTitle)
                    openFolder = folderOpen.copy(title = newTitle.trim().ifBlank { "Folder" })
                },
                appIconShape = prefs.appIconShape,
                themePalette = themePalette,
                renameDialogTitle = "Rename folder",
                emptyStateMessage = "No apps in this folder.",
            )
        }

        val homeGroupOpen = openHomeGroup
        if (homeGroupOpen != null) {
            HomeGroupFolderOverlay(
                groupTitle = homeGroupOpen.title,
                members = homeGroupOpen.members,
                onDismiss = { openHomeGroup = null },
                onLaunchApp = { pkg ->
                    vm.launchApp(pkg)
                    openHomeGroup = null
                },
                onRemoveFromGroup = { pkg ->
                    vm.removePackageFromHomeGroup(pkg, homeGroupOpen.id)
                    val updated = homeGroupOpen.members.filter { it.packageName != pkg }
                    openHomeGroup = if (updated.isEmpty()) null else homeGroupOpen.copy(members = updated)
                },
                onRenameGroup = { newTitle ->
                    vm.renameHomeGroup(homeGroupOpen.id, newTitle)
                    openHomeGroup = homeGroupOpen.copy(title = newTitle.trim().ifBlank { "Group" })
                },
                appIconShape = prefs.appIconShape,
                themePalette = themePalette,
                renameDialogTitle = "Rename group",
                emptyStateMessage = "No apps yet — long-press an app in the drawer to add it here.",
            )
        }

        val homeGroupMenuTarget = showHomeGroupMenu
        if (homeGroupMenuTarget != null) {
            HomeGroupContextMenu(
                group = homeGroupMenuTarget,
                otherGroupExists = prefs.homeGroups.size > 1,
                themePalette = themePalette,
                onDismiss = { showHomeGroupMenu = null },
                onOpenGroup = {
                    if (homeGroupMenuTarget.packageNames.isEmpty()) {
                        scope.launch {
                            homeStripSnackbarHostState.showSnackbar(
                                message = "This group is empty. Long-press an app in the drawer to add it.",
                            )
                        }
                    } else {
                        val members =
                            homeGroupMenuTarget.packageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                        openHomeGroup = OpenFolderState(homeGroupMenuTarget.id, members, homeGroupMenuTarget.title)
                    }
                    showHomeGroupMenu = null
                },
                onRenameGroup = { newTitle ->
                    vm.renameHomeGroup(homeGroupMenuTarget.id, newTitle)
                    showHomeGroupMenu = null
                },
                onMoveSide = {
                    val newSide = if (homeGroupMenuTarget.side == HomeGroupSide.LEFT) HomeGroupSide.RIGHT else HomeGroupSide.LEFT
                    vm.setHomeGroupSide(homeGroupMenuTarget.id, newSide)
                    showHomeGroupMenu = null
                },
                onDeleteGroup = {
                    vm.deleteHomeGroup(homeGroupMenuTarget.id)
                    showHomeGroupMenu = null
                },
            )
        }

        // Settings keeps keyboard/trackpad focus; Key.Back on that node dismisses settings. When a sub-screen
        // is composed on top (permissions, glance, etc.), do not consume Back there — let BackHandler close the top overlay.
        val settingsStackedOverlayOpen =
            showPermissionsSettings || showGlanceSettings ||
                showHomeGroupsSettings || showDockSlotPicker != null || showWallpaperSourceOverlay ||
                showGestureSettings || showAppDrawerBadges || showIconAppearanceSettings

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(160)),
        ) {
            SettingsScreenOverlay(
                stackedChildOverlayOpen = settingsStackedOverlayOpen,
                gridPreset = prefs.gridPreset,
                hapticsEnabled = prefs.hapticsEnabled,
                dockMailBody = remember(prefs.dockMailPackage, allApps) {
                    if (prefs.dockMailPackage.isEmpty()) {
                        "Default app chooser for mail intents."
                    } else {
                        allApps.find { it.packageName == prefs.dockMailPackage }?.label ?: prefs.dockMailPackage
                    }
                },
                dockSecondBody = remember(prefs.dockSecondPackage, allApps) {
                    if (prefs.dockSecondPackage.isEmpty()) {
                        "Default messaging app."
                    } else {
                        allApps.find { it.packageName == prefs.dockSecondPackage }?.label ?: prefs.dockSecondPackage
                    }
                },
                dockThirdBody = remember(prefs.dockCameraPackage, allApps) {
                    if (prefs.dockCameraPackage.isEmpty()) {
                        "Default: camera app. Open to choose any installed app."
                    } else {
                        val label = allApps.find { it.packageName == prefs.dockCameraPackage }?.label
                        label ?: prefs.dockCameraPackage
                    }
                },
                onGridPreset = vm::setGridPreset,
                dockMailTitle = prefs.dockMailTitle,
                dockSecondTitle = prefs.dockSecondTitle,
                dockThirdTitle = prefs.dockThirdTitle,
                onOpenDockSlotPicker = { showDockSlotPicker = it },
                glanceSubtitle = remember(
                    prefs.glanceEnabled,
                    prefs.glanceShowCalendar,
                    prefs.glanceShowFlashlight,
                    prefs.glanceShowBattery,
                    prefs.glanceShowAlarm,
                ) {
                    if (!prefs.glanceEnabled) {
                        "Off"
                    } else {
                        buildList {
                            add("Date & weather")
                            if (prefs.glanceShowCalendar) add("calendar")
                            if (prefs.glanceShowFlashlight) add("flashlight")
                            if (prefs.glanceShowBattery) add("battery")
                            if (prefs.glanceShowAlarm) add("alarm")
                        }.joinToString(prefix = "On · ", separator = ", ")
                    }
                },
                onOpenGlanceSettings = { showGlanceSettings = true },
                homeStripEnabled = prefs.homeStripEnabled,
                onToggleHomeStrip = vm::setHomeStripEnabled,
                homeGroupsSubtitle = remember(prefs.homeGroups) {
                    when {
                        prefs.homeGroups.isEmpty() -> "No groups — long-press apps in drawer to add"
                        else -> prefs.homeGroups.joinToString(" · ") { it.title }
                    }
                },
                onOpenHomeGroupsSettings = { showHomeGroupsSettings = true },
                permissionsSubtitle = remember(prefs, permRuntime) {
                    val missing = missingPermissionCount(prefs, permRuntime)
                    when {
                        missing == 0 -> "All set for enabled features"
                        missing == 1 -> "1 missing — open to grant or turn off features"
                        else -> "$missing missing — open to grant or turn off features"
                    }
                },
                onOpenPermissionsSettings = { showPermissionsSettings = true },
                onSetWallpaper = {
                    wallpaperSourceSub = WallpaperSourceSub.List
                    showWallpaperSourceOverlay = true
                },
                onToggleHaptics = { vm.setHapticsEnabled(!prefs.hapticsEnabled) },
                hapticIntensity = prefs.hapticIntensity,
                onSetHapticIntensity = vm::setHapticIntensity,
                onExportBackup = vm::exportBackupJson,
                onImportBackup = vm::importBackupJson,
                onResetTheme = vm::resetTheme,
                themePalette = themePalette,
                onDismiss = { showSettings = false },
                gestureSubtitle = remember(
                    prefs.swipeUpPackage,
                    prefs.doubleTapPackage,
                    prefs.doubleTapToSleepEnabled,
                    prefs.customQuickSettingsEnabled,
                    allApps,
                ) {
                    val parts = buildList {
                        if (prefs.swipeUpPackage.isNotEmpty()) {
                            add("Swipe up: " + (allApps.find { it.packageName == prefs.swipeUpPackage }?.label ?: prefs.swipeUpPackage))
                        }
                        when {
                            prefs.doubleTapToSleepEnabled -> add("Double tap: Sleep")
                            prefs.doubleTapPackage.isNotEmpty() -> add("Double tap: " + (allApps.find { it.packageName == prefs.doubleTapPackage }?.label ?: prefs.doubleTapPackage))
                        }
                        if (prefs.customQuickSettingsEnabled) add("Swipe-down panel on")
                    }
                    if (parts.isEmpty()) "Not configured" else parts.joinToString(" · ")
                },
                onOpenGestureSettings = { showGestureSettings = true },
                drawerBadgesSubtitle = remember(prefs.showIconNotifBadge) {
                    buildList {
                        if (prefs.showIconNotifBadge) add("Notifications")
                    }.let { if (it.isEmpty()) "All off" else it.joinToString(", ") }
                },
                appIconShape = prefs.appIconShape,
                onSetAppIconShape = vm::setAppIconShape,
                dockIconStyle = prefs.dockIconStyle,
                onSetDockIconStyle = vm::setDockIconStyle,
                onOpenAppearanceSettings = { showIconAppearanceSettings = true },
                classicMode = prefs.classicMode,
                onToggleClassicMode = { vm.setClassicMode(!prefs.classicMode) },
                dockSecondEnabled = prefs.dockSecondEnabled,
                onToggleDockSecond = { vm.setDockSecondEnabled(!prefs.dockSecondEnabled) },
            )
        }

        if (showIconAppearanceSettings) {
            IconAppearanceSettingsOverlay(
                gridPreset = prefs.gridPreset,
                appIconShape = prefs.appIconShape,
                showAppCardBackground = prefs.showAppCardBackground,
                drawerBadgesSubtitle = remember(prefs.showIconNotifBadge) {
                    buildList {
                        if (prefs.showIconNotifBadge) add("Notifications")
                    }.let { if (it.isEmpty()) "All off" else it.joinToString(", ") }
                },
                onGridPreset = vm::setGridPreset,
                onSetAppIconShape = vm::setAppIconShape,
                onToggleAppCardBackground = { vm.setShowAppCardBackground(!prefs.showAppCardBackground) },
                onOpenDrawerBadges = { showAppDrawerBadges = true },
                themePalette = themePalette,
                onDismiss = { showIconAppearanceSettings = false },
            )
        }

        if (showAppDrawerBadges) {
            AppDrawerBadgesOverlay(
                showUsageStatsBadge = false,
                showIconNotifBadge = prefs.showIconNotifBadge,
                notificationAccessReady = prefs.notificationBadgesEnabled && permRuntime.notificationAccess,
                themePalette = themePalette,
                onDismiss = { showAppDrawerBadges = false },
                onShowUsageStatsBadgeChange = {},
                onShowIconNotifBadgeChange = vm::setShowIconNotifBadge,
            )
        }

        if (showGestureSettings) {
            GestureShortcutsOverlay(
                allApps = allApps,
                swipeUpPackage = prefs.swipeUpPackage,
                doubleTapPackage = prefs.doubleTapPackage,
                doubleTapToSleepEnabled = prefs.doubleTapToSleepEnabled,
                customQuickSettingsEnabled = prefs.customQuickSettingsEnabled,
                themePalette = themePalette,
                onDismiss = { showGestureSettings = false },
                onSetSwipeUp = vm::setSwipeUpPackage,
                onSetDoubleTap = vm::setDoubleTapPackage,
                onDoubleTapSleepChange = vm::setDoubleTapToSleepEnabled,
                onCustomQuickSettingsChange = vm::setCustomQuickSettingsEnabled,
            )
        }

        if (showPermissionsSettings) {
            PermissionsSettingsOverlay(
                prefs = prefs,
                themePalette = themePalette,
                onDismiss = {
                    showPermissionsSettings = false
                    permRuntime = computePermRuntime(context)
                },
                onNotificationBadgesEnabled = vm::setNotificationBadgesEnabled,
                onDoubleTapSleepEnabled = vm::setDoubleTapToSleepEnabled,
                onGlanceEnabled = vm::setGlanceEnabled,
                onGlanceShowCalendar = vm::setGlanceShowCalendar,
            )
        }

        if (showGlanceSettings) {
            GlanceSettingsOverlay(
                glanceEnabled = prefs.glanceEnabled,
                glanceShowFlashlight = prefs.glanceShowFlashlight,
                glanceShowBattery = prefs.glanceShowBattery,
                glanceShowCalendar = prefs.glanceShowCalendar,
                glanceShowAlarm = prefs.glanceShowAlarm,
                glanceWeatherUnit = prefs.glanceWeatherUnit,
                themePalette = themePalette,
                onGlanceEnabled = vm::setGlanceEnabled,
                onGlanceShowFlashlight = vm::setGlanceShowFlashlight,
                onGlanceShowBattery = vm::setGlanceShowBattery,
                onGlanceShowCalendar = vm::setGlanceShowCalendar,
                onGlanceShowAlarm = vm::setGlanceShowAlarm,
                onGlanceWeatherUnit = vm::setGlanceWeatherUnit,
                onDismiss = { showGlanceSettings = false },
            )
        }

        if (showHomeGroupsSettings) {
            HomeGroupsSettingsOverlay(
                groups = prefs.homeGroups,
                allApps = allApps,
                themePalette = themePalette,
                onCreateGroup = { name ->
                    if (groupNameExists(name)) {
                        Toast.makeText(context, "Groups already exist", Toast.LENGTH_SHORT).show()
                    } else {
                        vm.createHomeGroup(name)
                    }
                },
                onDeleteGroup = { id -> vm.deleteHomeGroup(id) },
                onDismiss = { showHomeGroupsSettings = false },
            )
        }

        val activeDockSlot = showDockSlotPicker
        if (activeDockSlot != null) {
            DockShortcutPickerOverlay(
                apps = allApps,
                themePalette = themePalette,
                slot = activeDockSlot,
                currentName = when (activeDockSlot) {
                    DockSlot.Mail -> prefs.dockMailTitle
                    DockSlot.Shortcut -> prefs.dockSecondTitle
                    DockSlot.Camera -> prefs.dockThirdTitle
                },
                onNameChange = { vm.setDockSlotTitle(activeDockSlot, it) },
                onSelect = { pkg ->
                    when (activeDockSlot) {
                        DockSlot.Mail -> vm.setDockMailPackage(pkg)
                        DockSlot.Shortcut -> vm.setDockSecondPackage(pkg)
                        DockSlot.Camera -> vm.setDockCameraPackage(pkg)
                    }
                    showDockSlotPicker = null
                },
                onUseDefault = {
                    when (activeDockSlot) {
                        DockSlot.Mail -> vm.setDockMailPackage("")
                        DockSlot.Shortcut -> vm.setDockSecondPackage("")
                        DockSlot.Camera -> vm.setDockCameraPackage("")
                    }
                    showDockSlotPicker = null
                },
                onDismiss = { showDockSlotPicker = null },
                dockSecondEnabled = prefs.dockSecondEnabled,
                onToggleDockSecond = { vm.setDockSecondEnabled(!prefs.dockSecondEnabled) },
            )
        }

        val activeQuickSlot = dockQuickActionSlot
        if (activeQuickSlot != null) {
            BackHandler { dockQuickActionSlot = null }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    ) { dockQuickActionSlot = null },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E2430),
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = when (activeQuickSlot) {
                                DockSlot.Mail -> prefs.dockMailTitle
                                DockSlot.Shortcut -> prefs.dockSecondTitle
                                DockSlot.Camera -> prefs.dockThirdTitle
                            },
                            color = Color(0xFF8E95A3),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        // Change shortcut
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dockQuickActionSlot = null
                                    showDockSlotPicker = activeQuickSlot
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(14.dp))
                            Text("Change shortcut", color = Color.White, fontSize = 15.sp)
                        }
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        // Remove / hide
                        if (activeQuickSlot == DockSlot.Shortcut) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.setDockSecondEnabled(false)
                                        dockQuickActionSlot = null
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(14.dp))
                                Text("Hide from dock", color = Color(0xFFFF6B6B), fontSize = 15.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (activeQuickSlot) {
                                            DockSlot.Mail -> vm.setDockMailPackage("")
                                            DockSlot.Camera -> vm.setDockCameraPackage("")
                                            else -> Unit
                                        }
                                        dockQuickActionSlot = null
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(14.dp))
                                Text("Reset to default", color = Color(0xFFFF6B6B), fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        if (showWallpaperSourceOverlay) {
            WallpaperSourceOverlay(
                themePalette = themePalette,
                subView = wallpaperSourceSub,
                onSubViewChange = { wallpaperSourceSub = it },
                onDismiss = {
                    showWallpaperSourceOverlay = false
                    wallpaperSourceSub = WallpaperSourceSub.List
                },
            )
        }

        if (showQuickSettingsOverlay) {
            QuickSettingsOverlay(
                themePalette = themePalette,
                hapticsEnabled = prefs.hapticsEnabled,
                hapticIntensity = prefs.hapticIntensity,
                onDismiss = { showQuickSettingsOverlay = false },
            )
        }

        if (showSpotlightOverlay) {
            AppSpotlightOverlay(
                allApps = allApps,
                hiddenPackages = prefs.hiddenPackages,
                themePalette = themePalette,
                initialQuery = spotlightInitialQuery,
                onLaunchApp = { app ->
                    showSpotlightOverlay = false
                    spotlightInitialQuery = ""
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) context.startActivity(launchIntent)
                },
                onDismiss = {
                    showSpotlightOverlay = false
                    spotlightInitialQuery = ""
                },
            )
        }

        val pinToHomepageEnabled =
            !classicMode &&
                prefs.homeStripEnabled &&
                prefs.showShortcutApps &&
                prefs.canAddHomeStripItem()
        val pinnedPackageNamesForPicker = remember(prefs.homeShortcutPackages) {
            prefs.homeShortcutPackages.mapTo(HashSet()) { parseHomeShortcutToken(it).first }
        }

        if (showHomeActions) {
            HomeActionsSheet(
                themePalette = themePalette,
                hasWidget = homeWidgetId != null,
                newHomeGroupEnabled = !classicMode && prefs.homeStripEnabled && prefs.canAddHomeStripItem(),
                pinToHomepageEnabled = pinToHomepageEnabled,
                onAddWidget = {
                    val id = appWidgetHost.allocateAppWidgetId()
                    pendingWidgetId = id
                    val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    }
                    pickWidgetLauncher.launch(pickIntent)
                    showHomeActions = false
                },
                onRemoveWidget = {
                    homeWidgetId?.let { runCatching { appWidgetHost.deleteAppWidgetId(it) } }
                    homeWidgetId = null
                    showHomeActions = false
                },
                onOpenSettings = {
                    showHomeActions = false
                    showSettings = true
                },
                onOpenWallpaperChooser = {
                    showHomeActions = false
                    wallpaperSourceSub = WallpaperSourceSub.List
                    showWallpaperSourceOverlay = true
                },
                onOpenSystemSettings = {
                    showHomeActions = false
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                onNewHomeGroup = {
                    showHomeActions = false
                    newHomeGroupName = ""
                    showNewHomeGroupDialog = true
                },
                onPinToHomepage = {
                    showHomeActions = false
                    showPinAppToHome = true
                },
                onDismiss = { showHomeActions = false },
            )
        }

        if (showPinAppToHome) {
            PinAppToHomeSheet(
                allApps = allApps,
                hiddenPackages = prefs.hiddenPackages,
                pinnedPackageNames = pinnedPackageNamesForPicker,
                appIconShape = prefs.appIconShape,
                themePalette = themePalette,
                onSelect = { app ->
                    vm.addHomeShortcut(app.packageName)
                    showPinAppToHome = false
                },
                onDismiss = { showPinAppToHome = false },
            )
        }

        if (showNewHomeGroupDialog) {
            LaunchedEffect(Unit) { newHomeGroupFocusRequester.requestFocus() }
            AlertDialog(
                onDismissRequest = { showNewHomeGroupDialog = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = themePalette.settingsBg,
                titleContentColor = themePalette.settingsMenuTitle,
                textContentColor = themePalette.settingsMenuBody,
                title = { Text("New group", color = themePalette.settingsMenuTitle) },
                text = {
                    OutlinedTextField(
                        value = newHomeGroupName,
                        onValueChange = { newHomeGroupName = capitalizeFirstLetterForGroupInput(it) },
                        singleLine = true,
                        label = { Text("Group name", color = themePalette.settingsMenuBody) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE8EEF7),
                            unfocusedTextColor = Color(0xFFE8EEF7),
                            focusedLabelColor = themePalette.settingsMenuBody,
                            unfocusedLabelColor = themePalette.settingsMenuBody,
                            focusedIndicatorColor = Color(0xFF5B9BD5),
                            unfocusedIndicatorColor = Color(0xFF5F6A78),
                            focusedContainerColor = Color(0xFF1E2430),
                            unfocusedContainerColor = Color(0xFF1E2430),
                            disabledContainerColor = Color(0xFF1E2430),
                            cursorColor = Color(0xFF84D5F6),
                            focusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                            unfocusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(newHomeGroupFocusRequester),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = newHomeGroupName
                            if (groupNameExists(name)) {
                                Toast.makeText(context, "Groups already exist", Toast.LENGTH_SHORT).show()
                            } else if (!prefs.canAddHomeStripItem()) {
                                Toast.makeText(context, "Home strip is full (5 items)", Toast.LENGTH_SHORT).show()
                                showNewHomeGroupDialog = false
                            } else {
                                vm.createHomeGroup(name)
                                showNewHomeGroupDialog = false
                            }
                        },
                    ) { Text("Create", color = themePalette.settingsMenuBody) }
                },
                dismissButton = {
                    TextButton(onClick = { showNewHomeGroupDialog = false }) {
                        Text("Cancel", color = themePalette.settingsMenuBody)
                    }
                },
            )
        }

        SnackbarHost(
            hostState = homeStripSnackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        )
    }
}

/**
 * Long-press opens the home actions sheet. Double-tap to lock runs when [doubleTapToSleepEnabled] is on
 * ([SleepManager.lockNow]: accessibility lock helper first, else device admin); child views (glance strip)
 * consume their own taps first.
 */
@Composable
private fun HomePage(
    focusRequester: FocusRequester,
    homeActive: Boolean,
    onOpenQuickSettings: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    homeStripCount: Int,
    onActivateHomeStripIndex: (Int) -> Unit,
    onHomeDockFocusChanged: (Boolean, Int) -> Unit,
    onHomeStripFocusChanged: (Boolean, Int) -> Unit,
    onHomeDockActivate: (Int) -> Unit,
    classicMode: Boolean,
    allApps: List<AppEntry>,
    hiddenPackages: Set<String>,
    onLaunchApp: (String) -> Unit,
    swipeUpPackage: String,
    doubleTapPackage: String,
    hapticsEnabled: Boolean,
    hapticIntensity: Int,
    homeWidgetId: Int?,
    appWidgetHost: AppWidgetHost,
    onLongPress: () -> Unit,
    doubleTapToSleepEnabled: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    appIconShape: AppIconShape,
    themePalette: LauncherThemePalette,
    glanceEnabled: Boolean,
    glanceStripPreferences: GlanceStripPreferences,
) {
    val view = LocalView.current
    var navArea by remember { mutableStateOf(HomeNavArea.Strip) }
    var stripIndex by remember { mutableStateOf(0) }
    var dockIndex by remember { mutableStateOf(0) }
    val dockSize = if (classicMode) 2 else 4
    LaunchedEffect(homeActive) {
        if (!homeActive) {
            navArea = HomeNavArea.Strip
            onHomeDockFocusChanged(false, -1)
            onHomeStripFocusChanged(false, -1)
        }
    }
    LaunchedEffect(homeActive, navArea, stripIndex, homeStripCount) {
        if (!homeActive || navArea != HomeNavArea.Strip || homeStripCount <= 0) {
            onHomeStripFocusChanged(false, -1)
        } else {
            onHomeStripFocusChanged(true, stripIndex.coerceIn(0, homeStripCount - 1))
        }
    }
    val context = LocalContext.current
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    val glanceRef = remember { mutableStateOf<GlanceDateWeatherEventsView?>(null) }
    DisposableEffect(Unit) {
        onDispose { glanceRef.value?.dispose() }
    }
    // Battery optimization: don't run the glance strip when the page isn't visible (it may still be kept
    // in composition due to `beyondViewportPageCount`).
    LaunchedEffect(glanceEnabled, homeActive) {
        if (!glanceEnabled || !homeActive) {
            glanceRef.value?.dispose()
            glanceRef.value = null
        }
    }
    val currentOnLongPress = rememberUpdatedState(onLongPress)

    var searchFocusIndex by remember { mutableStateOf(-1) }
    val searchResults = remember(searchQuery, allApps, hiddenPackages) {
        if (searchQuery.isEmpty()) emptyList()
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) && it.packageName !in hiddenPackages }.take(4)
    }
    val settingsResults = remember(searchQuery) { matchSettingsEntries(searchQuery) }
    LaunchedEffect(searchQuery) { searchFocusIndex = -1 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val newQuery = tryConsumeSearchKey(ev, searchQuery)
                if (newQuery != null) {
                    onSearchQueryChange(newQuery)
                    return@onPreviewKeyEvent true
                }

                // When search overlay is active, D-pad navigates app + settings results
                val totalSearchItems = searchResults.size + settingsResults.size
                if (searchQuery.isNotEmpty() && totalSearchItems > 0) {
                    val nk = ev.nativeKeyEvent
                    when {
                        ev.key == Key.DirectionDown || nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            searchFocusIndex = (searchFocusIndex + 1).coerceAtMost(totalSearchItems - 1)
                            doNavFeedback(view, hapticsEnabled, hapticIntensity)
                            return@onPreviewKeyEvent true
                        }
                        ev.key == Key.DirectionUp || nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                            if (searchFocusIndex > 0) searchFocusIndex -= 1 else searchFocusIndex = -1
                            doNavFeedback(view, hapticsEnabled, hapticIntensity)
                            return@onPreviewKeyEvent true
                        }
                        (ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                            nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                            nk?.keyCode == android.view.KeyEvent.KEYCODE_ENTER) && searchFocusIndex >= 0 -> {
                            if (searchFocusIndex < searchResults.size) {
                                val app = searchResults.getOrNull(searchFocusIndex)
                                if (app != null) { onSearchQueryChange(""); onLaunchApp(app.packageName) }
                            } else {
                                val entry = settingsResults.getOrNull(searchFocusIndex - searchResults.size)
                                if (entry != null) {
                                    onSearchQueryChange("")
                                    val intent = Intent(entry.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    runCatching { context.startActivity(intent) }.onFailure {
                                        runCatching {
                                            context.startActivity(Intent(entry.fallbackAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        }
                                    }
                                }
                            }
                            return@onPreviewKeyEvent true
                        }
                    }
                }

                when (ev.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        if (navArea == HomeNavArea.Dock) onHomeDockActivate(dockIndex)
                        else if (homeStripCount > 0) onActivateHomeStripIndex(stripIndex.coerceIn(0, homeStripCount - 1))
                        true
                    }
                    Key.DirectionLeft -> {
                        if (navArea == HomeNavArea.Dock) {
                            val next = (dockIndex - 1).coerceAtLeast(0)
                            if (next != dockIndex) {
                                dockIndex = next
                                onHomeDockFocusChanged(true, dockIndex)
                                doNavFeedback(view, hapticsEnabled, hapticIntensity)
                            }
                        } else if (homeStripCount > 0) {
                            val next = (stripIndex - 1).coerceAtLeast(0)
                            if (next != stripIndex) {
                                stripIndex = next
                                doNavFeedback(view, hapticsEnabled, hapticIntensity)
                            }
                        }
                        true
                    }
                    Key.DirectionRight -> {
                        val longPressRight = (ev.nativeKeyEvent?.repeatCount ?: 0) > 0
                        if (navArea == HomeNavArea.Dock) {
                            if (dockIndex >= dockSize - 1) {
                                if (longPressRight) {
                                    onHomeDockFocusChanged(false, -1)
                                    onHomeStripFocusChanged(false, -1)
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    onOpenAppDrawer()
                                }
                            } else {
                                val next = (dockIndex + 1).coerceAtMost(dockSize - 1)
                                if (next != dockIndex) {
                                    dockIndex = next
                                    onHomeDockFocusChanged(true, dockIndex)
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                }
                            }
                        } else if (homeStripCount > 0) {
                            if (stripIndex >= homeStripCount - 1) {
                                if (longPressRight) {
                                    onHomeStripFocusChanged(false, -1)
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    onOpenAppDrawer()
                                }
                            } else {
                                val next = (stripIndex + 1).coerceAtMost(homeStripCount - 1)
                                if (next != stripIndex) {
                                    stripIndex = next
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                }
                            }
                        }
                        true
                    }
                    Key.DirectionDown -> {
                        if (navArea != HomeNavArea.Dock) {
                            if (homeStripCount > 0) {
                                dockIndex = if (homeStripCount <= 1 || dockSize <= 1) {
                                    0
                                } else {
                                    ((stripIndex.toFloat() / (homeStripCount - 1).toFloat()) * (dockSize - 1))
                                        .roundToInt()
                                        .coerceIn(0, dockSize - 1)
                                }
                            }
                            navArea = HomeNavArea.Dock
                            onHomeDockFocusChanged(true, dockIndex)
                            doNavFeedback(view, hapticsEnabled, hapticIntensity)
                        }
                        true
                    }
                    Key.DirectionUp -> {
                        if (navArea == HomeNavArea.Dock) {
                            navArea = HomeNavArea.Strip
                            if (homeStripCount > 0) {
                                val mapped = if (dockSize <= 1) {
                                    0
                                } else {
                                    ((dockIndex.toFloat() / (dockSize - 1).toFloat()) * (homeStripCount - 1))
                                        .roundToInt()
                                        .coerceIn(0, homeStripCount - 1)
                                }
                                stripIndex = mapped
                            }
                            onHomeDockFocusChanged(false, -1)
                            doNavFeedback(view, hapticsEnabled, hapticIntensity)
                        } else {
                            onHomeStripFocusChanged(false, -1)
                            doNavFeedback(view, hapticsEnabled, hapticIntensity)
                            onOpenAppDrawer()
                        }
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(doubleTapToSleepEnabled, doubleTapPackage, searchQuery) {
                detectTapGestures(
                    onLongPress = { if (searchQuery.isEmpty()) currentOnLongPress.value() },
                    onDoubleTap = {
                        if (searchQuery.isEmpty()) when {
                            doubleTapToSleepEnabled -> {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                SleepManager.lockNow(context)
                            }
                            doubleTapPackage.isNotEmpty() -> {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onLaunchApp(doubleTapPackage)
                            }
                        }
                    },
                )
            }
            .pointerInput(swipeUpPackage, searchQuery) {
                val threshold = 80.dp.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startY = down.position.y
                        var triggered = false
                        while (!triggered) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val dy = change.position.y - startY
                            if (dy < -threshold && swipeUpPackage.isNotEmpty() && searchQuery.isEmpty()) {
                                triggered = true
                                onLaunchApp(swipeUpPackage)
                            }
                        }
                    }
                }
            }
            .pointerInput(searchQuery) {
                val threshold = 72.dp.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startY = down.position.y
                        var triggered = false
                        while (!triggered) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val dy = change.position.y - startY
                            if (dy > threshold && searchQuery.isEmpty()) {
                                triggered = true
                                onOpenQuickSettings()
                            }
                        }
                    }
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        ) {
            if (glanceEnabled && homeActive) {
                AndroidView(
                    factory = { GlanceDateWeatherEventsView(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top)
                        .heightIn(max = 200.dp),
                    update = { v ->
                        glanceRef.value = v
                        v.applyStripPreferences(glanceStripPreferences)
                    },
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
        if (homeWidgetId != null && appWidgetManager.getAppWidgetInfo(homeWidgetId) != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        val hostView = appWidgetHost.createView(
                            ctx,
                            homeWidgetId,
                            appWidgetManager.getAppWidgetInfo(homeWidgetId),
                        )
                        hostView.setAppWidget(homeWidgetId, appWidgetManager.getAppWidgetInfo(homeWidgetId))
                        hostView
                    },
                    update = { hostView ->
                        if (hostView is AppWidgetHostView) {
                            hostView.setAppWidget(homeWidgetId, appWidgetManager.getAppWidgetInfo(homeWidgetId))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 180.dp),
                )
            }
        }

        // Home search overlay — compact top card, wallpaper visible below
        if (searchQuery.isNotEmpty()) {
            val allFiltered = remember(searchQuery, allApps, hiddenPackages) {
                allApps.filter { it.label.contains(searchQuery, ignoreCase = true) && it.packageName !in hiddenPackages }
            }
            val extra = allFiltered.size - searchResults.size
            BackHandler { onSearchQueryChange("") }
            val totalSearchItems = searchResults.size + settingsResults.size
            val searchRowBringers = remember(totalSearchItems) { List(totalSearchItems) { BringIntoViewRequester() } }
            val searchScrollScope = rememberCoroutineScope()
            LaunchedEffect(searchFocusIndex) {
                if (searchFocusIndex in 0 until totalSearchItems) {
                    searchScrollScope.launch { searchRowBringers[searchFocusIndex].bringIntoView() }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xF01A1F28)),
            ) {
                // Search bar row — fixed, never scrolls away
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color(0xFF8E95A3),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = searchQuery,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A3F4A))
                            .clickable { onSearchQueryChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Clear",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                // Scrollable results — capped so it never goes behind the dock
                Column(
                    modifier = Modifier
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                // Divider
                if (searchResults.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0x33FFFFFF)))
                }
                // Results list — top 4, with D-pad focus highlight
                searchResults.forEachIndexed { idx, app ->
                    val isFocused = idx == searchFocusIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(searchRowBringers[idx])
                            .background(if (isFocused) Color(0x336EA8D8) else Color.Transparent)
                            .clickable {
                                onSearchQueryChange("")
                                onLaunchApp(app.packageName)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = app.icon,
                            contentDescription = app.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(42.dp).clip(iconMaskShape(appIconShape)),
                        )
                        Spacer(Modifier.width(12.dp))
                        val baseColor = if (isFocused) Color(0xFF84D5F6) else HOME_STRIP_LABEL_COLOR
                        val highlightColor = Color(0xFF84D5F6)
                        val label = app.label
                        val matchStart = label.indexOf(searchQuery, ignoreCase = true)
                        val annotated = remember(label, searchQuery, isFocused) {
                            androidx.compose.ui.text.buildAnnotatedString {
                                if (matchStart >= 0) {
                                    val matchEnd = matchStart + searchQuery.length
                                    append(label.substring(0, matchStart))
                                    withStyle(
                                        androidx.compose.ui.text.SpanStyle(
                                            color = highlightColor,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    ) { append(label.substring(matchStart, matchEnd)) }
                                    append(label.substring(matchEnd))
                                } else {
                                    append(label)
                                }
                            }
                        }
                        Text(
                            text = annotated,
                            color = baseColor,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Settings results
                if (settingsResults.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0x22FFFFFF)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = Color(0xFF8E95A3),
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("Settings", color = Color(0xFF8E95A3), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    settingsResults.forEachIndexed { settingsIdx, entry ->
                        val absoluteIdx = searchResults.size + settingsIdx
                        val isFocused = absoluteIdx == searchFocusIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(searchRowBringers[absoluteIdx])
                                .background(if (isFocused) Color(0x336EA8D8) else Color.Transparent)
                                .clickable {
                                    onSearchQueryChange("")
                                    val intent = Intent(entry.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    runCatching { context.startActivity(intent) }.onFailure {
                                        runCatching {
                                            context.startActivity(
                                                Intent(entry.fallbackAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF2C3547), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = if (isFocused) Color(0xFF84D5F6) else Color(0xFF8E95A3),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = entry.label,
                                color = if (isFocused) Color(0xFF84D5F6) else HOME_STRIP_LABEL_COLOR,
                                fontSize = 14.sp,
                                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                // No results text
                if (searchResults.isEmpty() && settingsResults.isEmpty()) {
                    Text(
                        text = "No apps found",
                        color = Color(0xFF8E95A3),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                // Play Store row — only when no app results found
                if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0x22FFFFFF)))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openPlayStoreSearch(context, searchQuery) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF2C3547), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = Color(0xFF4A90D9),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Search \"$searchQuery\" on Play Store",
                        color = Color(0xFF84D5F6),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                } // end Play Store row
                Spacer(Modifier.height(8.dp))
                } // end scrollable results column
            }
        }
    }
}

private data class QuickTile(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val highlighted: Boolean = false,
    val showChevron: Boolean = false,
    val closeOnSuccess: Boolean = true,
    val onLongPress: (() -> Boolean)? = null,
    val onTap: () -> Boolean,
)

/**
 * Horizontal padding for QS so tiles line up with the status-bar clock, not with generic "system bar"
 * side insets (those are often much larger than where the time glyph starts).
 * Only display cutout is applied; [extra] is a tiny symmetric gutter (0 for max alignment with "1").
 */
private fun qsHorizontalEdgePadding(view: android.view.View, density: Density, extra: Dp): Pair<Dp, Dp> {
    val px = ViewCompat.getRootWindowInsets(view)
        ?.getInsets(WindowInsetsCompat.Type.displayCutout())
        ?: androidx.core.graphics.Insets.NONE
    return with(density) {
        (px.left.toDp() + extra) to (px.right.toDp() + extra)
    }
}

@Composable
private fun QuickSettingsOverlay(
    themePalette: LauncherThemePalette,
    hapticsEnabled: Boolean,
    hapticIntensity: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    /** Minimal extra beyond cutout — 0 so tile column aligns with status-bar time. */
    val qsHorizontalGutter = 0.dp
    /** Extra height below status bar for date + divider band — covered by a darker top scrim. */
    val qsTopDarkBelowStatusDp = 96.dp
    // Activity context: starting GMS barcode UI can mis-route via applicationContext on some OEMs.
    val actions = remember(context) { LauncherActions(context) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    var dateText by remember { mutableStateOf(dateFormatter.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            dateText = dateFormatter.format(Date())
            delay(60_000L)
        }
    }
    var bluetoothEnabled by remember { mutableStateOf(actions.isBluetoothEnabled()) }
    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            bluetoothEnabled = actions.isBluetoothEnabled()
        } else {
            Toast.makeText(context, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    val wifiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Location permission denied for Wi-Fi name", Toast.LENGTH_SHORT).show()
        }
    }
    val wifiPrecisePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ -> }
    val btSubtitle = when (bluetoothEnabled) {
        true -> "On"
        false -> "Off"
        null -> "Tap to allow control"
    }
    var wifiSubtitle by remember { mutableStateOf(actions.currentWifiSsidLabel()) }
    var carrierSubtitle by remember { mutableStateOf(actions.currentCarrierName()) }
    var wifiEnabled by remember { mutableStateOf(actions.isWifiEnabled()) }
    var mobileDataEnabled by remember { mutableStateOf(actions.isMobileDataEnabled()) }
    var wirelessDebugOn by remember { mutableStateOf(actions.isWirelessDebuggingEnabled()) }
    var batterySaverOn by remember { mutableStateOf(actions.isBatterySaverEnabled()) }
    var airplaneOn by remember { mutableStateOf(actions.isAirplaneModeEnabled()) }
    var dndOn by remember { mutableStateOf(actions.isDoNotDisturbEnabled()) }
    var hotspotOn by remember { mutableStateOf(actions.isHotspotEnabled()) }
    var nightLightOn by remember { mutableStateOf(actions.isNightLightEnabled()) }
    var autoRotateOn by remember { mutableStateOf(actions.isAutoRotateEnabled()) }
    var nfcOn by remember { mutableStateOf(actions.isNfcEnabled()) }
    var extraDimOn by remember { mutableStateOf(actions.isExtraDimEnabled()) }
    var torchOn by remember { mutableStateOf(actions.isTorchEnabled()) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            when (val r = actions.toggleTorch()) {
                is ToggleResult.Changed -> torchOn = r.enabled
                else -> Toast.makeText(context, "Could not toggle torch", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission denied for torch", Toast.LENGTH_SHORT).show()
        }
    }
    var keyboardMode by remember { mutableStateOf(actions.lastKnownKeyboardMode()) }
    var showTileEditor by remember { mutableStateOf(false) }
    var preciseWifiPromptAttempted by remember { mutableStateOf(false) }
    var batteryPct by remember { mutableStateOf(actions.batteryPercent()) }
    val hasBitwarden = remember(actions) { actions.isBitwardenInstalled() }
    val hasWellbeing = remember(actions) { actions.isDigitalWellbeingInstalled() }
    LaunchedEffect(Unit) {
        if (!actions.hasWifiNamePermission()) {
            wifiPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        while (true) {
            wifiSubtitle = actions.currentWifiSsidLabel()
            if (
                wifiSubtitle == "Connected" &&
                actions.isWifiConnected() &&
                !actions.hasPreciseLocationPermission() &&
                !preciseWifiPromptAttempted
            ) {
                preciseWifiPromptAttempted = true
                wifiPrecisePermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            carrierSubtitle = actions.currentCarrierName()
            wifiEnabled = actions.isWifiEnabled()
            mobileDataEnabled = actions.isMobileDataEnabled()
            wirelessDebugOn = actions.isWirelessDebuggingEnabled()
            batterySaverOn = actions.isBatterySaverEnabled()
            airplaneOn = actions.isAirplaneModeEnabled()
            dndOn = actions.isDoNotDisturbEnabled()
            hotspotOn = actions.isHotspotEnabled()
            nightLightOn = actions.isNightLightEnabled()
            autoRotateOn = actions.isAutoRotateEnabled()
            nfcOn = actions.isNfcEnabled()
            extraDimOn = actions.isExtraDimEnabled()
            torchOn = actions.isTorchEnabled()
            batteryPct = actions.batteryPercent()
            actions.currentKeyboardMode()?.let {
                keyboardMode = it
                actions.persistKeyboardModeLabel(it)
            }
            delay(5_000L)
        }
    }
    val internetSubtitle = when {
        wifiEnabled != false && wifiSubtitle != "Disconnected" -> wifiSubtitle
        mobileDataEnabled != false -> carrierSubtitle
        else -> "Off"
    }
    val internetHighlighted =
        (wifiEnabled != false && wifiSubtitle != "Disconnected") || (mobileDataEnabled != false)
    val defaultQuickTiles = buildList {
        add(
            QuickTile(
                id = "keyboard_mode",
                icon = Icons.Rounded.Tune,
                title = keyboardMode,
                subtitle = "",
                highlighted = true,
                closeOnSuccess = false,
                showChevron = true,
                onLongPress = actions::openKeyboardSettings,
                onTap = {
                    val nextMode = if (keyboardMode == "keyboard") "mouse" else "keyboard"
                    val ok = actions.setKeyboardMode(nextMode)
                    if (ok) {
                        keyboardMode = actions.currentKeyboardMode() ?: nextMode
                        actions.persistKeyboardModeLabel(keyboardMode)
                        Handler(Looper.getMainLooper()).postDelayed({
                            actions.currentKeyboardMode()?.let { keyboardMode = it }
                        }, 600L)
                    } else {
                        if (!actions.canWriteSystemSettings()) {
                            actions.requestWriteSettingsPermission()
                            Toast.makeText(
                                context,
                                "Allow Modify system settings, then tap keyboard tile again.",
                                Toast.LENGTH_LONG,
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Could not apply keyboard mode change. Long-press opens settings.",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                    true
                },
            ),
        )
        add(
            QuickTile(
                id = "internet",
                icon = Icons.Rounded.Wifi,
                title = "Internet",
                subtitle = internetSubtitle,
                highlighted = internetHighlighted,
                closeOnSuccess = false,
                showChevron = true,
                onLongPress = actions::openInternetSettings,
                onTap = actions::openInternetPanel,
            ),
        )
        add(
            QuickTile(
                id = "bluetooth",
                icon = Icons.Rounded.Bluetooth,
                title = "Bluetooth",
                subtitle = btSubtitle,
                highlighted = bluetoothEnabled == true,
                closeOnSuccess = false,
                onLongPress = actions::openBluetoothSettings,
                onTap = {
                    when (val r = actions.toggleBluetooth()) {
                        is ToggleResult.Changed -> {
                            bluetoothEnabled = r.enabled
                            Handler(Looper.getMainLooper()).postDelayed({
                                bluetoothEnabled = actions.isBluetoothEnabled()
                            }, 500L)
                            true
                        }
                        ToggleResult.PermissionRequired -> {
                            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            true
                        }
                        ToggleResult.Unsupported -> {
                            Toast.makeText(
                                context,
                                "Bluetooth toggle blocked. Long-press opens settings, or grant all toggles in system settings.",
                                Toast.LENGTH_LONG,
                            ).show()
                            true
                        }
                    }
                },
            ),
        )
        add(
            QuickTile(
                id = "qr_scanner",
                icon = Icons.Outlined.Search,
                title = "QR code scanner",
                subtitle = "",
                showChevron = true,
                closeOnSuccess = false,
                onLongPress = actions::openQrScanner,
                onTap = actions::openQrScanner,
            ),
        )
        add(
            QuickTile(
                id = "wireless_debugging",
                icon = Icons.Rounded.Settings,
                title = "Wireless debugging",
                subtitle = if (wirelessDebugOn) "On" else "Off",
                highlighted = wirelessDebugOn,
                closeOnSuccess = false,
                onLongPress = actions::openWirelessDebuggingSettings,
                onTap = actions::openWirelessDebuggingSettings,
            ),
        )
        add(
            QuickTile(
                id = "battery",
                icon = Icons.Rounded.BatteryStd,
                title = "Battery",
                subtitle = buildString {
                    batteryPct?.let { append("$it%") }
                    if (batterySaverOn) {
                        if (isNotEmpty()) append(" · ")
                        append("Saver on")
                    }
                },
                highlighted = batterySaverOn,
                closeOnSuccess = false,
                showChevron = true,
                onLongPress = actions::openBatterySaverSettings,
                onTap = actions::openBatteryUsageSummary,
            ),
        )
        if (hasBitwarden) {
            add(
                QuickTile(
                    id = "my_vault",
                    icon = Icons.Rounded.Lock,
                    title = "My vault",
                    subtitle = "",
                    showChevron = true,
                    closeOnSuccess = false,
                    onTap = actions::openBitwardenVault,
                ),
            )
        }
        add(
            QuickTile(
                id = "airplane_mode",
                icon = Icons.Rounded.SwapVert,
                title = "Aeroplane mode",
                subtitle = if (airplaneOn) "On" else "Off",
                highlighted = airplaneOn,
                onLongPress = actions::openAirplaneModeSettings,
                onTap = actions::openAirplaneModeSettings,
            ),
        )
        add(
            QuickTile(
                id = "torch",
                icon = Icons.Rounded.WbSunny,
                title = "Torch",
                subtitle = if (torchOn) "On" else "Off",
                highlighted = torchOn,
                closeOnSuccess = false,
                onLongPress = actions::openDisplaySettings,
                onTap = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        true
                    } else {
                        when (val r = actions.toggleTorch()) {
                            is ToggleResult.Changed -> {
                                torchOn = r.enabled
                                true
                            }
                            ToggleResult.Unsupported -> {
                                if (actions.hasTorchHardware()) {
                                    Toast.makeText(context, "Could not toggle torch", Toast.LENGTH_SHORT).show()
                                } else {
                                    actions.openDisplaySettings()
                                }
                                true
                            }
                            else -> true
                        }
                    }
                },
            ),
        )
        add(
            QuickTile(
                id = "dnd",
                icon = Icons.Rounded.VisibilityOff,
                title = "Do Not Disturb",
                subtitle = if (dndOn) "On" else "Off",
                highlighted = dndOn,
                onLongPress = actions::openDoNotDisturbSettings,
                onTap = actions::openDoNotDisturbSettings,
            ),
        )
        add(
            QuickTile(
                id = "storage",
                icon = Icons.Rounded.Info,
                title = "Storage",
                subtitle = "",
                showChevron = true,
                onLongPress = actions::openStorageSettings,
                onTap = actions::openStorageSettings,
            ),
        )
        add(
            QuickTile(
                id = "hotspot",
                icon = Icons.Rounded.SwapVert,
                title = "Hotspot",
                subtitle = if (hotspotOn) "On" else "Off",
                highlighted = hotspotOn,
                onLongPress = actions::openHotspotSettings,
                onTap = actions::openHotspotSettings,
            ),
        )
        add(
            QuickTile(
                id = "night_light",
                icon = Icons.Rounded.WbSunny,
                title = "Night Light",
                subtitle = if (nightLightOn) "On" else "Off",
                highlighted = nightLightOn,
                onLongPress = actions::openNightLightSettings,
                onTap = actions::openNightLightSettings,
            ),
        )
        add(
            QuickTile(
                id = "auto_rotate",
                icon = Icons.Rounded.SwapVert,
                title = "Auto-rotate",
                subtitle = if (autoRotateOn) "On" else "Off",
                highlighted = autoRotateOn,
                onLongPress = actions::openDisplaySettings,
                onTap = {
                    when (val r = actions.toggleAutoRotate()) {
                        is ToggleResult.Changed -> {
                            autoRotateOn = r.enabled
                            true
                        }
                        ToggleResult.PermissionRequired -> {
                            actions.requestWriteSettingsPermission()
                            Toast.makeText(
                                context,
                                "Allow Modify system settings to toggle auto-rotate.",
                                Toast.LENGTH_LONG,
                            ).show()
                            true
                        }
                        ToggleResult.Unsupported -> actions.openDisplaySettings()
                    }
                },
            ),
        )
        add(
            QuickTile(
                id = "nfc",
                icon = Icons.Rounded.Nfc,
                title = "NFC",
                subtitle = if (nfcOn) "On" else "Off",
                highlighted = nfcOn,
                closeOnSuccess = false,
                onLongPress = actions::openNfcSettings,
                onTap = {
                    when (val r = actions.toggleNfc()) {
                        is ToggleResult.Changed -> {
                            nfcOn = r.enabled
                            Handler(Looper.getMainLooper()).postDelayed({
                                nfcOn = actions.isNfcEnabled()
                            }, 500L)
                            true
                        }
                        else -> actions.openNfcSettings()
                    }
                },
            ),
        )
        add(
            QuickTile(
                id = "extra_dim",
                icon = Icons.Rounded.VisibilityOff,
                title = "Extra dim",
                subtitle = if (extraDimOn) "On" else "Off",
                highlighted = extraDimOn,
                onLongPress = actions::openExtraDimSettings,
                onTap = actions::openExtraDimSettings,
            ),
        )
        add(
            QuickTile(
                id = "screen_record",
                icon = Icons.Rounded.TouchApp,
                title = "Screen record",
                subtitle = "Start",
                showChevron = true,
                onLongPress = actions::openScreenRecordSettings,
                onTap = actions::openScreenRecordSettings,
            ),
        )
        add(
            QuickTile(
                id = "screen_cast",
                icon = Icons.Rounded.Wallpaper,
                title = "Screen Cast",
                subtitle = "Off",
                showChevron = true,
                onLongPress = actions::openCastSettings,
                onTap = actions::openCastSettings,
            ),
        )
        if (hasWellbeing) {
            add(
                QuickTile(
                    id = "grayscale",
                    icon = Icons.Rounded.FilterBAndW,
                    title = "Greyscale",
                    subtitle = "",
                    showChevron = true,
                    closeOnSuccess = false,
                    onLongPress = actions::openDigitalWellbeingHome,
                    onTap = actions::openDigitalWellbeingHome,
                ),
            )
        } else {
            add(
                QuickTile(
                    id = "bedtime",
                    icon = Icons.Rounded.AddAlarm,
                    title = "Bedtime mode",
                    subtitle = "Off",
                    onLongPress = actions::openBedtimeSettings,
                    onTap = actions::openBedtimeSettings,
                ),
            )
        }
    }
    val orderedTileIds = remember { mutableStateListOf<String>() }
    val defaultTileIds = defaultQuickTiles.map { it.id }
    LaunchedEffect(defaultTileIds) {
        if (orderedTileIds.isEmpty()) {
            orderedTileIds.addAll(defaultTileIds)
        } else {
            orderedTileIds.retainAll(defaultTileIds.toSet())
            defaultTileIds.forEach { id ->
                if (id !in orderedTileIds) orderedTileIds.add(id)
            }
        }
    }
    val tileById = defaultQuickTiles.associateBy { it.id }
    val allQuickTiles = orderedTileIds.mapNotNull { tileById[it] }
    fun moveTile(from: Int, to: Int) {
        if (from == to) return
        if (from !in orderedTileIds.indices || to !in orderedTileIds.indices) return
        val id = orderedTileIds.removeAt(from)
        orderedTileIds.add(to, id)
    }
    val qsTilesPerPage = 8
    val pages = allQuickTiles.chunked(qsTilesPerPage)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val (qsPadStart, qsPadEnd) = qsHorizontalEdgePadding(view, density, qsHorizontalGutter)
    val scope = rememberCoroutineScope()

    /** Index within [pagerState.currentPage] only (same idea as app drawer grid index). */
    var qsGridIndex by remember { mutableIntStateOf(0) }
    /** Focus on the tune row below the pager (like dock below drawer grid). */
    var qsOnEdit by remember { mutableStateOf(false) }
    val lastDirectionalRef = remember { longArrayOf(0L) }
    val lastMoveRef = remember { longArrayOf(0L) }
    val qsKeyFocusRequester = remember { FocusRequester() }
    LaunchedEffect(allQuickTiles.size, pages.size) {
        if (allQuickTiles.isEmpty()) {
            qsGridIndex = 0
            qsOnEdit = false
            return@LaunchedEffect
        }
        val lastPage = pages.lastIndex.coerceAtLeast(0)
        val p = pagerState.currentPage.coerceIn(0, lastPage)
        val slice = pages.getOrNull(p)?.size ?: 0
        if (slice > 0) {
            qsGridIndex = qsGridIndex.coerceIn(0, slice - 1)
        } else {
            qsGridIndex = 0
        }
    }
    LaunchedEffect(Unit) {
        qsKeyFocusRequester.requestFocus()
    }
    LaunchedEffect(showTileEditor) {
        if (!showTileEditor) {
            delay(50)
            qsKeyFocusRequester.requestFocus()
        }
    }

    fun runQsTileTap(tile: QuickTile) {
        if (tile.id == "qr_scanner") {
            onDismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                val ok = tile.onTap()
                if (!ok) {
                    Toast.makeText(context, "Could not open ${tile.title}", Toast.LENGTH_SHORT).show()
                }
            }, 120L)
        } else {
            val ok = tile.onTap()
            if (!ok) {
                Toast.makeText(context, "Could not open ${tile.title}", Toast.LENGTH_SHORT).show()
            } else if (tile.closeOnSuccess) {
                onDismiss()
            }
        }
    }

    fun runQsTileLongPress(tile: QuickTile) {
        if (tile.id == "qr_scanner") {
            onDismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                val longOk = tile.onLongPress?.invoke() ?: false
                if (!longOk) {
                    Toast.makeText(context, "Could not open ${tile.title}", Toast.LENGTH_SHORT).show()
                }
            }, 120L)
        } else {
            val longOk = tile.onLongPress?.invoke() ?: false
            if (longOk) onDismiss()
        }
    }

    val statusTopPx = ViewCompat.getRootWindowInsets(view)
        ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
    val qsTopDarkBand = with(density) { statusTopPx.toDp() } + qsTopDarkBelowStatusDp
    val qsTopDarkBandPx = with(density) { qsTopDarkBand.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(520f)
            .pointerInput(Unit) {
                val threshold = 72.dp.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startY = down.position.y
                        var triggered = false
                        while (!triggered) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val dy = change.position.y - startY
                            if (dy < -threshold) {
                                triggered = true
                                onDismiss()
                            }
                        }
                    }
                }
            },
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xE6000000)))
        // Stronger dim from the top edge through the date row so wallpaper does not show through.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(qsTopDarkBand)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFA000000),
                            Color(0xE8000000),
                            Color(0x00000000),
                        ),
                        startY = 0f,
                        endY = qsTopDarkBandPx,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = qsPadStart, end = qsPadEnd, top = 4.dp, bottom = 4.dp)
                .focusRequester(qsKeyFocusRequester)
                .focusable()
                .onPreviewKeyEvent { ev ->
                    if (showTileEditor) return@onPreviewKeyEvent false
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (ev.isVolumePanelKey()) return@onPreviewKeyEvent false
                    val n = allQuickTiles.size
                    if (ev.key == Key.Back) {
                        onDismiss()
                        return@onPreviewKeyEvent true
                    }
                    if (ev.isEndCallKey()) {
                        onDismiss()
                        return@onPreviewKeyEvent true
                    }
                    val directional =
                        ev.key == Key.DirectionLeft ||
                            ev.key == Key.DirectionRight ||
                            ev.key == Key.DirectionUp ||
                            ev.key == Key.DirectionDown
                    if (!directional) {
                        lastDirectionalRef[0] = 0L
                    }
                    val now = SystemClock.uptimeMillis()
                    val repeatHeavy = (ev.nativeKeyEvent?.repeatCount ?: 0) > 0
                    val isHard = if (directional) {
                        val quickPair = lastDirectionalRef[0] != 0L && (now - lastDirectionalRef[0]) < NAV_FRAME_MS
                        lastDirectionalRef[0] = now
                        repeatHeavy || quickPair
                    } else {
                        false
                    }
                    val cols = 2
                    val lastPageIdx = pages.lastIndex.coerceAtLeast(0)

                    when (ev.key) {
                        Key.Enter,
                        Key.NumPadEnter,
                        -> {
                            if (n == 0) return@onPreviewKeyEvent true
                            val shift = ev.nativeKeyEvent?.isShiftPressed == true
                            if (qsOnEdit) {
                                if (!shift) showTileEditor = true
                                return@onPreviewKeyEvent true
                            }
                            val page = pagerState.currentPage.coerceIn(0, lastPageIdx)
                            val pageList = pages.getOrNull(page) ?: emptyList()
                            val sc = pageList.size
                            if (sc <= 0) return@onPreviewKeyEvent true
                            val idx = qsGridIndex.coerceIn(0, sc - 1)
                            val tile = pageList[idx]
                            if (shift) runQsTileLongPress(tile) else runQsTileTap(tile)
                            true
                        }
                        Key.DirectionLeft,
                        Key.DirectionRight,
                        Key.DirectionUp,
                        Key.DirectionDown,
                        -> {
                            if (n == 0 || pages.isEmpty()) return@onPreviewKeyEvent true
                            if (qsOnEdit) {
                                if (ev.key == Key.DirectionUp) {
                                    qsOnEdit = false
                                    val page = pagerState.currentPage.coerceIn(0, lastPageIdx)
                                    val sc = pages.getOrNull(page)?.size ?: 0
                                    if (sc > 0) {
                                        val lastTile = sc - 1
                                        qsGridIndex = (lastTile / cols) * cols
                                    }
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                }
                                return@onPreviewKeyEvent true
                            }
                            val page = pagerState.currentPage.coerceIn(0, lastPageIdx)
                            val pageList = pages.getOrNull(page) ?: emptyList()
                            val sliceCount = pageList.size
                            if (sliceCount <= 0) return@onPreviewKeyEvent true

                            val currentIndex = qsGridIndex.coerceIn(0, sliceCount - 1)
                            val currentRow = currentIndex / cols
                            val currentCol = currentIndex % cols
                            val isFirstCol = currentCol == 0
                            val isLastCol = currentCol == cols - 1 || currentIndex == sliceCount - 1

                            if (ev.key == Key.DirectionLeft && isFirstCol && page > 0 && isHard &&
                                !pagerState.isScrollInProgress
                            ) {
                                val prevCount = pages[page - 1].size
                                val rowStart = currentRow * cols
                                val target = (rowStart + cols - 1).coerceAtMost(prevCount - 1).coerceAtLeast(0)
                                qsGridIndex = target
                                scope.launch {
                                    pagerState.animateScrollToPage(page - 1)
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                }
                                return@onPreviewKeyEvent true
                            }
                            if (ev.key == Key.DirectionRight && isLastCol && page < lastPageIdx && isHard &&
                                !pagerState.isScrollInProgress
                            ) {
                                val nextCount = pages[page + 1].size
                                val target = (currentRow * cols).coerceAtMost(nextCount - 1).coerceAtLeast(0)
                                qsGridIndex = target
                                scope.launch {
                                    pagerState.animateScrollToPage(page + 1)
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                }
                                return@onPreviewKeyEvent true
                            }

                            if (directional && (now - lastMoveRef[0]) < NAV_MOVE_MIN_MS) {
                                return@onPreviewKeyEvent true
                            }
                            lastMoveRef[0] = now

                            val beforeGrid = qsGridIndex
                            val afterGrid = NavState(gridIndex = currentIndex).onGridKey(ev.key, cols, sliceCount)
                            if (afterGrid.area == FocusArea.Dock) {
                                qsOnEdit = true
                                doNavFeedback(view, hapticsEnabled, hapticIntensity)
                            } else {
                                qsGridIndex = afterGrid.gridIndex
                                if (qsGridIndex != beforeGrid) {
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                }
                            }
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Text(
                dateText,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFFE6EBF2),
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    lineHeight = 18.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                HorizontalDivider(
                    color = Color(0xFF47515D),
                    thickness = 1.dp,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(pages.size) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (i == pagerState.currentPage) Color(0xFFE6EBF2) else Color(0xFF6E7885)),
                        )
                    }
                }
            }
            val maxPageRows = pages.maxOfOrNull { (it.size + 1) / 2 } ?: 0
            val pagerHeight = (maxPageRows * 70 + (maxPageRows - 1).coerceAtLeast(0) * 8).dp
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pagerHeight),
            ) { page ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(items = pages[page], key = { _, t -> t.id }) { _, tile ->
                        ClassicQuickTile(
                            tile = tile,
                            onTap = { runQsTileTap(tile) },
                            onLongPress = { runQsTileLongPress(tile) },
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Edit quick settings",
                    tint = Color(0x80E6EBF2),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { showTileEditor = true },
                )
            }
        }
        if (showTileEditor) {
            QuickSettingsTileEditorOverlay(
                tiles = allQuickTiles,
                onDismiss = { showTileEditor = false },
                onReset = {
                    orderedTileIds.clear()
                    orderedTileIds.addAll(defaultTileIds)
                },
                onMove = ::moveTile,
            )
        }
    }
}

@Composable
private fun QuickSettingsTileEditorOverlay(
    tiles: List<QuickTile>,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
) {
    val editorView = LocalView.current
    val editorDensity = LocalDensity.current
    val (editorPadStart, editorPadEnd) = qsHorizontalEdgePadding(editorView, editorDensity, 0.dp)
    val editorFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        editorFocusRequester.requestFocus()
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(600f)
            .focusRequester(editorFocusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (ev.isVolumePanelKey()) return@onPreviewKeyEvent false
                if (ev.key == Key.Back || ev.isEndCallKey()) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
        color = Color(0xF0000000),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = editorPadStart, end = editorPadEnd, top = 8.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFEAF0F6),
                    )
                }
                Text(
                    text = "Edit",
                    color = Color(0xFFEAF0F6),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 30.sp,
                    ),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReset) {
                    Text(
                        "RESET",
                        color = Color(0xFFEAF0F6),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            Text(
                text = "Hold and drag to rearrange tiles",
                color = Color(0xFFB8C1CE),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(items = tiles, key = { _, tile -> tile.id }) { _, tile ->
                    var dragX by remember(tile.id) { mutableFloatStateOf(0f) }
                    var dragY by remember(tile.id) { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = dragX
                                translationY = dragY
                            }
                            .pointerInput(tile.id, tiles.size) {
                                val horizontalThreshold = 58.dp.toPx()
                                val verticalThreshold = 48.dp.toPx()
                                detectDragGesturesAfterLongPress(
                                    onDragEnd = {
                                        dragX = 0f
                                        dragY = 0f
                                    },
                                    onDragCancel = {
                                        dragX = 0f
                                        dragY = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragX += amount.x
                                        dragY += amount.y
                                        val from = tiles.indexOfFirst { it.id == tile.id }
                                        if (from < 0) return@detectDragGesturesAfterLongPress
                                        if (dragX >= horizontalThreshold) {
                                            val to = from + 1
                                            if (to < tiles.size && (from / 2 == to / 2)) {
                                                onMove(from, to)
                                                dragX = 0f
                                                dragY = 0f
                                            }
                                        } else if (dragX <= -horizontalThreshold) {
                                            val to = from - 1
                                            if (to >= 0 && (from / 2 == to / 2)) {
                                                onMove(from, to)
                                                dragX = 0f
                                                dragY = 0f
                                            }
                                        } else if (dragY >= verticalThreshold) {
                                            val to = from + 2
                                            if (to < tiles.size) {
                                                onMove(from, to)
                                                dragX = 0f
                                                dragY = 0f
                                            }
                                        } else if (dragY <= -verticalThreshold) {
                                            val to = from - 2
                                            if (to >= 0) {
                                                onMove(from, to)
                                                dragX = 0f
                                                dragY = 0f
                                            }
                                        }
                                    },
                                )
                            },
                    ) {
                        ClassicQuickTile(
                            tile = tile,
                            onTap = {},
                            onLongPress = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicQuickTile(
    tile: QuickTile,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val darkCell = Color(0xFF191D22)
    val iconBoxColor = if (tile.highlighted) Color(0xFF145A77) else darkCell
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(68.dp)
                .fillMaxHeight()
                .background(iconBoxColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = Color(0xFFEAF0F6),
                modifier = Modifier.size(28.dp),
            )
            if (tile.highlighted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF00B7FF)),
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(darkCell)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tile.title,
                    color = Color(0xFFEAF0F6),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 16.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (tile.subtitle.isNotBlank()) {
                    Text(
                        tile.subtitle,
                        color = Color(0xFFAEB8C5),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinAppToHomeSheet(
    allApps: List<AppEntry>,
    hiddenPackages: Set<String>,
    pinnedPackageNames: Set<String>,
    appIconShape: AppIconShape,
    themePalette: LauncherThemePalette,
    onSelect: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val maxListHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp
    val visibleApps = remember(allApps, hiddenPackages, pinnedPackageNames, query) {
        allApps
            .asSequence()
            .filter { it.packageName !in hiddenPackages }
            .filter { it.packageName != AppsRepository.INTERNAL_SETTINGS_PACKAGE }
            .filter { it.packageName !in pinnedPackageNames }
            .filter {
                query.isBlank() ||
                    it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = themePalette.settingsBg,
        contentColor = themePalette.settingsMenuTitle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                "Add shortcut",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = themePalette.settingsMenuTitle,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Adds an app icon to the strip above the dock.",
                style = MaterialTheme.typography.bodySmall,
                color = themePalette.settingsMenuBody,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search apps", color = themePalette.settingsMenuBody) },
                placeholder = { Text("Type to filter…", color = themePalette.settingsMenuBody.copy(alpha = 0.55f)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFE8EEF7),
                    unfocusedTextColor = Color(0xFFE8EEF7),
                    focusedLabelColor = themePalette.settingsMenuBody,
                    unfocusedLabelColor = themePalette.settingsMenuBody,
                    focusedIndicatorColor = Color(0xFF5B9BD5),
                    unfocusedIndicatorColor = Color(0xFF5F6A78),
                    focusedContainerColor = Color(0xFF1E2430),
                    unfocusedContainerColor = Color(0xFF1E2430),
                    disabledContainerColor = Color(0xFF1E2430),
                    cursorColor = Color(0xFF84D5F6),
                    focusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                    unfocusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                ),
            )
            Spacer(Modifier.height(8.dp))
            if (visibleApps.isEmpty()) {
                Text(
                    "No apps match.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themePalette.settingsMenuBody,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight),
                ) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelect(app) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = app.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(iconMaskShape(appIconShape)),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = themePalette.settingsMenuTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeActionsSheet(
    themePalette: LauncherThemePalette,
    hasWidget: Boolean,
    newHomeGroupEnabled: Boolean,
    pinToHomepageEnabled: Boolean,
    onAddWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWallpaperChooser: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onNewHomeGroup: () -> Unit,
    onPinToHomepage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iconTint = themePalette.settingsMenuBody
    val labelColor = themePalette.settingsMenuTitle
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = themePalette.settingsBg,
        contentColor = labelColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                "Home",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = labelColor,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF2C3340))
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
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = labelColor,
                    )
                }
            }

            if (hasWidget) {
                MenuRow(Icons.Rounded.EventBusy, "Remove widget", onRemoveWidget)
            } else {
                MenuRow(Icons.Rounded.AddAlarm, "Add system widget", onAddWidget)
            }
            if (newHomeGroupEnabled) {
                MenuRow(Icons.Rounded.Folder, "New group", onClick = onNewHomeGroup)
            }
            if (pinToHomepageEnabled) {
                MenuRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add shortcut", onClick = onPinToHomepage)
            }
            MenuRow(
                Icons.Rounded.Settings,
                LocalContext.current.getString(R.string.home_menu_settings_title),
                onOpenSettings,
            )
            MenuRow(Icons.Rounded.Image, "Set wallpaper", onOpenWallpaperChooser)
            MenuRow(Icons.Rounded.Tune, "System settings", onOpenSystemSettings)
            Spacer(Modifier.height(12.dp))
        }
    }
}


@Composable
private fun AppDrawer(
    isActive: Boolean,
    /** When true, dock has only mail + camera (2 focus slots); matches [LauncherPrefs.classicMode]. */
    classicDock: Boolean,
    hapticsEnabled: Boolean,
    hapticIntensity: Int,
    gridPreset: GridPreset,
    gridCells: List<DrawerGridCell>,
    unreadPackages: Set<String>,
    usageStats: Map<String, Long>,
    sortByUsage: Boolean,
    showIconNotifBadge: Boolean,
    onToggleSortByUsage: () -> Unit,
    requestedPage: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    appIconShape: AppIconShape,
    showAppCardBackground: Boolean,
    reorderMode: Boolean,
    movingSlotId: String?,
    onToggleReorder: () -> Unit,
    onPageChange: (Int) -> Unit,
    onDockFocusChanged: (Boolean, Int) -> Unit,
    onDockActivate: (Int) -> Unit,
    onCellTap: (DrawerGridCell) -> Unit,
    onCellLongPress: (DrawerGridCell) -> Unit,
    onStartMove: (String) -> Unit,
    onReorderDrop: (String?) -> Unit,
    onExitToHome: () -> Unit,
    themePalette: LauncherThemePalette,
) {
    val view = LocalView.current
    val outerPadH = 22.dp
    val outerPadV = 11.dp
    val colSpacing = themePalette.appGridColumnSpacingDp.dp
    val rowSpacing = themePalette.appGridRowSpacingDp.dp
    val iconSize = themePalette.appGridIconSizeDp.dp
    val labelSizeSp = themePalette.appCardFontSp.toInt()

    val appsPerPage = gridPreset.rows * gridPreset.cols
    val pages = (gridCells.size + appsPerPage - 1) / appsPerPage
    val drawerPager = rememberPagerState(initialPage = 0, pageCount = { pages.coerceAtLeast(1) })
    var nav by remember { mutableStateOf(NavState()) }
    /** Uptime of last processed directional key (plain ref — no recomposition). Used for quick-pair "hard" detection. */
    val lastDirectionalRef = remember { longArrayOf(0L) }
    /** Uptime of last accepted cursor move; enforces NAV_MOVE_MIN_MS pacing for Q20 trackpad (plain ref). */
    val lastMoveRef = remember { longArrayOf(0L) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cellLayouts = remember { mutableMapOf<String, LayoutCoordinates>() }
    var reorderDragOffset by remember { mutableStateOf(Offset.Zero) }
    var reorderFingerDragging by remember { mutableStateOf(false) }
    // True from long-press pickup until the drop animation finishes — keeps the ghost visible
    // and the source tile hidden across both the drag and the brief drop animation.
    var ghostVisible by remember { mutableStateOf(false) }
    // Slot the drag ghost is currently hovering over (for drop-target highlight ring).
    var hoveredSlotId by remember { mutableStateOf<String?>(null) }
    // Animates the ghost scale: springs to 1.15 on pickup, shrinks on drop.
    val ghostScaleAnim = remember { Animatable(1f) }
    var dragFingerRoot by remember { mutableStateOf(Offset.Zero) }
    var dragOverlayDims by remember {
        mutableStateOf<Triple<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp>?>(null)
    }
    val edgeHoverJobRef = remember { mutableListOf<Job?>(null) }
    var pagerContainerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pagerViewportSize by remember { mutableStateOf(IntSize.Zero) }
    val pagerCoordsRef = remember { mutableListOf<LayoutCoordinates?>(null) }
    val dragFingerRootRef = remember { mutableListOf(Offset.Zero) }
    val pagesRef = remember { mutableListOf(pages) }
    pagesRef[0] = pages
    // Stable refs for haptic inside pointerInput closures (avoids stale captures).
    val hapticsEnabledRef = remember { mutableListOf(hapticsEnabled) }
    hapticsEnabledRef[0] = hapticsEnabled
    val hapticIntensityRef = remember { mutableListOf(hapticIntensity) }
    hapticIntensityRef[0] = hapticIntensity
    // Tracks the uptime of the last page flip to enforce a cooldown between edge-scroll flips.
    val lastPageFlipRef = remember { longArrayOf(0L) }
    // Snapshot of cell bounds captured at drag-start; used for hover-target detection so that
    // live grid reordering (displaySlice) doesn't cause the target to oscillate.
    val originalCellLayoutBoundsRef = remember { mutableListOf(emptyMap<String, Rect>()) }
    // Persists the last drag's moving/target slot IDs through the drop animation so displaySlice
    // doesn't revert to the old order before gridCells reflects the committed new order.
    var pendingDropMoving by remember { mutableStateOf<String?>(null) }
    var pendingDropTarget by remember { mutableStateOf<String?>(null) }
    val drawerContext = LocalContext.current
    val dockKeyNavSize = if (classicDock) 2 else 4

    DisposableEffect(Unit) {
        onDispose {
            edgeHoverJobRef[0]?.cancel()
            edgeHoverJobRef[0] = null
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) focusRequester.requestFocus()
    }
    LaunchedEffect(classicDock) {
        if (nav.area == FocusArea.Dock) {
            nav = NavState(area = FocusArea.Dock, dockIndex = 0)
            onDockFocusChanged(true, 0)
        }
    }
    LaunchedEffect(searchQuery) {
        if (pages > 0) drawerPager.animateScrollToPage(0)
    }
    LaunchedEffect(pages) {
        val last = (pages - 1).coerceAtLeast(0)
        if (drawerPager.currentPage > last) {
            drawerPager.scrollToPage(last)
        }
    }
    androidx.compose.runtime.LaunchedEffect(drawerPager.currentPage) {
        onPageChange(drawerPager.currentPage)
        cellLayouts.clear()
        if (!reorderFingerDragging) {
            reorderDragOffset = Offset.Zero
        } else {
            // Page flipped mid-drag via edge-scroll. Wait a couple of frames for the new
            // page's onGloballyPositioned callbacks to repopulate cellLayouts, then refresh
            // the hover-detection snapshot so hoveredSlotId targets the new page's cells.
            delay(50)
            val snap = cellLayouts.entries
                .filter { it.value.isAttached }
                .associate { (k, v) -> k to v.boundsInRoot() }
            if (snap.isNotEmpty()) originalCellLayoutBoundsRef[0] = snap
        }
    }
    // When the ViewModel clears movingSlotId (after gridCells is committed), drop the pending
    // display hint so displaySlice hands control back to the real gridCells order.
    androidx.compose.runtime.LaunchedEffect(movingSlotId) {
        if (movingSlotId == null) {
            pendingDropMoving = null
            pendingDropTarget = null
        }
    }
    // Light haptic click each time the hover target changes while actively dragging —
    // gives tactile confirmation that an item has shifted under the finger.
    androidx.compose.runtime.LaunchedEffect(hoveredSlotId) {
        if (hoveredSlotId != null && reorderFingerDragging && hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        }
    }
    // Haptic on entering rearrange mode; hard-reset drag state when exiting so that
    // cancelling the pointerInput coroutine (reorderMode key flip) doesn't leave ghost/
    // fingerDragging state dirty.
    androidx.compose.runtime.LaunchedEffect(reorderMode) {
        if (reorderMode) {
            doNavFeedback(view, hapticsEnabled, hapticIntensity)
        } else {
            reorderFingerDragging = false
            hoveredSlotId = null
            pendingDropMoving = null
            pendingDropTarget = null
            edgeHoverJobRef[0]?.cancel()
            edgeHoverJobRef[0] = null
            if (ghostVisible) {
                ghostScaleAnim.snapTo(1f)
                ghostVisible = false
                dragOverlayDims = null
                reorderDragOffset = Offset.Zero
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(requestedPage, pages) {
        val target = requestedPage.coerceIn(0, (pages - 1).coerceAtLeast(0))
        if (requestedPage >= 0 && target != drawerPager.currentPage) {
            drawerPager.animateScrollToPage(target)
        }
    }
    // Match HomePage: keep drawer below status bar when the activity draws edge-to-edge.
    // Sort row is ~32dp tall; when searching that row is hidden — reserve the same height so the grid
    // does not jump up under the status bar.
    val drawerSortHeaderHeight = 32.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            // Flutter `app_grid_theme.dart`: appGridOutterPadding top = 0, bottom = 12 (not 4dp top).
            .padding(start = outerPadH, end = outerPadH, top = 0.dp, bottom = outerPadV)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (ev.isVolumePanelKey()) return@onPreviewKeyEvent false
                // BB Classic red button: always go home from anywhere in the drawer.
                if (ev.isEndCallKey()) { onExitToHome(); return@onPreviewKeyEvent true }
                val searchUpdate = tryConsumeSearchKey(ev, searchQuery)
                if (searchUpdate != null) {
                    onSearchQueryChange(searchUpdate)
                    nav = nav.copy(area = FocusArea.DrawerGrid)
                        return@onPreviewKeyEvent true
                }
                val directional = ev.key == Key.DirectionLeft || ev.key == Key.DirectionRight || ev.key == Key.DirectionUp || ev.key == Key.DirectionDown
                if (!directional) {
                    lastDirectionalRef[0] = 0L
                }
                // Hard = second+ step in quick succession or OS key-repeat — used only for page/dock/home escapes.
                val now = SystemClock.uptimeMillis()
                val repeatHeavy = (ev.nativeKeyEvent?.repeatCount ?: 0) > 0
                val isHard = if (directional) {
                    val quickPair = lastDirectionalRef[0] != 0L && (now - lastDirectionalRef[0]) < NAV_FRAME_MS
                    lastDirectionalRef[0] = now
                    repeatHeavy || quickPair
                } else {
                    false
                }
                val cols = gridPreset.cols
                val appsPerPageLocal = appsPerPage
                val page = drawerPager.currentPage
                val start = page * appsPerPageLocal
                val end = (start + appsPerPageLocal).coerceAtMost(gridCells.size)
                val sliceCount = (end - start).coerceAtLeast(0)

                // Back key always handled the same regardless of focus area.
                if (ev.key == Key.Back) {
                    return@onPreviewKeyEvent when {
                        reorderMode -> { onToggleReorder(); true }
                        searchQuery.isNotEmpty() -> true
                        else -> { onExitToHome(); true }
                    }
                }

                when (nav.area) {
                    FocusArea.Dock -> {
                        // Enter activates the focused dock item.
                        if (ev.key == Key.Enter || ev.key == Key.NumPadEnter) {
                            onDockActivate(nav.dockIndex)
                            return@onPreviewKeyEvent true
                        }
                        if (!directional) return@onPreviewKeyEvent false
                        // Pace left/right dock moves; UP (exit to grid) is not paced.
                        val before = nav
                        val afterDock = nav.onDockKey(ev.key, dockSize = dockKeyNavSize)
                        if (afterDock.area == FocusArea.DrawerGrid) {
                            // Exiting dock upward — no pacing needed.
                            nav = afterDock
                            onDockFocusChanged(false, -1)
                            doNavFeedback(view, hapticsEnabled, hapticIntensity)
                            return@onPreviewKeyEvent true
                        }
                        if (directional && (now - lastMoveRef[0]) < NAV_MOVE_MIN_MS) {
                            return@onPreviewKeyEvent true
                        }
                        lastMoveRef[0] = now
                        nav = afterDock
                        if (nav != before) {
                            onDockFocusChanged(true, nav.dockIndex)
                            doNavFeedback(view, hapticsEnabled, hapticIntensity)
                        }
                        true
                    }

                    FocusArea.DrawerGrid -> {
                        when (ev.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                val idx = nav.gridIndex.coerceIn(0, (sliceCount - 1).coerceAtLeast(0))
                                val cell = if (sliceCount > 0) gridCells[start + idx] else null
                                if (cell != null) onCellTap(cell)
                                true
                            }
                            else -> {
                                val before = nav
                                val currentIndex = nav.gridIndex.coerceIn(0, (sliceCount - 1).coerceAtLeast(0))
                                val currentRow = if (cols <= 0) 0 else currentIndex / cols
                                val currentCol = if (cols <= 0) 0 else currentIndex % cols
                                // Page change at column edges: RIGHT at last col → next page (same row),
                                // LEFT at first col → previous page (same row).
                                val isFirstCol = currentCol == 0
                                val isLastCol = currentCol == cols - 1 || currentIndex == sliceCount - 1

                                // Page switching at column edges — land on same row of target page so
                                // vertical position is preserved across page changes.
                                // nav.gridIndex updated immediately to prevent cascade page changes.
                                if (ev.key == Key.DirectionLeft && isFirstCol && page > 0 && isHard &&
                                    !drawerPager.isScrollInProgress
                                ) {
                                    val prevStart = (page - 1) * appsPerPageLocal
                                    val prevEnd = (prevStart + appsPerPageLocal).coerceAtMost(gridCells.size)
                                    val prevCount = (prevEnd - prevStart).coerceAtLeast(0)
                                    // Same row, last column (or last available item if row is partial).
                                    val rowStart = currentRow * cols
                                    val target = (rowStart + cols - 1).coerceAtMost(prevCount - 1).coerceAtLeast(0)
                                    nav = nav.copy(gridIndex = target)
                                    scope.launch {
                                        drawerPager.animateScrollToPage(page - 1)
                                        doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    }
                                    return@onPreviewKeyEvent true
                                }

                                if (ev.key == Key.DirectionRight && isLastCol && page < pages - 1 && isHard &&
                                    !drawerPager.isScrollInProgress
                                ) {
                                    val nextStart = (page + 1) * appsPerPageLocal
                                    val nextEnd = (nextStart + appsPerPageLocal).coerceAtMost(gridCells.size)
                                    val nextCount = (nextEnd - nextStart).coerceAtLeast(0)
                                    // Same row, first column (coerced if next page is short).
                                    val target = (currentRow * cols).coerceAtMost(nextCount - 1).coerceAtLeast(0)
                                    nav = nav.copy(gridIndex = target)
                                    scope.launch {
                                        drawerPager.animateScrollToPage(page + 1)
                                        doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    }
                                    return@onPreviewKeyEvent true
                                }

                                // Q20 trackpad pacing: limit cursor steps to NAV_MOVE_MIN_MS intervals.
                                // Page/home transitions (checked above) bypass this so edge escapes still fire.
                                if (directional && (now - lastMoveRef[0]) < NAV_MOVE_MIN_MS) {
                                    return@onPreviewKeyEvent true
                                }
                                lastMoveRef[0] = now

                                val afterGrid = nav.onGridKey(ev.key, cols = cols, itemCount = sliceCount)
                                if (afterGrid.area == FocusArea.Dock) {
                                    // Entering dock from bottom of grid.
                                    nav = afterGrid
                                    onDockFocusChanged(true, nav.dockIndex)
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    return@onPreviewKeyEvent true
                                }
                                nav = afterGrid
                                if (nav != before) doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                // Always consume directional keys — even when cursor doesn't move (edge).
                                // Without this, the unconsumed event bubbles to the outer HorizontalPager
                                // (home↔drawer) and causes a phantom swipe-to-home glitch.
                                directional || nav != before
                            }
                        }
                    }
                }
            }
    ) {
        if (searchQuery.isNotEmpty()) {
            val extras = remember(searchQuery) {
                buildSearchExtras(drawerContext, searchQuery) {
                    Toast.makeText(drawerContext, "Could not open that screen", Toast.LENGTH_SHORT).show()
                }
            }
            if (extras.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 132.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    extras.forEach { ex ->
                        TextButton(
                            onClick = ex.onOpen,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(ex.title, color = themePalette.settingsMenuTitle)
                                Text(
                                    ex.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themePalette.settingsMenuBody,
                                )
                            }
                        }
                    }
                }
            }
            val pq = searchQuery.trim().lowercase()
            if (pq == "private" || pq.startsWith("private ")) {
                Text(
                    text = "Hidden apps: keep typing to filter the list, or use Hide in an app’s menu to manage hidden apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = privateSearchHintColor(themePalette),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                )
            }
            if (gridCells.isEmpty()) {
                Text(
                    text = "No apps found",
                    color = themePalette.settingsMenuBody,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                TextButton(
                    onClick = { openPlayStoreSearch(drawerContext, searchQuery) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = "Search \"$searchQuery\" on Play Store",
                        color = Color(0xFF84D5F6),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        // Sort toggle — hidden while searching, but keep the same vertical slot so the grid stays put.
        if (searchQuery.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (sortByUsage) {
                    Text(
                        text = "Most used",
                        fontSize = 11.sp,
                        color = themePalette.settingsMenuBody,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                IconButton(
                    onClick = onToggleSortByUsage,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QueryStats,
                        contentDescription = "Sort by usage",
                        tint = if (sortByUsage) themePalette.settingsMenuTitle else themePalette.settingsMenuBody.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else {
            Spacer(Modifier.height(drawerSortHeaderHeight))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { pagerViewportSize = it }
                .onGloballyPositioned {
                    pagerContainerCoords = it
                    pagerCoordsRef[0] = it
                },
        ) {
            HorizontalPager(
                state = drawerPager,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
            val start = page * appsPerPage
                val end = (start + appsPerPage).coerceAtMost(gridCells.size)
                val slice = if (start < end) gridCells.subList(start, end) else emptyList()
                // Live preview: reorder this page's items to show where the dragged app will land.
                // pendingDropMoving/Target keep this order alive through the drop animation until
                // movingSlotId goes null (after gridCells commits the new order).
                val displaySlice = run {
                    val moving = movingSlotId ?: pendingDropMoving
                    val hovered = hoveredSlotId ?: pendingDropTarget
                    if (moving == null || hovered == null) slice
                    else {
                        val fromIdx = slice.indexOfFirst { it.slotId == moving }
                        val toIdx = slice.indexOfFirst { it.slotId == hovered }
                        if (fromIdx < 0 || toIdx < 0 || fromIdx == toIdx) slice
                        else {
                            val mutable = slice.toMutableList()
                            mutable.add(toIdx, mutable.removeAt(fromIdx))
                            mutable
                        }
                    }
                }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Match AppTile/FolderTile vertical budget (icon pad top, max icon pad bottom, label block).
                // Prefer stable pager viewport dimensions to avoid first-open transient undersizing.
                val viewportWidth = if (pagerViewportSize.width > 0) {
                    with(density) { pagerViewportSize.width.toDp() }
                } else maxWidth
                val viewportHeight = if (pagerViewportSize.height > 0) {
                    with(density) { pagerViewportSize.height.toDp() }
                } else maxHeight
                val minLabelBlock = with(density) { (labelSizeSp * 2.55f).sp.toDp() }
                val minCellHeight = 3.dp + iconSize + 14.dp + 2.dp + minLabelBlock
                val minCellWidth = iconSize + 14.dp
                val cardW =
                    ((viewportWidth - colSpacing * (gridPreset.cols - 1)) / gridPreset.cols)
                        .coerceAtLeast(minCellWidth)
                val cardH =
                    ((viewportHeight - rowSpacing * (gridPreset.rows - 1)) / gridPreset.rows)
                        .coerceAtLeast(minCellHeight)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridPreset.cols),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(colSpacing),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(rowSpacing),
                ) {
                        items(displaySlice, key = { it.slotId }) { cell ->
                            val slot = cell.slotId
                            val itemAnimModifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = spring(
                                    stiffness = Spring.StiffnessMedium,
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                ),
                            )
                        val reorderDragModifier =
                                if (reorderMode) {
                                    itemAnimModifier.then(Modifier.pointerInput(
                                        reorderMode,
                                        slot,
                                        page,
                                        pages,
                                        themePalette.appGridEdgeHoverZoneWidthDp,
                                        themePalette.appGridEdgeHoverDurationMs,
                                    ) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { startOffset ->
                                                // Haptic: soft pickup buzz at the moment long-press fires.
                                                doNavFeedback(view, hapticsEnabledRef[0], hapticIntensityRef[0])
                                                onStartMove(slot)
                                                // Snapshot current bounds before any live reordering shifts items.
                                                originalCellLayoutBoundsRef[0] = cellLayouts.entries
                                                    .filter { it.value.isAttached }
                                                    .associate { (k, v) -> k to v.boundsInRoot() }
                                                reorderFingerDragging = true
                                                ghostVisible = true
                                                dragOverlayDims = Triple(cardW, cardH, iconSize)
                                                val src = cellLayouts[slot]
                                                if (src != null) {
                                                    dragFingerRoot = src.localToRoot(startOffset)
                                                    dragFingerRootRef[0] = dragFingerRoot
                                                }
                                                // Spring the ghost up to 115% to give a "lifted" feel.
                                                scope.launch {
                                                    ghostScaleAnim.animateTo(
                                                        1.15f,
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMedium,
                                                        ),
                                                    )
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                // slot is stable (captured from the LazyColumn item scope),
                                                // so we never need to cross-check movingSlotId here —
                                                // this gesture coroutine can only fire on the tile that
                                                // received the long press.
                                                reorderDragOffset += dragAmount
                                                change.consume()
                                                val src = cellLayouts[slot]
                                                if (src != null && src.isAttached) {
                                                    dragFingerRoot = src.localToRoot(change.position)
                                                } else {
                                                    dragFingerRoot = Offset(
                                                        dragFingerRoot.x + dragAmount.x,
                                                        dragFingerRoot.y + dragAmount.y,
                                                    )
                                                }
                                                dragFingerRootRef[0] = dragFingerRoot
                                                val pc = pagerCoordsRef[0] ?: return@detectDragGesturesAfterLongPress
                                                val bb = pc.boundsInRoot()
                                                val localX = dragFingerRoot.x - bb.left
                                                val zonePx = with(density) {
                                                    themePalette.appGridEdgeHoverZoneWidthDp.dp.toPx()
                                                }
                                                val inEdge = localX < zonePx || localX > bb.width - zonePx
                                                // 1200 ms cooldown between successive page flips so
                                                // a slow drag in the edge zone doesn't race through pages.
                                                val flipCooldownMs = 1200L
                                                val sinceLastFlip = android.os.SystemClock.uptimeMillis() - lastPageFlipRef[0]
                                                if (reorderFingerDragging && pages > 1 && inEdge && sinceLastFlip >= flipCooldownMs) {
                                                    if (edgeHoverJobRef[0]?.isActive != true) {
                                                        edgeHoverJobRef[0] = scope.launch {
                                                            delay(themePalette.appGridEdgeHoverDurationMs)
                                                            edgeHoverJobRef[0] = null
                                                            val b2 = pagerCoordsRef[0]?.boundsInRoot()
                                                                ?: return@launch
                                                            val lx = dragFingerRootRef[0].x - b2.left
                                                            val z = with(density) {
                                                                themePalette.appGridEdgeHoverZoneWidthDp.dp.toPx()
                                                            }
                                                            val cp = drawerPager.currentPage
                                                            val pageCount = pagesRef[0].coerceAtLeast(1)
                                                            if (lx < z && cp > 0) {
                                                                lastPageFlipRef[0] = android.os.SystemClock.uptimeMillis()
                                                                drawerPager.animateScrollToPage(cp - 1)
                                                            } else if (lx > b2.width - z && cp < pageCount - 1) {
                                                                lastPageFlipRef[0] = android.os.SystemClock.uptimeMillis()
                                                                drawerPager.animateScrollToPage(cp + 1)
                                                            }
                                                        }
                                                    }
                                                } else if (!inEdge) {
                                                    edgeHoverJobRef[0]?.cancel()
                                                    edgeHoverJobRef[0] = null
                                                }
                                                // Update hover target using the original (pre-drag) bounds
                                                // so that live grid reordering doesn't cause oscillation.
                                                hoveredSlotId = originalCellLayoutBoundsRef[0].entries
                                                    .filter { it.key != slot }
                                                    .minByOrNull { (_, bounds) ->
                                                        val c = bounds.center
                                                        hypot(
                                                            (c.x - dragFingerRoot.x).toDouble(),
                                                            (c.y - dragFingerRoot.y).toDouble(),
                                                        )
                                                    }?.key
                                            },
                                            onDragEnd = {
                                                edgeHoverJobRef[0]?.cancel()
                                                edgeHoverJobRef[0] = null
                                                reorderFingerDragging = false
                                                val target = hoveredSlotId
                                                // Lock in the preview order for the drop animation window.
                                                // displaySlice uses these until movingSlotId goes null
                                                // (i.e. after the ViewModel has saved the new order).
                                                pendingDropMoving = slot
                                                pendingDropTarget = target
                                                hoveredSlotId = null
                                                // Drop animation: ghost shrinks before disappearing,
                                                // then onReorderDrop fires and cleans up state.
                                                if (target != null) {
                                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                }
                                                scope.launch {
                                                    val targetCenter = target?.let {
                                                        originalCellLayoutBoundsRef[0][it]?.center
                                                    }
                                                    if (targetCenter != null) {
                                                        // Ghost flies to the drop slot while scaling to zero.
                                                        // Target's original center = where the moving item
                                                        // will appear in displaySlice, so the ghost lands
                                                        // exactly on the final tile position.
                                                        val startPos = dragFingerRoot
                                                        kotlinx.coroutines.coroutineScope {
                                                            launch {
                                                                androidx.compose.animation.core.animate(
                                                                    0f, 1f,
                                                                    animationSpec = tween(130, easing = FastOutSlowInEasing),
                                                                ) { p, _ ->
                                                                    dragFingerRoot = Offset(
                                                                        startPos.x + (targetCenter.x - startPos.x) * p,
                                                                        startPos.y + (targetCenter.y - startPos.y) * p,
                                                                    )
                                                                }
                                                            }
                                                            launch {
                                                                ghostScaleAnim.animateTo(
                                                                    0f,
                                                                    tween(130, easing = FastOutSlowInEasing),
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        ghostScaleAnim.animateTo(
                                                            0f,
                                                            tween(80, easing = FastOutSlowInEasing),
                                                        )
                                                    }
                                                    onReorderDrop(target)
                                                    ghostScaleAnim.snapTo(1f)
                                                    ghostVisible = false
                                                    dragOverlayDims = null
                                                    reorderDragOffset = Offset.Zero
                                                }
                                            },
                                            onDragCancel = {
                                                edgeHoverJobRef[0]?.cancel()
                                                edgeHoverJobRef[0] = null
                                                reorderFingerDragging = false
                                                hoveredSlotId = null
                                                pendingDropMoving = null
                                                pendingDropTarget = null
                                                ghostVisible = false
                                                dragOverlayDims = null
                                                scope.launch { ghostScaleAnim.snapTo(1f) }
                                                reorderDragOffset = Offset.Zero
                                            },
                                        )
                                    })
                                } else {
                                    itemAnimModifier
                                }
                            val focusedHere =
                                nav.area == FocusArea.DrawerGrid && displaySlice.getOrNull(nav.gridIndex)?.slotId == slot
                            val isMovingThisTile = ghostVisible && movingSlotId == slot
                            // During active drag the live grid preview makes the drop ring redundant;
                            // only show it in the brief window after finger lifts (drop animation).
                            val isDropTarget = reorderMode && ghostVisible && !reorderFingerDragging &&
                                slot == hoveredSlotId && slot != movingSlotId
                            when (cell) {
                                is DrawerGridCell.App -> {
                                    val app = cell.entry
                                    AppTile(
                                        app = app,
                                        reorderMode = reorderMode,
                                        appIconShape = appIconShape,
                                        selected = movingSlotId == slot || focusedHere,
                                        isDropTarget = isDropTarget,
                                        width = cardW,
                                        height = cardH,
                                        iconSize = iconSize,
                                        labelSizeSp = labelSizeSp,
                                        cardTop = themePalette.appCardTop,
                                        cardBottom = themePalette.appCardBottom,
                                        showAppCardBackground = showAppCardBackground,
                                        themePalette = themePalette,
                                        reorderDragModifier = reorderDragModifier,
                                        isFingerDraggingThisTile = isMovingThisTile,
                                        reorderDragOffset = reorderDragOffset,
                                        hideSourceWhileFingerDragging = isMovingThisTile,
                                        usageTimeMs = usageStats[app.packageName] ?: 0L,
                                        hasNotifBadge = showIconNotifBadge && app.packageName in unreadPackages,
                                        onGloballyPositioned = { coords ->
                                            cellLayouts[app.packageName] = coords
                                        },
                                        onClick = { onCellTap(cell) },
                                        onLongPress = { onCellLongPress(cell) },
                                    )
                                }
                                is DrawerGridCell.Folder -> {
                                    FolderTile(
                                        displayLabel = cell.displayTitle,
                                        members = cell.members,
                                        appIconShape = appIconShape,
                                        hasUnreadBadge = showIconNotifBadge && cell.members.any { it.packageName in unreadPackages },
                                        reorderMode = reorderMode,
                                        selected = movingSlotId == slot || focusedHere,
                                        isDropTarget = isDropTarget,
                                        width = cardW,
                                        height = cardH,
                                        iconSize = iconSize,
                                        labelSizeSp = labelSizeSp,
                                        cardTop = themePalette.appCardTop,
                                        cardBottom = themePalette.appCardBottom,
                                        showAppCardBackground = showAppCardBackground,
                                        themePalette = themePalette,
                                        reorderDragModifier = reorderDragModifier,
                                        isFingerDraggingThisTile = isMovingThisTile,
                                        reorderDragOffset = reorderDragOffset,
                                        hideSourceWhileFingerDragging = isMovingThisTile,
                                        onGloballyPositioned = { coords ->
                                            cellLayouts[cell.id] = coords
                                        },
                                        onClick = { onCellTap(cell) },
                                        onLongPress = { onCellLongPress(cell) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val dragCell = movingSlotId?.let { id -> gridCells.find { it.slotId == id } }
            val dims = dragOverlayDims
            if (ghostVisible && dragCell != null && dims != null && pagerContainerCoords != null) {
                val b = pagerContainerCoords!!.boundsInRoot()
                val (cw, ch, isz) = dims
                val xDp = with(density) { (dragFingerRoot.x - b.left).toDp() } - cw / 2
                val yDp = with(density) { (dragFingerRoot.y - b.top).toDp() } - ch / 2
                Box(
                    Modifier
                        .offset(xDp, yDp)
                        .zIndex(4f)
                        .graphicsLayer {
                            scaleX = ghostScaleAnim.value
                            scaleY = ghostScaleAnim.value
                        },
                ) {
                    when (dragCell) {
                        is DrawerGridCell.App -> AppTile(
                            app = dragCell.entry,
                            reorderMode = false,
                            appIconShape = appIconShape,
                            selected = false,
                            width = cw,
                            height = ch,
                            iconSize = isz,
                            labelSizeSp = labelSizeSp,
                            cardTop = themePalette.appCardTop,
                            cardBottom = themePalette.appCardBottom,
                            showAppCardBackground = showAppCardBackground,
                            themePalette = themePalette,
                            reorderDragModifier = Modifier,
                            isFingerDraggingThisTile = true,
                            reorderDragOffset = Offset.Zero,
                            hideSourceWhileFingerDragging = false,
                            onGloballyPositioned = {},
                            onClick = {},
                            onLongPress = {},
                        )
                        is DrawerGridCell.Folder -> FolderTile(
                            displayLabel = dragCell.displayTitle,
                            members = dragCell.members,
                            appIconShape = appIconShape,
                            hasUnreadBadge = dragCell.members.any { it.packageName in unreadPackages },
                            reorderMode = false,
                            selected = false,
                            width = cw,
                            height = ch,
                            iconSize = isz,
                            labelSizeSp = labelSizeSp,
                            cardTop = themePalette.appCardTop,
                            cardBottom = themePalette.appCardBottom,
                            showAppCardBackground = showAppCardBackground,
                            themePalette = themePalette,
                            reorderDragModifier = Modifier,
                            isFingerDraggingThisTile = true,
                            reorderDragOffset = Offset.Zero,
                            hideSourceWhileFingerDragging = false,
                            onGloballyPositioned = {},
                            onClick = {},
                            onLongPress = {},
                        )
                    }
                }
            }
        }
    }
}

private data class SettingsSearchEntry(
    val label: String,
    val keywords: List<String>,
    val action: String,
    val fallbackAction: String = Settings.ACTION_SETTINGS,
)

private val SETTINGS_SEARCH_ENTRIES = listOf(
    SettingsSearchEntry("Wi-Fi", listOf("wifi", "wi-fi", "wireless", "internet", "network"), Settings.ACTION_WIFI_SETTINGS),
    SettingsSearchEntry("Bluetooth", listOf("bluetooth", "bt", "pair", "headphone", "speaker", "earbuds"), Settings.ACTION_BLUETOOTH_SETTINGS),
    SettingsSearchEntry("Mobile Data", listOf("mobile data", "cellular", "data", "sim", "roaming", "4g", "5g", "lte"), Settings.ACTION_DATA_ROAMING_SETTINGS),
    SettingsSearchEntry("Airplane Mode", listOf("airplane", "flight mode", "aeroplane", "offline"), Settings.ACTION_AIRPLANE_MODE_SETTINGS),
    SettingsSearchEntry("Hotspot & Tethering", listOf("hotspot", "tethering", "share wifi", "personal hotspot"), Settings.ACTION_WIRELESS_SETTINGS),
    SettingsSearchEntry("Display", listOf("display", "screen", "resolution", "refresh rate", "adaptive"), Settings.ACTION_DISPLAY_SETTINGS),
    SettingsSearchEntry("Brightness", listOf("brightness", "dim", "auto brightness", "adaptive brightness"), Settings.ACTION_DISPLAY_SETTINGS),
    SettingsSearchEntry("Dark Mode", listOf("dark mode", "night mode", "dark theme", "light mode", "dark"), Settings.ACTION_DISPLAY_SETTINGS),
    SettingsSearchEntry("Screen Timeout", listOf("timeout", "sleep", "auto lock", "screen off", "always on"), Settings.ACTION_DISPLAY_SETTINGS),
    SettingsSearchEntry("Font Size", listOf("font", "font size", "text size", "large text"), Settings.ACTION_ACCESSIBILITY_SETTINGS),
    SettingsSearchEntry("Auto-rotate", listOf("rotate", "auto rotate", "rotation", "orientation", "portrait", "landscape"), Settings.ACTION_DISPLAY_SETTINGS),
    SettingsSearchEntry("Sound & Vibration", listOf("sound", "volume", "vibration", "vibrate", "silent", "mute", "ring"), Settings.ACTION_SOUND_SETTINGS),
    SettingsSearchEntry("Ringtone", listOf("ringtone", "notification tone", "alarm tone", "call sound"), Settings.ACTION_SOUND_SETTINGS),
    SettingsSearchEntry("Do Not Disturb", listOf("do not disturb", "dnd", "quiet", "focus", "priority mode"), Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
    SettingsSearchEntry("Notifications", listOf("notification", "alerts", "badge", "banner", "app notification"), "android.settings.NOTIFICATION_SETTINGS"),
    SettingsSearchEntry("Battery", listOf("battery", "power", "charging", "battery saver", "low power", "usage"), Settings.ACTION_BATTERY_SAVER_SETTINGS),
    SettingsSearchEntry("Storage", listOf("storage", "memory", "space", "disk", "files", "clear cache"), Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
    SettingsSearchEntry("Location", listOf("location", "gps", "maps", "find my", "google location", "tracking"), Settings.ACTION_LOCATION_SOURCE_SETTINGS),
    SettingsSearchEntry("Screen Lock & Security", listOf("security", "screen lock", "pin", "password", "pattern", "lock", "unlock"), Settings.ACTION_SECURITY_SETTINGS),
    SettingsSearchEntry("Fingerprint", listOf("fingerprint", "biometric", "touch id", "finger"), Settings.ACTION_SECURITY_SETTINGS),
    SettingsSearchEntry("Face Unlock", listOf("face", "face unlock", "face recognition", "face id"), Settings.ACTION_SECURITY_SETTINGS),
    SettingsSearchEntry("Privacy", listOf("privacy", "permissions", "tracking", "data privacy", "app permissions"), Settings.ACTION_PRIVACY_SETTINGS),
    SettingsSearchEntry("App Permissions", listOf("permission", "camera permission", "mic permission", "contacts permission", "allow"), Settings.ACTION_APPLICATION_SETTINGS),
    SettingsSearchEntry("Accessibility", listOf("accessibility", "magnifier", "color correction", "caption", "talkback"), Settings.ACTION_ACCESSIBILITY_SETTINGS),
    SettingsSearchEntry("Language & Input", listOf("language", "keyboard", "input method", "autocorrect", "spell check", "gboard"), Settings.ACTION_LOCALE_SETTINGS),
    SettingsSearchEntry("Date & Time", listOf("date", "time", "timezone", "clock", "24 hour", "ntp"), Settings.ACTION_DATE_SETTINGS),
    SettingsSearchEntry("VPN", listOf("vpn", "virtual private network", "proxy"), Settings.ACTION_VPN_SETTINGS),
    SettingsSearchEntry("NFC", listOf("nfc", "tap to pay", "contactless", "near field", "google pay", "payments"), Settings.ACTION_NFC_SETTINGS),
    SettingsSearchEntry("Accounts & Sync", listOf("account", "sync", "google account", "email account", "add account"), Settings.ACTION_SYNC_SETTINGS),
    SettingsSearchEntry("Default Apps", listOf("default app", "default browser", "default launcher", "default camera", "default"), Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
    SettingsSearchEntry("Developer Options", listOf("developer", "developer options", "dev options"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("USB Debugging", listOf("usb debug", "adb", "usb adb", "android debug bridge"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("Wireless Debugging", listOf("wireless debug", "wifi adb", "wireless adb", "adb wifi"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("Animation Scale", listOf("animation", "animation speed", "transition animation", "animator", "window animation", "slow animation"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("Mock Location", listOf("mock location", "fake gps", "fake location", "spoof location"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("GPU Overdraw", listOf("gpu", "overdraw", "gpu rendering", "profile gpu", "hardware layer"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("Background Process Limit", listOf("background process", "background limit", "process limit", "running services"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("OEM Unlock", listOf("oem unlock", "bootloader", "unlock bootloader"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("Stay Awake", listOf("stay awake", "screen awake", "screen on charging", "keep screen on"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("Bluetooth HCI Log", listOf("bluetooth log", "hci", "hci snoop", "bluetooth debug"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("Bug Report", listOf("bug report", "bugreport", "report bug", "capture logs"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("Show Touches", listOf("show touches", "touch indicator", "show tap", "pointer location"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("Don't Keep Activities", listOf("dont keep activities", "don't keep activities", "destroy activity", "activity limit"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    SettingsSearchEntry("About Phone", listOf("about", "android version", "build number", "software version", "imei", "model"), Settings.ACTION_DEVICE_INFO_SETTINGS),
    SettingsSearchEntry("Software Update", listOf("update", "software update", "system update", "ota", "upgrade"), "android.settings.SYSTEM_UPDATE_SETTINGS"),
    SettingsSearchEntry("Cast / Screen Share", listOf("cast", "screen mirror", "chromecast", "screen share", "wireless display"), Settings.ACTION_CAST_SETTINGS),
    SettingsSearchEntry("Mobile Network", listOf("mobile network", "apn", "carrier", "operator", "preferred network", "roam"), Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
)

private fun matchSettingsEntries(query: String): List<SettingsSearchEntry> {
    if (query.length < 2) return emptyList()
    val q = query.lowercase()
    return SETTINGS_SEARCH_ENTRIES.filter { entry ->
        entry.label.contains(q, ignoreCase = true) ||
            entry.keywords.any { it.contains(q, ignoreCase = true) || q.contains(it, ignoreCase = true) }
    }.take(3)
}

/** @return new query if key updates search; null if not a text/search key. */
private fun tryConsumeSearchKey(ev: KeyEvent, searchQuery: String): String? {
    if (ev.type != KeyEventType.KeyDown) return null
    if (ev.key == Key.Backspace && searchQuery.isNotEmpty()) return searchQuery.dropLast(1)
    if (ev.key == Key.Back && searchQuery.isNotEmpty()) return ""
    val native = ev.nativeKeyEvent
    val uc = native?.getUnicodeChar(native.metaState) ?: 0
    if (uc != 0 && Character.isValidCodePoint(uc)) {
        runCatching { String(Character.toChars(uc)) }.getOrNull()?.let { str ->
            if (str.isNotEmpty() && !str.first().isISOControl()) return searchQuery + str
        }
    }
    val typed = keyToTypedChar(ev.key)
    if (typed != null) return searchQuery + typed
    return null
}

private fun openPlayStoreSearch(context: android.content.Context, query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    val encoded = Uri.encode(trimmed)
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$encoded"))
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/search?q=$encoded&c=apps"),
    )
    runCatching { context.startActivity(marketIntent) }
        .onFailure {
            runCatching { context.startActivity(webIntent) }
                .onFailure {
                    Toast.makeText(context, "Could not open Play Store", Toast.LENGTH_SHORT).show()
                }
        }
}

private fun keyToTypedChar(key: Key): Char? = when (key) {
    Key.A -> 'a'
    Key.B -> 'b'
    Key.C -> 'c'
    Key.D -> 'd'
    Key.E -> 'e'
    Key.F -> 'f'
    Key.G -> 'g'
    Key.H -> 'h'
    Key.I -> 'i'
    Key.J -> 'j'
    Key.K -> 'k'
    Key.L -> 'l'
    Key.M -> 'm'
    Key.N -> 'n'
    Key.O -> 'o'
    Key.P -> 'p'
    Key.Q -> 'q'
    Key.R -> 'r'
    Key.S -> 's'
    Key.T -> 't'
    Key.U -> 'u'
    Key.V -> 'v'
    Key.W -> 'w'
    Key.X -> 'x'
    Key.Y -> 'y'
    Key.Z -> 'z'
    Key.Zero -> '0'
    Key.One -> '1'
    Key.Two -> '2'
    Key.Three -> '3'
    Key.Four -> '4'
    Key.Five -> '5'
    Key.Six -> '6'
    Key.Seven -> '7'
    Key.Eight -> '8'
    Key.Nine -> '9'
    Key.Spacebar -> ' '
    else -> null
}

private fun doNavFeedback(view: android.view.View, hapticsEnabled: Boolean, intensity: Int = 3) {
    if (!hapticsEnabled) return
    // Map intensity 1–5 to vibration amplitude 30–255. Use VibrationEffect for fine-grained control.
    // Falls back to VIRTUAL_KEY if amplitude control is unavailable (older devices/ROMs).
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        view.context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        view.context.getSystemService(android.os.Vibrator::class.java)
    }
    if (vibrator != null && vibrator.hasAmplitudeControl()) {
        val amplitude = when (intensity.coerceIn(1, 5)) {
            1 -> 30
            2 -> 70
            3 -> 120
            4 -> 180
            else -> 255
        }
        vibrator.vibrate(android.os.VibrationEffect.createOneShot(15, amplitude))
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}

@Composable
private fun FolderTile(
    displayLabel: String,
    members: List<AppEntry>,
    appIconShape: AppIconShape,
    reorderMode: Boolean,
    selected: Boolean,
    hasUnreadBadge: Boolean,
    isDropTarget: Boolean = false,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    labelSizeSp: Int,
    cardTop: Color,
    cardBottom: Color,
    showAppCardBackground: Boolean = false,
    themePalette: LauncherThemePalette,
    reorderDragModifier: Modifier = Modifier,
    isFingerDraggingThisTile: Boolean = false,
    reorderDragOffset: Offset = Offset.Zero,
    hideSourceWhileFingerDragging: Boolean = false,
    onGloballyPositioned: (LayoutCoordinates) -> Unit = {},
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    /** When true, [reorderDragModifier] owns long-press (e.g. home strip); inner tap must not register long-press. */
    stripOuterDragOwnsLongPress: Boolean = false,
) {
    /** Match app-tile geometry so folder labels/focus look identical in the drawer. */
    val folderShape = RoundedCornerShape(8.dp)
    val folderBg = Color(0xD9181C24)
    val folderBorderIdle = Color(0x38FFFFFF)
    val cardRadius = themePalette.appCardCornerRadiusDp.dp
    val selRadius = themePalette.selectorBorderRadiusDp.dp
    val phaseFlipped = remember(displayLabel) { displayLabel.hashCode() and 1 != 0 }
    val wiggle = rememberInfiniteTransition(label = "folderWiggle")
    val wiggleRotation by wiggle.animateFloat(
        initialValue = if (phaseFlipped) 2.8f else -2.8f,
        targetValue = if (phaseFlipped) -2.8f else 2.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wiggleRotation",
    )
    val showLiftInPlace = isFingerDraggingThisTile && !hideSourceWhileFingerDragging
    val wiggleStrength by animateFloatAsState(
        targetValue = if (reorderMode && !showLiftInPlace) 1f else 0f,
        animationSpec = tween(200),
        label = "folderWiggleStrength",
    )
    val density = LocalDensity.current
    val iconPadTop = 3.dp
    val textPadBottom = 2.dp
    // Keep folder tile vertical geometry in lockstep with AppTile so label baseline matches.
    val (iconSizeUsed, iconPadBottom) = remember(height, iconSize, labelSizeSp, density) {
        val minLabel = with(density) { (labelSizeSp * 2.55f).sp.toDp() }
        var sz = iconSize
        var pad = (height - iconPadTop - sz - textPadBottom - minLabel).coerceIn(3.dp, 14.dp)
        repeat(10) {
            val labelSpace = height - iconPadTop - sz - pad - textPadBottom
            if (labelSpace >= minLabel) return@remember Pair(sz, pad)
            if (sz <= 40.dp) return@remember Pair(sz, pad)
            sz -= 3.dp
            pad = (height - iconPadTop - sz - textPadBottom - minLabel).coerceIn(3.dp, 14.dp)
        }
        Pair(sz.coerceAtLeast(40.dp), pad)
    }
    val preview = members.take(4)
    val inset = 5.dp
    val gap = 2.dp
    val half = (iconSizeUsed - inset * 2 - gap) / 2
    Box(
        modifier = Modifier
            .zIndex(if (isFingerDraggingThisTile) 1f else 0f)
            .onGloballyPositioned(onGloballyPositioned)
            .size(width, height)
            .graphicsLayer {
                if (showLiftInPlace) {
                    translationX = reorderDragOffset.x
                    translationY = reorderDragOffset.y
                    alpha = 0.92f
                    shadowElevation = 12f
                } else {
                    rotationZ = wiggleRotation * wiggleStrength
                }
            }
            .alpha(animateFloatAsState(if (hideSourceWhileFingerDragging && isFingerDraggingThisTile) 0f else 1f, label = "folderTileAlpha").value)
            .pointerInput(reorderMode, stripOuterDragOwnsLongPress) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = when {
                        stripOuterDragOwnsLongPress || reorderMode -> null
                        else -> { _ -> onLongPress() }
                    },
                )
            }
            .then(reorderDragModifier),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(selRadius))
                    .background(themePalette.selectorBackgroundColour)
                    .border(
                        width = 1.dp,
                        color = themePalette.selectorBorderColour,
                        shape = RoundedCornerShape(selRadius),
                    ),
            )
        } else if (showAppCardBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cardRadius))
                    .background(brush = Brush.verticalGradient(listOf(cardTop, cardBottom)))
                    .border(
                        width = 0.8.dp,
                        color = Color(0x332F3B4F),
                        shape = RoundedCornerShape(cardRadius),
                    ),
            )
        }
        // Drop-target highlight ring — shown while a drag ghost hovers over this slot.
        if (isDropTarget) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(selRadius))
                    .border(2.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(selRadius)),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = iconPadTop, bottom = iconPadBottom, start = 6.dp, end = 6.dp)
                    .size(iconSizeUsed),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(folderShape)
                        .background(folderBg)
                        .border(
                            width = 0.5.dp,
                            color = folderBorderIdle,
                            shape = folderShape,
                        ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inset),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    preview.chunked(2).forEach { rowApps ->
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap),
                    ) {
                            rowApps.forEach { app ->
                                AsyncImage(
                                    model = app.icon,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(half).clip(iconMaskShape(appIconShape)),
                                )
                            }
                            if (rowApps.size == 1) {
                                Spacer(Modifier.size(half))
                            }
                        }
                    }
                }
                if (hasUnreadBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\u2731",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontSize = 9.sp,
                                lineHeight = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = textPadBottom, start = 3.dp, end = 3.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                OutlinedLabel(
                    text = displayLabel,
                    fontSizeSp = labelSizeSp,
                    textColor = themePalette.appCardTextColour,
                    outlineColor = themePalette.appCardTextOutlineColour,
                )
            }
        }
    }
}

@Composable
private fun AppTile(
    app: AppEntry,
    reorderMode: Boolean,
    appIconShape: AppIconShape,
    selected: Boolean,
    isDropTarget: Boolean = false,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    labelSizeSp: Int,
    cardTop: Color,
    cardBottom: Color,
    showAppCardBackground: Boolean = false,
    themePalette: LauncherThemePalette,
    reorderDragModifier: Modifier = Modifier,
    isFingerDraggingThisTile: Boolean = false,
    reorderDragOffset: Offset = Offset.Zero,
    /** When true, the tile stays hit-testable but the lifted drag preview is drawn in a drawer-level overlay. */
    hideSourceWhileFingerDragging: Boolean = false,
    usageTimeMs: Long = 0L,
    hasNotifBadge: Boolean = false,
    onGloballyPositioned: (LayoutCoordinates) -> Unit = {},
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    /** When true, [reorderDragModifier] owns long-press (e.g. home strip); inner tap must not register long-press. */
    stripOuterDragOwnsLongPress: Boolean = false,
) {
    // Flutter `app_card.dart` uses the raw title; no artificial line breaks.
    val displayLabel = app.label
    val cardRadius = themePalette.appCardCornerRadiusDp.dp
    val selRadius = themePalette.selectorBorderRadiusDp.dp
    // Phase-stagger: half the tiles start at +peak, half at –peak, so they don't lockstep.
    val phaseFlipped = remember(app.packageName) { app.packageName.hashCode() and 1 != 0 }
    val wiggle = rememberInfiniteTransition(label = "wiggle")
    // LinearEasing gives constant angular velocity so the icon decelerates smoothly at each
    // extreme — no asymmetric snap that FastOutSlowInEasing causes in reverse cycles.
    val wiggleRotation by wiggle.animateFloat(
        initialValue = if (phaseFlipped) 2.8f else -2.8f,
        targetValue = if (phaseFlipped) -2.8f else 2.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wiggleRotation",
    )

    val showLiftInPlace = isFingerDraggingThisTile && !hideSourceWhileFingerDragging
    // wiggleStrength fades 0→1 on entry and 1→0 on exit without chasing the live wiggle value.
    val wiggleStrength by animateFloatAsState(
        targetValue = if (reorderMode && !showLiftInPlace) 1f else 0f,
        animationSpec = tween(200),
        label = "appWiggleStrength",
    )
    Box(
        modifier = Modifier
            .zIndex(if (isFingerDraggingThisTile) 1f else 0f)
            .onGloballyPositioned(onGloballyPositioned)
            .size(width, height)
            .graphicsLayer {
                if (showLiftInPlace) {
                    translationX = reorderDragOffset.x
                    translationY = reorderDragOffset.y
                    alpha = 0.92f
                    shadowElevation = 12f
                } else {
                    rotationZ = wiggleRotation * wiggleStrength
                }
            }
            .alpha(animateFloatAsState(if (hideSourceWhileFingerDragging && isFingerDraggingThisTile) 0f else 1f, label = "appTileAlpha").value)
            .pointerInput(app.packageName, reorderMode, stripOuterDragOwnsLongPress) {
                // Drawer reorder: outer drag owns long-press. Home strip: same via stripOuterDragOwnsLongPress.
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = when {
                        stripOuterDragOwnsLongPress || reorderMode -> null
                        else -> { _ -> onLongPress() }
                    },
                )
            }
            .then(reorderDragModifier),
        // Top-align so the column fills from the top; Center would vertically center a short column (not our case with fillMaxSize).
        contentAlignment = Alignment.TopCenter,
    ) {
        val density = LocalDensity.current
        // Reserve extra height for two lines (descenders + outline); shrink icon/padding when the cell is short.
        val iconPadTop = 3.dp
        val textPadBottom = 2.dp
        val (iconSizeUsed, iconPadBottom) = remember(height, iconSize, labelSizeSp, density) {
            val minLabel = with(density) { (labelSizeSp * 2.55f).sp.toDp() }
            var sz = iconSize
            var pad = (height - iconPadTop - sz - textPadBottom - minLabel).coerceIn(3.dp, 14.dp)
            repeat(10) {
                val labelSpace = height - iconPadTop - sz - pad - textPadBottom
                if (labelSpace >= minLabel) return@remember Pair(sz, pad)
                if (sz <= 40.dp) return@remember Pair(sz, pad)
                sz -= 3.dp
                pad = (height - iconPadTop - sz - textPadBottom - minLabel).coerceIn(3.dp, 14.dp)
            }
            Pair(sz.coerceAtLeast(40.dp), pad)
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(selRadius))
                    .background(themePalette.selectorBackgroundColour)
                    .border(
                        width = 1.dp,
                        color = themePalette.selectorBorderColour,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(selRadius),
                    )
            )
        } else if (showAppCardBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(cardRadius))
                    .background(brush = Brush.verticalGradient(listOf(cardTop, cardBottom)))
                    .border(
                        width = 0.8.dp,
                        color = Color(0x332F3B4F),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(cardRadius)
                    )
            )
        }
        // Drop-target highlight ring — shown while a drag ghost hovers over this slot.
        if (isDropTarget) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(selRadius))
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.65f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(selRadius),
                    )
            )
        }

        // Mirrors Flutter `app_card.dart`: `appCardIconPadding` + Expanded + `appCardTextPadding`.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = iconPadTop, bottom = iconPadBottom, start = 7.dp, end = 7.dp)
                    .size(iconSizeUsed),
            ) {
                AsyncImage(
                    model = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(iconSizeUsed)
                        .clip(iconMaskShape(appIconShape))
                        .align(Alignment.Center),
                )
                if (hasNotifBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "\u2731",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontSize = 9.sp,
                                lineHeight = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
                if (usageTimeMs >= 60_000L) {
                    val usageLabel = remember(usageTimeMs) {
                        val m = (usageTimeMs / 60_000).toInt()
                        if (m >= 60) "${m / 60}h" else "${m}m"
                    }
                    Text(
                        text = usageLabel,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color(0xCC111111), RoundedCornerShape(3.dp))
                            .padding(horizontal = 2.dp, vertical = 0.5.dp),
                        fontSize = 7.sp,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        lineHeight = 9.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 2.dp, start = 3.dp, end = 3.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                OutlinedLabel(
                    text = displayLabel,
                    fontSizeSp = labelSizeSp,
                    textColor = themePalette.appCardTextColour,
                    outlineColor = themePalette.appCardTextOutlineColour,
                )
            }
        }
    }
}

@Composable
private fun OutlinedLabel(
    text: String,
    fontSizeSp: Int,
    textColor: Color = Color(0xFFE6E6E6),
    outlineColor: Color = Color.Black,
) {
    val lineHeightStyle = remember {
        LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = LineHeightStyle.Trim.None,
    )
    }
    val base = remember(fontSizeSp, textColor, lineHeightStyle) {
        TextStyle(
        fontSize = fontSizeSp.sp,
        fontWeight = FontWeight.W500,
        color = textColor,
        lineHeight = fontSizeSp.sp,
        lineHeightStyle = lineHeightStyle,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    }
    val outlineShadowStyles = remember(base, outlineColor) {
        OUTLINE_OFFSETS.map { o ->
            base.copy(
                color = textColor,
                shadow = androidx.compose.ui.graphics.Shadow(color = outlineColor, offset = o, blurRadius = 0f),
            )
        }
    }
    val mainShadowStyle = remember(base) {
        base.copy(
            shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, offset = MAIN_SHADOW_OFFSET, blurRadius = 3f),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        for (style in outlineShadowStyles) {
            Text(
                text,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                softWrap = true,
                style = style,
            )
        }
        Text(
            text,
            modifier = Modifier.fillMaxWidth(),
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            softWrap = true,
            style = mainShadowStyle,
        )
    }
}

/**
 * Bottom sheet: home strip groups and app-drawer folders share this UI and behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeGroupFolderOverlay(
    groupTitle: String,
    members: List<AppEntry>,
    onDismiss: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onRemoveFromGroup: (String) -> Unit,
    onRenameGroup: (String) -> Unit,
    appIconShape: AppIconShape,
    themePalette: LauncherThemePalette,
    renameDialogTitle: String = "Rename group",
    emptyStateMessage: String = "No apps yet — long-press an app in the drawer to add it here.",
) {
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(groupTitle) { mutableStateOf(groupTitle) }
    var focusedIndex by remember(members) { mutableIntStateOf(0) }
    val gridFocusRequester = remember { FocusRequester() }
    LaunchedEffect(members.size) {
        focusedIndex = focusedIndex.coerceIn(0, (members.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(members.isNotEmpty()) {
        if (members.isNotEmpty()) {
            gridFocusRequester.requestFocus()
        }
    }
    androidx.compose.runtime.LaunchedEffect(groupTitle) {
        renameText = groupTitle
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1F28),
        contentColor = HOME_STRIP_LABEL_COLOR,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            // Title + action icons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = groupTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = HOME_STRIP_LABEL_COLOR,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { renameOpen = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Rename",
                            tint = themePalette.settingsMenuBody,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            if (members.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    emptyStateMessage,
                    color = themePalette.settingsMenuBody,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            } else {
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .focusRequester(gridFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { ev ->
                            if (ev.type != KeyEventType.KeyDown || members.isEmpty()) return@onPreviewKeyEvent false
                            val cols = 4
                            val rows = ((members.size - 1) / cols) + 1
                            val row = focusedIndex / cols
                            val col = focusedIndex % cols
                            when (ev.key) {
                                Key.DirectionLeft -> {
                                    focusedIndex = (focusedIndex - 1).coerceAtLeast(0)
                                    true
                                }
                                Key.DirectionRight -> {
                                    focusedIndex = (focusedIndex + 1).coerceAtMost(members.lastIndex)
                                    true
                                }
                                Key.DirectionUp -> {
                                    focusedIndex = (focusedIndex - cols).coerceAtLeast(0)
                                    true
                                }
                                Key.DirectionDown -> {
                                    val targetRow = (row + 1).coerceAtMost(rows - 1)
                                    focusedIndex = (targetRow * cols + col).coerceAtMost(members.lastIndex)
                                    true
                                }
                                Key.Enter, Key.NumPadEnter -> {
                                    members.getOrNull(focusedIndex)?.let { onLaunchApp(it.packageName) }
                                    true
                                }
                                else -> false
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    itemsIndexed(members, key = { idx, it -> "${idx}_${it.packageName}" }) { index, app ->
                        val cardRadius = themePalette.appCardCornerRadiusDp.dp
                        val cardShape = RoundedCornerShape(cardRadius)
                        val isFocused = index == focusedIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(
                                    brush = if (isFocused) {
                                        Brush.verticalGradient(
                                            listOf(
                                                themePalette.dockSelected,
                                                themePalette.dockSelected,
                                            ),
                                        )
                                    } else {
                                        Brush.verticalGradient(listOf(themePalette.appCardTop, themePalette.appCardBottom))
                                    },
                                )
                                .border(
                                    width = if (isFocused) 1.dp else 0.8.dp,
                                    color = if (isFocused) Color(0x553D4B60) else Color(0x332F3B4F),
                                    shape = cardShape,
                                )
                                .padding(top = 8.dp, start = 4.dp, end = 4.dp, bottom = 6.dp)
                                .pointerInput(app.packageName) {
                                    detectTapGestures(
                                        onTap = {
                                            focusedIndex = index
                                            onLaunchApp(app.packageName)
                                        },
                                        onLongPress = { onRemoveFromGroup(app.packageName) },
                                    )
                                },
                        ) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = app.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(iconMaskShape(appIconShape)),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = app.label,
                                color = HOME_STRIP_LABEL_COLOR,
                                fontSize = HOME_STRIP_LABEL_FONT_SP,
                                lineHeight = HOME_STRIP_LABEL_LINE_SP,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
    if (renameOpen) {
        val renameFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { renameFocusRequester.requestFocus() }
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = themePalette.settingsBg,
            titleContentColor = themePalette.settingsMenuTitle,
            textContentColor = themePalette.settingsMenuBody,
            title = { Text(renameDialogTitle, color = themePalette.settingsMenuTitle) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = capitalizeFirstLetterForGroupInput(it) },
                    singleLine = true,
                    label = { Text("Name", color = themePalette.settingsMenuBody) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE8EEF7),
                        unfocusedTextColor = Color(0xFFE8EEF7),
                        focusedLabelColor = themePalette.settingsMenuBody,
                        unfocusedLabelColor = themePalette.settingsMenuBody,
                        focusedIndicatorColor = Color(0xFF5B9BD5),
                        unfocusedIndicatorColor = Color(0xFF5F6A78),
                        focusedContainerColor = Color(0xFF1E2430),
                        unfocusedContainerColor = Color(0xFF1E2430),
                        disabledContainerColor = Color(0xFF1E2430),
                        cursorColor = Color(0xFF84D5F6),
                        focusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(renameFocusRequester),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameGroup(renameText)
                        renameOpen = false
                    },
                ) { Text("Save", color = themePalette.settingsMenuBody) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) {
                    Text("Cancel", color = themePalette.settingsMenuBody)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeGroupContextMenu(
    group: HomeGroup,
    otherGroupExists: Boolean,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onOpenGroup: () -> Unit,
    onRenameGroup: (String) -> Unit,
    onMoveSide: () -> Unit,
    onDeleteGroup: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(group.id) { mutableStateOf(group.title) }
    val iconTint = themePalette.settingsMenuBody
    val labelColor = themePalette.settingsMenuTitle
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = themePalette.settingsBg,
        contentColor = labelColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                group.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = labelColor,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF2C3340))
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
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
                }
            }

            MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open group", onClick = onOpenGroup)
            MenuRow(Icons.Rounded.Tune, "Rename group", onClick = { renameOpen = true })
            if (otherGroupExists) {
                val sideLabel = if (group.side == HomeGroupSide.LEFT) "Move to right side" else "Move to left side"
                MenuRow(Icons.Rounded.SwapVert, sideLabel, onClick = onMoveSide)
            }
            MenuRow(Icons.Outlined.Delete, "Delete group", onClick = onDeleteGroup)
        }
    }
    if (renameOpen) {
        val renameFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { renameFocus.requestFocus() }
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = themePalette.settingsBg,
            titleContentColor = themePalette.settingsMenuTitle,
            textContentColor = themePalette.settingsMenuBody,
            title = { Text("Rename group", color = themePalette.settingsMenuTitle) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = capitalizeFirstLetterForGroupInput(it) },
                    singleLine = true,
                    label = { Text("Name", color = themePalette.settingsMenuBody) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE8EEF7),
                        unfocusedTextColor = Color(0xFFE8EEF7),
                        focusedLabelColor = themePalette.settingsMenuBody,
                        unfocusedLabelColor = themePalette.settingsMenuBody,
                        focusedIndicatorColor = Color(0xFF5B9BD5),
                        unfocusedIndicatorColor = Color(0xFF5F6A78),
                        focusedContainerColor = Color(0xFF1E2430),
                        unfocusedContainerColor = Color(0xFF1E2430),
                        disabledContainerColor = Color(0xFF1E2430),
                        cursorColor = Color(0xFF84D5F6),
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(renameFocus),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameGroup(renameText.trim().ifBlank { "Group" })
                    renameOpen = false
                    onDismiss()
                }) { Text("Save", color = themePalette.settingsMenuBody) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) {
                    Text("Cancel", color = themePalette.settingsMenuBody)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderDrawerContextMenu(
    folder: DrawerGridCell.Folder,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onOpenFolder: () -> Unit,
    onRenameFolder: (String) -> Unit,
    onReorderApps: () -> Unit,
    onDeleteFolder: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(folder.displayTitle) { mutableStateOf(folder.displayTitle) }
    LaunchedEffect(folder.id, folder.displayTitle) {
        renameText = folder.displayTitle
    }
    val iconTint = themePalette.settingsMenuBody
    val labelColor = themePalette.settingsMenuTitle
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = themePalette.settingsBg,
        contentColor = labelColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                folder.displayTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = labelColor,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF2C3340))
            Spacer(Modifier.height(4.dp))

            @Composable
            fun MenuRow(
                icon: ImageVector,
                label: String,
                onClick: () -> Unit,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = labelColor,
                    )
                }
            }

            MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open folder", onClick = onOpenFolder)
            MenuRow(
                Icons.Rounded.Tune,
                "Rename folder",
                onClick = { renameOpen = true },
            )
            MenuRow(Icons.Rounded.SwapVert, "Reorder apps", onClick = onReorderApps)
            MenuRow(Icons.Outlined.Delete, "Delete folder", onClick = onDeleteFolder)
        }
    }
    if (renameOpen) {
        val renameFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { renameFocusRequester.requestFocus() }
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = themePalette.settingsBg,
            titleContentColor = themePalette.settingsMenuTitle,
            textContentColor = themePalette.settingsMenuBody,
            title = { Text("Rename folder", color = themePalette.settingsMenuTitle) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = capitalizeFirstLetterForGroupInput(it) },
                    singleLine = true,
                    label = { Text("Name", color = themePalette.settingsMenuBody) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE8EEF7),
                        unfocusedTextColor = Color(0xFFE8EEF7),
                        focusedLabelColor = themePalette.settingsMenuBody,
                        unfocusedLabelColor = themePalette.settingsMenuBody,
                        focusedIndicatorColor = Color(0xFF5B9BD5),
                        unfocusedIndicatorColor = Color(0xFF5F6A78),
                        focusedContainerColor = Color(0xFF1E2430),
                        unfocusedContainerColor = Color(0xFF1E2430),
                        disabledContainerColor = Color(0xFF1E2430),
                        cursorColor = Color(0xFF84D5F6),
                        focusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(renameFocusRequester),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameFolder(renameText.trim().ifBlank { "Folder" })
                        renameOpen = false
                        onDismiss()
                    },
                ) { Text("Save", color = themePalette.settingsMenuBody) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) {
                    Text("Cancel", color = themePalette.settingsMenuBody)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContextMenu(
    app: AppEntry,
    themePalette: LauncherThemePalette,
    isHidden: Boolean,
    homeGroups: List<HomeGroup>,
    addHomeShortcutEnabled: Boolean,
    removeHomeShortcutEnabled: Boolean,
    drawerFolderActionsEnabled: Boolean,
    drawerFolders: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onInfo: () -> Unit,
    onHideToggle: () -> Unit,
    onReorder: () -> Unit,
    onAddHomeShortcut: () -> Unit,
    onRemoveHomeShortcut: () -> Unit,
    onAddToHomeGroup: (String) -> Unit,
    onRemoveFromHomeGroup: (String) -> Unit,
    onCreateDrawerFolder: (String) -> Boolean,
    onAddToDrawerFolder: (String) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newDrawerFolderDialogOpen by remember { mutableStateOf(false) }
    var newDrawerFolderName by remember { mutableStateOf("") }
    val newFolderFocusRequester = remember { FocusRequester() }
       val iconTint = themePalette.settingsMenuBody
    val labelColor = themePalette.settingsMenuTitle
    val menuScrollState = rememberScrollState()
    val maxMenuBodyHeight = (LocalConfiguration.current.screenHeightDp * 0.58f).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = themePalette.settingsBg,
        contentColor = labelColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                app.label,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = labelColor,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF2C3340))
            Spacer(Modifier.height(4.dp))

            @Composable
            fun MenuRow(
                icon: ImageVector,
                label: String,
                onClick: () -> Unit,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = labelColor,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxMenuBodyHeight)
                    .verticalScroll(menuScrollState),
            ) {
                MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open", onLaunch)
                MenuRow(Icons.Rounded.Info, "App info", onInfo)
                MenuRow(
                    if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    if (isHidden) "Unhide" else "Hide",
                    onHideToggle,
                )
                MenuRow(Icons.Rounded.SwapVert, "Reorder apps", onReorder)
                if (drawerFolderActionsEnabled) {
                    MenuRow(
                        Icons.Rounded.Folder,
                        "New Group",
                        onClick = {
                            newDrawerFolderName = ""
                            newDrawerFolderDialogOpen = true
                        },
                    )
                }
                if (app.packageName != AppsRepository.INTERNAL_SETTINGS_PACKAGE && homeGroups.isNotEmpty()) {
                    for (g in homeGroups) {
                        val inGroup = app.packageName in g.packageNames
                        if (inGroup) {
                            MenuRow(
                                Icons.Outlined.BookmarkRemove,
                                "Remove from ${g.title}",
                                { onRemoveFromHomeGroup(g.id) },
                            )
                        } else {
                            MenuRow(
                                Icons.Rounded.BookmarkAdd,
                                "Add to ${g.title}",
                                { onAddToHomeGroup(g.id) },
                            )
                        }
                    }
                }
                if (drawerFolderActionsEnabled) {
                    for ((folderId, label) in drawerFolders) {
                        MenuRow(
                            Icons.Rounded.BookmarkAdd,
                            "Add to $label",
                            onClick = {
                                onAddToDrawerFolder(folderId)
                                onDismiss()
                            },
                        )
                    }
                }
                if (addHomeShortcutEnabled) {
                    MenuRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add shortcut", onAddHomeShortcut)
                }
                if (removeHomeShortcutEnabled) {
                    MenuRow(Icons.Outlined.Close, "Remove shortcut", onRemoveHomeShortcut)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (newDrawerFolderDialogOpen) {
        LaunchedEffect(Unit) { newFolderFocusRequester.requestFocus() }
        AlertDialog(
            onDismissRequest = { newDrawerFolderDialogOpen = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = themePalette.settingsBg,
            titleContentColor = themePalette.settingsMenuTitle,
            textContentColor = themePalette.settingsMenuBody,
            title = { Text("New Group", color = themePalette.settingsMenuTitle) },
            text = {
                OutlinedTextField(
                    value = newDrawerFolderName,
                    onValueChange = { newDrawerFolderName = capitalizeFirstLetterForGroupInput(it) },
                    singleLine = true,
                    label = { Text("Group name", color = themePalette.settingsMenuBody) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE8EEF7),
                        unfocusedTextColor = Color(0xFFE8EEF7),
                        focusedLabelColor = themePalette.settingsMenuBody,
                        unfocusedLabelColor = themePalette.settingsMenuBody,
                        focusedIndicatorColor = Color(0xFF5B9BD5),
                        unfocusedIndicatorColor = Color(0xFF5F6A78),
                        focusedContainerColor = Color(0xFF1E2430),
                        unfocusedContainerColor = Color(0xFF1E2430),
                        disabledContainerColor = Color(0xFF1E2430),
                        cursorColor = Color(0xFF84D5F6),
                        focusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(newFolderFocusRequester),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onCreateDrawerFolder(newDrawerFolderName)) {
                            newDrawerFolderDialogOpen = false
                            onDismiss()
                        }
                    },
                ) { Text("Create", color = themePalette.settingsMenuBody) }
            },
            dismissButton = {
                TextButton(onClick = { newDrawerFolderDialogOpen = false }) {
                    Text("Cancel", color = themePalette.settingsMenuBody)
                }
            },
        )
    }
}

@Composable
private fun HomeStripItemLabel(text: String) {
    Text(
        text = text,
        color = HOME_STRIP_LABEL_COLOR,
        fontSize = HOME_STRIP_LABEL_FONT_SP,
        lineHeight = HOME_STRIP_LABEL_LINE_SP,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .widthIn(max = 88.dp)
            .padding(top = 1.dp),
    )
}

/**
 * Home group tile in [HomeShortcutStrip]: exact same outer 52dp × 12dp-rounded bounds as pinned app tiles (full-bleed folder panel).
 */
@Composable
private fun HomeGroupStripIcon(
    title: String,
    members: List<AppEntry>,
    appIconShape: AppIconShape,
    hasUnreadBadge: Boolean,
    gesturesEnabled: Boolean = true,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val tile = HOME_SHORTCUT_ICON_DP
    val stripCorner = RoundedCornerShape(12.dp)
    val folderBg = Color(0xD9181C24)
    val folderBorderIdle = Color(0x38FFFFFF)
    // Tighter outer inset + gap → larger mini-icons without changing the 48dp tile.
    val gridInset = 2.5.dp
    val gap = 1.5.dp
    val preview = members.take(4)
    val gridSide = tile - gridInset * 2
    val half = (gridSide - gap) / 2
    Box(
        modifier = Modifier
            .size(tile)
            .semantics { contentDescription = title }
            .clip(stripCorner)
            .background(folderBg)
            .border(0.5.dp, folderBorderIdle, stripCorner)
            .then(
                if (gesturesEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
                    }
                } else {
                    // Strip: parent Column handles tap when !reorderMode; stripDragGesture owns long-press/drag.
                    // Omit inner pointerInput so it cannot win the gesture arena over the parent drag detector.
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(gridInset),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (preview.size) {
                0 -> {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        tint = Color(0x55FFFFFF),
                        modifier = Modifier.size(gridSide * 0.6f),
                    )
                }
                1 -> {
                    val app = preview[0]
                    Box(
                        modifier = Modifier.size(gridSide),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = app.icon,
                            contentDescription = app.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(gridSide - 2.dp).clip(iconMaskShape(appIconShape)),
                        )
                    }
                }
                2 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        preview.forEach { app ->
                            AsyncImage(
                                model = app.icon,
                                contentDescription = app.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(half).clip(iconMaskShape(appIconShape)),
                            )
                        }
                    }
                }
                3 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        AsyncImage(
                            model = preview[0].icon,
                            contentDescription = preview[0].label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(half).clip(iconMaskShape(appIconShape)),
                        )
                        AsyncImage(
                            model = preview[1].icon,
                            contentDescription = preview[1].label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(half).clip(iconMaskShape(appIconShape)),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        AsyncImage(
                            model = preview[2].icon,
                            contentDescription = preview[2].label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(half).clip(iconMaskShape(appIconShape)),
                        )
                    }
                }
                else -> {
                    preview.chunked(2).forEach { rowApps ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            rowApps.forEach { app ->
                                AsyncImage(
                                    model = app.icon,
                                    contentDescription = app.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(half).clip(iconMaskShape(appIconShape)),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (hasUnreadBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD32F2F)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u2731",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontSize = 8.sp,
                        lineHeight = 8.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HomeShortcutStrip(
    @Suppress("UNUSED_PARAMETER") gridPreset: GridPreset,
    gridCells: List<DrawerGridCell>,
    stripSlotTokens: List<String?>,
    showShortcutApps: Boolean,
    homeGroups: List<HomeGroup>,
    showHomeGroups: Boolean,
    allApps: List<AppEntry>,
    unreadPackages: Set<String>,
    appIconShape: AppIconShape,
    showAppCardBackground: Boolean,
    themePalette: LauncherThemePalette,
    focusedIndex: Int?,
    hapticsEnabled: Boolean,
    hapticIntensity: Int,
    reorderMode: Boolean,
    movingSlotId: String?,
    onEnterReorderMode: () -> Unit,
    onExitReorderMode: () -> Unit,
    onStartStripMove: (String) -> Unit,
    onFinishReorderDrop: (String?) -> Unit,
    onClearMove: () -> Unit,
    onLaunchShortcut: (String) -> Unit,
    onOpenHomeGroup: (HomeGroup) -> Unit,
    onOpenDrawerFolder: (DrawerGridCell.Folder) -> Unit,
    onShowSettings: () -> Unit,
    onDeleteHomeShortcut: (String) -> Unit,
    onDeleteGroup: (HomeGroup) -> Unit,
    onStripReorderTap: (String) -> Unit,
    onClearStripKeyboardFocus: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    val outerPadH = 22.dp
    val colSpacing = themePalette.appGridColumnSpacingDp.dp
    val iconSize = themePalette.appGridIconSizeDp.dp
    val labelSizeSp = themePalette.appCardFontSp.toInt()

    val baseSlotTokens = remember(stripSlotTokens) {
        List(STRIP_TOTAL_SLOTS) { i -> stripSlotTokens.getOrNull(i) }
    }

    val cellLayouts = remember { mutableMapOf<String, LayoutCoordinates>() }
    val originalStripBoundsRef = remember { mutableListOf(emptyMap<String, Rect>()) }
    val stripHoverKeysRef = remember { mutableListOf(emptySet<String>()) }
    var hoveredSlotKey by remember { mutableStateOf<String?>(null) }
    var reorderFingerDragging by remember { mutableStateOf(false) }
    var dragFingerRoot by remember { mutableStateOf(Offset.Zero) }
    var ghostVisible by remember { mutableStateOf(false) }
    val ghostScaleAnim = remember { Animatable(1f) }
    var stripCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pendingDropMoving by remember { mutableStateOf<String?>(null) }
    var pendingDropTarget by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(movingSlotId) {
        if (movingSlotId == null) {
            pendingDropMoving = null
            pendingDropTarget = null
        }
    }

    val displaySlotTokens: List<String?> = run {
        val moving = movingSlotId ?: pendingDropMoving
        val hovered = hoveredSlotKey ?: pendingDropTarget
        if (moving == null || hovered == null) baseSlotTokens
        else {
            val fromI = baseSlotTokens.indexOfFirst { it == moving }
            val toI = baseSlotTokens.indexOfFirst { it == hovered }
            if (fromI < 0 || toI < 0 || fromI == toI) baseSlotTokens
            else {
                val m = baseSlotTokens.toMutableList()
                val tmp = m[fromI]
                m[fromI] = m[toI]
                m[toI] = tmp
                m
            }
        }
    }

    val stripHoverKeys: Set<String> = remember(displaySlotTokens) {
        buildSet {
            displaySlotTokens.forEachIndexed { i, tok ->
                tok?.let { add(it) }
                add("strip_empty_${i + 1}")
            }
        }
    }
    SideEffect { stripHoverKeysRef[0] = stripHoverKeys }

    val wiggle = rememberInfiniteTransition(label = "homeStripWiggle")
    val wiggleRot by wiggle.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(220, easing = LinearEasing), RepeatMode.Reverse),
        label = "homeStripWiggleRot",
    )
    val wiggleStr by animateFloatAsState(
        targetValue = if (ghostVisible) 1f else if (reorderMode) 0.7f else 0f,
        animationSpec = tween(180),
        label = "homeStripWiggleStr",
    )

    fun resolveDropTarget(hoverKey: String?): String? {
        if (hoverKey == null || hoverKey.startsWith("strip_empty_")) return null
        return hoverKey
    }

    fun Modifier.stripDragGesture(slotId: String): Modifier = this.pointerInput(slotId, reorderMode) {
        fun attachedBoundsSnapshot(): Map<String, Rect> =
            cellLayouts.entries
                .filter { it.value.isAttached }
                .associate { (k, v) -> k to v.boundsInRoot() }

        fun nearestStripHover(source: String, finger: Offset, bounds: Map<String, Rect>): String? =
            bounds.entries
                .asSequence()
                .filter { (k, _) -> k != source && k in stripHoverKeysRef[0] }
                .minByOrNull { (_, b) ->
                    val c = b.center
                    hypot((c.x - finger.x).toDouble(), (c.y - finger.y).toDouble())
                }?.key

        detectDragGesturesAfterLongPress(
            onDragStart = { startOffset ->
                onClearStripKeyboardFocus()
                if (!reorderMode) onEnterReorderMode()
                doNavFeedback(view, hapticsEnabled, hapticIntensity)
                onStartStripMove(slotId)
                hoveredSlotKey = null
                reorderFingerDragging = true
                originalStripBoundsRef[0] = attachedBoundsSnapshot()
                ghostVisible = true
                val src = cellLayouts[slotId]
                if (src != null && src.isAttached) dragFingerRoot = src.localToRoot(startOffset)
                scope.launch {
                    ghostScaleAnim.animateTo(
                        1.15f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    )
                }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                val src = cellLayouts[slotId]
                dragFingerRoot = if (src != null && src.isAttached) {
                    src.localToRoot(change.position)
                } else {
                    Offset(dragFingerRoot.x + dragAmount.x, dragFingerRoot.y + dragAmount.y)
                }
                if (originalStripBoundsRef[0].isEmpty()) {
                    val snap = attachedBoundsSnapshot()
                    if (snap.isNotEmpty()) originalStripBoundsRef[0] = snap
                }
                val nearest = nearestStripHover(slotId, dragFingerRoot, originalStripBoundsRef[0])
                if (nearest != hoveredSlotKey) {
                    hoveredSlotKey = nearest
                    if (nearest != null && hapticsEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                    }
                }
            },
            onDragEnd = {
                reorderFingerDragging = false
                val hoverKey = hoveredSlotKey ?: nearestStripHover(slotId, dragFingerRoot, originalStripBoundsRef[0])
                pendingDropMoving = slotId
                pendingDropTarget = hoverKey
                hoveredSlotKey = null
                scope.launch {
                    val targetCenter = hoverKey?.let { originalStripBoundsRef[0][it]?.center }
                    if (targetCenter != null && hapticsEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    if (targetCenter != null) {
                        val startPos = dragFingerRoot
                        kotlinx.coroutines.coroutineScope {
                            launch {
                                androidx.compose.animation.core.animate(
                                    0f,
                                    1f,
                                    animationSpec = tween(120, easing = FastOutSlowInEasing),
                                ) { p, _ ->
                                    dragFingerRoot = Offset(
                                        startPos.x + (targetCenter.x - startPos.x) * p,
                                        startPos.y + (targetCenter.y - startPos.y) * p,
                                    )
                                }
                            }
                            launch { ghostScaleAnim.animateTo(0f, tween(120, easing = FastOutSlowInEasing)) }
                        }
                    } else {
                        ghostScaleAnim.animateTo(0f, tween(80, easing = FastOutSlowInEasing))
                    }
                    onFinishReorderDrop(resolveDropTarget(hoverKey))
                    ghostScaleAnim.snapTo(1f)
                    ghostVisible = false
                }
            },
            onDragCancel = {
                reorderFingerDragging = false
                hoveredSlotKey = null
                pendingDropMoving = null
                pendingDropTarget = null
                scope.launch { ghostScaleAnim.snapTo(1f) }
                ghostVisible = false
                onClearMove()
            },
        )
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = reorderMode,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Hold & drag to reorder",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFB8C1CE),
            )
            Text(
                "Done",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF84D5F6),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onExitReorderMode() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = outerPadH, vertical = 6.dp)
            .onGloballyPositioned { stripCoords = it },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            val viewportWidth = maxWidth
            val minLabelBlock = with(density) { (labelSizeSp * 2.55f).sp.toDp() }
            val minCellHeight = 3.dp + iconSize + 14.dp + 2.dp + minLabelBlock
            val minCellWidth = iconSize + 14.dp
            val stripCols = 5
            val cardW =
                ((viewportWidth - colSpacing * (stripCols - 1)) / stripCols).coerceAtLeast(minCellWidth)
            val cardH = minCellHeight
            val ghostDims = Triple(cardW, cardH, iconSize)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(colSpacing),
                verticalAlignment = Alignment.Bottom,
            ) {
                for (col in 0 until STRIP_TOTAL_SLOTS) {
                    val rawTok = displaySlotTokens.getOrNull(col)
                    val visibleTok: String? = when (val t = rawTok) {
                        null -> null
                        else -> when {
                            HomeGroupIds.isHomeGroupId(t) -> if (showHomeGroups) t else null
                            else -> if (showShortcutApps) t else null
                        }
                    }
                    val emptyKey = "strip_empty_${col + 1}"
                    val selected = focusedIndex == col
                    val group = visibleTok?.takeIf { HomeGroupIds.isHomeGroupId(it) }
                        ?.let { tid -> homeGroups.find { g -> g.id == tid } }
                    if (visibleTok != null && group != null) {
                        val stripToken = group.id
                        val members =
                            group.packageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                        val reorderDragModifier = Modifier.stripDragGesture(stripToken)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .height(cardH)
                                .graphicsLayer { rotationZ = wiggleRot * wiggleStr },
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) themePalette.dockSelected else Color.Transparent)
                                    .padding(horizontal = 2.dp, vertical = 3.dp)
                                    .then(
                                        if (!reorderMode) {
                                            Modifier.pointerInput(group.id) {
                                                detectTapGestures(
                                                    onTap = { onOpenHomeGroup(group) },
                                                    onLongPress = { onEnterReorderMode() },
                                                )
                                            }
                                        } else {
                                            Modifier.pointerInput(group.id, reorderMode) {
                                                detectTapGestures(onTap = { onStripReorderTap(stripToken) })
                                            }
                                        },
                                    )
                                    .then(reorderDragModifier),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(iconSize + 8.dp)
                                        .onGloballyPositioned { coords -> cellLayouts[stripToken] = coords },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    HomeGroupStripIcon(
                                        title = group.title,
                                        members = members,
                                        appIconShape = appIconShape,
                                        hasUnreadBadge = group.packageNames.any { it in unreadPackages },
                                        gesturesEnabled = false,
                                        onClick = {},
                                        onLongPress = {},
                                    )
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = reorderMode,
                                        modifier = Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-4).dp),
                                        enter = androidx.compose.animation.fadeIn(tween(120)) + androidx.compose.animation.scaleIn(tween(120)),
                                        exit = androidx.compose.animation.fadeOut(tween(100)) + androidx.compose.animation.scaleOut(tween(100)),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD32F2F))
                                                .clickable { onDeleteGroup(group) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Outlined.Close,
                                                contentDescription = "Delete group",
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp),
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(HOME_STRIP_ICON_LABEL_GAP))
                                HomeStripItemLabel(group.title)
                            }
                        }
                    } else if (visibleTok != null) {
                        val stripToken = visibleTok
                        val cell = drawerCellForHomeStripToken(stripToken, gridCells, allApps)
                        if (cell == null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(cardH)
                                    .onGloballyPositioned { coords -> cellLayouts[emptyKey] = coords },
                            )
                        } else {
                            val isMovingThis = ghostVisible && movingSlotId == stripToken
                            val isDropTarget = reorderMode && ghostVisible && !reorderFingerDragging &&
                                hoveredSlotKey == stripToken && stripToken != movingSlotId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(cardH),
                            ) {
                                val reorderDragModifier = Modifier.stripDragGesture(stripToken)
                                when (cell) {
                                    is DrawerGridCell.App -> {
                                        val app = cell.entry
                                        AppTile(
                                            app = app,
                                            reorderMode = reorderMode,
                                            appIconShape = appIconShape,
                                            selected = movingSlotId == stripToken || selected,
                                            isDropTarget = isDropTarget,
                                            width = cardW,
                                            height = cardH,
                                            iconSize = iconSize,
                                            labelSizeSp = labelSizeSp,
                                            cardTop = themePalette.appCardTop,
                                            cardBottom = themePalette.appCardBottom,
                                            showAppCardBackground = showAppCardBackground,
                                            themePalette = themePalette,
                                            reorderDragModifier = reorderDragModifier,
                                            isFingerDraggingThisTile = isMovingThis,
                                            reorderDragOffset = Offset.Zero,
                                            hideSourceWhileFingerDragging = isMovingThis,
                                            usageTimeMs = 0L,
                                            hasNotifBadge = app.packageName in unreadPackages,
                                            onGloballyPositioned = { coords -> cellLayouts[stripToken] = coords },
                                            onClick = {
                                                if (reorderMode) onStripReorderTap(stripToken)
                                                else {
                                                    if (app.packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) {
                                                        onShowSettings()
                                                    } else {
                                                        onLaunchShortcut(stripToken)
                                                    }
                                                }
                                            },
                                            onLongPress = { },
                                            stripOuterDragOwnsLongPress = true,
                                        )
                                    }
                                    is DrawerGridCell.Folder -> {
                                        FolderTile(
                                            displayLabel = cell.displayTitle,
                                            members = cell.members,
                                            appIconShape = appIconShape,
                                            reorderMode = reorderMode,
                                            selected = movingSlotId == stripToken || selected,
                                            hasUnreadBadge = cell.members.any { it.packageName in unreadPackages },
                                            isDropTarget = isDropTarget,
                                            width = cardW,
                                            height = cardH,
                                            iconSize = iconSize,
                                            labelSizeSp = labelSizeSp,
                                            cardTop = themePalette.appCardTop,
                                            cardBottom = themePalette.appCardBottom,
                                            showAppCardBackground = showAppCardBackground,
                                            themePalette = themePalette,
                                            reorderDragModifier = reorderDragModifier,
                                            isFingerDraggingThisTile = isMovingThis,
                                            reorderDragOffset = Offset.Zero,
                                            hideSourceWhileFingerDragging = isMovingThis,
                                            onGloballyPositioned = { coords -> cellLayouts[stripToken] = coords },
                                            onClick = {
                                                if (reorderMode) onStripReorderTap(stripToken)
                                                else onOpenDrawerFolder(cell)
                                            },
                                            onLongPress = { },
                                            stripOuterDragOwnsLongPress = true,
                                        )
                                    }
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = reorderMode && cell is DrawerGridCell.App,
                                    modifier = Modifier.align(Alignment.TopStart).offset(x = 2.dp, y = 2.dp),
                                    enter = androidx.compose.animation.fadeIn(tween(120)) + androidx.compose.animation.scaleIn(tween(120)),
                                    exit = androidx.compose.animation.fadeOut(tween(100)) + androidx.compose.animation.scaleOut(tween(100)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFD32F2F))
                                            .clickable { onDeleteHomeShortcut(stripToken) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Remove shortcut",
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(cardH)
                                .onGloballyPositioned { coords -> cellLayouts[emptyKey] = coords },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(iconSize + 8.dp)
                                        .border(
                                            1.5.dp,
                                            Color(0x55B8C1CE),
                                            RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Apps,
                                        null,
                                        tint = Color(0x55B8C1CE),
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(Modifier.height(HOME_STRIP_ICON_LABEL_GAP))
                                HomeStripItemLabel("")
                            }
                        }
                    }
                }
            }

            val dragCell = movingSlotId?.takeIf { !HomeGroupIds.isHomeGroupId(it) }?.let { token ->
                drawerCellForHomeStripToken(token, gridCells, allApps)
            }
            val dragGroupGhost = movingSlotId?.takeIf { HomeGroupIds.isHomeGroupId(it) }?.let { id ->
                homeGroups.find { it.id == id }
            }
            if (ghostVisible && stripCoords != null && (dragCell != null || dragGroupGhost != null)) {
                val b = stripCoords!!.boundsInRoot()
                val (cw, ch, isz) = ghostDims
                val xDp = with(density) { (dragFingerRoot.x - b.left).toDp() } - cw / 2
                val yDp = with(density) { (dragFingerRoot.y - b.top).toDp() } - ch / 2
                Box(
                    Modifier
                        .offset(xDp, yDp)
                        .zIndex(4f)
                        .graphicsLayer {
                            scaleX = ghostScaleAnim.value
                            scaleY = ghostScaleAnim.value
                        },
                ) {
                    when {
                        dragGroupGhost != null -> {
                            val gg = dragGroupGhost
                            val members =
                                gg.packageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                            Column(
                                modifier = Modifier.width(cw).height(ch),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
                            ) {
                                Box(
                                    modifier = Modifier.size(isz + 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    HomeGroupStripIcon(
                                        title = gg.title,
                                        members = members,
                                        appIconShape = appIconShape,
                                        hasUnreadBadge = gg.packageNames.any { it in unreadPackages },
                                        gesturesEnabled = false,
                                        onClick = {},
                                        onLongPress = {},
                                    )
                                }
                                Spacer(Modifier.height(HOME_STRIP_ICON_LABEL_GAP))
                                HomeStripItemLabel(gg.title)
                            }
                        }
                        dragCell is DrawerGridCell.App -> AppTile(
                            app = dragCell.entry,
                            reorderMode = false,
                            appIconShape = appIconShape,
                            selected = false,
                            width = cw,
                            height = ch,
                            iconSize = isz,
                            labelSizeSp = labelSizeSp,
                            cardTop = themePalette.appCardTop,
                            cardBottom = themePalette.appCardBottom,
                            showAppCardBackground = showAppCardBackground,
                            themePalette = themePalette,
                            reorderDragModifier = Modifier,
                            isFingerDraggingThisTile = true,
                            reorderDragOffset = Offset.Zero,
                            hideSourceWhileFingerDragging = false,
                            onGloballyPositioned = {},
                            onClick = {},
                            onLongPress = {},
                        )
                        dragCell is DrawerGridCell.Folder -> FolderTile(
                            displayLabel = dragCell.displayTitle,
                            members = dragCell.members,
                            appIconShape = appIconShape,
                            hasUnreadBadge = dragCell.members.any { it.packageName in unreadPackages },
                            reorderMode = false,
                            selected = false,
                            width = cw,
                            height = ch,
                            iconSize = isz,
                            labelSizeSp = labelSizeSp,
                            cardTop = themePalette.appCardTop,
                            cardBottom = themePalette.appCardBottom,
                            showAppCardBackground = showAppCardBackground,
                            themePalette = themePalette,
                            reorderDragModifier = Modifier,
                            isFingerDraggingThisTile = true,
                            reorderDragOffset = Offset.Zero,
                            hideSourceWhileFingerDragging = false,
                            onGloballyPositioned = {},
                            onClick = {},
                            onLongPress = {},
                        )
                    }
                }
            }
        }
    }
}

/** Flutter `TextFieldTheme`: solid #202020. LOCKED baseline — height ~45dp, 16sp query; do not widen without UX ask. */
@Composable
private fun DrawerSearchBar(
    query: String,
    onClear: () -> Unit,
) {
    val scroll = rememberScrollState()
    LaunchedEffect(query) {
        scroll.scrollTo(scroll.maxValue)
    }
    // Flutter textFieldHeight 44 + small vertical insets; shorter than dock/navBarHeight (82).
    val searchBarHeight = 45.dp
    val fieldBg = Color(0xFF202020)
    val horizontalInset = 7.dp
    val clearIcon = 22.dp
    val clearPad = 5.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(searchBarHeight)
            .background(fieldBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalInset, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = query,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scroll),
            )
            Box(
                modifier = Modifier
                    .padding(clearPad)
                    .size(clearIcon)
                    .clip(CircleShape)
                    .border(1.2.dp, Color.White, CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun Dock(
    pageIndex: Int,
    homeActive: Boolean = false,
    onMail: () -> Unit,
    onShortcut: () -> Unit,
    onHome: () -> Unit,
    onCamera: () -> Unit,
    onLongPressMail: (() -> Unit)? = null,
    onLongPressShortcut: (() -> Unit)? = null,
    onLongPressCamera: (() -> Unit)? = null,
    onScrubPage: (Int) -> Unit,
    drawerPageCount: Int,
    mailHasUnread: Boolean,
    shortcutHasUnread: Boolean,
    /** When false, the centre home icon is omitted (drawer-only launcher). */
    showHomeButton: Boolean = true,
    /** When false, the Messages/WhatsApp dock icon is omitted (drawer-only launcher). */
    showMessagesShortcut: Boolean = true,
    dockStartIconModel: Any?,
    dockMiddleIconModel: Any?,
    secondDockFallbackResId: Int?,
    thirdDockFallbackResId: Int?,
    /** When non-null, show this drawable in the dock end slot (same as app grid). Null = default camera asset. */
    dockEndIconModel: Any?,
    appIconShape: AppIconShape,
    dockIconStyle: DockIconStyle,
    selectedTint: Color = Color(0x664FC3F7),
    themePalette: LauncherThemePalette,
    /** True when DPAD focus is inside the dock (from AppDrawer key handler). */
    focused: Boolean = false,
    /** Which dock slot is focused: 0=Mail, 1=Home, 2=Shortcut, 3=Camera (or 0=Mail, 1=Camera when compact). */
    focusedIndex: Int = 0,
) {
    val homeFocusIdx = if (showHomeButton) 1 else -1
    val shortcutFocusIdx = when {
        showHomeButton && showMessagesShortcut -> 2
        !showHomeButton && showMessagesShortcut -> 1
        else -> -1
    }
    val cameraFocusIdx = when {
        showHomeButton && showMessagesShortcut -> 3
        showHomeButton && !showMessagesShortcut -> 2
        !showHomeButton && showMessagesShortcut -> 2
        else -> 1
    }
    val density = LocalDensity.current
    val navBarHeight = themePalette.navBarHeight()
    val navBarSpacing = themePalette.navBarSpacing()
    val navMidSpacing = navBarSpacing * 0.6f
    val navIconSize = themePalette.navIconSize()
    val dockIconTint = themePalette.dockIconTint
    var scrubbing by remember { mutableStateOf(false) }
    var hoveredPage by remember { mutableStateOf(0) }
    val scrubOverlayAlpha by animateFloatAsState(
        targetValue = if (scrubbing) 1f else 0f,
        animationSpec = tween(100),
        label = "scrubOverlayAlpha",
    )
    var dotsWidthPx by remember { mutableStateOf(1f) }
    var fingerXpx by remember { mutableStateOf(0f) }

    fun xToPage(x: Float): Int {
        if (drawerPageCount <= 1) return 0
        val width = dotsWidthPx.coerceAtLeast(1f)
        val clamped = x.coerceIn(0f, width - 1f)
        val ratio = clamped / width
        val raw = (ratio * drawerPageCount).toInt()
        return raw.coerceIn(0, drawerPageCount - 1)
    }

    Box(
        modifier = Modifier
            .height(navBarHeight)
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0x77000000)))
            )
            .padding(horizontal = 0.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dockStartIconModel == null) {
                DockPng(
                    resId = R.drawable.ic_dock_mail,
                    onClick = onMail,
                    onLongPress = onLongPressMail,
                    hasUnread = mailHasUnread,
                    buttonSize = 68.dp,
                    iconSize = 48.dp,
                    selected = focused && focusedIndex == 0,
                    selectedTint = selectedTint,
                    iconTint = dockIconTint,
                )
            } else {
                DockEndSlot(
                    iconModel = dockStartIconModel,
                    fallbackIcon = Icons.Rounded.MailOutline,
                    appIconShape = appIconShape,
                    onClick = onMail,
                    onLongPress = onLongPressMail,
                    hasUnread = mailHasUnread,
                    buttonSize = 68.dp,
                    iconSize = 52.dp,
                    selected = focused && focusedIndex == 0,
                    selectedTint = selectedTint,
                    iconTint = dockIconTint,
                    forceMonochrome = false,
                )
            }
            Spacer(Modifier.weight(1f))

            if (showHomeButton) {
                DockIcon(
                    icon = Icons.Rounded.Home,
                    onClick = onHome,
                    buttonSize = 56.dp,
                    iconSize = navIconSize,
                    selected = focused && focusedIndex == homeFocusIdx,
                    selectedTint = selectedTint,
                    iconTint = dockIconTint,
                )
                Spacer(Modifier.width(navMidSpacing))
            }
            Box(
                modifier = Modifier
                    .onSizeChanged { dotsWidthPx = it.width.toFloat() }
                    .pointerInput(drawerPageCount) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Main)
                            fingerXpx = down.position.x

                            val startMs = SystemClock.uptimeMillis()
                            var activated = false

                            while (true) {
                                val remaining = (250L - (SystemClock.uptimeMillis() - startMs)).coerceAtLeast(0L)
                                val event = if (!activated && remaining > 0L) {
                                    withTimeoutOrNull(remaining) { awaitPointerEvent(pass = PointerEventPass.Main) }
                                } else {
                                    awaitPointerEvent(pass = PointerEventPass.Main)
                                }

                                if (event == null) {
                                    activated = true
                                    scrubbing = true
                                    val page = xToPage(fingerXpx)
                                    hoveredPage = page
                                    onScrubPage(page)
                                    continue
                                }

                                val change = event.changes.firstOrNull() ?: break
                                if (change.changedToUp()) {
                                    scrubbing = false
                                    break
                                }

                                if (change.positionChanged()) {
                                    fingerXpx = change.position.x
                                    if (activated) {
                                        val page = xToPage(fingerXpx)
                                        if (page != hoveredPage) {
                                            hoveredPage = page
                                            onScrubPage(page)
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
                    Dots(
                        current = if (scrubbing) hoveredPage else pageIndex.coerceIn(0, drawerPageCount - 1),
                        count = drawerPageCount,
                        themePalette = themePalette,
                        emphasize = false,
                        hideFirstPageLabel = homeActive,
                    )
                }
            }
            if (showMessagesShortcut) {
                Spacer(Modifier.width(navMidSpacing))
                DockEndSlot(
                    iconModel = dockMiddleIconModel,
                    fallbackIcon = if (secondDockFallbackResId != null) Icons.Rounded.Apps else Icons.Outlined.MailOutline,
                    fallbackResId = secondDockFallbackResId,
                    fallbackScale = if (secondDockFallbackResId == R.drawable.ic_dock_whatsapp) 0.76f else 0.84f,
                    appIconShape = appIconShape,
                    onClick = onShortcut,
                    onLongPress = onLongPressShortcut,
                    hasUnread = shortcutHasUnread,
                    buttonSize = 56.dp,
                    iconSize = navIconSize,
                    selected = focused && focusedIndex == shortcutFocusIdx,
                    selectedTint = selectedTint,
                    iconTint = dockIconTint,
                    forceMonochrome = false,
                )
            }

            Spacer(Modifier.weight(1f))
            // Third shortcut: same 68×52.dp icon box as mail dock; padding/scale so app icons match the envelope.
            DockEndSlot(
                iconModel = dockEndIconModel,
                fallbackIcon = Icons.Rounded.PhotoCamera,
                fallbackResId = thirdDockFallbackResId,
                fallbackScale = 0.84f,
                appIconShape = appIconShape,
                onClick = onCamera,
                onLongPress = onLongPressCamera,
                buttonSize = 68.dp,
                iconSize = 52.dp,
                selected = focused && focusedIndex == cameraFocusIdx,
                selectedTint = selectedTint,
                iconTint = dockIconTint,
                forceMonochrome = false,
            )
        }

        // Flutter `SwipablePageIndicators`: FadeTransition 100ms, full-width bar height = navBarHeight,
        // `pageIndicatorSwipeBackgroundColour` (black), finger dot uses `pageIndicatorSwipeDotColour`.
        if (scrubbing || scrubOverlayAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(0f)
                    .fillMaxWidth()
                    .height(navBarHeight)
                    .alpha(scrubOverlayAlpha)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Dots(
                    current = hoveredPage,
                    count = drawerPageCount,
                    themePalette = themePalette,
                    emphasize = true,
                    hideFirstPageLabel = homeActive,
                )
            }

            val xDp = with(density) { fingerXpx.toDp() }
            val swipeDotSize = 48.dp
            val swipeDotBottomLift = navBarHeight + 28.dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .zIndex(0f)
                    .absoluteOffset(x = xDp - 20.dp, y = -swipeDotBottomLift)
                    .size(swipeDotSize)
                    .shadow(
                        elevation = 10.dp,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        ambientColor = Color(0x42000000),
                        spotColor = Color(0x42000000),
                    )
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xBF000000))
                    .alpha(scrubOverlayAlpha),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (homeActive && hoveredPage == 0) "" else (hoveredPage + 1).toString(),
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontSize = 32.sp,
                        lineHeight = 32.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DockIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    hasUnread: Boolean = false,
    buttonSize: androidx.compose.ui.unit.Dp = 52.dp,
    iconSize: androidx.compose.ui.unit.Dp = 34.dp,
    selected: Boolean = false,
    selectedTint: Color = Color(0x664FC3F7),
    iconTint: Color = Color(0xFFE6E6E6),
) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .size(buttonSize)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .background(if (selected) selectedTint else Color.Transparent)
        ) {
        AppIconWithBadge(hasUnread = hasUnread) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
private fun DockPng(
    resId: Int,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    hasUnread: Boolean = false,
    buttonSize: androidx.compose.ui.unit.Dp = 62.dp,
    iconSize: androidx.compose.ui.unit.Dp = 44.dp,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectedTint: Color = Color(0x664FC3F7),
    iconTint: Color = Color(0xFFE6E6E6),
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(if (selected) selectedTint else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        AppIconWithBadge(hasUnread = hasUnread) {
            Image(
                painter = androidx.compose.ui.res.painterResource(resId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun Dots(
    current: Int,
    count: Int,
    themePalette: LauncherThemePalette,
    emphasize: Boolean = false,
    hideFirstPageLabel: Boolean = false,
) {
    val maxDots = 5
    val dotCount = count.coerceIn(0, maxDots)
    // Do not wrap with modulo: once we reach the last visible dot, keep it there
    // so page numbers progress 5, 6, 7... without jumping back to dot position 1.
    val activeDot = if (dotCount == 0) 0 else current.coerceIn(0, dotCount - 1)
    // Flutter `page_indicator.dart`: black border + black38 shadow (blur ~5, offset y ~2).
    val spotShadow = Color(0x61000000)
    val activeDp = themePalette.pageIndicatorActiveDp.dp
    val inactiveDp = themePalette.pageIndicatorInactiveDp.dp
    val spacingDp = themePalette.pageIndicatorSpacingDp.dp
    val pageColor = themePalette.pageIndicatorColour
    val inactiveAlpha = if (emphasize) 0.92f else 0.69f
    val suppressActiveFirstDot = hideFirstPageLabel && current == 0
    // Flutter `page_indicator.dart`: squircle uses ContinuousRectangleBorder with radius 10.
    val squircleShape = RoundedCornerShape(10.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(dotCount) { i ->
            if (i == activeDot && !suppressActiveFirstDot) {
                val activeShape =
                    if (themePalette.pageIndicatorShapeSquircle) squircleShape else CircleShape
                Box(
                    modifier = Modifier
                        .size(activeDp)
                        .shadow(5.dp, activeShape, ambientColor = spotShadow, spotColor = spotShadow)
                        .clip(activeShape)
                        .background(pageColor)
                        .border(width = 1.dp, color = Color.Black, shape = activeShape)
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (hideFirstPageLabel && current == 0) "" else (current + 1).toString(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.Black,
                            fontSize = themePalette.pageIndicatorFontSp.sp,
                            lineHeight = themePalette.pageIndicatorFontSp.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                val inactiveShape =
                    if (themePalette.pageIndicatorShapeSquircle) squircleShape else CircleShape
                Box(
                    modifier = Modifier
                        .size(inactiveDp)
                        .shadow(5.dp, inactiveShape, ambientColor = spotShadow, spotColor = spotShadow)
                        .clip(inactiveShape)
                        .background(color = pageColor.copy(alpha = inactiveAlpha))
                        .border(1.dp, Color.Black, inactiveShape)
                )
            }
            if (i != dotCount - 1) Spacer(Modifier.width(spacingDp))
        }
    }
}

@Composable
private fun DockEndSlot(
    iconModel: Any?,
    fallbackIcon: ImageVector,
    fallbackResId: Int? = null,
    fallbackScale: Float = 0.84f,
    appIconShape: AppIconShape,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    buttonSize: androidx.compose.ui.unit.Dp = 62.dp,
    iconSize: androidx.compose.ui.unit.Dp = 44.dp,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectedTint: Color = Color(0x664FC3F7),
    iconTint: Color = Color(0xFFE6E6E6),
    forceMonochrome: Boolean = false,
    hasUnread: Boolean = false,
) {
    val resolvedIconModel = remember(iconModel, forceMonochrome, iconTint) {
        if (forceMonochrome) dockMonochromeModel(iconModel, iconTint) else iconModel
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(if (selected) selectedTint else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        AppIconWithBadge(hasUnread = hasUnread) {
            if (resolvedIconModel != null) {
                AsyncImage(
                    model = resolvedIconModel,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(if (forceMonochrome) iconSize * fallbackScale else iconSize)
                        .clip(iconMaskShape(appIconShape)),
                )
            } else if (fallbackResId != null) {
                val fallbackResSize = iconSize * fallbackScale
                Icon(
                    painter = androidx.compose.ui.res.painterResource(fallbackResId),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(fallbackResSize),
                )
            } else {
                Icon(
                    fallbackIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

@Composable
private fun DockShortcutPickerOverlay(
    apps: List<AppEntry>,
    themePalette: LauncherThemePalette,
    slot: DockSlot,
    currentName: String,
    onNameChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onUseDefault: () -> Unit,
    onDismiss: () -> Unit,
    dockSecondEnabled: Boolean = true,
    onToggleDockSecond: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var name by remember(slot, currentName) { mutableStateOf(currentName) }
    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        apps.filter {
            if (it.internal) return@filter false
            if (q.isEmpty()) return@filter true
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(420f),
        color = themePalette.settingsBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    when (slot) {
                        DockSlot.Mail -> "Mail dock shortcut"
                        DockSlot.Shortcut -> "Second dock shortcut"
                        DockSlot.Camera -> "Third dock shortcut"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = themePalette.settingsMenuTitle,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            // Toggle enable/disable — only for the second shortcut slot
            if (slot == DockSlot.Shortcut) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDockSecond() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Show in dock",
                        color = themePalette.settingsMenuTitle,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = dockSecondEnabled,
                        onCheckedChange = { onToggleDockSecond() },
                    )
                }
                HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)
            }
            TextButton(
                onClick = onUseDefault,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    when (slot) {
                        DockSlot.Mail -> "Use default (mail app)"
                        DockSlot.Shortcut -> "Use default (messages app)"
                        DockSlot.Camera -> "Use default (camera app)"
                    },
                    color = Color(0xFF84D5F6),
                )
            }
            // Label field — hidden for second shortcut (app label is used directly)
            if (slot != DockSlot.Shortcut) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onNameChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Shortcut label", color = themePalette.settingsMenuBody) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = themePalette.settingsMenuTitle,
                        unfocusedTextColor = themePalette.settingsMenuTitle,
                        focusedContainerColor = Color(0xFF1E2430),
                        unfocusedContainerColor = Color(0xFF1E2430),
                    ),
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search apps", color = themePalette.settingsMenuBody) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = themePalette.settingsMenuTitle,
                    unfocusedTextColor = themePalette.settingsMenuTitle,
                    focusedLabelColor = themePalette.settingsMenuBody,
                    unfocusedLabelColor = themePalette.settingsMenuBody,
                    focusedContainerColor = Color(0xFF1E2430),
                    unfocusedContainerColor = Color(0xFF1E2430),
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search,
                ),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app.packageName) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = app.icon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = themePalette.settingsMenuTitle,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = themePalette.settingsMenuBody,
                                    fontSize = 12.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 70.dp)
                            .height(0.5.dp)
                            .background(Color(0xFF2C3340)),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeGroupsSettingsOverlay(
    groups: List<HomeGroup>,
    allApps: List<AppEntry>,
    themePalette: LauncherThemePalette,
    onCreateGroup: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val subtitleColor = Color(0xFF8E95A3)
    var newName by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(404f),
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
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    "Home strip",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = themePalette.settingsMenuTitle,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Groups appear on the home strip. Long-press and drag to rearrange slots. " +
                        "Long-press an app in the drawer and use Add to… to include it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
                // ── Create new group ──────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = capitalizeFirstLetterForGroupInput(it) },
                        singleLine = true,
                        label = { Text("New group name", color = themePalette.settingsMenuBody) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE8EEF7),
                            unfocusedTextColor = Color(0xFFE8EEF7),
                            focusedLabelColor = themePalette.settingsMenuBody,
                            unfocusedLabelColor = themePalette.settingsMenuBody,
                            focusedIndicatorColor = Color(0xFF5B9BD5),
                            unfocusedIndicatorColor = Color(0xFF5F6A78),
                            focusedContainerColor = Color(0xFF1E2430),
                            unfocusedContainerColor = Color(0xFF1E2430),
                            disabledContainerColor = Color(0xFF1E2430),
                            cursorColor = Color(0xFF84D5F6),
                            focusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                            unfocusedPlaceholderColor = themePalette.settingsMenuBody.copy(alpha = 0.7f),
                            disabledTextColor = Color(0xFF8E95A3),
                            disabledLabelColor = themePalette.settingsMenuBody.copy(alpha = 0.5f),
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreateGroup(newName)
                                newName = ""
                            }
                        },
                        enabled = newName.isNotBlank(),
                    ) {
                        Text(
                            "Add",
                            color = if (newName.isNotBlank()) themePalette.settingsMenuBody else subtitleColor,
                        )
                    }
                }
                // ── Existing groups ───────────────────────────────────────
                if (groups.isNotEmpty()) {
                    Text(
                        "Groups",
                        style = MaterialTheme.typography.titleSmall,
                        color = themePalette.settingsMenuTitle,
                    )
                }
                groups.forEach { g ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E2430),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    g.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = themePalette.settingsMenuTitle,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "${g.packageNames.size} apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subtitleColor,
                                )
                            }
                            TextButton(onClick = { onDeleteGroup(g.id) }) {
                                Text("Delete", color = Color(0xFFFF6B6B))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlanceSettingsOverlay(
    glanceEnabled: Boolean,
    glanceShowFlashlight: Boolean,
    glanceShowBattery: Boolean,
    glanceShowCalendar: Boolean,
    glanceShowAlarm: Boolean,
    glanceWeatherUnit: GlanceWeatherUnit,
    themePalette: LauncherThemePalette,
    onGlanceEnabled: (Boolean) -> Unit,
    onGlanceShowFlashlight: (Boolean) -> Unit,
    onGlanceShowBattery: (Boolean) -> Unit,
    onGlanceShowCalendar: (Boolean) -> Unit,
    onGlanceShowAlarm: (Boolean) -> Unit,
    onGlanceWeatherUnit: (GlanceWeatherUnit) -> Unit,
    onDismiss: () -> Unit,
) {
    val subtitleColor = Color(0xFF8E95A3)
    val cardBg = Color(0xFF1E2430)
    val cardShape = RoundedCornerShape(12.dp)

    @Composable
    fun ToggleCard(
        title: String,
        subtitle: String,
        checked: Boolean,
        enabled: Boolean = true,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        Surface(
            shape = cardShape,
            color = cardBg,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = themePalette.settingsMenuTitle,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = subtitleColor),
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = if (enabled) onCheckedChange else null,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4A90D9),
                        uncheckedThumbColor = Color(0xFF9AA0A8),
                        uncheckedTrackColor = Color(0xFF3A3F4A),
                    ),
                )
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(403f),
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
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    "Glance",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = themePalette.settingsMenuTitle,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Home strip: date, weather, and optional alerts. Turn the strip off to hide it completely.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
                ToggleCard(
                    title = "Show glance strip",
                    subtitle = "Master switch for the top-of-home strip",
                    checked = glanceEnabled,
                    onCheckedChange = onGlanceEnabled,
                )
                Surface(shape = cardShape, color = cardBg, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "Weather unit",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = themePalette.settingsMenuTitle,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141A23))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (glanceWeatherUnit == GlanceWeatherUnit.CELSIUS) Color(0xFF4A90D9) else Color.Transparent)
                                    .clickable(enabled = glanceEnabled) { onGlanceWeatherUnit(GlanceWeatherUnit.CELSIUS) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Celsius",
                                    color = if (glanceWeatherUnit == GlanceWeatherUnit.CELSIUS) Color.White else subtitleColor,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (glanceWeatherUnit == GlanceWeatherUnit.FAHRENHEIT) Color(0xFF4A90D9) else Color.Transparent)
                                    .clickable(enabled = glanceEnabled) { onGlanceWeatherUnit(GlanceWeatherUnit.FAHRENHEIT) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Fahrenheit",
                                    color = if (glanceWeatherUnit == GlanceWeatherUnit.FAHRENHEIT) Color.White else subtitleColor,
                                )
                            }
                        }
                    }
                }
                ToggleCard(
                    title = "Flashlight on glance",
                    subtitle = "Show an alert while the torch is on",
                    checked = glanceShowFlashlight,
                    enabled = glanceEnabled,
                    onCheckedChange = onGlanceShowFlashlight,
                )
                ToggleCard(
                    title = "Calendar on glance",
                    subtitle = "Upcoming events when permission is granted",
                    checked = glanceShowCalendar,
                    enabled = glanceEnabled,
                    onCheckedChange = onGlanceShowCalendar,
                )
                ToggleCard(
                    title = "Battery on glance",
                    subtitle = "Charging or low-battery hint (when supported)",
                    checked = glanceShowBattery,
                    enabled = glanceEnabled,
                    onCheckedChange = onGlanceShowBattery,
                )
                ToggleCard(
                    title = "Next alarm on glance",
                    subtitle = "Next alarm within 12 hours (when supported)",
                    checked = glanceShowAlarm,
                    enabled = glanceEnabled,
                    onCheckedChange = onGlanceShowAlarm,
                )
            }
        }
    }
}

@Composable
private fun IconAppearanceSettingsOverlay(
    gridPreset: GridPreset,
    appIconShape: AppIconShape,
    showAppCardBackground: Boolean,
    drawerBadgesSubtitle: String,
    onGridPreset: (GridPreset) -> Unit,
    onSetAppIconShape: (AppIconShape) -> Unit,
    onToggleAppCardBackground: () -> Unit,
    onOpenDrawerBadges: () -> Unit,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var selectedIndex by remember { mutableStateOf(0) }
    val itemCount = 4
    val subtitleColor = Color(0xFF8E95A3)
    val cardBg = Color(0xFF1E2430)
    val cardShape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(403f),
        color = themePalette.settingsBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    "App icons",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = themePalette.settingsMenuTitle,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { ev ->
                        if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val nk = ev.nativeKeyEvent
                        when {
                            ev.key == Key.Back || nk?.keyCode == AndroidKeyEvent.KEYCODE_BACK || ev.isEndCallKey() -> {
                                onDismiss()
                                true
                            }
                            ev.key == Key.DirectionUp || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                val next = (selectedIndex - 1).coerceAtLeast(0)
                                if (next != selectedIndex) {
                                    selectedIndex = next
                                }
                                true
                            }
                            ev.key == Key.DirectionDown || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                val next = (selectedIndex + 1).coerceAtMost(itemCount - 1)
                                if (next != selectedIndex) {
                                    selectedIndex = next
                                }
                                true
                            }
                            ev.key == Key.Enter ||
                                ev.key == Key.NumPadEnter ||
                                nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                                nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER -> {
                                when (selectedIndex) {
                                    0 -> {
                                        val all = GridPreset.entries
                                        onGridPreset(all[(all.indexOf(gridPreset) + 1) % all.size])
                                    }
                                    1 -> {
                                        val all = AppIconShape.entries
                                        onSetAppIconShape(all[(all.indexOf(appIconShape) + 1) % all.size])
                                    }
                                    2 -> onToggleAppCardBackground()
                                    3 -> onOpenDrawerBadges()
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 0) {
                    SettingsRow(
                        icon = Icons.Rounded.GridView,
                        title = "App grid size",
                        subtitle = "${gridPreset.rows} × ${gridPreset.cols}",
                        selected = selectedIndex == 0,
                        themePalette = themePalette,
                        subtitleColor = subtitleColor,
                        onClick = {
                            selectedIndex = 0
                            val all = GridPreset.entries
                            onGridPreset(all[(all.indexOf(gridPreset) + 1) % all.size])
                        },
                    )
                }
                SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 1) {
                    SettingsRow(
                        icon = Icons.Rounded.Apps,
                        title = "App icon shape",
                        subtitle = appIconShapeLabel(appIconShape),
                        selected = selectedIndex == 1,
                        themePalette = themePalette,
                        subtitleColor = subtitleColor,
                        onClick = {
                            selectedIndex = 1
                            val all = AppIconShape.entries
                            onSetAppIconShape(all[(all.indexOf(appIconShape) + 1) % all.size])
                        },
                    )
                }
                SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 2) {
                    SettingsRow(
                        icon = Icons.Rounded.GridView,
                        title = "App card background",
                        subtitle = if (showAppCardBackground) "Gradient card shown behind each app" else "No background behind app icons",
                        selected = selectedIndex == 2,
                        themePalette = themePalette,
                        subtitleColor = subtitleColor,
                        onClick = {
                            selectedIndex = 2
                            onToggleAppCardBackground()
                        },
                        trailingContent = {
                            Switch(
                                checked = showAppCardBackground,
                                onCheckedChange = { onToggleAppCardBackground() },
                            )
                        },
                    )
                }
                SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 3) {
                    SettingsRow(
                        icon = Icons.Outlined.QueryStats,
                        title = "App icon badges",
                        subtitle = drawerBadgesSubtitle,
                        selected = selectedIndex == 3,
                        themePalette = themePalette,
                        subtitleColor = subtitleColor,
                        onClick = {
                            selectedIndex = 3
                            onOpenDrawerBadges()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreenOverlay(
    /** When true, a full-screen settings sub-overlay is shown above this list — Back must not dismiss settings. */
    stackedChildOverlayOpen: Boolean,
    gridPreset: GridPreset,
    dockMailTitle: String,
    dockSecondTitle: String,
    dockThirdTitle: String,
    dockMailBody: String,
    dockSecondBody: String,
    dockThirdBody: String,
    glanceSubtitle: String,
    homeGroupsSubtitle: String,
    hapticsEnabled: Boolean,
    hapticIntensity: Int,
    onSetHapticIntensity: (Int) -> Unit,
    onGridPreset: (GridPreset) -> Unit,
    onOpenDockSlotPicker: (DockSlot) -> Unit,
    onOpenGlanceSettings: () -> Unit,
    homeStripEnabled: Boolean,
    onToggleHomeStrip: (Boolean) -> Unit,
    onOpenHomeGroupsSettings: () -> Unit,
    permissionsSubtitle: String,
    onOpenPermissionsSettings: () -> Unit,
    onSetWallpaper: () -> Unit,
    onToggleHaptics: () -> Unit,
    onExportBackup: () -> String,
    /** @return true if backup was accepted and will be applied. */
    onImportBackup: (String) -> Boolean,
    onResetTheme: () -> Unit,
    gestureSubtitle: String,
    onOpenGestureSettings: () -> Unit,
    drawerBadgesSubtitle: String,
    classicMode: Boolean,
    onToggleClassicMode: () -> Unit,
    dockSecondEnabled: Boolean,
    onToggleDockSecond: () -> Unit,
    appIconShape: AppIconShape,
    onSetAppIconShape: (AppIconShape) -> Unit,
    dockIconStyle: DockIconStyle,
    onSetDockIconStyle: (DockIconStyle) -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var selectedIndex by remember { mutableStateOf(0) }
    val itemCount = 14

    val createBackupDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val json = onExportBackup()
            val out = context.contentResolver.openOutputStream(uri)
                ?: error("Could not open file for writing")
            out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Backup failed: ${it.message ?: "unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }

    val openBackupFromDownloads = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (!SettingsDownloads.isPickerOk(result.resultCode)) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { ins ->
                ins.readBytes().toString(Charsets.UTF_8)
            } ?: return@runCatching
            if (onImportBackup(text)) {
                Toast.makeText(context, "Settings restored from backup", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Invalid or unsupported backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun activate(index: Int) {
        when (index) {
            // HOME SCREEN
            0 -> onOpenAppearanceSettings()
            1 -> onToggleClassicMode()
            2 -> onOpenHomeGroupsSettings()
            3 -> onOpenGestureSettings()
            // DISPLAY
            4 -> onOpenGlanceSettings()
            5 -> onSetWallpaper()
            6 -> onResetTheme()
            // DOCK
            7 -> onOpenDockSlotPicker(DockSlot.Mail)
            8 -> onOpenDockSlotPicker(DockSlot.Shortcut)
            9 -> onOpenDockSlotPicker(DockSlot.Camera)
            // SYSTEM
            10 -> onToggleHaptics()
            11 -> onOpenPermissionsSettings()
            // BACKUP
            12 -> {
                val name = "classiclauncher_backup_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".json"
                createBackupDocument.launch(name)
            }
            13 -> openBackupFromDownloads.launch(SettingsDownloads.openBackupJsonPickerIntent())
        }
        doNavFeedback(view, hapticsEnabled, hapticIntensity)
        scope.launch { focusRequester.requestFocus() }
    }

    val cardBg = Color(0xFF1E2430)
    val cardShape = RoundedCornerShape(16.dp)
    val subtitleColor = Color(0xFF8E95A3)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(400f),
        color = themePalette.settingsBg,
    ) {
        val focusManager = LocalFocusManager.current
        val settingsScrollState = rememberScrollState()
        val rowBringers = remember { List(itemCount) { BringIntoViewRequester() } }
        // Steal focus from drawer/pager (still composed under us) so trackpad/DPAD events hit settings.
        LaunchedEffect(Unit) {
            focusManager.clearFocus(force = true)
            focusRequester.requestFocus()
        }
        LaunchedEffect(classicMode) {
            focusRequester.requestFocus()
        }
        // BB Classic / Q20 trackpad sends DPAD up/down: we move selection but must scroll the viewport too.
        LaunchedEffect(selectedIndex) {
            rowBringers[selectedIndex].bringIntoView()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = themePalette.settingsMenuTitle,
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))

            // verticalScroll + DPAD: Q20-style trackpad emits KEYCODE_DPAD_UP/DOWN, not mouse wheel — selection
            // must drive bringIntoView so the highlighted row stays on screen.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val nk = ev.nativeKeyEvent
                        when {
                            ev.isEndCallKey() -> {
                                if (stackedChildOverlayOpen) return@onPreviewKeyEvent false
                                onDismiss()
                                true
                            }
                            nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                                activate(selectedIndex)
                                true
                            }
                            ev.key == Key.DirectionUp || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                            val next = (selectedIndex - 1).coerceAtLeast(0)
                            if (next != selectedIndex) {
                                selectedIndex = next
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                            }
                            true
                        }
                            ev.key == Key.DirectionDown || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                val next = (selectedIndex + 1).coerceAtMost(itemCount - 1)
                            if (next != selectedIndex) {
                                selectedIndex = next
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                            }
                            true
                        }
                            ev.key == Key.Enter ||
                                ev.key == Key.NumPadEnter ||
                                nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER -> {
                            activate(selectedIndex)
                            true
                        }
                            ev.key == Key.Back || nk?.keyCode == AndroidKeyEvent.KEYCODE_BACK -> {
                            if (stackedChildOverlayOpen) return@onPreviewKeyEvent false
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                }
                    .verticalScroll(settingsScrollState),
            ) {
                // ── HOME SCREEN ──────────────────────────────────────────
                Text(
                    "HOME SCREEN",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[0])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 0) {
                        SettingsRow(
                            icon = Icons.Rounded.Apps,
                            title = "App icons",
                            subtitle = "Grid size, icon shape, badges",
                            selected = selectedIndex == 0,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(0) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[1])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 1) {
                        SettingsRow(
                            icon = Icons.Outlined.Menu,
                            title = "Classic mode",
                            subtitle = if (classicMode) {
                                "On — app grid only; compact dock (no home / Messages shortcut); glance off"
                            } else {
                                "Off — home page, full dock, and glance strip when enabled"
                            },
                            selected = selectedIndex == 1,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(1) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[2])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 2) {
                        SettingsRow(
                            icon = Icons.Rounded.BookmarkAdd,
                            title = "Home strip",
                            subtitle = if (homeStripEnabled) homeGroupsSubtitle else "Off",
                            selected = selectedIndex == 2,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { if (homeStripEnabled) activate(2) },
                            trailingContent = {
                                Switch(
                                    checked = homeStripEnabled,
                                    onCheckedChange = onToggleHomeStrip,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF4A90D9),
                                        uncheckedThumbColor = Color(0xFF9AA0A8),
                                        uncheckedTrackColor = Color(0xFF3A3F4A),
                                    ),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[3])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 3) {
                        SettingsRow(
                            icon = Icons.Rounded.TouchApp,
                            title = "Home gestures",
                            subtitle = gestureSubtitle,
                            selected = selectedIndex == 3,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(3) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A3040), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // ── DISPLAY ──────────────────────────────────────────────
                Text(
                    "DISPLAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[4])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 4) {
                        SettingsRow(
                            icon = Icons.Rounded.WbSunny,
                            title = "Glance",
                            subtitle = glanceSubtitle,
                            selected = selectedIndex == 4,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(4) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[5])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 5) {
                        SettingsRow(
                            icon = Icons.Rounded.Wallpaper,
                            title = "Set wallpaper",
                            subtitle = "Zeno wallpapers or Wallpapers & style",
                            selected = selectedIndex == 5,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(5) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[6])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 6) {
                        SettingsRow(
                            icon = Icons.Rounded.Palette,
                            title = "Reset theme",
                            subtitle = "Restore default launcher colours",
                            selected = selectedIndex == 6,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(6) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A3040), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // ── DOCK ─────────────────────────────────────────────────
                Text(
                    "DOCK",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[7])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 7) {
                        SettingsRow(
                            icon = Icons.Rounded.MailOutline,
                            title = dockMailTitle,
                            subtitle = dockMailBody,
                            selected = selectedIndex == 7,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(7) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[8])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 8) {
                        SettingsRow(
                            icon = Icons.Rounded.TouchApp,
                            title = dockSecondTitle,
                            subtitle = if (!dockSecondEnabled) "Hidden from dock" else dockSecondBody + if (classicMode) " · hidden in Classic mode" else "",
                            selected = selectedIndex == 8,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { selectedIndex = 8; activate(8) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[9])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 9) {
                        SettingsRow(
                            icon = Icons.Rounded.Apps,
                            title = dockThirdTitle,
                            subtitle = dockThirdBody,
                            selected = selectedIndex == 9,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(9) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A3040), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // ── SYSTEM ───────────────────────────────────────────────
                Text(
                    "SYSTEM",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[10])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 10) {
                        SettingsRow(
                            icon = Icons.Outlined.Vibration,
                            title = "Haptic feedback",
                            subtitle = if (hapticsEnabled) "On (trackpad and keyboard navigation)" else "Off",
                            selected = selectedIndex == 10,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(10) },
                        )
                        AnimatedVisibility(
                            visible = hapticsEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column(modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Light", style = MaterialTheme.typography.labelSmall, color = subtitleColor)
                                    Text(
                                        "Intensity: $hapticIntensity",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = themePalette.settingsMenuTitle,
                                    )
                                    Text("Strong", style = MaterialTheme.typography.labelSmall, color = subtitleColor)
                                }
                                Slider(
                                    value = hapticIntensity.toFloat(),
                                    onValueChange = { onSetHapticIntensity(it.toInt()) },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[11])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 11) {
                        SettingsRow(
                            icon = Icons.Rounded.Security,
                            title = "Permissions",
                            subtitle = permissionsSubtitle,
                            selected = selectedIndex == 11,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(11) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A3040), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // ── BACKUP ───────────────────────────────────────────────
                Text(
                    "BACKUP",
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[12])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 12) {
                        SettingsRow(
                            icon = Icons.Rounded.SettingsBackupRestore,
                            title = "Export backup",
                            subtitle = "Save settings as JSON file",
                            selected = selectedIndex == 12,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(12) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[13])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 13) {
                        SettingsRow(
                            icon = Icons.Rounded.SettingsBackupRestore,
                            title = "Restore from backup",
                            subtitle = "Pick a backup file from Downloads",
                            selected = selectedIndex == 13,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(13) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    cardBg: Color,
    cardShape: RoundedCornerShape,
    selected: Boolean,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = cardShape,
        color = if (selected) Color(0xFF252D3E) else cardBg,
        border = if (selected) BorderStroke(1.dp, Color(0x6684D5F6)) else null,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    themePalette: LauncherThemePalette,
    subtitleColor: Color,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val bgColor = if (selected) themePalette.settingsSelected else Color.Transparent
    val titleColor = if (selected) themePalette.settingsMenuTitleSelected else themePalette.settingsMenuTitle
    val subColor = if (selected) themePalette.settingsMenuBodySelected else subtitleColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF84D5F6) else Color(0xFF7A8290),
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = titleColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = subColor,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Composable
private fun AppSpotlightOverlay(
    allApps: List<AppEntry>,
    hiddenPackages: Set<String>,
    themePalette: LauncherThemePalette,
    initialQuery: String = "",
    onLaunchApp: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    var focusIndex by remember { mutableIntStateOf(-1) }

    val baseApps = remember(allApps, hiddenPackages) {
        allApps.filter { it.packageName !in hiddenPackages }
    }
    val visibleApps = remember(baseApps, query) {
        if (query.isEmpty()) baseApps
        else baseApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    // Alphabet index: letter → first index in baseApps (only used when not searching)
    val letterIndex: Map<Char, Int> = remember(baseApps) {
        val map = linkedMapOf<Char, Int>()
        baseApps.forEachIndexed { idx, app ->
            val ch = app.label.firstOrNull()?.uppercaseChar() ?: return@forEachIndexed
            if (!map.containsKey(ch)) map[ch] = idx
        }
        map
    }
    val presentLetters = remember(letterIndex) { letterIndex.keys.toList() }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Entrance animation — fade + slide down from slightly above
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val animAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "spotlightFade",
    )
    val animOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else -40f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "spotlightSlide",
    )

    LaunchedEffect(focusIndex, visibleApps.size) {
        if (focusIndex in visibleApps.indices) listState.animateScrollToItem(focusIndex)
    }
    LaunchedEffect(query) { focusIndex = -1 }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(500f)
            // Subtle dark scrim — wallpaper still shows but text is readable
            .background(Color(0x33000000))
            .graphicsLayer { alpha = animAlpha; translationY = animOffsetY }
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (ev.key) {
                    Key.DirectionDown -> {
                        focusIndex = (focusIndex + 1).coerceAtMost(visibleApps.lastIndex)
                        true
                    }
                    Key.DirectionUp -> {
                        focusIndex = (focusIndex - 1).coerceAtLeast(0)
                        true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        if (focusIndex in visibleApps.indices) {
                            onLaunchApp(visibleApps[focusIndex]); true
                        } else false
                    }
                    Key.Escape, Key.Back -> { onDismiss(); true }
                    Key.Backspace -> {
                        if (query.isNotEmpty()) { query = query.dropLast(1); true } else false
                    }
                    else -> {
                        val ch = keyToTypedChar(ev.key)
                        if (ch != null) { query += ch; true } else false
                    }
                }
            },
    ) {
        // Swipe-down to dismiss
        val swipeDismissModifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {},
                onDrag = { _, dragAmount ->
                    if (dragAmount.y > 40f) onDismiss()
                },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(swipeDismissModifier),
        ) {
            Spacer(Modifier.height(16.dp))

            // Centered pill search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x99000000), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color(0xAAFFFFFF),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    if (query.isEmpty()) {
                        Text(
                            "Search apps…",
                            color = Color(0x88FFFFFF),
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            query,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Clear",
                            tint = Color(0xAAFFFFFF),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { query = "" },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // App list + alphabet sidebar
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(
                        items = visibleApps,
                        key = { app: AppEntry -> app.packageName },
                    ) { app: AppEntry ->
                        val idx = visibleApps.indexOf(app)
                        val isFocused = idx == focusIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isFocused) Color(0x44FFFFFF)
                                    else Color.Transparent,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { onLaunchApp(app) }
                                .padding(horizontal = 20.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = app.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(44.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Right alphabet sidebar — hidden while searching
                if (query.isEmpty() && presentLetters.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .width(22.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        presentLetters.forEach { letter ->
                            Text(
                                text = letter.toString(),
                                color = Color(0xCCFFFFFF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val targetIdx = letterIndex[letter] ?: return@clickable
                                        scope.launch { listState.animateScrollToItem(targetIdx) }
                                    }
                                    .padding(vertical = 1.5.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun android.content.Context.findHostActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findHostActivity()
        else -> null
    }

