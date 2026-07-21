package com.zeno.classiclauncher.nlauncher.recents

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.ui.LocalReservedStatusBarHeightPx
import kotlinx.coroutines.launch

private val TILE_BG = Color(0xFF14181F)
private val TILE_HEADER_BG = Color(0xFFECEEF1)
private val TILE_HEADER_LABEL = Color(0xFF15181D)
private val TILE_HEADER_ICON_SIZE = 20.dp
private val TILE_HEADER_VERTICAL_PADDING = 8.dp
/** Close (X) button's touch target — bigger than its 18dp glyph so it's not an easy-to-miss tap
 *  target sitting this close to neighboring tiles. Taller than the label icon, so it (not the
 *  icon) sets the header row's real content height. */
private val TILE_HEADER_CLOSE_TOUCH_SIZE = 36.dp
private val TILE_HEADER_CONTENT_HEIGHT = maxOf(TILE_HEADER_ICON_SIZE, TILE_HEADER_CLOSE_TOUCH_SIZE)
/** Header row's own total height — reused by [ActiveFramesOverlay] to size tiles so the next
 *  row peeks in by exactly this much, instead of guessing an arbitrary "peek" constant separate
 *  from the header's real layout. */
private val TILE_HEADER_HEIGHT = TILE_HEADER_CONTENT_HEIGHT + TILE_HEADER_VERTICAL_PADDING * 2

/**
 * BB10-style "Active Frames": a grid of recently-used apps, each tile showing that app's actual
 * last-seen screenshot (see [ActiveFramesRepository]'s own doc for how — root-only, see there for
 * why). Opened by swiping down (or long-pressing, as a fallback) on the dock's
 * Home icon in Classic Mode. On a tile: swipe right (or tap its X) closes that app; swipe left
 * exits Active Frames straight to the drawer's first page, leaving the app itself in recents.
 */
@Composable
internal fun ActiveFramesOverlay(
    /** The dock's own on-screen height — reference BB10 leaves the dock visible and live
     *  underneath Active Frames, so this overlay's own bounds stop short of it instead of
     *  covering it (the real Dock composable, drawn earlier in the same parent Box, shows
     *  through untouched in that strip). */
    dockHeightDp: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit,
    onLaunchApp: (String) -> Unit,
    /** Swipe-left-on-a-tile's exit gesture — dismisses this overlay and opens the drawer's
     *  first page, distinct from [onDismiss] (tap-outside/back, which just returns home). */
    onSwipeLeftToDrawer: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<ActiveFrameTask>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tasks = ActiveFramesRepository.getRecentTasks(context)
        loaded = true
    }

    BackHandler(onBack = onDismiss)

    // Reserves the same strip ZenoStatusBar draws in — this overlay's own background/grid used
    // to start at y=0 and paint straight through that area, so tiles visually sat behind/under
    // the status bar's icons instead of leaving it clear (confirmed on-device).
    val reservedHeightPx = LocalReservedStatusBarHeightPx.current
    val reservedHeightDp = with(LocalDensity.current) { reservedHeightPx.toDp() }

    // MainActivity's theme sets windowShowWallpaper, so the real system wallpaper already draws
    // behind this whole window — leaving this Box's own background untouched (no opaque fill)
    // lets it show through exactly like the rest of the home screen, matching the reference
    // (which shows its own wallpaper through the gaps) instead of a flat black screen.

    // Tile height is derived from actually-available screen space, not a fixed aspect ratio, so
    // exactly 2 full rows (4 tiles) show plus the next row's header strip peeking at the bottom —
    // matching the reference — on whatever screen this runs on, rather than a number tuned for
    // one device. Every input here is a real measurement (screen height, the status bar's own
    // reserved height, the dock's own computed height) or this file's own header layout constants
    // (gridTopPadding/gridBottomPadding/rowGap are this screen's small chrome margins, see the
    // contentPadding/arrangement below — not app-tile sizing, so they stay fixed dp).
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    // Was 8dp, bumped 10% more per feedback ("gap is good but add at least 10% more").
    val gridTopPadding = 8.8.dp
    val gridBottomPadding = 8.dp
    val gridSidePadding = 8.dp
    // One shared gap value for both directions — was 3dp, which read as tiles touching once they
    // grew tall enough to fill 2 whole rows; this is the actual "breathing room" the tiles need.
    val gap = 8.dp
    val availableGridHeightDp = screenHeightDp - reservedHeightDp - dockHeightDp - gridTopPadding - gridBottomPadding
    val tileHeightDp = (availableGridHeightDp - gap * 2 - TILE_HEADER_HEIGHT) / 2f
    // How much of the bottom row fades to transparent right where this composable's own bounds
    // meet the dock strip — the reference has no hard edge there (its dock floats translucently
    // over the last row instead), so a flat cut where our opaque tiles meet the real Dock read as
    // a visible seam. Capped to a fraction of a tile so it can never swallow a whole row.
    val bottomFadeHeightDp = minOf(dockHeightDp, tileHeightDp * 0.4f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Shrinks this composable's own bounds above the dock strip — applied BEFORE
            // clickable() so it doesn't hit-test into that area at all, leaving the real Dock
            // (already drawn earlier in the shared parent Box) tappable underneath.
            .padding(bottom = dockHeightDp)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadeHeightPx = bottomFadeHeightDp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = size.height - fadeHeightPx,
                        endY = size.height,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            .clickable(onClick = onDismiss),
    ) {
        if (loaded && tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.active_frames_empty),
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            // gridTopPadding lives here (the grid's own viewport inset), NOT in contentPadding —
            // contentPadding is scrollable content spacing, so it only ever showed before the
            // first scroll; once you scrolled, later rows reached this exact top edge with no
            // gap at all. A modifier padding instead shrinks the grid's actual viewport, so the
            // gap from the status bar is permanent regardless of scroll position (confirmed as
            // the inconsistency being reported).
            modifier = Modifier
                .fillMaxSize()
                .padding(top = reservedHeightDp + gridTopPadding)
                .clickable(enabled = false) {},
            // Shrinks tiles in from every edge with real breathing room — was 3dp all round,
            // which read as tiles touching once they grew tall enough to fill 2 whole rows.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = gridSidePadding,
                end = gridSidePadding,
                bottom = gridBottomPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(tasks, key = { it.packageName }) { task ->
                ActiveFrameTile(
                    task = task,
                    heightDp = tileHeightDp,
                    // Deliberately does NOT call onDismiss(): showActiveFrames stays true behind
                    // the launched app, so pressing back there returns to this exact list (same
                    // remembered `tasks`, not reloaded) instead of the normal home screen — lets
                    // the user switch straight to another recent app.
                    onOpen = { onLaunchApp(task.packageName) },
                    onClose = {
                        tasks = tasks.filterNot { it.packageName == task.packageName }
                        scope.launch { ActiveFramesRepository.closeTask(task.packageName, task.taskId) }
                    },
                    onSwipeLeftToDrawer = onSwipeLeftToDrawer,
                )
            }
        }
    }
}

@Composable
private fun ActiveFrameTile(
    task: ActiveFrameTask,
    heightDp: androidx.compose.ui.unit.Dp,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    // Swipe-left is a distinct exit gesture, not another way to close this one tile: it leaves
    // this app in recents and instead backs all the way out to the drawer's first page.
    onSwipeLeftToDrawer: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    // Raw accumulated drag, independent of offsetX (the tile's own visual position) — swipe-left
    // is a clean exit gesture with NO tile movement (it's the whole screen changing underneath,
    // not this tile), so tracking it separately from what actually animates onscreen is what
    // stops a left swipe from also dragging the tile and confusing the user about what happened.
    var dragAccumPx by remember { mutableStateOf(0f) }
    // Swipe-right closes this tile (thrown off-screen before onClose fires, rather than requiring
    // the small X hit-target); swipe-left instead exits Active Frames to the drawer.
    val dismissThresholdPx = with(density) { 90.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
            .graphicsLayer { translationX = offsetX.value }
            .pointerInput(dismissThresholdPx) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val accum = dragAccumPx
                        dragAccumPx = 0f
                        when {
                            accum > dismissThresholdPx -> scope.launch {
                                offsetX.animateTo(1500f)
                                onClose()
                            }
                            accum < -dismissThresholdPx -> onSwipeLeftToDrawer()
                            else -> scope.launch { offsetX.animateTo(0f) }
                        }
                    },
                    onDragCancel = {
                        dragAccumPx = 0f
                        scope.launch { offsetX.animateTo(0f) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumPx += dragAmount
                        // Only the rightward (close) direction gets visual feedback.
                        if (dragAccumPx > 0f) {
                            scope.launch { offsetX.snapTo(dragAccumPx) }
                        }
                    },
                )
            }
            // Sharp corners, matching the original BB10 tile — was rounded before.
            .background(TILE_BG)
            .clickable(onClick = onOpen),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                task.thumbnail != null -> Image(
                    bitmap = task.thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                task.icon != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = task.icon,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = Color(0xFF3A4150))
                }
            }
        }
        // Solid header bar (icon + label + close) sits at the BOTTOM of the tile, over the
        // preview content — matching the original BB10 Active Frames layout, not a top strip.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(TILE_HEADER_BG)
                .padding(horizontal = 10.dp, vertical = TILE_HEADER_VERTICAL_PADDING),
        ) {
            AsyncImage(
                model = task.icon,
                contentDescription = null,
                modifier = Modifier.size(TILE_HEADER_ICON_SIZE).clip(RoundedCornerShape(5.dp)),
            )
            Text(
                text = task.label,
                color = TILE_HEADER_LABEL,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // Bigger touch target around an 18dp glyph — was clickable on the glyph's own bounds,
            // an easy-to-miss target for a close button that sits this close to other tiles.
            Box(
                modifier = Modifier
                    .size(TILE_HEADER_CLOSE_TOUCH_SIZE)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = TILE_HEADER_LABEL,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
