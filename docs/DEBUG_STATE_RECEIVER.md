# Debug State Receiver

Debug-only `BroadcastReceiver` zwracający strukturalne dane o stanie aplikacji jako JSON w logcat oraz umożliwiający sterowanie aplikacją bez interakcji z UI.

> **WAŻNE**: Na Androidzie 14+ (SDK 34+) implicit broadcasts nie docierają do manifest-registered receivers. Wszystkie komendy muszą zawierać jawny komponent `-n io.github.wykopmobilny.debug/.DebugStateReceiver`.

## Dostępne akcje

### 1. DEBUG_STATE — dump stanu aplikacji
Zwraca strukturalne dane o aktualnym stanie aplikacji.

```bash
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_STATE
```

Odczyt wyniku:
```bash
adb logcat -s DebugState -d | tail -1
```

Tryb verbose (back stack, info o urządzeniu):
```bash
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_STATE --ez verbose true
```

### 2. DEBUG_CLEAR_CACHE — czyszczenie cache
Czyści katalog cache aplikacji (context.cacheDir) wraz ze wszystkimi podkatalogami.

```bash
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_CLEAR_CACHE
```

Odpowiedź:
```json
{
  "action": "clear_cache",
  "success": true,
  "files_deleted": 42,
  "cache_path": "/data/user/0/io.github.wykopmobilny.debug/cache"
}
```

### 3. DEBUG_LOGOUT — wylogowanie użytkownika
Wymusza wylogowanie aktualnie zalogowanego użytkownika (czyści JWT token i dane użytkownika).

```bash
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_LOGOUT
```

Odpowiedź:
```json
{
  "action": "logout",
  "success": true,
  "was_logged_in": true
}
```

### 4. DEBUG_SWITCH_TAB — przełączanie zakładek
Otwiera MainNavigationActivity z wybraną zakładką (wymaga parametru `--es tab`).

```bash
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab "promoted"
```

Dostępne nazwy zakładek:
- `promoted` / `home` — Strona główna (promoted links)
- `upcoming` — Wykopalisko
- `hits` — Hity
- `hot` — Mikroblog (hot entries)
- `mywykop` — Mój Wykop
- `favorite` — Ulubione
- `search` — Wyszukiwanie
- `messages` — Wiadomości prywatne
- `notifications` — Powiadomienia

Odpowiedź:
```json
{
  "action": "switch_tab",
  "success": true,
  "tab": "promoted",
  "note": "Tab switch requested - check if MainNavigationActivity is in foreground"
}
```

## Przykładowe odpowiedzi

### DEBUG_STATE (podstawowa)

```json
{
  "activity": "MainNavigationActivity",
  "activity_class": "io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationActivity",
  "fragment": "PromotedFragment",
  "all_fragments": [
    {"name": "PromotedFragment", "visible": true, "resumed": true}
  ],
  "child_fragment": "LinksFragment",
  "child_fragments": [
    {"name": "LinksFragment", "visible": true},
    {"name": "HitsFragment", "visible": false}
  ],
  "user_logged_in": true,
  "user_login": "username",
  "package": "io.github.wykopmobilny.debug",
  "version_name": "1.0.0"
}
```

### DEBUG_STATE (verbose)

Verbose dodaje:
```json
{
  "back_stack_count": 0,
  "back_stack": [],
  "device": {
    "model": "Pixel 6",
    "sdk": 34,
    "manufacturer": "Google"
  }
}
```

## Pola

| Pole | Opis |
|------|------|
| `activity` | Nazwa aktualnej Activity (lub "none" gdy w tle) |
| `fragment` | Widoczny fragment w głównym kontenerze |
| `all_fragments` | Lista wszystkich fragmentów z flagami visible/resumed |
| `child_fragment` | Widoczny fragment zagnieżdżony (np. tab w ViewPager) |
| `user_logged_in` | Czy użytkownik jest zalogowany |
| `user_login` | Login użytkownika (null gdy niezalogowany) |

## Architektura

- `DebugActivityTracker` — singleton `ActivityLifecycleCallbacks`, śledzi bieżące Activity
- `DebugToolsInitializer` — rejestruje tracker przez AndroidX Startup
- `DebugStateReceiver` — BroadcastReceiver zbierający stan i logujący JSON

Wszystkie pliki w `app/src/debug/` — nie trafiają do release buildu.

## Przykłady użycia

### One-liner do sprawdzenia stanu
```bash
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_STATE && adb logcat -s DebugState -d | tail -1
```

### Scenariusz testowy: reset stanu aplikacji
```bash
# Wyloguj użytkownika
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_LOGOUT

# Wyczyść cache
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_CLEAR_CACHE

# Zrestartuj aplikację
adb shell am force-stop io.github.wykopmobilny.debug
adb shell am start -n io.github.wykopmobilny.debug/io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationActivity
```

### Scenariusz testowy: nawigacja między ekranami
```bash
# Otwórz Mikroblog
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab "hot"
sleep 2

# Otwórz Wykopalisko
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab "upcoming"
sleep 2

# Sprawdź stan
adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver -a io.github.wykopmobilny.debug.DEBUG_STATE && adb logcat -s DebugState -d | tail -1
```

### Skrypt bash: automatyczne testy nawigacji
```bash
#!/bin/bash
DSR="-n io.github.wykopmobilny.debug/.DebugStateReceiver"
TABS=("promoted" "upcoming" "hits" "hot" "mywykop" "favorite")

for tab in "${TABS[@]}"; do
  echo "Testing tab: $tab"
  adb shell am broadcast $DSR -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab "$tab"
  sleep 1
  adb shell am broadcast $DSR -a io.github.wykopmobilny.debug.DEBUG_STATE
  adb logcat -s DebugState -d | tail -1 | grep "\"fragment\""
  sleep 1
done
```
