# Custom Flipper Plugin Proposal: Moshi Response Viewer

## Executive Summary

**Problem**: Current debugging tools show either raw JSON (NetworkFlipperPlugin, HttpLoggingInterceptor) or plain text logs (MoshiResponseLoggingInterceptor). There's no visual way to explore parsed WykopApiResponseV3 objects in Flipper Desktop.

**Solution**: Create a custom Flipper plugin that displays deserialized Moshi objects in a structured, interactive UI.

**Effort**: ~2-3 days (16-24 hours)

**Priority**: Medium-High (would significantly improve debugging experience for API v3 migration)

---

## Current State vs Proposed State

### Current Debugging Workflow

1. **Flipper Network Plugin**: Shows raw HTTP request/response
   - ✅ Good for inspecting headers, status codes, raw JSON
   - ❌ Hard to understand complex nested JSON structures
   - ❌ No type information (is it a List? Single object? null?)

2. **MoshiResponseLoggingInterceptor**: Logs parsed objects to Logcat
   - ✅ Shows Kotlin types and structure
   - ✅ Easy to filter by tag
   - ❌ Plain text output (hard to read nested objects)
   - ❌ No UI, no search, no comparison

3. **Breakpoints**: Manual debugging in Android Studio
   - ✅ Full access to object properties
   - ❌ Requires stopping execution
   - ❌ Tedious for repetitive checks

### Proposed Workflow

**Custom Flipper Plugin**: Visual Moshi Response Viewer in Flipper Desktop

- ✅ Interactive tree view of parsed objects
- ✅ Side-by-side: raw JSON vs deserialized Kotlin model
- ✅ Search and filter capabilities
- ✅ Type information and null handling
- ✅ Pagination details highlighted
- ✅ Historical view (all responses stored during session)
- ✅ Export to JSON/CSV

---

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Flipper Desktop                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Moshi Response Viewer Plugin (JavaScript/TypeScript)│  │
│  │  - Tree view UI                                       │  │
│  │  - Search/filter                                      │  │
│  │  - JSON comparison                                    │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────┘
                         │ WebSocket (Flipper Protocol)
                         │
┌────────────────────────┴────────────────────────────────────┐
│                   Android App (Debug)                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  MoshiFlipperPlugin (Kotlin)                          │  │
│  │  - FlipperPlugin implementation                       │  │
│  │  - Serializes WykopApiResponseV3 to JSON             │  │
│  │  - Sends events to Desktop                           │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  MoshiFlipperInterceptor (OkHttp)                     │  │
│  │  - Intercepts API v3 responses                        │  │
│  │  - Parses with Moshi                                  │  │
│  │  - Sends to MoshiFlipperPlugin                        │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **API Request**: OkHttpClient sends request to `/api/v3/*`
2. **Response**: Server returns JSON
3. **Interceptor**: `MoshiFlipperInterceptor` captures response
4. **Parse**: Moshi deserializes JSON to `WykopApiResponseV3<T>`
5. **Serialize**: Convert parsed object to JSON (for transport)
6. **Send**: `MoshiFlipperPlugin` sends event to Flipper Desktop via WebSocket
7. **Display**: Desktop plugin renders in UI

### Event Schema

```json
{
  "event": "MoshiResponse",
  "timestamp": 1678901234567,
  "endpoint": "/api/v3/entries",
  "method": "GET",
  "statusCode": 200,
  "dataType": "ArrayList<EntryResponseV3>",
  "itemCount": 25,
  "pagination": {
    "perPage": 25,
    "total": 1234,
    "next": "eyJwYWdlIjoyLCJsYXN0SWQiOjEyMzQ1fQ=="
  },
  "parsedObject": {
    // Serialized WykopApiResponseV3 as JSON
    "data": [...],
    "pagination": {...}
  },
  "rawJson": "{\"data\":[...],\"pagination\":{...}}",
  "parseTime": 12  // milliseconds
}
```

---

## Implementation Plan

### Phase 1: Android Plugin (8-12 hours)

#### 1.1 Create MoshiFlipperPlugin (4 hours)

**File**: `app/src/debug/kotlin/io/github/wykopmobilny/debug/MoshiFlipperPlugin.kt`

```kotlin
class MoshiFlipperPlugin(
    private val moshi: Moshi,
) : FlipperPlugin {
    private var connection: FlipperConnection? = null

    override fun getId() = "MoshiResponseViewer"

    override fun onConnect(connection: FlipperConnection) {
        this.connection = connection
    }

    override fun onDisconnect() {
        connection = null
    }

    override fun runInBackground() = true

    fun logResponse(
        endpoint: String,
        method: String,
        statusCode: Int,
        response: WykopApiResponseV3<*>,
        rawJson: String,
        parseTime: Long,
    ) {
        connection?.send("MoshiResponse", mapOf(
            "timestamp" to System.currentTimeMillis(),
            "endpoint" to endpoint,
            "method" to method,
            "statusCode" to statusCode,
            "dataType" to getDataType(response.data),
            "itemCount" to getItemCount(response.data),
            "pagination" to serializePagination(response.pagination),
            "parsedObject" to serializeObject(response),
            "rawJson" to rawJson,
            "parseTime" to parseTime,
        ))
    }

    private fun getDataType(data: Any?): String = when (data) {
        null -> "null"
        is List<*> -> "List<${data.firstOrNull()?.javaClass?.simpleName}>"
        else -> data.javaClass.simpleName
    }

    private fun getItemCount(data: Any?): Int? = (data as? List<*>)?.size

    private fun serializePagination(pagination: PaginationResponseV3?) = pagination?.let {
        mapOf(
            "perPage" to it.perPage,
            "total" to it.total,
            "next" to it.next,
        )
    }

    private fun serializeObject(obj: Any): Any {
        // Convert WykopApiResponseV3 to JSON-serializable map
        val adapter = moshi.adapter(obj.javaClass)
        val json = adapter.toJson(obj)
        return moshi.adapter<Map<String, Any>>(Map::class.java).fromJson(json) ?: emptyMap()
    }
}
```

#### 1.2 Create MoshiFlipperInterceptor (2 hours)

**File**: `app/src/debug/kotlin/io/github/wykopmobilny/debug/MoshiFlipperInterceptor.kt`

```kotlin
class MoshiFlipperInterceptor(
    private val moshi: Moshi,
    private val plugin: MoshiFlipperPlugin,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)

        if (!response.isSuccessful || !request.url.encodedPath.startsWith("/api/v3/")) {
            return response
        }

        try {
            val responseBody = response.body ?: return response
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer.clone()
            val bodyString = buffer.readUtf8()

            val adapter = moshi.adapter<WykopApiResponseV3<Any>>(
                createParameterizedType(WykopApiResponseV3::class.java, Any::class.java),
            )

            val parsedResponse = adapter.fromJson(bodyString)
            val parseTime = System.currentTimeMillis() - startTime

            if (parsedResponse != null) {
                plugin.logResponse(
                    endpoint = request.url.encodedPath,
                    method = request.method,
                    statusCode = response.code,
                    response = parsedResponse,
                    rawJson = bodyString,
                    parseTime = parseTime,
                )
            }

            return response.newBuilder()
                .body(bodyString.toResponseBody(responseBody.contentType()))
                .build()
        } catch (e: Exception) {
            return response
        }
    }
}
```

#### 1.3 Integrate with Dagger (2 hours)

- Add `MoshiFlipperPlugin` to `FlipperPluginHolder`
- Create interceptor in `FlipperInterceptorFactory`
- Update `FlipperInitializer` to register plugin
- Add to OkHttp chain in `RetrofitModule`

**Changes**:
- `FlipperPluginHolder.kt`: Add `var moshiPlugin: MoshiFlipperPlugin? = null`
- `FlipperInterceptorFactory.kt`: Add `createMoshiInterceptor(moshi: Moshi): Interceptor?`
- `FlipperInitializer.kt`: Initialize and register plugin
- `RetrofitModule.kt`: Add interceptor to chain

#### 1.4 Testing (4 hours)

- Unit tests for serialization logic
- Integration tests with mock responses
- Manual testing with real API calls
- Performance profiling (ensure <5ms overhead)

---

### Phase 2: Desktop Plugin (8-12 hours)

#### 2.1 Setup Plugin Scaffold (2 hours)

**Directory**: `flipper-plugin-moshi-viewer/`

```
flipper-plugin-moshi-viewer/
├── package.json
├── src/
│   ├── index.tsx          # Main plugin entry
│   ├── MoshiResponseTable.tsx
│   ├── MoshiResponseDetail.tsx
│   ├── JsonComparison.tsx
│   └── types.ts
└── README.md
```

**package.json**:
```json
{
  "name": "flipper-plugin-moshi-viewer",
  "version": "1.0.0",
  "main": "dist/bundle.js",
  "flipperBundlerEntry": "src/index.tsx",
  "dependencies": {
    "flipper": "^0.264.0",
    "react": "^18.0.0",
    "antd": "^5.0.0"
  }
}
```

#### 2.2 Implement UI Components (4 hours)

**Main Table** (`MoshiResponseTable.tsx`):
- List of all API responses
- Columns: Timestamp, Endpoint, Method, Status, Data Type, Items, Parse Time
- Click to expand details

**Detail View** (`MoshiResponseDetail.tsx`):
- Tree view of parsed object (expandable JSON tree)
- Pagination info (highlighted if present)
- Parse time and metadata

**JSON Comparison** (`JsonComparison.tsx`):
- Side-by-side view: Raw JSON (left) vs Parsed Object (right)
- Syntax highlighting
- Diff highlighting (if types mismatch)

#### 2.3 Features (2 hours)

- **Search**: Filter by endpoint, data type, or status code
- **Export**: Download selected responses as JSON/CSV
- **Clear**: Clear history
- **Auto-scroll**: Option to auto-scroll to latest response

#### 2.4 Testing & Polish (4 hours)

- Test with various response types (lists, single objects, null data)
- Error handling (parse failures, connection issues)
- Performance optimization (virtualized list for >1000 responses)
- Documentation

---

## User Interface Mockup

### Main View

```
╔═══════════════════════════════════════════════════════════════════════════════════╗
║ Moshi Response Viewer                                         [Clear] [Export]    ║
╠═══════════════════════════════════════════════════════════════════════════════════╣
║ Search: [/api/v3/entries                        ] Filter: [All ▼] Status: [2xx ▼] ║
╠══════════╦════════════════════════╦════════╦══════╦═══════════════════╦══════╦════╣
║ Time     ║ Endpoint               ║ Method ║ Code ║ Data Type         ║ Items║ ms ║
╠══════════╬════════════════════════╬════════╬══════╬═══════════════════╬══════╬════╣
║ 12:34:56 ║ /api/v3/entries        ║ GET    ║ 200  ║ List<EntryResp..> ║   25 ║  8 ║
║ 12:34:58 ║ /api/v3/links          ║ GET    ║ 200  ║ List<LinkRespo..> ║   50 ║ 12 ║
║ 12:35:01 ║ /api/v3/profile/user1  ║ GET    ║ 200  ║ ProfileResponse   ║   -  ║  5 ║
║ ...                                                                                 ║
╚═══════════════════════════════════════════════════════════════════════════════════╝
```

### Detail View (when row clicked)

```
╔═══════════════════════════════════════════════════════════════════════════════════╗
║ Response Details: /api/v3/entries                                    [Close]      ║
╠═══════════════════════════════════════════════════════════════════════════════════╣
║ Metadata                                                                          ║
║   Timestamp:  2024-03-08 12:34:56                                                 ║
║   Method:     GET                                                                 ║
║   Status:     200 OK                                                              ║
║   Data Type:  List<EntryResponseV3> (25 items)                                    ║
║   Parse Time: 8ms                                                                 ║
║                                                                                   ║
║ Pagination                                                                        ║
║   per_page: 25                                                                    ║
║   total:    1234                                                                  ║
║   next:     eyJwYWdlIjoyLCJsYXN0SWQiOjEyMzQ1fQ==                                  ║
╠═══════════════════════════════════════════════════════════════════════════════════╣
║ [Parsed Object] [Raw JSON] [Comparison]                                          ║
╠═══════════════════════════════════════════════════════════════════════════════════╣
║ WykopApiResponseV3                                                                ║
║   ├─ data: List<EntryResponseV3> (25)                                             ║
║   │   ├─ [0]: EntryResponseV3                                                     ║
║   │   │   ├─ id: 123                                                              ║
║   │   │   ├─ content: "Example entry text..."                                     ║
║   │   │   ├─ author: AuthorResponseV3                                             ║
║   │   │   │   ├─ username: "user123"                                              ║
║   │   │   │   ├─ avatar: "https://..."                                            ║
║   │   │   │   └─ color: 5                                                         ║
║   │   │   ├─ createdAt: 2024-03-08T12:30:00Z                                      ║
║   │   │   └─ media: null                                                          ║
║   │   ├─ [1]: ...                                                                 ║
║   └─ pagination: PaginationResponseV3                                             ║
║       ├─ perPage: 25                                                              ║
║       ├─ total: 1234                                                              ║
║       └─ next: "eyJwYWdlIjoyLCJsYXN0SWQiOjEyMzQ1fQ=="                             ║
╚═══════════════════════════════════════════════════════════════════════════════════╝
```

---

## Benefits

### For Developers

1. **Faster Debugging**: No need to set breakpoints or parse logs manually
2. **Better Understanding**: Visual tree view makes complex structures obvious
3. **Type Safety**: Immediately see what Kotlin types are parsed
4. **Pagination Debugging**: Quickly verify `next` hashes and page counts
5. **Historical View**: Compare multiple responses to spot patterns/bugs

### For QA

1. **Non-technical Testing**: QA can inspect API responses without code knowledge
2. **Bug Reports**: Export responses and attach to bug tickets
3. **Regression Testing**: Compare responses before/after code changes

### For API Migration

1. **v2 vs v3 Comparison**: Verify new models match expected structure
2. **Coverage Tracking**: See which endpoints are being tested
3. **Edge Cases**: Discover null data, empty lists, etc.

---

## Alternatives Considered

### Alternative 1: Extend NetworkFlipperPlugin

**Pros**:
- No new plugin to install
- Reuses existing Network UI

**Cons**:
- Network plugin not designed for parsed objects
- Hard to customize UI for Moshi-specific needs
- Would clutter existing Network view

**Verdict**: ❌ Not ideal

### Alternative 2: Enhance MoshiResponseLoggingInterceptor

**Pros**:
- Simpler implementation (just better logging)
- No Desktop plugin needed

**Cons**:
- Still plain text, no UI
- No search/filter capabilities
- No historical view

**Verdict**: ✅ Already implemented as stopgap

### Alternative 3: Custom Flipper Plugin (Proposed)

**Pros**:
- Best UX (visual, interactive, searchable)
- Dedicated UI for Moshi-specific features
- Extensible for future enhancements

**Cons**:
- More initial effort (~2-3 days)
- Requires Desktop plugin development

**Verdict**: ✅ **Recommended**

---

## Risks & Mitigations

### Risk 1: Performance Overhead

**Impact**: Serializing objects on every response could slow down app

**Mitigation**:
- Only serialize in debug builds
- Add toggle to enable/disable plugin
- Limit history size (e.g., keep last 500 responses)
- Use background thread for serialization

### Risk 2: Complex Serialization

**Impact**: Some objects might not serialize correctly (circular refs, custom types)

**Mitigation**:
- Use Moshi's built-in adapters (already configured)
- Add error handling for serialization failures
- Log errors to Logcat for debugging

### Risk 3: Desktop Plugin Complexity

**Impact**: Flipper Desktop plugin development has learning curve

**Mitigation**:
- Start with minimal UI (table + detail view)
- Follow official Flipper plugin examples
- Iterate based on feedback

---

## Timeline

| Phase | Task | Duration | Total |
|-------|------|----------|-------|
| **Phase 1: Android** | | | **12h** |
| 1.1 | MoshiFlipperPlugin | 4h | |
| 1.2 | MoshiFlipperInterceptor | 2h | |
| 1.3 | Dagger integration | 2h | |
| 1.4 | Testing | 4h | |
| **Phase 2: Desktop** | | | **12h** |
| 2.1 | Plugin scaffold | 2h | |
| 2.2 | UI components | 4h | |
| 2.3 | Features (search/export) | 2h | |
| 2.4 | Testing & polish | 4h | |
| **Total** | | | **24h** |

**Best case**: 16 hours (2 days)
**Expected**: 24 hours (3 days)
**Worst case**: 32 hours (4 days)

---

## Success Criteria

1. ✅ Plugin appears in Flipper Desktop plugin list
2. ✅ All API v3 responses are logged and displayed
3. ✅ Parse time <10ms per response (on average)
4. ✅ UI is responsive with 1000+ logged responses
5. ✅ Search and filter work correctly
6. ✅ Export to JSON succeeds
7. ✅ No crashes or errors in production
8. ✅ Documentation complete with examples

---

## Next Steps

### Immediate (Current Solution)

- ✅ **DONE**: MoshiResponseLoggingInterceptor (Logcat-based)
- ✅ **DONE**: Documentation (`MOSHI_RESPONSE_LOGGING.md`)
- ✅ **DONE**: Usage examples

### Short Term (Next Sprint)

- Gather feedback on MoshiResponseLoggingInterceptor
- Identify most painful debugging scenarios
- Prioritize custom plugin features

### Medium Term (Next Month)

- **Implement custom Flipper plugin** (if feedback is positive)
- Beta test with development team
- Iterate based on usage patterns

### Long Term (Future)

- Add more visualization features (graphs, charts)
- Integration with other debugging tools (Stetho, Chuck)
- Public release as open-source Flipper plugin

---

## References

- [Flipper Plugin Development Guide](https://fbflipper.com/docs/tutorial/intro/)
- [Example Plugins](https://github.com/facebook/flipper/tree/main/desktop/plugins)
- [FlipperPlugin API](https://fbflipper.com/docs/extending/flipper-plugin/)
- Related docs:
  - `FLIPPER_INTEGRATION.md` - Main Flipper integration
  - `MOSHI_RESPONSE_LOGGING.md` - Current Logcat solution
  - `ERROR_HANDLER_V3.md` - API v3 error handling

---

## Appendix: Code Snippets

### Example Usage in Desktop Plugin

```typescript
// index.tsx
import { PluginClient, usePlugin, createState } from 'flipper-plugin';

type MoshiResponse = {
  timestamp: number;
  endpoint: string;
  method: string;
  statusCode: number;
  dataType: string;
  itemCount?: number;
  pagination?: {
    perPage: number;
    total: number;
    next?: string;
  };
  parsedObject: any;
  rawJson: string;
  parseTime: number;
};

export function plugin(client: PluginClient) {
  const responses = createState<MoshiResponse[]>([]);

  client.onMessage('MoshiResponse', (payload: MoshiResponse) => {
    responses.update(draft => {
      draft.unshift(payload);
      if (draft.length > 500) draft.pop(); // Keep last 500
    });
  });

  return { responses };
}

export function Component() {
  const instance = usePlugin(plugin);
  const responses = instance.responses.get();

  return (
    <MoshiResponseTable responses={responses} />
  );
}
```

---

**Status**: Proposal (Ready for Review)
**Created**: 2024-03-08
**Author**: Claude Code
**Reviewers**: Wykop Android Team
