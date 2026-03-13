# Migracja na WindowInsetsCompat API - Plan

## Problem

Obecnie margines nad toolbarem (dla status bar) jest zdefiniowany jako stała wartość **24dp** (`@dimen/toolbar_top_padding`).
Na urządzeniach z innym rozmiarem status bar (np. tablety, urządzenia z wycięciem/notch, dynamic island) wartość 24dp może być za mała lub za duża.

**Obecna implementacja:**
- `toolbar_top_padding = 24dp` w `dimens.xml`
- `android:paddingTop="@dimen/toolbar_top_padding"` na toolbarach
- `android:fitsSystemWindows="true"` na toolbarach w `toolbar.xml` i `activity_conversation.xml`
- Stała wartość + `fitsSystemWindows` to podwójne dodawanie paddingu (fitsSystemWindows dodaje swoje insets + 24dp na wierzchu)

## Cel

Zastapic stala 24dp dynamicznym odczytem wysokosci status bar przez `WindowInsetsCompat` API.
Dzieki temu padding automatycznie dopasuje sie do rzeczywistej wysokosci status bar na kazdym urzadzeniu.

## Zasieg zmian

### Pliki XML uzywajace `toolbar_top_padding` (3 pliki):
1. `app/src/main/res/layout/toolbar.xml` - wspolny toolbar (includowany w 13 layoutach)
2. `app/src/main/res/layout/activity_conversation.xml` - bezposredni toolbar
3. `app/src/main/res/layout/activity_related_links.xml` - bezposredni toolbar

### Layouty includujace `toolbar.xml` (13 plikow):
- `activity_navigation.xml` (glowna nawigacja)
- `activity_addlink.xml`
- `activity_blacklist.xml`
- `activity_embedview.xml`
- `activity_entry.xml`
- `activity_link_details.xml`
- `activity_notifications.xml`
- `activity_photoview.xml`
- `activity_profile.xml`
- `activity_settings.xml`
- `activity_tag.xml`
- `activity_voterslist.xml`
- `activity_write_comment.xml`

### Pliki Kotlin do modyfikacji:
- `BaseActivity.kt` - centralne miejsce aplikowania insets

## Wymagania

- AndroidX Core KTX **1.17.0** (juz dostepne w projekcie - `google-androidxcore = "1.17.0"`)
- `WindowInsetsCompat` jest czescia `androidx.core:core-ktx`

## Plan implementacji

### Etap 1: Utworzenie utility function

Utworzyc extension function w `app/src/main/kotlin/io/github/wykopmobilny/utils/StatusBarInsetsUtils.kt`:

```kotlin
package io.github.wykopmobilny.utils

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Ustawia gorny padding toolbara na rzeczywista wysokosc status bar
 * uzywajac WindowInsetsCompat API zamiast stalej wartosci 24dp.
 */
fun Toolbar.applyStatusBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
        view.setPadding(
            view.paddingLeft,
            statusBarInsets.top,
            view.paddingRight,
            view.paddingBottom,
        )
        windowInsets
    }
}
```

### Etap 2: Modyfikacja XML layoutow

**toolbar.xml** - usunac `fitsSystemWindows="true"` i `paddingTop="@dimen/toolbar_top_padding"`:
```xml
<androidx.appcompat.widget.Toolbar
    ...
    android:layout_height="wrap_content"
    android:background="?attr/colorPrimaryDark"
    android:focusableInTouchMode="true"
    android:minHeight="?attr/actionBarSize"
    android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar"
    app:contentInsetStartWithNavigation="0dp"
    app:elevation="0dp"
    ... />
```

**activity_conversation.xml** - analogicznie usunac `fitsSystemWindows="true"` i `paddingTop`:
```xml
<androidx.appcompat.widget.Toolbar
    ...
    android:layout_height="56dp"
    android:background="?attr/colorPrimaryDark"
    ... />
```

**activity_related_links.xml** - usunac `paddingTop`:
```xml
<androidx.appcompat.widget.Toolbar
    ...
    android:layout_height="?attr/actionBarSize"
    android:background="?attr/colorPrimaryDark"
    ... />
```

### Etap 3: Aplikacja insets w BaseActivity

W `BaseActivity.onCreate()` po `super.onCreate(savedInstanceState)` dodac:

```kotlin
import io.github.wykopmobilny.utils.applyStatusBarInsets

override fun onCreate(savedInstanceState: Bundle?) {
    // ... existing code ...
    super.onCreate(savedInstanceState)

    // Dynamiczne dopasowanie toolbara do wysokosci status bar
    findViewById<Toolbar>(R.id.toolbar)?.applyStatusBarInsets()

    // ... rest of existing code ...
}
```

Dzieki temu **wszystkie 15 Activity** (13 z include + conversation + related_links) automatycznie otrzymaja dynamiczny padding.

### Etap 4: Opcjonalne czyszczenie

- Usunac `<dimen name="toolbar_top_padding">24dp</dimen>` z `dimens.xml` (jesli nie jest uzywane nigdzie indziej)
- Usunac konwencje z pamieci projektu ("Margines nad toolbarem definiowany przez @dimen/toolbar_top_padding")

## Korzysci

1. **Dynamiczny padding** - automatyczne dopasowanie do rzeczywistej wysokosci status bar
2. **Obsluga notch/cutout** - poprawne renderowanie na urzadzeniach z wycieciem
3. **Centralne zarzadzanie** - jeden punkt w BaseActivity zamiast powielania w XML
4. **Eliminacja podwojnego paddingu** - usuwamy konflikt miedzy `fitsSystemWindows` a stalym `paddingTop`

## Ryzyka i mitygacja

| Ryzyko | Mitygacja |
|--------|-----------|
| Activity bez toolbar (np. LoginScreenActivity) | `findViewById` zwroci null - bezpiecznie pominiete |
| Rozny layout toolbar w conversation/related_links | Uzycie tego samego `R.id.toolbar` - dziala jednakowo |
| Brak status bar insets na starszych urzadzeniach | WindowInsetsCompat zapewnia wsteczna kompatybilnosc |
| Zmiana wizualna na urzadzeniach testowych | 24dp to standardowa wartosc status bar - zmiana bedzie minimalna lub zerowa na wielu urzadzeniach |

## Weryfikacja

1. Zbudowac aplikacje: `./gradlew assembleDebug`
2. Przetestowac na emulatorze standardowym (24dp status bar) - brak zmiany wizualnej
3. Przetestowac na emulatorze z notch (API 28+) - toolbar dopasowuje sie do wiekszego status bar
4. Sprawdzic dark/light/amoled theme - wszystkie powinny dzialac identycznie
5. Sprawdzic activity_conversation i activity_related_links osobno (bezposredni toolbar)
