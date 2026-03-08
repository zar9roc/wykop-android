# Fix: Routing API Ulubionych Linków Użytkownika

**Data:** 2026-03-08
**Task:** Naprawić routing API ulubionych linków użytkownika

## Problem

Ładowanie ulubionych znalezisk (Links) użytkownika poprzez adres:
```
https://wykop.pl/api/v3/links/observed
```
Było niepoprawne i zwracany był błąd 404 - nieprawidłowy routing.

## Analiza

Po przeanalizowaniu dokumentacji API v3 (`docs/wykop_api_v3_openapi.yaml`) okazało się, że:

1. **Endpoint `/v3/links/observed` nie istnieje** w API v3
2. **Poprawny endpoint to `/v3/favourites`** - Lista ulubionych (linia 3336 w OpenAPI spec)
3. Endpoint `/v3/favourites` zwraca **mieszane typy**: zarówno linki (`Link`) jak i wpisy (`Entry`)
   ```yaml
   items:
     anyOf:
       - $ref: '#/components/schemas/Link'
       - $ref: '#/components/schemas/Entry'
   ```

## Różnica między "Observed" a "Favourites"

W kontekście API Wykop:
- **Observed** (obserwowane) - dyskusje/użytkownicy/tagi które obserwujesz dla powiadomień
  - Endpoint: `/v3/observed/all` - zwraca obserwowane treści
- **Favourites** (ulubione) - treści (linki + entries) które dodajesz do ulubionych
  - Endpoint: `/v3/favourites` - zwraca ulubione treści

W starym API v2 był endpoint `/links/observed` który odpowiadał za "ulubione linki", ale nazwa była myląca.

## Rozwiązanie

### 1. Dodanie metody `getFavourites()` do `FavouritesV3RetrofitApi`

```kotlin
@GET("v3/favourites")
suspend fun getFavourites(
    @Query("page") page: String? = null,
    @Query("limit") limit: Int? = null,
): WykopApiResponseV3<List<ObservedItemV3>>
```

**Typ zwracany:** `List<ObservedItemV3>` - sealed class obsługująca oba typy (Link i Entry)

### 2. Deprecation starej metody w `LinksV3RetrofitApi`

```kotlin
@Deprecated(
    "Endpoint /v3/links/observed does not exist in API v3. Use FavouritesV3RetrofitApi.getFavourites() instead, which returns both links and entries.",
    ReplaceWith("favouritesApiV3.getFavourites(page)"),
)
@GET("v3/links/observed")
suspend fun getObserved(...)
```

### 3. Aktualizacja `LinksRepository.getObserved()`

Zmieniono implementację aby:
- Używać `favouritesApiV3.getFavourites()` zamiast `linksApiV3.getObserved()`
- Filtrować tylko linki z wyników: `filterIsInstance<ObservedItemV3.LinkItem>()`
- Mapować `LinkItem.link` do `LinkResponseV3`

```kotlin
override fun getObserved(page: String?) =
    rxSingle { favouritesApiV3.getFavourites(page) }
        .retryWhen(userTokenRefresher)
        .map { response ->
            val links =
                response.data
                    .orEmpty()
                    .filterIsInstance<ObservedItemV3.LinkItem>()
                    .map { it.link }
            links.filterLinksV3(owmContentFilter, response.pagination)
        }
```

## Istniejąca Infrastruktura

Na szczęście projekt już miał zaimplementowaną infrastrukturę do obsługi mieszanych typów:

1. **ObservedItemV3** - sealed class z dwoma wariantami:
   - `EntryItem(entry: EntryResponseV3)`
   - `LinkItem(link: LinkResponseV3)`

2. **ObservedItemV3Adapter** - Moshi adapter parsujący pole `"resource"` z JSON:
   - `"resource": "entry"` → `ObservedItemV3.EntryItem`
   - `"resource": "link"` → `ObservedItemV3.LinkItem`

3. **Konfiguracja Dagger:**
   - `FavouritesV3RetrofitApi` już był wstrzyknięty w `LinksRepository`
   - Provider już istniał w `WykopModule.kt`
   - Deklaracja już była w `WykopApi.kt`

4. **Konfiguracja Moshi:**
   - `ObservedItemV3Adapter.FACTORY` już był zarejestrowany w `RetrofitModule.kt`

## Zmienione Pliki

1. **data/wykop/api/.../FavouritesV3RetrofitApi.kt**
   - Dodano metodę `getFavourites()`

2. **data/wykop/api/.../LinksV3RetrofitApi.kt**
   - Dodano `@Deprecated` do `getObserved()`

3. **app/.../api/links/LinksRepository.kt**
   - Zaktualizowano implementację `getObserved()` aby używać nowego endpointu
   - Dodano import `ObservedItemV3`
   - Dodano filtrowanie `filterIsInstance<ObservedItemV3.LinkItem>()`

## Weryfikacja

Kompilacja projektu przebiegła pomyślnie:
```
BUILD SUCCESSFUL in 18s
317 actionable tasks: 317 up-to-date
```

## Impact

- **Ekran ulubionych linków** (`LinksFavoritePresenter`) będzie teraz poprawnie ładował dane
- Błąd 404 na endpoincie `/v3/links/observed` nie będzie się już pojawiał
- Aplikacja używa teraz poprawnego endpointu zgodnego z dokumentacją API v3

## Notatki

- Endpoint `/v3/favourites` zwraca zarówno linki jak i entries, ale dzięki filtrowi w repository, ekran ulubionych linków otrzymuje tylko linki
- Jeśli w przyszłości będzie potrzebny ekran pokazujący wszystkie ulubione (linki + entries), można użyć tego samego endpointu bez filtrowania
- Pełna dokumentacja OpenAPI v3 znajduje się w `docs/wykop_api_v3_openapi.yaml`
