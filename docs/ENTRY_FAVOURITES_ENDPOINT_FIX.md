# Fix: Routing API Ulubionych Wpisów Użytkownika

**Data:** 2026-03-08
**Task:** Naprawić routing API ulubionych wpisów (entries) użytkownika

## Problem

Ładowanie ulubionych wpisów (Entries) użytkownika poprzez endpoint:
```
https://wykop.pl/api/v3/observed/tags/stream
```
Było niepoprawne - endpoint zwracał wpisy z **obserwowanych tagów**, a nie **ulubione wpisy** użytkownika.

## Analiza

Po przeanalizowaniu dokumentacji API v3 (`docs/wykop_api_v3_openapi.yaml`) oraz rozwiązania z Task #168 (ulubione linki) okazało się, że:

1. **Endpoint `/v3/observed/tags/stream` jest poprawny** - zwraca wpisy z obserwowanych tagów
2. **Problem:** Był używany w niewłaściwym kontekście - do ładowania ulubionych wpisów zamiast obserwowanych tagów
3. **Poprawny endpoint to `/v3/favourites`** - Lista ulubionych (linia 3336 w OpenAPI spec)
4. Endpoint `/v3/favourites` zwraca **mieszane typy**: zarówno linki (`Link`) jak i wpisy (`Entry`)
   ```yaml
   items:
     anyOf:
       - $ref: '#/components/schemas/Link'
       - $ref: '#/components/schemas/Entry'
   ```

## Różnica między "Observed" a "Favourites"

W kontekście API Wykop:
- **Observed** (obserwowane) - dyskusje/użytkownicy/tagi które obserwujesz dla powiadomień
  - Endpoint: `/v3/observed/tags/stream` - zwraca wpisy z obserwowanych tagów
  - Endpoint: `/v3/observed/all` - zwraca wszystkie obserwowane treści
- **Favourites** (ulubione) - treści (linki + entries) które dodajesz do ulubionych
  - Endpoint: `/v3/favourites` - zwraca ulubione treści

**EntryFavoritePresenter** powinien ładować **ulubione wpisy** (favourites), a nie wpisy z obserwowanych tagów (observed/tags/stream).

## Rozwiązanie

### 1. Aktualizacja `EntriesRepository.getObserved()`

Zmieniono implementację aby:
- Używać `favouritesApiV3.getFavourites()` zamiast `entriesApiV3.getObservedTagsStream()`
- Filtrować tylko wpisy z wyników: `filterIsInstance<ObservedItemV3.EntryItem>()`
- Mapować `EntryItem.entry` do `EntryResponseV3`

**Przed:**
```kotlin
override fun getObserved(page: String?) =
    rxSingle { entriesApiV3.getObservedTagsStream(page) }
        .retryWhen(userTokenRefresher)
        .map { response ->
            response.data.orEmpty().filterEntriesV3(owmContentFilter, response.pagination)
        }
```

**Po:**
```kotlin
override fun getObserved(page: String?) =
    rxSingle { favouritesApiV3.getFavourites(page) }
        .retryWhen(userTokenRefresher)
        .map { response ->
            val entries =
                response.data
                    .orEmpty()
                    .filterIsInstance<ObservedItemV3.EntryItem>()
                    .map { it.entry }
            entries.filterEntriesV3(owmContentFilter, response.pagination)
        }
```

### 2. Dodanie importu `ObservedItemV3`

W `EntriesRepository.kt` dodano import:
```kotlin
import io.github.wykopmobilny.api.responses.v3.observed.ObservedItemV3
```

## Istniejąca Infrastruktura

Projekt już miał zaimplementowaną infrastrukturę do obsługi mieszanych typów (dodaną w Task #168):

1. **ObservedItemV3** - sealed class z dwoma wariantami:
   - `EntryItem(entry: EntryResponseV3)`
   - `LinkItem(link: LinkResponseV3)`

2. **ObservedItemV3Adapter** - Moshi adapter parsujący pole `"resource"` z JSON:
   - `"resource": "entry"` → `ObservedItemV3.EntryItem`
   - `"resource": "link"` → `ObservedItemV3.LinkItem`

3. **Konfiguracja Dagger:**
   - `FavouritesV3RetrofitApi` już był wstrzyknięty w `EntriesRepository`
   - Provider już istniał w `WykopModule.kt`
   - Deklaracja już była w `WykopApi.kt`

4. **Konfiguracja Moshi:**
   - `ObservedItemV3Adapter.FACTORY` już był zarejestrowany w `RetrofitModule.kt`

## Zmienione Pliki

1. **app/.../api/entries/EntriesRepository.kt**
   - Dodano import `ObservedItemV3`
   - Zaktualizowano implementację `getObserved()` aby używać nowego endpointu
   - Dodano filtrowanie `filterIsInstance<ObservedItemV3.EntryItem>()`

## Weryfikacja

Kompilacja projektu przebiegła pomyślnie:
```
BUILD SUCCESSFUL in 24s
658 actionable tasks: 49 executed, 4 from cache, 605 up-to-date
```

## Impact

- **Ekran ulubionych wpisów** (`EntryFavoritePresenter`) będzie teraz poprawnie ładował dane
- Użytkownicy zobaczą swoje **ulubione wpisy** zamiast wpisów z **obserwowanych tagów**
- Aplikacja używa teraz poprawnego endpointu zgodnego z dokumentacją API v3
- Spójne zachowanie z ekranem ulubionych linków (Task #168)

## Notatki

- Endpoint `/v3/favourites` zwraca zarówno linki jak i entries, ale dzięki filtrowi w repository, ekran ulubionych wpisów otrzymuje tylko wpisy
- Endpoint `/v3/observed/tags/stream` nadal istnieje i jest poprawny - służy do ładowania wpisów z obserwowanych tagów (inna funkcjonalność)
- Analogiczne rozwiązanie jak w Task #168 dla ulubionych linków
- Pełna dokumentacja OpenAPI v3 znajduje się w `docs/wykop_api_v3_openapi.yaml`

## Powiązane Taski

- **Task #168**: Naprawienie routingu API ulubionych linków (analogiczny problem, rozwiązanie posłużyło jako wzorzec)
