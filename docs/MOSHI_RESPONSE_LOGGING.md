# Moshi Response Logging

## Overview

`MoshiResponseLoggingInterceptor` is a debug-only OkHttp interceptor that logs parsed Moshi response objects. It allows you to see deserialized `WykopApiResponseV3` models alongside raw JSON in Logcat, without using breakpoints.

## Purpose

When debugging API v3 integration:
- **Network Plugin** shows raw HTTP request/response in Flipper Desktop
- **HttpLoggingInterceptor** shows raw JSON in Logcat
- **MoshiResponseLoggingInterceptor** shows **parsed objects** in Logcat

This is useful for:
- Verifying that JSON is correctly deserialized into Kotlin models
- Inspecting the structure of `WykopApiResponseV3<T>` objects
- Debugging pagination (`PaginationResponseV3`)
- Understanding what data types are returned (List, single object, etc.)
- Catching parsing errors early

## How It Works

1. Intercepts all successful HTTP responses (2xx) to `/api/v3/*` endpoints
2. Clones the response body (preserves it for Retrofit)
3. Parses JSON as `WykopApiResponseV3<Any>` using Moshi
4. Logs parsed object structure to Logcat via Napier
5. Returns original response unchanged

## Architecture

### Debug Build Only

```
app/src/debug/kotlin/io/github/wykopmobilny/debug/
└── MoshiResponseLoggingInterceptor.kt    # Full implementation

app/src/release/kotlin/io/github/wykopmobilny/debug/
└── MoshiResponseLoggingInterceptor.kt    # Stub (no-op)
```

### Integration Point

Added to OkHttp pipeline in `RetrofitModule.kt`:

```kotlin
.apply {
    if (isDebug) {
        debugNetworkInterceptor?.let { addInterceptor(it) }  // Flipper
        addInterceptor(MoshiResponseLoggingInterceptor(moshi))  // Moshi logging
        addInterceptor(HttpLoggingInterceptor().apply {        // HTTP logging
            level = HttpLoggingInterceptor.Level.BODY
        })
    }
}
```

**Interceptor order**: Flipper → Moshi logging → HTTP logging

## Usage

### 1. Run Debug Build

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Filter Logcat

Use tag filter to see only Moshi logs:

```bash
adb logcat -s MoshiResponse
```

Or in Android Studio Logcat:
- Filter: `tag:MoshiResponse`

### 3. Trigger API Calls

Navigate through the app to trigger API v3 calls (entries, links, profile, etc.)

### 4. Read Logs

Example log output:

```
╔═══════════════════════════════════════════════════════════════
║ Moshi Parsed Response
╠═══════════════════════════════════════════════════════════════
║ Endpoint: /api/v3/entries
║ Data Type: ArrayList
║ Data Preview: List<EntryResponseV3>(25 items)
║ Pagination:
║   - per_page: 25
║   - total: 1234
║   - next: eyJwYWdlIjoyLCJsYXN0SWQiOjEyMzQ1fQ==
╠═══════════════════════════════════════════════════════════════
║ Parsed Object:
║ WykopApiResponseV3(data=[EntryResponseV3(id=123, content=...), ...], pagination=PaginationResponseV3(...))
╠═══════════════════════════════════════════════════════════════
║ Raw JSON (first 500 chars):
║ {"data":[{"id":123,"content":"Example entry",...
╚═══════════════════════════════════════════════════════════════
```

## Log Structure

Each log entry contains:

1. **Endpoint**: API path (e.g., `/api/v3/entries`)
2. **Data Type**: Kotlin class name of `response.data` (e.g., `ArrayList`, `EntryResponseV3`)
3. **Data Preview**:
   - For lists: `List<ItemType>(N items)`
   - For maps: `Map(N entries)`
   - For objects: First 100 chars of `toString()`
4. **Pagination**: `per_page`, `total`, `next` values (if present)
5. **Parsed Object**: Full `WykopApiResponseV3` object via `toString()`
6. **Raw JSON**: First 500 characters of original JSON

## Debugging Tips

### Find Specific Endpoint

```bash
adb logcat -s MoshiResponse | grep "/api/v3/entries"
```

### Find List Responses

```bash
adb logcat -s MoshiResponse | grep "Data Type: ArrayList"
```

### Find Pagination Issues

```bash
adb logcat -s MoshiResponse | grep "Pagination:"
```

### Combined with Network Plugin

1. Open Flipper Desktop → Network tab
2. Click on a request to see raw JSON
3. Check Logcat `MoshiResponse` tag to see parsed object
4. Compare raw JSON structure with Kotlin model

## Error Handling

If Moshi fails to parse a response:
- Logs warning: `Failed to parse response for /api/v3/... : [error]`
- Tag: `MoshiResponse`
- Original response is **not affected** (Retrofit receives unchanged body)

Common parse errors:
- Missing required fields in JSON
- Type mismatches (e.g., `String` vs `Int`)
- Unknown JSON fields (usually ignored by Moshi)
- Malformed JSON (rare)

## Performance Impact

- **Debug build**: Minimal overhead (~1-5ms per response)
  - Clones response body buffer (fast)
  - Parses JSON once (Moshi is fast with codegen)
  - Logging to Logcat (async)
- **Release build**: Zero overhead (stub implementation)

## Limitations

1. **Only API v3**: Filters paths starting with `/api/v3/`
2. **Only successful responses**: Ignores HTTP errors (4xx, 5xx)
3. **Generic parsing**: Parses as `WykopApiResponseV3<Any>`, not specific types
4. **Preview length**: Data preview truncated to 100 chars, raw JSON to 500 chars
5. **No Flipper UI**: Logs to Logcat only (not integrated with Flipper Desktop)

## Future Enhancements

### Option 1: Custom Flipper Plugin (Recommended)

Create a dedicated Flipper Desktop plugin for Moshi responses:

**Features**:
- Visual tree view of parsed objects
- Side-by-side comparison: raw JSON vs parsed model
- Filter by endpoint, data type, or response code
- Search within parsed objects
- Export to JSON/CSV

**Implementation**:
1. Android plugin (Java/Kotlin):
   - `MoshiFlipperPlugin` extends `FlipperPlugin`
   - Serializes `WykopApiResponseV3` to JSON
   - Sends events to Flipper Desktop
2. Desktop plugin (JavaScript/TypeScript):
   - Renders parsed objects in custom UI
   - Provides search/filter capabilities

**Estimated effort**: ~2-3 days

### Option 2: Enhanced Logging

Improve current interceptor:
- Pretty-print full JSON (not just 500 chars)
- Colorized output for different data types
- Configurable log levels (verbose, debug, info)
- Save logs to file for later analysis

**Estimated effort**: ~4-8 hours

### Option 3: Integration with Existing Plugins

Extend NetworkFlipperPlugin to show parsed objects:
- Add custom metadata field to network requests
- Serialize `WykopApiResponseV3` to JSON
- Display in Network plugin's "Response" tab

**Estimated effort**: ~1-2 days

## References

- [OkHttp Interceptors](https://square.github.io/okhttp/interceptors/)
- [Moshi Documentation](https://github.com/square/moshi)
- [Flipper Custom Plugins](https://fbflipper.com/docs/tutorial/intro/)
- Related docs:
  - `FLIPPER_INTEGRATION.md` - Main Flipper documentation
  - `ERROR_HANDLER_V3.md` - API v3 error handling

## Examples

See `docs/examples/MoshiResponseLoggingUsage.md` for practical examples.
