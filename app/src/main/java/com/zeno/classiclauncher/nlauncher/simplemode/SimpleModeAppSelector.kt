package com.zeno.classiclauncher.nlauncher.simplemode

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.zeno.classiclauncher.nlauncher.apps.AppEntry

private val SELECTOR_BG = Color(0xFF0D0D0D)
private val SELECTOR_ROW_FOCUSED = Color(0xFF1E2A3A)
private val SEARCH_BG = Color(0xFF1A1A1A)

@Composable
internal fun SimpleModeAppSelector(
    allApps: List<AppEntry>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var focusedIndex by rememberSaveable { mutableIntStateOf(0) }
    val searchFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val filtered = remember(query, allApps) {
        val q = query.trim().lowercase()
        allApps
            .filter { !it.internal }
            .filter { q.isEmpty() || it.label.lowercase().contains(q) || it.packageName.contains(q) }
            .sortedBy { it.label.lowercase() }
    }

    // Keep focusedIndex in bounds when filter changes
    LaunchedEffect(filtered.size) {
        if (focusedIndex >= filtered.size) focusedIndex = (filtered.size - 1).coerceAtLeast(0)
    }

    LaunchedEffect(focusedIndex) {
        if (filtered.isNotEmpty()) listState.animateScrollToItem(focusedIndex)
    }

    LaunchedEffect(Unit) { searchFocus.requestFocus() }

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SELECTOR_BG)
            .statusBarsPadding()
            .navigationBarsPadding()
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown -> {
                        if (focusedIndex < filtered.size - 1) focusedIndex++; true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp -> {
                        if (focusedIndex > 0) focusedIndex--; true
                    }
                    event.type == KeyEventType.KeyDown && event.key == Key.Enter -> {
                        filtered.getOrNull(focusedIndex)?.let { onSelect(it.packageName) }; true
                    }
                    else -> false
                }
            },
    ) {
        Spacer(Modifier.height(16.dp))

        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(SEARCH_BG, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Color(0xFF666666),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it; focusedIndex = 0 },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocus),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color(0xFF4DA3FF)),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search apps…", fontSize = 16.sp, color = Color(0xFF555555))
                    }
                    inner()
                },
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.packageName }) { app ->
                val idx = filtered.indexOf(app)
                val isFocused = idx == focusedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isFocused) SELECTOR_ROW_FOCUSED else Color.Transparent)
                        .clickable { onSelect(app.packageName) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val icon = app.icon
                    if (icon != null) {
                        val bmp = remember(icon) { icon.toBitmap(48, 48) }
                        androidx.compose.foundation.Image(
                            painter = BitmapPainter(bmp.asImageBitmap()),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            text = app.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = app.packageName,
                            fontSize = 11.sp,
                            color = Color(0xFF555555),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
