# Rozwijalne Spoilery w Wpisach

## Podsumowanie

Dodano funkcjonalność rozwijalnych spoilerów w wpisach mikroblogowych. Spoilery w początkowej fazie wyświetlają się jako `[pokaż spoiler]`, a po kliknięciu rozwijają się do pełnej treści. Każdy spoiler działa niezależnie.

## Implementacja

### Pliki zmodyfikowane

1. **SpoilerClickableSpan.kt** (nowy plik)
   - Custom `ClickableSpan` obsługujący rozwijanie/zwijanie spoilerów
   - Przechowuje stan (expanded/collapsed) dla każdej instancji
   - Przy kliknięciu przełącza widoczność między `[pokaż spoiler]` a pełną treścią

2. **CodeTagHandler.kt**
   - Dodano obsługę tagu `<spoiler>`
   - Tag `<spoiler>` jest konwertowany na `SpoilerClickableSpan`
   - Zachowano istniejącą obsługę tagu `<code>` dla snippetów kodu

3. **HtmlUtils.kt**
   - Zmieniono konwersję spoilerów z `<code>` na `<spoiler>`
   - Linie zaczynające się od `!` są teraz parsowane jako `<spoiler>treść</spoiler>`

## Sposób działania

### Parsowanie

```kotlin
// Treść wpisu:
! To jest spoiler
Zwykły tekst

// Konwersja HTML:
<spoiler>To jest spoiler</spoiler><br>Zwykły tekst
```

### Renderowanie

1. HTML jest parsowany przez `HtmlCompat.fromHtml()` z `CodeTagHandler`
2. Tag `<spoiler>` jest wykrywany przez handler
3. Treść spoilera jest wyciągana i przechowywana w `SpoilerClickableSpan`
4. Tekst jest zamieniany na `[pokaż spoiler]` z clickable span
5. Po kliknięciu span zamienia tekst na pełną treść spoilera
6. Ponowne kliknięcie zwija spoiler z powrotem

### Przykład użycia

```kotlin
val content = """
Treść wpisu
! To jest ukryta treść
Więcej tekstu
! Kolejny spoiler
""".trimIndent()

val html = content.convertWykopContentToHtml()
textView.prepareBody(html) { url ->
    // handle link clicks
}
```

## Zachowanie

- **Stan początkowy**: Spoiler wyświetla się jako `[pokaż spoiler]` (niebieski, podkreślony link)
- **Po kliknięciu**: Rozwijany do pełnej treści (pozostaje jako clickable span)
- **Po ponownym kliknięciu**: Zwijany z powrotem do `[pokaż spoiler]`
- **Niezależność**: Każdy spoiler ma własny stan - kliknięcie jednego nie wpływa na inne

## Różnice względem poprzedniej implementacji

### Przed (Task #176)
- Spoilery renderowane jako `<code>` (monospace inline text)
- Treść zawsze widoczna, brak możliwości ukrycia

### Po (Task #178)
- Spoilery renderowane jako rozwijalne clickable spans
- Domyślnie ukryte jako `[pokaż spoiler]`
- Kliknięcie rozija/zwija treść
- Każdy spoiler działa oddzielnie

## Kwestie techniczne

### Detekt

Początkowa implementacja miała problem z regułą `FunctionOnlyReturningConstant`:
```kotlin
// Przed:
fun getCollapsedText(): String = "[pokaż spoiler]"

// Po:
companion object {
    const val COLLAPSED_TEXT = "[pokaż spoiler]"
}
```

### Backward Compatibility

Parametr `openSpoilersDialog` w `TextViewExtensions.kt` został oznaczony jako `@Suppress("UNUSED_PARAMETER")` dla zachowania kompatybilności wstecznej. Nowa implementacja nie używa już dialogów - spoilery rozwijają się inline.

## Pliki dotknięte

- `app/src/main/kotlin/io/github/wykopmobilny/utils/textview/SpoilerClickableSpan.kt` (nowy)
- `app/src/main/kotlin/io/github/wykopmobilny/utils/textview/CodeTagHandler.kt` (zmodyfikowany)
- `app/src/main/kotlin/io/github/wykopmobilny/utils/textview/HtmlUtils.kt` (zmodyfikowany)

## Testowanie

### Scenariusze testowe

1. **Jeden spoiler**: Wpis z jednym spoilerem powinien wyświetlać `[pokaż spoiler]`, kliknięcie rozija treść
2. **Wiele spoilerów**: Każdy spoiler działa niezależnie, można rozwinąć wybrane
3. **Spoiler + inne formatowanie**: Spoilery współpracują z bold, italic, linkami, etc.
4. **Puste spoilery**: Linia `!` bez treści nie powinna tworzyć spoilera (wymaga `length > 1`)

### Przykładowa treść do testów

```
! Pierwszy spoiler
Zwykły tekst **pogrubiony**
! Drugi spoiler z _kursywą_
Kolejny tekst
! Trzeci spoiler [z linkiem](https://wykop.pl)
```
