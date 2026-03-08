#!/bin/bash
# Run Debug Server Integration Tests
#
# This script:
# 1. Sets up ADB port forwarding
# 2. Builds debug APK
# 3. Installs on device/emulator
# 4. Runs debug server integration tests
#
# Prerequisites:
# - Android device/emulator connected via ADB
# - JDK 17+ installed

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "╔══════════════════════════════════════════════════════════╗"
echo "║  Debug Server Integration Tests Runner                  ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ ERROR: No Android device/emulator connected"
    echo
    echo "Start emulator or connect device, then run again."
    exit 1
fi

DEVICE_NAME=$(adb devices | grep "device$" | head -1 | awk '{print $1}')
echo "✓ Device connected: $DEVICE_NAME"
echo

# Setup port forwarding
echo "📡 Setting up ADB port forwarding (tcp:8899 → tcp:8899)..."
adb forward tcp:8899 tcp:8899
echo "✓ Port forwarding configured"
echo

# Build debug APK
echo "🔨 Building debug APK..."
if ./gradlew assembleDebug --quiet; then
    echo "✓ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi
echo

# Install APK
echo "📦 Installing debug APK..."
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    if adb install -r "$APK_PATH" > /dev/null 2>&1; then
        echo "✓ Installation successful"
    else
        echo "❌ Installation failed"
        exit 1
    fi
else
    echo "❌ APK not found at: $APK_PATH"
    exit 1
fi
echo

# Run tests
echo "🧪 Running NavigationDebugServerIntegrationTest..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo

TEST_CLASS="io.github.wykopmobilny.tests.NavigationDebugServerIntegrationTest"

if ./gradlew connectedDebugAndroidTest --tests "$TEST_CLASS" --stacktrace; then
    echo
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "✓ All tests passed!"
    echo
    echo "Test results: app/build/reports/androidTests/connected/index.html"
else
    echo
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "❌ Some tests failed"
    echo
    echo "Test results: app/build/reports/androidTests/connected/index.html"
    echo "Logcat: adb logcat -s DebugHttpServer:* DebugState:* TestRunner:*"
    exit 1
fi

echo
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  Debug commands:                                         ║"
echo "╠══════════════════════════════════════════════════════════╣"
echo "║  curl localhost:8899/state | jq .                       ║"
echo "║  curl -X POST localhost:8899/navigate/hot | jq .        ║"
echo "║  adb logcat -s DebugHttpServer:* | grep -v "            "║"
echo "╚══════════════════════════════════════════════════════════╝"
echo
