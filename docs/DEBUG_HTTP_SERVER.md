# Debug HTTP Server

Lekki serwer HTTP działający w debug buildach, udostępniający stan aplikacji i akcje debugowe przez REST API. Alternatywa dla BroadcastReceiver — dostępny z przeglądarki, curl, lub dowolnego klienta HTTP.

## Architektura

- `DebugHttpServer` — serwer oparty na `ServerSocket` (zero zależności zewnętrznych)
- Uruchamiany przez `DebugToolsInitializer` przy starcie aplikacji
- Działa na osobnym wątku (`Dispatchers.IO`)
- Port: **8099** (unika kolizji z typowymi serwerami dev)
- Tylko debug build — brak kodu w release

## Endpointy

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| GET | `/` | Dashboard HTML z linkami do wszystkich endpointów |
| GET | `/state` | Stan aplikacji (JSON) |
| GET | `/state?verbose=true` | Stan z back stack i info o urządzeniu |
| POST | `/clear-cache` | Czyszczenie cache |
| POST | `/logout` | Wylogowanie użytkownika |
| POST | `/switch-tab?tab=promoted` | Przełączenie zakładki |

## Dostęp

### Z komputera (przez ADB port forwarding)
```bash
adb forward tcp:8099 tcp:8099
curl http://localhost:8099/state
```

### Z przeglądarki na telefonie
```
http://localhost:8099/
```

## Implementacja

### Pliki
- `app/src/debug/kotlin/.../debug/DebugHttpServer.kt` — serwer HTTP
- `app/src/debug/kotlin/.../initializers/DebugToolsInitializer.kt` — modyfikacja: start serwera

### Wzorzec
- Reużycie logiki z `DebugStateReceiver` (buildState, clearCache, forceLogout, switchTab)
- `DebugHttpServer` przyjmuje `Context` i korzysta z `DebugActivityTracker`
- Odpowiedzi JSON z `Content-Type: application/json`
- Dashboard HTML z `Content-Type: text/html`

### Bezpieczeństwo
- Nasłuchuje na `0.0.0.0` (dostępny w sieci lokalnej)
- Tylko w debug buildach
- `network_security_config.xml` już pozwala na cleartext localhost
