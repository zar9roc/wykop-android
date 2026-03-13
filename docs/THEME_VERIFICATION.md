# Theme Verification Guide

## Problem

Gdy definiujesz nową Activity w AndroidManifest.xml z atrybutem `android:theme`, musisz upewnić się, że dany theme jest zdefiniowany w pliku styles.xml lub themes.xml. W przeciwnym razie aplikacja crashuje w runtime z błędem:

```
android.view.InflateException: Binary XML file line #X: Error inflating class <Activity>
Caused by: android.content.res.Resources$NotFoundException: Resource ID #0xXXXXXXXX
```

## Używane Theme w Projekcie

### Zdefiniowane w `app/src/main/res/values/styles.xml`:

- **WykopAppTheme** (parent: Base.WykopAppTheme)
  - Bazowy theme aplikacji, używany jako domyślny dla całej aplikacji

- **WykopAppTheme.Dark** (parent: Theme.AppCompat.NoActionBar)
  - Ciemny wariant theme aplikacji
  - Używany w: LinkDetailsActivityV2, LoginScreenActivity, SettingsActivity, TwoFactorAuthorizationActivity, ProfileActivityV2

- **WykopAppTheme.Amoled** (parent: WykopAppTheme.Dark)
  - AMOLED wariant (czarne tło) theme aplikacji
  - Nie używany obecnie w manifeście, ale dostępny dla użytkownika w ustawieniach

- **TransparentActivityTheme**
  - Theme dla przezroczystych Activity (dialogi, overlays)
  - Używany w: PhotoViewActivity, EntryActivity, TagActivity, ConversationActivity, UpvotersActivity, DownvotersActivity, RelatedActivity, ProfileActivity, NotificationsListActivity, EmbedViewActivity

- **FullscreenActivityTheme** (parent: WykopAppTheme)
  - Theme dla pełnoekranowych Activity
  - Nie używany obecnie w manifeście

### Zdefiniowane w `ui/base/android/src/main/res/values/themes.xml`:

- **Theme.App.Light** (parent: Theme.MaterialComponents.Light.NoActionBar)
  - Jasny wariant nowego theme opartego na Material Components
  - Nie używany obecnie w manifeście

- **Theme.App.Dark** (parent: Theme.MaterialComponents.NoActionBar)
  - Ciemny wariant nowego theme opartego na Material Components
  - Nie używany obecnie w manifeście (zmigrowano do WykopAppTheme.Dark)

- **Theme.App.Amoled** (parent: Theme.App.Dark)
  - AMOLED wariant nowego theme
  - Nie używany obecnie w manifeście

### Zdefiniowane w `ui/base/android/src/main/res/values/styles.xml`:

Zawiera pomocnicze style widgetów używane przez główne theme:
- Widget.App.Toolbar / Widget.App.Toolbar.Dark / Widget.App.Toolbar.Amoled
- Widget.App.Tab / Widget.App.Tab.Dark
- Widget.App.TextInputLayout
- Widget.App.Button.TextButton

### Theme z Android Framework (nie wymagają definicji):

- **Theme.AppCompat.DayNight**
  - Theme z biblioteki AppCompat, automatycznie przełącza się między jasnym/ciemnym
  - Używany w: MainNavigationActivity

## Procedura Weryfikacji

### Przed dodaniem nowej Activity:

1. **Wybierz odpowiedni theme** z listy powyżej lub zdecyduj czy potrzebujesz nowego

2. **Jeśli używasz istniejącego theme**, upewnij się że:
   - Nazwa jest dokładnie taka sama (case-sensitive)
   - Pełna nazwa z prefixem (np. `@style/WykopAppTheme.Dark`)

3. **Jeśli tworzysz nowy theme**:
   - Zdefiniuj go w odpowiednim pliku:
     - `app/src/main/res/values/styles.xml` - dla theme związanych z legacy UI
     - `ui/base/android/src/main/res/values/themes.xml` - dla nowych theme Material Components
   - Wybierz odpowiedni parent style
   - Dodaj wpis do tego dokumentu

### Weryfikacja manualna:

```bash
# 1. Znajdź wszystkie użycia android:theme w manifeście
grep -n "android:theme" app/src/main/AndroidManifest.xml

# 2. Dla każdego znalezionego theme, sprawdź czy istnieje definicja:
grep -r "name=\"NazwaTheme\"" app/src/main/res/values/*.xml ui/base/android/src/main/res/values/*.xml
```

### Weryfikacja w Android Studio:

1. Otwórz `AndroidManifest.xml`
2. Kliknij na wartość `android:theme` (np. `@style/WykopAppTheme.Dark`)
3. Jeśli Android Studio podświetla na czerwono lub Ctrl+Click nie działa → theme nie istnieje
4. Jeśli Ctrl+Click przenosi do definicji → theme istnieje i jest poprawny

## Konwencje

### Nazewnictwo theme:

- **WykopAppTheme.XXX** - legacy theme, starsze Activity
- **Theme.App.XXX** - nowe theme Material Components, nowsze Activity
- **TransparentActivityTheme** - przezroczyste Activity (dialogi, overlays)

### Warianty kolorystyczne:

- Bez sufiksu (np. WykopAppTheme) - jasny wariant
- `.Dark` - ciemny wariant
- `.Amoled` - wariant AMOLED z czarnym tłem

### Parent styles:

- `Theme.AppCompat.*` - legacy AppCompat theme
- `Theme.MaterialComponents.*` - nowoczesne Material Components theme
- `WykopAppTheme` - własny bazowy theme projektu

## Historia zmian

### 2026-03-13
- Utworzenie dokumentu
- Zinwentaryzowanie wszystkich theme używanych w projekcie
- Dodanie procedury weryfikacji
- Poprzedni problem: Task #234 - LinkDetailsActivityV2 używał nieistniejącego theme, naprawiono na WykopAppTheme.Dark
- Migracja ThemableActivity activities (LoginScreenActivity, SettingsActivity, TwoFactorAuthorizationActivity, ProfileActivityV2) z Theme.App.Dark na WykopAppTheme.Dark dla spójności z ThemableActivity.updateTheme()

## Powiązane problemy

- [Task #234] Fix theme LinkDetailsActivityV2 - brakujący theme powodował crash
- ThemableActivity pattern - Activity które muszą respektować wybór theme użytkownika

## Przyszłe usprawnienia

Możliwe do dodania w przyszłości:
- Gradle task który automatycznie weryfikuje theme przed buildem
- Lint rule który wykrywa nieistniejące theme w czasie edycji
- Unit test który parsuje manifest i sprawdza istnienie każdego theme
