package com.zeno.classiclauncher.nlauncher.minimalmode

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.apps.AppEntry

private val EDITOR_BG = Color(0xFF0D0D0D)
private val SELECTED_ROW_BG = Color(0xFF1E2A3A)
private val CHECK_COLOR = Color(0xFF4DA3FF)
private val TITLE_COLOR = Color.White
private val SUBTITLE_COLOR = Color(0xFF8B8B8B)

@Composable
internal fun MinimalModeAppsEditor(
    allApps: List<AppEntry>,
    selectedPackages: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedKey = selectedPackages.joinToString(",")
    // Work on a local mutable copy; reset when the incoming list changes (editor reopened)
    var currentSelected by rememberSaveable(selectedKey) { mutableStateOf(selectedPackages.toMutableList()) }
    var focusedIndex by rememberSaveable(selectedKey) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    // Sorted: selected (in order) first, then remaining alphabetically
    val sortedApps = remember(allApps, currentSelected) {
        val selectedSet = currentSelected.toSet()
        val selectedApps = currentSelected.mapNotNull { pkg -> allApps.firstOrNull { it.packageName == pkg } }
        val unselectedApps = allApps
            .filter { it.packageName !in selectedSet && !it.internal }
            .sortedBy { it.label.lowercase() }
        selectedApps + unselectedApps
    }

    BackHandler { onSave(currentSelected); onDismiss() }

    LaunchedEffect(focusedIndex) {
        if (focusedIndex >= 0) listState.animateScrollToItem(focusedIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EDITOR_BG)
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                        if (focusedIndex < sortedApps.size - 1) focusedIndex++
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                        if (focusedIndex > 0) focusedIndex--
                        true
                    }
                    event.type == KeyEventType.KeyDown && (event.key == Key.Spacebar) -> {
                        val app = sortedApps.getOrNull(focusedIndex) ?: return@onPreviewKeyEvent false
                        val pkg = app.packageName
                        val newList = currentSelected.toMutableList()
                        if (pkg in newList) newList.remove(pkg) else if (newList.size < 12) newList.add(pkg)
                        currentSelected = newList
                        true
                    }
                    // Move item up in the selected list
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> {
                        val app = sortedApps.getOrNull(focusedIndex) ?: return@onPreviewKeyEvent false
                        val pkg = app.packageName
                        val idx = currentSelected.indexOf(pkg)
                        if (idx > 0) {
                            val newList = currentSelected.toMutableList()
                            newList.removeAt(idx)
                            newList.add(idx - 1, pkg)
                            currentSelected = newList
                            focusedIndex = (focusedIndex - 1).coerceAtLeast(0)
                        }
                        true
                    }
                    // Move item down in the selected list
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight -> {
                        val app = sortedApps.getOrNull(focusedIndex) ?: return@onPreviewKeyEvent false
                        val pkg = app.packageName
                        val idx = currentSelected.indexOf(pkg)
                        if (idx >= 0 && idx < currentSelected.size - 1) {
                            val newList = currentSelected.toMutableList()
                            newList.removeAt(idx)
                            newList.add(idx + 1, pkg)
                            currentSelected = newList
                            focusedIndex = (focusedIndex + 1).coerceAtMost(sortedApps.size - 1)
                        }
                        true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.Enter -> {
                        onSave(currentSelected); onDismiss(); true
                    }
                    else -> false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.minimal_mode_edit_apps_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TITLE_COLOR,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.minimal_mode_edit_apps_hint),
                fontSize = 12.sp,
                color = SUBTITLE_COLOR,
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                itemsIndexed(sortedApps, key = { _, app -> app.packageName }) { index, app ->
                    val isSelected = app.packageName in currentSelected
                    val isFocused = index == focusedIndex
                    EditorRow(
                        app = app,
                        isSelected = isSelected,
                        isFocused = isFocused,
                        onClick = {
                            focusedIndex = index
                            val pkg = app.packageName
                            val newList = currentSelected.toMutableList()
                            if (pkg in newList) newList.remove(pkg) else if (newList.size < 12) newList.add(pkg)
                            currentSelected = newList
                        },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.minimal_mode_editor_save_hint, currentSelected.size),
                fontSize = 12.sp,
                color = SUBTITLE_COLOR,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun EditorRow(
    app: AppEntry,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    isFocused -> SELECTED_ROW_BG
                    else -> Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Checkbox / check icon
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = CHECK_COLOR,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        val icon = app.icon
        if (icon != null) {
            val bmp = remember(icon) { icon.toBitmap(48, 48) }
            androidx.compose.foundation.Image(
                painter = BitmapPainter(bmp.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
        }

        Text(
            text = app.label,
            fontSize = 16.sp,
            color = if (isSelected) Color.White else Color(0xFFCCCCCC),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Show reorder arrows for selected items when focused
        if (isSelected && isFocused) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = CHECK_COLOR,
                modifier = Modifier.size(18.dp),
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = CHECK_COLOR,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
