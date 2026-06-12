#!/bin/bash
# Full release build + ADB install for Zeno Classic Launcher
# Always run this after code changes — never partial builds.

set -e

JAVA_HOME_PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
# APK filename is set dynamically in build.gradle.kts (zeno-classic-launcher-vX.Y.Z.apk)
APK_PATH=$(ls app/build/outputs/apk/release/zeno-classic-launcher-v*.apk 2>/dev/null | head -1)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"

echo "======================================"
echo " Zeno Classic Launcher — Build+Install"
echo "======================================"

# Verify JAVA_HOME
export JAVA_HOME="$JAVA_HOME_PATH"
if [ ! -d "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME not found at $JAVA_HOME"
    echo "Update JAVA_HOME_PATH in this script."
    exit 1
fi

# Verify device connected
DEVICES=$(adb devices 2>/dev/null | grep -v "^List" | grep "device$" | wc -l | tr -d ' ')
if [ "$DEVICES" -eq 0 ]; then
    echo "ERROR: No ADB device connected. Connect device and retry."
    adb devices
    exit 1
fi

echo ""
echo "[1/2] Building full release APK..."
./gradlew assembleRelease --no-daemon

if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: APK not found at: $APK_PATH"
    exit 1
fi

APK_SIZE=$(du -sh "$APK_PATH" | cut -f1)
echo "      APK built: $APK_PATH ($APK_SIZE)"

echo ""
echo "[2/2] Installing on device..."
adb install -r "$APK_PATH"

echo ""
echo "======================================"
echo " Installed successfully!"
echo " → Please test the app on the device."
echo "======================================"

# Push reminder
AHEAD=$(git rev-list --count @{u}..HEAD 2>/dev/null || echo 0)
if [ "$AHEAD" -ge 5 ]; then
    echo ""
    echo "⚠️  PUSH REMINDER: $AHEAD commits ahead of origin/main"
    echo "   Run: git push origin main"
fi
