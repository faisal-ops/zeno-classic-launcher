@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.zeno.classiclauncher.nlauncher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.KeyEvent as AndroidKeyEvent
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
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.AddAlarm
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Language
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontFamily
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zeno.classiclauncher.nlauncher.glance.GlanceDateWeatherEventsView
import com.zeno.classiclauncher.nlauncher.glance.GlanceStripPreferences
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import com.zeno.classiclauncher.nlauncher.apps.IconPackEntry
import com.zeno.classiclauncher.nlauncher.apps.IconPackRepository
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions
import com.zeno.classiclauncher.nlauncher.apps.SoundProfileMode
import com.zeno.classiclauncher.nlauncher.apps.ToggleResult
import com.zeno.classiclauncher.nlauncher.apps.parseHomeShortcutToken
import com.zeno.classiclauncher.nlauncher.BuildConfig
import com.zeno.classiclauncher.nlauncher.badges.AppIconWithBadge
import com.zeno.classiclauncher.nlauncher.badges.BadgeNotificationListener
import com.zeno.classiclauncher.nlauncher.power.SleepManager
import com.zeno.classiclauncher.nlauncher.folders.DrawerGridCell
import com.zeno.classiclauncher.nlauncher.folders.FolderIds
import com.zeno.classiclauncher.nlauncher.locale.LauncherLocale
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette
import com.zeno.classiclauncher.nlauncher.prefs.GridPreset
import com.zeno.classiclauncher.nlauncher.prefs.GlanceWeatherUnit
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroup
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroupIds
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroupSide
import com.zeno.classiclauncher.nlauncher.prefs.HomeWidgetConfig
import com.zeno.classiclauncher.nlauncher.prefs.STRIP_TOTAL_SLOTS
import com.zeno.classiclauncher.nlauncher.prefs.canAddHomeStripItem
import com.zeno.classiclauncher.nlauncher.prefs.effectiveHomeStripSlotOrder
import com.zeno.classiclauncher.nlauncher.prefs.moveHomeStripSlot
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefs
import com.zeno.classiclauncher.nlauncher.prefs.AppIconShape
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
import kotlin.math.sin
import kotlin.math.PI
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
private const val HOME_GRID_COLS = 4
private const val HOME_GRID_ROWS = 4
private val HOME_SHORTCUT_ICON_DP = 52.dp
private val HOME_SHORTCUT_FALLBACK_ICON_DP = 48.dp
private val HOME_STRIP_LABEL_COLOR = Color(0xFFE8EEF7)
/** Compact strip captions shared by apps, folders, and groups. */
private val HOME_STRIP_LABEL_FONT_SP = 11.sp
private val HOME_STRIP_LABEL_LINE_SP = 13.sp
/** Space between the icon tile and caption across all home-strip item types. */
private val HOME_STRIP_ICON_LABEL_GAP = 4.dp
/** Centre shortcuts: tight fixed gap between slots. */
private val HOME_STRIP_SHORTCUT_GAP = 4.dp
private val HOME_STRIP_FOCUS_RADIUS = 5.dp
private val HOME_STRIP_FOCUS_INSET = 1.dp
private val HOME_STRIP_CONTENT_VERTICAL_INSET = 2.dp

/**
 * Whether the focus highlight is currently visible.
 * True only while the trackpad/D-pad is in active use; auto-hides after [TRACKPAD_IDLE_HIDE_MS].
 * Default false = highlight hidden until first trackpad event.
 */
private val LocalTrackpadActive = androidx.compose.runtime.compositionLocalOf { false }
private const val TRACKPAD_IDLE_HIDE_MS = 2000L
private const val HOME_STRIP_DND_TAG = "HomeStripDnD"
private inline fun logHomeStripDnD(message: () -> String) {
    if (BuildConfig.DEBUG) Log.d(HOME_STRIP_DND_TAG, message())
}
private val textHandleMoveFeedback: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        HapticFeedbackConstants.TEXT_HANDLE_MOVE
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }
private const val QS_DEBUG_TAG = "QuickSettings"
private inline fun logQuickSettings(message: () -> String) {
    if (BuildConfig.DEBUG) Log.d(QS_DEBUG_TAG, message())
}

private fun homeStripIconSize(iconSizeDp: Float): Dp =
    (iconSizeDp + 2f).dp

private val OUTLINE_OFFSETS = arrayOf(
    Offset(-0.8f, -0.8f),
    Offset(0.8f, -0.8f),
    Offset(0.8f, 0.8f),
    Offset(-0.8f, 0.8f),
)
private val MAIN_SHADOW_OFFSET = Offset(0f, 1.5f)

private val VISIBLE_ICON_SHAPES = listOf(
    AppIconShape.SQUARE,
    AppIconShape.SOFT_SQUARE,
    AppIconShape.SQUIRCLE,
    AppIconShape.CIRCLE,
    AppIconShape.CUT_CORNER,
)
private const val DEFAULT_APP_ICON_SIZE_DP = 52f
private const val MIN_APP_ICON_SIZE_DP = 44f
private const val MAX_APP_ICON_SIZE_DP = 80f
private val ICON_SETTINGS_PREVIEW_HEIGHT = 115.dp
private val ICON_SETTINGS_PREVIEW_HEIGHT_LARGE = 160.dp
private val ICON_SETTINGS_PREVIEW_CELL_WIDTH = 96.dp
private val ICON_SETTINGS_PREVIEW_LABEL_SP = 12
private val ICON_SETTINGS_PREVIEW_CARD_SHAPE = RoundedCornerShape(10.dp)
private val SETTINGS_TITLE_TEXT_SP = 18.sp
private val SETTINGS_BODY_TEXT_SP = 14.sp
private val SETTINGS_VALUE_TEXT_SP = 15.sp

private fun iconMaskShape(shape: AppIconShape): Shape = when (shape) {
    AppIconShape.SQUARE -> RectangleShape
    AppIconShape.ROUNDED -> RoundedCornerShape(9.dp) // Legacy saved value; no longer exposed in settings.
    AppIconShape.SQUIRCLE -> RoundedCornerShape(18.dp)
    AppIconShape.CIRCLE -> CircleShape
    AppIconShape.SOFT_SQUARE -> RoundedCornerShape(9.dp)
    AppIconShape.CUT_CORNER -> CutCornerShape(10.dp)
}

private fun appIconShapeLabel(shape: AppIconShape): String = when (shape) {
    AppIconShape.SQUARE -> "Square"
    AppIconShape.ROUNDED -> "Soft square"
    AppIconShape.SQUIRCLE -> "Squircle"
    AppIconShape.CIRCLE -> "Circle"
    AppIconShape.SOFT_SQUARE -> "Soft square"
    AppIconShape.CUT_CORNER -> "Cut corner"
}

@SuppressLint("MissingPermission")
private fun safeWallpaperDrawable(context: android.content.Context): Drawable? =
    runCatching { WallpaperManager.getInstance(context).drawable }.getOrNull()

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
    val location: Boolean,
    val calendar: Boolean,
)

private fun computePermRuntime(context: android.content.Context): PermRuntime =
    PermRuntime(
        notificationAccess = isNotificationListenerEnabled(context),
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

private data class HomeWidgetSpan(val cols: Int, val rows: Int)

private data class HomeWidgetSizeOption(
    val label: String,
    val span: HomeWidgetSpan,
    val recommended: Boolean = false,
)

private fun estimateHomeWidgetSpan(info: AppWidgetProviderInfo): HomeWidgetSpan {
    val minWidth = listOf(info.minResizeWidth, info.minWidth).filter { it > 0 }.minOrNull() ?: info.minWidth
    val minHeight = listOf(info.minResizeHeight, info.minHeight).filter { it > 0 }.minOrNull() ?: info.minHeight
    val cols = when {
        minWidth <= 96 -> 1
        minWidth <= 180 -> 2
        minWidth <= 270 -> 3
        else -> 4
    }
    val rows = when {
        minHeight <= 88 -> 1
        minHeight <= 180 -> 2
        minHeight <= 270 -> 3
        else -> 4
    }
    return HomeWidgetSpan(
        cols = cols.coerceIn(1, HOME_GRID_COLS),
        rows = rows.coerceIn(1, HOME_GRID_ROWS),
    )
}

private fun homeWidgetSizeOptions(info: AppWidgetProviderInfo): List<HomeWidgetSizeOption> {
    val min = estimateHomeWidgetSpan(info)
    val candidates = listOf(
        HomeWidgetSizeOption("Compact", min),
        HomeWidgetSizeOption(
            "Standard",
            HomeWidgetSpan(
                cols = maxOf(min.cols, min.cols.coerceAtLeast(2)).coerceAtMost(HOME_GRID_COLS),
                rows = maxOf(min.rows, min.rows.coerceAtLeast(2)).coerceAtMost(HOME_GRID_ROWS),
            ),
            recommended = true,
        ),
        HomeWidgetSizeOption(
            "Expanded",
            HomeWidgetSpan(
                cols = HOME_GRID_COLS,
                rows = maxOf(min.rows, (min.rows + 1).coerceAtLeast(2)).coerceAtMost(HOME_GRID_ROWS),
            ),
        ),
    )
    return candidates.distinctBy { "${it.span.cols}x${it.span.rows}" }.take(3)
}

private fun normalizedHomeWidgetConfig(
    config: HomeWidgetConfig,
    fallback: HomeWidgetSpan = HomeWidgetSpan(4, 2),
): HomeWidgetConfig {
    val cols = config.cols.takeIf { it > 0 } ?: fallback.cols
    val rows = config.rows.takeIf { it > 0 } ?: fallback.rows
    val c = cols.coerceIn(1, HOME_GRID_COLS)
    val r = rows.coerceIn(1, HOME_GRID_ROWS)
    return config.copy(
        cols = c,
        rows = r,
        col = config.col.coerceIn(0, HOME_GRID_COLS - c),
        row = config.row.coerceIn(0, HOME_GRID_ROWS - r),
    )
}

private fun minimumCenteredRect(bounds: Rect, minSizePx: Float): Rect {
    val width = maxOf(bounds.width, minSizePx)
    val height = maxOf(bounds.height, minSizePx)
    val centerX = (bounds.left + bounds.right) / 2f
    val centerY = (bounds.top + bounds.bottom) / 2f
    return Rect(
        left = centerX - width / 2f,
        top = centerY - height / 2f,
        right = centerX + width / 2f,
        bottom = centerY + height / 2f,
    )
}

private data class AddToContainerTarget(
    val id: String,
    val title: String,
    val existingPackageNames: Set<String>,
    val isHomeGroup: Boolean,
)

private enum class HomeNavArea { Strip, Dock }

@Composable
fun LauncherScreen(
    vm: LauncherViewModel = viewModel(),
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    // Centralised adaptive layout — recomputes on rotation, fold, DPI/display-size changes.
    val adaptiveLayout = rememberAdaptiveLayout()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val allApps by vm.apps.collectAsStateWithLifecycle()
    val gridCells by vm.filteredGridCells.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val reorderMode by vm.isReorderMode.collectAsStateWithLifecycle()
    val moving by vm.moving.collectAsStateWithLifecycle()
    val hasUnreadMail by vm.hasUnreadMail.collectAsStateWithLifecycle()
    val hasUnreadSms by vm.hasUnreadSms.collectAsStateWithLifecycle()
    val hasUnreadWhatsApp by vm.hasUnreadWhatsApp.collectAsStateWithLifecycle()
    val unreadPackages by vm.packagesWithUnread.collectAsStateWithLifecycle()
    val drawerSortMode by vm.drawerSortMode.collectAsStateWithLifecycle()
    val newAppAddedToast by vm.newAppAddedToast.collectAsStateWithLifecycle()
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
    val dockStartIconModel = remember(prefs.dockMailPackage, prefs.customIconPackages, allApps) {
        com.zeno.classiclauncher.nlauncher.apps.CustomIconStore.load(context, com.zeno.classiclauncher.nlauncher.apps.CustomIconStore.DOCK_MAIL_KEY)
            ?: run {
                val pkg = prefs.dockMailPackage.trim()
                if (keepEnvelopeForMail(pkg)) null else appIconFor(pkg)
            }
    }
    val dockMiddleIconModel = remember(prefs.dockSecondPackage, prefs.customIconPackages, allApps) {
        com.zeno.classiclauncher.nlauncher.apps.CustomIconStore.load(context, com.zeno.classiclauncher.nlauncher.apps.CustomIconStore.DOCK_SHORTCUT_KEY)
            ?: run {
                val pkg = prefs.dockSecondPackage.trim()
                if (keepEnvelopeForSecondShortcut(pkg) || pkg == "com.apple.android.music" || pkg == "com.zeno.pulse") null else appIconFor(pkg)
            }
    }
    val secondDockFallbackResId = remember(prefs.dockSecondPackage, prefs.secondShortcutTarget) {
        val pkg = prefs.dockSecondPackage.trim()
        when {
            pkg == "com.apple.android.music" -> R.drawable.ic_dock_apple_music
            pkg == "com.zeno.pulse" -> R.drawable.ic_dock_pulse
            pkg == "com.whatsapp" || (pkg.isEmpty() && prefs.secondShortcutTarget == SecondShortcutTarget.WHATSAPP) -> R.drawable.ic_dock_whatsapp
            else -> null
        }
    }
    val thirdDockFallbackResId = remember(prefs.dockCameraPackage) {
        when (prefs.dockCameraPackage.trim()) {
            "com.spotify.music" -> R.drawable.ic_dock_spotify
            "us.zoom.videomeetings" -> R.drawable.ic_dock_zoom
            "com.apple.android.music" -> R.drawable.ic_dock_apple_music
            "com.zeno.pulse" -> R.drawable.ic_dock_pulse
            else -> null
        }
    }
    val dockEndIconModel = remember(prefs.dockCameraPackage, prefs.customIconPackages, allApps) {
        com.zeno.classiclauncher.nlauncher.apps.CustomIconStore.load(context, com.zeno.classiclauncher.nlauncher.apps.CustomIconStore.DOCK_CAMERA_KEY)
            ?: run {
                val pkg = prefs.dockCameraPackage.trim()
                if (pkg.isEmpty() || thirdDockFallbackResId != null) null else appIconFor(pkg)
            }
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
    var showLanguageSettings by remember { mutableStateOf(false) }
    var showAppDrawerBadges by remember { mutableStateOf(false) }
    var showIconAppearanceSettings by remember { mutableStateOf(false) }
    var showMinimalModeSettings by remember { mutableStateOf(false) }
    var showRootSettings by remember { mutableStateOf(false) }
    var showModesFromQs by remember { mutableStateOf(false) }
    var showDevDiagnostics by remember { mutableStateOf(false) }
    var showHomeActions by remember { mutableStateOf(false) }
    var showPinAppToHome by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showWidgetResizeSheet by remember { mutableStateOf(false) }
    var showWidgetConfigMode by remember { mutableStateOf(false) }
    var showRemoveWidgetConfirm by remember { mutableStateOf(false) }
    var showNewHomeGroupDialog by remember { mutableStateOf(false) }
    var newHomeGroupName by remember { mutableStateOf("") }
    val newHomeGroupFocusRequester = remember { FocusRequester() }
    var showQuickSettingsOverlay by remember { mutableStateOf(false) }
    var showSpotlightOverlay by remember { mutableStateOf(false) }
    var spotlightInitialQuery by remember { mutableStateOf("") }
    val appWidgetHost = remember(context) { AppWidgetHost(context, HOME_WIDGET_HOST_ID) }
    var homeWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingWidgetId by remember { mutableStateOf<Int?>(null) }
    var pendingWidgetProvider by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    fun commitHomeWidget(widgetId: Int) {
        val info = appWidgetManager.getAppWidgetInfo(widgetId)
        val span = info?.let(::estimateHomeWidgetSpan) ?: HomeWidgetSpan(4, 2)
        val previousWidgetId = homeWidgetId
        val nextConfig =
            HomeWidgetConfig(
                appWidgetId = widgetId,
                providerPackage = info?.provider?.packageName.orEmpty(),
                providerClass = info?.provider?.className.orEmpty(),
                row = (HOME_GRID_ROWS - span.rows) / 2,
                col = (HOME_GRID_COLS - span.cols) / 2,
                cols = span.cols,
                rows = span.rows,
            )
        vm.setHomeWidget(
            nextConfig,
            onComplete = { error ->
                if (error == null) {
                    previousWidgetId?.let { oldId ->
                        if (oldId != widgetId) runCatching { appWidgetHost.deleteAppWidgetId(oldId) }
                    }
                    homeWidgetId = widgetId
                    pendingWidgetId = null
                    pendingWidgetProvider = null
                    showWidgetPicker = false
                } else {
                    runCatching { appWidgetHost.deleteAppWidgetId(widgetId) }
                    Toast.makeText(context, context.getString(R.string.widget_save_failed), Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
    fun removeHomeWidget() {
        val widgetId = homeWidgetId
        vm.clearHomeWidget { error ->
            if (error == null) {
                widgetId?.let { runCatching { appWidgetHost.deleteAppWidgetId(it) } }
                homeWidgetId = null
                showWidgetConfigMode = false
                showRemoveWidgetConfirm = false
            } else {
                Toast.makeText(context, context.getString(R.string.widget_remove_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
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
        commitHomeWidget(widgetId)
    }
    val configureWidgetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val widgetId = pendingWidgetId
        if (widgetId != null && appWidgetManager.getAppWidgetInfo(widgetId) != null) {
            if (widgetId == homeWidgetId) {
                pendingWidgetId = null
                pendingWidgetProvider = null
            } else {
                commitHomeWidget(widgetId)
            }
        } else {
            pendingWidgetId = null
            pendingWidgetProvider = null
        }
    }
    val bindWidgetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val widgetId = pendingWidgetId
        val provider = pendingWidgetProvider
        if (result.resultCode != Activity.RESULT_OK || widgetId == null || provider == null) {
            widgetId?.let { runCatching { appWidgetHost.deleteAppWidgetId(it) } }
            pendingWidgetId = null
            pendingWidgetProvider = null
            return@rememberLauncherForActivityResult
        }
        val configure = provider.configure
        if (configure != null) {
            configureWidgetLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                },
            )
        } else {
            commitHomeWidget(widgetId)
        }
    }
    fun addWidgetFromProvider(provider: AppWidgetProviderInfo) {
        val id = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        pendingWidgetProvider = provider
        val bound = runCatching {
            appWidgetManager.bindAppWidgetIdIfAllowed(id, provider.provider)
        }.getOrDefault(false)
        if (bound) {
            val configure = provider.configure
            if (configure != null) {
                configureWidgetLauncher.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    },
                )
            } else {
                commitHomeWidget(id)
            }
        } else {
            bindWidgetLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                },
            )
        }
    }
    LaunchedEffect(prefs.homeWidget.appWidgetId) {
        homeWidgetId = prefs.homeWidget.appWidgetId.takeIf { it > 0 }
    }
    // ── Trackpad-aware focus visibility ──────────────────────────────────────
    // The focus highlight is only shown while the trackpad/D-pad is in use.
    // Any D-pad key event resets the idle timer; after TRACKPAD_IDLE_HIDE_MS of inactivity
    // the highlight fades out. Touch input has no effect on this timer.
    var lastTrackpadEventMs by remember { mutableStateOf(0) }
    var trackpadActive by remember { mutableStateOf(false) }
    LaunchedEffect(lastTrackpadEventMs) {
        if (lastTrackpadEventMs > 0) {
            trackpadActive = true
            kotlinx.coroutines.delay(TRACKPAD_IDLE_HIDE_MS)
            trackpadActive = false
        }
    }

    var showAppMenu by remember { mutableStateOf<AppEntry?>(null) }
    // rememberSaveable survives activity recreation (e.g. gallery picker kills launcher on low RAM)
    var pendingIconChangePkg: String? by rememberSaveable { mutableStateOf(null) }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        val pkg = pendingIconChangePkg
        if (uri != null && pkg != null) vm.setCustomIcon(pkg, uri)
        pendingIconChangePkg = null
    }
    /** Storage token (`pkg` or `pkg#id`) when the app menu was opened from the home shortcut strip. */
    var homeShortcutMenuToken by remember { mutableStateOf<String?>(null) }
    var openFolder by remember { mutableStateOf<OpenFolderState?>(null) }
    /** App-drawer folder: long-press menu (Open / Rename / Reorder / Delete). */
    var drawerFolderMenu by remember { mutableStateOf<DrawerGridCell.Folder?>(null) }
    var openHomeGroup by remember { mutableStateOf<OpenFolderState?>(null) }
    var addToContainerTarget by remember { mutableStateOf<AddToContainerTarget?>(null) }
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
    var homeStripBounds by remember { mutableStateOf<Rect?>(null) }
    var homeStripRemoveBounds by remember { mutableStateOf<Rect?>(null) }
    var homeStripRemoveVisible by remember { mutableStateOf(false) }
    var homeStripRemoveActive by remember { mutableStateOf(false) }

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

    // Consume back for overlays / drawer / search; on home (page 0) with nothing open, do nothing — system volume
    // and recents are handled elsewhere; no moveTaskToBack (avoids feeling like “recent apps”).
    BackHandler(enabled = true) {
        when {
            showModesFromQs -> showModesFromQs = false
            showQuickSettingsOverlay -> showQuickSettingsOverlay = false
            showAppMenu != null -> {
                showAppMenu = null
                appMenuFromHomeShortcut = false
                homeShortcutMenuToken = null
            }
            drawerFolderMenu != null -> drawerFolderMenu = null
            dockQuickActionSlot != null -> dockQuickActionSlot = null
            showDockSlotPicker != null -> showDockSlotPicker = null
            showRootSettings -> showRootSettings = false
            showPermissionsSettings -> showPermissionsSettings = false
            showGestureSettings -> showGestureSettings = false
            showAppDrawerBadges -> showAppDrawerBadges = false
            showIconAppearanceSettings -> showIconAppearanceSettings = false
            showMinimalModeSettings -> showMinimalModeSettings = false
            showHomeGroupsSettings -> showHomeGroupsSettings = false
            showGlanceSettings -> showGlanceSettings = false
            showSettings -> showSettings = false
            addToContainerTarget != null -> addToContainerTarget = null
            showHomeGroupMenu != null -> showHomeGroupMenu = null
            openHomeGroup != null -> openHomeGroup = null
            openFolder != null -> openFolder = null
            showHomeActions -> showHomeActions = false
            showPinAppToHome -> showPinAppToHome = false
            showWidgetConfigMode -> showWidgetConfigMode = false
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
    val navigateHomeEvent by vm.navigateHomeEvent.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(navigateHomeEvent) {
        if (navigateHomeEvent > 0) {
            // Dismiss all overlays so home/end-call button returns to the home screen
            showSettings = false
            showRootSettings = false
            showPermissionsSettings = false
            showGlanceSettings = false
            showHomeGroupsSettings = false
            showGestureSettings = false
            showLanguageSettings = false
            showAppDrawerBadges = false
            showIconAppearanceSettings = false
            showMinimalModeSettings = false
            showModesFromQs = false
            showDevDiagnostics = false
            showHomeActions = false
            showPinAppToHome = false
            showWidgetPicker = false
            showWidgetResizeSheet = false
            showWidgetConfigMode = false
            showRemoveWidgetConfirm = false
            showNewHomeGroupDialog = false
            showQuickSettingsOverlay = false
            showSpotlightOverlay = false
            showAppMenu = null
            showHomeGroupMenu = null
            showDockSlotPicker = null
            pagerState.animateScrollToPage(0)
        }
    }

    val dismissLauncherQsEvent by vm.dismissLauncherQsEvent.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(dismissLauncherQsEvent) {
        if (dismissLauncherQsEvent > 0) showQuickSettingsOverlay = false
    }

    androidx.compose.runtime.LaunchedEffect(reorderMode, moving) {
        if (reorderMode && moving == null) {
            showWidgetConfigMode = false
            kotlinx.coroutines.delay(10_000L)
            if (reorderMode && moving == null) vm.toggleReorderMode()
        }
    }
    androidx.compose.runtime.LaunchedEffect(reorderMode) {
        if (reorderMode) showWidgetConfigMode = false
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
    CompositionLocalProvider(LocalTrackpadActive provides trackpadActive) {
    Box(modifier = Modifier
        .fillMaxSize()
        .then(
            if (prefs.minimalModeGreyscale)
                Modifier.drawWithContent {
                    drawContent()
                    drawRect(color = Color.Black, blendMode = androidx.compose.ui.graphics.BlendMode.Saturation)
                }
            else Modifier
        )
        .onPreviewKeyEvent { ev ->
            if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val nk = ev.nativeKeyEvent
            val isDpad = ev.key == Key.DirectionUp || ev.key == Key.DirectionDown ||
                ev.key == Key.DirectionLeft || ev.key == Key.DirectionRight ||
                ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP ||
                nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN ||
                nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
                nk?.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT
            if (isDpad) {
                // Set trackpadActive synchronously so the focus highlight appears on the SAME frame
                // as the first D-pad keypress (LaunchedEffect fires asynchronously, so first-press
                // would otherwise show no highlight until the next composition).
                trackpadActive = true
                lastTrackpadEventMs = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            }
            false // never consume — just observe
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom status bar: shown in place of the system status bar (which is hidden in MainActivity).
            // statusBarsPadding() in child pages returns 0 when system bar is hidden, so content
            // naturally starts below this bar with no double-padding.
            if (prefs.customStatusBarEnabled && prefs.rootGranted) {
                com.zeno.classiclauncher.nlauncher.root.NormalModeTopBar()
            }
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
                        reorderMode = reorderMode,
                        onEnterReorderMode = { if (!reorderMode) vm.toggleReorderMode() },
                        onExitReorderMode = { if (reorderMode) vm.toggleReorderMode() },
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
                                    FolderIds.isFolderId(tok) && prefs.showShortcutApps -> {
                                        gridCells.filterIsInstance<DrawerGridCell.Folder>()
                                            .find { it.id == tok }
                                            ?.let { cell ->
                                                openFolder = OpenFolderState(cell.id, cell.members, cell.displayTitle)
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
                        swipeRightPackage = prefs.swipeRightPackage,
                        onOpenGestureSettings = { showGestureSettings = true },
                        doubleTapPackage = prefs.doubleTapPackage,
                        hapticsEnabled = prefs.hapticsEnabled,
                        hapticIntensity = prefs.hapticIntensity,
                        homeWidgetId = homeWidgetId,
                        homeWidgetConfig = prefs.homeWidget,
                        appWidgetHost = appWidgetHost,
                        widgetConfigMode = showWidgetConfigMode,
                        onOpenWidgetConfigMode = {
                            if (reorderMode) vm.toggleReorderMode()
                            showWidgetConfigMode = true
                        },
                        onDismissWidgetConfigMode = { showWidgetConfigMode = false },
                        onUpdateHomeWidget = vm::setHomeWidget,
                        onResizeWidget = {
                            showWidgetConfigMode = false
                            showWidgetResizeSheet = true
                        },
                        onReplaceWidget = {
                            showWidgetConfigMode = false
                            showWidgetPicker = true
                        },
                        onRemoveWidget = {
                            showRemoveWidgetConfirm = true
                        },
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
                        homeStripBounds = homeStripBounds,
                        removeDropVisible = homeStripRemoveVisible,
                        onRemoveDropVisibleChanged = { homeStripRemoveVisible = it },
                        onRemoveDropActiveChanged = { homeStripRemoveActive = it },
                        removeDropActive = homeStripRemoveActive,
                        onRemoveDropBoundsChanged = { homeStripRemoveBounds = it },
                        removeDropBounds = homeStripRemoveBounds,
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
                        drawerSortMode = drawerSortMode,
                        showIconNotifBadge = prefs.notificationBadgesEnabled,
                        onSortModeSelected = { mode ->
                            if (mode != DrawerSortMode.MOST_USED || vm.hasUsagePermission()) {
                                vm.setDrawerSortMode(mode)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.drawer_usage_access_prompt),
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
                        onStripReorderTap = { token ->
                            val m = moving
                            if (m == null) vm.startStripMove(token)
                            else vm.finishReorderDrop(token)
                        },
                        onClearStripKeyboardFocus = {
                            homeStripFocused = false
                            homeStripFocusIndex = -1
                        },
                        onStripBoundsChanged = { homeStripBounds = it },
                        onRemoveDropVisibleChanged = { homeStripRemoveVisible = it },
                        onRemoveDropActiveChanged = { homeStripRemoveActive = it },
                        onRemoveDropBoundsChanged = { homeStripRemoveBounds = it },
                        removeDropBounds = homeStripRemoveBounds,
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
                hasCustomIcon = prefs.customIconPackages.contains(selectedApp.packageName),
                homeGroups = prefs.homeGroups,
                addHomeShortcutEnabled = canAddHomeShortcut,
                removeHomeShortcutEnabled = appMenuFromHomeShortcut,
                drawerFolderActionsEnabled = drawerFolderActionsEnabled,
                drawerFolders = drawerFolderChoices,
                onChangeIcon = {
                    pendingIconChangePkg = selectedApp.packageName
                    showAppMenu = null
                    iconPickerLauncher.launch("image/*")
                },
                onResetIcon = {
                    vm.clearCustomIcon(selectedApp.packageName)
                    showAppMenu = null
                },
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
                    val isCurrentlyHidden = prefs.hiddenPackages.contains(selectedApp.packageName)
                    vm.setHidden(selectedApp.packageName, !isCurrentlyHidden)
                    if (!isCurrentlyHidden) {
                        Toast.makeText(context, context.getString(R.string.app_hidden_toast), Toast.LENGTH_SHORT).show()
                    }
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
                        Toast.makeText(context, context.getString(R.string.home_groups_already_exist), Toast.LENGTH_SHORT).show()
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
                showPinToHomeStrip = !classicMode && prefs.homeStripEnabled,
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
                onPinToHomeStrip = {
                    scope.launch {
                        val ok = vm.pinDrawerFolderToHomeStrip(folderMenuCell.id)
                        Toast.makeText(
                            context,
                            if (ok) context.getString(R.string.home_strip_pinned) else context.getString(R.string.home_strip_full_short),
                            Toast.LENGTH_SHORT,
                        ).show()
                        drawerFolderMenu = null
                    }
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
                showAddButton = !reorderMode,
                onAddApp = {
                    addToContainerTarget = AddToContainerTarget(
                        id = folderOpen.id,
                        title = folderOpen.title,
                        existingPackageNames = folderOpen.members.map { it.packageName }.toSet(),
                        isHomeGroup = false,
                    )
                },
                hapticsEnabled = prefs.hapticsEnabled,
                hapticIntensity = prefs.hapticIntensity,
                appIconShape = prefs.appIconShape,
                themePalette = themePalette,
                unreadPackages = if (prefs.notificationBadgesEnabled) unreadPackages else emptySet(),
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
                showAddButton = !reorderMode,
                onAddApp = {
                    addToContainerTarget = AddToContainerTarget(
                        id = homeGroupOpen.id,
                        title = homeGroupOpen.title,
                        existingPackageNames = homeGroupOpen.members.map { it.packageName }.toSet(),
                        isHomeGroup = true,
                    )
                },
                hapticsEnabled = prefs.hapticsEnabled,
                hapticIntensity = prefs.hapticIntensity,
                appIconShape = prefs.appIconShape,
                themePalette = themePalette,
                unreadPackages = if (prefs.notificationBadgesEnabled) unreadPackages else emptySet(),
                renameDialogTitle = "Rename group",
                emptyStateMessage = "No apps yet — long-press an app in the drawer to add it here.",
            )
        }

        val addTarget = addToContainerTarget
        if (addTarget != null) {
            AddAppToContainerSheet(
                title = addTarget.title,
                allApps = allApps,
                hiddenPackages = prefs.hiddenPackages,
                existingPackageNames = addTarget.existingPackageNames,
                appIconShape = prefs.appIconShape,
                themePalette = themePalette,
                onSelect = { app ->
                    if (addTarget.isHomeGroup) {
                        vm.addPackageToHomeGroup(app.packageName, addTarget.id)
                        openHomeGroup = openHomeGroup?.takeIf { it.id == addTarget.id }?.let { cur ->
                            if (cur.members.any { it.packageName == app.packageName }) cur else cur.copy(members = cur.members + app)
                        }
                    } else {
                        vm.addAppToFolder(app.packageName, addTarget.id)
                        openFolder = openFolder?.takeIf { it.id == addTarget.id }?.let { cur ->
                            if (cur.members.any { it.packageName == app.packageName }) cur else cur.copy(members = cur.members + app)
                        }
                    }
                    addToContainerTarget = null
                },
                onDismiss = { addToContainerTarget = null },
            )
        }

        val homeGroupMenuTarget = showHomeGroupMenu
        if (homeGroupMenuTarget != null) {
            val groupAlreadyPinned =
                homeGroupMenuTarget.id in prefs.effectiveHomeStripSlotOrder().filterNotNull().toSet()
            HomeGroupContextMenu(
                group = homeGroupMenuTarget,
                otherGroupExists = prefs.homeGroups.size > 1,
                showPinToHomeStrip = !classicMode && prefs.homeStripEnabled && !groupAlreadyPinned,
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
                onPinToHomeStrip = {
                    scope.launch {
                        val ok = vm.pinHomeGroupToHomeStrip(homeGroupMenuTarget.id)
                        Toast.makeText(
                            context,
                            if (ok) context.getString(R.string.home_strip_pinned) else context.getString(R.string.home_strip_full_short),
                            Toast.LENGTH_SHORT,
                        ).show()
                        showHomeGroupMenu = null
                    }
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
                showHomeGroupsSettings || showDockSlotPicker != null ||
                showGestureSettings || showLanguageSettings || showAppDrawerBadges || showIconAppearanceSettings || showMinimalModeSettings || showRootSettings
        val settingsOn = stringResource(R.string.settings_on)
        val settingsOff = stringResource(R.string.settings_off)
        val settingsConfigured = stringResource(R.string.settings_configured)
        val settingsNotConfigured = stringResource(R.string.settings_not_configured)
        val settingsDefaultApp = stringResource(R.string.settings_default_app)
        val settingsHidden = stringResource(R.string.settings_hidden)
        val settingsHiddenClassic = stringResource(R.string.settings_hidden_classic)
        val settingsPermissionsReady = stringResource(R.string.settings_permissions_ready)
        val settingsPermissionsMissingOne = stringResource(R.string.settings_permissions_missing_one)
        val settingsPermissionsMissingMany = stringResource(R.string.settings_permissions_missing_many)
        val settingsAllOff = stringResource(R.string.settings_all_off)
        val settingsNotifications = stringResource(R.string.settings_notifications)
        val dockMailDefaultTitle = stringResource(R.string.dock_mail)
        val dockMessagesDefaultTitle = stringResource(R.string.dock_messages)
        val dockCameraDefaultTitle = stringResource(R.string.dock_camera)

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(160)),
        ) {
            SettingsScreenOverlay(
                stackedChildOverlayOpen = settingsStackedOverlayOpen,
                gridPreset = prefs.gridPreset,
                hapticsEnabled = prefs.hapticsEnabled,
                dockMailBody = remember(prefs.dockMailPackage, allApps, settingsDefaultApp) {
                    if (prefs.dockMailPackage.isEmpty()) {
                        settingsDefaultApp
                    } else {
                        allApps.find { it.packageName == prefs.dockMailPackage }?.label ?: prefs.dockMailPackage
                    }
                },
                dockSecondBody = remember(prefs.dockSecondPackage, allApps, settingsDefaultApp) {
                    if (prefs.dockSecondPackage.isEmpty()) {
                        settingsDefaultApp
                    } else {
                        allApps.find { it.packageName == prefs.dockSecondPackage }?.label ?: prefs.dockSecondPackage
                    }
                },
                dockThirdBody = remember(prefs.dockCameraPackage, allApps, settingsDefaultApp) {
                    if (prefs.dockCameraPackage.isEmpty()) {
                        settingsDefaultApp
                    } else {
                        val label = allApps.find { it.packageName == prefs.dockCameraPackage }?.label
                        label ?: prefs.dockCameraPackage
                    }
                },
                onGridPreset = vm::setGridPreset,
                dockMailTitle = if (prefs.dockMailTitle == "Mail") dockMailDefaultTitle else prefs.dockMailTitle,
                dockSecondTitle = if (prefs.dockSecondTitle == "Messages") dockMessagesDefaultTitle else prefs.dockSecondTitle,
                dockThirdTitle = if (prefs.dockThirdTitle == "Camera") dockCameraDefaultTitle else prefs.dockThirdTitle,
                onOpenDockSlotPicker = { showDockSlotPicker = it },
                glanceSubtitle = remember(
                    prefs.glanceEnabled,
                    settingsOn,
                    settingsOff,
                ) {
                    if (prefs.glanceEnabled) settingsOn else settingsOff
                },
                onOpenGlanceSettings = { showGlanceSettings = true },
                homeGroupsSubtitle = remember(prefs.homeStripEnabled, prefs.homeGroups, settingsOn, settingsOff) {
                    if (!prefs.homeStripEnabled) {
                        settingsOff
                    } else {
                        when {
                            prefs.homeGroups.isEmpty() -> settingsOn
                            else -> prefs.homeGroups.joinToString(prefix = "$settingsOn · ", separator = " · ") { it.title }
                        }
                    }
                },
                permissionsSubtitle = remember(prefs, permRuntime, settingsPermissionsReady, settingsPermissionsMissingOne, settingsPermissionsMissingMany) {
                    val missing = missingPermissionCount(prefs, permRuntime)
                    when {
                        missing == 0 -> settingsPermissionsReady
                        missing == 1 -> settingsPermissionsMissingOne
                        else -> settingsPermissionsMissingMany.format(missing)
                    }
                },
                onOpenPermissionsSettings = { showPermissionsSettings = true },
                languageSubtitle = remember(context, prefs.languageCode) {
                    LauncherLocale.languageTitle(LauncherLocale.currentLanguageCode(context))
                },
                onOpenLanguageSettings = { showLanguageSettings = true },
                rootGranted = prefs.rootGranted,
                onOpenRootSettings = { showRootSettings = true },
                onSetWallpaper = {
                    runCatching {
                        context.startActivity(Intent("android.settings.WALLPAPER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
                onToggleHaptics = { vm.setHapticsEnabled(!prefs.hapticsEnabled) },
                hapticIntensity = prefs.hapticIntensity,
                onSetHapticIntensity = vm::setHapticIntensity,
                manualGpsCoords = prefs.glanceWeatherManualLatitude.isNotEmpty(),
                onExportBackup = vm::exportBackupJson,
                onImportBackup = vm::importBackupJson,
                onResetTheme = vm::resetTheme,
                themePalette = themePalette,
                onDismiss = { showSettings = false },
                onShowDiagnostics = { showDevDiagnostics = true },
                gestureSubtitle = remember(
                    prefs.swipeUpPackage,
                    prefs.swipeRightPackage,
                    prefs.doubleTapPackage,
                    prefs.doubleTapToSleepEnabled,
                    prefs.customQuickSettingsEnabled,
                    settingsConfigured,
                    settingsNotConfigured,
                ) {
                    if (prefs.swipeUpPackage.isNotEmpty() ||
                        prefs.swipeRightPackage.isNotEmpty() ||
                        prefs.doubleTapToSleepEnabled ||
                        prefs.doubleTapPackage.isNotEmpty() ||
                        prefs.customQuickSettingsEnabled
                    ) {
                        settingsConfigured
                    } else {
                        settingsNotConfigured
                    }
                },
                onOpenGestureSettings = { showGestureSettings = true },
                onOpenScreenSaverSettings = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.onFailure {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                },
                drawerBadgesSubtitle = remember(prefs.showIconNotifBadge, settingsNotifications, settingsAllOff) {
                    buildList {
                        if (prefs.showIconNotifBadge) add(settingsNotifications)
                    }.let { if (it.isEmpty()) settingsAllOff else it.joinToString(", ") }
                },
                appIconShape = prefs.appIconShape,
                onSetAppIconShape = vm::setAppIconShape,
                onOpenAppearanceSettings = { showIconAppearanceSettings = true },
                minimalModeEnabled = prefs.minimalModeEnabled,
                onOpenMinimalModeSettings = { showMinimalModeSettings = true },
                homeStripEnabled = prefs.homeStripEnabled,
                onToggleHomeStrip = { vm.setHomeStripEnabled(!prefs.homeStripEnabled) },
                dockSecondEnabled = prefs.dockSecondEnabled,
                onToggleDockSecond = { vm.setDockSecondEnabled(!prefs.dockSecondEnabled) },
                childContent = {
                    // All settings sub-overlays rendered INSIDE SettingsScreenOverlay's Box
                    // so their BackHandlers are always deepest in the composition tree.
                    if (showIconAppearanceSettings) {
                        IconAppearanceSettingsOverlay(
                            gridPreset = prefs.gridPreset,
                            appIconShape = prefs.appIconShape,
                            iconPackPackage = prefs.iconPackPackage,
                            showAppCardBackground = prefs.showAppCardBackground,
                            showIconNotifBadge = prefs.showIconNotifBadge,
                            notificationAccessReady = prefs.notificationBadgesEnabled && permRuntime.notificationAccess,
                            previewApps = allApps,
                            drawerBadgesSubtitle = remember(prefs.showIconNotifBadge, settingsNotifications, settingsAllOff) {
                                buildList {
                                    if (prefs.showIconNotifBadge) add(settingsNotifications)
                                }.let { if (it.isEmpty()) settingsAllOff else it.joinToString(", ") }
                            },
                            onGridPreset = vm::setGridPreset,
                            onAppGridIconSize = vm::setAppGridIconSize,
                            onAppGridFontSize = vm::setAppGridFontSize,
                            onAppGridFontWeight = vm::setAppGridFontWeight,
                            onSetAppIconShape = vm::setAppIconShape,
                            onSetIconPackPackage = vm::setIconPackPackage,
                            onToggleAppCardBackground = { vm.setShowAppCardBackground(!prefs.showAppCardBackground) },
                            onShowIconNotifBadgeChange = vm::setShowIconNotifBadge,
                            themePalette = themePalette,
                            onDismiss = { showIconAppearanceSettings = false },
                        )
                    }
                    if (showAppDrawerBadges) {
                        AppDrawerBadgesOverlay(
                            showUsageStatsBadge = prefs.showUsageStatsBadge,
                            showIconNotifBadge = prefs.showIconNotifBadge,
                            notificationAccessReady = prefs.notificationBadgesEnabled && permRuntime.notificationAccess,
                            themePalette = themePalette,
                            onDismiss = { showAppDrawerBadges = false },
                            onShowUsageStatsBadgeChange = vm::setShowUsageStatsBadge,
                            onShowIconNotifBadgeChange = vm::setShowIconNotifBadge,
                        )
                    }
                    if (showMinimalModeSettings) {
                        com.zeno.classiclauncher.nlauncher.minimalmode.MinimalModeSettingsOverlay(
                            vm = vm,
                            onDismiss = { showMinimalModeSettings = false },
                        )
                    }
                    if (showRootSettings) {
                        com.zeno.classiclauncher.nlauncher.root.RootAccessScreen(
                            rootGranted = prefs.rootGranted,
                            customStatusBarEnabled = prefs.customStatusBarEnabled,
                            onDismiss = { showRootSettings = false },
                            onRootGranted = {
                                vm.setRootGranted(true)
                                vm.setCustomStatusBarEnabled(true)
                            },
                            onRootRevoked = {
                                vm.setRootGranted(false)
                                vm.setCustomStatusBarEnabled(false)
                            },
                            onCustomStatusBarToggled = { enabled ->
                                vm.setCustomStatusBarEnabled(enabled)
                            },
                        )
                    }
                    if (showGestureSettings) {
                        GestureShortcutsOverlay(
                            allApps = allApps,
                            swipeUpPackage = prefs.swipeUpPackage,
                            swipeRightPackage = prefs.swipeRightPackage,
                            doubleTapPackage = prefs.doubleTapPackage,
                            doubleTapToSleepEnabled = prefs.doubleTapToSleepEnabled,
                            customQuickSettingsEnabled = prefs.customQuickSettingsEnabled,
                            themePalette = themePalette,
                            onDismiss = { showGestureSettings = false },
                            onSetSwipeUp = vm::setSwipeUpPackage,
                            onSetSwipeRight = vm::setSwipeRightPackage,
                            onSetDoubleTap = vm::setDoubleTapPackage,
                            onDoubleTapSleepChange = vm::setDoubleTapToSleepEnabled,
                            onCustomQuickSettingsChange = vm::setCustomQuickSettingsEnabled,
                        )
                    }
                    if (showLanguageSettings) {
                        LanguageSettingsOverlay(
                            currentLanguageCode = prefs.languageCode,
                            themePalette = themePalette,
                            onLanguageSelected = { code ->
                                vm.setLanguageCode(code)
                                showLanguageSettings = false
                                context.restartHostActivityForLocaleChange()
                            },
                            onDismiss = { showLanguageSettings = false },
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
                            homeStripEnabled = prefs.homeStripEnabled,
                            themePalette = themePalette,
                            onToggleHomeStrip = vm::setHomeStripEnabled,
                            onCreateGroup = { name ->
                                if (groupNameExists(name)) {
                                    Toast.makeText(context, context.getString(R.string.home_groups_already_exist), Toast.LENGTH_SHORT).show()
                                } else {
                                    vm.createHomeGroup(name)
                                }
                            },
                            onDeleteGroup = { id -> vm.deleteHomeGroup(id) },
                            onDismiss = { showHomeGroupsSettings = false },
                        )
                    }
                },
            )
        }

        // DockShortcutPickerOverlay at parent level so it works both from settings
        // AND from the home-screen dock long-press (when showSettings = false).
        val activeDockSlot = showDockSlotPicker
        if (activeDockSlot != null) {
            DockShortcutPickerOverlay(
                apps = allApps,
                themePalette = themePalette,
                slot = activeDockSlot,
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

        if (!prefs.setupComplete && !showPermissionsSettings) {
            ZenoSetupOverlay(
                themePalette = themePalette,
                onApply = { mode ->
                    when (mode) {
                        0 -> { // Classic BlackBerry
                            vm.setClassicMode(true)
                            vm.setGlanceEnabled(false)
                            vm.setHomeStripEnabled(false)
                            vm.setCustomQuickSettingsEnabled(false)
                        }
                        1 -> { // Smart Home
                            vm.setClassicMode(false)
                            vm.setGlanceEnabled(true)
                            vm.setHomeStripEnabled(true)
                            vm.setCustomQuickSettingsEnabled(true)
                        }
                        else -> { // Minimal
                            vm.setClassicMode(false)
                            vm.setGlanceEnabled(false)
                            vm.setHomeStripEnabled(true)
                            vm.setCustomQuickSettingsEnabled(false)
                        }
                    }
                    vm.setSetupComplete(true)
                },
                onOpenPermissions = { showPermissionsSettings = true },
            )
        }

        // Developer Diagnostics — triggered by 7-tap on Settings gear icon (hidden panel).
        if (showDevDiagnostics) {
            DevDiagnosticsOverlay(onDismiss = { showDevDiagnostics = false })
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
                onAutoUnlockEnabled = vm::setAutoUnlockEnabled,
                onAutoUnlockPinDigits = vm::setAutoUnlockPinDigits,
                onGlanceEnabled = vm::setGlanceEnabled,
                onGlanceShowCalendar = vm::setGlanceShowCalendar,
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
                            Text(stringResource(R.string.action_change_shortcut), color = Color.White, fontSize = 15.sp)
                        }
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        // Change icon
                        val dockSlotKey = when (activeQuickSlot) {
                            DockSlot.Mail     -> com.zeno.classiclauncher.nlauncher.apps.CustomIconStore.DOCK_MAIL_KEY
                            DockSlot.Shortcut -> com.zeno.classiclauncher.nlauncher.apps.CustomIconStore.DOCK_SHORTCUT_KEY
                            DockSlot.Camera   -> com.zeno.classiclauncher.nlauncher.apps.CustomIconStore.DOCK_CAMERA_KEY
                        }
                        val hasDockCustomIcon = dockSlotKey in prefs.customIconPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingIconChangePkg = dockSlotKey
                                    dockQuickActionSlot = null
                                    iconPickerLauncher.launch("image/*")
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(stringResource(R.string.action_change_icon), color = Color.White, fontSize = 15.sp)
                        }
                        if (hasDockCustomIcon) {
                            HorizontalDivider(color = Color(0x22FFFFFF))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.clearCustomIcon(dockSlotKey)
                                        dockQuickActionSlot = null
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = null, tint = Color(0xFF8E95A3), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(stringResource(R.string.action_reset_icon), color = Color(0xFF8E95A3), fontSize = 15.sp)
                            }
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
                                Text(stringResource(R.string.action_hide_from_dock), color = Color(0xFFFF6B6B), fontSize = 15.sp)
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
                                Text(stringResource(R.string.action_reset_to_default), color = Color(0xFFFF6B6B), fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        if (showQuickSettingsOverlay) {
            QuickSettingsOverlay(
                allApps = allApps,
                hiddenPackages = prefs.hiddenPackages,
                qrScannerPackage = prefs.quickSettingsQrScannerPackage,
                savedTileOrder = prefs.quickSettingsTileOrder,
                themePalette = themePalette,
                hapticsEnabled = prefs.hapticsEnabled,
                hapticIntensity = prefs.hapticIntensity,
                greyscaleEnabled = prefs.minimalModeGreyscale,
                onOpenModes = { showQuickSettingsOverlay = false; showModesFromQs = true },
                onDismiss = { showQuickSettingsOverlay = false },
                onSetQrScannerPackage = vm::setQuickSettingsQrScannerPackage,
                onSetTileOrder = vm::setQuickSettingsTileOrder,
                onToggleGreyscale = { vm.setMinimalModeGreyscale(!prefs.minimalModeGreyscale) },
            )
        }
        if (showModesFromQs) {
            com.zeno.classiclauncher.nlauncher.minimalmode.MinimalModeSettingsOverlay(
                vm = vm,
                onDismiss = { showModesFromQs = false },
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
                    showHomeActions = false
                    showWidgetPicker = true
                },
                onRemoveWidget = {
                    showHomeActions = false
                    showRemoveWidgetConfirm = true
                },
                onEditHomeLayout = {
                    showHomeActions = false
                    showWidgetConfigMode = false
                    if (!reorderMode) vm.toggleReorderMode()
                },
                onOpenSettings = {
                    showHomeActions = false
                    showSettings = true
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
                onPinToHomeStrip = {
                    showHomeActions = false
                    showPinAppToHome = true
                },
                onDismiss = { showHomeActions = false },
            )
        }

        homeWidgetId?.takeIf { showWidgetResizeSheet }?.let { widgetId ->
            val info = appWidgetManager.getAppWidgetInfo(widgetId)
            if (info != null) {
                HomeWidgetResizeSheet(
                    info = info,
                    currentConfig = prefs.homeWidget,
                    onSelect = { span ->
                        vm.setHomeWidget(
                            normalizedHomeWidgetConfig(
                                prefs.homeWidget.copy(cols = span.cols, rows = span.rows),
                                span,
                            ),
                        )
                        showWidgetResizeSheet = false
                    },
                    onDismiss = { showWidgetResizeSheet = false },
                )
            } else {
                showWidgetResizeSheet = false
            }
        }

        if (showWidgetPicker) {
            HomeWidgetPickerSheet(
                appWidgetManager = appWidgetManager,
                themePalette = themePalette,
                onSelectWidget = ::addWidgetFromProvider,
                onOpenSystemPicker = {
                    val id = appWidgetHost.allocateAppWidgetId()
                    pendingWidgetId = id
                    val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    }
                    pickWidgetLauncher.launch(pickIntent)
                    showWidgetPicker = false
                },
                onDismiss = { showWidgetPicker = false },
            )
        }

        if (showRemoveWidgetConfirm) {
            AlertDialog(
                onDismissRequest = { showRemoveWidgetConfirm = false },
                containerColor = Color(0xFF111820),
                titleContentColor = Color(0xFFEAF2F8),
                textContentColor = Color(0xFFB7C2CF),
                title = { Text(stringResource(R.string.dialog_remove_widget_title)) },
                text = { Text(stringResource(R.string.dialog_remove_widget_body)) },
                confirmButton = {
                    TextButton(onClick = { removeHomeWidget() }) {
                        Text(stringResource(R.string.action_remove), color = Color(0xFFFF9EAA), fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveWidgetConfirm = false }) {
                        Text(stringResource(R.string.action_cancel), color = Color(0xFF9AE2FF))
                    }
                },
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
            val onCreateGroup = {
                val name = newHomeGroupName
                if (groupNameExists(name)) {
                    Toast.makeText(context, context.getString(R.string.home_groups_already_exist), Toast.LENGTH_SHORT).show()
                } else if (!prefs.canAddHomeStripItem()) {
                    Toast.makeText(context, context.getString(R.string.home_strip_full), Toast.LENGTH_SHORT).show()
                    showNewHomeGroupDialog = false
                } else {
                    vm.createHomeGroup(name)
                    showNewHomeGroupDialog = false
                }
            }
            AlertDialog(
                onDismissRequest = { showNewHomeGroupDialog = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = themePalette.settingsBg,
                titleContentColor = themePalette.settingsMenuTitle,
                textContentColor = themePalette.settingsMenuBody,
                title = { Text(stringResource(R.string.dialog_new_group_title), color = themePalette.settingsMenuTitle) },
                text = {
                    OutlinedTextField(
                        value = newHomeGroupName,
                        onValueChange = { newHomeGroupName = capitalizeFirstLetterForGroupInput(it) },
                        singleLine = true,
                        label = { Text(stringResource(R.string.dialog_group_name_hint), color = themePalette.settingsMenuBody) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onCreateGroup() }),
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
                    TextButton(onClick = onCreateGroup) { Text(stringResource(R.string.action_create), color = themePalette.settingsMenuBody) }
                },
                dismissButton = {
                    TextButton(onClick = { showNewHomeGroupDialog = false }) {
                        Text(stringResource(R.string.action_cancel), color = themePalette.settingsMenuBody)
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
    } // end CompositionLocalProvider(LocalTrackpadActive)
}

/**
 * Long-press opens the home actions sheet. Double-tap to lock runs when [doubleTapToSleepEnabled] is on
 * ([SleepManager.lockNow]: built-in lock path first, with optional lock-helper/fallback paths); child views (glance strip)
 * consume their own taps first.
 */
@Composable
private fun HomeGridCanvas(
    homeWidgetId: Int?,
    homeWidgetConfig: HomeWidgetConfig,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    reorderMode: Boolean,
    onEnterReorderMode: () -> Unit,
    widgetConfigMode: Boolean,
    onOpenWidgetConfigMode: () -> Unit,
    hapticsEnabled: Boolean,
    removeDropBounds: Rect?,
    onRemoveDropVisibleChanged: (Boolean) -> Unit,
    onRemoveDropActiveChanged: (Boolean) -> Unit,
    onResizeWidget: () -> Unit,
    onReplaceWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    onUpdateHomeWidget: (HomeWidgetConfig) -> Unit,
    onWidgetBoundsChanged: (Rect?) -> Unit,
    onWidgetControlsBoundsChanged: (Rect?) -> Unit,
    onWidgetDragActiveChanged: (Boolean) -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val currentRemoveDropBounds = rememberUpdatedState(removeDropBounds)
    val currentOnOpenWidgetConfigMode = rememberUpdatedState(onOpenWidgetConfigMode)
    val currentOnRemoveDropVisibleChanged = rememberUpdatedState(onRemoveDropVisibleChanged)
    val currentOnRemoveDropActiveChanged = rememberUpdatedState(onRemoveDropActiveChanged)
    val currentOnRemoveWidget = rememberUpdatedState(onRemoveWidget)
    val currentOnUpdateHomeWidget = rememberUpdatedState(onUpdateHomeWidget)
    val currentOnWidgetDragActiveChanged = rememberUpdatedState(onWidgetDragActiveChanged)
    val removeFallbackTopPx = with(density) { 116.dp.toPx() }
    val widgetInfo = remember(homeWidgetId) {
        homeWidgetId?.let { appWidgetManager.getAppWidgetInfo(it) }
    }
    LaunchedEffect(widgetInfo) {
        if (widgetInfo == null) onWidgetBoundsChanged(null)
    }
    LaunchedEffect(widgetConfigMode, widgetInfo) {
        if (!widgetConfigMode || widgetInfo == null) onWidgetControlsBoundsChanged(null)
    }
    var widgetDragOffset by remember { mutableStateOf(Offset.Zero) }
    var widgetDragging by remember { mutableStateOf(false) }
    var widgetRemoveActive by remember { mutableStateOf(false) }
    var widgetFingerRoot by remember { mutableStateOf(Offset.Zero) }
    var widgetCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var gridCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    fun resetWidgetDrag() {
        widgetDragging = false
        widgetDragOffset = Offset.Zero
        widgetRemoveActive = false
        widgetFingerRoot = Offset.Zero
        currentOnWidgetDragActiveChanged.value(false)
        currentOnRemoveDropVisibleChanged.value(false)
        currentOnRemoveDropActiveChanged.value(false)
    }
    fun updateWidgetRemoveTarget(fingerRoot: Offset) {
        val inRemove = currentRemoveDropBounds.value?.contains(fingerRoot) == true || fingerRoot.y <= removeFallbackTopPx
        if (inRemove != widgetRemoveActive) {
            widgetRemoveActive = inRemove
            currentOnRemoveDropActiveChanged.value(inRemove)
            if (inRemove && hapticsEnabled) {
                view.performHapticFeedback(textHandleMoveFeedback)
            }
        }
    }
    LaunchedEffect(reorderMode) {
        if (!reorderMode && widgetDragging) resetWidgetDrag()
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { gridCoords = it },
    ) {
        val gap = 8.dp
        val cellW = ((maxWidth - gap * (HOME_GRID_COLS - 1)) / HOME_GRID_COLS)
            .coerceAtLeast(54.dp)
        val cellH = ((maxHeight - gap * (HOME_GRID_ROWS - 1)) / HOME_GRID_ROWS)
            .coerceAtLeast(54.dp)

        widgetInfo?.let { info ->
            val widgetId = homeWidgetId ?: return@let
            val span = normalizedHomeWidgetConfig(homeWidgetConfig, estimateHomeWidgetSpan(info))
            val providerMinWidth = maxOf(info.minWidth, info.minResizeWidth).takeIf { it > 0 }?.dp ?: 0.dp
            val providerMinHeight = maxOf(info.minHeight, info.minResizeHeight).takeIf { it > 0 }?.dp ?: 0.dp
            val width = (cellW * span.cols + gap * (span.cols - 1))
                .coerceAtLeast(providerMinWidth)
                .coerceAtMost(maxWidth)
            val height = (cellH * span.rows + gap * (span.rows - 1))
                .coerceAtLeast(providerMinHeight)
                .coerceAtMost(maxHeight)
            val baseX = (cellW + gap) * span.col
            val baseY = (cellH + gap) * span.row
            val animatedBaseX by animateDpAsState(
                targetValue = baseX,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "homeWidgetSnapX",
            )
            val animatedBaseY by animateDpAsState(
                targetValue = baseY,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "homeWidgetSnapY",
            )
            val widgetWidthDp = width.value.roundToInt().coerceAtLeast(1)
            val widgetHeightDp = height.value.roundToInt().coerceAtLeast(1)
            val widgetHeightPx = with(density) { height.toPx() }
            val controlsCompact = span.cols <= 1 || width < 176.dp
            val controlsMedium = !controlsCompact && width < 260.dp
            val controlsPanelWidth = when {
                controlsCompact -> 124.dp
                controlsMedium -> width.coerceIn(184.dp, 220.dp)
                else -> width.coerceIn(244.dp, 320.dp)
            }.coerceAtMost(maxWidth)
            val controlsPanelHeight = if (controlsCompact) 132.dp else 48.dp
            val controlsGap = 8.dp
            val maxControlsX = (maxWidth - controlsPanelWidth).coerceAtLeast(0.dp)
            val controlsX = (baseX + width / 2 - controlsPanelWidth / 2).coerceIn(0.dp, maxControlsX)
            val belowControlsY = baseY + height + controlsGap
            val controlsY = if (belowControlsY + controlsPanelHeight <= maxHeight) {
                belowControlsY
            } else {
                (baseY - controlsPanelHeight - controlsGap).coerceAtLeast(0.dp)
            }
            val removeHoverOffset = currentRemoveDropBounds.value
                ?.takeIf { widgetDragging && widgetRemoveActive }
                ?.let { bounds ->
                    val coords = widgetCoords?.takeIf { it.isAttached } ?: return@let widgetDragOffset
                    val topAtFinger = coords.boundsInRoot().top + widgetDragOffset.y
                    val clampedTop = bounds.bottom - widgetHeightPx * 0.42f
                    if (topAtFinger < clampedTop) {
                        Offset(widgetDragOffset.x, widgetDragOffset.y + (clampedTop - topAtFinger))
                    } else {
                        widgetDragOffset
                    }
                }
                ?: widgetDragOffset
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = if (widgetDragging) baseX else animatedBaseX,
                        y = if (widgetDragging) baseY else animatedBaseY,
                    )
                    .width(width)
                    .height(height)
                    .onGloballyPositioned {
                        widgetCoords = it
                        onWidgetBoundsChanged(it.boundsInRoot())
                    }
                    .graphicsLayer {
                        translationX = removeHoverOffset.x
                        translationY = removeHoverOffset.y
                        scaleX = when {
                            widgetDragging && widgetRemoveActive -> 1.02f
                            widgetDragging -> 1.04f
                            else -> 1f
                        }
                        scaleY = when {
                            widgetDragging && widgetRemoveActive -> 1.02f
                            widgetDragging -> 1.04f
                            else -> 1f
                        }
                        alpha = if (widgetDragging) 0.96f else 1f
                        shadowElevation = if (widgetDragging) 16f else 0f
                    },
            ) {
                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            appWidgetHost.createView(ctx, widgetId, info).apply {
                                setAppWidget(widgetId, info)
                                isLongClickable = true
                                setOnLongClickListener {
                                    currentOnOpenWidgetConfigMode.value()
                                    if (hapticsEnabled) {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                    true
                                }
                            }
                        },
                        update = { hostView ->
                            if (hostView is AppWidgetHostView) {
                                hostView.setAppWidget(widgetId, info)
                                hostView.isLongClickable = true
                                hostView.setOnLongClickListener {
                                    currentOnOpenWidgetConfigMode.value()
                                    if (hapticsEnabled) {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                    true
                                }
                                hostView.updateAppWidgetSize(
                                    null,
                                    widgetWidthDp,
                                    widgetHeightDp,
                                    widgetWidthDp,
                                    widgetHeightDp,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (reorderMode) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(widgetId, reorderMode) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { start ->
                                            widgetDragging = true
                                            widgetDragOffset = Offset.Zero
                                            widgetRemoveActive = false
                                            currentOnWidgetDragActiveChanged.value(true)
                                            currentOnRemoveDropVisibleChanged.value(true)
                                            currentOnRemoveDropActiveChanged.value(false)
                                            if (hapticsEnabled) {
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            }
                                            widgetFingerRoot = widgetCoords
                                                ?.takeIf { it.isAttached }
                                                ?.localToRoot(start)
                                                ?: Offset.Zero
                                            updateWidgetRemoveTarget(widgetFingerRoot)
                                        },
                                        onDrag = { change, dragAmount ->
                                            widgetDragOffset += dragAmount
                                            widgetFingerRoot += dragAmount
                                            change.consume()
                                            updateWidgetRemoveTarget(widgetFingerRoot)
                                        },
                                        onDragEnd = {
                                            val shouldRemove = widgetDragging && widgetRemoveActive
                                            if (!shouldRemove) {
                                                val grid = gridCoords?.takeIf { it.isAttached }
                                                val widget = widgetCoords?.takeIf { it.isAttached }
                                                if (grid != null && widget != null) {
                                                    val gridBounds = grid.boundsInRoot()
                                                    val widgetTopLeft = widget.boundsInRoot().topLeft + widgetDragOffset
                                                    val cellWpx = with(density) { (cellW + gap).toPx() }
                                                    val cellHpx = with(density) { (cellH + gap).toPx() }
                                                    val nextCol = ((widgetTopLeft.x - gridBounds.left) / cellWpx)
                                                        .roundToInt()
                                                        .coerceIn(0, HOME_GRID_COLS - span.cols)
                                                    val nextRow = ((widgetTopLeft.y - gridBounds.top) / cellHpx)
                                                        .roundToInt()
                                                        .coerceIn(0, HOME_GRID_ROWS - span.rows)
                                                    currentOnUpdateHomeWidget.value(span.copy(col = nextCol, row = nextRow))
                                                }
                                            }
                                            resetWidgetDrag()
                                            if (shouldRemove) {
                                                if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                currentOnRemoveWidget.value()
                                            }
                                        },
                                        onDragCancel = {
                                            resetWidgetDrag()
                                        },
                                    )
                                },
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = widgetConfigMode && !widgetDragging,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = controlsX, y = controlsY)
                    .width(controlsPanelWidth)
                    .zIndex(8f),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                WidgetConfigControls(
                    compact = controlsCompact,
                    medium = controlsMedium,
                    onResizeWidget = onResizeWidget,
                    onReplaceWidget = onReplaceWidget,
                    onRemoveWidget = onRemoveWidget,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xE6111720))
                        .border(0.6.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                        .onGloballyPositioned { onWidgetControlsBoundsChanged(it.boundsInRoot()) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigControls(
    compact: Boolean,
    medium: Boolean,
    onResizeWidget: () -> Unit,
    onReplaceWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (compact) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WidgetEditChip("Resize", onResizeWidget, modifier = Modifier.fillMaxWidth())
            WidgetEditChip("Change", onReplaceWidget, modifier = Modifier.fillMaxWidth())
            WidgetEditChip("Remove", onRemoveWidget, danger = true, modifier = Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetEditChip(
                if (medium) "Size" else "Resize",
                onResizeWidget,
                modifier = Modifier.weight(1f),
            )
            WidgetEditChip(
                "Change",
                onReplaceWidget,
                modifier = Modifier.weight(1f),
            )
            WidgetEditChip("Remove", onRemoveWidget, danger = true, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WidgetEditChip(
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = if (danger) Color(0xFFFFCED5) else Color(0xFFEAF2F8),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (danger) Color(0x662B1217) else Color(0xFF202B36))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

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
    reorderMode: Boolean,
    onEnterReorderMode: () -> Unit,
    onExitReorderMode: () -> Unit,
    allApps: List<AppEntry>,
    hiddenPackages: Set<String>,
    onLaunchApp: (String) -> Unit,
    swipeUpPackage: String,
    swipeRightPackage: String,
    doubleTapPackage: String,
    hapticsEnabled: Boolean,
    hapticIntensity: Int,
    homeWidgetId: Int?,
    homeWidgetConfig: HomeWidgetConfig,
    appWidgetHost: AppWidgetHost,
    widgetConfigMode: Boolean,
    onOpenWidgetConfigMode: () -> Unit,
    onDismissWidgetConfigMode: () -> Unit,
    onUpdateHomeWidget: (HomeWidgetConfig) -> Unit,
    onResizeWidget: () -> Unit,
    onReplaceWidget: () -> Unit,
    onRemoveWidget: () -> Unit,
    onLongPress: () -> Unit,
    doubleTapToSleepEnabled: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    appIconShape: AppIconShape,
    themePalette: LauncherThemePalette,
    glanceEnabled: Boolean,
    glanceStripPreferences: GlanceStripPreferences,
    homeStripBounds: Rect?,
    removeDropVisible: Boolean,
    removeDropActive: Boolean,
    onRemoveDropVisibleChanged: (Boolean) -> Unit = {},
    onRemoveDropActiveChanged: (Boolean) -> Unit = {},
    removeDropBounds: Rect? = null,
    onRemoveDropBoundsChanged: (Rect?) -> Unit = {},
    onOpenGestureSettings: () -> Unit = {},
) {
    val view = LocalView.current
    var navArea by remember { mutableStateOf(HomeNavArea.Strip) }
    var stripIndex by remember { mutableStateOf(0) }
    var dockIndex by remember { mutableStateOf(0) }
    val dockSize = if (classicMode) 2 else 4
    var homePageBounds by remember { mutableStateOf<Rect?>(null) }
    var homeWidgetBounds by remember { mutableStateOf<Rect?>(null) }
    var homeWidgetControlsBounds by remember { mutableStateOf<Rect?>(null) }
    var homeWidgetDragActive by remember { mutableStateOf(false) }
    LaunchedEffect(reorderMode) {
        if (!reorderMode) {
            homeWidgetDragActive = false
            homeWidgetControlsBounds = null
            onRemoveDropVisibleChanged(false)
            onRemoveDropActiveChanged(false)
            onRemoveDropBoundsChanged(null)
        }
    }
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
    val density = LocalDensity.current
    val adaptiveLayout = rememberAdaptiveLayout()
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    val glanceRef = remember { mutableStateOf<GlanceDateWeatherEventsView?>(null) }
    val actions = remember(context) { LauncherActions(context) }
    var soundProfile by remember { mutableStateOf(actions.currentSoundProfile()) }
    var showSoundMenu by remember { mutableStateOf(false) }

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
    LaunchedEffect(homeActive) {
        if (homeActive) {
            var lastLoggedProfile: SoundProfileMode? = null
            while (true) {
                val resolved = actions.currentSoundProfile()
                if (resolved != lastLoggedProfile) {
                    Log.d("SoundProfile", "UI sync profile=${resolved.name}")
                    lastLoggedProfile = resolved
                }
                soundProfile = resolved
                delay(1500L)
            }
        }
    }
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    fun isInsideHomeWidget(localPosition: Offset): Boolean {
        val pageBounds = homePageBounds ?: return false
        val widgetBounds = homeWidgetBounds ?: return false
        return widgetBounds.contains(pageBounds.topLeft + localPosition)
    }
    fun isInsideHomeWidgetManagementTarget(localPosition: Offset): Boolean {
        val pageBounds = homePageBounds ?: return false
        val widgetBounds = homeWidgetBounds ?: return false
        val targetBounds = minimumCenteredRect(
            bounds = widgetBounds,
            minSizePx = with(density) { 96.dp.toPx() },
        )
        return targetBounds.contains(pageBounds.topLeft + localPosition)
    }

    var searchFocusIndex by remember { mutableStateOf(-1) }
    val searchResults = remember(searchQuery, allApps, hiddenPackages) {
        rankHomeSearchApps(
            query = searchQuery,
            allApps = allApps,
            hiddenPackages = hiddenPackages,
        ).take(4)
    }
    val settingsResults = remember(searchQuery) { matchSettingsEntries(searchQuery) }
    LaunchedEffect(searchQuery, searchResults, settingsResults) {
        searchFocusIndex = when {
            searchQuery.isEmpty() -> -1
            searchResults.isNotEmpty() || settingsResults.isNotEmpty() -> 0
            else -> -1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onGloballyPositioned { homePageBounds = it.boundsInRoot() }
            .pointerInput(reorderMode, widgetConfigMode, searchQuery, homeStripBounds, homeWidgetBounds, homeWidgetControlsBounds, homePageBounds, homeWidgetDragActive) {
                if ((!reorderMode && !widgetConfigMode) || searchQuery.isNotEmpty()) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial) ?: return@awaitEachGesture
                    val pageBounds = homePageBounds ?: return@awaitEachGesture
                    val tapRoot = pageBounds.topLeft + up.position
                    val insideWidget = homeWidgetBounds?.contains(tapRoot) == true
                    val insideControls = homeWidgetControlsBounds?.contains(tapRoot) == true
                    val insideStrip = homeStripBounds?.contains(tapRoot) == true
                    if (widgetConfigMode) {
                        if (!insideWidget && !insideControls) {
                            onDismissWidgetConfigMode()
                            down.consume()
                            up.consume()
                        }
                        return@awaitEachGesture
                    }
                    if (!insideStrip && !insideWidget && !insideControls && !homeWidgetDragActive) {
                        onExitReorderMode()
                        down.consume()
                        up.consume()
                    }
                }
            }
            .onPreviewKeyEvent { ev ->
                if (!homeActive) return@onPreviewKeyEvent false
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
                            nk?.keyCode == android.view.KeyEvent.KEYCODE_ENTER) -> {
                            val effectiveSearchIndex =
                                if (searchFocusIndex >= 0) searchFocusIndex else 0
                            if (effectiveSearchIndex < searchResults.size) {
                                val app = searchResults.getOrNull(effectiveSearchIndex)
                                if (app != null) { onSearchQueryChange(""); onLaunchApp(app.packageName) }
                            } else {
                                val entry = settingsResults.getOrNull(effectiveSearchIndex - searchResults.size)
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
            .pointerInput(doubleTapToSleepEnabled, doubleTapPackage, searchQuery, homeWidgetBounds, homePageBounds) {
                if (reorderMode) return@pointerInput
                val longPressMs = android.view.ViewConfiguration.getLongPressTimeout().toLong()
                val doubleTapMs = android.view.ViewConfiguration.getDoubleTapTimeout().toLong()
                val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat()
                var lastTapMs = 0L
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (isInsideHomeWidgetManagementTarget(down.position)) {
                        var cancelledByMove = false
                        var releasedBeforeLongPress = false
                        withTimeoutOrNull(longPressMs) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: continue
                                if (!change.pressed) {
                                    releasedBeforeLongPress = true
                                    return@withTimeoutOrNull
                                }
                                if ((change.position - down.position).getDistance() > touchSlop) {
                                    cancelledByMove = true
                                    return@withTimeoutOrNull
                                }
                            }
                        }
                        if (!cancelledByMove && !releasedBeforeLongPress) {
                            down.consume()
                            if (hapticsEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            onOpenWidgetConfigMode()
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                                if (event.changes.none { it.pressed }) break
                            }
                            return@awaitEachGesture
                        }
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        return@awaitEachGesture
                    }
                    if (isInsideHomeWidget(down.position)) {
                        // Do not observe, consume, or compete with AppWidgetHostView. Widgets own
                        // normal taps so playback buttons, launch areas, checkboxes, etc. work.
                        return@awaitEachGesture
                    }
                    if (searchQuery.isNotEmpty()) {
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        return@awaitEachGesture
                    }
                    var cancelledByMove = false
                    var releasedBeforeLongPress = false
                    withTimeoutOrNull(longPressMs) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: continue
                            if (!change.pressed) {
                                releasedBeforeLongPress = true
                                return@withTimeoutOrNull
                            }
                            if ((change.position - down.position).getDistance() > touchSlop) {
                                cancelledByMove = true
                                return@withTimeoutOrNull
                            }
                        }
                    }
                    if (cancelledByMove) {
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        return@awaitEachGesture
                    }
                    if (!releasedBeforeLongPress) {
                        currentOnLongPress.value()
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        return@awaitEachGesture
                    }
                    val now = SystemClock.uptimeMillis()
                    if (now - lastTapMs <= doubleTapMs) {
                        lastTapMs = 0L
                        when {
                            doubleTapToSleepEnabled -> {
                                // doVibrate fires unconditionally (ignores hapticsEnabled) and
                                // calls VibrationEffect directly so screen-off can't cancel it
                                doVibrate(view, hapticIntensity)
                                SleepManager.lockNow(context)
                            }
                            doubleTapPackage.isNotEmpty() -> {
                                doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                onLaunchApp(doubleTapPackage)
                            }
                        }
                    } else {
                        lastTapMs = now
                    }
                }
            }
            .pointerInput(swipeUpPackage, searchQuery, homeWidgetBounds, homePageBounds, reorderMode) {
                if (reorderMode) return@pointerInput
                val threshold = 80.dp.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        if (isInsideHomeWidget(down.position)) {
                            waitForUpOrCancellation(pass = PointerEventPass.Final)
                            continue
                        }
                        val startY = down.position.y
                        var triggered = false
                        while (!triggered) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
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
            .pointerInput(swipeRightPackage, searchQuery, homeWidgetBounds, homePageBounds, reorderMode) {
                if (reorderMode) return@pointerInput
                val threshold = 80.dp.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        if (isInsideHomeWidget(down.position)) {
                            waitForUpOrCancellation(pass = PointerEventPass.Final)
                            continue
                        }
                        val startX = down.position.x
                        val startY = down.position.y
                        var triggered = false
                        while (!triggered) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val dx = change.position.x - startX
                            val dy = change.position.y - startY
                            // Only fire if swipe is predominantly rightward (not a diagonal down/up swipe)
                            if (dx > threshold && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f
                                && swipeRightPackage.isNotEmpty() && searchQuery.isEmpty()
                            ) {
                                triggered = true
                                val canLaunch = context.packageManager
                                    .getLaunchIntentForPackage(swipeRightPackage) != null
                                if (canLaunch) {
                                    onLaunchApp(swipeRightPackage)
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.blackberry_hub_not_installed),
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                    onOpenGestureSettings()
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(searchQuery, homeWidgetBounds, homePageBounds, reorderMode) {
                if (reorderMode) return@pointerInput
                val threshold = 72.dp.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        if (isInsideHomeWidget(down.position)) {
                            waitForUpOrCancellation(pass = PointerEventPass.Final)
                            continue
                        }
                        val startY = down.position.y
                        var triggered = false
                        while (!triggered) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
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
        if (reorderMode && removeDropVisible) {
            val removeBandAlpha by animateFloatAsState(
                targetValue = if (removeDropActive) 1f else 0.82f,
                animationSpec = tween(140),
                label = "removeBandAlpha",
            )
            val removeBandScale by animateFloatAsState(
                targetValue = if (removeDropActive) 1.02f else 1f,
                animationSpec = tween(140, easing = FastOutSlowInEasing),
                label = "removeBandScale",
            )
            Box(
                modifier = Modifier
                    .zIndex(20f)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(if (removeDropActive) Color(0xF70A0C13) else Color(0xF1080B12))
                    .onGloballyPositioned { coords ->
                        onRemoveDropBoundsChanged(coords.boundsInRoot())
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = removeBandScale
                            scaleY = removeBandScale
                            alpha = removeBandAlpha
                        }
                        .background(if (removeDropActive) Color(0xFF2B1217) else Color(0xF2090D14))
                        .border(
                            width = if (removeDropActive) 1.5.dp else 0.dp,
                            color = if (removeDropActive) Color(0xB8FF6B7A) else Color.Transparent,
                            shape = RectangleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = if (removeDropActive) Color(0xFFFFC7CF) else Color(0xFFEAEFF6),
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Remove",
                            color = if (removeDropActive) Color(0xFFFFE7EA) else Color(0xFFF0F3F8),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (homeWidgetDragActive) 30f else 0f)
                .statusBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        ) {
            if (glanceEnabled && homeActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top)
                        .heightIn(max = 200.dp),
                ) {
                    AndroidView(
                        factory = { GlanceDateWeatherEventsView(it) },
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(align = Alignment.Top),
                        update = { v ->
                            glanceRef.value = v
                            v.applyStripPreferences(glanceStripPreferences)
                        },
                    )
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        SoundProfileHeaderIcon(
                            profile = soundProfile,
                            onClick = { showSoundMenu = true },
                        )
                        DropdownMenu(
                            expanded = showSoundMenu,
                            onDismissRequest = { showSoundMenu = false },
                            modifier = Modifier.background(Color(0xFF1A2035)),
                        ) {
                            listOf(
                                SoundProfileMode.RING,
                                SoundProfileMode.VIBRATE,
                                SoundProfileMode.DND,
                            ).forEach { mode ->
                                val selected = soundProfile == mode
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE8EBF0)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = when (mode) {
                                                        SoundProfileMode.RING -> Icons.AutoMirrored.Rounded.VolumeUp
                                                        SoundProfileMode.VIBRATE -> Icons.Outlined.Vibration
                                                        SoundProfileMode.DND -> Icons.AutoMirrored.Rounded.VolumeOff
                                                    },
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E3440),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                            Text(
                                                when (mode) {
                                                    SoundProfileMode.RING -> "Ring"
                                                    SoundProfileMode.VIBRATE -> "Vibrate"
                                                    SoundProfileMode.DND -> "DND"
                                                },
                                                color = if (selected) Color(0xFF5EB6FF) else Color(0xFFDDE4F0),
                                                fontSize = 14.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (selected) Text("✓", color = Color(0xFF5EB6FF), fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        val ok = actions.applySoundProfile(mode)
                                        if (ok) {
                                            soundProfile = mode
                                            showSoundMenu = false
                                        } else {
                                            showSoundMenu = false
                                            if (mode == SoundProfileMode.DND) {
                                                actions.openDoNotDisturbSettings()
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            ) {
                HomeGridCanvas(
                    homeWidgetId = homeWidgetId,
                    homeWidgetConfig = homeWidgetConfig,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager,
                    reorderMode = reorderMode,
                    onEnterReorderMode = onEnterReorderMode,
                    widgetConfigMode = widgetConfigMode,
                    onOpenWidgetConfigMode = onOpenWidgetConfigMode,
                    hapticsEnabled = hapticsEnabled,
                    removeDropBounds = removeDropBounds,
                    onRemoveDropVisibleChanged = onRemoveDropVisibleChanged,
                    onRemoveDropActiveChanged = onRemoveDropActiveChanged,
                    onResizeWidget = {
                        if (reorderMode) onExitReorderMode()
                        onResizeWidget()
                    },
                    onReplaceWidget = {
                        if (reorderMode) onExitReorderMode()
                        onReplaceWidget()
                    },
                    onRemoveWidget = {
                        onRemoveWidget()
                        if (reorderMode) onExitReorderMode()
                    },
                    onUpdateHomeWidget = onUpdateHomeWidget,
                    onWidgetBoundsChanged = { homeWidgetBounds = it },
                    onWidgetControlsBoundsChanged = { homeWidgetControlsBounds = it },
                    onWidgetDragActiveChanged = { homeWidgetDragActive = it },
                )
            }
        }

        // Home search overlay — compact top card, wallpaper visible below
        if (homeActive && searchQuery.isNotEmpty()) {
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
                            contentDescription = stringResource(R.string.action_clear),
                            tint = Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                // Scrollable results — capped so it never goes behind the dock.
                // Driven by AdaptiveLayout so the cap responds to screen size/rotation/fold.
                val searchResultsMaxHeight = adaptiveLayout.homeSearchResultsMaxHeightDp
                Column(
                    modifier = Modifier
                        .heightIn(max = searchResultsMaxHeight)
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
                            .background(if (isFocused && LocalTrackpadActive.current) Color(0x336EA8D8) else Color.Transparent)
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
                        val baseColor = if (isFocused && LocalTrackpadActive.current) Color(0xFF84D5F6) else HOME_STRIP_LABEL_COLOR
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
                        Text(stringResource(R.string.action_settings_label), color = Color(0xFF8E95A3), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    settingsResults.forEachIndexed { settingsIdx, entry ->
                        val absoluteIdx = searchResults.size + settingsIdx
                        val isFocused = absoluteIdx == searchFocusIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(searchRowBringers[absoluteIdx])
                                .background(if (isFocused && LocalTrackpadActive.current) Color(0x336EA8D8) else Color.Transparent)
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
                                    tint = if (isFocused && LocalTrackpadActive.current) Color(0xFF84D5F6) else Color(0xFF8E95A3),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = entry.label,
                                color = if (isFocused && LocalTrackpadActive.current) Color(0xFF84D5F6) else HOME_STRIP_LABEL_COLOR,
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
                        text = stringResource(R.string.drawer_no_apps_found),
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
    val actionLabel: String = "",
    val closeOnSuccess: Boolean = true,
    val onLongPress: (() -> Boolean)? = null,
    val onTap: () -> Boolean,
)

@Composable
private fun SoundProfileHeaderIcon(
    profile: SoundProfileMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (profile) {
        SoundProfileMode.RING -> Icons.AutoMirrored.Rounded.VolumeUp
        SoundProfileMode.VIBRATE -> Icons.Outlined.Vibration
        SoundProfileMode.DND -> Icons.AutoMirrored.Rounded.VolumeOff
    }
    val iconTint = Color(0xFFF3F6FA)
    Row(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.cd_sound_profile),
            tint = iconTint,
            modifier = Modifier.size(31.dp),
        )
    }
}

@Composable

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
internal fun QuickSettingsOverlay(
    allApps: List<AppEntry>,
    hiddenPackages: Set<String>,
    qrScannerPackage: String,
    savedTileOrder: List<String>,
    themePalette: LauncherThemePalette,
    hapticsEnabled: Boolean,
    hapticIntensity: Int,
    greyscaleEnabled: Boolean,
    onOpenModes: () -> Unit,
    onDismiss: () -> Unit,
    onSetQrScannerPackage: (String) -> Unit,
    onSetTileOrder: (List<String>) -> Unit,
    onToggleGreyscale: () -> Unit,
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val quickSettingsInternetTitle = stringResource(R.string.quick_settings_internet)
    val quickSettingsBluetoothTitle = stringResource(R.string.quick_settings_bluetooth)
    val quickSettingsQrTileTitle = stringResource(R.string.quick_settings_qr_tile)
    val quickSettingsWirelessDebuggingTitle = stringResource(R.string.quick_settings_wireless_debugging)
    val quickSettingsBatteryTitle = stringResource(R.string.quick_settings_battery)
    val quickSettingsAeroplaneModeTitle = stringResource(R.string.quick_settings_aeroplane_mode)
    val quickSettingsTorchTitle = stringResource(R.string.quick_settings_torch)
    val quickSettingsMyVaultTitle = stringResource(R.string.quick_settings_my_vault)
    val quickSettingsBatterySaverOn = stringResource(R.string.quick_settings_battery_saver_on)
    val quickSettingsDndTitle = stringResource(R.string.quick_settings_dnd)
    val quickSettingsStorageTitle = stringResource(R.string.quick_settings_storage)
    val quickSettingsHotspotTitle = stringResource(R.string.quick_settings_hotspot)
    val quickSettingsNightLightTitle = stringResource(R.string.quick_settings_night_light)
    val quickSettingsAutoRotateTitle = stringResource(R.string.quick_settings_auto_rotate)
    val quickSettingsNfcTitle = stringResource(R.string.quick_settings_nfc)
    val quickSettingsExtraDimTitle = stringResource(R.string.quick_settings_extra_dim)
    val quickSettingsLocationTitle = stringResource(R.string.quick_settings_location)
    val quickSettingsScreenRecordTitle = stringResource(R.string.quick_settings_screen_record)
    val quickSettingsScreenCastTitle = stringResource(R.string.quick_settings_screen_cast)
    val quickSettingsGreyscaleTitle = stringResource(R.string.quick_settings_greyscale)
    val quickSettingsBedtimeTitle = stringResource(R.string.quick_settings_bedtime)
    val quickSettingsStart = stringResource(R.string.quick_settings_start)
    val quickSettingsOn = stringResource(R.string.settings_on)
    val quickSettingsOff = stringResource(R.string.settings_off)
    val soundProfileRingLabel = stringResource(R.string.sound_profile_ring)
    val soundProfileVibrateLabel = stringResource(R.string.sound_profile_vibrate)
    val soundProfileDndLabel = stringResource(R.string.quick_settings_dnd)
    val quickSettingsTapToAllowControl = stringResource(R.string.quick_settings_tap_to_allow_control)
    val quickSettingsBluetoothPermissionDenied = stringResource(R.string.quick_settings_bluetooth_permission_denied)
    val quickSettingsWifiNamePermissionDenied = stringResource(R.string.quick_settings_wifi_name_permission_denied)
    val quickSettingsTorchToggleFailed = stringResource(R.string.quick_settings_torch_toggle_failed)
    val quickSettingsCameraPermissionDenied = stringResource(R.string.quick_settings_camera_permission_denied)
    val quickSettingsKeyboardMouse = stringResource(R.string.quick_settings_keyboard_mouse)
    val quickSettingsKeyboardModeFailed = stringResource(R.string.quick_settings_keyboard_mode_failed)
    val quickSettingsKeyboardModePermissionPrompt = stringResource(R.string.quick_settings_keyboard_mode_permission_prompt)
    val quickSettingsBluetoothBlocked = stringResource(R.string.quick_settings_bluetooth_blocked)
    val quickSettingsAutoRotatePermissionPrompt = stringResource(R.string.quick_settings_auto_rotate_permission_prompt)
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    var dateText by remember { mutableStateOf(dateFormatter.format(Date())) }
    var bluetoothEnabled by remember { mutableStateOf(actions.isBluetoothEnabled()) }
    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            bluetoothEnabled = actions.isBluetoothEnabled()
        } else {
            Toast.makeText(context, quickSettingsBluetoothPermissionDenied, Toast.LENGTH_SHORT).show()
        }
    }
    val wifiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, quickSettingsWifiNamePermissionDenied, Toast.LENGTH_SHORT).show()
        }
    }
    val wifiPrecisePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ -> }
    val btSubtitle = when (bluetoothEnabled) {
        true -> quickSettingsOn
        false -> quickSettingsOff
        null -> quickSettingsTapToAllowControl
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
    // greyscaleOn removed — now driven by prefs.minimalModeGreyscale via onToggleGreyscale
    var locationOn by remember { mutableStateOf(actions.isLocationEnabled()) }
    var torchOn by remember { mutableStateOf(actions.isTorchEnabled()) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            when (val r = actions.toggleTorch()) {
                is ToggleResult.Changed -> torchOn = r.enabled
                else -> Toast.makeText(context, quickSettingsTorchToggleFailed, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, quickSettingsCameraPermissionDenied, Toast.LENGTH_SHORT).show()
        }
    }
    var keyboardMode by remember { mutableStateOf(actions.lastKnownKeyboardMode()) }
    var showTileEditor by remember { mutableStateOf(false) }
    var showQrScannerPicker by remember { mutableStateOf(false) }
    var preciseWifiPromptAttempted by remember { mutableStateOf(false) }
    var batteryPct by remember { mutableStateOf(actions.batteryPercent()) }
    var soundProfile by remember { mutableStateOf(actions.currentSoundProfile()) }
    var soundProfileSetAt by remember { mutableLongStateOf(0L) }
    val hasBitwarden = remember(actions) { actions.isBitwardenInstalled() }
    val hasWellbeing = remember(actions) { actions.isDigitalWellbeingInstalled() }
    val hasScreenRecordSettings = remember(actions) { actions.canOpenScreenRecordSettings() }
    val quickSettingsSystemSettingsTitle = stringResource(R.string.quick_settings_system_settings)
    val quickSettingsWifiTitle = stringResource(R.string.quick_settings_wifi)
    val quickSettingsNotificationsTileTitle = stringResource(R.string.quick_settings_notifications_tile)
    val quickSettingsMobileDataTitle = stringResource(R.string.quick_settings_mobile_data)
    fun refreshQuickSettingsState(promptForPreciseWifi: Boolean) {
        dateText = dateFormatter.format(Date())
        wifiSubtitle = actions.currentWifiSsidLabel()
        if (
            promptForPreciseWifi &&
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
        bluetoothEnabled = actions.isBluetoothEnabled()
        wirelessDebugOn = actions.isWirelessDebuggingEnabled()
        batterySaverOn = actions.isBatterySaverEnabled()
        airplaneOn = actions.isAirplaneModeEnabled()
        dndOn = actions.isDoNotDisturbEnabled()
        hotspotOn = actions.isHotspotEnabled()
        nightLightOn = actions.isNightLightEnabled()
        autoRotateOn = actions.isAutoRotateEnabled()
        nfcOn = actions.isNfcEnabled()
        extraDimOn = actions.isExtraDimEnabled()
        locationOn = actions.isLocationEnabled()
        torchOn = actions.isTorchEnabled()
        batteryPct = actions.batteryPercent()
        if (android.os.SystemClock.elapsedRealtime() - soundProfileSetAt > 2_000L) {
            soundProfile = actions.currentSoundProfile()
        }
        actions.currentKeyboardMode()?.let {
            keyboardMode = it
            actions.persistKeyboardModeLabel(it)
        }
    }
    LaunchedEffect(Unit) {
        if (!actions.hasWifiNamePermission()) {
            wifiPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        refreshQuickSettingsState(promptForPreciseWifi = true)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshQuickSettingsState(promptForPreciseWifi = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val internetSubtitle = when {
        wifiEnabled != false && wifiSubtitle != "Disconnected" -> wifiSubtitle
        mobileDataEnabled != false -> carrierSubtitle
        else -> quickSettingsOff
    }
    val internetHighlighted =
        (wifiEnabled != false && wifiSubtitle != "Disconnected") || (mobileDataEnabled != false)
    fun launchConfiguredQrScanner(): Boolean {
        val pkg = qrScannerPackage.trim()
        logQuickSettings {
            "launchConfiguredQrScanner pkg=${if (pkg.isEmpty()) "<default>" else pkg}"
        }
        return if (pkg.isEmpty()) {
            actions.openQrScanner()
        } else {
            actions.launchApp(pkg) || actions.openQrScanner()
        }
    }
    val soundIcon = when (soundProfile) {
        SoundProfileMode.RING    -> Icons.Rounded.Notifications
        SoundProfileMode.VIBRATE -> Icons.Rounded.Vibration
        SoundProfileMode.DND     -> Icons.Rounded.NotificationsOff
    }
    val soundSubtitle = when (soundProfile) {
        SoundProfileMode.RING    -> soundProfileRingLabel
        SoundProfileMode.VIBRATE -> soundProfileVibrateLabel
        SoundProfileMode.DND     -> soundProfileDndLabel
    }
    val defaultQuickTiles = buildList {
        // ── First 8: mirror Minimal Mode tiles ───────────────────────────────
        add(
            QuickTile(
                id = "system_settings",
                icon = Icons.Rounded.Settings,
                title = quickSettingsSystemSettingsTitle,
                subtitle = "",
                showChevron = true,
                actionLabel = "Open",
                onTap = { actions.openSystemSettings(); true },
            ),
        )
        add(
            QuickTile(
                id = "mobile_data",
                icon = Icons.Rounded.CellTower,
                title = quickSettingsMobileDataTitle,
                subtitle = carrierSubtitle,
                highlighted = mobileDataEnabled != false,
                closeOnSuccess = false,
                showChevron = true,
                actionLabel = "Settings",
                onLongPress = actions::openMobileNetworkSettings,
                onTap = { actions.openMobileNetworkSettings(); true },
            ),
        )
        add(
            QuickTile(
                id = "wifi",
                icon = Icons.Rounded.Wifi,
                title = quickSettingsWifiTitle,
                subtitle = if (wifiEnabled != false) wifiSubtitle else quickSettingsOff,
                highlighted = wifiEnabled != false && wifiSubtitle != "Disconnected",
                closeOnSuccess = false,
                showChevron = true,
                actionLabel = "Panel",
                onLongPress = actions::openInternetSettings,
                onTap = actions::openInternetPanel,
            ),
        )
        add(
            QuickTile(
                id = "torch",
                icon = Icons.Rounded.Lightbulb,
                title = quickSettingsTorchTitle,
                subtitle = if (torchOn) quickSettingsOn else quickSettingsOff,
                highlighted = torchOn,
                closeOnSuccess = false,
                actionLabel = "Toggle",
                onLongPress = actions::openDisplaySettings,
                onTap = {
                    when (val r = actions.toggleTorch()) {
                        is ToggleResult.Changed -> { torchOn = r.enabled; true }
                        else -> {
                            Toast.makeText(context, quickSettingsTorchToggleFailed, Toast.LENGTH_SHORT).show()
                            true
                        }
                    }
                },
            ),
        )
        add(
            QuickTile(
                id = "notifications",
                icon = soundIcon,
                title = quickSettingsNotificationsTileTitle,
                subtitle = soundSubtitle,
                highlighted = soundProfile == SoundProfileMode.RING,
                closeOnSuccess = false,
                actionLabel = "Toggle",
                onTap = {
                    val next = when (soundProfile) {
                        SoundProfileMode.RING    -> SoundProfileMode.VIBRATE
                        SoundProfileMode.VIBRATE -> SoundProfileMode.DND
                        SoundProfileMode.DND     -> SoundProfileMode.RING
                    }
                    val ok = actions.applySoundProfile(next)
                    if (ok) {
                        soundProfile = next
                        soundProfileSetAt = android.os.SystemClock.elapsedRealtime()
                    } else if (next == SoundProfileMode.DND && !actions.hasDoNotDisturbAccess()) {
                        actions.openDoNotDisturbSettings()
                    }
                    true
                },
            ),
        )
        add(
            QuickTile(
                id = "bluetooth",
                icon = Icons.Rounded.Bluetooth,
                title = quickSettingsBluetoothTitle,
                subtitle = btSubtitle,
                highlighted = bluetoothEnabled == true,
                closeOnSuccess = false,
                actionLabel = "Toggle",
                onLongPress = actions::openBluetoothSettings,
                onTap = {
                    when (val r = actions.toggleBluetooth()) {
                        is ToggleResult.Changed -> {
                            bluetoothEnabled = r.enabled
                            Handler(Looper.getMainLooper()).postDelayed({ bluetoothEnabled = actions.isBluetoothEnabled() }, 500L)
                            true
                        }
                        ToggleResult.PermissionRequired -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            true
                        }
                        ToggleResult.Unsupported -> {
                            actions.openBluetoothSettings()
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
                title = quickSettingsQrTileTitle,
                subtitle = "",
                showChevron = true,
                closeOnSuccess = false,
                actionLabel = "Scan",
                onLongPress = { showQrScannerPicker = true; true },
                onTap = {
                    if (!launchConfiguredQrScanner()) showQrScannerPicker = true
                    true
                },
            ),
        )
        add(
            QuickTile(
                id = "hotspot",
                icon = Icons.Rounded.WifiTethering,
                title = quickSettingsHotspotTitle,
                subtitle = if (hotspotOn) quickSettingsOn else quickSettingsOff,
                highlighted = hotspotOn,
                actionLabel = "Settings",
                onLongPress = actions::openHotspotSettings,
                onTap = actions::openHotspotSettings,
            ),
        )
        // ── Remaining tiles ───────────────────────────────────────────────────
        add(
            QuickTile(
                id = "keyboard_mode",
                icon = Icons.Rounded.Tune,
                title = quickSettingsKeyboardMouse,
                subtitle = keyboardMode.replaceFirstChar { it.uppercase() },
                highlighted = true,
                closeOnSuccess = false,
                showChevron = true,
                actionLabel = "Mode",
                onLongPress = actions::openKeyboardSettings,
                onTap = {
                    val currentMode = actions.currentKeyboardMode() ?: keyboardMode
                    val nextMode = if (currentMode == "keyboard") "mouse" else "keyboard"
                    logQuickSettings { "keyboardTileTap current=$currentMode label=$keyboardMode next=$nextMode" }
                    val ok = actions.setKeyboardMode(nextMode)
                    if (ok) {
                        keyboardMode = actions.currentKeyboardMode() ?: nextMode
                        actions.persistKeyboardModeLabel(keyboardMode)
                        logQuickSettings { "keyboardTileApplied mode=$keyboardMode" }
                        Handler(Looper.getMainLooper()).postDelayed({
                            actions.currentKeyboardMode()?.let {
                                keyboardMode = it
                                logQuickSettings { "keyboardTileRefreshed mode=$it" }
                            }
                        }, 600L)
                    } else {
                        logQuickSettings { "keyboardTileFailed next=$nextMode canWrite=${actions.canWriteSystemSettings()}" }
                        if (!actions.canWriteSystemSettings()) {
                            actions.requestWriteSettingsPermission()
                            Toast.makeText(
                                context,
                                quickSettingsKeyboardModePermissionPrompt,
                                Toast.LENGTH_LONG,
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                quickSettingsKeyboardModeFailed,
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
                id = "wireless_debugging",
                icon = Icons.Rounded.Settings,
                title = quickSettingsWirelessDebuggingTitle,
                subtitle = if (wirelessDebugOn) quickSettingsOn else quickSettingsOff,
                highlighted = wirelessDebugOn,
                closeOnSuccess = false,
                actionLabel = "Settings",
                onLongPress = actions::openWirelessDebuggingSettings,
                onTap = actions::openWirelessDebuggingSettings,
            ),
        )
        add(
            QuickTile(
                id = "battery",
                icon = Icons.Rounded.BatteryStd,
                title = quickSettingsBatteryTitle,
                subtitle = buildString {
                    batteryPct?.let { append("$it%") }
                    if (batterySaverOn) {
                        if (isNotEmpty()) append(" · ")
                        append(quickSettingsBatterySaverOn)
                    }
                },
                highlighted = batterySaverOn,
                closeOnSuccess = false,
                showChevron = true,
                actionLabel = "Details",
                onLongPress = actions::openBatterySaverSettings,
                onTap = actions::openBatteryUsageSummary,
            ),
        )
        if (hasBitwarden) {
            add(
                QuickTile(
                    id = "my_vault",
                    icon = Icons.Rounded.Lock,
                    title = quickSettingsMyVaultTitle,
                    subtitle = "",
                    showChevron = true,
                    closeOnSuccess = false,
                    actionLabel = "App",
                    onLongPress = actions::openBitwardenVault,
                    onTap = actions::openBitwardenVault,
                ),
            )
        }
        add(
            QuickTile(
                id = "airplane_mode",
                icon = Icons.Rounded.SwapVert,
                title = quickSettingsAeroplaneModeTitle,
                subtitle = if (airplaneOn) quickSettingsOn else quickSettingsOff,
                highlighted = airplaneOn,
                actionLabel = "Settings",
                onLongPress = actions::openAirplaneModeSettings,
                onTap = actions::openAirplaneModeSettings,
            ),
        )
        add(
            QuickTile(
                id = "storage",
                icon = Icons.Rounded.Info,
                title = quickSettingsStorageTitle,
                subtitle = "",
                showChevron = true,
                actionLabel = "Details",
                onLongPress = actions::openStorageSettings,
                onTap = actions::openStorageSettings,
            ),
        )
        add(
            QuickTile(
                id = "night_light",
                icon = Icons.Rounded.WbSunny,
                title = quickSettingsNightLightTitle,
                subtitle = if (nightLightOn) quickSettingsOn else quickSettingsOff,
                highlighted = nightLightOn,
                actionLabel = "Settings",
                onLongPress = actions::openNightLightSettings,
                onTap = actions::openNightLightSettings,
            ),
        )
        add(
            QuickTile(
                id = "auto_rotate",
                icon = Icons.Rounded.SwapVert,
                title = quickSettingsAutoRotateTitle,
                subtitle = if (autoRotateOn) quickSettingsOn else quickSettingsOff,
                highlighted = autoRotateOn,
                actionLabel = "Toggle",
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
                                quickSettingsAutoRotatePermissionPrompt,
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
                title = quickSettingsNfcTitle,
                subtitle = if (nfcOn) quickSettingsOn else quickSettingsOff,
                highlighted = nfcOn,
                closeOnSuccess = false,
                actionLabel = "Toggle",
                onLongPress = actions::openNfcSettings,
                onTap = {
                    when (val r = actions.toggleNfc()) {
                        is ToggleResult.Changed -> {
                            nfcOn = r.enabled
                            Handler(Looper.getMainLooper()).postDelayed({
                                nfcOn = actions.isNfcEnabled()
                            }, 1000L)
                            true
                        }
                        else -> { actions.openNfcSettings(); true }
                    }
                },
            ),
        )
        add(
            QuickTile(
                id = "location",
                icon = Icons.Rounded.LocationOn,
                title = quickSettingsLocationTitle,
                subtitle = if (locationOn) quickSettingsOn else quickSettingsOff,
                highlighted = locationOn,
                showChevron = true,
                actionLabel = "Settings",
                onLongPress = actions::openLocationSettings,
                onTap = { actions.openLocationSettings(); locationOn = actions.isLocationEnabled(); true },
            ),
        )
        add(
            QuickTile(
                id = "extra_dim",
                icon = Icons.Rounded.VisibilityOff,
                title = quickSettingsExtraDimTitle,
                subtitle = if (extraDimOn) quickSettingsOn else quickSettingsOff,
                highlighted = extraDimOn,
                closeOnSuccess = false,
                actionLabel = "Toggle",
                onLongPress = actions::openExtraDimSettings,
                onTap = {
                    when (val r = actions.toggleExtraDim()) {
                        is ToggleResult.Changed -> { extraDimOn = r.enabled; true }
                        else -> { actions.openExtraDimSettings(); true }
                    }
                },
            ),
        )
        add(
            QuickTile(
                id = "screen_record",
                icon = Icons.Rounded.TouchApp,
                title = quickSettingsScreenRecordTitle,
                subtitle = quickSettingsStart,
                showChevron = true,
                actionLabel = "Start",
                onLongPress = actions::openScreenRecordSettings,
                onTap = actions::openScreenRecordSettings,
            ),
        )
        add(
            QuickTile(
                id = "screen_cast",
                icon = Icons.Rounded.Wallpaper,
                title = quickSettingsScreenCastTitle,
                subtitle = quickSettingsOff,
                showChevron = true,
                actionLabel = "Settings",
                onLongPress = actions::openCastSettings,
                onTap = actions::openCastSettings,
            ),
        )
        add(
            QuickTile(
                id = "grayscale",
                icon = Icons.Rounded.FilterBAndW,
                title = quickSettingsGreyscaleTitle,
                subtitle = if (greyscaleEnabled) quickSettingsOn else quickSettingsOff,
                highlighted = greyscaleEnabled,
                closeOnSuccess = false,
                actionLabel = "Toggle",
                onTap = {
                    onToggleGreyscale()
                    true
                },
            ),
        )
        add(
            QuickTile(
                id = "bedtime",
                icon = Icons.Rounded.AddAlarm,
                title = quickSettingsBedtimeTitle,
                subtitle = quickSettingsOff,
                showChevron = true,
                actionLabel = "Settings",
                onLongPress = actions::openBedtimeSettings,
                onTap = actions::openBedtimeSettings,
            ),
        )
    }
    val orderedTileIds = remember { mutableStateListOf<String>() }
    val defaultTileIds = defaultQuickTiles.map { it.id }
    LaunchedEffect(defaultTileIds, savedTileOrder) {
        val defaultSet = defaultTileIds.toSet()
        val reconciledOrder = buildList {
            savedTileOrder.forEach { id ->
                if (id in defaultSet && id !in this) add(id)
            }
            defaultTileIds.forEach { id ->
                if (id !in this) add(id)
            }
        }
        if (orderedTileIds.toList() != reconciledOrder) {
            orderedTileIds.clear()
            orderedTileIds.addAll(reconciledOrder)
        }
    }
    val tileById = defaultQuickTiles.associateBy { it.id }
    val allQuickTiles = orderedTileIds.mapNotNull { tileById[it] }
    fun moveTile(from: Int, to: Int) {
        if (from == to) return
        if (from !in orderedTileIds.indices || to !in orderedTileIds.indices) return
        val id = orderedTileIds.removeAt(from)
        orderedTileIds.add(to, id)
        onSetTileOrder(orderedTileIds.toList())
        logQuickSettings { "savedTileOrder ids=${orderedTileIds.joinToString("|")}" }
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
    BackHandler(enabled = showQrScannerPicker) {
        showQrScannerPicker = false
    }
    val settingsDefaultApp = stringResource(R.string.settings_default_app)
    val qrScannerTitle = stringResource(R.string.quick_settings_qr_scanner_title)
    val qrScannerSubtitle = stringResource(R.string.quick_settings_qr_scanner_subtitle)
    val qrScannerDefaultText = stringResource(R.string.quick_settings_qr_use_default)
    val visibleQrApps = remember(allApps, hiddenPackages) {
        allApps.filter { it.packageName !in hiddenPackages && !it.internal }
    }

    fun runQsTileTap(tile: QuickTile) {
        logQuickSettings { "tileTap id=${tile.id}" }
        if (tile.id == "qr_scanner") {
            onDismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                val ok = tile.onTap()
                if (!ok) {
                    Toast.makeText(context, context.getString(R.string.could_not_open_tile, tile.title), Toast.LENGTH_SHORT).show()
                }
            }, 120L)
        } else {
            val ok = tile.onTap()
            if (!ok) {
                Toast.makeText(context, context.getString(R.string.could_not_open_tile, tile.title), Toast.LENGTH_SHORT).show()
            } else if (tile.closeOnSuccess) {
                onDismiss()
            }
        }
    }

    fun runQsTileLongPress(tile: QuickTile) {
        logQuickSettings { "tileLongPress id=${tile.id}" }
        if (tile.id == "qr_scanner") {
            showQrScannerPicker = true
        } else {
            val longOk = tile.onLongPress?.invoke() ?: false
            if (longOk) onDismiss()
        }
    }

    val statusTopPx = ViewCompat.getRootWindowInsets(view)
        ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
    val qsTopDarkBand = with(density) { statusTopPx.toDp() } + qsTopDarkBelowStatusDp
    val qsTopDarkBandPx = with(density) { qsTopDarkBand.toPx() }
    val dismissSwipeModifier =
        if (showTileEditor) {
            Modifier
        } else {
            Modifier.pointerInput(Unit) {
                val threshold = 72.dp.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val startY = down.position.y
                        var triggered = false
                        while (!triggered) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val dy = change.position.y - startY
                            if (dy < -threshold) {
                                triggered = true
                                logQuickSettings { "dismissSwipeTriggered dy=$dy showTileEditor=$showTileEditor" }
                                onDismiss()
                            }
                        }
                    }
                }
            }
        }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(520f)
            .then(dismissSwipeModifier),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xE6000000)).clickable(onClick = onDismiss))
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
        if (!showTileEditor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = qsPadStart, end = qsPadEnd, top = 4.dp, bottom = 4.dp)
                    .focusRequester(qsKeyFocusRequester)
                    .focusable()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        dateText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFFE6EBF2),
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                            lineHeight = 18.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_qs_open_settings),
                        tint = Color(0x66FFFFFF),
                        modifier = Modifier
                            .size(22.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onOpenModes() })
                            },
                    )
                }
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
                val pagerHeight = (maxPageRows * 72 + (maxPageRows - 1).coerceAtLeast(0) * 5).dp
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(pagerHeight),
                ) { page ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
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
                        contentDescription = stringResource(R.string.action_edit_quick_settings),
                        tint = Color(0x80E6EBF2),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { showTileEditor = true },
                    )
                }
            }
        }
        if (showTileEditor) {
            QuickSettingsTileEditorOverlay(
                tiles = allQuickTiles,
                onDismiss = { showTileEditor = false },
                onReset = {
                    orderedTileIds.clear()
                    orderedTileIds.addAll(defaultTileIds)
                    onSetTileOrder(defaultTileIds)
                    logQuickSettings { "resetSavedTileOrder ids=${defaultTileIds.joinToString("|")}" }
                },
                onMove = ::moveTile,
            )
        }
        if (showQrScannerPicker) {
            AppPickerOverlay(
                title = qrScannerTitle,
                subtitle = qrScannerSubtitle,
                apps = visibleQrApps,
                themePalette = themePalette,
                onSelect = {
                    onSetQrScannerPackage(it)
                    showQrScannerPicker = false
                },
                onUseDefault = {
                    onSetQrScannerPackage("")
                    showQrScannerPicker = false
                },
                onDismiss = { showQrScannerPicker = false },
                useDefaultLabel = qrScannerDefaultText,
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
    var draggingTileId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }
    val currentTiles = rememberUpdatedState(tiles)
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
                        contentDescription = stringResource(R.string.action_back),
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
                userScrollEnabled = draggingTileId == null,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(tiles, key = { _, tile -> tile.id }) { index, tile ->
                    var dragX by remember(tile.id) { mutableFloatStateOf(0f) }
                    var dragY by remember(tile.id) { mutableFloatStateOf(0f) }
                    var tileSize by remember(tile.id) { mutableStateOf(IntSize.Zero) }
                    val isDragging = draggingTileId == tile.id
                    val isDropTarget = !isDragging && dragTargetIndex == index && draggingTileId != null
                    val liftScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.04f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "qsEditorTileLift",
                    )
                    val targetScale by animateFloatAsState(
                        targetValue = if (isDropTarget) 0.96f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "qsEditorDropTarget",
                    )
                            Box(
                                modifier = Modifier
                            .animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = spring(
                                    stiffness = Spring.StiffnessLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                ),
                            )
                            .zIndex(if (isDragging) 1f else 0f)
                            .onSizeChanged { tileSize = it }
                            .graphicsLayer {
                                translationX = if (isDragging) dragX else 0f
                                translationY = if (isDragging) dragY else 0f
                                val scale = liftScale * targetScale
                                scaleX = scale
                                scaleY = scale
                                if (isDragging) {
                                    alpha = 0.94f
                                    shadowElevation = 16f
                                }
                            }
                            .pointerInput(tile.id, tiles.size, tileSize) {
                                val gapPx = 8.dp.toPx()
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { startOffset ->
                                        val latestTiles = currentTiles.value
                                        val startIndex = latestTiles.indexOfFirst { it.id == tile.id }
                                        if (startIndex < 0) return@detectDragGesturesAfterLongPress
                                        draggingTileId = tile.id
                                        dragStartIndex = startIndex
                                        dragTargetIndex = startIndex
                                        editorView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        logQuickSettings { "editorDragStart id=${tile.id} index=$startIndex startOffset=$startOffset" }
                                    },
                                    onDragEnd = {
                                        val latestTiles = currentTiles.value
                                        val from = latestTiles.indexOfFirst { it.id == tile.id }
                                        val to = dragTargetIndex.coerceIn(0, latestTiles.lastIndex)
                                        logQuickSettings { "editorDragEnd id=${tile.id} from=$from to=$to" }
                                        if (from >= 0 && to >= 0 && from != to) {
                                            onMove(from, to)
                                        }
                                        draggingTileId = null
                                        dragStartIndex = -1
                                        dragTargetIndex = -1
                                        dragX = 0f
                                        dragY = 0f
                                    },
                                    onDragCancel = {
                                        logQuickSettings { "editorDragCancel id=${tile.id}" }
                                        draggingTileId = null
                                        dragStartIndex = -1
                                        dragTargetIndex = -1
                                        dragX = 0f
                                        dragY = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragX += amount.x
                                        dragY += amount.y
                                        val target = qsEditorTargetIndexFromDrag(
                                            startIndex = dragStartIndex,
                                            dragX = dragX,
                                            dragY = dragY,
                                            tileSize = tileSize,
                                            gapPx = gapPx,
                                            itemCount = currentTiles.value.size,
                                        )
                                        if (target >= 0 && target != dragTargetIndex) {
                                            logQuickSettings {
                                                "editorPreviewMove id=${tile.id} from=$dragTargetIndex to=$target start=$dragStartIndex drag=($dragX,$dragY)"
                                            }
                                            dragTargetIndex = target
                                            editorView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                    },
                                )
                            },
                            ) {
                                ClassicQuickTile(
                                    tile = tile,
                                    onTap = {},
                                    onLongPress = {},
                                    enableGestures = false,
                                )
                            }
                }
            }
        }
    }
}

private fun qsEditorTargetIndexFromDrag(
    startIndex: Int,
    dragX: Float,
    dragY: Float,
    tileSize: IntSize,
    gapPx: Float,
    itemCount: Int,
): Int {
    if (startIndex !in 0 until itemCount || itemCount <= 0) return -1
    val tileWidth = tileSize.width.toFloat().coerceAtLeast(1f)
    val tileHeight = tileSize.height.toFloat().coerceAtLeast(1f)
    val startRow = startIndex / 2
    val startCol = startIndex % 2
    val targetRow = (startRow + (dragY / (tileHeight + gapPx)).roundToInt()).coerceAtLeast(0)
    val targetCol = (startCol + (dragX / (tileWidth + gapPx)).roundToInt()).coerceIn(0, 1)
    return (targetRow * 2 + targetCol).coerceIn(0, itemCount - 1)
}

@Composable
private fun ClassicQuickTile(
    tile: QuickTile,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    enableGestures: Boolean = true,
) {
    val darkCell  = Color(0xFF191D22)
    val activeBg  = Color(0xFF145A77)
    val tileBg    = if (tile.highlighted) activeBg else darkCell
    val iconBoxBg = if (tile.highlighted) Color(0x33000000) else Color(0xFF1A2530)
    val tileModifier =
        if (enableGestures) {
            Modifier.pointerInput(tile.id) {
                detectTapGestures(
                    onTap = {
                        logQuickSettings { "tileTapGesture id=${tile.id}" }
                        onTap()
                    },
                    onLongPress = {
                        logQuickSettings { "tileLongPressGesture id=${tile.id}" }
                        onLongPress()
                    },
                )
            }
        } else {
            Modifier
        }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(tileBg)
            .then(tileModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon box — square left section
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(iconBoxBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = Color(0xFFEAF0F6),
                modifier = Modifier.size(32.dp),
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
        // Text column — inherits tileBg from Row
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                tile.title,
                color = Color(0xFFEAF0F6),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tile.subtitle.isNotBlank()) {
                Text(
                    tile.subtitle,
                    color = Color(0xFFAEB8C5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
    val maxListHeight = rememberAdaptiveLayout().sheetListMaxHeightDp
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
                stringResource(R.string.action_pin_to_home_strip),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = themePalette.settingsMenuTitle,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pins an app icon to the strip above the dock.",
                style = MaterialTheme.typography.bodySmall,
                color = themePalette.settingsMenuBody,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.search_apps_hint), color = themePalette.settingsMenuBody) },
                placeholder = { Text(stringResource(R.string.search_apps_filter_hint), color = themePalette.settingsMenuBody.copy(alpha = 0.55f)) },
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
private fun AddAppToContainerSheet(
    title: String,
    allApps: List<AppEntry>,
    hiddenPackages: Set<String>,
    existingPackageNames: Set<String>,
    appIconShape: AppIconShape,
    themePalette: LauncherThemePalette,
    onSelect: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val maxListHeight = rememberAdaptiveLayout().sheetListMaxHeightDp
    val visibleApps = remember(allApps, hiddenPackages, existingPackageNames, query) {
        allApps
            .asSequence()
            .filter { it.packageName !in hiddenPackages }
            .filter { it.packageName != AppsRepository.INTERNAL_SETTINGS_PACKAGE }
            .filter { it.packageName !in existingPackageNames }
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
                "Add app to $title",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = themePalette.settingsMenuTitle,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.search_apps_hint), color = themePalette.settingsMenuBody) },
                placeholder = { Text(stringResource(R.string.search_apps_filter_hint), color = themePalette.settingsMenuBody.copy(alpha = 0.55f)) },
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
                    "No apps available to add.",
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

@Composable
private fun ZenoSetupOverlay(
    themePalette: LauncherThemePalette,
    /** Called with 0=Classic, 1=SmartHome, 2=Minimal when user presses Apply. */
    onApply: (mode: Int) -> Unit,
    onOpenPermissions: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    // selectedMode = which layout card is chosen (0-2)
    var selectedMode by remember { mutableStateOf(0) }
    // selectedIndex = which row the trackpad cursor is on (0-4)
    var selectedIndex by remember { mutableStateOf(0) }
    // 0=Classic, 1=SmartHome, 2=Minimal, 3=Permissions, 4=Apply
    val itemCount = 5
    val accentColor = Color(0xFF84D5F6)
    val scope = rememberCoroutineScope()
    val rowBringers = remember { List(itemCount) { BringIntoViewRequester() } }

    fun activate(index: Int) {
        when (index) {
            0, 1, 2 -> selectedMode = index   // select mode, stay on screen
            3 -> onOpenPermissions()
            4 -> onApply(selectedMode)
        }
    }

    LaunchedEffect(selectedIndex) {
        rowBringers[selectedIndex].bringIntoView()
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(700f)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent as? AndroidKeyEvent
                when {
                    ev.key == Key.DirectionUp || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                        val next = (selectedIndex - 1).coerceAtLeast(0)
                        if (next != selectedIndex) {
                            selectedIndex = next
                            scope.launch { rowBringers[next].bringIntoView() }
                        }
                        true
                    }
                    ev.key == Key.DirectionDown || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                        val next = (selectedIndex + 1).coerceAtMost(itemCount - 1)
                        if (next != selectedIndex) {
                            selectedIndex = next
                            scope.launch { rowBringers[next].bringIntoView() }
                        }
                        true
                    }
                    ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                        nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER -> {
                        activate(selectedIndex); true
                    }
                    else -> false
                }
            },
        color = Color(0xF4090D12),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Text(
                "Set up Zeno",
                color = Color(0xFFEAF2F8),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a starting layout. You can change everything later from Settings.",
                color = Color(0xFF9EADB8),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(18.dp))
            Column(Modifier.bringIntoViewRequester(rowBringers[0])) {
                SetupModeCard(
                    icon = Icons.Rounded.GridView,
                    title = "Classic BlackBerry",
                    subtitle = "Drawer-first, compact pages, dock-focused navigation.",
                    accentColor = accentColor,
                    selected = selectedMode == 0,
                    cursorOn = selectedIndex == 0,
                    onClick = { selectedMode = 0; selectedIndex = 0 },
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.bringIntoViewRequester(rowBringers[1])) {
                SetupModeCard(
                    icon = Icons.Rounded.WbSunny,
                    title = "Smart Home",
                    subtitle = "Home strip, Glance, and launcher Quick Settings enabled.",
                    accentColor = accentColor,
                    selected = selectedMode == 1,
                    cursorOn = selectedIndex == 1,
                    onClick = { selectedMode = 1; selectedIndex = 1 },
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.bringIntoViewRequester(rowBringers[2])) {
                SetupModeCard(
                    icon = Icons.Rounded.FilterBAndW,
                    title = "Minimal",
                    subtitle = "Clean home screen with only essentials visible.",
                    accentColor = accentColor,
                    selected = selectedMode == 2,
                    cursorOn = selectedIndex == 2,
                    onClick = { selectedMode = 2; selectedIndex = 2 },
                )
            }
            Spacer(Modifier.height(18.dp))
            Column(Modifier.bringIntoViewRequester(rowBringers[3])) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF111820),
                    border = BorderStroke(
                        0.7.dp,
                        if (selectedIndex == 3) accentColor else Color.White.copy(alpha = 0.10f),
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Recommended permissions",
                            color = Color(0xFFEAF2F8),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Enable only what you use: badges, calendar, weather, sleep helper, and usage sorting.",
                            color = Color(0xFF9EADB8),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    if (selectedIndex == 3) accentColor.copy(alpha = 0.15f)
                                    else Color(0xFF1B2630)
                                )
                                .clickable(onClick = { selectedIndex = 3; onOpenPermissions() })
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = accentColor, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Review permissions",
                                color = Color(0xFFEAF2F8),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .bringIntoViewRequester(rowBringers[4])
                    .fillMaxWidth(),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onApply(selectedMode) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedIndex == 4) accentColor else accentColor.copy(alpha = 0.90f),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 14.dp),
                    ) {
                        Text(
                            "Apply",
                            color = Color(0xFF070C11),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SetupModeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    /** True when this mode is the chosen layout (filled accent border + tint). */
    selected: Boolean = false,
    /** True when the trackpad cursor is resting on this row (dim highlight only). */
    cursorOn: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = when {
            selected -> accentColor.copy(alpha = 0.08f)
            cursorOn -> Color.White.copy(alpha = 0.04f)
            else -> Color(0xFF111820)
        },
        border = BorderStroke(
            if (selected) 1.2.dp else 0.8.dp,
            when {
                selected -> accentColor
                cursorOn -> accentColor.copy(alpha = 0.45f)
                else -> Color.White.copy(alpha = 0.10f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color(0xFFEAF2F8),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    subtitle,
                    color = Color(0xFF9EADB8),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeWidgetPickerSheet(
    appWidgetManager: AppWidgetManager,
    themePalette: LauncherThemePalette,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
    onOpenSystemPicker: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val pm = context.packageManager
    val searchFocusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }
    val providers = remember {
        runCatching {
            appWidgetManager
                .getInstalledProvidersForProfile(Process.myUserHandle())
                .sortedWith(
                    compareBy<AppWidgetProviderInfo>(
                        { runCatching { it.loadLabel(pm)?.toString().orEmpty().lowercase(Locale.getDefault()) }.getOrDefault("") },
                        { it.provider.packageName },
                    ),
                )
        }.getOrDefault(emptyList())
    }
    val filtered = remember(providers, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            providers
        } else {
            providers.filter { info ->
                val label = runCatching { info.loadLabel(pm)?.toString().orEmpty() }.getOrDefault("")
                val appLabel = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                }.getOrDefault(info.provider.packageName)
                label.contains(q, ignoreCase = true) ||
                    appLabel.contains(q, ignoreCase = true) ||
                    info.provider.packageName.contains(q, ignoreCase = true)
            }
        }
    }
    fun widgetAppLabel(info: AppWidgetProviderInfo): String =
        runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
        }.getOrDefault(info.provider.packageName)
    val grouped = remember(filtered) {
        filtered
            .groupBy { widgetAppLabel(it) }
            .toSortedMap(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }

    @Composable
    fun WidgetPreviewCard(info: AppWidgetProviderInfo) {
        val label = runCatching { info.loadLabel(pm)?.toString().orEmpty() }.getOrDefault("")
            .ifEmpty { info.provider.shortClassName.substringAfterLast('.') }
        val appLabel = widgetAppLabel(info)
        val span = estimateHomeWidgetSpan(info)
        val sizeOptions = remember(info.provider) { homeWidgetSizeOptions(info) }
        val preview = remember(info.provider) {
            runCatching { info.loadPreviewImage(context, 0) }.getOrNull()
        }
        val icon = remember(info.provider) {
            runCatching { info.loadIcon(context, 0) }.getOrNull()
        }
        val cardHeight = when {
            span.rows >= 3 -> 300.dp
            span.rows >= 2 -> 248.dp
            span.cols >= 3 -> 168.dp
            else -> 178.dp
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clickable { onSelectWidget(info) },
            shape = RoundedCornerShape(2.dp),
            color = Color(0xFF203236),
            border = BorderStroke(0.6.dp, Color.Black.copy(alpha = 0.28f)),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF34464A))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        color = Color(0xFFEAF2F8),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${span.cols} × ${span.rows}",
                        color = Color(0xFFEAF2F8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (sizeOptions.size > 1) {
                    Text(
                        "${sizeOptions.size} sizes available",
                        color = Color(0xFF9DB1B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2A3A3D))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preview != null) {
                        AsyncImage(
                            model = preview,
                            contentDescription = label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF11191D),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .heightIn(min = 72.dp, max = 108.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    model = icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        appLabel,
                                        color = Color(0xFFDCE5EC),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "Preview unavailable",
                                        color = Color(0xFF90A0AA),
                                        fontSize = 11.sp,
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
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(560f),
        color = Color(0xF21D2022),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFFB7BCC3), modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = {
                        Text(stringResource(R.string.search_widgets_hint), color = Color(0xFFB7BCC3), fontSize = 20.sp)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFEAF1FB),
                        unfocusedTextColor = Color(0xFFEAF1FB),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF00A6D6),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchFocusRequester)
                        .onPreviewKeyEvent { ev ->
                            val native = ev.nativeKeyEvent ?: return@onPreviewKeyEvent false
                            ev.type == KeyEventType.KeyDown &&
                                native.repeatCount > 0 &&
                                native.unicodeChar != 0
                        },
                )
            }
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (grouped.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.Apps,
                                    contentDescription = null,
                                    tint = Color(0xFF6F7D84),
                                    modifier = Modifier.size(34.dp),
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    if (query.isBlank()) "No widgets available" else "No widgets found",
                                    color = Color(0xFFD4DEE4),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    if (query.isBlank()) {
                                        "Installed apps have not exposed widgets."
                                    } else {
                                        "Try a different app or widget name."
                                    },
                                    color = Color(0xFF8FA0A8),
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
                grouped.forEach { (appLabel, widgets) ->
                    item(
                        key = "header:$appLabel",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Text(
                            appLabel,
                            color = Color(0xFFB9C7CB),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
                        )
                    }
                    items(
                        items = widgets,
                        key = { it.provider.flattenToString() },
                    ) { info ->
                        WidgetPreviewCard(info)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeWidgetResizeSheet(
    info: AppWidgetProviderInfo,
    currentConfig: HomeWidgetConfig,
    onSelect: (HomeWidgetSpan) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = remember(info.provider) { homeWidgetSizeOptions(info) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Color(0xFF111820),
        contentColor = Color(0xFFEAF2F8),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(
                "Resize widget",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFEAF2F8),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Choose a supported home-grid size for this widget.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB7C2CF),
            )
            Spacer(Modifier.height(12.dp))
            options.forEach { option ->
                val selected = currentConfig.cols == option.span.cols && currentConfig.rows == option.span.rows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Color(0xFF203746) else Color.Transparent)
                        .clickable { onSelect(option.span) }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeWidgetSpanPreview(option.span, selected)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            option.label,
                            color = if (selected) Color(0xFF9AE2FF) else Color(0xFFEAF2F8),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            "${option.span.cols} x ${option.span.rows}" +
                                if (option.recommended) " recommended" else "",
                            color = Color(0xFFB7C2CF),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF84D5F6))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeWidgetSpanPreview(
    span: HomeWidgetSpan,
    selected: Boolean,
) {
    val activeColor = if (selected) Color(0xFF84D5F6) else Color(0xFFD7E2EC)
    val inactiveColor = Color(0xFF26313C)
    Column(
        modifier = Modifier
            .size(width = 52.dp, height = 42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0C1218))
            .border(0.6.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(HOME_GRID_ROWS) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(HOME_GRID_COLS) { col ->
                    val active = row < span.rows && col < span.cols
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (active) activeColor else inactiveColor),
                    )
                }
            }
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
    onEditHomeLayout: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onNewHomeGroup: () -> Unit,
    onPinToHomeStrip: () -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                "Home",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = labelColor,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (hasWidget) "Long-press the widget itself to resize, replace, or remove it." else "Add widgets, groups, and shortcuts to your Zeno home.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9EADB8),
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
            HorizontalDivider(color = Color(0xFF2C3340))
            Spacer(Modifier.height(4.dp))

            @Composable
            fun MenuRow(icon: ImageVector, label: String, helper: String = "", danger: Boolean = false, onClick: () -> Unit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (danger) Color(0x221F0E12) else Color.Transparent)
                        .clickable(onClick = onClick)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (danger) Color(0x332B1217) else Color(0xFF1B2630)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (danger) Color(0xFFFF9EAA) else iconTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = if (danger) Color(0xFFFFCED5) else labelColor,
                        )
                        if (helper.isNotBlank()) {
                            Text(
                                helper,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E95A3),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            if (!hasWidget) {
                MenuRow(Icons.Rounded.AddAlarm, "Add system widget", "Pick a preview and place it on the grid", onClick = onAddWidget)
            } else {
                MenuRow(Icons.Rounded.GridView, "Edit home layout", "Rearrange apps and groups", onClick = onEditHomeLayout)
                MenuRow(Icons.Rounded.EventBusy, "Remove widget", "Delete it from the home grid", danger = true, onClick = onRemoveWidget)
            }
            if (newHomeGroupEnabled) {
                MenuRow(Icons.Rounded.Folder, "New group", "Create a compact home folder", onClick = onNewHomeGroup)
            }
            MenuRow(
                Icons.Rounded.Settings,
                LocalContext.current.getString(R.string.home_menu_settings_title),
                "Customize Zeno Classic",
                onClick = onOpenSettings,
            )
            MenuRow(Icons.Rounded.Tune, "System settings", "Open Android settings", onClick = onOpenSystemSettings)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun drawerSortModeLabel(mode: DrawerSortMode): String = when (mode) {
    DrawerSortMode.CLASSIC -> stringResource(R.string.drawer_sort_classic)
    DrawerSortMode.ALPHABETICAL -> stringResource(R.string.drawer_sort_alphabetical)
    DrawerSortMode.MOST_USED -> stringResource(R.string.drawer_sort_most_used)
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
    drawerSortMode: DrawerSortMode,
    showIconNotifBadge: Boolean,
    onSortModeSelected: (DrawerSortMode) -> Unit,
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
    val adaptiveLayout = rememberAdaptiveLayout()
    val outerPadH = 12.dp
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
    val drawerHoverSwitchThresholdPx = with(density) { 18.dp.toPx() }
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
            view.performHapticFeedback(textHandleMoveFeedback)
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
    val drawerSortHeaderHeight = 22.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            // imePadding: when a software keyboard appears (e.g., rename dialog, some ROMs showing
            // partial IME on physical-keyboard devices), the column shrinks from the bottom so the
            // dock and search bar are never hidden behind the keyboard.
            // On Zinwa Q25 (physical keyboard only) the IME height = 0 → this is a no-op.
            .imePadding()
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
                    Toast.makeText(drawerContext, drawerContext.getString(R.string.could_not_open_screen), Toast.LENGTH_SHORT).show()
                }
            }
            if (extras.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 132dp ≈ 3 extras rows at default scale; use adaptive % so it doesn't
                        // overflow on small BB-keyboard screens (500dp height → ~130dp safe).
                        .heightIn(max = (adaptiveLayout.screenHeightDp * 0.26f).toInt().dp)
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
                    text = stringResource(R.string.hidden_apps_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = privateSearchHintColor(themePalette),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                )
            }
            if (gridCells.isEmpty()) {
                Text(
                    text = stringResource(R.string.drawer_no_apps_found),
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
        // Sort menu — hidden while searching, but keep the same vertical slot so the grid stays put.
        if (searchQuery.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (reorderMode) {
                    // Arrange mode active — show Done button
                    Text(
                        text = "Done",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4FC3F7),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleReorder() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                } else {
                    val sortCycleOrder = listOf(
                        DrawerSortMode.ALPHABETICAL,
                        DrawerSortMode.CLASSIC,
                        DrawerSortMode.MOST_USED,
                    )
                    val cycleNext = {
                        val next = sortCycleOrder[(sortCycleOrder.indexOf(drawerSortMode) + 1) % sortCycleOrder.size]
                        onSortModeSelected(next)
                    }
                    IconButton(
                        onClick = { cycleNext() },
                        modifier = Modifier.size(36.dp).padding(4.dp),
                    ) {
                        val sortIcon = when (drawerSortMode) {
                            DrawerSortMode.ALPHABETICAL -> Icons.Outlined.SortByAlpha
                            DrawerSortMode.CLASSIC -> Icons.Outlined.GridView
                            DrawerSortMode.MOST_USED -> Icons.Outlined.QueryStats
                        }
                        Icon(
                            imageVector = sortIcon,
                            contentDescription = stringResource(R.string.cd_drawer_sort, drawerSortModeLabel(drawerSortMode)),
                            tint = if (drawerSortMode == DrawerSortMode.MOST_USED) {
                                themePalette.settingsMenuTitle
                            } else {
                                themePalette.settingsMenuBody.copy(alpha = 0.65f)
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
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
                val minLabelBlock = with(density) { (labelSizeSp * 2.3f).sp.toDp() }
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
                                    stiffness = Spring.StiffnessLow,
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
                                                        1.1f,
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessLow,
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
                                                val nearest = originalCellLayoutBoundsRef[0].entries
                                                    .filter { it.key != slot }
                                                    .minByOrNull { (_, bounds) ->
                                                        val c = bounds.center
                                                        hypot(
                                                            (c.x - dragFingerRoot.x).toDouble(),
                                                            (c.y - dragFingerRoot.y).toDouble(),
                                                        )
                                                    }?.key
                                                val current = hoveredSlotId
                                                if (nearest != current) {
                                                    fun hoverDistancePx(key: String?): Float? {
                                                        if (key == null) return null
                                                        val rect = originalCellLayoutBoundsRef[0][key] ?: return null
                                                        val center = rect.center
                                                        return hypot(
                                                            (center.x - dragFingerRoot.x).toDouble(),
                                                            (center.y - dragFingerRoot.y).toDouble(),
                                                        ).toFloat()
                                                    }
                                                    val nearestDist = hoverDistancePx(nearest)
                                                    val currentDist = hoverDistancePx(current)
                                                    if (
                                                        nearest != null &&
                                                        current != null &&
                                                        nearestDist != null &&
                                                        currentDist != null &&
                                                        nearestDist + drawerHoverSwitchThresholdPx >= currentDist
                                                    ) {
                                                        return@detectDragGesturesAfterLongPress
                                                    }
                                                }
                                                hoveredSlotId = nearest
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
                                                                    animationSpec = tween(110, easing = FastOutSlowInEasing),
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
                                                                    tween(110, easing = FastOutSlowInEasing),
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        ghostScaleAnim.animateTo(
                                                            0f,
                                                            tween(70, easing = FastOutSlowInEasing),
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
            val pagerBounds = pagerContainerCoords?.boundsInRoot()
            if (ghostVisible && dragCell != null && dims != null && pagerBounds != null) {
                val b = pagerBounds
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
                            alpha = ghostScaleAnim.value.coerceIn(0f, 1f)
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

private fun rankHomeSearchApps(
    query: String,
    allApps: List<AppEntry>,
    hiddenPackages: Set<String>,
): List<AppEntry> {
    if (query.isBlank()) return emptyList()
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return allApps
        .asSequence()
        .filter { it.packageName !in hiddenPackages }
        .mapNotNull { app ->
            val label = app.label.lowercase()
            val pkg = app.packageName.lowercase()
            val score = when {
                label == q -> 0
                label.startsWith(q) -> 1
                label.split(Regex("[\\s._-]+")).any { it.startsWith(q) } -> 2
                label.contains(q) -> 3
                pkg.contains(q) -> 4
                else -> null
            } ?: return@mapNotNull null
            Triple(score, app.label.lowercase(), app)
        }
        .sortedWith(compareBy<Triple<Int, String, AppEntry>> { it.first }.thenBy { it.second })
        .map { it.third }
        .toList()
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
                    Toast.makeText(context, context.getString(R.string.could_not_open_play_store), Toast.LENGTH_SHORT).show()
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
    // BB-keyboard punctuation — physical keys present on Q25 / Titan 2 / Titan 2 Elite.
    // These allow typed search without needing Alt/Symbol layer activation.
    Key.Period -> '.'
    Key.Comma -> ','
    Key.Apostrophe -> '\''
    Key.Minus -> '-'
    Key.Equals -> '+'
    Key.Slash -> '/'
    Key.At -> '@'
    Key.Pound -> '#'
    Key.Semicolon -> ';'
    Key.Backslash -> '\\'
    Key.LeftBracket -> '('
    Key.RightBracket -> ')'
    Key.Grave -> '`'
    else -> null
}

private fun doNavFeedback(view: android.view.View, hapticsEnabled: Boolean, intensity: Int = 3) {
    if (!hapticsEnabled) return
    doVibrate(view, intensity)
}

/**
 * Fires a haptic pulse unconditionally (ignores the Zeno hapticsEnabled pref).
 * Use for confirmations where feedback must always occur regardless of user preference —
 * e.g. double-tap to lock where the pulse IS the confirmation signal.
 *
 * Uses [android.os.VibrationEffect] directly (API 26+) so it is not cancelled by an
 * immediate screen-off and bypasses [android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED].
 * Falls back to [HapticFeedbackConstants.VIRTUAL_KEY] if amplitude control is unavailable.
 */
private fun doVibrate(view: android.view.View, intensity: Int = 3) {
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
    focusHorizontalInset: androidx.compose.ui.unit.Dp = 0.dp,
    contentVerticalInset: androidx.compose.ui.unit.Dp = 0.dp,
    labelContentAlignment: Alignment = Alignment.TopCenter,
    useOutlinedLabel: Boolean = true,
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
    val showLiftInPlace = isFingerDraggingThisTile && !hideSourceWhileFingerDragging
    val shouldWiggle = reorderMode && !showLiftInPlace && !isFingerDraggingThisTile
    val wiggleAngle = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(shouldWiggle, phaseFlipped) {
        if (!shouldWiggle) { wiggleAngle.floatValue = 0f; return@LaunchedEffect }
        // Sine-wave via withFrameNanos — works the same way iOS CAAnimation drives its layer.
        // period = 900ms per full swing; phase offset staggers alternate icons.
        val peak = 5f
        val periodNs = 900_000_000L
        val phaseOffset = if (phaseFlipped) PI else 0.0
        var startNs = -1L
        while (true) {
            withFrameNanos { nanos ->
                if (startNs < 0L) startNs = nanos
                val t = (nanos - startNs).toDouble() / periodNs.toDouble()
                wiggleAngle.floatValue = (peak * sin(2.0 * PI * t + phaseOffset)).toFloat()
            }
        }
    }
    val currentWiggle = wiggleAngle.floatValue
    val density = LocalDensity.current
    val iconPadTop = 4.dp
    val textPadBottom = 2.dp
    val contentHeight = (height - contentVerticalInset * 2).coerceAtLeast(iconSize)
    // Keep folder tile vertical geometry in lockstep with AppTile so label baseline matches.
    val (iconSizeUsed, iconPadBottom) = remember(contentHeight, iconSize, labelSizeSp, density) {
        val minLabel = with(density) { (labelSizeSp * 2.3f).sp.toDp() }
        var sz = iconSize
        var pad = (contentHeight - iconPadTop - sz - textPadBottom - minLabel).coerceIn(2.dp, 8.dp)
        repeat(10) {
            val labelSpace = contentHeight - iconPadTop - sz - pad - textPadBottom
            if (labelSpace >= minLabel) return@remember Pair(sz, pad)
            if (sz <= 40.dp) return@remember Pair(sz, pad)
            sz -= 3.dp
            pad = (contentHeight - iconPadTop - sz - textPadBottom - minLabel).coerceIn(2.dp, 8.dp)
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
            .rotate(currentWiggle)
            .graphicsLayer {
                if (showLiftInPlace) {
                    translationX = reorderDragOffset.x
                    translationY = reorderDragOffset.y
                    alpha = 0.92f
                    shadowElevation = 12f
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
        val folderTrackpadActive = LocalTrackpadActive.current
        val folderHighlightAlpha by animateFloatAsState(
            targetValue = if (selected && folderTrackpadActive) 1f else 0f,
            animationSpec = tween(durationMillis = if (selected && folderTrackpadActive) 120 else 500),
            label = "folderFocusHighlightAlpha",
        )
        if (folderHighlightAlpha > 0f) {
            Box(
                modifier = Modifier
                    .padding(horizontal = focusHorizontalInset)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(selRadius))
                    .background(themePalette.selectorBackgroundColour.copy(alpha = folderHighlightAlpha))
                    .border(
                        width = 1.dp,
                        color = themePalette.selectorBorderColour.copy(alpha = folderHighlightAlpha),
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
                    .padding(horizontal = focusHorizontalInset)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(selRadius))
                    .border(2.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(selRadius)),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = contentVerticalInset),
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
                            // sizeIn (not size) so circle grows with glyph at large font-scale
                            .sizeIn(minWidth = 16.dp, minHeight = 16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✱",
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
                contentAlignment = labelContentAlignment,
            ) {
                if (useOutlinedLabel) {
                    OutlinedLabel(
                        text = displayLabel,
                        fontSizeSp = labelSizeSp,
                        textColor = themePalette.appCardTextColour,
                        outlineColor = themePalette.appCardTextOutlineColour,
                        fontWeight = fontWeightFromName(themePalette.appCardFontWeightName),
                    )
                } else {
                    HomeStripItemLabel(displayLabel)
                }
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
    focusHorizontalInset: androidx.compose.ui.unit.Dp = 0.dp,
    contentVerticalInset: androidx.compose.ui.unit.Dp = 0.dp,
    labelContentAlignment: Alignment = Alignment.TopCenter,
    useOutlinedLabel: Boolean = true,
    /** When true, [reorderDragModifier] owns long-press (e.g. home strip); inner tap must not register long-press. */
    stripOuterDragOwnsLongPress: Boolean = false,
) {
    // Flutter `app_card.dart` uses the raw title; no artificial line breaks.
    val displayLabel = app.label
    val cardRadius = themePalette.appCardCornerRadiusDp.dp
    val selRadius = themePalette.selectorBorderRadiusDp.dp
    // Phase-stagger: half the tiles start at +peak, half at –peak, so they don't lockstep.
    val phaseFlipped = remember(app.packageName) { app.packageName.hashCode() and 1 != 0 }
    val showLiftInPlace = isFingerDraggingThisTile && !hideSourceWhileFingerDragging
    val shouldWiggle = reorderMode && !showLiftInPlace && !isFingerDraggingThisTile
    // Manual coroutine wiggle — rememberInfiniteTransition does not drive per-frame
    // recomposition inside LazyVerticalGrid items (lazy skip optimisation blocks it).
    val wiggleAngle = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(shouldWiggle, phaseFlipped) {
        if (!shouldWiggle) { wiggleAngle.floatValue = 0f; return@LaunchedEffect }
        // Sine-wave via withFrameNanos — works the same way iOS CAAnimation drives its layer.
        // period = 900ms per full swing; phase offset staggers alternate icons.
        val peak = 5f
        val periodNs = 900_000_000L
        val phaseOffset = if (phaseFlipped) PI else 0.0
        var startNs = -1L
        while (true) {
            withFrameNanos { nanos ->
                if (startNs < 0L) startNs = nanos
                val t = (nanos - startNs).toDouble() / periodNs.toDouble()
                wiggleAngle.floatValue = (peak * sin(2.0 * PI * t + phaseOffset)).toFloat()
            }
        }
    }
    val currentWiggle = wiggleAngle.floatValue   // read in composition → triggers recompose each frame
    Box(
        modifier = Modifier
            .zIndex(if (isFingerDraggingThisTile) 1f else 0f)
            .onGloballyPositioned(onGloballyPositioned)
            .size(width, height)
            .rotate(currentWiggle)
            .graphicsLayer {
                if (showLiftInPlace) {
                    translationX = reorderDragOffset.x
                    translationY = reorderDragOffset.y
                    alpha = 0.92f
                    shadowElevation = 12f
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
        val iconPadTop = 4.dp
        val textPadBottom = 2.dp
        val contentHeight = (height - contentVerticalInset * 2).coerceAtLeast(iconSize)
        val (iconSizeUsed, iconPadBottom) = remember(contentHeight, iconSize, labelSizeSp, density) {
            val minLabel = with(density) { (labelSizeSp * 2.3f).sp.toDp() }
            var sz = iconSize
            var pad = (contentHeight - iconPadTop - sz - textPadBottom - minLabel).coerceIn(2.dp, 8.dp)
            repeat(10) {
                val labelSpace = contentHeight - iconPadTop - sz - pad - textPadBottom
                if (labelSpace >= minLabel) return@remember Pair(sz, pad)
                if (sz <= 40.dp) return@remember Pair(sz, pad)
                sz -= 3.dp
                pad = (contentHeight - iconPadTop - sz - textPadBottom - minLabel).coerceIn(2.dp, 8.dp)
            }
            Pair(sz.coerceAtLeast(40.dp), pad)
        }

        val trackpadActive = LocalTrackpadActive.current
        val highlightAlpha by animateFloatAsState(
            targetValue = if (selected && trackpadActive) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (selected && trackpadActive) 120 else 500,
                easing = FastOutSlowInEasing,
            ),
            label = "focusHighlightAlpha",
        )
        if (highlightAlpha > 0f) {
            Box(
                modifier = Modifier
                    .padding(horizontal = focusHorizontalInset)
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(selRadius))
                    .background(themePalette.selectorBackgroundColour.copy(alpha = highlightAlpha))
                    .border(
                        width = 1.dp,
                        color = themePalette.selectorBorderColour.copy(alpha = highlightAlpha),
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
                    .padding(horizontal = focusHorizontalInset)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = contentVerticalInset),
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
                            // sizeIn (not size) so circle grows with glyph at large font-scale
                            .sizeIn(minWidth = 16.dp, minHeight = 16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✱",
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
                contentAlignment = labelContentAlignment,
            ) {
                if (useOutlinedLabel) {
                    OutlinedLabel(
                        text = displayLabel,
                        fontSizeSp = labelSizeSp,
                        textColor = themePalette.appCardTextColour,
                        outlineColor = themePalette.appCardTextOutlineColour,
                        fontWeight = fontWeightFromName(themePalette.appCardFontWeightName),
                    )
                } else {
                    HomeStripItemLabel(displayLabel)
                }
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
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val lineHeightStyle = remember {
        LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = LineHeightStyle.Trim.None,
    )
    }
    val base = remember(fontSizeSp, textColor, fontWeight, lineHeightStyle) {
        compactAppLabelStyle(
            fontSizeSp = fontSizeSp,
            textColor = textColor,
            fontWeight = fontWeight,
            lineHeightStyle = lineHeightStyle,
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

    // wrapContentHeight so the label sits tight below the icon rather than
    // floating in a large gap when cells are tall (e.g. 3-row grid).
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        for (style in outlineShadowStyles) {
            Text(
                text,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                softWrap = true,
                style = style,
            )
        }
        Text(
            text,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
    showAddButton: Boolean,
    onAddApp: () -> Unit,
    hapticsEnabled: Boolean,
    hapticIntensity: Int,
    appIconShape: AppIconShape,
    themePalette: LauncherThemePalette,
    unreadPackages: Set<String> = emptySet(),
    renameDialogTitle: String = "Rename group",
    emptyStateMessage: String = "No apps yet — long-press an app in the drawer to add it here.",
) {
    val view = LocalView.current
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
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val gridColumns = 4
    val gridItemCount = members.size + if (showAddButton && members.isNotEmpty()) 1 else 0
    val gridRows = ((gridItemCount - 1).coerceAtLeast(0) / gridColumns) + 1
    val tileIconSize = themePalette.appGridIconSizeDp.dp
    val tileVerticalPadding = 6.dp
    val iconLabelGap = 5.dp
    val gridVerticalGap = 10.dp
    // Compute row height from actual tile content — adapts to theme icon size and font scale.
    // label: 2 lines at HOME_STRIP_LABEL_LINE_SP (13sp) ≈ 28dp at 100% scale; scale up at large font.
    val fontScale = LocalConfiguration.current.fontScale
    val labelEstimateDp = (28f * fontScale.coerceAtLeast(1f)).dp
    val tileRowHeight = tileVerticalPadding * 2 + tileIconSize + iconLabelGap + labelEstimateDp
    // Max height from AdaptiveLayout (55% of screen). Old fixed 360dp ceiling wasted tablet space.
    val maxGridHeight = rememberAdaptiveLayout().folderOverlayMaxHeightDp
    val gridHeight = (tileRowHeight * gridRows + gridVerticalGap * (gridRows - 1))
        .coerceAtMost(maxGridHeight)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = Color(0xFF1A1F28),
        contentColor = HOME_STRIP_LABEL_COLOR,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF6C717C).copy(alpha = 0.72f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
        ) {
            // Title row: tap group/folder name to rename.
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
                        fontSize = 16.sp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { renameOpen = true }
                        .padding(vertical = 1.dp),
                )
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
                if (showAddButton) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x3B5B9BD5), RoundedCornerShape(12.dp))
                                .clickable { onAddApp() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.action_add_app),
                                tint = Color(0xFF91B3DA),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Add app",
                                color = Color(0xFFA9BCD3),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                        .focusRequester(gridFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { ev ->
                            if (ev.type != KeyEventType.KeyDown || members.isEmpty()) return@onPreviewKeyEvent false
                            val cols = gridColumns
                            val rows = ((members.size - 1) / cols) + 1
                            val row = focusedIndex / cols
                            val col = focusedIndex % cols
                            when (ev.key) {
                                Key.DirectionLeft -> {
                                    val next = (focusedIndex - 1).coerceAtLeast(0)
                                    if (next != focusedIndex) {
                                        focusedIndex = next
                                        doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    val next = (focusedIndex + 1).coerceAtMost(members.lastIndex)
                                    if (next != focusedIndex) {
                                        focusedIndex = next
                                        doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    val next = (focusedIndex - cols).coerceAtLeast(0)
                                    if (next != focusedIndex) {
                                        focusedIndex = next
                                        doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    }
                                    true
                                }
                                Key.DirectionDown -> {
                                    val targetRow = (row + 1).coerceAtMost(rows - 1)
                                    val next = (targetRow * cols + col).coerceAtMost(members.lastIndex)
                                    if (next != focusedIndex) {
                                        focusedIndex = next
                                        doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    }
                                    true
                                }
                                Key.Enter, Key.NumPadEnter -> {
                                    doNavFeedback(view, hapticsEnabled, hapticIntensity)
                                    members.getOrNull(focusedIndex)?.let { onLaunchApp(it.packageName) }
                                    true
                                }
                                else -> false
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(gridVerticalGap),
                    contentPadding = PaddingValues(vertical = 2.dp),
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
                                .background(if (isFocused) Color(0x3385BFFF) else Color.Transparent)
                                .padding(top = tileVerticalPadding, start = 4.dp, end = 4.dp, bottom = tileVerticalPadding)
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
                            AppIconWithBadge(hasUnread = app.packageName in unreadPackages) {
                                AsyncImage(
                                    model = app.icon,
                                    contentDescription = app.label,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(tileIconSize)
                                        .clip(iconMaskShape(appIconShape)),
                                )
                            }
                            Spacer(Modifier.height(iconLabelGap))
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
                    if (showAddButton) {
                        item(key = "add_app_tile") {
                            val cardRadius = themePalette.appCardCornerRadiusDp.dp
                            val cardShape = RoundedCornerShape(cardRadius)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(Color(0x1E1E2430))
                                    .border(0.8.dp, Color(0x3B5B9BD5), cardShape)
                                    .clickable { onAddApp() }
                                    .padding(top = tileVerticalPadding, start = 4.dp, end = 4.dp, bottom = tileVerticalPadding),
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    // Keep the previous tile height footprint while centering the "+" box exactly.
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Spacer(Modifier.height(tileIconSize))
                                        Spacer(Modifier.height(iconLabelGap))
                                        Text(
                                            text = "Add app",
                                            color = Color.Transparent,
                                            fontSize = HOME_STRIP_LABEL_FONT_SP,
                                            lineHeight = HOME_STRIP_LABEL_LINE_SP,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(tileIconSize)
                                            .clip(RoundedCornerShape(13.dp))
                                            .border(1.dp, Color(0x595B9BD5), RoundedCornerShape(13.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = stringResource(R.string.action_add_app),
                                            tint = Color(0xFF91B3DA),
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
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
                    label = { Text(stringResource(R.string.dialog_name_hint), color = themePalette.settingsMenuBody) },
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
                ) { Text(stringResource(R.string.action_save), color = themePalette.settingsMenuBody) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) {
                    Text(stringResource(R.string.action_cancel), color = themePalette.settingsMenuBody)
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
    showPinToHomeStrip: Boolean,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onOpenGroup: () -> Unit,
    onRenameGroup: (String) -> Unit,
    onMoveSide: () -> Unit,
    onPinToHomeStrip: () -> Unit,
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
            if (showPinToHomeStrip) {
                MenuRow(Icons.Rounded.BookmarkAdd, stringResource(R.string.action_pin_to_home_strip), onClick = onPinToHomeStrip)
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
            title = { Text(stringResource(R.string.dialog_rename_group_title), color = themePalette.settingsMenuTitle) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = capitalizeFirstLetterForGroupInput(it) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.dialog_name_hint), color = themePalette.settingsMenuBody) },
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
                    Text(stringResource(R.string.action_cancel), color = themePalette.settingsMenuBody)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderDrawerContextMenu(
    folder: DrawerGridCell.Folder,
    showPinToHomeStrip: Boolean,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onOpenFolder: () -> Unit,
    onRenameFolder: (String) -> Unit,
    onReorderApps: () -> Unit,
    onPinToHomeStrip: () -> Unit,
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
            MenuRow(Icons.Rounded.SwapVert, stringResource(R.string.action_arrange), onClick = onReorderApps)
            if (showPinToHomeStrip) {
                MenuRow(Icons.Rounded.BookmarkAdd, stringResource(R.string.action_pin_to_home_strip), onClick = onPinToHomeStrip)
            }
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
            title = { Text(stringResource(R.string.dialog_rename_folder_title), color = themePalette.settingsMenuTitle) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = capitalizeFirstLetterForGroupInput(it) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.dialog_name_hint), color = themePalette.settingsMenuBody) },
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
                ) { Text(stringResource(R.string.action_save), color = themePalette.settingsMenuBody) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) {
                    Text(stringResource(R.string.action_cancel), color = themePalette.settingsMenuBody)
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
    hasCustomIcon: Boolean,
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
    onChangeIcon: () -> Unit,
    onResetIcon: () -> Unit,
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
    val maxMenuBodyHeight = rememberAdaptiveLayout().settingsMenuMaxHeightDp
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
                MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.action_open), onLaunch)
                MenuRow(Icons.Rounded.Info, stringResource(R.string.action_app_info), onInfo)
                MenuRow(
                    if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    if (isHidden) stringResource(R.string.action_unhide) else stringResource(R.string.action_hide),
                    onHideToggle,
                )
                MenuRow(Icons.Rounded.SwapVert, stringResource(R.string.action_arrange), onReorder)
                if (!app.internal) {
                    MenuRow(Icons.Rounded.Image, stringResource(R.string.action_change_icon), onChangeIcon)
                    if (hasCustomIcon) {
                        MenuRow(Icons.Rounded.SettingsBackupRestore, stringResource(R.string.action_reset_icon), onResetIcon)
                    }
                }
                if (drawerFolderActionsEnabled) {
                    MenuRow(
                        Icons.Rounded.Folder,
                        stringResource(R.string.action_new_group),
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
                                stringResource(R.string.action_remove_from, g.title),
                                { onRemoveFromHomeGroup(g.id) },
                            )
                        } else {
                            MenuRow(
                                Icons.Rounded.BookmarkAdd,
                                stringResource(R.string.action_add_to, g.title),
                                { onAddToHomeGroup(g.id) },
                            )
                        }
                    }
                }
                if (drawerFolderActionsEnabled) {
                    for ((folderId, label) in drawerFolders) {
                        MenuRow(
                            Icons.Rounded.BookmarkAdd,
                            stringResource(R.string.action_add_to, label),
                            onClick = {
                                onAddToDrawerFolder(folderId)
                                onDismiss()
                            },
                        )
                    }
                }
                if (addHomeShortcutEnabled) {
                    MenuRow(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.action_pin_to_home_strip), onAddHomeShortcut)
                }
                if (removeHomeShortcutEnabled) {
                    MenuRow(Icons.Outlined.Close, stringResource(R.string.action_remove_from_home_strip), onRemoveHomeShortcut)
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
            title = { Text(stringResource(R.string.action_new_group), color = themePalette.settingsMenuTitle) },
            text = {
                OutlinedTextField(
                    value = newDrawerFolderName,
                    onValueChange = { newDrawerFolderName = capitalizeFirstLetterForGroupInput(it) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.dialog_group_name_hint), color = themePalette.settingsMenuBody) },
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
                ) { Text(stringResource(R.string.action_create), color = themePalette.settingsMenuBody) }
            },
            dismissButton = {
                TextButton(onClick = { newDrawerFolderDialogOpen = false }) {
                    Text(stringResource(R.string.action_cancel), color = themePalette.settingsMenuBody)
                }
            },
        )
    }
}

@Composable
private fun HomeStripItemLabel(text: String) {
    Text(
        text = text,
        style = compactAppLabelStyle(
            fontSizeSp = HOME_STRIP_LABEL_FONT_SP.value.toInt(),
            textColor = HOME_STRIP_LABEL_COLOR,
        ).copy(lineHeight = HOME_STRIP_LABEL_LINE_SP),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        softWrap = true,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .widthIn(max = 88.dp)
            .padding(top = 1.dp),
    )
}

private fun fontWeightFromName(name: String): FontWeight = when (name) {
    "Light"    -> FontWeight.Light
    "Medium"   -> FontWeight.Medium
    "SemiBold" -> FontWeight.SemiBold
    "Bold"     -> FontWeight.Bold
    else       -> FontWeight.Normal
}

private fun compactAppLabelStyle(
    fontSizeSp: Int,
    textColor: Color,
    fontWeight: FontWeight = FontWeight.Normal,
    lineHeightStyle: LineHeightStyle? = null,
): TextStyle =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = fontSizeSp.sp,
        fontWeight = fontWeight,
        color = textColor,
        lineHeight = (fontSizeSp + 1).sp,
        letterSpacing = (-0.18).sp,
        lineHeightStyle = lineHeightStyle,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

/**
 * Home group tile in [HomeShortcutStrip]: exact same outer 52dp × 12dp-rounded bounds as pinned app tiles (full-bleed folder panel).
 */
@Composable
private fun HomeGroupStripIcon(
    title: String,
    members: List<AppEntry>,
    appIconShape: AppIconShape,
    hasUnreadBadge: Boolean,
    tileSize: Dp = HOME_SHORTCUT_ICON_DP,
    gesturesEnabled: Boolean = true,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val tile = tileSize
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
                    // sizeIn (not size) so circle grows with glyph at large font-scale
                    .sizeIn(minWidth = 14.dp, minHeight = 14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD32F2F)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✱",
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
    onStripReorderTap: (String) -> Unit,
    onStripBoundsChanged: (Rect?) -> Unit = {},
    onRemoveDropVisibleChanged: (Boolean) -> Unit = {},
    onRemoveDropActiveChanged: (Boolean) -> Unit = {},
    onRemoveDropBoundsChanged: (Rect?) -> Unit = {},
    removeDropBounds: Rect? = null,
    onClearStripKeyboardFocus: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    val outerPadH = 22.dp
    val stripColSpacing = HOME_STRIP_SHORTCUT_GAP
    val iconSize = homeStripIconSize(themePalette.appGridIconSizeDp)
    val labelSizeSp = HOME_STRIP_LABEL_FONT_SP.value.roundToInt()
    val selectorRadius = HOME_STRIP_FOCUS_RADIUS

    val baseSlotTokens = remember(stripSlotTokens) {
        List(STRIP_TOTAL_SLOTS) { i -> stripSlotTokens.getOrNull(i) }
    }
    var reorderStripDragOffset by remember { mutableStateOf(Offset.Zero) }

    val cellLayouts = remember { mutableMapOf<String, LayoutCoordinates>() }
    val originalStripBoundsRef = remember { mutableListOf(emptyMap<String, Rect>()) }
    val stripHoverKeysRef = remember { mutableListOf(emptySet<String>()) }
    var hoveredSlotKey by remember { mutableStateOf<String?>(null) }
    var reorderFingerDragging by remember { mutableStateOf(false) }
    var dragFingerRoot by remember { mutableStateOf(Offset.Zero) }
    var removeBandVisible by remember { mutableStateOf(false) }
    var removeZoneActive by remember { mutableStateOf(false) }
    var ghostVisible by remember { mutableStateOf(false) }
    val ghostScaleAnim = remember { Animatable(1f) }
    var stripCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pendingDropMoving by remember { mutableStateOf<String?>(null) }
    var pendingDropTarget by remember { mutableStateOf<String?>(null) }
    var pendingDropSlots by remember { mutableStateOf<List<String?>?>(null) }
    val removeDropKey = HOME_STRIP_REMOVE_DROP_KEY

    fun previewHomeStripSlots(
        source: List<String?>,
        moving: String?,
        hovered: String?,
    ): List<String?> {
        if (moving == null || hovered == null) return source
        val fromI = source.indexOfFirst { it == moving }
        val toI = when {
            hovered.startsWith("strip_empty_") -> {
                hovered.removePrefix("strip_empty_").toIntOrNull()?.minus(1) ?: -1
            }
            else -> source.indexOfFirst { it == hovered }
        }
        if (fromI < 0 || toI < 0 || fromI == toI) return source
        return source.toMutableList().apply { moveHomeStripSlot(fromI, toI) }
    }
    // Mirror drawer behavior: emit a light tick only when hover target changes while actively dragging.
    androidx.compose.runtime.LaunchedEffect(hoveredSlotKey) {
        if (hoveredSlotKey == null || !reorderFingerDragging) return@LaunchedEffect
        if (hoveredSlotKey == removeDropKey) {
            logHomeStripDnD { "hover removeZone slot=$hoveredSlotKey dragging=$reorderFingerDragging" }
        } else if (hapticsEnabled) {
            view.performHapticFeedback(textHandleMoveFeedback)
        }
    }

    val activeMoving = movingSlotId ?: pendingDropMoving
    val activeHovered = hoveredSlotKey ?: pendingDropTarget
    val displaySlotTokens: List<String?> =
        pendingDropSlots ?: previewHomeStripSlots(baseSlotTokens, activeMoving, activeHovered)

    androidx.compose.runtime.LaunchedEffect(
        movingSlotId,
        baseSlotTokens,
        pendingDropSlots,
    ) {
        val pending = pendingDropSlots
        if (movingSlotId == null && pending != null) {
            if (baseSlotTokens == pending) {
                pendingDropMoving = null
                pendingDropTarget = null
                pendingDropSlots = null
            } else {
                kotlinx.coroutines.delay(250)
                if (pendingDropSlots == pending) {
                    pendingDropMoving = null
                    pendingDropTarget = null
                    pendingDropSlots = null
                }
            }
        }
    }

    val stripHoverKeys: Set<String> = remember(displaySlotTokens) {
        buildSet {
            if (reorderMode) add(removeDropKey)
            displaySlotTokens.forEachIndexed { i, tok ->
                tok?.let { add(it) }
                add("strip_empty_${i + 1}")
            }
        }
    }
    SideEffect { stripHoverKeysRef[0] = stripHoverKeys }

    val wiggle = rememberInfiniteTransition(label = "homeStripWiggle")
    val wiggleRot by wiggle.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(200, easing = LinearEasing), RepeatMode.Reverse),
        label = "homeStripWiggleRot",
    )
    val wiggleStr by animateFloatAsState(
        targetValue = if (ghostVisible) 1f else if (reorderMode) 0.7f else 0f,
        animationSpec = tween(180),
        label = "homeStripWiggleStr",
    )
    val reorderModeRef = rememberUpdatedState(reorderMode)

    fun Modifier.stripDragGesture(slotId: String): Modifier = this.pointerInput(slotId) {
        val hoverSwitchThresholdPx = if (HomeGroupIds.isHomeGroupId(slotId)) {
            with(density) { 20.dp.toPx() }
        } else {
            with(density) { 10.dp.toPx() }
        }
        fun attachedBoundsSnapshot(): Map<String, Rect> =
            cellLayouts.entries
                .filter { it.value.isAttached }
                .associate { (k, v) -> k to v.boundsInRoot() }
                .toMutableMap()
                .also { bounds ->
                    // Header remove target lives outside the strip row but still acts as a drop zone.
                    // We keep it in the same hover map so drag/drop behavior stays uniform.
                    removeDropBounds?.let { bounds[removeDropKey] = it }
                }

        fun nearestStripHover(source: String, finger: Offset, bounds: Map<String, Rect>): String? =
            bounds.entries
                .asSequence()
                .filter { (k, _) -> k != source && k in stripHoverKeysRef[0] }
                .let { candidates ->
                    val candidateList = candidates.toList()
                    val underFinger = candidateList.filter { (_, b) ->
                        finger.x >= b.left && finger.x <= b.right &&
                            finger.y >= b.top && finger.y <= b.bottom
                    }
                    val pool = if (underFinger.isNotEmpty()) underFinger else candidateList
                    pool.minByOrNull { (_, b) ->
                        val c = b.center
                        hypot((c.x - finger.x).toDouble(), (c.y - finger.y).toDouble())
                    }?.key
                }
        fun hoverDistancePx(key: String?, finger: Offset, bounds: Map<String, Rect>): Float? {
            if (key == null) return null
            val rect = bounds[key] ?: return null
            val c = rect.center
            return hypot((c.x - finger.x).toDouble(), (c.y - finger.y).toDouble()).toFloat()
        }

        val removeZoneBottomSlackPx = with(density) { 8.dp.toPx() }
        val removeZoneTopFallbackPx = with(density) { 116.dp.toPx() }
        val removeBandRevealGapPx = with(density) { 72.dp.toPx() }
        val removeBandHideGapPx = with(density) { 40.dp.toPx() }

        fun shouldShowRemoveBand(finger: Offset): Boolean {
            val stripTop = stripCoords?.takeIf { it.isAttached }?.boundsInRoot()?.top
                ?: originalStripBoundsRef[0].values.minOfOrNull { it.top }
                ?: return false
            val revealThreshold = stripTop - removeBandRevealGapPx
            val hideThreshold = stripTop - removeBandHideGapPx
            return if (removeBandVisible) finger.y <= hideThreshold else finger.y <= revealThreshold
        }

        fun removeZoneMatch(finger: Offset): Pair<Boolean, String> {
            val bounds = removeDropBounds
            if (bounds != null) {
                if (bounds.contains(finger)) return true to "bounds"
                if (
                    finger.x >= bounds.left &&
                    finger.x <= bounds.right &&
                    finger.y >= bounds.top &&
                    finger.y <= bounds.bottom + removeZoneBottomSlackPx
                ) {
                    return true to "bottomSlack"
                }
            }
            if (finger.y <= removeZoneTopFallbackPx) return true to "topFallback"
            return false to "none"
        }

        fun isInsideRemoveZone(finger: Offset): Boolean = removeZoneMatch(finger).first

        fun beginDrag(startOffset: Offset) {
            onClearStripKeyboardFocus()
            if (!reorderModeRef.value) onEnterReorderMode()
            reorderStripDragOffset = Offset.Zero
            onStartStripMove(slotId)
            if (hapticsEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            hoveredSlotKey = null
            removeBandVisible = false
            onRemoveDropVisibleChanged(false)
            removeZoneActive = false
            onRemoveDropActiveChanged(false)
            reorderFingerDragging = true
            ghostVisible = true
            // Empty drop slots compose only while dragging, so refresh bounds on the first move.
            originalStripBoundsRef[0] = emptyMap()
            val src = cellLayouts[slotId]
            if (src != null && src.isAttached) dragFingerRoot = src.localToRoot(startOffset)
            logHomeStripDnD {
                "beginDrag slot=$slotId reorderMode=${reorderModeRef.value} " +
                    "hoverKeys=${stripHoverKeysRef[0].size} bounds=${originalStripBoundsRef[0].size}"
            }
            scope.launch {
                ghostScaleAnim.animateTo(
                    1.1f,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                )
            }
        }

        fun updateDrag(change: PointerInputChange, dragAmount: Offset) {
            reorderStripDragOffset += dragAmount
            change.consume()
            val src = cellLayouts[slotId]
            dragFingerRoot = if (src != null && src.isAttached) {
                src.localToRoot(change.position)
            } else {
                Offset(dragFingerRoot.x + dragAmount.x, dragFingerRoot.y + dragAmount.y)
            }
            if (
                originalStripBoundsRef[0].isEmpty() ||
                    !originalStripBoundsRef[0].keys.containsAll(stripHoverKeysRef[0])
            ) {
                val snap = attachedBoundsSnapshot()
                if (snap.isNotEmpty()) originalStripBoundsRef[0] = snap
            }
            val showRemoveBand = shouldShowRemoveBand(dragFingerRoot)
            if (showRemoveBand != removeBandVisible) {
                removeBandVisible = showRemoveBand
                onRemoveDropVisibleChanged(showRemoveBand)
                logHomeStripDnD {
                    "removeBandVisible slot=$slotId visible=$showRemoveBand finger=(${dragFingerRoot.x},${dragFingerRoot.y})"
                }
            }
            val (inRemoveZone, removeZoneReason) = removeZoneMatch(dragFingerRoot)
            if (inRemoveZone && !removeZoneActive) {
                val boundsText = removeDropBounds?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "null"
                logHomeStripDnD {
                    "enterRemoveZone slot=$slotId finger=(${dragFingerRoot.x},${dragFingerRoot.y}) bounds=$boundsText reason=$removeZoneReason"
                }
                removeZoneActive = true
                onRemoveDropActiveChanged(true)
            } else if (!inRemoveZone && removeZoneActive) {
                logHomeStripDnD {
                    "exitRemoveZone slot=$slotId finger=(${dragFingerRoot.x},${dragFingerRoot.y})"
                }
                removeZoneActive = false
                onRemoveDropActiveChanged(false)
            }
            if (inRemoveZone) {
                if (hoveredSlotKey != removeDropKey) {
                    logHomeStripDnD {
                        "hover slot=$slotId from=$hoveredSlotKey to=$removeDropKey finger=(${dragFingerRoot.x},${dragFingerRoot.y}) reason=$removeZoneReason"
                    }
                    hoveredSlotKey = removeDropKey
                }
                return
            }
            val nearest = nearestStripHover(slotId, dragFingerRoot, originalStripBoundsRef[0])
            if (nearest != hoveredSlotKey) {
                val current = hoveredSlotKey
                val nearestDist = hoverDistancePx(nearest, dragFingerRoot, originalStripBoundsRef[0])
                val currentDist = hoverDistancePx(current, dragFingerRoot, originalStripBoundsRef[0])
                if (nearest != null && current != null && nearestDist != null && currentDist != null) {
                    // Hysteresis avoids rapid bounce between neighboring slots (most visible on groups).
                    if (nearestDist + hoverSwitchThresholdPx >= currentDist) return
                }
                logHomeStripDnD {
                    "hover slot=$slotId from=$hoveredSlotKey to=$nearest finger=(${dragFingerRoot.x},${dragFingerRoot.y})"
                }
                hoveredSlotKey = nearest
            }
        }

        fun finishDrag() {
            reorderFingerDragging = false
            removeBandVisible = false
            onRemoveDropVisibleChanged(false)
            removeZoneActive = false
            onRemoveDropActiveChanged(false)
            // Match drawer behavior: only drop when we had an explicit hover target during drag.
            val (inRemoveZone, removeZoneReason) = removeZoneMatch(dragFingerRoot)
            val hoverKey = hoveredSlotKey ?: if (inRemoveZone) removeDropKey else null
            logHomeStripDnD {
                "finishDrag slot=$slotId hoverKey=$hoverKey pendingBefore=$pendingDropMoving/$pendingDropTarget removeReason=$removeZoneReason"
            }
            if (hoverKey != null) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            pendingDropMoving = slotId
            pendingDropTarget = hoverKey
            pendingDropSlots = previewHomeStripSlots(baseSlotTokens, slotId, hoverKey)
            hoveredSlotKey = null
            scope.launch {
                logHomeStripDnD { "commitDrop slot=$slotId hoverKey=$hoverKey" }
                onFinishReorderDrop(hoverKey)
                // Let the persisted order compose under the lifted ghost before release fade.
                kotlinx.coroutines.delay(if (hoverKey != null) 45 else 0)
                ghostScaleAnim.animateTo(
                    0f,
                    tween(
                        durationMillis = if (hoverKey != null) 75 else 65,
                        easing = FastOutSlowInEasing,
                    ),
                )
                ghostScaleAnim.snapTo(1f)
                ghostVisible = false
                reorderStripDragOffset = Offset.Zero
            }
        }

        fun cancelDrag() {
            logHomeStripDnD { "cancelDrag slot=$slotId" }
            reorderFingerDragging = false
            removeBandVisible = false
            onRemoveDropVisibleChanged(false)
            removeZoneActive = false
            onRemoveDropActiveChanged(false)
            hoveredSlotKey = null
            pendingDropMoving = null
            pendingDropTarget = null
            pendingDropSlots = null
            reorderStripDragOffset = Offset.Zero
            scope.launch { ghostScaleAnim.snapTo(1f) }
            ghostVisible = false
            onClearMove()
        }

        // Match drawer reorder UX: long-press pickup while in reorder mode.
        detectDragGesturesAfterLongPress(
            onDragStart = { beginDrag(it) },
            onDrag = { change, dragAmount -> updateDrag(change, dragAmount) },
            onDragEnd = { finishDrag() },
            onDragCancel = { cancelDrag() },
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Drag apps, groups, or widgets to arrange",
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
            .padding(start = outerPadH, end = outerPadH, top = 1.dp, bottom = 0.dp)
            .onGloballyPositioned {
                stripCoords = it
                onStripBoundsChanged(it.boundsInRoot())
            },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            val viewportWidth = maxWidth
            val minLabelBlock = with(density) { (labelSizeSp * 2.3f).sp.toDp() }
            val minCellHeight = 3.dp + iconSize + HOME_STRIP_ICON_LABEL_GAP + 2.dp + minLabelBlock
            val minCellWidth = iconSize + 10.dp
            // AdaptiveLayout: capped at STRIP_TOTAL_SLOTS (5) but safe for very narrow screens.
            val stripCols = rememberAdaptiveLayout().maxStripCols
            val cardW =
                ((viewportWidth - stripColSpacing * (stripCols - 1)) / stripCols).coerceAtLeast(minCellWidth)
            val cardH = minCellHeight
            val ghostDims = Triple(cardW, cardH, iconSize)

            fun resolveVisibleStripToken(rawToken: String?): String? = when (val t = rawToken) {
                null -> null
                else -> when {
                    HomeGroupIds.isHomeGroupId(t) -> if (showHomeGroups) t else null
                    else -> if (showShortcutApps) t else null
                }
            }
            val showAllStripSlots = ghostVisible || movingSlotId != null
            val renderedStripSlots: List<Pair<Int, String?>> = if (showAllStripSlots) {
                List(STRIP_TOTAL_SLOTS) { idx -> idx to resolveVisibleStripToken(displaySlotTokens.getOrNull(idx)) }
            } else {
                displaySlotTokens.mapIndexedNotNull { idx, raw ->
                    resolveVisibleStripToken(raw)?.let { idx to it }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    stripColSpacing,
                    if (showAllStripSlots) Alignment.Start else Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.Bottom,
            ) {
                for ((col, visibleTok) in renderedStripSlots) {
                    val emptyKey = "strip_empty_${col + 1}"
                    val slotComposeKey = visibleTok ?: emptyKey
                    androidx.compose.runtime.key(slotComposeKey) {
                        val selected = focusedIndex == col
                        val group = visibleTok?.takeIf { HomeGroupIds.isHomeGroupId(it) }
                            ?.let { tid -> homeGroups.find { g -> g.id == tid } }
                        if (visibleTok != null && group != null) {
                        val stripToken = group.id
                        val isMovingThisGroup = ghostVisible && movingSlotId == stripToken
                        val members =
                            group.packageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                        val reorderDragModifier = Modifier.stripDragGesture(stripToken)
                        Box(
                            modifier = Modifier
                                .width(cardW)
                                .height(cardH)
                                .onGloballyPositioned { coords -> cellLayouts[stripToken] = coords }
                                .graphicsLayer {
                                    if (!isMovingThisGroup) {
                                        rotationZ = wiggleRot * wiggleStr
                                    }
                                    // While dragging, draw only the shared ghost overlay (same as apps).
                                    alpha = if (isMovingThisGroup) 0f else 1f
                                },
                        ) {
                            FolderTile(
                                displayLabel = group.title,
                                members = members,
                                appIconShape = appIconShape,
                                reorderMode = reorderMode,
                                selected = movingSlotId == stripToken || selected,
                                hasUnreadBadge = group.packageNames.any { it in unreadPackages },
                                width = cardW,
                                height = cardH,
                                iconSize = iconSize,
                                labelSizeSp = labelSizeSp,
                                cardTop = themePalette.appCardTop,
                                cardBottom = themePalette.appCardBottom,
                                showAppCardBackground = showAppCardBackground,
                                themePalette = themePalette,
                                reorderDragModifier = reorderDragModifier,
                                isFingerDraggingThisTile = isMovingThisGroup,
                                reorderDragOffset = if (isMovingThisGroup) reorderStripDragOffset else Offset.Zero,
                                hideSourceWhileFingerDragging = isMovingThisGroup,
                                focusHorizontalInset = HOME_STRIP_FOCUS_INSET,
                                contentVerticalInset = HOME_STRIP_CONTENT_VERTICAL_INSET,
                                labelContentAlignment = Alignment.BottomCenter,
                                useOutlinedLabel = false,
                                onGloballyPositioned = { coords -> cellLayouts[stripToken] = coords },
                                onClick = {
                                    if (reorderMode) onStripReorderTap(stripToken)
                                    else onOpenHomeGroup(group)
                                },
                                onLongPress = { },
                                stripOuterDragOwnsLongPress = true,
                            )
                        }
                        } else if (visibleTok != null) {
                        val stripToken = visibleTok
                        val cell = drawerCellForHomeStripToken(stripToken, gridCells, allApps)
                        if (cell == null) {
                            Box(
                                modifier = Modifier
                                    .width(cardW)
                                    .height(cardH)
                                    .onGloballyPositioned { coords -> cellLayouts[emptyKey] = coords },
                            )
                        } else {
                            val isMovingThis = ghostVisible && movingSlotId == stripToken
                            val isDropTarget = reorderMode && ghostVisible && !reorderFingerDragging &&
                                hoveredSlotKey == stripToken && stripToken != movingSlotId
                            Box(
                                modifier = Modifier
                                    .width(cardW)
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
                                            reorderDragOffset = if (isMovingThis) reorderStripDragOffset else Offset.Zero,
                                            hideSourceWhileFingerDragging = isMovingThis,
                                            usageTimeMs = 0L,
                                            hasNotifBadge = app.packageName in unreadPackages,
                                            focusHorizontalInset = HOME_STRIP_FOCUS_INSET,
                                            contentVerticalInset = HOME_STRIP_CONTENT_VERTICAL_INSET,
                                            labelContentAlignment = Alignment.BottomCenter,
                                            useOutlinedLabel = false,
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
                                            reorderDragOffset = if (isMovingThis) reorderStripDragOffset else Offset.Zero,
                                            hideSourceWhileFingerDragging = isMovingThis,
                                            focusHorizontalInset = HOME_STRIP_FOCUS_INSET,
                                            contentVerticalInset = HOME_STRIP_CONTENT_VERTICAL_INSET,
                                            labelContentAlignment = Alignment.BottomCenter,
                                            useOutlinedLabel = false,
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
                            }
                        }
                        } else {
                        val emptyDropActive = ghostVisible && hoveredSlotKey == emptyKey
                        val emptyDropAlpha by animateFloatAsState(
                            targetValue = if (emptyDropActive) 1f else 0.48f,
                            animationSpec = tween(140),
                            label = "homeStripEmptyDropAlpha",
                        )
                        val emptyDropScale by animateFloatAsState(
                            targetValue = if (emptyDropActive) 1.06f else 1f,
                            animationSpec = tween(140, easing = FastOutSlowInEasing),
                            label = "homeStripEmptyDropScale",
                        )
                        Box(
                            modifier = Modifier
                                .width(cardW)
                                .height(cardH)
                                .onGloballyPositioned { coords -> cellLayouts[emptyKey] = coords },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = emptyDropScale
                                            scaleY = emptyDropScale
                                            alpha = emptyDropAlpha
                                        }
                                        .size(iconSize + 8.dp)
                                        .border(
                                            if (emptyDropActive) 2.dp else 1.5.dp,
                                            if (emptyDropActive) Color(0xCC84D5F6) else Color(0x55B8C1CE),
                                            RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Apps,
                                        null,
                                        tint = if (emptyDropActive) Color(0xCC84D5F6) else Color(0x55B8C1CE),
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
            }

            val dragCell = movingSlotId?.takeIf { !HomeGroupIds.isHomeGroupId(it) }?.let { token ->
                drawerCellForHomeStripToken(token, gridCells, allApps)
            }
            val dragGroupGhost = movingSlotId?.takeIf { HomeGroupIds.isHomeGroupId(it) }?.let { id ->
                homeGroups.find { it.id == id }
            }
            val stripBounds = stripCoords?.boundsInRoot()
            if (ghostVisible && stripBounds != null && (dragCell != null || dragGroupGhost != null)) {
                val b = stripBounds
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
                            alpha = ghostScaleAnim.value.coerceIn(0f, 1f)
                        },
                ) {
                    when {
                        dragGroupGhost != null -> {
                            val gg = dragGroupGhost
                            val members =
                                gg.packageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                            FolderTile(
                                displayLabel = gg.title,
                                members = members,
                                appIconShape = appIconShape,
                                hasUnreadBadge = gg.packageNames.any { it in unreadPackages },
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
                                focusHorizontalInset = HOME_STRIP_FOCUS_INSET,
                                contentVerticalInset = HOME_STRIP_CONTENT_VERTICAL_INSET,
                                labelContentAlignment = Alignment.BottomCenter,
                                useOutlinedLabel = false,
                                onGloballyPositioned = {},
                                onClick = {},
                                onLongPress = {},
                            )
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
    // Min height from AdaptiveLayout — grows at large font-scale instead of clipping 16sp text.
    val searchBarMinHeight = rememberAdaptiveLayout().searchBarMinHeightDp
    val fieldBg = Color(0xFF202020)
    val horizontalInset = 7.dp
    val clearIcon = 22.dp
    val clearPad = 5.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = searchBarMinHeight)
            .background(fieldBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    contentDescription = stringResource(R.string.action_clear_search),
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
                Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))
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
                                if (change.changedToUp()) { scrubbing = false; break }
                                if (change.positionChanged()) {
                                    fingerXpx = change.position.x
                                    if (activated) {
                                        val page = xToPage(fingerXpx)
                                        if (page != hoveredPage) { hoveredPage = page; onScrubPage(page) }
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
                    fallbackScale = when (secondDockFallbackResId) {
                        R.drawable.ic_dock_whatsapp -> 0.76f
                        R.drawable.ic_dock_pulse -> 1.06f
                        else -> 0.84f
                    },
                    appIconShape = appIconShape,
                    onClick = onShortcut,
                    onLongPress = onLongPressShortcut,
                    hasUnread = shortcutHasUnread,
                    buttonSize = 56.dp,
                    iconSize = navIconSize,
                    selected = focused && focusedIndex == shortcutFocusIdx,
                    selectedTint = selectedTint,
                    iconTint = dockIconTint,
                )
            }

            Spacer(Modifier.weight(1f))
            // Third shortcut: same 68×52.dp icon box as mail dock; padding/scale so app icons match the envelope.
            DockEndSlot(
                iconModel = dockEndIconModel,
                fallbackIcon = Icons.Rounded.PhotoCamera,
                fallbackResId = thirdDockFallbackResId,
                fallbackScale = if (thirdDockFallbackResId == R.drawable.ic_dock_pulse) 1.06f else 0.84f,
                appIconShape = appIconShape,
                onClick = onCamera,
                onLongPress = onLongPressCamera,
                buttonSize = 68.dp,
                iconSize = 52.dp,
                selected = focused && focusedIndex == cameraFocusIdx,
                selectedTint = selectedTint,
                iconTint = dockIconTint,
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
    val trackpadActive = LocalTrackpadActive.current
    val highlightAlpha by animateFloatAsState(
        targetValue = if (selected && trackpadActive) 1f else 0f,
        animationSpec = tween(durationMillis = if (selected && trackpadActive) 120 else 500),
        label = "dockIconHighlight",
    )
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .size(buttonSize)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(selectedTint.copy(alpha = selectedTint.alpha * highlightAlpha))
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
    val trackpadActive = LocalTrackpadActive.current
    val highlightAlpha by animateFloatAsState(
        targetValue = if (selected && trackpadActive) 1f else 0f,
        animationSpec = tween(durationMillis = if (selected && trackpadActive) 120 else 500),
        label = "dockPngHighlight",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(selectedTint.copy(alpha = selectedTint.alpha * highlightAlpha))
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
                        .border(width = 1.dp, color = Color.Black, shape = activeShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (hideFirstPageLabel && current == 0) "" else (current + 1).toString(),
                        style = TextStyle(
                            color = Color.Black,
                            fontSize = themePalette.pageIndicatorFontSp.sp,
                            lineHeight = themePalette.pageIndicatorFontSp.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both,
                            ),
                        ),
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
    hasUnread: Boolean = false,
) {
    val trackpadActive = LocalTrackpadActive.current
    val highlightAlpha by animateFloatAsState(
        targetValue = if (selected && trackpadActive) 1f else 0f,
        animationSpec = tween(durationMillis = if (selected && trackpadActive) 120 else 500),
        label = "dockSlotHighlight",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(selectedTint.copy(alpha = selectedTint.alpha * highlightAlpha))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        AppIconWithBadge(hasUnread = hasUnread) {
            if (iconModel != null) {
                AsyncImage(
                    model = iconModel,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(iconSize) // always full icon size; fallbackScale is only for built-in PNG assets
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
    onSelect: (String) -> Unit,
    onUseDefault: () -> Unit,
    onDismiss: () -> Unit,
    dockSecondEnabled: Boolean = true,
    onToggleDockSecond: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        apps.filter {
            if (it.internal) return@filter false
            if (q.isEmpty()) return@filter true
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }
    val dockPickerFR = remember { FocusRequester() }
    val searchFieldFR = remember { FocusRequester() }
    var focusedApp by remember { mutableIntStateOf(0) }
    val appListState = rememberLazyListState()
    LaunchedEffect(filtered) { focusedApp = 0 }
    LaunchedEffect(focusedApp) {
        if (filtered.isNotEmpty()) appListState.animateScrollToItem(focusedApp.coerceIn(0, filtered.size - 1))
    }
    BackHandler(enabled = true, onBack = onDismiss)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(420f)
            .focusRequester(dockPickerFR)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                when {
                    up    -> { focusedApp = (focusedApp - 1).coerceAtLeast(0); true }
                    down  -> { focusedApp = (focusedApp + 1).coerceAtMost((filtered.size - 1).coerceAtLeast(0)); true }
                    enter -> { filtered.getOrNull(focusedApp)?.let { onSelect(it.packageName) }; true }
                    else  -> false
                }
            },
        color = themePalette.settingsBg,
    ) {
        LaunchedEffect(Unit) { runCatching { searchFieldFR.requestFocus() } }
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
                        contentDescription = stringResource(R.string.action_back),
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    when (slot) {
                        DockSlot.Mail -> stringResource(R.string.dock_shortcut_mail_title)
                        DockSlot.Shortcut -> stringResource(R.string.dock_shortcut_second_title)
                        DockSlot.Camera -> stringResource(R.string.dock_shortcut_camera_title)
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
                        text = stringResource(R.string.dock_show_in_dock),
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
                        DockSlot.Mail -> stringResource(R.string.dock_use_default_mail)
                        DockSlot.Shortcut -> stringResource(R.string.dock_use_default_messages)
                        DockSlot.Camera -> stringResource(R.string.dock_use_default_camera)
                    },
                    color = Color(0xFF84D5F6),
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .focusRequester(searchFieldFR),
                placeholder = { Text(stringResource(R.string.search_apps_hint), color = themePalette.settingsMenuBody) },
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
                state = appListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (focusedApp == index) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
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
private fun AppPickerOverlay(
    title: String,
    subtitle: String,
    apps: List<AppEntry>,
    themePalette: LauncherThemePalette,
    onSelect: (String) -> Unit,
    onUseDefault: () -> Unit,
    onDismiss: () -> Unit,
    useDefaultLabel: String,
) {
    var query by remember { mutableStateOf("") }
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
                        contentDescription = stringResource(R.string.action_back),
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = themePalette.settingsMenuTitle,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = themePalette.settingsMenuBody,
                    )
                }
            }
            TextButton(
                onClick = onUseDefault,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color(0xFF84D5F6),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            useDefaultLabel,
                            color = Color(0xFF84D5F6),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = themePalette.settingsMenuBody,
                        modifier = Modifier.size(18.dp),
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.search_apps_hint), color = themePalette.settingsMenuBody) },
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
    homeStripEnabled: Boolean,
    themePalette: LauncherThemePalette,
    onToggleHomeStrip: (Boolean) -> Unit,
    onCreateGroup: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val subtitleColor = Color(0xFF8E95A3)
    val cardBg = Color(0xFF1E2430)
    val cardFocusedBg = Color(0xFF252D3E)
    val cardFocusedBorder = BorderStroke(1.dp, Color(0x6684D5F6))
    var newName by remember { mutableStateOf("") }
    val homeGroupsFR = remember { FocusRequester() }
    var focusedItem by remember { mutableIntStateOf(0) }
    BackHandler(enabled = true, onBack = onDismiss)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(404f)
            .focusRequester(homeGroupsFR)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                when {
                    up    -> { focusedItem = (focusedItem - 1).coerceAtLeast(0); true }
                    down  -> { focusedItem = (focusedItem + 1).coerceAtMost(groups.size); true }
                    enter -> { if (focusedItem == 0) onToggleHomeStrip(!homeStripEnabled); true }
                    else  -> false
                }
            },
        color = themePalette.settingsBg,
    ) {
        LaunchedEffect(Unit) { homeGroupsFR.requestFocus() }
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
                        contentDescription = stringResource(R.string.action_back),
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    "Home Strip",
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
                    "Groups appear on the Home Strip. Long-press and drag to rearrange slots. " +
                        "Long-press an app in the drawer and use Add to… to include it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (focusedItem == 0) cardFocusedBg else cardBg,
                    border = if (focusedItem == 0) cardFocusedBorder else null,
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
                                "Show Home Strip",
                                style = MaterialTheme.typography.bodyLarge,
                                color = themePalette.settingsMenuTitle,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                if (homeStripEnabled) {
                                    "Apps and groups appear above the dock"
                                } else {
                                    "Hidden from the home screen"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = subtitleColor,
                            )
                        }
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
                    }
                }
                // ── Create new group ──────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = capitalizeFirstLetterForGroupInput(it) },
                        singleLine = true,
                        label = { Text(stringResource(R.string.dialog_new_group_name_hint), color = themePalette.settingsMenuBody) },
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
                groups.forEachIndexed { gIdx, g ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (focusedItem == gIdx + 1) cardFocusedBg else cardBg,
                        border = if (focusedItem == gIdx + 1) cardFocusedBorder else null,
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
                                Text(stringResource(R.string.action_delete), color = Color(0xFFFF6B6B))
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
    val cardFocusedBg = Color(0xFF252D3E)
    val cardFocusedBorder = BorderStroke(1.dp, Color(0x6684D5F6))
    val cardShape = RoundedCornerShape(12.dp)
    val glanceFR = remember { FocusRequester() }
    var focusedGlance by remember { mutableIntStateOf(0) }
    val glanceBringers = remember { List(6) { BringIntoViewRequester() } }
    LaunchedEffect(focusedGlance) { glanceBringers[focusedGlance].bringIntoView() }
    BackHandler(enabled = true, onBack = onDismiss)

    @Composable
    fun ToggleCard(
        title: String,
        subtitle: String,
        checked: Boolean,
        enabled: Boolean = true,
        focused: Boolean = false,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        Surface(
            shape = cardShape,
            color = if (focused) cardFocusedBg else cardBg,
            border = if (focused) cardFocusedBorder else null,
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
            .zIndex(403f)
            .focusRequester(glanceFR)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                when {
                    up    -> { focusedGlance = (focusedGlance - 1).coerceAtLeast(0); true }
                    down  -> { focusedGlance = (focusedGlance + 1).coerceAtMost(5); true }
                    enter -> {
                        when (focusedGlance) {
                            0 -> onGlanceEnabled(!glanceEnabled)
                            1 -> onGlanceWeatherUnit(if (glanceWeatherUnit == GlanceWeatherUnit.CELSIUS) GlanceWeatherUnit.FAHRENHEIT else GlanceWeatherUnit.CELSIUS)
                            2 -> if (glanceEnabled) onGlanceShowFlashlight(!glanceShowFlashlight)
                            3 -> if (glanceEnabled) onGlanceShowCalendar(!glanceShowCalendar)
                            4 -> if (glanceEnabled) onGlanceShowBattery(!glanceShowBattery)
                            5 -> if (glanceEnabled) onGlanceShowAlarm(!glanceShowAlarm)
                        }
                        true
                    }
                    else -> false
                }
            },
        color = themePalette.settingsBg,
    ) {
        LaunchedEffect(Unit) { glanceFR.requestFocus() }
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
                        contentDescription = stringResource(R.string.action_back),
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    stringResource(R.string.settings_glance_strip_title),
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
                    stringResource(R.string.glance_settings_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
                Box(modifier = Modifier.bringIntoViewRequester(glanceBringers[0])) {
                    ToggleCard(
                        title = stringResource(R.string.glance_show_title),
                        subtitle = stringResource(R.string.glance_show_subtitle),
                        checked = glanceEnabled,
                        focused = focusedGlance == 0,
                        onCheckedChange = onGlanceEnabled,
                    )
                }
                Surface(shape = cardShape, color = if (focusedGlance == 1) cardFocusedBg else cardBg, border = if (focusedGlance == 1) cardFocusedBorder else null, modifier = Modifier.fillMaxWidth().bringIntoViewRequester(glanceBringers[1])) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            stringResource(R.string.glance_weather_unit),
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
                                    text = stringResource(R.string.glance_celsius),
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
                                    text = stringResource(R.string.glance_fahrenheit),
                                    color = if (glanceWeatherUnit == GlanceWeatherUnit.FAHRENHEIT) Color.White else subtitleColor,
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.bringIntoViewRequester(glanceBringers[2])) {
                    ToggleCard(
                        title = stringResource(R.string.glance_flashlight_title),
                        subtitle = stringResource(R.string.glance_flashlight_subtitle),
                        checked = glanceShowFlashlight,
                        enabled = glanceEnabled,
                        focused = focusedGlance == 2,
                        onCheckedChange = onGlanceShowFlashlight,
                    )
                }
                Box(modifier = Modifier.bringIntoViewRequester(glanceBringers[3])) {
                    ToggleCard(
                        title = stringResource(R.string.glance_calendar_title),
                        subtitle = stringResource(R.string.glance_calendar_subtitle),
                        checked = glanceShowCalendar,
                        enabled = glanceEnabled,
                        focused = focusedGlance == 3,
                        onCheckedChange = onGlanceShowCalendar,
                    )
                }
                Box(modifier = Modifier.bringIntoViewRequester(glanceBringers[4])) {
                    ToggleCard(
                        title = stringResource(R.string.glance_battery_title),
                        subtitle = stringResource(R.string.glance_battery_subtitle),
                        checked = glanceShowBattery,
                        enabled = glanceEnabled,
                        focused = focusedGlance == 4,
                        onCheckedChange = onGlanceShowBattery,
                    )
                }
                Box(modifier = Modifier.bringIntoViewRequester(glanceBringers[5])) {
                    ToggleCard(
                        title = stringResource(R.string.glance_alarm_title),
                        subtitle = stringResource(R.string.glance_alarm_subtitle),
                        checked = glanceShowAlarm,
                        enabled = glanceEnabled,
                        focused = focusedGlance == 5,
                        onCheckedChange = onGlanceShowAlarm,
                    )
                }
            }
        }
    }
}

@Composable
private fun IconAppearanceSettingsOverlay(
    gridPreset: GridPreset,
    appIconShape: AppIconShape,
    iconPackPackage: String,
    showAppCardBackground: Boolean,
    showIconNotifBadge: Boolean,
    notificationAccessReady: Boolean,
    previewApps: List<AppEntry>,
    drawerBadgesSubtitle: String,
    onGridPreset: (GridPreset) -> Unit,
    onAppGridIconSize: (Float) -> Unit,
    onAppGridFontSize: (Float) -> Unit,
    onAppGridFontWeight: (String) -> Unit,
    onSetAppIconShape: (AppIconShape) -> Unit,
    onSetIconPackPackage: (String) -> Unit,
    onToggleAppCardBackground: () -> Unit,
    onShowIconNotifBadgeChange: (Boolean) -> Unit,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var selectedIndex by remember { mutableStateOf(0) }
    val itemCount = 3
    val subtitleColor = Color(0xFF8E95A3)
    val cardBg = Color(0xFF1E2430)
    val cardShape = RoundedCornerShape(12.dp)
    var showIconLayoutSettings by remember { mutableStateOf(false) }
    var showCardBackgroundSettings by remember { mutableStateOf(false) }
    var showBadgeSettings by remember { mutableStateOf(false) }
    BackHandler(enabled = true) {
        when {
            showIconLayoutSettings -> showIconLayoutSettings = false
            showCardBackgroundSettings -> showCardBackgroundSettings = false
            showBadgeSettings -> showBadgeSettings = false
            else -> onDismiss()
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
                        contentDescription = stringResource(R.string.action_back),
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    stringResource(R.string.settings_icon_layout_title),
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
                                        showIconLayoutSettings = true
                                    }
                                    1 -> showCardBackgroundSettings = true
                                    2 -> showBadgeSettings = true
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
                        title = stringResource(R.string.icon_app_grid_size),
                        subtitle = "${gridPreset.rows} × ${gridPreset.cols}",
                        selected = selectedIndex == 0,
                        themePalette = themePalette,
                        subtitleColor = subtitleColor,
                        onClick = {
                            selectedIndex = 0
                            showIconLayoutSettings = true
                        },
                        trailingContent = {
                            Text("›", fontSize = 20.sp, color = if (selectedIndex == 0) Color(0xFF84D5F6) else Color(0xFF7A8290))
                        },
                    )
                }
                SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 1) {
                    SettingsRow(
                        icon = Icons.Rounded.GridView,
                        title = stringResource(R.string.icon_card_background_title),
                        subtitle = if (showAppCardBackground) stringResource(R.string.icon_card_background_on) else stringResource(R.string.icon_card_background_off),
                        selected = selectedIndex == 1,
                        themePalette = themePalette,
                        subtitleColor = subtitleColor,
                        onClick = {
                            selectedIndex = 1
                            showCardBackgroundSettings = true
                        },
                        trailingContent = {
                            Text("›", fontSize = 20.sp, color = if (selectedIndex == 1) Color(0xFF84D5F6) else Color(0xFF7A8290))
                        },
                    )
                }
                SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 2) {
                    SettingsRow(
                        icon = Icons.Outlined.QueryStats,
                        title = stringResource(R.string.icon_badges_title),
                        subtitle = drawerBadgesSubtitle,
                        selected = selectedIndex == 2,
                        themePalette = themePalette,
                        subtitleColor = subtitleColor,
                        onClick = {
                            selectedIndex = 2
                            showBadgeSettings = true
                        },
                        trailingContent = {
                            Text("›", fontSize = 20.sp, color = if (selectedIndex == 2) Color(0xFF84D5F6) else Color(0xFF7A8290))
                        },
                    )
                }
            }
        }
    }
    if (showIconLayoutSettings) {
        IconLayoutSettingsOverlay(
            gridPreset = gridPreset,
            previewApps = previewApps,
            appIconShape = appIconShape,
            themePalette = themePalette,
            onGridPreset = onGridPreset,
            onAppGridIconSize = onAppGridIconSize,
            onAppGridFontSize = onAppGridFontSize,
            onAppGridFontWeight = onAppGridFontWeight,
            onSetAppIconShape = onSetAppIconShape,
            iconPackPackage = iconPackPackage,
            onSetIconPackPackage = onSetIconPackPackage,
            onDismiss = { showIconLayoutSettings = false },
        )
    }
    if (showCardBackgroundSettings) {
        IconPreviewToggleOverlay(
            title = stringResource(R.string.icon_card_background_title),
            description = stringResource(R.string.icon_card_background_description),
            checked = showAppCardBackground,
            enabled = true,
            toggleTitle = stringResource(R.string.icon_card_background_toggle),
            toggleSubtitle = if (showAppCardBackground) stringResource(R.string.icon_card_background_on) else stringResource(R.string.icon_card_background_off),
            previewApps = previewApps,
            appIconShape = appIconShape,
            iconSizeDp = themePalette.appGridIconSizeDp,
            showCardBackground = showAppCardBackground,
            showBadgePreview = false,
            themePalette = themePalette,
            onCheckedChange = { onToggleAppCardBackground() },
            onDismiss = { showCardBackgroundSettings = false },
            largePreview = true,
        )
    }
    if (showBadgeSettings) {
        IconPreviewToggleOverlay(
            title = stringResource(R.string.icon_badges_title),
            description = stringResource(R.string.icon_badges_description),
            checked = showIconNotifBadge && notificationAccessReady,
            enabled = notificationAccessReady,
            toggleTitle = stringResource(R.string.icon_badge_toggle),
            toggleSubtitle = when {
                !notificationAccessReady -> stringResource(R.string.icon_badge_permission_required)
                showIconNotifBadge -> stringResource(R.string.icon_badge_on)
                else -> stringResource(R.string.settings_off)
            },
            previewApps = previewApps,
            appIconShape = appIconShape,
            iconSizeDp = themePalette.appGridIconSizeDp,
            showCardBackground = showAppCardBackground,
            showBadgePreview = showIconNotifBadge && notificationAccessReady,
            themePalette = themePalette,
            onCheckedChange = onShowIconNotifBadgeChange,
            onDismiss = { showBadgeSettings = false },
        )
    }
}

@Composable
private fun IconPreviewToggleOverlay(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    toggleTitle: String,
    toggleSubtitle: String,
    previewApps: List<AppEntry>,
    appIconShape: AppIconShape,
    iconSizeDp: Float,
    showCardBackground: Boolean,
    showBadgePreview: Boolean,
    themePalette: LauncherThemePalette,
    onCheckedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    largePreview: Boolean = false,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val currentWallpaper = remember { safeWallpaperDrawable(context) }
    val previewItems = remember(previewApps) {
        previewApps
            .asSequence()
            .filter { !it.internal }
            .take(6)
            .toList()
    }
    BackHandler(enabled = true, onBack = onDismiss)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(405f),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val nk = ev.nativeKeyEvent
                    when {
                        ev.key == Key.Enter ||
                            ev.key == Key.NumPadEnter ||
                            nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                            nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER -> {
                            if (enabled) onCheckedChange(!checked)
                            true
                        }
                        else -> false
                    }
                },
        ) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color(0xFF292B2B)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                )
            }
            IconPreviewStrip(
                previewApps = previewItems,
                appIconShape = appIconShape,
                iconSizeDp = iconSizeDp,
                showCardBackground = showCardBackground,
                showBadgePreview = showBadgePreview,
                currentWallpaper = currentWallpaper,
                themePalette = themePalette,
                largePreview = largePreview,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF2C2F2F))
                    .padding(horizontal = 26.dp, vertical = 18.dp),
            ) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    color = Color(0xFF9EA4A9),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF242828))
                        .border(
                            BorderStroke(1.dp, Color(0xFF00A9E0).copy(alpha = 0.35f)),
                            RoundedCornerShape(12.dp),
                        )
                        .clickable(enabled = enabled) { onCheckedChange(!checked) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            toggleTitle,
                            color = if (enabled) Color.White else Color(0xFF9EA4A9),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            toggleSubtitle,
                            color = Color(0xFF9EA4A9),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = checked,
                        enabled = enabled,
                        onCheckedChange = onCheckedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4A90D9),
                            uncheckedThumbColor = Color(0xFF8E95A3),
                            uncheckedTrackColor = Color(0xFF2E3545),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun IconPreviewStrip(
    previewApps: List<AppEntry>,
    appIconShape: AppIconShape,
    iconSizeDp: Float,
    showCardBackground: Boolean,
    showBadgePreview: Boolean,
    currentWallpaper: Drawable?,
    themePalette: LauncherThemePalette,
    largePreview: Boolean = false,
) {
    val previewIconSize = iconSettingsPreviewIconSize(iconSizeDp)
    val previewHeight = if (largePreview) ICON_SETTINGS_PREVIEW_HEIGHT_LARGE else ICON_SETTINGS_PREVIEW_HEIGHT
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(previewHeight),
    ) {
        if (currentWallpaper != null) {
            AsyncImage(
                model = currentWallpaper,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF202344)),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33000000)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            previewApps.take(6).forEachIndexed { index, app ->
                val badgeVisible = !largePreview && showBadgePreview && index % 2 == 0
                IconSettingsPreviewItem(
                    app = app,
                    previewIconSize = previewIconSize,
                    appIconShape = appIconShape,
                    showCardBackground = showCardBackground,
                    showBadge = badgeVisible,
                    themePalette = themePalette,
                )
            }
        }
    }
}

/**
 * Maps the real icon size (44–64 dp slider range) to a preview-friendly size (36–54 dp).
 * The preview container is smaller than the actual grid, so we scale down proportionally
 * rather than using the raw value — but the relative difference must be visible as the
 * slider moves across the full range.
 */
private fun iconSettingsPreviewIconSize(iconSizeDp: Float): Dp {
    val t = (iconSizeDp - MIN_APP_ICON_SIZE_DP) / (MAX_APP_ICON_SIZE_DP - MIN_APP_ICON_SIZE_DP)
    return (36f + t * (54f - 36f)).dp
}

@Composable
private fun IconSettingsPreviewItem(
    app: AppEntry,
    previewIconSize: Dp,
    appIconShape: AppIconShape,
    showCardBackground: Boolean,
    showBadge: Boolean,
    labelSizeSp: Int = ICON_SETTINGS_PREVIEW_LABEL_SP,
    fontWeightName: String = "Normal",
    themePalette: LauncherThemePalette,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(ICON_SETTINGS_PREVIEW_CELL_WIDTH)
            .padding(top = 8.dp)
            .then(
                if (showCardBackground) {
                    Modifier
                        .padding(horizontal = 5.dp)
                        .clip(ICON_SETTINGS_PREVIEW_CARD_SHAPE)
                        .background(
                            Brush.verticalGradient(
                                listOf(themePalette.appCardTop, themePalette.appCardBottom),
                            ),
                        )
                        .border(
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                            ICON_SETTINGS_PREVIEW_CARD_SHAPE,
                        )
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                } else {
                    Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
                },
            ),
    ) {
        Box(
            modifier = Modifier.size(previewIconSize + 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = app.icon,
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(previewIconSize)
                    .clip(iconMaskShape(appIconShape)),
            )
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✱",
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
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.label,
            style = compactAppLabelStyle(
                fontSizeSp = labelSizeSp,
                textColor = Color(0xFFE8EEF7),
                fontWeight = fontWeightFromName(fontWeightName),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun IconLayoutSettingsOverlay(
    gridPreset: GridPreset,
    previewApps: List<AppEntry>,
    appIconShape: AppIconShape,
    iconPackPackage: String,
    themePalette: LauncherThemePalette,
    onGridPreset: (GridPreset) -> Unit,
    onAppGridIconSize: (Float) -> Unit,
    onAppGridFontSize: (Float) -> Unit,
    onAppGridFontWeight: (String) -> Unit,
    onSetAppIconShape: (AppIconShape) -> Unit,
    onSetIconPackPackage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val iconPackRepo = remember(context) { IconPackRepository(context) }
    val focusRequester = remember { FocusRequester() }
    // Needed to guard preset selection — prevents applying presets with cells smaller than minimum.
    val adaptiveLayout = rememberAdaptiveLayout()
    var iconSize by remember(themePalette.appGridIconSizeDp) {
        mutableFloatStateOf(themePalette.appGridIconSizeDp.coerceIn(MIN_APP_ICON_SIZE_DP, MAX_APP_ICON_SIZE_DP))
    }
    var labelSize by remember(themePalette.appCardFontSp) {
        mutableFloatStateOf(themePalette.appCardFontSp.coerceIn(8f, 14f))
    }
    val weightOptions = listOf("Light", "Normal", "Medium", "SemiBold", "Bold")
    val currentShape = if (appIconShape == AppIconShape.ROUNDED) AppIconShape.SOFT_SQUARE else appIconShape
    val currentWallpaper = remember { safeWallpaperDrawable(context) }
    val previewItems = remember(previewApps) {
        previewApps
            .asSequence()
            .filter { !it.internal }
            .take(6)
            .toList()
    }
    var gridMenuExpanded by remember { mutableStateOf(false) }
    var iconPackMenuExpanded by remember { mutableStateOf(false) }
    var iconPacks by remember { mutableStateOf<List<IconPackEntry>>(emptyList()) }
    var iconPacksLoaded by remember { mutableStateOf(false) }
    // Trackpad nav: 0=iconSize, 1=shape, 2=labelSize, 3=labelWeight, 4=grid, 5=iconpack
    var focusedItem by remember { mutableIntStateOf(0) }
    val itemBringers = remember { List(6) { BringIntoViewRequester() } }
    // Cursor index inside each open dropdown (tracks keyboard focus, not selection)
    var gridCursor     by remember { mutableIntStateOf(0) }
    var iconPackCursor by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        iconPacks = iconPackRepo.installedIconPacks()
        iconPacksLoaded = true
    }
    LaunchedEffect(focusedItem) {
        itemBringers[focusedItem].bringIntoView()
    }
    val selectedIconPackLabel = remember(iconPackPackage, iconPacks, iconPacksLoaded) {
        when {
            iconPackPackage.isBlank() -> context.getString(R.string.icon_pack_system_icons)
            else -> iconPacks.firstOrNull { it.packageName == iconPackPackage }?.label
                ?: context.getString(R.string.icon_pack_missing)
        }
    }
    fun snapIconSize(value: Float): Float =
        (MIN_APP_ICON_SIZE_DP + ((value - MIN_APP_ICON_SIZE_DP) / 4f).roundToInt() * 4f)
            .coerceIn(MIN_APP_ICON_SIZE_DP, MAX_APP_ICON_SIZE_DP)
    fun preferredPresetFor(rows: Int, cols: Int): GridPreset =
        GridPreset.entries.firstOrNull { it.rows == rows && it.cols == cols }
            ?: GridPreset.entries
                .filter { it.cols == cols }
                .minByOrNull { abs(it.rows - rows) }
            ?: GridPreset.entries
                .filter { it.rows == rows }
                .minByOrNull { abs(it.cols - cols) }
            ?: gridPreset
    fun cycleShape() {
        val currentIndex = VISIBLE_ICON_SHAPES.indexOf(currentShape).coerceAtLeast(0)
        onSetAppIconShape(VISIBLE_ICON_SHAPES[(currentIndex + 1) % VISIBLE_ICON_SHAPES.size])
    }
    BackHandler(enabled = true, onBack = onDismiss)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(404f),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val nk = ev.nativeKeyEvent
                    val isDpadUp    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                    val isDpadDown  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                    val isDpadLeft  = ev.key == Key.DirectionLeft  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT
                    val isDpadRight = ev.key == Key.DirectionRight || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT
                    val isEnter     = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                                      nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                                      nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                    when {
                        isDpadUp -> {
                            focusedItem = (focusedItem - 1).coerceAtLeast(0)
                            true
                        }
                        isDpadDown -> {
                            focusedItem = (focusedItem + 1).coerceAtMost(5)
                            true
                        }
                        isDpadLeft && focusedItem == 0 -> {
                            val snapped = snapIconSize(iconSize - 4f)
                            iconSize = snapped; onAppGridIconSize(snapped)
                            true
                        }
                        isDpadRight && focusedItem == 0 -> {
                            val snapped = snapIconSize(iconSize + 4f)
                            iconSize = snapped; onAppGridIconSize(snapped)
                            true
                        }
                        isDpadLeft && focusedItem == 1 -> {
                            val next = (labelSize - 1f).coerceAtLeast(8f)
                            labelSize = next; onAppGridFontSize(next)
                            true
                        }
                        isDpadRight && focusedItem == 1 -> {
                            val next = (labelSize + 1f).coerceAtMost(14f)
                            labelSize = next; onAppGridFontSize(next)
                            true
                        }
                        isEnter -> {
                            when (focusedItem) {
                                2 -> {
                                    val cur = weightOptions.indexOf(themePalette.appCardFontWeightName).coerceAtLeast(0)
                                    onAppGridFontWeight(weightOptions[(cur + 1) % weightOptions.size])
                                }
                                3 -> cycleShape()
                                4 -> {
                                    gridCursor = GridPreset.entries.indexOf(gridPreset).coerceAtLeast(0)
                                    gridMenuExpanded = true
                                }
                                5 -> {
                                    val allPkgs = listOf("") + iconPacks.map { it.packageName }
                                    iconPackCursor = allPkgs.indexOf(iconPackPackage).coerceAtLeast(0)
                                    iconPackMenuExpanded = true
                                }
                            }
                            true
                        }
                        else -> false
                    }
                },
        ) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFF101619)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    stringResource(R.string.settings_icon_layout_title),
                    color = Color.White,
                    fontSize = SETTINGS_TITLE_TEXT_SP,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF101619))
                    .verticalScroll(rememberScrollState()),
            ) {
                // Full-width preview strip with wallpaper
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                ) {
                    if (currentWallpaper != null) {
                        AsyncImage(
                            model = currentWallpaper,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF202344)))
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Color(0x44000000)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top,
                    ) {
                        previewItems.forEach { app ->
                            IconSettingsPreviewItem(
                                app = app,
                                previewIconSize = iconSettingsPreviewIconSize(iconSize),
                                appIconShape = currentShape,
                                showCardBackground = false,
                                showBadge = false,
                                labelSizeSp = labelSize.roundToInt(),
                                fontWeightName = themePalette.appCardFontWeightName,
                                themePalette = themePalette,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    // Single settings card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF171F27),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                    ) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            // Icon Size
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(itemBringers[0])
                                    .then(if (focusedItem == 0) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 6.dp, bottom = 2.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            stringResource(R.string.icon_layout_size),
                                            color = Color.White,
                                            fontSize = SETTINGS_TITLE_TEXT_SP,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            stringResource(
                                                R.string.icon_layout_size_value,
                                                iconSize.roundToInt(),
                                                DEFAULT_APP_ICON_SIZE_DP.roundToInt(),
                                            ),
                                            color = Color(0xFF9EA4A9),
                                            fontSize = SETTINGS_BODY_TEXT_SP,
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            "−",
                                            color = if (iconSize > MIN_APP_ICON_SIZE_DP) Color.White else Color(0xFF4A5060),
                                            fontSize = 20.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable(enabled = iconSize > MIN_APP_ICON_SIZE_DP) {
                                                    val s = snapIconSize(iconSize - 4f)
                                                    iconSize = s; onAppGridIconSize(s)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                        Slider(
                                            value = iconSize,
                                            onValueChange = {
                                                val snapped = snapIconSize(it)
                                                iconSize = snapped
                                                onAppGridIconSize(snapped)
                                            },
                                            valueRange = MIN_APP_ICON_SIZE_DP..MAX_APP_ICON_SIZE_DP,
                                            steps = 8,
                                            modifier = Modifier.weight(1f).height(36.dp),
                                        )
                                        Text(
                                            "+",
                                            color = if (iconSize < MAX_APP_ICON_SIZE_DP) Color.White else Color(0xFF4A5060),
                                            fontSize = 20.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable(enabled = iconSize < MAX_APP_ICON_SIZE_DP) {
                                                    val s = snapIconSize(iconSize + 4f)
                                                    iconSize = s; onAppGridIconSize(s)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                }
                            }
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.07f),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            // Label Size
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(itemBringers[1])
                                    .then(if (focusedItem == 1) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 6.dp, bottom = 2.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "Label size",
                                            color = Color.White,
                                            fontSize = SETTINGS_TITLE_TEXT_SP,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            "${labelSize.roundToInt()}sp",
                                            color = Color(0xFF9EA4A9),
                                            fontSize = SETTINGS_BODY_TEXT_SP,
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            "−",
                                            color = if (labelSize > 8f) Color.White else Color(0xFF4A5060),
                                            fontSize = 20.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable(enabled = labelSize > 8f) {
                                                    val s = (labelSize - 1f).coerceAtLeast(8f)
                                                    labelSize = s; onAppGridFontSize(s)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                        Slider(
                                            value = labelSize,
                                            onValueChange = { v ->
                                                val snapped = v.roundToInt().toFloat().coerceIn(8f, 14f)
                                                labelSize = snapped
                                                onAppGridFontSize(snapped)
                                            },
                                            valueRange = 8f..14f,
                                            steps = 5,
                                            modifier = Modifier.weight(1f).height(36.dp),
                                        )
                                        Text(
                                            "+",
                                            color = if (labelSize < 14f) Color.White else Color(0xFF4A5060),
                                            fontSize = 20.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable(enabled = labelSize < 14f) {
                                                    val s = (labelSize + 1f).coerceAtMost(14f)
                                                    labelSize = s; onAppGridFontSize(s)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                }
                            }
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.07f),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            // Label Weight
                            Box(
                                modifier = Modifier
                                    .bringIntoViewRequester(itemBringers[2])
                                    .then(if (focusedItem == 2) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier),
                            ) {
                                IconLayoutValueRow(
                                    label = "Label weight",
                                    subtitle = "Grid",
                                    value = themePalette.appCardFontWeightName,
                                    onClick = {
                                        val cur = weightOptions.indexOf(themePalette.appCardFontWeightName).coerceAtLeast(0)
                                        onAppGridFontWeight(weightOptions[(cur + 1) % weightOptions.size])
                                    },
                                    focused = focusedItem == 2,
                                )
                            }
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.07f),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            // Icon Shape
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(itemBringers[3])
                                    .then(if (focusedItem == 3) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier),
                            ) {
                                IconShapeValueRow(currentShape = currentShape, onClick = ::cycleShape)
                            }
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.07f),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            // Grid (columns × rows)
                            Box(modifier = Modifier.bringIntoViewRequester(itemBringers[4])) {
                                IconLayoutValueRow(
                                    label = "Grid",
                                    subtitle = stringResource(R.string.icon_layout_grid),
                                    value = "${gridPreset.cols} × ${gridPreset.rows}",
                                    onClick = {
                                        gridCursor = GridPreset.entries.indexOf(gridPreset).coerceAtLeast(0)
                                        gridMenuExpanded = true
                                    },
                                    focused = focusedItem == 4,
                                )
                                GridPresetDropdown(
                                    expanded = gridMenuExpanded,
                                    onDismiss = { gridMenuExpanded = false },
                                    selected = gridPreset,
                                    focusedIndex = gridCursor,
                                    onMoveCursor = { delta ->
                                        gridCursor = (gridCursor + delta).coerceIn(0, GridPreset.entries.size - 1)
                                    },
                                    onConfirm = {
                                        gridMenuExpanded = false
                                        val chosen = GridPreset.entries[gridCursor]
                                        // Guard: keyboard Enter should also refuse unsafe presets
                                        if (adaptiveLayout.isDrawerPresetSafe(chosen.cols, chosen.rows)) {
                                            onGridPreset(chosen)
                                        }
                                    },
                                    onSelect = { preset ->
                                        gridMenuExpanded = false
                                        // onClick on the DropdownMenuItem already has `enabled = isSafe`,
                                        // but double-check here for safety.
                                        if (adaptiveLayout.isDrawerPresetSafe(preset.cols, preset.rows)) {
                                            onGridPreset(preset)
                                        }
                                    },
                                )
                            }
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.07f),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            // Icon Pack
                            Box(modifier = Modifier.bringIntoViewRequester(itemBringers[5])) {
                                IconPackValueRow(
                                    selectedLabel = selectedIconPackLabel,
                                    iconPacksLoaded = iconPacksLoaded,
                                    onClick = {
                                        val allPkgs = listOf("") + iconPacks.map { it.packageName }
                                        iconPackCursor = allPkgs.indexOf(iconPackPackage).coerceAtLeast(0)
                                        iconPackMenuExpanded = true
                                    },
                                    focused = focusedItem == 5,
                                )
                                IconPackDropdown(
                                    expanded = iconPackMenuExpanded,
                                    onDismiss = { iconPackMenuExpanded = false },
                                    iconPacks = iconPacks,
                                    iconPacksLoaded = iconPacksLoaded,
                                    iconPackPackage = iconPackPackage,
                                    focusedIndex = iconPackCursor,
                                    onMoveCursor = { delta ->
                                        iconPackCursor = (iconPackCursor + delta).coerceIn(0, iconPacks.size)
                                    },
                                    onConfirm = {
                                        iconPackMenuExpanded = false
                                        val pkg = if (iconPackCursor == 0) "" else iconPacks.getOrNull(iconPackCursor - 1)?.packageName ?: ""
                                        onSetIconPackPackage(pkg)
                                    },
                                    onSelect = { pkg ->
                                        iconPackMenuExpanded = false
                                        onSetIconPackPackage(pkg)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPackValueRow(
    selectedLabel: String,
    iconPacksLoaded: Boolean,
    onClick: () -> Unit,
    focused: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = iconPacksLoaded, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.icon_pack_title),
            color = Color.White,
            fontSize = SETTINGS_TITLE_TEXT_SP,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (iconPacksLoaded) selectedLabel else stringResource(R.string.icon_pack_loading),
            color = Color(0xFF00A9E0),
            fontSize = SETTINGS_VALUE_TEXT_SP,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 190.dp),
        )
    }
}

@Composable
private fun IconShapeValueRow(
    currentShape: AppIconShape,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.icon_layout_shape),
                color = Color.White,
                fontSize = SETTINGS_TITLE_TEXT_SP,
            )
            Text(
                stringResource(R.string.icon_layout_shape_default),
                color = Color(0xFF9EA4A9),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            appIconShapeLabel(currentShape),
            color = Color(0xFF00A9E0),
            fontSize = SETTINGS_VALUE_TEXT_SP,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun IconLayoutValueRow(
    label: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
    focused: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = Color.White,
                fontSize = SETTINGS_TITLE_TEXT_SP,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                color = Color(0xFF9EA4A9),
                fontSize = 13.sp,
            )
        }
        Text(
            value,
            color = Color(0xFF00A9E0),
            fontSize = SETTINGS_VALUE_TEXT_SP,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            Icons.Rounded.ArrowDropDown,
            contentDescription = null,
            tint = Color(0xFF9EA4A9),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun IconLayoutDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    options: List<Int>,
    selected: Int,
    defaultValue: Int,
    focusedIndex: Int = -1,
    onMoveCursor: (Int) -> Unit = {},
    onConfirm: () -> Unit = {},
    onSelect: (Int) -> Unit,
) {
    val innerFocus = remember { FocusRequester() }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(210.dp)
            .background(Color(0xFF252828))
            .focusRequester(innerFocus)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                val back  = ev.key == Key.Back || nk?.keyCode == AndroidKeyEvent.KEYCODE_BACK
                when {
                    up    -> { onMoveCursor(-1); true }
                    down  -> { onMoveCursor(1);  true }
                    enter -> { onConfirm();      true }
                    back  -> { onDismiss();      true }
                    else  -> false
                }
            },
    ) {
        LaunchedEffect(expanded) { if (expanded) innerFocus.requestFocus() }
        options.forEachIndexed { index, option ->
            DropdownMenuItem(
                text = {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (option == defaultValue) "Default ($option)" else option.toString(),
                            color = Color(0xFFE0E3E6),
                            fontSize = SETTINGS_VALUE_TEXT_SP,
                            modifier = Modifier.weight(1f),
                        )
                        if (option == selected) {
                            Text("✓", color = Color(0xFF00C853), fontSize = SETTINGS_VALUE_TEXT_SP)
                        }
                    }
                },
                onClick = { onSelect(option) },
                modifier = if (index == focusedIndex) Modifier.background(Color.White.copy(alpha = 0.1f)) else Modifier,
            )
        }
    }
}

@Composable
private fun GridPresetDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    selected: GridPreset,
    focusedIndex: Int = -1,
    onMoveCursor: (Int) -> Unit = {},
    onConfirm: () -> Unit = {},
    onSelect: (GridPreset) -> Unit,
) {
    val innerFocus = remember { FocusRequester() }
    val presets = GridPreset.entries
    val default = GridPreset.R3C5
    // Compute once per device so unsafe presets can be flagged in the dropdown.
    val adaptiveLayout = rememberAdaptiveLayout()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(210.dp)
            .background(Color(0xFF252828))
            .focusRequester(innerFocus)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                val back  = ev.key == Key.Back || nk?.keyCode == AndroidKeyEvent.KEYCODE_BACK
                when {
                    up    -> { onMoveCursor(-1); true }
                    down  -> { onMoveCursor(1);  true }
                    enter -> { onConfirm();      true }
                    back  -> { onDismiss();      true }
                    else  -> false
                }
            },
    ) {
        LaunchedEffect(expanded) { if (expanded) innerFocus.requestFocus() }
        presets.forEachIndexed { index, preset ->
            val isFocused = index == focusedIndex
            // Flag presets where rows/cols exceed safe limits for this screen size.
            val isSafe = adaptiveLayout.isDrawerPresetSafe(preset.cols, preset.rows)
            val suffix = when {
                preset == default -> " (Default)"
                !isSafe -> " ⚠"          // overflows on current device
                else -> ""
            }
            val label = "${preset.cols} × ${preset.rows}$suffix"
            val textColor = when {
                !isSafe -> Color(0xFF5A5F62)   // grayed-out — not selectable on this device
                isFocused -> Color.White
                else -> Color(0xFFE0E3E6)
            }
            DropdownMenuItem(
                text = {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = SETTINGS_VALUE_TEXT_SP,
                            modifier = Modifier.weight(1f),
                        )
                        if (preset == selected) {
                            Text("✓", color = Color(0xFF00C853), fontSize = SETTINGS_VALUE_TEXT_SP)
                        }
                    }
                },
                // Unsafe presets are not selectable — they would cause cells smaller than
                // the 66dp minimum, making icons overlap on this screen size.
                onClick = { if (isSafe) onSelect(preset) },
                enabled = isSafe,
                modifier = Modifier.then(
                    if (isFocused) Modifier.background(Color.White.copy(alpha = 0.1f)) else Modifier,
                ),
            )
        }
    }
}

@Composable
private fun IconPackDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    iconPacks: List<IconPackEntry>,
    iconPacksLoaded: Boolean,
    iconPackPackage: String,
    focusedIndex: Int = -1,
    onMoveCursor: (Int) -> Unit = {},
    onConfirm: () -> Unit = {},
    onSelect: (String) -> Unit,
) {
    val innerFocus = remember { FocusRequester() }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(220.dp)
            .background(Color(0xFF252828))
            .focusRequester(innerFocus)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                val back  = ev.key == Key.Back || nk?.keyCode == AndroidKeyEvent.KEYCODE_BACK
                when {
                    up    -> { onMoveCursor(-1); true }
                    down  -> { onMoveCursor(1);  true }
                    enter -> { onConfirm();      true }
                    back  -> { onDismiss();      true }
                    else  -> false
                }
            },
    ) {
        LaunchedEffect(expanded) { if (expanded) innerFocus.requestFocus() }
        DropdownMenuItem(
            text = {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.icon_pack_system_icons),
                        color = Color(0xFFE0E3E6),
                        fontSize = SETTINGS_VALUE_TEXT_SP,
                        modifier = Modifier.weight(1f),
                    )
                    if (iconPackPackage.isBlank()) {
                        Text("✓", color = Color(0xFF00C853), fontSize = SETTINGS_VALUE_TEXT_SP)
                    }
                }
            },
            onClick = { onSelect("") },
            modifier = if (focusedIndex == 0) Modifier.background(Color.White.copy(alpha = 0.1f)) else Modifier,
        )
        if (iconPacksLoaded && iconPacks.isEmpty()) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.icon_pack_none_found),
                        color = Color(0xFF9EA4A9),
                        fontSize = SETTINGS_VALUE_TEXT_SP,
                    )
                },
                enabled = false,
                onClick = {},
            )
        }
        iconPacks.forEachIndexed { i, pack ->
            DropdownMenuItem(
                text = {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pack.label,
                            color = Color(0xFFE0E3E6),
                            fontSize = SETTINGS_VALUE_TEXT_SP,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (pack.packageName == iconPackPackage) {
                            Text("✓", color = Color(0xFF00C853), fontSize = SETTINGS_VALUE_TEXT_SP)
                        }
                    }
                },
                onClick = { onSelect(pack.packageName) },
                modifier = if (focusedIndex == i + 1) Modifier.background(Color.White.copy(alpha = 0.1f)) else Modifier,
            )
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
    permissionsSubtitle: String,
    onOpenPermissionsSettings: () -> Unit,
    languageSubtitle: String,
    onOpenLanguageSettings: () -> Unit,
    rootGranted: Boolean,
    onOpenRootSettings: () -> Unit,
    onSetWallpaper: () -> Unit,
    onToggleHaptics: () -> Unit,
    manualGpsCoords: Boolean,
    onExportBackup: () -> String,
    /** @return true if backup was accepted and will be applied. */
    onImportBackup: (String) -> Boolean,
    onResetTheme: () -> Unit,
    gestureSubtitle: String,
    onOpenGestureSettings: () -> Unit,
    onOpenScreenSaverSettings: () -> Unit,
    drawerBadgesSubtitle: String,
    minimalModeEnabled: Boolean,
    onOpenMinimalModeSettings: () -> Unit,
    homeStripEnabled: Boolean,
    onToggleHomeStrip: () -> Unit,
    dockSecondEnabled: Boolean,
    onToggleDockSecond: () -> Unit,
    appIconShape: AppIconShape,
    onSetAppIconShape: (AppIconShape) -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    themePalette: LauncherThemePalette,
    onDismiss: () -> Unit,
    onShowDiagnostics: () -> Unit = {},
    /** Sub-overlays (Gesture, Glance, Language, etc.) rendered inside the settings panel
     *  so their BackHandlers are deepest in the composition tree and always fire first. */
    childContent: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var selectedIndex by remember { mutableStateOf(0) }
    // 7-tap counter for developer diagnostics easter egg on the title.
    var diagTapCount by remember { mutableIntStateOf(0) }
    val itemCount = 17
    var showGpsExportWarn by remember { mutableStateOf(false) }
    var pendingExportFilename by remember { mutableStateOf("") }

    val createBackupDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val json = onExportBackup()
            val out = context.contentResolver.openOutputStream(uri)
                ?: error("Could not open file for writing")
            out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            Toast.makeText(context, context.getString(R.string.backup_saved), Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, context.getString(R.string.backup_failed, it.message ?: "unknown error"), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, context.getString(R.string.settings_restored_from_backup), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.backup_invalid_file), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun activate(index: Int) {
        // Actions that open a full-screen sub-overlay: do NOT steal focus back to the
        // settings list — the sub-overlay's own LaunchedEffect will request focus.
        // Actions that stay on the settings list (toggles, resets, backups): reclaim
        // focus so trackpad DPAD navigation continues to work.
        val opensSubOverlay = index in setOf(0, 1, 3, 4, 8, 9, 10, 12, 13, 14)
        when (index) {
            // HOME SCREEN
            0 -> onOpenAppearanceSettings()
            1 -> onOpenMinimalModeSettings()
            2 -> onToggleHomeStrip()
            3 -> onOpenGestureSettings()
            // DISPLAY
            4 -> onOpenGlanceSettings()
            5 -> onSetWallpaper()
            6 -> onOpenScreenSaverSettings()
            7 -> onResetTheme()
            // DOCK
            8 -> onOpenDockSlotPicker(DockSlot.Mail)
            9 -> onOpenDockSlotPicker(DockSlot.Shortcut)
            10 -> onOpenDockSlotPicker(DockSlot.Camera)
            // SYSTEM
            11 -> onToggleHaptics()
            12 -> onOpenLanguageSettings()
            13 -> onOpenPermissionsSettings()
            14 -> onOpenRootSettings()
            // BACKUP
            15 -> {
                val name = "classiclauncher_backup_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".json"
                if (manualGpsCoords) {
                    pendingExportFilename = name
                    showGpsExportWarn = true
                } else {
                    createBackupDocument.launch(name)
                }
            }
            16 -> openBackupFromDownloads.launch(SettingsDownloads.openBackupJsonPickerIntent())
        }
        doNavFeedback(view, hapticsEnabled, hapticIntensity)
        if (!opensSubOverlay) {
            scope.launch { focusRequester.requestFocus() }
        }
    }

    // Own BackHandler: disabled when a child overlay is open so that child's BackHandler
    // fires first. Without this, the main screen's single BackHandler would sometimes close
    // settings entirely instead of just the child sub-screen.
    BackHandler(enabled = !stackedChildOverlayOpen) {
        onDismiss()
    }

    val cardBg = Color(0xFF1E2430)
    val cardShape = RoundedCornerShape(16.dp)
    val subtitleColor = Color(0xFF8E95A3)

    if (showGpsExportWarn) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGpsExportWarn = false },
            title = { Text(stringResource(R.string.backup_gps_warning_title)) },
            text = { Text(stringResource(R.string.backup_gps_warning_body)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showGpsExportWarn = false
                    createBackupDocument.launch(pendingExportFilename)
                }) { Text(stringResource(R.string.backup_gps_warning_export)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showGpsExportWarn = false }) {
                    Text(stringResource(R.string.backup_gps_warning_cancel))
                }
            },
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(400f),
        color = themePalette.settingsBg,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        val focusManager = LocalFocusManager.current
        val settingsScrollState = rememberScrollState()
        val rowBringers = remember { List(itemCount) { BringIntoViewRequester() } }
        // Steal focus from drawer/pager (still composed under us) so trackpad/DPAD events hit settings.
        LaunchedEffect(Unit) {
            focusManager.clearFocus(force = true)
            focusRequester.requestFocus()
        }
        // When a sub-overlay closes (stackedChildOverlayOpen transitions true→false),
        // reclaim focus so trackpad DPAD navigation works again immediately.
        LaunchedEffect(stackedChildOverlayOpen) {
            if (!stackedChildOverlayOpen) {
                focusRequester.requestFocus()
            }
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
                        contentDescription = stringResource(R.string.action_back),
                        tint = themePalette.settingsMenuTitle,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    "Zeno Classic",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = themePalette.settingsMenuTitle,
                        fontWeight = FontWeight.Normal,
                    ),
                    // 7 taps on the title opens the Developer Diagnostics screen.
                    modifier = Modifier.clickable {
                        diagTapCount++
                        if (diagTapCount >= 7) {
                            diagTapCount = 0
                            onShowDiagnostics()
                        }
                    },
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
                            else -> false
                        }
                    }
                    .verticalScroll(settingsScrollState),
            ) {
                // ── HOME SCREEN ──────────────────────────────────────────
                Text(
                    stringResource(R.string.settings_home_screen),
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[0])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 0) {
                        SettingsRow(
                            icon = Icons.Rounded.Apps,
                            title = stringResource(R.string.settings_icon_layout_title),
                            subtitle = stringResource(R.string.settings_icon_layout_subtitle),
                            selected = selectedIndex == 0,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(0) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 0) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[1])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 1) {
                        SettingsRow(
                            icon = Icons.Outlined.ViewList,
                            title = stringResource(R.string.settings_minimal_mode_title),
                            subtitle = if (minimalModeEnabled) {
                                stringResource(R.string.settings_on)
                            } else {
                                stringResource(R.string.settings_minimal_mode_subtitle)
                            },
                            selected = selectedIndex == 1,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(1) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 1) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[2])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 2) {
                        SettingsRow(
                            icon = Icons.Rounded.BookmarkAdd,
                            title = stringResource(R.string.settings_home_strip_title),
                            subtitle = homeGroupsSubtitle,
                            selected = selectedIndex == 2,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(2) },
                            trailingContent = {
                                Switch(
                                    checked = homeStripEnabled,
                                    onCheckedChange = { onToggleHomeStrip() },
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
                            title = stringResource(R.string.settings_home_gestures_title),
                            subtitle = gestureSubtitle,
                            selected = selectedIndex == 3,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(3) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 3) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A3040), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // ── DISPLAY ──────────────────────────────────────────────
                Text(
                    stringResource(R.string.settings_display),
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[4])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 4) {
                        SettingsRow(
                            icon = Icons.Rounded.WbSunny,
                            title = stringResource(R.string.settings_glance_strip_title),
                            subtitle = glanceSubtitle,
                            selected = selectedIndex == 4,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(4) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 4) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[5])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 5) {
                        SettingsRow(
                            icon = Icons.Rounded.Wallpaper,
                            title = stringResource(R.string.settings_wallpaper_title),
                            subtitle = stringResource(R.string.settings_wallpaper_subtitle),
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
                            icon = Icons.Rounded.WbSunny,
                            title = stringResource(R.string.settings_flip_clock_title),
                            subtitle = stringResource(R.string.settings_flip_clock_subtitle),
                            selected = selectedIndex == 6,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(6) },
                        )
                        AnimatedVisibility(visible = selectedIndex == 6) {
                            ZenoFlipClockSettingsPreview(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[7])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 7) {
                        SettingsRow(
                            icon = Icons.Rounded.Palette,
                            title = stringResource(R.string.settings_reset_theme_title),
                            subtitle = stringResource(R.string.settings_reset_theme_subtitle),
                            selected = selectedIndex == 7,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(7) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A3040), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // ── DOCK ─────────────────────────────────────────────────
                Text(
                    stringResource(R.string.settings_dock),
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[8])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 8) {
                        SettingsRow(
                            icon = Icons.Rounded.MailOutline,
                            title = dockMailTitle,
                            subtitle = dockMailBody,
                            selected = selectedIndex == 8,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(8) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 8) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[9])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 9) {
                        SettingsRow(
                            icon = Icons.Rounded.TouchApp,
                            title = dockSecondTitle,
                            subtitle = if (!dockSecondEnabled) {
                                stringResource(R.string.settings_hidden)
                            } else {
                                dockSecondBody
                            },
                            selected = selectedIndex == 9,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { selectedIndex = 9; activate(9) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 9) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[10])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 10) {
                        SettingsRow(
                            icon = Icons.Rounded.Apps,
                            title = dockThirdTitle,
                            subtitle = dockThirdBody,
                            selected = selectedIndex == 10,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(10) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 10) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A3040), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // ── SYSTEM ───────────────────────────────────────────────
                Text(
                    stringResource(R.string.settings_system),
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[11])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 11) {
                        SettingsRow(
                            icon = Icons.Outlined.Vibration,
                            title = stringResource(R.string.settings_haptics_title),
                            subtitle = if (hapticsEnabled) stringResource(R.string.settings_on) else stringResource(R.string.settings_off),
                            selected = selectedIndex == 11,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(11) },
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
                                    Text(stringResource(R.string.haptic_light), style = MaterialTheme.typography.labelSmall, color = subtitleColor)
                                    Text(
                                        "Intensity: $hapticIntensity",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = themePalette.settingsMenuTitle,
                                    )
                                    Text(stringResource(R.string.haptic_strong), style = MaterialTheme.typography.labelSmall, color = subtitleColor)
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
                Column(Modifier.bringIntoViewRequester(rowBringers[12])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 12) {
                        SettingsRow(
                            icon = Icons.Rounded.Language,
                            title = stringResource(R.string.settings_language_title),
                            subtitle = languageSubtitle,
                            selected = selectedIndex == 12,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(12) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 12) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[13])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 13) {
                        SettingsRow(
                            icon = Icons.Rounded.Security,
                            title = stringResource(R.string.settings_permissions_title),
                            subtitle = permissionsSubtitle,
                            selected = selectedIndex == 13,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(13) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 13) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[14])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 14) {
                        SettingsRow(
                            icon = Icons.Rounded.Security,
                            title = stringResource(R.string.settings_root_access_title),
                            subtitle = if (rootGranted) stringResource(R.string.settings_root_access_granted) else stringResource(R.string.settings_root_access_subtitle),
                            selected = selectedIndex == 14,
                            themePalette = themePalette,
                            subtitleColor = if (rootGranted) Color(0xFF34C759) else subtitleColor,
                            onClick = { activate(14) },
                            trailingContent = {
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    color = if (selectedIndex == 14) Color(0xFF84D5F6) else Color(0xFF7A8290),
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF2A3040), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                // ── BACKUP ───────────────────────────────────────────────
                Text(
                    stringResource(R.string.settings_backup_restore),
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Column(Modifier.bringIntoViewRequester(rowBringers[15])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 15) {
                        SettingsRow(
                            icon = Icons.Rounded.SettingsBackupRestore,
                            title = stringResource(R.string.settings_export_title),
                            subtitle = stringResource(R.string.settings_export_subtitle),
                            selected = selectedIndex == 15,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(15) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.bringIntoViewRequester(rowBringers[16])) {
                    SettingsCategoryCard(cardBg = cardBg, cardShape = cardShape, selected = selectedIndex == 16) {
                        SettingsRow(
                            icon = Icons.Rounded.SettingsBackupRestore,
                            title = stringResource(R.string.settings_import_title),
                            subtitle = stringResource(R.string.settings_import_subtitle),
                            selected = selectedIndex == 16,
                            themePalette = themePalette,
                            subtitleColor = subtitleColor,
                            onClick = { activate(16) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        // Child overlays rendered INSIDE the settings panel's Box so their
        // BackHandlers are always deepest in the composition tree (LIFO → fires first).
        childContent()
        } // end Box
    }
}

@Composable
private fun LanguageSettingsOverlay(
    currentLanguageCode: String,
    themePalette: LauncherThemePalette,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = true, onBack = onDismiss)
    val context = LocalContext.current
    val adaptiveLayout = rememberAdaptiveLayout()
    val selectedCode = LauncherLocale.currentLanguageCode(context).ifEmpty {
        LauncherLocale.normalize(currentLanguageCode)
    }
    val langFR = remember { FocusRequester() }
    var focusedLang by remember { mutableIntStateOf(0) }
    val langListState = rememberLazyListState()
    LaunchedEffect(selectedCode) {
        val idx = LauncherLocale.supportedLanguages.indexOfFirst { it.code == selectedCode }
        if (idx >= 0) focusedLang = idx
    }
    LaunchedEffect(focusedLang) { langListState.animateScrollToItem(focusedLang) }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(520f)
            .focusRequester(langFR)
            .focusable()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val nk = ev.nativeKeyEvent
                val up    = ev.key == Key.DirectionUp    || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP
                val down  = ev.key == Key.DirectionDown  || nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                val enter = ev.key == Key.Enter || ev.key == Key.NumPadEnter ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    nk?.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                when {
                    up    -> { focusedLang = (focusedLang - 1).coerceAtLeast(0); true }
                    down  -> { focusedLang = (focusedLang + 1).coerceAtMost(LauncherLocale.supportedLanguages.size - 1); true }
                    enter -> { onLanguageSelected(LauncherLocale.supportedLanguages[focusedLang].code); true }
                    else  -> false
                }
            },
        color = Color.Black.copy(alpha = 0.62f),
    ) {
        LaunchedEffect(Unit) { langFR.requestFocus() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (adaptiveLayout.screenHeightDp * 0.76f).toInt().dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF24292E),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 14.dp, top = 18.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.language_setting_title),
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.settings_close),
                                tint = Color.White,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 0.5.dp)
                    LazyColumn(
                        state = langListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            // AdaptiveLayout.sheetListMaxHeightDp (50% of screen height).
                            // Old 420dp = 84% on 500dp devices; 50% leaves room for header + bars.
                            .heightIn(max = adaptiveLayout.sheetListMaxHeightDp),
                    ) {
                        itemsIndexed(LauncherLocale.supportedLanguages) { index, language ->
                            val selected = language.code == selectedCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (focusedLang == index) Modifier.background(Color.White.copy(alpha = 0.07f)) else Modifier)
                                    .clickable { onLanguageSelected(language.code) }
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        language.title,
                                        color = if (selected) Color(0xFFFF665C) else Color.White,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (language.subtitle.isNotBlank()) {
                                        Text(
                                            language.subtitle,
                                            color = Color.White.copy(alpha = 0.42f),
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = stringResource(R.string.settings_selected),
                                        tint = Color(0xFFFF665C),
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                        }
                    }
                    Text(
                        stringResource(R.string.language_setting_restart_note),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        color = Color.White.copy(alpha = 0.46f),
                        fontSize = 12.sp,
                    )
                }
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
private fun ZenoFlipClockSettingsPreview(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.Black,
        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("09", "58").forEachIndexed { index, value ->
                    FlipPreviewTile(value, showPeriod = index == 0)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                "Minute-based screen saver · no seconds",
                color = Color(0xFF8E95A3),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.1.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FlipPreviewTile(value: String, showPeriod: Boolean) {
    Box(
        modifier = Modifier
            .width(82.dp)
            .height(74.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF222222), Color(0xFF111111)),
                ),
            )
            .border(0.6.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black.copy(alpha = 0.72f)),
        )
        if (showPeriod) {
            Text(
                "PM",
                color = Color(0xFF8E8E8E),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 7.dp),
            )
        }
        Text(
            value,
            color = Color(0xFFE6E6E6),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
        )
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
                            contentDescription = stringResource(R.string.action_clear),
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

private fun android.content.Context.restartHostActivityForLocaleChange() {
    val activity = findHostActivity() ?: return
    Handler(Looper.getMainLooper()).post {
        val restartIntent = Intent(activity, activity::class.java).apply {
            action = activity.intent?.action ?: Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        runCatching {
            activity.startActivity(restartIntent)
            activity.overridePendingTransition(0, 0)
            activity.finish()
        }
    }
}
