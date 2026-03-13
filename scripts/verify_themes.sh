#!/bin/bash

# Theme Verification Script
# Sprawdza czy wszystkie theme używane w AndroidManifest.xml są zdefiniowane w plikach zasobów

set -e

MANIFEST="app/src/main/AndroidManifest.xml"
STYLES_APP="app/src/main/res/values/styles.xml"
THEMES_UI_BASE="ui/base/android/src/main/res/values/themes.xml"
STYLES_UI_BASE="ui/base/android/src/main/res/values/styles.xml"

# Kolory
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🔍 Theme Verification Script"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Sprawdź czy pliki istnieją
if [ ! -f "$MANIFEST" ]; then
    echo -e "${RED}❌ Nie znaleziono AndroidManifest.xml: $MANIFEST${NC}"
    exit 1
fi

# Wyciągnij wszystkie android:theme z manifestu
# Ignoruje komentarze i linijki rozpoczynające się od <!--
themes=$(grep -v "<!--" "$MANIFEST" | grep -o 'android:theme="[^"]*"' | cut -d'"' -f2 | sort -u)

if [ -z "$themes" ]; then
    echo -e "${YELLOW}⚠️  Nie znaleziono żadnych theme w manifeście${NC}"
    exit 0
fi

echo "📋 Znalezione theme w manifeście:"
echo "$themes" | sed 's/^/  - /'
echo ""

errors=0
warnings=0

# Framework themes które nie wymagają definicji
framework_themes=(
    "@android:style/Theme.NoTitleBar.Fullscreen"
    "@style/Theme.AppCompat.DayNight"
)

# Sprawdź każdy theme
while IFS= read -r theme; do
    if [ -z "$theme" ]; then
        continue
    fi

    # Usuń prefix @style/ lub @android:style/
    theme_name="${theme#@style/}"
    theme_name="${theme_name#@android:style/}"

    # Sprawdź czy to framework theme
    is_framework=0
    for fw_theme in "${framework_themes[@]}"; do
        if [ "$theme" = "$fw_theme" ]; then
            is_framework=1
            break
        fi
    done

    if [ $is_framework -eq 1 ]; then
        echo -e "${GREEN}✓${NC} $theme (Android framework theme)"
        continue
    fi

    # Szukaj definicji theme
    found=0
    location=""

    if [ -f "$STYLES_APP" ]; then
        if grep -q "name=\"$theme_name\"" "$STYLES_APP"; then
            found=1
            location="$STYLES_APP"
        fi
    fi

    if [ $found -eq 0 ] && [ -f "$THEMES_UI_BASE" ]; then
        if grep -q "name=\"$theme_name\"" "$THEMES_UI_BASE"; then
            found=1
            location="$THEMES_UI_BASE"
        fi
    fi

    if [ $found -eq 0 ] && [ -f "$STYLES_UI_BASE" ]; then
        if grep -q "name=\"$theme_name\"" "$STYLES_UI_BASE"; then
            found=1
            location="$STYLES_UI_BASE"
        fi
    fi

    if [ $found -eq 1 ]; then
        echo -e "${GREEN}✓${NC} $theme"
        echo -e "  └─ Zdefiniowany w: ${location}"
    else
        echo -e "${RED}✗${NC} $theme"
        echo -e "  └─ ${RED}BŁĄD: Nie znaleziono definicji!${NC}"
        errors=$((errors + 1))
    fi
done <<< "$themes"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $errors -eq 0 ]; then
    echo -e "${GREEN}✅ Wszystkie theme są poprawnie zdefiniowane${NC}"
    exit 0
else
    echo -e "${RED}❌ Znaleziono $errors błędów${NC}"
    echo ""
    echo "Sprawdź czy theme jest zdefiniowany w:"
    echo "  - $STYLES_APP"
    echo "  - $THEMES_UI_BASE"
    echo "  - $STYLES_UI_BASE"
    echo ""
    echo "Zobacz dokumentację: docs/THEME_VERIFICATION.md"
    exit 1
fi
