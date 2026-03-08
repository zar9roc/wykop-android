# Tests README

## Debug Server Integration Tests

Testy wykorzystujące Debug HTTP Server do weryfikacji nawigacji bez interakcji UI.

### Quick Start

1. Setup port forwarding (raz na sesję ADB):
   ```bash
   adb forward tcp:8899 tcp:8899
   ```

2. Uruchom testy:
   ```bash
   ./gradlew connectedDebugAndroidTest --tests "*.NavigationDebugServerIntegrationTest"
   ```

   Lub użyj skryptu:
   ```bash
   ./scripts/run-debug-server-tests.sh
   ```

### Pliki

- **utils/DebugHttpClient.kt** - Klient HTTP do komunikacji z debug serverem
- **rules/DebugServerRule.kt** - JUnit Rule weryfikujący dostępność serwera
- **NavigationDebugServerIntegrationTest.kt** - Testy integracyjne (self-contained)
- **NavigationViaDebugServerTest.kt** - Testy E2E (wymaga ręcznego startu aplikacji)

### Dokumentacja

Pełna dokumentacja: **[docs/DEBUG_SERVER_TESTS.md](../../../../../../../../docs/DEBUG_SERVER_TESTS.md)**

Zawiera:
- Motywację i architekturę
- Szczegółowe instrukcje setup i uruchomienia
- Przykłady użycia i wzorce testowe
- Debugging i integrację z CI/CD
- Porównanie z tradycyjnymi testami Espresso
