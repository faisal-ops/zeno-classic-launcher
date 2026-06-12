package com.zeno.classiclauncher.nlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.draw.clip

/**
 * Developer Diagnostics Overlay — shows all adaptive layout values for the current device.
 *
 * Triggered via the hidden "Version" tap (7×) in Settings.
 * Useful for diagnosing responsiveness issues on new devices without attaching a debugger.
 *
 * Displays:
 *  - Screen dimensions and density
 *  - Window size class (Compact / Medium / Expanded)
 *  - AdaptiveLayout computed values (grid counts, heights, etc.)
 *  - Font scale
 *  - Orientation
 *  - Device category assessment
 */
@Composable
fun DevDiagnosticsOverlay(onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val adaptiveLayout = rememberAdaptiveLayout()

    val screenWidthDp = config.screenWidthDp
    val screenHeightDp = config.screenHeightDp
    val fontScale = config.fontScale
    val orientation = if (config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
        "Landscape" else "Portrait"

    val densityValue = with(density) { 1.dp.toPx() }  // px per dp
    val dpi = (densityValue * 160f).toInt()

    val widthPx = with(density) { screenWidthDp.dp.roundToPx() }
    val heightPx = with(density) { screenHeightDp.dp.roundToPx() }

    val widthClassName = adaptiveLayout.widthClass.name
    val widthClassColor = when (adaptiveLayout.widthClass) {
        AdaptiveLayout.WidthClass.Compact -> Color(0xFF4CAF50)
        AdaptiveLayout.WidthClass.Medium -> Color(0xFFFF9800)
        AdaptiveLayout.WidthClass.Expanded -> Color(0xFF2196F3)
    }

    // Assess device category
    val deviceCategory = when {
        screenWidthDp <= 380 && screenHeightDp <= 600 -> "BB-style keyboard phone (Zinwa Q25 / Titan 2)"
        screenWidthDp <= 420 && screenHeightDp <= 700 -> "Compact keyboard phone"
        screenWidthDp < 600 -> "Compact phone"
        screenWidthDp < 840 -> "Medium (large phone / small tablet)"
        else -> "Expanded (tablet / foldable)"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0A0F17))
            .zIndex(999f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111923))
                .border(1.dp, Color(0xFF2A3848), RoundedCornerShape(16.dp))
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🔧 Developer Diagnostics",
                    color = Color(0xFFE8EEF7),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    "✕",
                    color = Color(0xFF6C7A8D),
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp),
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                deviceCategory,
                color = widthClassColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(Modifier.height(16.dp))
            DiagSection("SCREEN")
            DiagRow("Width", "${screenWidthDp}dp  (${widthPx}px)")
            DiagRow("Height", "${screenHeightDp}dp  (${heightPx}px)")
            DiagRow("Density", "%.2f  (~${dpi}dpi)".format(densityValue))
            DiagRow("Font Scale", "%.2f  (${(fontScale * 100).toInt()}%%)".format(fontScale))
            DiagRow("Orientation", orientation)

            Spacer(Modifier.height(12.dp))
            DiagSection("WINDOW SIZE CLASS")
            DiagRow("Class", widthClassName, valueColor = widthClassColor)
            DiagRow("Thresholds", "Compact <600dp · Medium 600–839dp · Expanded ≥840dp")

            Spacer(Modifier.height(12.dp))
            DiagSection("ADAPTIVE LAYOUT — COMPUTED VALUES")
            DiagRow("Max Drawer Cols", "${adaptiveLayout.maxDrawerCols}")
            DiagRow("Max Drawer Rows", "${adaptiveLayout.maxDrawerRows}")
            DiagRow("Home Grid Cols", "${adaptiveLayout.homeGridCols}")
            DiagRow("Home Grid Rows", "${adaptiveLayout.homeGridRows}")
            DiagRow("Max Strip Cols", "${adaptiveLayout.maxStripCols}")
            DiagRow("Search Bar Min H", "${adaptiveLayout.searchBarMinHeightDp}")
            DiagRow("Sheet List Max H", "${adaptiveLayout.sheetListMaxHeightDp}")
            DiagRow("Home Search Max H", "${adaptiveLayout.homeSearchResultsMaxHeightDp}")
            DiagRow("Settings Menu Max H", "${adaptiveLayout.settingsMenuMaxHeightDp}")
            DiagRow("Folder Overlay Max H", "${adaptiveLayout.folderOverlayMaxHeightDp}")

            Spacer(Modifier.height(12.dp))
            DiagSection("PRESET SAFETY CHECK")
            val presets = listOf(
                "R3C4" to (3 to 4),
                "R3C5" to (3 to 5),
                "R4C4" to (4 to 4),
                "R4C6" to (4 to 6),
                "R5C5" to (5 to 5),
            )
            presets.forEach { (name, rc) ->
                val (rows, cols) = rc
                val safe = adaptiveLayout.isDrawerPresetSafe(cols, rows)
                DiagRow(
                    name,
                    if (safe) "✓ Safe" else "⚠ Overflows on this device",
                    valueColor = if (safe) Color(0xFF4CAF50) else Color(0xFFFFB300),
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1E2A38))
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap anywhere outside to dismiss",
                color = Color(0xFF3D4B5E),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun DiagSection(title: String) {
    Text(
        title,
        color = Color(0xFF4A90D9),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.2.sp,
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun DiagRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFFE8EEF7),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = Color(0xFF7A8EA8),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
        )
    }
}
