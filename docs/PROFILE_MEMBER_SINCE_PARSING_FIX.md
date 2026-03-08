# Fix: Parsowanie wieku konta w podglądzie profilu

## Problem
Podgląd profilu niepoprawnie parsował wiek konta. Dla użytkownika zarejestrowanego `2011-10-15 15:15:12` (powinno być "14 lat i 4-5 mies.") wyświetlał się błędny tekst, np. "102026 lat 2 mies." lub inny niepoprawny wynik.

## Przyczyna
Format daty `member_since` w API v3 to `"2011-10-15 15:15:12"` (ze spacją między datą a czasem), natomiast `Instant.parse()` wymaga formatu ISO-8601: `"2011-10-15T15:15:12Z"` (z literą 'T' zamiast spacji).

Gdy parsowanie zawodzi, kod używał fallback wartości `Instant.DISTANT_PAST` (około -1 miliard lat), co prowadzi do niepoprawnego obliczenia wieku konta.

## Rozwiązanie
Przed parsowaniem, zamieniamy spację na 'T' i dodajemy 'Z' (UTC):

```kotlin
val signupAt = runCatching {
    // member_since format: "2011-10-15 15:15:12" (with space)
    // Instant.parse() expects ISO-8601: "2011-10-15T15:15:12Z"
    // Replace space with 'T' and append 'Z' for UTC
    val isoFormat = memberSince.orEmpty().replace(" ", "T") + "Z"
    Instant.parse(isoFormat)
}.getOrElse { Instant.DISTANT_PAST }
```

## Pliki zmienione
1. **domain/src/main/kotlin/io/github/wykopmobilny/domain/profile/di/ProfileModule.kt** (linia 127)
   - Funkcja `UserFullResponseV3.toProfileEntity()` - parsowanie `memberSince` do `signupAt` przed zapisem do cache

2. **app/src/main/kotlin/io/github/wykopmobilny/ui/modules/profile/ProfileActivity.kt** (linia 101)
   - Funkcja `showProfile()` - parsowanie `memberSince` do wyświetlenia wieku konta

## Dodatkowe poprawki
W trakcie kompilacji naprawiono błędy override modifiers w 7 plikach:
- BaseEntriesFragment.kt
- BaseEntryCommentFragment.kt
- BaseEntryLinkFragment.kt
- BaseLinkCommentFragment.kt
- BaseLinksFragment.kt
- EntrySearchView.kt
- LinkSearchView.kt

Dodano `override` przed właściwością `showSearchEmptyView`.

## Testowanie
Po zastosowaniu poprawki:
1. Otwórz profil dowolnego użytkownika
2. Sprawdź czy wiek konta jest poprawnie wyświetlany (np. "14 lat 5 mies." dla użytkownika z 2011 roku)
3. Zweryfikuj dla różnych dat rejestracji

## Format funkcji `toPrettyString()`
Formatowanie okresu obsługiwane przez `DateTimePeriod.toPrettyString()`:
- Lata: "1 rok", "2-4 lata", "5-21 lat", "X lat"
- Miesiące: "X mies."
- Dni: "1 dzień", "X dni" (tylko gdy years == 0)
- Godziny: "X godz." (tylko gdy years == 0 && months == 0 && days == 0)
- Minuty: "X min" (tylko gdy brak wyższych jednostek)
- Fallback: "przed chwilą" (gdy wszystko == 0)
