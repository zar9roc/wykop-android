# Testy automatyczne z wykorzystaniem Debug HTTP Server

Rozszerzenie testów androidTest o możliwość weryfikacji nawigacji i stanu aplikacji przez Debug HTTP Server bez potrzeby interakcji UI (Espresso).

## Motywacja

| Cecha | Testy Espresso | Testy Debug Server |
|-------|----------------|-------------------|
| Szybkość | Wolne (traversal UI, animacje) | Szybkie (HTTP request) |
| Stabilność | Flaky (timing, rendering) | Stabilne (API call) |
| Co testują | UI rendering + logika | Logika nawigacji + stan |
| Setup | Brak dodatkowego | `adb forward tcp:8899 tcp:8899` |
| Inspekcja danych | Trudna (view matchers) | Łatwa (JSON response) |

## Architektura

```
┌─────────────────────────────────────────────────────┐
│  Test (androidTest)                                 │
│                                                     │
│  ┌──────────────────────┐                          │
│  │  NavigationDebug...  │                          │
│  │  IntegrationTest     │                          │
│  │                      │                          │
│  │  BaseActivityTest    │ ──┐                      │
│  │  + DebugHttpClient   │   │                      │
│  └──────────────────────┘   │                      │
│                              │                      │
└──────────────────────────────┼──────────────────────┘
                               │
                               │ HTTP (localhost:8899)
                               │ adb forward tcp:8899 tcp:8899
                               │
┌──────────────────────────────▼──────────────────────┐
│  App (debug build)                                  │
│                                                     │
│  ┌────────────────────┐    ┌──────────────────┐    │
│  │ DebugHttpServer    │───▶│ MainNavigation   │    │
│  │ (NanoHTTPD :8899)  │    │ Activity         │    │
│  │                    │    │  └─ Fragments    │    │
│  │ /navigate/hot      │    │     └─ Adapters  │    │
│  │ /screen/entries    │    │                  │    │
│  │ /state             │    │                  │    │
│  └────────────────────┘    └──────────────────┘    │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## Komponenty

### 1. DebugHttpClient

**Lokalizacja:** `app/src/androidTest/.../tests/utils/DebugHttpClient.kt`

Klient HTTP do komunikacji z debug serverem. Typowane metody dla wszystkich endpointów.

```kotlin
val client = DebugHttpClient()

// Nawigacja
client.navigateTo("hot")

// Stan aplikacji
val state = client.getState()
val screen = client.getScreen()

// Dane z ekranu
val entries = client.getScreenEntries()
val links = client.getScreenLinks()

// Akcje
client.voteEntry(12345)
client.clearCache()
```

### 2. DebugServerRule

**Lokalizacja:** `app/src/androidTest/.../tests/rules/DebugServerRule.kt`

JUnit Rule weryfikujący że serwer jest dostępny przed uruchomieniem testu.

```kotlin
@get:Rule
val debugServerRule = DebugServerRule()

@Test
fun myTest() {
    val client = debugServerRule.client
    // ...
}
```

### 3. NavigationViaDebugServerTest

**Lokalizacja:** `app/src/androidTest/.../tests/NavigationViaDebugServerTest.kt`

Testy czysto end-to-end — wymagają ręcznego uruchomienia aplikacji.

**Testy:**
- `testNavigateAllTabs` — nawigacja po wszystkich zakładkach
- `testHotEntriesLoaded` — weryfikacja ładowania wpisów
- `testPromotedLinksLoaded` — weryfikacja ładowania linków
- `testNavigationStability` — wielokrotna nawigacja promoted↔hot
- `testScreenInfoForAllTabs` — inspekcja stanu dla wszystkich zakładek

### 4. NavigationDebugServerIntegrationTest

**Lokalizacja:** `app/src/androidTest/.../tests/NavigationDebugServerIntegrationTest.kt`

Testy integracyjne łączące:
- `BaseActivityTest` — infrastruktura testowa + MockWebServer
- `launchActivity` — automatyczne uruchomienie aplikacji
- `DebugHttpClient` — weryfikacja przez HTTP

**Testy:**
- `testNavigationViaDebugServer` — nawigacja hot → promoted
- `testScreenDataRetrieval` — odczyt danych z adaptera
- `testStateQuery` — weryfikacja stanu aplikacji
- `testMultipleNavigations` — sekwencja nawigacji hot→promoted→upcoming→hot

## Setup

### Jednorazowo (wymagane)

```bash
# 1. Port forwarding (trwa do odłączenia urządzenia lub `adb kill-server`)
adb forward tcp:8899 tcp:8899

# Weryfikacja (opcjonalne):
curl -s localhost:8899/ | jq .
```

### Uruchomienie testów

#### Wariant A: Integracyjne (zalecane dla CI/CD)

Testy uruchamiają aplikację automatycznie.

```bash
# Wszystkie testy debug server
./gradlew connectedDebugAndroidTest --tests "*.NavigationDebugServerIntegrationTest"

# Konkretny test
./gradlew connectedDebugAndroidTest --tests "*.NavigationDebugServerIntegrationTest.testNavigationViaDebugServer"
```

#### Wariant B: End-to-end (wymaga ręcznego startu)

1. Zbuduj i zainstaluj debug APK:
   ```bash
   ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. Uruchom aplikację ręcznie (kliknij ikonę na urządzeniu)

3. Uruchom testy:
   ```bash
   ./gradlew connectedDebugAndroidTest --tests "*.NavigationViaDebugServerTest"
   ```

## Przykłady użycia

### Test podstawowy: nawigacja

```kotlin
@Test
fun testNavigation() {
    // Setup
    mockWebServerRule.callsOnAppStart()
    launchActivity<MainNavigationActivity>()
    val client = DebugHttpClient()

    // Navigate
    client.navigateTo("hot")
    Thread.sleep(500)

    // Verify
    val screen = client.getScreen()
    assertTrue(screen.getString("fragment").contains("Hot"))
}
```

### Test zaawansowany: weryfikacja danych

```kotlin
@Test
fun testEntriesLoaded() {
    client.navigateTo("hot")
    Thread.sleep(1000)

    val result = client.getScreenEntries()
    val count = result.getInt("count")
    assertTrue("Expected entries, got $count", count > 0)

    val entries = result.getJSONArray("entries")
    val firstEntry = entries.getJSONObject(0)

    // Verify entry structure
    assertNotNull(firstEntry.getLong("id"))
    assertNotNull(firstEntry.getString("body"))
    assertTrue(firstEntry.getInt("vote_count") >= 0)
}
```

### Test stabilności: wielokrotna nawigacja

```kotlin
@Test
fun testNavigationStability() {
    repeat(10) {
        client.navigateTo("hot")
        Thread.sleep(400)
        assertEquals("HotFragment", client.getScreen().getString("fragment"))

        client.navigateTo("promoted")
        Thread.sleep(400)
        assertEquals("PromotedFragment", client.getScreen().getString("fragment"))
    }
}
```

## Wzorce testowe

### Pattern 1: Navigate + Verify

```kotlin
fun navigateAndVerify(tab: String, expectedFragment: String) {
    client.navigateTo(tab)
    Thread.sleep(500)

    val screen = client.getScreen()
    assertTrue(
        "Expected $expectedFragment",
        screen.getString("fragment").contains(expectedFragment, ignoreCase = true)
    )
}
```

### Pattern 2: Wait for data

```kotlin
fun waitForEntries(maxRetries: Int = 10): Int {
    repeat(maxRetries) {
        val result = client.getScreenEntries()
        val count = result.getInt("count")
        if (count > 0) return count
        Thread.sleep(300)
    }
    return 0
}
```

### Pattern 3: Compare before/after

```kotlin
@Test
fun testVoteIncrementsCounter() {
    client.navigateTo("hot")
    Thread.sleep(1000)

    val entriesBefore = client.getScreenEntries()
    val firstEntry = entriesBefore.getJSONArray("entries").getJSONObject(0)
    val entryId = firstEntry.getLong("id")
    val votesBefore = firstEntry.getInt("vote_count")

    // Vote
    client.voteEntry(entryId)
    Thread.sleep(500)

    // Refresh screen
    client.navigateTo("promoted")
    client.navigateTo("hot")
    Thread.sleep(1000)

    val entriesAfter = client.getScreenEntries()
    val entryAfter = entriesAfter.getJSONArray("entries").getJSONObject(0)
    val votesAfter = entryAfter.getInt("vote_count")

    assertEquals("Vote count should increment", votesBefore + 1, votesAfter)
}
```

## Debugging

### Problem: Server not reachable

```
DebugHttpServer is not reachable at localhost:8899
```

**Rozwiązanie:**

1. Sprawdź czy aplikacja działa:
   ```bash
   adb shell pidof io.github.wykopmobilny.debug
   ```

2. Sprawdź port forwarding:
   ```bash
   adb forward --list
   # Powinno być: 8899 tcp:8899
   ```

3. Zresetuj forwarding:
   ```bash
   adb forward --remove-all
   adb forward tcp:8899 tcp:8899
   ```

4. Sprawdź czy serwer działa na urządzeniu:
   ```bash
   curl -s localhost:8899/ | jq .server
   # Powinno zwrócić: "DebugHttpServer"
   ```

### Problem: Empty adapter

```
Expected entries, got 0
```

**Możliwe przyczyny:**

1. **Dane się jeszcze nie załadowały** — zwiększ `Thread.sleep()` po nawigacji
2. **API mock nie zwraca danych** — sprawdź `MockWebServerRule` w BaseActivityTest
3. **Błąd API** — sprawdź logcat: `adb logcat -s MoshiResponse:* BearerAuthInterceptor:*`

### Problem: Fragment mismatch

```
Expected HotFragment but got HotEntriesFragment
```

**To nie jest problem** — fragmenty mogą mieć zagnieżdżenia. Używaj `contains()`:

```kotlin
assertTrue(
    screen.getString("fragment").contains("Hot", ignoreCase = true)
)
```

## Integracja z CI/CD

### GitHub Actions

```yaml
name: Debug Server Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Start Android Emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          target: default
          arch: x86_64
          script: |
            # Port forwarding
            adb forward tcp:8899 tcp:8899

            # Run integration tests
            ./gradlew connectedDebugAndroidTest \
              --tests "*.NavigationDebugServerIntegrationTest"
```

### Skrypt lokalny

```bash
#!/bin/bash
# run-debug-server-tests.sh

set -e

echo "=== Setup ==="
adb forward tcp:8899 tcp:8899

echo "=== Building debug APK ==="
./gradlew assembleDebug

echo "=== Installing ==="
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "=== Running tests ==="
./gradlew connectedDebugAndroidTest \
  --tests "*.NavigationDebugServerIntegrationTest" \
  --stacktrace

echo "=== Done ==="
```

## Porównanie z tradycyjnymi testami

### NavigationTest (Espresso)

```kotlin
@Test
fun navigation() {
    mockWebServerRule.callsOnAppStart()
    launchActivity<MainNavigationActivity>()
    Espresso.onIdle()

    MainPage.openDrawer()          // Kliknięcie
    MainPage.tapDrawerOption(...)  // Kliknięcie
    SettingsPage.assertVisible()   // Traversal view hierarchy

    Espresso.pressBack()           // Symulacja przycisku
    MainPage.openDrawer()
    MainPage.closeDrawer()
}
```

### NavigationDebugServerIntegrationTest

```kotlin
@Test
fun testNavigation() {
    mockWebServerRule.callsOnAppStart()
    launchActivity<MainNavigationActivity>()

    client.navigateTo("settings")  // HTTP call
    val screen = client.getScreen()
    assertEquals("SettingsFragment", screen.getString("fragment"))

    client.navigateTo("promoted")
    assertEquals("PromotedFragment", client.getScreen().getString("fragment"))
}
```

**Różnice:**
- **Espresso:** testuje UI flow (drawer, kliknięcia, back button)
- **Debug Server:** testuje logikę nawigacji (MainActivity.switchTab)

**Oba podejścia są wartościowe:**
- Espresso → UI/UX regression tests
- Debug Server → business logic + integration tests

## Rozszerzenia

### Dodanie nowego endpointu do testów

1. Dodaj endpoint w `DebugHttpServer.kt`:
   ```kotlin
   method == Method.GET && uri == "/my/endpoint" -> handleMyEndpoint()
   ```

2. Dodaj metodę w `DebugHttpClient.kt`:
   ```kotlin
   fun getMyData(): JSONObject = get("$baseUrl/my/endpoint")
   ```

3. Użyj w teście:
   ```kotlin
   @Test
   fun testMyFeature() {
       val data = client.getMyData()
       assertNotNull(data)
   }
   ```

### Mockowanie odpowiedzi API dla testów

Testy integracyjne używają `MockWebServerRule` z `BaseActivityTest`:

```kotlin
@Before
fun setUp() {
    // Mock API v3 entries
    mockWebServerRule.enqueue("/api/v3/entries") {
        MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "data": {
                        "items": [
                            {"id": 1, "body": "Test entry", ...}
                        ]
                    }
                }
            """.trimIndent())
    }
}
```

## Pliki

| Plik | Opis |
|------|------|
| `app/src/androidTest/.../tests/utils/DebugHttpClient.kt` | Klient HTTP |
| `app/src/androidTest/.../tests/rules/DebugServerRule.kt` | JUnit Rule |
| `app/src/androidTest/.../tests/NavigationViaDebugServerTest.kt` | Testy E2E (wymaga ręcznego startu) |
| `app/src/androidTest/.../tests/NavigationDebugServerIntegrationTest.kt` | Testy integracyjne (self-contained) |
| `docs/DEBUG_SERVER_TESTS.md` | Ta dokumentacja |

## Następne kroki

- [ ] Dodać testy vote/unvote przez debug server
- [ ] Dodać testy weryfikujące paginację (scroll → load more)
- [ ] Rozszerzyć o testy dla zalogowanego użytkownika
- [ ] Dodać snapshot testing dla adapter data
- [ ] Integracja z screenshot tests (navigate → screenshot)
