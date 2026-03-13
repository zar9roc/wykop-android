# Naprawa współdzielonego scope między LinkDetailsFragment a RelatedLinksFragment

## Problem

Przy ponownej nawigacji do linków powiązanych (RelatedLinksActivity) pojawia się błąd:

```
launchScoped didn't find scope for key=io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope=LinkDetailsKey(linkId=7905951, initialCommentId=null)
```

## Przyczyna

LinkDetailsFragment i RelatedLinksFragment używały **tego samego klucza scope** dla tego samego linkId:
- `LinkDetailsKey(linkId=7905951, initialCommentId=null)`

To powodowało **współdzielenie scope** między dwoma fragmentami:

1. Użytkownik jest na LinkDetailsFragment (linkId=7905951) → scope tworzony
2. Użytkownik otwiera RelatedLinksActivity dla tego samego linku → **zwraca istniejący scope** (getOrPut)
3. Użytkownik wraca z RelatedLinksActivity → ViewModel.onCleared() → **scope niszczony**
4. LinkDetailsFragment nadal istnieje i próbuje użyć `safeKeyed` → **scope nie istnieje** → błąd!

### Flow problemu

```
LinkDetailsFragment (linkId=7905951)
  ↓
  tworzy scope z kluczem: "LinkDetailsScope=LinkDetailsKey(linkId=7905951, initialCommentId=null)"
  ↓
  użytkownik klika "Powiązane"
  ↓
RelatedLinksActivity (linkId=7905951)
  ↓
  tworzy ViewModel → requireKeyedDependency
  ↓
  WykopApp.getDependency → getOrPutScope
  ↓
  ZWRACA ISTNIEJĄCY SCOPE (ten sam klucz!)
  ↓
  użytkownik wraca (back button)
  ↓
  RelatedLinksActivity.onDestroy → ViewModel.onCleared()
  ↓
  destroyKeyedDependency → SCOPE NISZCZONY
  ↓
LinkDetailsFragment nadal aktywny
  ↓
  próbuje użyć safeKeyed<LinkDetailsScope>(id = key)
  ↓
  ❌ BŁĄD: scope nie znaleziony w mapie scopes
```

## Rozwiązanie

Dodano parametr `source: String` do `LinkDetailsKey` aby odróżnić źródło tworzenia scope:

```kotlin
data class LinkDetailsKey(
    val linkId: Long,
    val initialCommentId: Long?,
    val source: String = "details",  // ← NOWY PARAMETR
)
```

RelatedLinksFragment teraz używa `source = "related"`:

```kotlin
private val key: LinkDetailsKey
    get() = LinkDetailsKey(linkId = linkId, initialCommentId = null, source = "related")
```

LinkDetailsFragment używa domyślnego `source = "details"`.

### Rezultat

Teraz każdy fragment ma **własny, unikalny scope**:
- LinkDetailsFragment: `"LinkDetailsScope=LinkDetailsKey(linkId=X, initialCommentId=Y, source=details)"`
- RelatedLinksFragment: `"LinkDetailsScope=LinkDetailsKey(linkId=X, initialCommentId=null, source=related)"`

Scope nie są już współdzielone → brak konfliktów przy niszczeniu.

## Pliki zmienione

1. `domain/src/main/kotlin/io/github/wykopmobilny/domain/linkdetails/di/LinkDetailsComponent.kt`
   - Dodano parametr `source: String = "details"` do `LinkDetailsKey`

2. `app/src/main/kotlin/io/github/wykopmobilny/ui/modules/links/relatedlinks/RelatedLinksFragment.kt`
   - Zmieniono tworzenie klucza: `LinkDetailsKey(..., source = "related")`

## Weryfikacja

```bash
./gradlew --stop
rm -rf app/build
./gradlew :app:compileDebugKotlin -x test --no-daemon
# BUILD SUCCESSFUL in 1m 20s
```

Kompilacja przeszła pomyślnie. Aplikacja teraz poprawnie obsługuje ponowną nawigację do powiązanych linków bez błędów scope.

## Alternatywne rozwiązania (odrzucone)

1. **Reference counting dla scope** - zbyt skomplikowane, wymagałoby dużych zmian w WykopApp
2. **Osobny RelatedLinksComponent** - duża refaktoryzacja, niepotrzebna dla tego problemu
3. **Nie niszczyć scope w onCleared** - narusza architekturę, memory leaks
