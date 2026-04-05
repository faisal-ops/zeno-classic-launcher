package com.zeno.classiclauncher.native.ui

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette
import com.zeno.classiclauncher.nlauncher.wallpaper.ZenoWallpaperPublicExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ASSET_WALLPAPER_DIR = "zeno_wallpapers"

enum class WallpaperSourceSub {
    List,
    ZenoGrid,
}

/**
 * In-launcher substitute for the system “Choose wallpaper from” screen: we cannot modify OEM UI,
 * but we can offer the same style of list plus **Zeno wallpapers** (bundled under [ASSET_WALLPAPER_DIR]).
 */
@Composable
fun WallpaperSourceOverlay(
    themePalette: LauncherThemePalette,
    subView: WallpaperSourceSub,
    onSubViewChange: (WallpaperSourceSub) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actions = remember(context) { LauncherActions(context.applicationContext) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ZenoWallpaperPublicExport.syncFromAssets(context)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(500f),
        color = Color(0xFF121418),
    ) {
        when (subView) {
            WallpaperSourceSub.List -> WallpaperSourceList(
                themePalette = themePalette,
                onBack = onDismiss,
                onZeno = { onSubViewChange(WallpaperSourceSub.ZenoGrid) },
                onGallery = { actions.openGalleryForWallpaper(); onDismiss() },
                onLiveWallpapers = { actions.openLiveWallpaperChooser(); onDismiss() },
                onPhotos = { actions.openPhotosForWallpaper(); onDismiss() },
                onWallpaperAndStyle = { actions.openWallpaperStyleSettings(); onDismiss() },
                onSystemWallpapers = { actions.openSystemWallpaperChooser(); onDismiss() },
            )
            WallpaperSourceSub.ZenoGrid -> ZenoWallpaperGrid(
                themePalette = themePalette,
                onBack = { onSubViewChange(WallpaperSourceSub.List) },
                onPickAsset = { fileName ->
                    scope.launch(Dispatchers.IO) {
                        val bmp = runCatching {
                            context.assets.open("$ASSET_WALLPAPER_DIR/$fileName").use { ins ->
                                BitmapFactory.decodeStream(ins)
                            }
                        }.getOrNull()
                        if (bmp == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }
                        val ok = withContext(Dispatchers.Main) {
                            runCatching {
                                WallpaperManager.getInstance(context).setBitmap(bmp)
                            }.isSuccess
                        }
                        withContext(Dispatchers.Main) {
                            if (ok) {
                                Toast.makeText(context, "Wallpaper applied", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Could not set wallpaper", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun WallpaperSourceList(
    themePalette: LauncherThemePalette,
    onBack: () -> Unit,
    onZeno: () -> Unit,
    onGallery: () -> Unit,
    onLiveWallpapers: () -> Unit,
    onPhotos: () -> Unit,
    onWallpaperAndStyle: () -> Unit,
    onSystemWallpapers: () -> Unit,
) {
    val titleColor = themePalette.settingsMenuTitle
    val labelColor = Color(0xFFE8EEF7)
    val iconTint = Color(0xFFB8C0CC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = titleColor,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Text(
            "Choose wallpaper from",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            style = MaterialTheme.typography.headlineSmall.copy(
                color = titleColor,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
            ),
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SourceRow(Icons.Rounded.Collections, "Zeno wallpapers", iconTint, labelColor, onZeno)
            SourceRow(Icons.Rounded.PhotoLibrary, "Gallery", iconTint, labelColor, onGallery)
            SourceRow(Icons.Rounded.LiveTv, "Live wallpapers", iconTint, labelColor, onLiveWallpapers)
            SourceRow(Icons.Rounded.Photo, "Photos", iconTint, labelColor, onPhotos)
            SourceRow(Icons.Rounded.Palette, "Wallpaper and style", iconTint, labelColor, onWallpaperAndStyle)
            SourceRow(Icons.Rounded.Wallpaper, "Wallpapers", iconTint, labelColor, onSystemWallpapers)
            Spacer(Modifier.height(20.dp))
            Text(
                "Zeno wallpapers are saved to Pictures/ZenoClassicWallpapers so they show in Gallery, " +
                    "Photos, and other apps. The system “Wallpapers” app uses its own catalog and cannot " +
                    "list third-party files.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF8E95A3),
                    lineHeight = 18.sp,
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SourceRow(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    labelColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(color = labelColor),
        )
    }
}

@Composable
private fun ZenoWallpaperGrid(
    themePalette: LauncherThemePalette,
    onBack: () -> Unit,
    onPickAsset: (String) -> Unit,
) {
    val context = LocalContext.current
    val titleColor = themePalette.settingsMenuTitle
    var assetNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        assetNames = context.assets.list(ASSET_WALLPAPER_DIR)
            ?.filter { it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) || it.endsWith(".png", true) }
            .orEmpty()
            .sorted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = titleColor,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                "Zeno wallpapers",
                style = MaterialTheme.typography.titleLarge.copy(color = titleColor, fontWeight = FontWeight.Normal),
            )
        }
        if (assetNames.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bundled wallpapers", color = Color(0xFF8E95A3))
            }
            return
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(assetNames, key = { it }) { name ->
                val uri = "file:///android_asset/$ASSET_WALLPAPER_DIR/${Uri.encode(name)}"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2430))
                        .clickable { onPickAsset(name) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Text(
                        humanizeAssetLabel(name),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB8C0CC)),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

private fun humanizeAssetLabel(fileName: String): String {
    val base = fileName.substringBeforeLast('.')
    return base
        .replace("_720", "")
        .replace('_', ' ')
        .trim()
        .ifEmpty { fileName }
}
