# Debug HTTP Server

Wbudowany serwer HTTP w debug buildzie, udostępniający stan aplikacji i akcje sterujące przez REST API.
Zamiennik workflow `am broadcast` + parsowanie logcat na prosty `curl localhost:8899`.

> **Tylko debug build** — cały kod w `app/src/debug/`, nie trafia do release.

## Motywacja

| Cecha | BroadcastReceiver (obecny) | HTTP Server (propozycja) |
|-------|---------------------------|--------------------------|
| Odczyt odpowiedzi | `adb logcat -s DebugState -d \| tail -1` | `curl localhost:8899/state` |
| Format odpowiedzi | tekst w logcat (trzeba wyciąć prefix) | czysty JSON |
| Narzędzia CLI | wymaga `adb`, parsowanie jest kruche | `curl` + `jq`, zero parsowania |
| Automatyzacja | utrudniona (race condition z logcat) | trywialna (HTTP request-response) |
| Nowe endpointy | nowa akcja w Receiver + manifest | nowy `when` branch w `serve()` |
| Integracja z Claude Code | wielokrokowa (broadcast → sleep → logcat) | jeden krok (curl) |
| Android 14+ | wymaga explicit broadcast z `-n` | brak ograniczeń |

## Architektura

```
┌──────────────────────────────────────────────────────────┐
│  Host (PC / Claude Code)                                 │
│  curl localhost:8899/state                               │
│         │                                                │
│    adb forward tcp:8899 tcp:8899                         │
└─────────┼────────────────────────────────────────────────┘
          │
┌─────────▼────────────────────────────────────────────────┐
│  Android Device (debug build)                            │
│                                                          │
│  ┌─────────────────────┐     ┌────────────────────────┐  │
│  │  DebugHttpServer     │────▶ DebugActivityTracker   │  │
│  │  (NanoHTTPD :8899)   │     │ .currentActivity      │  │
│  │                      │     └────────────────────────┘  │
│  │  wątek NanoHTTPD     │                │               │
│  │    ▼                 │     ┌──────────▼────────────┐  │
│  │  serve(session)      │     │  FragmentActivity     │  │
│  │    │                 │     │  └─ FragmentManager   │  │
│  │    │ CountDownLatch  │     │     └─ Adapter        │  │
│  │    │ + Handler(Main) │     │        └─ dataset     │  │
│  │    ▼                 │     └───────────────────────┘  │
│  │  JSON response       │                                │
│  └─────────────────────┘                                 │
│                                                          │
│  DebugToolsInitializer (AndroidX Startup)                │
│    ├─ registerActivityLifecycleCallbacks(Tracker)        │
│    └─ DebugHttpServer(context).start()                   │
└──────────────────────────────────────────────────────────┘
```

### Kluczowe komponenty

1. **NanoHTTPD** — lekki serwer HTTP (jedna klasa, ~150 KB). Port `8899`.
2. **DebugActivityTracker** — istniejący singleton (`app/src/debug/.../debug/DebugActivityTracker.kt`), daje `currentActivity`.
3. **DebugToolsInitializer** — istniejący initializer AndroidX Startup (`app/src/debug/.../initializers/DebugToolsInitializer.kt`), już zawiera kod startujący DebugHttpServer.
4. **CountDownLatch + Handler(Looper.getMainLooper())** — bridge do main thread. NanoHTTPD obsługuje requesty na swoich wątkach roboczych; dostęp do UI (Activity, FragmentManager, Adapter) wymaga main thread.
5. **`org.json.JSONObject`** — serializacja do JSON bez dodatkowych zależności (wbudowane w Android SDK).

## Thread safety — main thread bridge

NanoHTTPD wywołuje `serve()` na wątku roboczym. Dostęp do UI (Activity, Fragment, Adapter) jest dozwolony TYLKO na main thread.

Wzorzec:

```kotlin
private fun runOnMainThread(block: () -> JSONObject): JSONObject {
    val latch = CountDownLatch(1)
    var result = JSONObject()
    Handler(Looper.getMainLooper()).post {
        try {
            result = block()
        } catch (e: Exception) {
            result = JSONObject().apply {
                put("error", e.message)
            }
        } finally {
            latch.countDown()
        }
    }
    latch.await(5, TimeUnit.SECONDS)
    return result
}
```

Zasady:
- **Czytanie UI** (Activity, Fragment, Adapter, View) — zawsze przez `runOnMainThread`
- **Operacje bez UI** (czyszczenie cache, read SharedPreferences) — można bezpośrednio na wątku serwera
- **Timeout 5s** — zabezpieczenie przed deadlockiem gdy main thread jest zablokowany
- **Nigdy nie blokuj main thread** — `latch.await()` jest na wątku NanoHTTPD, nie na main

## Konfiguracja

### 1. Zależność Gradle

W `gradle/libs.versions.toml`:

```toml
[versions]
mavencentral-nanohttpd = "2.3.1"

[libraries]
nanohttpd-core = { module = "org.nanohttpd:nanohttpd", version.ref = "mavencentral-nanohttpd" }
```

W `app/build.gradle.kts`:

```kotlin
debugImplementation(libs.nanohttpd.core)
```

> `debugImplementation` — zależność tylko w debug buildzie. Release nie zawiera NanoHTTPD.

### 2. DebugHttpServer.kt

Nowy plik: `app/src/debug/kotlin/io/github/wykopmobilny/debug/DebugHttpServer.kt`

```kotlin
package io.github.wykopmobilny.debug

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
// ...

class DebugHttpServer(
    private val appContext: Context,
) : NanoHTTPD(PORT) {

    companion object {
        const val PORT = 8899
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return when {
            method == Method.GET && uri == "/" -> handleIndex()
            method == Method.GET && uri == "/state" -> handleState()
            method == Method.GET && uri == "/screen" -> handleScreen()
            method == Method.GET && uri == "/screen/entries" -> handleScreenEntries()
            method == Method.GET && uri == "/screen/links" -> handleScreenLinks()
            method == Method.POST && uri.startsWith("/navigate/") -> handleNavigate(uri)
            method == Method.POST && uri.matches(Regex("/action/vote/entry/\\d+")) -> handleVoteEntry(uri, vote = true)
            method == Method.DELETE && uri.matches(Regex("/action/vote/entry/\\d+")) -> handleVoteEntry(uri, vote = false)
            method == Method.POST && uri == "/action/clear-cache" -> handleClearCache()
            method == Method.POST && uri == "/action/logout" -> handleLogout()
            else -> jsonResponse(Status.NOT_FOUND, JSONObject().put("error", "Unknown endpoint: $method $uri"))
        }
    }

    private fun jsonResponse(status: Response.Status, json: JSONObject): Response {
        return newFixedLengthResponse(status, "application/json", json.toString(2))
    }
    // ... handler methods
}
```

### 3. Rejestracja w DebugToolsInitializer

`DebugToolsInitializer` już zawiera kod startujący serwer:

```kotlin
class DebugToolsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(DebugActivityTracker)

        val server = DebugHttpServer(context.applicationContext)
        server.start()
    }
}
```

### 4. ADB port forwarding

Przed użyciem z hosta:

```bash
adb forward tcp:8899 tcp:8899
```

Teraz `curl localhost:8899` na hoście trafia do serwera na urządzeniu.

## Endpointy

> To punkt startowy — endpointy można swobodnie dodawać w miarę potrzeb.

### `GET /` — lista endpointów

Self-documenting index. Zwraca listę wszystkich zarejestrowanych endpointów z opisem.

```bash
curl localhost:8899/
```

```json
{
  "server": "DebugHttpServer",
  "port": 8899,
  "endpoints": [
    {"method": "GET",    "path": "/",                         "description": "This index"},
    {"method": "GET",    "path": "/state",                    "description": "App state (activity, fragment, user)"},
    {"method": "GET",    "path": "/screen",                   "description": "Current screen summary"},
    {"method": "GET",    "path": "/screen/entries",           "description": "Entry list from current adapter"},
    {"method": "GET",    "path": "/screen/links",             "description": "Link list from current adapter"},
    {"method": "GET",    "path": "/screen/link-detail",       "description": "Link detail from current screen"},
    {"method": "POST",   "path": "/navigate/{tab}",           "description": "Switch to tab"},
    {"method": "POST",   "path": "/action/vote/entry/{id}",   "description": "Vote on entry"},
    {"method": "DELETE", "path": "/action/vote/entry/{id}",   "description": "Unvote entry"},
    {"method": "POST",   "path": "/action/open/link/{id}",    "description": "Open link detail by ID"},
    {"method": "POST",   "path": "/action/open/entry/{id}",   "description": "Open entry detail by ID"},
    {"method": "POST",   "path": "/action/clear-cache",       "description": "Clear app cache"},
    {"method": "POST",   "path": "/action/logout",            "description": "Force logout"}
  ]
}
```

### `GET /state` — stan aplikacji

Reuse logiki z `DebugStateReceiver.buildState()`. Te same dane, ale zwrócone jako HTTP response zamiast logcat.

```bash
curl -s localhost:8899/state | jq .
```

```json
{
  "activity": "MainNavigationActivity",
  "fragment": "HotFragment",
  "child_fragment": "HotEntriesFragment",
  "all_fragments": [
    {"name": "HotFragment", "visible": true, "resumed": true}
  ],
  "user_logged_in": true,
  "user_login": "username",
  "package": "io.github.wykopmobilny.debug",
  "version_name": "1.0.0"
}
```

### `GET /screen` — podsumowanie ekranu

Rozszerzenie `/state` o informacje o adapterze i liczbie elementów na ekranie.

```bash
curl -s localhost:8899/screen | jq .
```

```json
{
  "activity": "MainNavigationActivity",
  "fragment": "HotEntriesFragment",
  "adapter": "EntryAdapter",
  "item_count": 25,
  "item_type": "entry"
}
```

### `GET /screen/entries` — lista wpisów

Serializuje `adapter.dataset` do JSON.

Traversal chain: `DebugActivityTracker.currentActivity` → `supportFragmentManager` → visible Fragment → `childFragmentManager` → `view.findViewById<RecyclerView>` → `adapter as EndlessProgressAdapter` → `data`.

```bash
curl -s localhost:8899/screen/entries | jq '.entries[:2]'
```

```json
{
  "count": 25,
  "screen": "HotEntriesFragment",
  "entries": [
    {
      "id": 12345,
      "body": "Treść wpisu...",
      "author": "username",
      "vote_count": 42,
      "is_voted": false,
      "comments_count": 5
    }
  ]
}
```

### `GET /screen/links` — lista linków

Analogicznie do `/screen/entries`, ale dla linków.

```bash
curl -s localhost:8899/screen/links | jq '.links[:2]'
```

```json
{
  "count": 20,
  "screen": "PromotedFragment",
  "links": [
    {
      "id": 67890,
      "title": "Tytuł linku...",
      "url": "https://example.com",
      "vote_count": 150,
      "comments_count": 30
    }
  ]
}
```

### `POST /navigate/{tab}` — nawigacja

Reuse logiki z `DebugStateReceiver.switchTab()`.

```bash
curl -s -X POST localhost:8899/navigate/hot | jq .
```

```json
{
  "action": "navigate",
  "success": true,
  "tab": "hot"
}
```

Dostępne taby: `promoted`, `upcoming`, `hits`, `hot`, `mywykop`, `favorite`, `search`, `messages`, `notifications`.

### `POST /action/vote/entry/{id}` — plusowanie wpisu

Wywołuje API v3 `POST /v3/entries/{id}/votes` przez Retrofit.

```bash
curl -s -X POST localhost:8899/action/vote/entry/12345 | jq .
```

```json
{
  "action": "vote_entry",
  "success": true,
  "entry_id": 12345
}
```

### `DELETE /action/vote/entry/{id}` — cofnięcie plusa

Wywołuje API v3 `DELETE /v3/entries/{id}/votes`.

```bash
curl -s -X DELETE localhost:8899/action/vote/entry/12345 | jq .
```

```json
{
  "action": "unvote_entry",
  "success": true,
  "entry_id": 12345
}
```

### `POST /action/clear-cache` — czyszczenie cache

Reuse logiki z `DebugStateReceiver.clearCache()`.

```bash
curl -s -X POST localhost:8899/action/clear-cache | jq .
```

```json
{
  "action": "clear_cache",
  "success": true,
  "files_deleted": 42,
  "cache_path": "/data/user/0/io.github.wykopmobilny.debug/cache"
}
```

### `POST /action/logout` — wylogowanie

Reuse logiki z `DebugStateReceiver.forceLogout()`.

```bash
curl -s -X POST localhost:8899/action/logout | jq .
```

```json
{
  "action": "logout",
  "success": true,
  "was_logged_in": true
}
```

### `POST /action/open/link/{id}` — otwórz szczegóły linka

Otwiera ekran szczegółów znaleziska (LinkDetailsActivity) dla podanego ID.

```bash
curl -s -X POST localhost:8899/action/open/link/67890 | jq .
```

```json
{
  "action": "open_link",
  "success": true,
  "link_id": 67890
}
```

### `POST /action/open/entry/{id}` — otwórz szczegóły wpisu

Otwiera ekran szczegółów wpisu mikroblogowego (EntryActivity) dla podanego ID.

```bash
curl -s -X POST localhost:8899/action/open/entry/12345 | jq .
```

```json
{
  "action": "open_entry",
  "success": true,
  "entry_id": 12345
}
```

## Jak dodawać nowe endpointy

### Krok po kroku

1. **Dodaj branch w `serve()`**:
   ```kotlin
   method == Method.GET && uri == "/my/endpoint" -> handleMyEndpoint()
   ```

2. **Napisz handler**:
   ```kotlin
   private fun handleMyEndpoint(): Response {
       val json = runOnMainThread {
           val activity = DebugActivityTracker.currentActivity
           JSONObject().apply {
               put("my_data", "value")
           }
       }
       return jsonResponse(Response.Status.OK, json)
   }
   ```

3. **Dodaj do indeksu** (w handlerze `GET /`):
   ```kotlin
   JSONObject().apply {
       put("method", "GET")
       put("path", "/my/endpoint")
       put("description", "My custom endpoint")
   }
   ```

4. **Rebuild** i `curl localhost:8899/my/endpoint`.

### Wzorce do reuse

| Potrzeba | Jak uzyskać |
|----------|-------------|
| Aktualna Activity | `DebugActivityTracker.currentActivity` |
| Widoczny Fragment | `activity.supportFragmentManager.fragments.first { it.isVisible }` |
| Zagnieżdżony Fragment | `fragment.childFragmentManager.fragments.first { it.isVisible }` |
| Adapter RecyclerView | `fragment.view?.findViewById<RecyclerView>(R.id.recyclerView)?.adapter` |
| Dataset z adaptera | `(adapter as EndlessProgressAdapter<*, *>).data` |
| UserManager | `(appContext as WykopApp).userManagerApi.get()` |
| Retrofit API | Przez Dagger component: `(appContext as WykopApp).wykopComponent.entriesV3RetrofitApi()` |

### Konwencje

- **GET** — odczyt stanu, bez efektów ubocznych
- **POST** — akcje zmieniające stan (nawigacja, vote, cache)
- **DELETE** — cofanie akcji (unvote)
- Odpowiedzi błędów — zawsze JSON z polem `"error"`, status HTTP 4xx/5xx
- Ścieżki — lowercase, REST-style, slashe jako separatory

## Przykłady użycia

### Podstawowe

```bash
# Setup (raz na sesję ADB)
adb forward tcp:8899 tcp:8899

# Sprawdź stan
curl -s localhost:8899/state | jq .

# Odczytaj wpisy z ekranu
curl -s localhost:8899/screen/entries | jq '.entries[] | {id, body, vote_count}'

# Przełącz na mikroblog i odczytaj wpisy
curl -s -X POST localhost:8899/navigate/hot
sleep 1
curl -s localhost:8899/screen/entries | jq '.entries[:3]'

# Plusuj wpis
curl -s -X POST localhost:8899/action/vote/entry/12345 | jq .

# Otwórz szczegóły linku
curl -s -X POST localhost:8899/action/open/link/67890 | jq .

# Otwórz szczegóły wpisu
curl -s -X POST localhost:8899/action/open/entry/12345 | jq .
```

### Skrypt: test nawigacji po zakładkach

```bash
#!/bin/bash
adb forward tcp:8899 tcp:8899

TABS=("promoted" "upcoming" "hits" "hot" "mywykop" "favorite")

for tab in "${TABS[@]}"; do
    echo "=== $tab ==="
    curl -s -X POST "localhost:8899/navigate/$tab"
    sleep 1
    curl -s localhost:8899/screen | jq '{fragment, adapter, item_count}'
    echo
done
```

### Claude Code — jeden krok zamiast trzech

Przed (BroadcastReceiver):
```bash
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver \
    -a io.github.wykopmobilny.debug.DEBUG_STATE
sleep 1
adb logcat -s DebugState -d | tail -1 | python3 -c "import sys,json; print(json.dumps(json.loads(sys.stdin.read().split(': ',1)[1]), indent=2))"
```

Po (HTTP Server):
```bash
curl -s localhost:8899/state | jq .
```

## Relacja z DebugStateReceiver

### Koegzystencja

Serwer HTTP **nie zastępuje** DebugStateReceiver — oba mechanizmy współistnieją:

- **DebugStateReceiver** — działa bez `adb forward`, wystarczy `adb shell am broadcast`. Przydatny gdy port forwarding jest niedostępny (np. wireless ADB bez portu).
- **DebugHttpServer** — preferowany dla automatyzacji i Claude Code. Prostszy w użyciu, czyste JSON response, brak problemów z Android 14+ implicit broadcast restrictions.

### Reuse kodu

Serwer HTTP powinien wywoływać te same metody co DebugStateReceiver, wyciągnięte do wspólnej klasy pomocniczej:

```
DebugStateHelper (nowy)
  ├─ buildState(context, verbose): JSONObject
  ├─ clearCache(context): JSONObject
  ├─ forceLogout(context): JSONObject
  └─ switchTab(context, tab): JSONObject

DebugStateReceiver → deleguje do DebugStateHelper
DebugHttpServer    → deleguje do DebugStateHelper
```

Nowe funkcje (odczyt adaptera, vote) dodawane bezpośrednio w `DebugHttpServer`.

## Pliki do stworzenia/zmodyfikowania

| Plik | Akcja | Opis |
|------|-------|------|
| `app/src/debug/kotlin/.../debug/DebugHttpServer.kt` | **Nowy** | Klasa serwera (extends NanoHTTPD), routing, handlery |
| `app/src/debug/kotlin/.../debug/DebugStateHelper.kt` | **Nowy** | Wyciągnięta logika wspólna z DebugStateReceiver |
| `gradle/libs.versions.toml` | **Edycja** | Dodanie `mavencentral-nanohttpd = "2.3.1"` + library entry |
| `app/build.gradle.kts` | **Edycja** | `debugImplementation(libs.nanohttpd.core)` |
| `app/src/debug/.../debug/DebugStateReceiver.kt` | **Edycja** | Delegacja do DebugStateHelper |
| `app/src/debug/.../initializers/DebugToolsInitializer.kt` | Bez zmian | Już zawiera kod startujący DebugHttpServer |
| `app/src/debug/AndroidManifest.xml` | Bez zmian | Serwer nie wymaga zmian w manifeście |
