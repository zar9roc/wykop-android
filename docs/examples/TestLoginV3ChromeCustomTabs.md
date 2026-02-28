# Instrukcja testowania logowania v3 z Chrome Custom Tabs

## Jak przetestować
1. Odinstaluj aplikację (jeśli jest zainstalowana)
2. Zainstaluj nową wersję z poprawkami
3. Uruchom aplikację
4. Przejdź do ekranu logowania
5. Kliknij "Zaloguj się"
6. **Ważne:** Chrome Custom Tabs powinien się otworzyć z formularzem logowania wykop.pl
7. Zaloguj się na swoje konto
8. Po akceptacji GDPR (jeśli wymagane), wykop.pl przekieruje na callback URL
9. **Oczekiwane zachowanie:** Aplikacja powinna automatycznie przechwycić callback URL i zalogować użytkownika

## Co powinno się wydarzyć
- Chrome Custom Tabs powinien się **automatycznie zamknąć** po przekierowaniu
- LoginScreenActivity powinien otrzymać Intent z callback URL
- Aplikacja powinna pokazać loader "Ładowanie..."
- Po chwili aplikacja powinna zrestartować się i zalogować użytkownika
- Toast: "Zalogowano pomyślnie!"

## Logowanie debug
Aby zobaczyć szczegółowe logi, użyj Android Studio Logcat z filtrem:
```
tag:LoginScreenActivity|LoginV3Fragment|LoginV3Query
```

### Kluczowe logi do sprawdzenia
```
✅ LoginScreenActivity: onNewIntent: Received new intent with data: https://wykop.pl/?token=...
✅ LoginScreenActivity: handleIntent: Deep link received: https://wykop.pl/?token=...
✅ LoginScreenActivity: handleIntent: Found fragment, passing callback
✅ LoginV3Fragment: handleCallback: Callback URL detected, parsing credentials
✅ LoginV3Query: onUrlInvoked: Token saved, restarting app
```

## Jeśli coś nie działa

### Scenariusz 1: Chrome Custom Tabs nie zamyka się
**Objaw:** Po zalogowaniu Chrome Custom Tabs pozostaje otwarty, aplikacja nie reaguje

**Debug:**
1. Sprawdź logcat - czy `onNewIntent` jest wywoływany?
2. Jeśli **NIE** → problem z Intent filterem (zgłoś bug z logami)
3. Jeśli **TAK** → sprawdź czy `intent.data` nie jest null

**Workaround:** Ręcznie zamknij Chrome Custom Tabs (przycisk X lub back), aplikacja może przechwycić Intent po zamknięciu

### Scenariusz 2: "Fragment not found" w logach
**Objaw:** W logach widać: `LoginScreenActivity: handleIntent: Fragment not found in container, saving as pending`

**Debug:**
1. Sprawdź czy po chwili pojawia się: `LoginScreenActivity: onResume: Processing pending deep link`
2. Jeśli **TAK** → to normalne, mechanizm pending zadziałał poprawnie
3. Jeśli **NIE** → sprawdź czy fragment został utworzony (może być crash)

### Scenariusz 3: "URL does not match callback pattern"
**Objaw:** W logach widać: `LoginV3Query: onUrlInvoked: URL does not match callback pattern`

**Problem:** Callback URL ma inny format niż oczekiwany

**Debug:**
1. Sprawdź pełny URL w logach: `LoginV3Fragment: handleCallback: URL=...`
2. Porównaj z oczekiwanym: `https://wykop.pl/?token=XXX&rtoken=YYY`
3. Jeśli różni się → zgłoś bug z dokładnym URL

### Scenariusz 4: Chrome Custom Tabs nie otwiera się
**Objaw:** Kliknięcie "Zaloguj się" nie powoduje otwarcia Chrome Custom Tabs

**Debug:**
1. Sprawdź logcat: `LoginV3Fragment: Opening Chrome Custom Tab with connect URL`
2. Jeśli **NIE MA** → problem z pobieraniem connect URL z API
3. Sprawdź czy jest error: `LoginV3Query: login: Failed to get connect URL`

## Znane problemy i ograniczenia
1. **Chrome Custom Tabs może nie zamykać się automatycznie** - to zależy od wersji Chrome i Androida
2. **Intent może być opóźniony** - Android może dostarczyć Intent dopiero po zamknięciu Chrome Custom Tabs
3. **Callback może nie działać na emulatorze** - testuj na prawdziwym urządzeniu

## Wymagania
- Android 5.0+ (API 21+)
- Chrome lub inna przeglądarka obsługująca Chrome Custom Tabs
- Połączenie z internetem

## Porównanie z WebView (stara implementacja)
| Aspekt | WebView | Chrome Custom Tabs |
|--------|---------|-------------------|
| Zamykanie po callback | Automatyczne | Może wymagać ręcznego zamknięcia |
| Cookies | Wymagana synchronizacja | Dzielone z Chrome |
| GDPR overlay | Problem z zamykaniem | Brak problemu |
| Wydajność | Wolniejszy | Szybszy (natywny) |
| Wygląd | Custom | Systemowy |
| Autofill | Nie działa | Działa |

## Co dalej
Jeśli napotkasz problemy, zgłoś je z:
1. Pełnymi logami Logcat (filtr: `tag:LoginScreenActivity|LoginV3Fragment|LoginV3Query`)
2. Opisem kroku po kroku co zrobiłeś
3. Wersją Androida i aplikacji Chrome
4. Czy to emulator czy prawdziwe urządzenie
