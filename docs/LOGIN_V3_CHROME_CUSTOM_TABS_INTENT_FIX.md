# Naprawa przechwytywania URL z tokenem w Chrome Custom Tabs - Login v3

## Problem
Po zalogowaniu przez Chrome Custom Tabs, Android nie przechwytywał callback URL z tokenem JWT przez Intent filter.

## Przyczyna
1. **Brakująca ścieżka w Intent filter**: Intent filter w `AndroidManifest.xml` dla `LoginScreenActivity` nie miał zdefiniowanego `android:path="/"`, co mogło powodować problemy z przechwytywaniem URL callback `https://wykop.pl/?token=...&rtoken=...`

2. **Brak `setIntent()` w `onNewIntent()`**: W trybie `launchMode="singleTask"`, Activity nie jest tworzony ponownie, tylko wywoływany jest `onNewIntent()`. Bez `setIntent(intent)`, nowy Intent nie był zapisywany jako aktualny Intent Activity.

3. **Timing issue**: Fragment mógł nie być gotowy w momencie przetwarzania Intent w `onCreate()` lub `onNewIntent()`.

## Rozwiązanie

### 1. AndroidManifest.xml
Dodano `android:path="/"` do Intent filtera:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <data
        android:scheme="https"
        android:host="wykop.pl"
        android:path="/"
        />
</intent-filter>
```

**Dlaczego to działa:**
- `android:path="/"` dopasowuje root path (`/`)
- Query parametry (`?token=...&rtoken=...`) NIE są częścią path, więc Intent filter prawidłowo przechwytuje URL `https://wykop.pl/?token=...&rtoken=...`

### 2. LoginScreenActivity.kt
Dodano trzy usprawnienia:

#### a) Pole `pendingDeepLink`
```kotlin
private var pendingDeepLink: String? = null
```
Przechowuje deep link, jeśli fragment nie jest jeszcze gotowy.

#### b) Metoda `onResume()`
```kotlin
override fun onResume() {
    super.onResume()
    pendingDeepLink?.let { deepLink ->
        val fragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
        if (fragment != null) {
            fragment.handleLoginV3Callback(deepLink)
            pendingDeepLink = null
        }
    }
}
```
Przetwarza pending deep link, gdy fragment jest gotowy.

#### c) `setIntent()` w `onNewIntent()`
```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)  // KRYTYCZNE: zapisuje nowy Intent jako aktualny
    handleIntent(intent)
}
```

#### d) Rozszerzone logowanie
Dodano logowanie w:
- `onCreate()` - pokazuje czy Intent ma data
- `onNewIntent()` - pokazuje czy nowy Intent przyszedł z data
- `onResume()` - pokazuje przetwarzanie pending deep link
- `handleIntent()` - pokazuje pełny flow przetwarzania deep link

## Flow logowania z Chrome Custom Tabs

### Scenariusz 1: Pierwszy start (Intent w onCreate)
1. Użytkownik otwiera `LoginScreenActivity` po raz pierwszy
2. `onCreate()` → `handleIntent()` → sprawdza `intent.data`
3. Jeśli brak data (normalne uruchomienie) → nic nie robi
4. Fragment zostaje utworzony

### Scenariusz 2: Callback po zalogowaniu (Intent w onNewIntent)
1. Użytkownik klika "Zaloguj się" → otwiera Chrome Custom Tabs
2. Po zalogowaniu wykop.pl przekierowuje na `https://wykop.pl/?token=...&rtoken=...`
3. Android znajduje Intent filter w `LoginScreenActivity`
4. Ponieważ Activity jest w trybie `singleTask` i już istnieje → wywołuje `onNewIntent()`
5. `onNewIntent()` → `setIntent(intent)` → `handleIntent(intent)`
6. `handleIntent()` → sprawdza czy fragment istnieje:
   - Jeśli TAK → `fragment.handleLoginV3Callback(url)`
   - Jeśli NIE → `pendingDeepLink = url`
7. Jeśli zapisano `pendingDeepLink` → `onResume()` go przetworzy

### Scenariusz 3: Pending deep link (Fragment nie gotowy)
1. Intent przyszedł w `onCreate()` lub `onNewIntent()`, ale fragment nie jest gotowy
2. `handleIntent()` zapisuje URL w `pendingDeepLink`
3. `onResume()` wykrywa `pendingDeepLink != null`
4. Sprawdza czy fragment istnieje → `fragment.handleLoginV3Callback(deepLink)`
5. Czyści `pendingDeepLink = null`

## Callback URL format
Zgodnie z `LoginV3Query.kt`:
```kotlin
// API v3 callback format: https://wykop.pl/?token={JWT}&rtoken={REFRESH_TOKEN}
private val connectCallbackPattern = "[?]token=([^&]+)&rtoken=([^&]+)".toRegex()
```

- **Host**: `wykop.pl` (bez www)
- **Path**: `/` (root)
- **Query params**: `?token={JWT}&rtoken={REFRESH_TOKEN}`

## Testowanie

### Logowanie
Filtruj logcat po tagach:
```
tag:LoginScreenActivity|LoginV3Fragment|LoginV3Query
```

### Oczekiwane logi przy prawidłowym flow
1. `LoginScreenActivity: onCreate: savedInstanceState=false, intent.data=null`
2. `LoginV3Fragment: loginButton clicked`
3. `LoginV3Query: login: Starting login flow`
4. `LoginV3Fragment: Opening Chrome Custom Tab`
5. **[Użytkownik loguje się w Chrome Custom Tabs]**
6. `LoginScreenActivity: onNewIntent: Received new intent with data: https://wykop.pl/?token=...`
7. `LoginScreenActivity: handleIntent: Deep link received: https://wykop.pl/?token=...`
8. `LoginScreenActivity: handleIntent: Found fragment, passing callback`
9. `LoginV3Fragment: handleCallback: URL=https://wykop.pl/?token=...`
10. `LoginV3Fragment: handleCallback: Callback URL detected, parsing credentials`
11. `LoginV3Query: onUrlInvoked: Processing callback URL`
12. `LoginV3Query: onUrlInvoked: Token saved, restarting app`

### Debug gdy nie działa
Jeśli callback nie działa, sprawdź:
1. Czy `onNewIntent()` jest wywoływany? → Jeśli NIE, problem z Intent filterem
2. Czy `intent.data` nie jest null? → Jeśli TAK, problem z Intent filterem
3. Czy fragment istnieje? → Jeśli NIE, `pendingDeepLink` powinien go obsłużyć w `onResume()`
4. Czy `handleLoginV3Callback()` jest wywoływany? → sprawdź logi

## Alternatywne rozwiązanie (jeśli nie działa)
Jeśli obecne rozwiązanie nie działa (np. Chrome Custom Tabs nie przekazuje Intentu), można użyć **custom scheme**:

### Option: Custom scheme (wykop://)
1. Zmień callback URL w backend API na `wykop://callback?token=...&rtoken=...`
2. Zmień Intent filter w `AndroidManifest.xml`:
   ```xml
   <data
       android:scheme="wykop"
       android:host="callback"
       android:path="/"
       />
   ```
3. Zmień pattern w `LoginV3Query.kt`:
   ```kotlin
   private val connectCallbackPattern = "wykop://callback[?]token=([^&]+)&rtoken=([^&]+)".toRegex()
   ```

**Uwaga:** Custom scheme wymaga zmian po stronie backend API Wykop.pl, dlatego nie jest to preferowane rozwiązanie.

## Zmienione pliki
1. `app/src/main/AndroidManifest.xml` - dodano `android:path="/"` do Intent filtera
2. `app/src/main/kotlin/io/github/wykopmobilny/ui/modules/loginscreen/LoginScreenActivity.kt` - dodano:
   - Pole `pendingDeepLink`
   - Metodę `onResume()` do przetwarzania pending deep link
   - `setIntent(intent)` w `onNewIntent()`
   - Rozszerzone logowanie

## Referencje
- [Android Deep Links Documentation](https://developer.android.com/training/app-links/deep-linking)
- [Chrome Custom Tabs Documentation](https://developer.chrome.com/docs/android/custom-tabs/)
- [Intent Filter Testing](https://developer.android.com/training/app-links/verify-android-applinks#testing)
