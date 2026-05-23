# Project structure (Kotlin / Compose)

Sources live under `app/src/main/java/`. Physical folders now match the declared **`com.zeno.classiclauncher.nlauncher`** package tree. The **`namespace`** and **`applicationId`** are both `com.zeno.classiclauncher.nlauncher`.

## Entry & composition

| Area | Path / types |
|------|----------------|
| Activity | `nlauncher/MainActivity.kt` |
| Application | `nlauncher/LauncherApplication.kt` |
| Root UI | `nlauncher/ui/LauncherScreen.kt` |
| ViewModel | `nlauncher/ui/LauncherViewModel.kt` |
| Theme wrapper | `nlauncher/ui/UiTheme.kt` (`BbTheme`) |

## Data & persistence

| Area | Path |
|------|------|
| Preferences (DataStore) | `nlauncher/prefs/LauncherPrefs.kt`, `LauncherPrefsRepository` (in same file) |
| Backup JSON | `nlauncher/prefs/LauncherBackup.kt` |
| App list | `nlauncher/apps/AppsRepository.kt`, `AppEntry.kt`, `LauncherActions.kt` |

## Drawer & grid

| Area | Path |
|------|------|
| Cell model & builder | `nlauncher/folders/DrawerGridCell.kt`, `DrawerGridCells.kt`, `FolderIds.kt` |

## Overlays & secondary screens

Composable overlays live under **`nlauncher/ui/`**:

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
| Notification badges | `nlauncher/badges/BadgeNotificationListener.kt`, `NotificationRepository.kt` |
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

Unit tests live under `app/src/test`; add instrumented tests under `app/src/androidTest` for device-only flows.
