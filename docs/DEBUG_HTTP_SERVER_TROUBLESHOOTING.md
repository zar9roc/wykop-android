# Debug HTTP Server - Analiza Problemów i Rozwiązania

## Problem: Intermittent Connection Refused (Exit Code 52)

### Symptomy
- Server okresowo zwraca exit code 52 (connection refused)
- Curl/HTTP klient nie może połączyć się z `localhost:8899`
- Testy E2E失败 z błędem "DebugHttpServer is not reachable"

### Diagnoza

#### 1. **Główny Problem: Brak Trwałej Referencji (Garbage Collection)**

**Lokalizacja:** `app/src/debug/kotlin/io/github/wykopmobilny/initializers/DebugToolsInitializer.kt:23-24`

```kotlin
val server = DebugHttpServer(context.applicationContext, entriesApi)
server.start()
```

**Problem:**
- Server jest tworzony jako zmienna lokalna w metodzie `create()`
- Po zakończeniu metody nie ma żadnej referencji do obiektu servera
- Garbage Collector może zniszczyć obiekt, zamykając otwarty socket
- NanoHTTPD wymaga żywej referencji do obiektu aby socket pozostał otwarty

**Dowód:**
```bash
# Server startuje poprawnie (z logcat):
03-08 22:20:41.553 I DebugHttpServer: Debug HTTP server started on port 8899

# Ale po pewnym czasie (szczególnie po GC):
curl localhost:8899
# curl: (52) Empty reply from server
# lub
# curl: (7) Failed to connect to localhost port 8899: Connection refused
```

**Porównanie:**
- `DebugActivityTracker` - **działa stabilnie** bo jest `object` (singleton)
- `DebugHttpServer` - **niestabilny** bo jest tworzony jako zmienna lokalna

#### 2. **Problem: Process Death**

Gdy Android zabija proces aplikacji (low memory, user swipe), server przestaje działać.
Testy zakładają że aplikacja już działa i server jest uruchomiony.

**Implikacje:**
- Jeśli aplikacja nie jest w foreground, server może nie działać
- Po restarcie aplikacji server jest restartowany, ale testy mogą próbować połączyć się za wcześnie

#### 3. **Problem: Port Forwarding**

ADB port forwarding (`adb forward tcp:8899 tcp:8899`) jest volatile:
- Resetowany po odłączeniu/ponownym podłączeniu urządzenia
- Resetowany po `adb kill-server` / `adb start-server`
- NIE jest automatycznie ustawiany przez testy

**Weryfikacja:**
```bash
adb forward --list
# Powinno pokazać: emulator-5554 tcp:8899 tcp:8899
# Jeśli puste -> connection refused
```

#### 4. **Problem: Race Condition przy Starcie**

`DebugToolsInitializer` jest uruchamiane przez AndroidX Startup podczas `Application.onCreate()`.
Testy mogą próbować połączyć się przed pełnym startem servera.

**Mitigacja w testach:**
```kotlin
// NavigationDebugServerIntegrationTest.kt:55
Thread.sleep(300)  // Give server time to start
```

To nie jest niezawodne rozwiązanie.

### Weryfikacja Obecnego Stanu

#### Test 1: Czy server jest uruchomiony?
```bash
adb logcat -s DebugHttpServer DebugToolsInitializer -d | tail -5
# Powinieneś zobaczyć:
# I DebugHttpServer: Debug HTTP server started on port 8899
# D DebugToolsInitializer: DebugHttpServer started
```

#### Test 2: Czy port forwarding działa?
```bash
adb forward --list
# Powinno zwrócić:
# emulator-5554 tcp:8899 tcp:8899
```

#### Test 3: Czy server odpowiada?
```bash
curl http://localhost:8899/
# Powinno zwrócić JSON z listą endpointów
```

#### Test 4: Czy server ma live reference?
Nie ma łatwego sposobu aby to sprawdzić bez debuggera. Jedyny sposób:
```bash
# Wymuś garbage collection i sprawdź czy server nadal działa
# (wymaga root lub debuggable app)
adb shell am broadcast -a com.android.internal.intent.action.REQUEST_GC
sleep 2
curl http://localhost:8899/
```

### Rozwiązania

#### Rozwiązanie 1: Singleton dla DebugHttpServer (ZALECANE)

Zmienić `DebugToolsInitializer` aby trzymał referencję do servera:

```kotlin
object DebugServerHolder {
    private var server: DebugHttpServer? = null

    fun initialize(context: Context, entriesApi: EntriesV3RetrofitApi) {
        if (server == null) {
            server = DebugHttpServer(context, entriesApi).apply {
                start()
            }
            Napier.i("DebugHttpServer initialized and started", tag = "DebugServerHolder")
        }
    }

    fun getServer(): DebugHttpServer? = server

    fun isRunning(): Boolean = server?.isAlive == true
}

// W DebugToolsInitializer.create():
DebugServerHolder.initialize(context.applicationContext, entriesApi)
```

**Zalety:**
- Trwała referencja - server nie będzie GC'owany
- Można sprawdzić czy server żyje
- Można go restartować jeśli potrzeba

**Wady:**
- Trzeba dodać metodę `isAlive` do `DebugHttpServer` (NanoHTTPD ma `isAlive()` protected)

#### Rozwiązanie 2: Trzymać referencję w Application

```kotlin
// W WykopApp.kt (tylko debug)
class WykopApp : Application() {
    var debugServer: DebugHttpServer? = null  // null w release

    // ...
}

// W DebugToolsInitializer:
val wykopApp = context.applicationContext as WykopApp
wykopApp.debugServer = DebugHttpServer(context.applicationContext, entriesApi).apply {
    start()
}
```

**Zalety:**
- Prosty
- Referencja żyje tak długo jak aplikacja

**Wady:**
- Wymaga modyfikacji głównej klasy `Application`
- Dodaje debug-specific kod do production class

#### Rozwiązanie 3: Automatyczny Port Forwarding w Testach

Dodać setup w `DebugServerRule` lub `BaseActivityTest`:

```kotlin
@Before
fun setupPortForwarding() {
    // Automatycznie ustawiaj port forwarding
    ProcessBuilder("adb", "forward", "tcp:8899", "tcp:8899")
        .start()
        .waitFor(5, TimeUnit.SECONDS)
}
```

**Zalety:**
- Eliminuje manual setup
- Mniej prone to human error

**Wady:**
- Wymaga `adb` na PATH
- Może nie działać w CI

#### Rozwiązanie 4: Lepsze Logowanie i Diagnostyka

Dodać do `DebugHttpServer`:

```kotlin
override fun start() {
    try {
        super.start()
        Napier.i("Debug HTTP server started on port $PORT (socket=$serverSocketFactory)", tag = TAG)
    } catch (e: java.io.IOException) {
        Napier.e("CRITICAL: Failed to start debug HTTP server on port $PORT", e, tag = TAG)
        throw e  // Re-throw aby Initializer failure był widoczny
    }
}

override fun stop() {
    super.stop()
    Napier.w("Debug HTTP server stopped on port $PORT", tag = TAG)
}

fun isAlive(): Boolean = isAlive  // Expose protected method
```

**Zalety:**
- Łatwiejsze debugowanie
- Można wykryć czy server działa

### Rekomendacje

**Natychmiastowe (High Priority):**
1. Implementuj **Rozwiązanie 1** (Singleton) - eliminuje główny problem z GC
2. Implementuj **Rozwiązanie 4** (Lepsze logowanie) - ułatwia diagnostykę

**Średni Priorytet:**
3. Dodaj `@Before` w testach które automatycznie sprawdzają i ustawiają port forwarding
4. Dodaj timeout/retry w `DebugServerRule.isServerReachable()` zamiast single shot

**Niski Priorytet:**
5. Rozważ watchdog który sprawdza czy server żyje i restartuje go jeśli nie

### Workaround dla Obecnego Kodu

Jeśli nie możesz zmodyfikować kodu teraz, użyj tego skryptu przed testami:

```bash
#!/bin/bash
# setup-debug-server.sh

# 1. Ustaw port forwarding
adb forward tcp:8899 tcp:8899

# 2. Sprawdź czy aplikacja działa
if ! adb shell pidof io.github.wykopmobilny.debug > /dev/null; then
    echo "App not running. Starting..."
    adb shell am start -n io.github.wykopmobilny.debug/.ui.modules.mainnavigation.MainNavigationActivity
    sleep 2
fi

# 3. Sprawdź czy server odpowiada
for i in {1..10}; do
    if curl -s http://localhost:8899/ > /dev/null; then
        echo "Server is ready"
        exit 0
    fi
    echo "Attempt $i: Server not ready, waiting..."
    sleep 1
done

echo "ERROR: Server did not start after 10 seconds"
exit 1
```

### Test Verification

Po implementacji rozwiązania 1 + 4, uruchom:

```bash
# Test 1: Garbage Collection Stress Test
for i in {1..50}; do
    curl -s http://localhost:8899/ > /dev/null
    if [ $? -ne 0 ]; then
        echo "FAIL at iteration $i"
        exit 1
    fi
    echo "OK: $i"
    sleep 0.5
done
echo "PASS: Server survived 50 requests"

# Test 2: Background/Foreground Cycle
adb shell input keyevent KEYCODE_HOME  # Send to background
sleep 5
adb shell am start -n io.github.wykopmobilny.debug/.ui.modules.mainnavigation.MainNavigationActivity
sleep 2
curl http://localhost:8899/
# Powinno działać

# Test 3: Process Restart
adb shell am force-stop io.github.wykopmobilny.debug
sleep 1
adb shell am start -n io.github.wykopmobilny.debug/.ui.modules.mainnavigation.MainNavigationActivity
sleep 2
curl http://localhost:8899/
# Powinno działać
```

## Podsumowanie

| Problem | Root Cause | Rozwiązanie | Priorytet |
|---------|-----------|-------------|-----------|
| Intermittent connection refused | Brak trwałej referencji, GC | Singleton holder | **HIGH** |
| Brak diagnostyki | Brak logowania lifecycle | Lepsze logi + `isAlive()` | **HIGH** |
| Manual port forwarding | Testy nie ustawiają ADB forward | Auto-setup w testach | MEDIUM |
| Race condition przy starcie | Sleep(300) nie jest niezawodny | Proper readiness check | MEDIUM |
| Process death | Server ginie z aplikacją | Dokumentacja + watchdog | LOW |

## Appendix: NanoHTTPD Lifecycle

NanoHTTPD trzyma otwarty `ServerSocket` w polu instancji. Gdy obiekt jest GC'owany:
1. `finalize()` jest wywoływane (deprecated ale nadal używane w NanoHTTPD 2.x)
2. Socket jest zamykany
3. Kolejne requesty dostają "connection refused"

Dlatego KRYTYCZNE jest trzymanie live reference do instancji `DebugHttpServer`.

## Reference

- NanoHTTPD GitHub: https://github.com/NanoHttpd/nanohttpd
- AndroidX Startup: https://developer.android.com/topic/libraries/app-startup
- ADB Port Forwarding: https://developer.android.com/tools/adb#forwardports
