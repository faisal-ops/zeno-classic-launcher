# Zeno Classic Launcher

Native **Android home app** (launcher) built with **Jetpack Compose**, tuned for **square, keyboard-first phones** - especially the **Zinwa Q25** (720x720, physical QWERTY and navigation keys). It is designed to be set as the **default home** (`HOME` / `DEFAULT`) and works best with **BlackBerry-style** workflows: paged drawer, dock, launcher Quick Settings, glance strip, compact labels, and D-pad / trackpad-friendly focus. Behavior on tall slab phones may differ.

**Not regularly tested on:** Unihertz Titan / Titan Elite (similar form factors may work but are unverified).

---

## Screenshots

| Home | App drawer | App drawer (classic) |
|------|------------|----------------------|
| ![Home with glance strip, folders, dock](docs/screenshots/home-glance-dock.png) | ![App drawer](docs/screenshots/app-drawer-1.png) | ![App drawer classic](docs/screenshots/app-drawer-2.png) |

*Sample UI; apps and wallpaper are illustrative.*

---

## Releases

[![Downloads](https://img.shields.io/github/downloads/faisal-ops/zeno-classic-launcher/total?style=flat-square&label=downloads)](https://github.com/faisal-ops/zeno-classic-launcher/releases)
[![Latest release](https://img.shields.io/github/v/release/faisal-ops/zeno-classic-launcher?sort=semver&style=flat-square&label=release)](https://github.com/faisal-ops/zeno-classic-launcher/releases/latest)

**Install:** open **[Latest release](https://github.com/faisal-ops/zeno-classic-launcher/releases/latest)** and download the attached APK (currently **`zeno-classic-launcher-v1.3.0.apk`**).  
All releases: [github.com/faisal-ops/zeno-classic-launcher/releases](https://github.com/faisal-ops/zeno-classic-launcher/releases).

---

## Features

- **Home + horizontal app drawer** — Themed grid, **folders**, reorder (tap + drag), hidden apps, search ranking, most-used sorting, and compact BlackBerry-style app labels
- **Launcher Quick Settings** — Swipe-down QS overlay with keyboard mode, internet, Bluetooth, QR scanner, battery, torch, DND, storage, hotspot, night light, rotate, NFC, cast, and more where supported by the device
- **QS customization** — Pick the QR scanner app, long-press tiles for settings/actions, drag to rearrange tiles, reset tile order, and persist changes automatically
- **Dock** — Mail badge (notification listener or auto/user mail app), home/page dots with scrub, optional second shortcut, configurable mail/messages/camera-style dock targets, and **custom icon override per dock slot**
- **Glance strip** — Date, Open-Meteo **weather** (coarse location), **calendar** instances, optional battery / alarm hints, and localized glance text
- **Settings** — Grid size, gestures, home strip, icon layout, theme JSON, app icon shape, dock shortcuts, permissions, haptics, language, and **JSON backup / restore**
- **Hardware & keyboard** — D-pad / trackpad navigation with **smart focus visibility** (highlight appears only while trackpad is in use, auto-hides after 2 s), search key handling, haptic navigation, and keyboard-first edit flows
- **Auto Unlock** — On screen-on, skips the "tap to unlock" overlay and auto-submits PIN after 4 digits (physical keyboard; toggle in Settings → Permissions)
- **Sound profiles** — Ring, Vibrate, and DND selector with system-aware fallbacks
- **Localization** — App/settings/glance strings for English plus German, Spanish, French, Hindi, Indonesian, Italian, Japanese, Korean, Portuguese, Russian, and Chinese
- **Notification badges** — Single toggle controls both dock and drawer badges; stuck badge fix (Gmail/Outlook group summaries excluded)
- **Custom app icons** — Long-press any app or dock shortcut → "Change icon" to pick from gallery; "Reset icon" to restore default
- **Notification listener** (optional) — Unread styling for dock mail badge (`BadgeNotificationListener`)
- **Widgets** — Add widgets via system picker from the launcher settings sheet
- **DataStore** — Preferences + versioned backup format

## Technical details

| Item | Value |
|------|--------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose |
| **`applicationId`** | `com.zeno.classiclauncher.nlauncher` |
| **Version** | **1.3.0** (`versionCode` **14**) |
| **Min SDK** | **26** (Android 8.0) |
| **Target SDK** | 34 |
| **Release APK filename** | `zeno-classic-launcher-vX.Y.Z.apk` (see `app/build.gradle.kts` `outputFileName`) |
| **Theme** | JSON-driven palette (`LauncherThemePalette`); import/export in settings |

## Permissions (high level)

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Weather (Open‑Meteo), network-backed features |
| `ACCESS_COARSE_LOCATION` | Approximate location for weather |
| `ACCESS_FINE_LOCATION` | Optional precise Wi-Fi name / weather-related flows where Android requires it |
| `READ_CALENDAR` | Glance strip calendar instances |
| `CAMERA` | Torch tile and QR-related flows where needed |
| `BLUETOOTH_CONNECT` | Bluetooth tile state/settings on Android 12+ |
| `WRITE_SETTINGS` | Optional keyboard mode / auto-rotate style system-setting actions |
| `ACCESS_NOTIFICATION_POLICY` | Optional DND / sound profile behavior |
| `VIBRATE` | Haptic / vibration where enabled in settings |
| `SET_WALLPAPER` | Apply wallpapers from the app |
| `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion` 28) | Legacy optional export to public Pictures (ignored on API 29+) |
| `PACKAGE_USAGE_STATS` | Optional **most-used** app ordering (special access; `tools:ignore` in manifest for protected permission) |

**Services / special roles (no `uses-permission` entry):**

| Component | Role |
|-----------|------|
| **Notification listener** (`BadgeNotificationListener`) | User enables in system settings; used for dock mail-style badge |

Exact declarations are in `app/src/main/AndroidManifest.xml` and runtime flows in the app (e.g. special-access grants for usage stats).

## Build

Use **JDK 17** and Android Studio’s **embedded JDK** for Gradle (`JAVA_HOME` pointing at Android Studio’s JBR where applicable).

### Debug

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/Zeno Classic-debug.apk`

### Release (signed)

1. Add **`key.properties`** at the **repository root** (never commit secrets). See `app/build.gradle.kts` for `storeFile`, `storePassword`, `keyPassword`, `keyAlias`.
2. Build:

```bash
./gradlew :app:assembleRelease
```

APK: `app/build/outputs/apk/release/zeno-classic-launcher-v1.3.0.apk`

```bash
adb install -r app/build/outputs/apk/release/zeno-classic-launcher-v1.3.0.apk
```

Or install via Gradle (uses `adb` under the hood):

```bash
./gradlew :app:installRelease
```

## Tested devices

| Device | Display | Notes |
|--------|---------|--------|
| **Zinwa Q25** | 720×720 | Primary target |

## Contributing

Issues and pull requests are welcome. Follow AGENTS.md for JDK, release builds, and device install expectations when changing code.
