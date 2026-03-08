# Debug State Receiver

Debug-only `BroadcastReceiver` zwracający strukturalne dane o stanie aplikacji jako JSON w logcat.

## Użycie

Podstawowe wywołanie (jedna komenda):
```bash
adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE
```

Odczyt wyniku:
```bash
adb logcat -s DebugState -d | tail -1
```

Tryb verbose (back stack, info o urządzeniu):
```bash
adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE --ez verbose true
```

## Przykładowa odpowiedź

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

## One-liner

```bash
adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE && adb logcat -s DebugState -d | tail -1
```
