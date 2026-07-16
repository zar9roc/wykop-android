# Analiza crashlogów 2026-07-15 → 2026-07-16

**Źródło:** `crashlogs/log.txt` z urządzenia (build release ~1.5.0, R8 zaciemniony -
numery linii odnoszą się do stanu sprzed commitów z 2026-07-16).
**Zakres:** 2026-07-15 18:58 → 2026-07-16 19:06.
**Bilans:** 5 × FATAL, 6 × ERROR, 111 × WARNING.

## FATAL (crashe aplikacji)

### 1-2. `MonthYearPickerDialog.setYear` — ArrayIndexOutOfBoundsException (2×) ✅ naprawione

```
java.lang.ArrayIndexOutOfBoundsException: length=8; index=12
  at android.widget.NumberPicker.updateInputTextView / setDisplayedValues
  at MonthYearPickerDialog.kt:113 / :116  (onValueChange yearPickera)
```

Dokładnie scenariusz z analizy planu: tryb "Cały rok" (index 12) + zmiana roku na
bieżący (lipiec = 7 miesięcy + "Cały rok" = tablica 8 elementów). Naprawione
w `5608d3f2` (przepisany `setYear`, kolejność: null → zakres → tablica).
**Potwierdzone przez użytkownika na urządzeniu.**

### 3. `PhotoViewActions.addImageToGallery` — "Mutation of _data is not allowed" ✅ naprawione teraz

```
java.lang.IllegalArgumentException: Mutation of _data is not allowed.
  at ContentResolver.insert
  at PhotoViewActions.kt:53 (ścieżka shareImage → addImageToGallery)
```

Rejestracja udostępnianego obrazka w galerii przez `ContentResolver.insert`
z kolumną `MediaStore.MediaColumns.DATA` — zabronione od Androida 10.
**Fix:** `MediaScannerConnection.scanFile()` zamiast inserta (działa na każdym API).

### 4. `YTPlayer.onCreate` — "Developer key cannot be null or empty" ✅ naprawione teraz

```
java.lang.IllegalArgumentException: Developer key cannot be null or empty
  at YTPlayer.kt:450 (YouTubePlayerView.initialize)
```

Build bez skonfigurowanego `wykop.youtubeKey` (`findProperty` zwraca `""`) →
crash przy każdym otwarciu YT we wbudowanym playerze. **Fix:** guard w `onCreate` -
pusty klucz otwiera wideo w aplikacji YouTube / przeglądarce i zamyka aktywność.
Dodatkowo opcja "player YouTube" w ustawieniach **wyszarzona** (`isEnabled = false`) -
player oparty jest o martwe API Google (etap 5 planu: wymiana na
android-youtube-player / NewPipeExtractor).

### 5. `EllipsizingTextView` — "PARAGRAPH span must end at paragraph boundary" ✅ naprawione teraz

```
java.lang.RuntimeException: PARAGRAPH span must end at paragraph boundary (401 follows h)
  at SpannableStringBuilder.setSpan ← TextUtils.copySpansFrom
  at EllipsizingTextView.kt:174 (onDraw → createEllipsizedText)
```

Treść v3 to markdown → HTML z cytatami/listami (QuoteSpan/BulletSpan mają flagę
`SPAN_PARAGRAPH`). Przy przycinaniu ("pokaż całość") `copySpansFrom` przenosił
span, którego koniec po ucięciu lądował w środku wiersza → crash w onDraw
(wywala scroll listy). **Fix:** własny `copySpansSafely` — przycina zakresy do
długości celu i pomija spany, których nie da się osadzić (`runCatching` per span).

## ERROR (nie-fatalne)

- **`HTTP 400` z `LinksRepository.kt:55` (2×, "Unknown error")** — toggle ulubionych
  (`POST/DELETE /v3/favourites`). UWAGA: to komplikuje diagnozę bugu ulubionych -
  obok problemu z parsowaniem 204 serwer potrafi też odpowiedzieć **400** (najpewniej
  dla `type="link_comment"` — być może zła wartość type/source_id). Po fixie
  `requireSuccessful` błąd propaguje się czysto jako HttpException; jeśli 400 dalej
  występuje po aktualizacji, trzeba podejrzeć body błędu (ErrorBodyParserV3) i
  zweryfikować wartości `type` — **do sprawdzenia na urządzeniu po 1.5.0.003**.
- **"Something wasn't safe 😬" (3×)** — `JobCancellationException` łapane przez
  wrapper `safe`; szum przy zamykaniu ekranów, nie błąd.

## WARNING (szum w logach)

- **96× "Invalid dimension resource"** (2 atrybuty × 48 wystąpień, każde z pełnym
  stacktrace) — `EmbedMediaView` czyta `?cornerRadius`/`?outlineWidth`, zdefiniowane
  tylko w motywach nowego stacku (`Theme.App.*`), a inflacja szła pod legacy
  `WykopAppTheme*`. ✅ Dodane `cornerRadius`/`outlineWidth`/`colorOutline` do
  `Base.WykopAppTheme`, `WykopAppTheme.Dark` (Amoled dziedziczy) — znika spam
  i embedy w komentarzach v3 dostają poprawne obramowania.
- **13× "No bearer token available"** — start bez tokenu (przed logowaniem /
  odświeżeniem); częściowo zaadresowane w `3f341e0e`, obserwować.
- **2× "Report functionality not available in API v3"** — stary stub; zgłaszanie
  wróciło w `20529e3f` (WebView z sesją).

## Wnioski

1. Wszystkie 5 crashy z logów ma fix (2 wcześniej, 3 w tym commicie).
2. Bug ulubionych może mieć DWA oblicza: 204-parse (naprawione) i HTTP 400
   dla komentarzy znalezisk (wymaga weryfikacji na urządzeniu, ewentualnie
   poprawki wartości `type` w `FavouriteRequestV3`).
3. Po wgraniu nowego builda logi powinny być o rząd wielkości czystsze -
   łatwiej będzie wyłapać kolejne realne problemy.
