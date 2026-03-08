# Komendy ADB do Debugowania Aplikacji Wykop Android

Gotowe komendy copy-paste do testowania i debugowania aplikacji.

## Package Name

```bash
# Release
PACKAGE="io.github.wykopmobilny"

# Debug
PACKAGE="io.github.wykopmobilny.debug"
```

---

## 1. Deep Linki (Intent Tests)

### Link (Artykuł)
```bash
# Przykład: link z ID 123456
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://www.wykop.pl/link/1234567/test-artykulu" \
  io.github.wykopmobilny.debug

# Alternatywnie HTTP
adb shell am start -W -a android.intent.action.VIEW \
  -d "http://www.wykop.pl/link/9876543/inny-artykul" \
  io.github.wykopmobilny.debug
```

### Tag
```bash
# Tag: #heheszki
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://www.wykop.pl/tag/heheszki" \
  io.github.wykopmobilny.debug

# Tag z przestrzenią
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://www.wykop.pl/tag/programowanie" \
  io.github.wykopmobilny.debug
```

### Wpis (Entry)
```bash
# Wpis mikroblogu z ID
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://www.wykop.pl/wpis/12345678" \
  io.github.wykopmobilny.debug
```

### Ludzie (Profil Użytkownika)
```bash
# Profil użytkownika
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://www.wykop.pl/ludzie/username123" \
  io.github.wykopmobilny.debug
```

### Sprawdzenie czy Intent się uruchomił
```bash
# Pokaże ostatnio uruchomioną aktywność
adb shell dumpsys activity activities | grep mResumedActivity
```

---

## 2. Logcat - Filtry

### Podstawowe filtrowanie

```bash
# Wszystkie logi aplikacji (przez package name)
adb logcat --pid=$(adb shell pidof -s io.github.wykopmobilny.debug)

# Alternatywnie przez grep
adb logcat | grep "io.github.wykopmobilny"
```

### OkHttp - Requesty HTTP/HTTPS
```bash
# Wszystkie requesty OkHttp (networking)
adb logcat | grep -E "OkHttp|okhttp"

# Z poziomem DEBUG i wyżej
adb logcat OkHttp:D *:S

# Requesty + Responses
adb logcat | grep -E "OkHttp|-->|<--"
```

### Napier Tags - Logowanie Custom

Projekt używa Napier do logowania. Tagi odpowiadają nazwom klas.

```bash
# Konkretny tag (np. LoginV3Fragment)
adb logcat | grep "tag:LoginV3Fragment"

# Wiele tagów naraz (OR)
adb logcat | grep -E "tag:LoginV3Fragment|tag:LoginV3Query|tag:LoginScreenActivity"

# Wszystkie tagi zawierające "Login"
adb logcat | grep "tag:.*Login"

# Wszystkie tagi z "Repository"
adb logcat | grep "tag:.*Repository"
```

### API V3 - Debugging migracji
```bash
# Wszystkie wywołania API v3
adb logcat | grep "/api/v3/"

# API v3 + Napier tags
adb logcat | grep -E "/api/v3/|tag:.*V3"

# Błędy parsowania Moshi
adb logcat | grep "MoshiErrorBodyParserV3"
```

### Interceptory + Auth
```bash
# JWT + Bearer Auth
adb logcat | grep -E "JwtAuthInterceptor|BearerAuthInterceptor"

# API Signing
adb logcat | grep "ApiSignInterceptor"

# Wszystkie interceptory
adb logcat | grep "Interceptor"
```

### Błędy + Warningi
```bash
# Tylko errory
adb logcat *:E

# Error + Warning
adb logcat *:W

# Crashe + ANR
adb logcat | grep -E "FATAL|ANR"

# Stack traces
adb logcat | grep -A 20 "Exception"
```

### Kombinowane (Most Useful)
```bash
# OkHttp + API errors + Custom tags
adb logcat | grep -E "OkHttp|/api/v3/|tag:.*Repository|Exception"

# Networking + Auth flow
adb logcat | grep -E "OkHttp|JwtAuthInterceptor|BearerAuthInterceptor|/connect|/auth"

# Entry/Link operations (debug paginacji)
adb logcat | grep -E "EntriesRepository|LinksRepository|pagination|page="
```

---

## 3. dumpsys activity

### Aktywna Activity + Fragment
```bash
# Obecna resumed activity
adb shell dumpsys activity activities | grep mResumedActivity

# Wszystkie activities w stacku
adb shell dumpsys activity activities | grep "Run #"

# Top activity details
adb shell dumpsys activity top
```

### Intent Filters
```bash
# Sprawdzenie intent-filterów aplikacji
adb shell dumpsys package io.github.wykopmobilny.debug | grep -A 5 "android.intent.action.VIEW"

# Deep link handlers
adb shell dumpsys package io.github.wykopmobilny.debug | grep -E "pathPrefix|pathPattern|scheme|host"
```

### Activity Stack
```bash
# Full stack trace
adb shell dumpsys activity activities

# Tylko ostatni task
adb shell dumpsys activity activities | grep -A 30 "Task id #"
```

---

## 4. JDWP Port Forwarding (Remote Debugging)

### Setup dla Android Studio Debugger

```bash
# 1. Znajdź PID aplikacji
adb shell ps | grep io.github.wykopmobilny.debug

# LUB automatycznie
APP_PID=$(adb shell pidof -s io.github.wykopmobilny.debug)
echo "App PID: $APP_PID"

# 2. Forward JDWP port (domyślnie 8700)
adb forward tcp:8700 jdwp:$APP_PID

# 3. Sprawdź forwarding
adb forward --list

# 4. Usuń forwarding (cleanup)
adb forward --remove tcp:8700

# Usuń wszystkie forwardy
adb forward --remove-all
```

### Automatyczny skrypt (Bash)
```bash
#!/bin/bash
PACKAGE="io.github.wykopmobilny.debug"
PORT=8700

PID=$(adb shell pidof -s $PACKAGE)
if [ -z "$PID" ]; then
  echo "App is not running!"
  exit 1
fi

echo "Forwarding JDWP port $PORT for PID $PID"
adb forward tcp:$PORT jdwp:$PID
echo "Done. Attach debugger to localhost:$PORT"
```

### Android Studio: Attach to Process
1. Uruchom forwarding: `adb forward tcp:8700 jdwp:<PID>`
2. W Android Studio: `Run → Attach Debugger to Android Process`
3. Wybierz proces `io.github.wykopmobilny.debug`
4. Możesz teraz stawiać breakpointy

---

## 5. Użyteczne Komendy

### Instalacja + Uruchomienie
```bash
# Zainstaluj APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Odinstaluj
adb uninstall io.github.wykopmobilny.debug

# Uruchom główną aktywność
adb shell am start -n io.github.wykopmobilny.debug/.ui.modules.mainnavigation.MainNavigationActivity
```

### Clear Data + Cache
```bash
# Wyczyść dane aplikacji (factory reset)
adb shell pm clear io.github.wykopmobilny.debug

# Tylko cache
adb shell rm -rf /data/data/io.github.wykopmobilny.debug/cache/*

# SharedPreferences
adb shell run-as io.github.wykopmobilny.debug \
  rm /data/data/io.github.wykopmobilny.debug/shared_prefs/*
```

### Database (SQLDelight)
```bash
# Pokaż bazy danych
adb shell run-as io.github.wykopmobilny.debug \
  ls /data/data/io.github.wykopmobilny.debug/databases/

# Pull bazy do analizy
adb shell run-as io.github.wykopmobilny.debug \
  cat /data/data/io.github.wykopmobilny.debug/databases/wykop.db > wykop.db

# Usuń bazę (force reset schema)
adb shell run-as io.github.wykopmobilny.debug \
  rm /data/data/io.github.wykopmobilny.debug/databases/wykop.db
```

### Permissions
```bash
# Sprawdź uprawnienia
adb shell dumpsys package io.github.wykopmobilny.debug | grep permission

# Grant runtime permission
adb shell pm grant io.github.wykopmobilny.debug android.permission.POST_NOTIFICATIONS

# Revoke
adb shell pm revoke io.github.wykopmobilny.debug android.permission.POST_NOTIFICATIONS
```

### Screen Recording
```bash
# Nagrywaj ekran (max 3 minuty)
adb shell screenrecord /sdcard/test.mp4

# Pull nagranie
adb pull /sdcard/test.mp4 .

# Screenshot
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png .
```

---

## 6. Tips & Tricks

### Filtrowanie przez czas
```bash
# Logi z ostatnich 5 minut
adb logcat -t '01-08 12:00:00.000'

# Od teraz (clear buffer i pokaż nowe)
adb logcat -c && adb logcat
```

### Zapis do pliku
```bash
# Zapisz logi do pliku
adb logcat > logcat_$(date +%Y%m%d_%H%M%S).txt

# Z timestampem
adb logcat -v time > logcat.txt

# Z threadem + timestamp
adb logcat -v threadtime > logcat.txt
```

### Multiple Devices
```bash
# Lista urządzeń
adb devices

# Wybierz konkretne urządzenie
adb -s <device_id> logcat

# Wszystkie komendy z -s
adb -s emulator-5554 shell am start ...
```

### Network Stats
```bash
# Statystyki sieci dla aplikacji
adb shell dumpsys netstats | grep io.github.wykopmobilny

# Bieżące połączenia
adb shell netstat | grep io.github.wykopmobilny
```

---

## Quick Reference

### Najczęściej używane kombinacje

```bash
# 1. Testowanie deep linku + logi
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://www.wykop.pl/wpis/123" io.github.wykopmobilny.debug && \
adb logcat | grep -E "DeepLinkActivity|EntryActivity"

# 2. Clear + Install + Run + Logs
adb uninstall io.github.wykopmobilny.debug && \
adb install -r app-debug.apk && \
adb shell am start -n io.github.wykopmobilny.debug/.ui.modules.mainnavigation.MainNavigationActivity && \
adb logcat --pid=$(adb shell pidof -s io.github.wykopmobilny.debug)

# 3. API debugging (networking)
adb logcat | grep -E "OkHttp|/api/v3/|Exception|tag:.*Repository"

# 4. JDWP attach
adb forward tcp:8700 jdwp:$(adb shell pidof -s io.github.wykopmobilny.debug) && \
echo "Attach debugger to localhost:8700"
```

---

## Referencje

- [ADB Documentation](https://developer.android.com/studio/command-line/adb)
- [Logcat Documentation](https://developer.android.com/studio/command-line/logcat)
- [Intent Filters](https://developer.android.com/guide/components/intents-filters)
- `AndroidManifest.xml` - Deep link definitions (lines 140-276)
- Napier tags convention: `Qualifiers.kt`, naming matches class names
