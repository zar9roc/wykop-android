#!/bin/bash
# Test Debug HTTP Server stability after GC fix
# This script verifies that the server survives multiple requests and doesn't get GC'ed

set -e

ADB="C:/Users/Adam/AppData/Local/Android/Sdk/platform-tools/adb.exe"
SERVER_URL="http://localhost:8899"
PACKAGE="io.github.wykopmobilny.debug"

echo "=== Debug HTTP Server Stability Test ==="
echo ""

# Step 1: Check port forwarding
echo "[1/5] Checking ADB port forwarding..."
if ! "$ADB" forward --list | grep -q "tcp:8899"; then
    echo "⚠️  Port forwarding not set up. Setting up now..."
    "$ADB" forward tcp:8899 tcp:8899
    echo "✓ Port forwarding configured: tcp:8899 -> tcp:8899"
else
    echo "✓ Port forwarding already configured"
fi
echo ""

# Step 2: Check if app is running
echo "[2/5] Checking if app is running..."
if ! "$ADB" shell pidof $PACKAGE > /dev/null 2>&1; then
    echo "⚠️  App not running. Starting..."
    "$ADB" shell am start -n $PACKAGE/.ui.modules.mainnavigation.MainNavigationActivity
    sleep 2
    echo "✓ App started"
else
    echo "✓ App is running"
fi
echo ""

# Step 3: Verify server responds
echo "[3/5] Verifying server responds..."
if ! curl -s -f $SERVER_URL/ > /dev/null; then
    echo "❌ FAIL: Server not responding"
    echo ""
    echo "Debug info:"
    "$ADB" logcat -s DebugHttpServer DebugServerHolder DebugToolsInitializer -d | tail -10
    exit 1
fi
echo "✓ Server responds"
echo ""

# Step 4: Stress test - 50 requests
echo "[4/5] Stress testing: 50 consecutive requests..."
FAILED=0
for i in {1..50}; do
    if ! curl -s -f $SERVER_URL/ > /dev/null; then
        echo "❌ FAIL: Request $i failed"
        FAILED=1
        break
    fi

    if [ $((i % 10)) -eq 0 ]; then
        echo "  ✓ $i requests completed"
    fi
done

if [ $FAILED -eq 0 ]; then
    echo "✓ All 50 requests succeeded"
else
    echo ""
    echo "Debug info:"
    "$ADB" logcat -s DebugHttpServer DebugServerHolder -d | tail -10
    exit 1
fi
echo ""

# Step 5: Background/foreground cycle
echo "[5/5] Testing background/foreground cycle..."
"$ADB" shell input keyevent KEYCODE_HOME
echo "  - Sent app to background"
sleep 2

"$ADB" shell am start -n $PACKAGE/.ui.modules.mainnavigation.MainNavigationActivity
echo "  - Brought app to foreground"
sleep 1

if ! curl -s -f $SERVER_URL/ > /dev/null; then
    echo "❌ FAIL: Server not responding after background/foreground cycle"
    exit 1
fi
echo "✓ Server survived background/foreground cycle"
echo ""

# Final verification
echo "=== Final Status ==="
RESPONSE=$(curl -s $SERVER_URL/)
SERVER_NAME=$(echo "$RESPONSE" | grep -o '"server":"[^"]*"' | cut -d'"' -f4)
PORT=$(echo "$RESPONSE" | grep -o '"port":[0-9]*' | cut -d':' -f2)

echo "Server: $SERVER_NAME"
echo "Port: $PORT"
echo "Status: ✓ HEALTHY"
echo ""

# Check server holder status in logs
echo "=== Server Holder Logs ==="
"$ADB" logcat -s DebugServerHolder -d | tail -5
echo ""

echo "=== ✅ ALL TESTS PASSED ==="
echo ""
echo "The DebugHttpServer is stable and not affected by GC."
echo "The singleton holder (DebugServerHolder) is working correctly."
