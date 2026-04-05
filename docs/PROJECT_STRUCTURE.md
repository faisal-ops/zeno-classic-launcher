# Project structure (Kotlin / Compose)

Sources live under `app/src/main/java/`. **Physical folders** use `native/…` and `nlauncher/…`, but many files declare **`com.zeno.classiclauncher.nlauncher`** packages (e.g. `native/ui/*.kt` → `com.zeno.classiclauncher.nlauncher.ui`). Other code uses **`com.zeno.classiclauncher.native.*`** (prefs, apps, badges). The **`namespace`** and **`applicationId`** are both `com.zeno.classiclauncher.nlauncher`.

## Entry & composition

| Area | Path / types |
|------|----------------|
| Activity | `native/MainActivity.kt` — package `com.zeno.classiclauncher.nlauncher` |
| Application | `nlauncher/LauncherApplication.kt` |
| Root UI | `native/ui/LauncherScreen.kt` — package `…nlauncher.ui` |
| ViewModel | `native/ui/LauncherViewModel.kt` — package `…nlauncher.ui` |
| Theme wrapper | `native/ui/UiTheme.kt` (`BbTheme`) — package `…nlauncher.ui` |

## Data & persistence

| Area | Path |
|------|------|
| Preferences (DataStore) | `native/prefs/LauncherPrefs.kt`, `LauncherPrefsRepository` (in same file) |
| Backup JSON | `native/prefs/LauncherBackup.kt` |
| App list | `native/apps/AppsRepository.kt`, `AppEntry.kt`, `LauncherActions.kt` |

## Drawer & grid

| Area | Path |
|------|------|
| Cell model & builder | `nlauncher/folders/DrawerGridCell.kt`, `DrawerGridCells.kt`, `FolderIds.kt` |

## Overlays & secondary screens

Composable overlays live under **`native/ui/`** (package **`com.zeno.classiclauncher.nlauncher.ui`** unless noted):

- `LauncherScreen.kt` — home, drawer, dock, settings host, search, modals  
- `AppDrawerBadgesOverlay.kt`, `GestureShortcutsOverlay.kt`, `PermissionsSettingsOverlay.kt`  
- `WallpaperSourceOverlay.kt`, `KeyNav.kt`, `LockWakeSettingsOverlay.kt`  
- Search extras: `nlauncher/ui/SearchExtras.kt`  

Glance (home strip):

- `nlauncher/glance/GlanceDateWeatherEventsView.kt` (custom `FrameLayout` + coroutines)  
- `nlauncher/glance/GlanceStripPreferences.kt`  

## System integration

| Area | Path |
|------|------|
| Notification badges | `native/badges/BadgeNotificationListener.kt`, `NotificationRepository.kt` |
| Sleep / device admin | `nlauncher/power/SleepManager.kt`, `LauncherDeviceAdminReceiver.kt` |
| Usage stats (optional sort) | `nlauncher/usage/UsageStatsRepository.kt` |

## Theme

| Area | Path |
|------|------|
| Palette from JSON | `nlauncher/theme/LauncherThemePalette.kt` |

## Resources

- `app/src/main/res/` — layouts (`glance_date_weather_events_strip.xml`), drawables, `values/strings.xml`  
- `app/src/main/AndroidManifest.xml` — HOME/LAUNCHER, notification listener, device admin  

## Tests

Add instrumented / unit tests under `app/src/test` and `app/src/androidTest` when introduced; this tree may be minimal until expanded.
