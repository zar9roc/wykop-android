#!/bin/bash
# Debug Actions Usage Examples
# Przykładowe komendy ADB do sterowania aplikacją wykop-android w trybie debug

# PACKAGE NAME - zmień jeśli używasz innego build variant
PACKAGE="io.github.wykopmobilny.debug"

echo "=== Debug State Receiver - Example Commands ==="
echo ""

# 1. DEBUG_STATE - sprawdź stan aplikacji
echo "1. Sprawdź aktualny stan aplikacji:"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE"
echo "   adb logcat -s DebugState -d | tail -1"
echo ""

# 2. DEBUG_STATE verbose
echo "2. Sprawdź stan aplikacji (verbose - z back stack i device info):"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE --ez verbose true"
echo "   adb logcat -s DebugState -d | tail -1"
echo ""

# 3. DEBUG_CLEAR_CACHE
echo "3. Wyczyść cache aplikacji:"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_CLEAR_CACHE"
echo "   adb logcat -s DebugState -d | tail -1"
echo ""

# 4. DEBUG_LOGOUT
echo "4. Wyloguj użytkownika:"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_LOGOUT"
echo "   adb logcat -s DebugState -d | tail -1"
echo ""

# 5. DEBUG_SWITCH_TAB - różne zakładki
echo "5. Przełącz zakładki:"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab \"promoted\""
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab \"upcoming\""
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab \"hits\""
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab \"hot\""
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab \"mywykop\""
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab \"favorite\""
echo ""

echo "=== Scenariusze testowe ==="
echo ""

# Scenariusz 1: Full reset
echo "Scenariusz 1: Pełny reset aplikacji"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_LOGOUT"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_CLEAR_CACHE"
echo "   adb shell am force-stop $PACKAGE"
echo "   adb shell am start -n $PACKAGE/.ui.modules.splash.SplashActivity"
echo ""

# Scenariusz 2: Test nawigacji
echo "Scenariusz 2: Test nawigacji między zakładkami"
echo "   for tab in promoted upcoming hits hot; do"
echo "       adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab \"\$tab\""
echo "       sleep 2"
echo "       adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE"
echo "       adb logcat -s DebugState -d | tail -1 | grep '\"fragment\"'"
echo "   done"
echo ""

# One-liner
echo "=== One-liners ==="
echo ""
echo "Sprawdź stan i wyświetl:"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE && adb logcat -s DebugState -d | tail -1"
echo ""
echo "Wyczyść cache i sprawdź wynik:"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_CLEAR_CACHE && adb logcat -s DebugState -d | tail -1"
echo ""
echo "Wyloguj i sprawdź wynik:"
echo "   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_LOGOUT && adb logcat -s DebugState -d | tail -1"
echo ""

echo "=== Funkcje interaktywne ==="
echo ""

# Funkcja do uruchomienia akcji z automatycznym wyświetleniem wyniku
function debug_action() {
    local action=$1
    local extras=$2
    echo "Wykonuję: $action $extras"
    adb shell am broadcast -a "$action" $extras
    sleep 0.5
    echo "Wynik:"
    adb logcat -s DebugState -d | tail -1
    echo ""
}

# Przykłady użycia funkcji
echo "Funkcja debug_action - przykłady użycia:"
echo "   debug_action io.github.wykopmobilny.debug.DEBUG_STATE"
echo "   debug_action io.github.wykopmobilny.debug.DEBUG_STATE \"--ez verbose true\""
echo "   debug_action io.github.wykopmobilny.debug.DEBUG_CLEAR_CACHE"
echo "   debug_action io.github.wykopmobilny.debug.DEBUG_LOGOUT"
echo "   debug_action io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB \"--es tab promoted\""
echo ""

echo "=== Dostępne akcje ==="
echo "DEBUG_STATE        - Dump stanu aplikacji (Activity, Fragment, User)"
echo "DEBUG_CLEAR_CACHE  - Czyszczenie cache aplikacji"
echo "DEBUG_LOGOUT       - Wylogowanie użytkownika"
echo "DEBUG_SWITCH_TAB   - Przełączenie zakładki (wymaga --es tab \"nazwa\")"
echo ""

echo "=== Dostępne zakładki dla DEBUG_SWITCH_TAB ==="
echo "promoted    - Strona główna (promoted links)"
echo "upcoming    - Wykopalisko"
echo "hits        - Hity"
echo "hot         - Mikroblog (hot entries)"
echo "mywykop     - Mój Wykop"
echo "favorite    - Ulubione"
echo "search      - Wyszukiwanie"
echo "messages    - Wiadomości prywatne"
echo "notifications - Powiadomienia"
echo ""

echo "Aby wykonać konkretną komendę, skopiuj i wklej do terminala."
echo "Wszystkie wyniki są logowane z tagiem 'DebugState' w logcat."
