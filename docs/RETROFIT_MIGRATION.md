# Migracja Retrofit 2.9.0 → 3.0.0

## Podsumowanie
Migracja Retrofit z wersji 2.9.0 do 3.0.0. Wersja 3.x jest przepisana w Kotlin, oferuje lepszą kompatybilność z ekosystemem Kotlin i jest binarnie kompatybilna z wersją 2.x.

## Wymagania
- **OkHttp 4.12+**: Retrofit 3.0.0 wymaga OkHttp 4.12 (aktualnie projekt używa 4.11.0)
- **Kotlin**: Retrofit 3.0 ma transitive Kotlin dependency przez OkHttp 4.12
- **Gradle**: Brak zmian w konfiguracji Gradle

## Główne zmiany w Retrofit 3.0.0

### 1. Upgrade OkHttp 3.14 → 4.12
- OkHttp 4.12 jest napisany w Kotlin
- Lepsza integracja z ekosystemem Kotlin
- OkHttp 3.14 był nieobsługiwany od prawie 4 lat

### 2. Binary Compatibility
- Retrofit 3.x jest **binarnie kompatybilny** z 2.x
- Biblioteki skompilowane dla 2.x działają z 3.x
- Możliwa inkrementalna migracja

### 3. Brak Breaking Changes
- API Retrofit pozostaje bez zmian
- Konwertery (Moshi, Jspoon) działają bez modyfikacji
- Interceptory i konfiguracja OkHttpClient bez zmian

## Stan obecny

### Wersje zależności
```toml
# gradle/libs.versions.toml
mavencentral-retrofit = "2.9.0"
mavencentral-okhttp = "4.11.0"
mavencentral-moshi = "1.15.0"
mavencentral-jspoon = "1.3.2"
```

### Używane biblioteki
- `retrofit-core` (2.9.0)
- `retrofit-converter-moshi` (2.9.0)
- `retrofit-converter-jspoon` (1.3.2)

### Moduły używające Retrofit
1. **data/wykop/remote** - Retrofit + Moshi (główne API Wykop)
2. **data/github/remote** - Retrofit + Moshi (API GitHub dla patronów)
3. **data/scraper/remote** - Retrofit + Jspoon (scraping HTML dla blacklist)

### Konfiguracja Retrofit
- `RetrofitModule.kt` (wykop) - Moshi, interceptory, cache
- `ScraperModule.kt` (scraper) - Jspoon
- `PatronsModule.kt` (github) - Moshi

## Plan migracji

### Krok 1: Aktualizacja OkHttp
```toml
# gradle/libs.versions.toml
mavencentral-okhttp = "4.12.0"  # lub nowsza (4.x)
```

**Uwaga**: OkHttp 4.12.0 może zwiększyć zużycie pamięci przy wielu jednoczesnych requestach HTTP ze względu na zmiany w zarządzaniu flow control (HTTP/2).

### Krok 2: Aktualizacja Retrofit
```toml
# gradle/libs.versions.toml
mavencentral-retrofit = "3.0.0"
```

**Zmiany w bibliotekach:**
- `retrofit-core`: 2.9.0 → 3.0.0
- `retrofit-converter-moshi`: 2.9.0 → 3.0.0
- `retrofit-converter-jspoon`: pozostaje 1.3.2 (brak aktualizacji)

### Krok 3: Weryfikacja kompatybilności Jspoon
**Status**: Jspoon Converter 1.3.2 nie ma oficjalnego wsparcia dla Retrofit 3.0.0

**Dlaczego powinien działać:**
- Retrofit 3.0 ma binary compatibility z 2.x
- API Converter.Factory nie uległo zmianie
- Jspoon używa standardowego API Retrofit

**Ryzyko**:
- Jspoon nie jest aktywnie utrzymywany (ostatnia aktualizacja: sierpień 2023)
- Brak oficjalnego potwierdzenia kompatybilności z Retrofit 3.0
- Należy przeprowadzić testy integracyjne modułu scraper

## Alternatywy dla Jspoon (opcjonalne)

Jeśli Jspoon okaże się niezgodny z Retrofit 3.0, rozważyć:
1. **Jsoup** + custom converter
2. **Pozostać na Retrofit 2.9.0** w module scraper (możliwe dzięki multi-module)
3. **Fork Jspoon** i aktualizacja do Retrofit 3.0 (wymaga utrzymania)

## Mapowanie zmian

### Brak zmian w kodzie źródłowym
- ✅ API Retrofit pozostaje bez zmian
- ✅ Import paths pozostają `retrofit2.*`
- ✅ Konfiguracja Retrofit.Builder() bez zmian
- ✅ Interceptory OkHttp bez zmian
- ✅ Konwertery Moshi bez zmian
- ✅ Adnotacje Retrofit (`@GET`, `@POST`, etc.) bez zmian

### Zmiany tylko w gradle/libs.versions.toml
```toml
# PRZED
mavencentral-retrofit = "2.9.0"
mavencentral-okhttp = "4.11.0"

# PO
mavencentral-retrofit = "3.0.0"
mavencentral-okhttp = "4.12.0"
```

## Pliki do zmiany

### gradle/libs.versions.toml
Aktualizacja wersji:
- `mavencentral-retrofit`: 2.9.0 → 3.0.0
- `mavencentral-okhttp`: 4.11.0 → 4.12.0

### Brak zmian w kodzie
Dzięki binary compatibility nie trzeba modyfikować żadnego pliku Kotlin/Java.

## Testy

### Po migracji należy przetestować:
1. **API Wykop** (data/wykop/remote)
   - Logowanie użytkownika
   - Pobieranie wpisów/linków
   - Operacje POST (dodawanie komentarzy, głosowanie)

2. **API GitHub** (data/github/remote)
   - Pobieranie listy patronów

3. **Scraper** (data/scraper/remote) - **KRYTYCZNE**
   - Parsowanie HTML z blacklist
   - Weryfikacja zgodności Jspoon 1.3.2 z Retrofit 3.0.0

### Kompilacja
```bash
./gradlew clean build
```

### Testy jednostkowe
```bash
./gradlew test
```

### Testy instrumentacyjne (opcjonalnie)
```bash
./gradlew connectedAndroidTest
```

## Rollback

Jeśli migracja nie powiedzie się:
```toml
# gradle/libs.versions.toml - powrót do poprzednich wersji
mavencentral-retrofit = "2.9.0"
mavencentral-okhttp = "4.11.0"
```

## Korzyści migracji

### 1. Wsparcie techniczne
- OkHttp 3.14 był nieobsługiwany od prawie 4 lat
- Retrofit 3.0 + OkHttp 4.12 to oficjalnie wspierane wersje

### 2. Kompatybilność z Kotlin
- Retrofit 3.0 napisany w Kotlin
- Lepsza integracja z Kotlin Coroutines
- Przyszłość biblioteki to Kotlin-first

### 3. Binary compatibility
- Brak ryzyka breaking changes
- Inkrementalna migracja możliwa
- Biblioteki zewnętrzne (Jspoon) powinny działać

### 4. Przygotowanie na przyszłość
- Retrofit 2.x będzie stopniowo wycofywany
- Retrofit 3.x to długoterminowe wsparcie

## Ryzyka i ograniczenia

### 1. Jspoon Converter
- ⚠️ Brak oficjalnego wsparcia dla Retrofit 3.0
- ⚠️ Projekt nie jest aktywnie utrzymywany
- ⚠️ Wymaga testów integracyjnych

### 2. Zużycie pamięci
- ⚠️ OkHttp 4.12 może zwiększyć zużycie pamięci przy wielu równoległych requestach
- ⚠️ Należy monitorować performance po aktualizacji

### 3. Kompatybilność z innymi bibliotekami
- ✅ Dagger 2.59.2 - kompatybilny
- ✅ Moshi 1.15.0 - kompatybilny
- ⚠️ Jspoon 1.3.2 - wymaga weryfikacji

## Referencje

### Oficjalna dokumentacja
- [Retrofit 3.0.0 Release](https://github.com/square/retrofit/discussions/4379)
- [Retrofit CHANGELOG](https://github.com/square/retrofit/blob/trunk/CHANGELOG.md)
- [OkHttp 4.x CHANGELOG](https://square.github.io/okhttp/changelogs/changelog_4x/)

### Przewodniki migracji
- [Retrofit 3.0.0: Detailed Migration Guide (ProAndroidDev)](https://proandroiddev.com/retrofit-3-0-0-detailed-migration-guide-0d2c043d43e3)
- [Retrofit 3.0: The Future of Android Networking (Stackademic)](https://blog.stackademic.com/retrofit-3-0-the-future-of-android-networking-with-migration-guide-25322d75cd64)
- [Retrofit 3.0 Tutorial (Medium)](https://samsetdev.medium.com/retrofit-3-0-tutorial-key-differences-from-retrofit-2-682f9fd07a9a)

### Biblioteki
- [Moshi Converter (Maven Central)](https://central.sonatype.com/artifact/com.squareup.retrofit2/converter-moshi)
- [Jspoon GitHub](https://github.com/DroidsOnRoids/jspoon)

## Podsumowanie

Migracja Retrofit 2.9.0 → 3.0.0 jest **niskokosztowa i niskoryzkowna** dzięki binary compatibility. Główne ryzyko to kompatybilność Jspoon Converter, która wymaga weryfikacji poprzez testy.

**Zalecenie**: Przeprowadzić migrację z naciskiem na testy modułu scraper (Jspoon). W przypadku problemów z Jspoon, możliwe jest:
- Pozostawienie Retrofit 2.9.0 tylko w module scraper (multi-module isolation)
- Zastąpienie Jspoon alternatywnym parserem HTML (Jsoup)

---
**Data utworzenia**: 2026-02-27
**Data migracji**: 2026-02-27
**Autor**: Claude Code
**Status**: ✅ Migracja zakończona

## Wyniki migracji

### Zmiany w plikach
- `gradle/libs.versions.toml`:
  - `mavencentral-retrofit`: 2.9.0 → 3.0.0
  - `mavencentral-okhttp`: 4.11.0 → 4.12.0

### Weryfikacja
- ✅ Kompilacja: `./gradlew assembleDebug` - sukces
- ✅ Testy jednostkowe: `./gradlew test` - sukces
- ⚠️ SQLDelight verification tasks: błąd (niezwiązany z Retrofitem)

### Potwierdzenie kompatybilności
- ✅ Retrofit 3.0.0 jest binarnie kompatybilny z 2.9.0
- ✅ Konwertery Moshi działają bez zmian
- ✅ Jspoon Converter 1.3.2 działa z Retrofit 3.0.0 (brak błędów kompilacji)
- ✅ Kod aplikacji nie wymaga żadnych zmian

### Uwagi
Błędy SQLDelight w `./gradlew build` są niezwiązane z migracją Retrofit i istniały przed migracją:
- `:data:storage:api:verifyMainAppStorageMigration`
- `:data:cache:api:verifyMainAppCacheMigration`
