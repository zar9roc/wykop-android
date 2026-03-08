# Flipper Integration

## Overview

Flipper is a platform for debugging Android and iOS apps, developed by Meta (Facebook). This document describes the integration of Flipper in the wykop-android project for debug builds only.

## Features Integrated

### 1. Network Plugin (FlipperOkhttpInterceptor)
- **Location**: `RetrofitModule.kt`
- **Purpose**: Monitors all HTTP requests/responses made through Retrofit
- **Features**:
  - Request/response inspection
  - Headers inspection
  - Body inspection (JSON, form data, etc.)
  - Timing information
  - Error tracking

### 2. Layout Inspector Plugin
- **Purpose**: Inspect and modify the view hierarchy in real-time
- **Features**:
  - View tree visualization
  - Property inspection
  - Real-time layout debugging

### 3. SharedPreferences Plugin
- **Purpose**: View and edit SharedPreferences data
- **Features**:
  - View all shared preferences
  - Edit values in real-time
  - Monitor changes

### 4. Database Plugin (SQLDelight)
- **Database**: `wykop_storage.sqlite`
- **Purpose**: Inspect SQLite database
- **Features**:
  - View all tables
  - Execute SQL queries
  - Browse data
  - Monitor database operations

### 5. LeakCanary Plugin
- **Purpose**: Integration with LeakCanary for memory leak detection
- **Features**:
  - View leak traces in Flipper
  - Better visualization of memory leaks
  - Heap dump analysis

## Architecture

### Debug Build Only

Flipper is integrated ONLY in debug builds. The architecture ensures zero impact on release builds:

1. **Build Variants**:
   - `app/src/debug/` - Contains Flipper implementation
   - `app/src/release/` - Contains stub implementation

2. **Key Files**:
   ```
   app/src/debug/kotlin/io/github/wykopmobilny/
   ├── debug/
   │   ├── FlipperPluginHolder.kt         # Holds NetworkFlipperPlugin
   │   └── FlipperInterceptorFactory.kt   # Creates FlipperOkhttpInterceptor
   └── initializers/
       └── FlipperInitializer.kt          # Initializes Flipper with all plugins

   app/src/release/kotlin/io/github/wykopmobilny/debug/
   ├── FlipperPluginHolder.kt             # Stub (returns null)
   └── FlipperInterceptorFactory.kt       # Stub (returns null)
   ```

3. **Dependencies**:
   - All Flipper dependencies are declared as `debugImplementation` in `app/build.gradle`
   - No Flipper code is included in release APK

### Initialization Flow

1. **App Startup** (debug build only):
   - `FlipperInitializer` runs via androidx.startup
   - SoLoader initializes native libraries
   - AndroidFlipperClient creates and starts
   - All plugins are added and configured
   - NetworkFlipperPlugin is stored in `FlipperPluginHolder`

2. **Network Interceptor Setup**:
   - `WykopApp` calls `FlipperInterceptorFactory.create()`
   - Factory retrieves NetworkFlipperPlugin from holder
   - Creates `FlipperOkhttpInterceptor` with the plugin
   - Passes interceptor to `WykopComponent.Factory`
   - `RetrofitModule` adds interceptor to OkHttpClient (debug only)

3. **Release Build**:
   - Stub implementations return `null`
   - No interceptor is added to OkHttpClient
   - Zero overhead

## Dependencies

Added to `gradle/libs.versions.toml`:
```toml
[versions]
mavencentral-flipper = "0.264.0"
mavencentral-soloader = "0.12.1"

[libraries]
flipper-core = { module = "com.facebook.flipper:flipper", version.ref = "mavencentral-flipper" }
flipper-network = { module = "com.facebook.flipper:flipper-network-plugin", version.ref = "mavencentral-flipper" }
flipper-leakcanary = { module = "com.facebook.flipper:flipper-leakcanary2-plugin", version.ref = "mavencentral-flipper" }
soloader-core = { module = "com.facebook.soloader:soloader", version.ref = "mavencentral-soloader" }
```

Added to `app/build.gradle`:
```gradle
debugImplementation(libs.flipper.core)
debugImplementation(libs.flipper.network)
debugImplementation(libs.flipper.leakcanary)
debugImplementation(libs.soloader.core)
```

## Modified Files

### New Files
1. `app/src/debug/kotlin/io/github/wykopmobilny/initializers/FlipperInitializer.kt`
2. `app/src/debug/kotlin/io/github/wykopmobilny/debug/FlipperPluginHolder.kt`
3. `app/src/debug/kotlin/io/github/wykopmobilny/debug/FlipperInterceptorFactory.kt`
4. `app/src/release/kotlin/io/github/wykopmobilny/debug/FlipperPluginHolder.kt` (stub)
5. `app/src/release/kotlin/io/github/wykopmobilny/debug/FlipperInterceptorFactory.kt` (stub)

### Modified Files
1. `gradle/libs.versions.toml` - Added Flipper versions and libraries
2. `app/build.gradle` - Added debugImplementation dependencies
3. `app/src/debug/AndroidManifest.xml` - Added FlipperInitializer to startup
4. `app/src/main/kotlin/io/github/wykopmobilny/WykopApp.kt` - Added FlipperInterceptorFactory import and usage
5. `data/wykop/remote/src/main/kotlin/io/github/wykopmobilny/wykop/remote/WykopComponent.kt` - Added debugNetworkInterceptor parameter
6. `data/wykop/remote/src/main/kotlin/io/github/wykopmobilny/wykop/remote/Qualifiers.kt` - Added @DebugNetworkInterceptor qualifier
7. `data/wykop/remote/src/main/kotlin/io/github/wykopmobilny/wykop/remote/RetrofitModule.kt` - Added debugNetworkInterceptor to retrofit provider

## Usage

### 1. Install Flipper Desktop
Download from: https://fbflipper.com/

### 2. Run Debug Build
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Connect to Flipper
1. Open Flipper Desktop
2. Connect your device/emulator via USB
3. Enable USB debugging on device
4. Accept RSA key fingerprint if prompted
5. Your app should appear in Flipper's device list

### 4. Use Plugins
- **Network**: Click on Network tab to see all HTTP requests
- **Layout**: Click on Layout to inspect view hierarchy
- **Preferences**: Click on Shared Preferences to view/edit preferences
- **Database**: Click on Databases to browse SQLite data
- **LeakCanary**: View memory leaks in Flipper UI

## Troubleshooting

### Flipper doesn't see the app
1. Make sure you're running a debug build (not release)
2. Check USB debugging is enabled
3. Try restarting adb: `adb kill-server && adb start-server`
4. Check Flipper logs in Desktop app

### Network requests not showing
1. Verify FlipperInitializer is running (check Logcat for "Flipper initialized successfully")
2. Verify interceptor is added (check RetrofitModule)
3. Make sure NetworkFlipperPlugin is not null in FlipperPluginHolder

### Database not showing
1. Verify database file exists: `wykop_storage.sqlite`
2. Check database path in WykopApp.kt
3. Flipper requires the app to be running to access the database

## Performance Impact

- **Debug build**: Minimal overhead (network logging, view inspection)
- **Release build**: Zero overhead (no Flipper code included)

## Future Enhancements

Potential plugins to add:
- Crash Reporter plugin
- Images plugin (for image loading debugging)
- Redux/State Management plugin (if applicable)
- Custom plugins for Wykop-specific debugging

## References

- [Flipper Documentation](https://fbflipper.com/docs/)
- [Network Plugin](https://fbflipper.com/docs/features/plugins/network/)
- [Layout Inspector](https://fbflipper.com/docs/features/plugins/inspector/)
- [Databases Plugin](https://fbflipper.com/docs/features/plugins/databases/)
