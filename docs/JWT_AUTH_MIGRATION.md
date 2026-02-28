# JWT Authentication Migration (Faza 0)

## Podsumowanie

Implementacja JWT (JSON Web Token) autentykacji dla Wykop API v3 jako fundament dla dalszej migracji. System JWT zastępuje stary mechanizm autentykacji (userKey + accountKey + MD5 signing) dla endpointów v3.

**Status**: ✅ Zaimplementowano (Faza 0 - Fundament)

**Data**: 2026-02-27

## Architektura JWT Authentication

### Flow logowania:
1. Użytkownik podaje username + password
2. POST `/v3/auth` → zwraca `{ token, refresh_token, expires_in }`
3. JWT zapisywane w JwtTokenStorage (DataStore)
4. Wszystkie requesty v3 (oprócz `/v3/auth`) dostają header `Authorization: Bearer {token}`

### Auto-refresh tokenu:
1. Przy 401 Unauthorized, TokenRefreshAuthenticator automatycznie:
   - Wywołuje POST `/v3/auth/refresh` z refresh_token
   - Zapisuje nowy token
   - Retryuje oryginalny request z nowym tokenem
2. Jeśli refresh się nie uda, czyści tokeny (wymaga re-loginu)

## Komponenty

### 1. Modele (data/wykop/api)

**AuthResponseV3.kt** - Response z endpointu `/auth`:
```kotlin
@JsonClass(generateAdapter = true)
data class AuthResponseV3(
    @field:Json(name = "token") val token: String,
    @field:Json(name = "refresh_token") val refreshToken: String,
    @field:Json(name = "expires_in") val expiresIn: Long,
)
```

**AuthRequestV3.kt** - Request do `/auth`:
```kotlin
@JsonClass(generateAdapter = true)
data class AuthRequestV3(
    @field:Json(name = "username") val username: String,
    @field:Json(name = "password") val password: String,
)
```

**RefreshTokenRequestV3.kt** - Request do `/auth/refresh`:
```kotlin
@JsonClass(generateAdapter = true)
data class RefreshTokenRequestV3(
    @field:Json(name = "refresh_token") val refreshToken: String,
)
```

### 2. Storage (data/storage/api + android)

**JwtTokenStorage.kt** - Interface dla przechowywania JWT:
```kotlin
interface JwtTokenStorage {
    val jwtToken: Flow<JwtToken?>
    suspend fun updateJwtToken(value: JwtToken?)
}

data class JwtToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
)
```

**CredentialsPreferences.kt** - Implementacja z DataStore:
- Klucze: `jwtAccessToken`, `jwtRefreshToken`, `jwtExpiresAt`
- Storage typu: String, String, Long
- Automatyczne clear przy null

### 3. API Endpoints (data/wykop/api)

**AuthV3RetrofitApi.kt**:
```kotlin
interface AuthV3RetrofitApi {
    @POST("/v3/auth")
    suspend fun authenticate(@Body request: AuthRequestV3): WykopApiResponseV3<AuthResponseV3>

    @POST("/v3/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequestV3): WykopApiResponseV3<AuthResponseV3>
}
```

### 4. Interceptors & Authenticators (data/wykop/remote)

**JwtAuthInterceptor.kt**:
- Dodaje `Authorization: Bearer {token}` header dla wszystkich requestów v3
- Skip dla `/v3/auth` (login endpoint)
- Jeśli brak tokenu, request przechodzi bez headera

**TokenRefreshAuthenticator.kt**:
- Obsługuje 401 Unauthorized dla endpointów v3
- Automatycznie odświeża token przez `/v3/auth/refresh`
- Retry oryginalnego requesta z nowym tokenem
- Synchronizacja (unika race conditions przy wielu requestach)
- Clear tokenów jeśli refresh się nie uda

### 5. UserManager (app)

Rozszerzono o metody JWT:
```kotlin
interface UserManagerApi {
    suspend fun saveJwtCredentials(username: String, authResponse: AuthResponseV3)
    suspend fun getJwtToken(): JwtToken?
    suspend fun isJwtAuthorized(): Boolean
}
```

## Zasady

### 1. Rozróżnienie API v1/v2 vs v3
- **v1/v2**: Stary signing (userKey + accountKey + MD5 apisign header)
- **v3**: JWT Bearer token w Authorization header
- Oba systemy działają równolegle (backward compatibility)

### 2. Token Lifecycle
- **Access Token**: Krótkotrwały (expiresIn sekundy), używany w requestach
- **Refresh Token**: Długotrwały, służy do odnawiania access tokenu
- **expiresAt**: Timestamp wygaśnięcia (currentTimeMillis + expiresIn * 1000)

### 3. Interceptor Order (w Retrofit)
1. `PathFixingInterceptor` - Zamienia APP_KEY placeholder na prawdziwy klucz
2. `JwtAuthInterceptor` - Dodaje Bearer token dla v3
3. `SigningInterceptor` - Dodaje MD5 signing dla v1/v2
4. `TokenRefreshAuthenticator` - Auto-refresh przy 401

### 4. Thread Safety
- `TokenRefreshAuthenticator` używa `synchronized(this)` dla uniknięcia race conditions
- `runBlocking` w interceptorach (OkHttp nie obsługuje suspend)

### 5. Error Handling
- 401 na v3 (oprócz `/v3/auth`) → Auto-refresh
- Refresh failed → Clear wszystkich tokenów
- Brak tokenu → Request bez Authorization header (publiczne endpointy)

## Zmienione pliki

### Utworzone pliki:
- `data/wykop/api/src/main/.../responses/v3/auth/AuthResponseV3.kt`
- `data/wykop/api/src/main/.../requests/v3/auth/AuthRequestV3.kt`
- `data/wykop/api/src/main/.../requests/v3/auth/RefreshTokenRequestV3.kt`
- `data/wykop/api/src/main/.../endpoints/v3/AuthV3RetrofitApi.kt`
- `data/storage/api/src/main/.../storage/api/JwtTokenStorage.kt`
- `data/wykop/remote/src/main/.../wykop/remote/JwtAuthInterceptor.kt`
- `data/wykop/remote/src/main/.../wykop/remote/TokenRefreshAuthenticator.kt`
- `docs/JWT_AUTH_MIGRATION.md`

### Zmodyfikowane pliki:
- `data/storage/api/src/main/.../storage/api/Storages.kt` - Dodano `jwtTokenStorage()`
- `data/storage/android/src/main/.../storage/android/CredentialsPreferences.kt` - Implementacja JwtTokenStorage
- `data/storage/android/src/main/.../storage/android/StoragesComponent.kt` - Binding JwtTokenStorage
- `data/wykop/remote/src/main/.../wykop/remote/Qualifiers.kt` - Dodano `@JwtAuthInterceptor`
- `data/wykop/remote/src/main/.../wykop/remote/RetrofitModule.kt` - Provider + integracja z Retrofit
- `data/wykop/remote/src/main/.../wykop/remote/WykopModule.kt` - Provider dla AuthV3RetrofitApi
- `data/wykop/api/src/main/.../api/WykopApi.kt` - Dodano `authV3RetrofitApi()`
- `app/src/main/.../utils/usermanager/UserManager.kt` - Metody JWT

## Użycie

### Logowanie z JWT:
```kotlin
class LoginViewModel @Inject constructor(
    private val authApi: AuthV3RetrofitApi,
    private val userManager: UserManagerApi,
) {
    suspend fun login(username: String, password: String) {
        val response = authApi.authenticate(
            AuthRequestV3(username, password)
        )

        if (response.data != null) {
            userManager.saveJwtCredentials(username, response.data)
            // Użytkownik zalogowany, token zapisany
        } else {
            // Obsługa błędu (response.error)
        }
    }
}
```

### Wywołanie endpointu v3 z JWT:
```kotlin
// Token jest automatycznie dodawany przez JwtAuthInterceptor
val links = linksV3Api.getPromoted(page = 1)
// Header: Authorization: Bearer eyJhbGc...
```

### Auto-refresh przy 401:
```kotlin
// Użytkownik nie musi się martwić - TokenRefreshAuthenticator
// automatycznie odświeża token i retryuje request
val entries = entriesV3Api.getObserved(page = 1)
// Jeśli token wygasł:
// 1. 401 Unauthorized
// 2. Auto-refresh przez /v3/auth/refresh
// 3. Retry z nowym tokenem
// 4. Success
```

### Wylogowanie:
```kotlin
userManager.logoutUser()
// Czyści: sessionStorage, userInfoStorage, jwtTokenStorage
```

## Faza 2 - User Profile Endpoint (✅ COMPLETED 2026-02-28)

### Zaimplementowano:
1. ✅ **UserMeResponseV3** - Model response dla endpointu `/v3/users/me`
2. ✅ **UsersV3RetrofitApi** - Nowy interfejs Retrofit z metodą `getUserProfile()`
3. ✅ **Integracja w UserManager** - `saveJwtCredentials()` teraz pobiera profil po logowaniu
4. ✅ **Zapis profilu** - Automatyczne zapisywanie do `UserInfoStorage` z JWT flow

### Zmienione pliki:
- `data/wykop/api/.../responses/v3/user/UserMeResponseV3.kt` - Created
- `data/wykop/api/.../endpoints/v3/UsersV3RetrofitApi.kt` - Created
- `data/wykop/api/.../WykopApi.kt` - Dodano `usersV3RetrofitApi()`
- `data/wykop/remote/.../WykopModule.kt` - Dodano provider dla UsersV3RetrofitApi
- `app/.../utils/usermanager/UserManager.kt` - Pobieranie profilu po logowaniu JWT

### Flow po logowaniu JWT:
1. Użytkownik loguje się przez `/v3/auth` → otrzymuje token
2. Token zapisywany do `JwtTokenStorage`
3. **NOWE**: Automatyczne wywołanie `/v3/users/me` dla pobrania profilu
4. Profil zapisywany do `UserInfoStorage` (z pustym `userToken` - legacy field)
5. Użytkownik w pełni zalogowany z profilem

## Ograniczenia i TODO

### Obecne ograniczenia:
1. ⚠️ `isJwtAuthorized()` nie sprawdza expiration - tylko czy token istnieje
2. ⚠️ Stary system autentykacji (v1/v2) nadal aktywny - równoległa praca
3. ⚠️ `userToken` w `LoggedUserInfo` jest pusty dla JWT flow (legacy field)

### TODO (kolejne fazy):
- ~~**Faza 1**: Utworzyć LoginV3Activity z formularzem username/password~~ ✅ COMPLETED
- ~~**Faza 2**: Dodać endpoint GET `/v3/users/me` i pobieranie profilu po logowaniu JWT~~ ✅ COMPLETED
- **Faza 3**: Sprawdzanie expiration w `isJwtAuthorized()` (currentTime < expiresAt)
- **Faza 4**: Migracja wszystkich endpointów zapisu (POST/PUT/DELETE) na v3
- **Faza 5**: Deprecation starego systemu autentykacji (po pełnej migracji)

## Referencje

### Standardy:
- RFC 7519: JSON Web Token (JWT) - https://tools.ietf.org/html/rfc7519
- RFC 6750: Bearer Token Usage - https://tools.ietf.org/html/rfc6750

### Dokumentacja:
- OkHttp Interceptors - https://square.github.io/okhttp/interceptors/
- OkHttp Authenticator - https://square.github.io/okhttp/4.x/okhttp/okhttp3/-authenticator/
- Retrofit - https://square.github.io/retrofit/
- Moshi - https://github.com/square/moshi

### Projekt:
- ERROR_HANDLER_V3.md - Obsługa błędów dla API v3
- WYKOP_API_V3_MIGRATION_PLAN.md - Plan pełnej migracji na v3
