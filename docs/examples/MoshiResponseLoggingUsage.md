# Moshi Response Logging - Usage Examples

This document provides practical examples of using `MoshiResponseLoggingInterceptor` for debugging API v3 responses.

## Example 1: Debugging Entries List

### Scenario
You're implementing the entries feed and want to verify that `EntryResponseV3` objects are correctly parsed.

### Steps

1. **Run debug build**:
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Start Logcat with filter**:
   ```bash
   adb logcat -s MoshiResponse
   ```

3. **Navigate to Entries screen** in the app

4. **Check log output**:
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
   ║ WykopApiResponseV3(data=[EntryResponseV3(id=123, content=...), ...], pagination=...)
   ╚═══════════════════════════════════════════════════════════════
   ```

### What to Verify

- ✅ Data Type is `ArrayList` (not null or wrong type)
- ✅ Data Preview shows correct count (25 items)
- ✅ Pagination has `next` hash for loading more pages
- ✅ Parsed Object contains `EntryResponseV3` instances

---

## Example 2: Debugging Pagination Issues

### Scenario
Users report that infinite scroll doesn't work - same entries are loaded repeatedly.

### Steps

1. **Filter for pagination in logs**:
   ```bash
   adb logcat -s MoshiResponse | grep "Pagination:"
   ```

2. **Scroll down to trigger page 2**

3. **Compare logs**:

   **First request** (page 1):
   ```
   ║ Endpoint: /api/v3/entries
   ║ Pagination:
   ║   - per_page: 25
   ║   - total: 1234
   ║   - next: eyJwYWdlIjoyLCJsYXN0SWQiOjEyMzQ1fQ==
   ```

   **Second request** (page 2):
   ```
   ║ Endpoint: /api/v3/entries?page=eyJwYWdlIjoyLCJsYXN0SWQiOjEyMzQ1fQ%3D%3D
   ║ Pagination:
   ║   - per_page: 25
   ║   - total: 1234
   ║   - next: eyJwYWdlIjozLCJsYXN0SWQiOjY3ODkwfQ==
   ```

### What to Check

- ✅ `next` hash changes on each page (indicates new data)
- ✅ `page` parameter in URL matches previous `next` value
- ❌ If `next` is null or same value → pagination broken

### Fix

If pagination is broken:
1. Check `EntriesRepository.getEntries()` - verify `page` parameter is passed
2. Check presenter - verify `pagination.next` is used for subsequent requests
3. See memory rule: *"NIE polegać wyłącznie na pagination.next - fallback pageNumber"*

---

## Example 3: Debugging Links vs Entries (Mixed Content)

### Scenario
Implementing favourites screen that returns both links and entries.

### Steps

1. **Filter for favourites endpoint**:
   ```bash
   adb logcat -s MoshiResponse | grep "/api/v3/favourites"
   ```

2. **Navigate to Favourites screen**

3. **Check parsed type**:
   ```
   ╔═══════════════════════════════════════════════════════════════
   ║ Endpoint: /api/v3/favourites
   ║ Data Type: ArrayList
   ║ Data Preview: List<ObservedItemV3>(15 items)
   ║ Parsed Object:
   ║ WykopApiResponseV3(data=[
   ║   ObservedItemV3.LinkItem(link=LinkResponseV3(...)),
   ║   ObservedItemV3.EntryItem(entry=EntryResponseV3(...)),
   ║   ObservedItemV3.LinkItem(link=LinkResponseV3(...)),
   ║   ...
   ║ ], pagination=...)
   ╚═══════════════════════════════════════════════════════════════
   ```

### What to Verify

- ✅ Data Type is `ArrayList` of `ObservedItemV3`
- ✅ Mix of `LinkItem` and `EntryItem` present
- ✅ Each item has correct nested type (`LinkResponseV3` or `EntryResponseV3`)

### Adapter Implementation

Based on parsed structure, implement adapter:

```kotlin
class FavouritesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ObservedItemV3.LinkItem -> VIEW_TYPE_LINK
        is ObservedItemV3.EntryItem -> VIEW_TYPE_ENTRY
    }

    // ... rest of adapter
}
```

---

## Example 4: Debugging API v3 Delete Operations

### Scenario
Testing entry deletion - API returns 204 No Content with empty body.

### Steps

1. **Filter for delete endpoint**:
   ```bash
   adb logcat -s MoshiResponse | grep "/api/v3/entries"
   ```

2. **Delete an entry**

3. **Check if log appears**:
   - ✅ **No log** → Correct! (204 responses have no body to parse)
   - ❌ **Parse error** → Retrofit expecting non-null body

### Expected Behavior

Delete endpoints return `WykopApiResponseV3<Unit>?` (nullable):
```kotlin
@DELETE("v3/entries/{entryId}")
suspend fun deleteEntry(
    @Path("entryId") entryId: Int,
): WykopApiResponseV3<Unit>?  // Nullable!
```

If you see parse errors, check:
1. Return type is nullable (`WykopApiResponseV3<Unit>?`)
2. Repository handles null response correctly

---

## Example 5: Combined Debugging (Flipper + Moshi Logs)

### Scenario
Entry content is displayed incorrectly - suspect parsing error in nested fields.

### Steps

1. **Open Flipper Desktop** → Network tab
2. **Start Logcat**: `adb logcat -s MoshiResponse`
3. **Trigger API call** (open entry)
4. **In Flipper**: Click request → Response tab → see raw JSON:
   ```json
   {
     "data": {
       "id": 123,
       "content": "Test entry",
       "author": {
         "username": "user123",
         "avatar": "https://...",
         "color": 5
       },
       "media": {
         "photo": {
           "url": "https://...",
           "key": "abc123"
         }
       }
     }
   }
   ```

5. **In Logcat**: Check parsed object:
   ```
   ║ Parsed Object:
   ║ WykopApiResponseV3(data=EntryResponseV3(
   ║   id=123,
   ║   content=Test entry,
   ║   author=AuthorResponseV3(username=user123, avatar=https://..., color=5),
   ║   media=MediaResponseV3(photo=PhotoResponseV3(url=https://..., key=abc123))
   ║ ))
   ```

### What to Compare

- ✅ JSON field names match Kotlin property names
- ✅ Nested objects (`author`, `media`) are correctly deserialized
- ✅ Types match (numbers, strings, nulls)
- ❌ If field is missing in parsed object → check `@Json(name = "...")` annotation

---

## Example 6: Debugging Custom Adapters

### Scenario
Testing `ObservedItemV3Adapter` - polymorphic JSON parsing for links/entries.

### Steps

1. **Add debug logs to adapter**:
   ```kotlin
   class ObservedItemV3Adapter {
       override fun fromJson(reader: JsonReader): ObservedItemV3? {
           val resource = reader.peekJson().use { peek ->
               // ... existing code
           }
           Napier.d("Parsed resource type: $resource", tag = "ObservedItemV3Adapter")
           // ... rest of adapter
       }
   }
   ```

2. **Run app and check Logcat**:
   ```bash
   adb logcat -s "MoshiResponse|ObservedItemV3Adapter"
   ```

3. **Compare adapter logs with MoshiResponseLoggingInterceptor**:

   **Adapter log**:
   ```
   D/ObservedItemV3Adapter: Parsed resource type: link
   D/ObservedItemV3Adapter: Parsed resource type: entry
   ```

   **Interceptor log**:
   ```
   ║ Data Preview: List<ObservedItemV3>(2 items)
   ║ Parsed Object: [LinkItem(...), EntryItem(...)]
   ```

### What to Verify

- ✅ Adapter recognizes correct resource type
- ✅ Final parsed object has correct sealed class variant
- ✅ All items in list are parsed (no nulls)

---

## Example 7: Debugging Null Data

### Scenario
App crashes with `IllegalStateException: API v3 response contains null data`.

### Steps

1. **Check MoshiResponse logs**:
   ```bash
   adb logcat -s MoshiResponse
   ```

2. **Look for warning**:
   ```
   W/MoshiResponse: Failed to parse response for /api/v3/entries/123: API v3 response contains null data
   ```

3. **Check Flipper Network tab** → find request → Response:
   ```json
   {
     "data": null,
     "pagination": null
   }
   ```

### Possible Causes

- ❌ Entry was deleted (404 should be returned, but API returns 200 with null data)
- ❌ User is blocked/banned
- ❌ Content is hidden (NSFW, adult, etc.)
- ❌ API bug

### Fix

Add null handling in repository:
```kotlin
suspend fun getEntry(id: Int): EntryResponseV3? {
    return try {
        val response = api.getEntry(id)
        response.data  // May be null
    } catch (e: HttpException) {
        null
    }
}
```

---

## Troubleshooting

### No logs appear

**Cause**: MoshiResponseLoggingInterceptor only logs API v3 responses (2xx).

**Check**:
1. Is the endpoint `/api/v3/*`?
2. Is the response successful (not 4xx/5xx)?
3. Are you running debug build?

### Parse errors for all responses

**Cause**: Moshi adapter mismatch.

**Check**:
1. All v3 models have `@JsonClass(generateAdapter = true)`
2. RetrofitModule moshi() includes all custom adapters
3. No conflicting adapters

### Logs show wrong data type

**Cause**: Generic parsing as `WykopApiResponseV3<Any>`.

**Expected**: Interceptor logs generic type for simplicity. Check `toString()` output for actual types.

---

## Tips

1. **Use combined filters**:
   ```bash
   adb logcat -s "MoshiResponse" | grep "EntryResponseV3"
   ```

2. **Save logs to file**:
   ```bash
   adb logcat -s MoshiResponse > moshi_logs.txt
   ```

3. **Compare before/after changes**:
   - Save logs before code change
   - Save logs after code change
   - Diff the files

4. **Search for specific IDs**:
   ```bash
   adb logcat -s MoshiResponse | grep "id=123"
   ```

---

## Next Steps

After debugging with logs, consider:
1. Write unit tests for Moshi adapters
2. Add instrumentation tests for API responses
3. Document edge cases in code comments
4. Propose custom Flipper plugin for better UX (see `MOSHI_RESPONSE_LOGGING.md`)
