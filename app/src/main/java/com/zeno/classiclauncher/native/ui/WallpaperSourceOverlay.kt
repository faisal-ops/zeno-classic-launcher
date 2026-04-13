package com.zeno.classiclauncher.native.ui

import android.app.WallpaperManager
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Palette
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette
import com.zeno.classiclauncher.nlauncher.wallpaper.ZenoWallpaperPublicExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ASSET_WALLPAPER_DIR = "zeno_wallpapers"
private const val PUBLIC_WALLPAPER_DIR = "ZenoClassicWallpapers"

private data class ZenoWallpaperItem(
    val id: String,
    val label: String,
    val previewUri: String,
    val sourceAssetName: String? = null,
    val sourceContentUri: Uri? = null,
)

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
    var mediaPermissionGranted by remember { mutableStateOf(hasMediaReadPermission(context)) }
    val mediaPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        mediaPermissionGranted = granted
        if (!granted) {
            Toast.makeText(context, "Allow Photos permission to show device wallpapers", Toast.LENGTH_SHORT).show()
        }
    }

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
                onWallpaperAndStyle = { actions.openWallpaperStyleSettings(); onDismiss() },
            )
            WallpaperSourceSub.ZenoGrid -> ZenoWallpaperGrid(
                themePalette = themePalette,
                onBack = { onSubViewChange(WallpaperSourceSub.List) },
                mediaPermissionGranted = mediaPermissionGranted,
                onRequestMediaPermission = {
                    val perm = mediaReadPermission()
                    if (perm != null) mediaPermLauncher.launch(perm)
                },
                onPickWallpaper = { item ->
                    scope.launch(Dispatchers.IO) {
                        val bmp = runCatching {
                            when {
                                item.sourceAssetName != null ->
                                    context.assets.open("$ASSET_WALLPAPER_DIR/${item.sourceAssetName}").use { ins ->
                                        BitmapFactory.decodeStream(ins)
                                    }
                                item.sourceContentUri != null ->
                                    context.contentResolver.openInputStream(item.sourceContentUri)?.use { ins ->
                                        BitmapFactory.decodeStream(ins)
                                    }
                                else -> null
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
    onWallpaperAndStyle: () -> Unit,
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
            SourceRow(Icons.Rounded.Palette, "Wallpapers & style", iconTint, labelColor, onWallpaperAndStyle)
            Spacer(Modifier.height(20.dp))
            Text(
                "Choose from bundled Zeno wallpapers, or open system Wallpapers & style settings.",
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
    mediaPermissionGranted: Boolean,
    onRequestMediaPermission: () -> Unit,
    onPickWallpaper: (ZenoWallpaperItem) -> Unit,
) {
    val context = LocalContext.current
    val titleColor = themePalette.settingsMenuTitle
    var wallpapers by remember { mutableStateOf<List<ZenoWallpaperItem>>(emptyList()) }
    LaunchedEffect(mediaPermissionGranted) {
        val assetItems = context.assets.list(ASSET_WALLPAPER_DIR)
            ?.filter { it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) || it.endsWith(".png", true) }
            .orEmpty()
            .sorted()
            .map { assetName ->
                ZenoWallpaperItem(
                    id = "asset:$assetName",
                    label = humanizeAssetLabel(assetName),
                    previewUri = "file:///android_asset/$ASSET_WALLPAPER_DIR/${Uri.encode(assetName)}",
                    sourceAssetName = assetName,
                )
            }
        val deviceItems = if (mediaPermissionGranted) queryDeviceZenoWallpapers(context) else emptyList()
        wallpapers = (assetItems + deviceItems).distinctBy { it.id }
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
        if (!mediaPermissionGranted) {
            Text(
                "Allow Photos permission to include images from Pictures/$PUBLIC_WALLPAPER_DIR.",
                color = Color(0xFF8E95A3),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Text(
                "Grant permission",
                color = Color(0xFF84D5F6),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onRequestMediaPermission)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        if (wallpapers.isEmpty()) {
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
            items(wallpapers, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2430))
                        .clickable { onPickWallpaper(item) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = item.previewUri,
                        contentDescription = item.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Text(
                        item.label,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB8C0CC)),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

private fun hasMediaReadPermission(context: android.content.Context): Boolean {
    val permission = mediaReadPermission() ?: return true
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun mediaReadPermission(): String? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_IMAGES
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Manifest.permission.READ_EXTERNAL_STORAGE
    else -> null
}

private fun queryDeviceZenoWallpapers(context: android.content.Context): List<ZenoWallpaperItem> {
    val resolver = context.contentResolver
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.RELATIVE_PATH,
    )
    val selection: String
    val args: Array<String>
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        args = arrayOf("${Environment.DIRECTORY_PICTURES}/$PUBLIC_WALLPAPER_DIR/%")
    } else {
        @Suppress("DEPRECATION")
        run {
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            args = arrayOf("%/${Environment.DIRECTORY_PICTURES}/$PUBLIC_WALLPAPER_DIR/%")
        }
    }
    return buildList {
        resolver.query(
            collection,
            projection,
            selection,
            args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val uri = Uri.withAppendedPath(collection, id.toString())
                add(
                    ZenoWallpaperItem(
                        id = "device:$id",
                        label = humanizeAssetLabel(name),
                        previewUri = uri.toString(),
                        sourceContentUri = uri,
                    )
                )
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
