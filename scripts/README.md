# Scripts

Katalog zawiera skrypty pomocnicze do weryfikacji i utrzymania projektu.

## verify_themes.sh

**Cel**: Weryfikuje czy wszystkie theme używane w AndroidManifest.xml są zdefiniowane w plikach zasobów.

**Użycie**:
```bash
# Bezpośrednio
./scripts/verify_themes.sh

# Przez Gradle
./gradlew verifyThemes
```

**Co robi**:
1. Wyciąga wszystkie `android:theme` z `app/src/main/AndroidManifest.xml`
2. Sprawdza czy są zdefiniowane w:
   - `app/src/main/res/values/styles.xml`
   - `ui/base/android/src/main/res/values/themes.xml`
   - `ui/base/android/src/main/res/values/styles.xml`
3. Ignoruje framework themes (np. `Theme.AppCompat.DayNight`, `@android:style/*`)
4. Zwraca exit code 0 jeśli wszystkie theme OK, 1 jeśli któryś brakuje

**Integracja z buildem**:
Task `verifyThemes` jest automatycznie uruchamiany przed `preBuild`, więc każdy build weryfikuje theme.

**Output przykładowy**:
```
🔍 Theme Verification Script
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 Znalezione theme w manifeście:
  - @style/Theme.App.Dark
  - @style/WykopAppTheme
  - @style/WykopAppTheme.Dark

✓ @style/Theme.App.Dark
  └─ Zdefiniowany w: ui/base/android/src/main/res/values/themes.xml
✓ @style/WykopAppTheme
  └─ Zdefiniowany w: app/src/main/res/values/styles.xml
✓ @style/WykopAppTheme.Dark
  └─ Zdefiniowany w: app/src/main/res/values/styles.xml

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Wszystkie theme są poprawnie zdefiniowane
```

**Dokumentacja**: Zobacz `docs/THEME_VERIFICATION.md` dla pełnej listy dostępnych theme.
