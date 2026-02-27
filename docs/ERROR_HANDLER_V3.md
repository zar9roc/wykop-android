# ErrorHandlerTransformerV3 Documentation

**Created:** 2026-02-27
**Purpose:** Dedicated error handling for Wykop API v3 responses

---

## Overview

`ErrorHandlerTransformerV3` is a dedicated transformer for handling API v3 responses. It unwraps `WykopApiResponseV3<T>` and extracts the `data` field, providing a clean separation from the legacy v2 error handling.

## Key Differences: v2 vs v3

### API v2 Response Format
```json
// Success
{
  "data": { ... },
  "error": null
}

// Error
{
  "data": null,
  "error": {
    "code": 401,
    "message_pl": "Nieprawidłowe dane"
  }
}
```

### API v3 Response Format
```json
// Success (HTTP 200)
{
  "data": { ... },
  "pagination": { ... }
}

// Error (HTTP 4xx/5xx)
{
  "code": 401,
  "hash": "abc123",
  "error": {
    "message": "Nieprawidłowe dane uwierzytelniające",
    "key": 1001
  }
}
```

**Key difference:** API v3 uses completely different structures for success and error responses. Errors are indicated by HTTP status codes (4xx/5xx), not by an `error` field in the response body.

## Components

### 1. ErrorHandlerTransformerV3

A `SingleTransformer` for RxJava that unwraps `WykopApiResponseV3<T>` to `T`.

**Usage:**
```kotlin
rxSingle { entriesApiV3.getStream(page) }
    .retryWhen(userTokenRefresher)
    .compose(ErrorHandlerTransformerV3<List<EntryResponseV3>>())
    .map { entries -> // entries is List<EntryResponseV3>, not WykopApiResponseV3
        filterEntriesV3(entries, owmContentFilter)
    }
```

**Before (incorrect):**
```kotlin
rxSingle { entriesApiV3.getStream(page) }
    .retryWhen(userTokenRefresher)
    .compose(ErrorHandlerTransformer()) // Wrong! Type inference fails
    .map { response -> // response is WykopApiResponseV3 - transformer didn't unwrap
        response.data.orEmpty() // Manual unwrapping needed
    }
```

### 2. unwrappingV3()

A suspend function for coroutines that unwraps `WykopApiResponseV3<T>` to `T`.

**Usage:**
```kotlin
suspend fun getEntry(id: Long): Entry {
    return unwrappingV3 {
        entriesApiV3.getEntry(id)
    }
}
```

## Error Handling

### HTTP Errors (4xx, 5xx)

HTTP errors are handled by:
1. **Retrofit** - throws `HttpException` for non-2xx responses
2. **UserTokenRefresher** - intercepts 401 errors and refreshes tokens
3. **Application code** - catches and handles specific exceptions

### Null Data

If API returns HTTP 200 but `data` is null (which shouldn't happen), both `ErrorHandlerTransformerV3` and `unwrappingV3()` throw `IllegalStateException`.

## Migration Guide

### For Repository Methods (RxJava)

**Old code:**
```kotlin
override fun getStream(page: Int) =
    rxSingle { entriesApiV3.getStream(page) }
        .retryWhen(userTokenRefresher)
        .compose(ErrorHandlerTransformer())
        .map { response ->
            filterEntriesV3(response.data.orEmpty(), owmContentFilter)
        }
```

**New code:**
```kotlin
override fun getStream(page: Int) =
    rxSingle { entriesApiV3.getStream(page) }
        .retryWhen(userTokenRefresher)
        .compose(ErrorHandlerTransformerV3<List<EntryResponseV3>>())
        .map { entries ->
            filterEntriesV3(entries, owmContentFilter)
        }
```

### For Suspend Functions

**Without unwrappingV3:**
```kotlin
suspend fun getEntry(id: Long): Entry {
    val response = entriesApiV3.getEntry(id)
    val entryResponse = response.data ?: throw IllegalStateException("Entry not found")
    return EntryMapperV3.map(entryResponse)
}
```

**With unwrappingV3:**
```kotlin
suspend fun getEntry(id: Long): Entry {
    val entryResponse = unwrappingV3 { entriesApiV3.getEntry(id) }
    return EntryMapperV3.map(entryResponse)
}
```

## Future Enhancements

### Error Body Parsing

Currently, HTTP errors throw generic `HttpException`. To provide better error messages:

1. Create `ErrorBodyParserV3` that parses `WykopErrorResponseV3` from error responses
2. Create custom exception type for API v3 errors (e.g., `WykopApiExceptionV3`)
3. Integrate with `UserTokenRefresher` to handle v3-specific error codes

Example structure:
```kotlin
interface ErrorBodyParserV3 {
    suspend fun parse(body: ResponseBody): WykopErrorResponseV3?
}

class WykopApiExceptionV3(
    val code: Int,
    val hash: String?,
    override val message: String,
    val key: String?
) : IOException()
```

## Files Modified

- `app/src/main/kotlin/io/github/wykopmobilny/api/errorhandler/ErrorHandlerTransformerV3.kt` - New file

## Related Files

- `app/src/main/kotlin/io/github/wykopmobilny/api/errorhandler/ErrorHandlerTransformer.kt` - v2 transformer
- `app/src/main/kotlin/io/github/wykopmobilny/api/UserTokenRefresher.kt` - Token refresh and error interception
- `data/wykop/api/src/main/kotlin/io/github/wykopmobilny/api/responses/v3/common/WykopApiResponseV3.kt` - v3 response model
- `data/wykop/api/src/main/kotlin/io/github/wykopmobilny/api/responses/v3/common/WykopErrorResponseV3.kt` - v3 error model

## References

- [WYKOP_API_V3_VALIDATION.md](WYKOP_API_V3_VALIDATION.md) - API v3 format validation
- [Task #39](../README.md) - Initial v3 migration (Entries and Links read endpoints)
