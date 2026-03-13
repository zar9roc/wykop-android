# Testy integracyjne Scope Lifecycle

## Cel

Testy integracyjne weryfikujące poprawne tworzenie, używanie i niszczenie scope w aplikacji. Testy te mają na celu wykrycie problemów podobnych do tego z Task #265, gdzie użycie `safe<T>` zamiast `safeKeyed<T>(id)` powodowało, że operacje nie były wykonywane w odpowiednim scope.

## Problem który wykrywają

W Task #265 wystąpił problem:
- Scope został utworzony z kluczem `LinkDetailsKey(linkId=7905951, initialCommentId=null)`
- Metoda `refresh()` używała `safe<LinkDetailsScope>` (bez klucza, id=null)
- Klucze nie pasowały: `"...LinkDetailsScope=LinkDetailsKey(...)"` vs `"...LinkDetailsScope=null"`
- Operacja nie została wykonana, tylko pojawił się warning w logach

## Mechanizm Scope

1. **Klucz scope**: `"${ScopeClass::class.qualifiedName}=$id"`
2. **Tworzenie scope**: `getDependency(clazz, scopeId)` tworzy scope i rejestruje w mapie `scopes`
3. **Używanie scope**: `launchScoped(clazz, id, block)` szuka scope po kluczu i wykonuje block
4. **Niszczenie scope**: `destroyDependency(clazz, scopeId)` usuwa scope z mapy i canceluje coroutine scope

## Scenariusze testowe

### 1. testScopeCreationWithKey
Sprawdza czy scope jest poprawnie utworzony z kluczem i dostępny pod tym kluczem.

**Kroki:**
1. Utwórz scope z kluczem `LinkDetailsKey(linkId=123, initialCommentId=null)`
2. Zweryfikuj że scope istnieje w mapie scopes
3. Zweryfikuj że klucz scope zawiera id

**Oczekiwany rezultat:** Scope jest utworzony i dostępny.

### 2. testScopeUsageWithMatchingKey
Sprawdza czy operacje wykonywane w scope z odpowiednim kluczem działają poprawnie.

**Kroki:**
1. Utwórz scope z kluczem
2. Wykonaj operację używając `safeKeyed<T>(id)` z tym samym kluczem
3. Zweryfikuj że operacja została wykonana

**Oczekiwany rezultat:** Operacja wykonana pomyślnie.

### 3. testScopeUsageWithMismatchedKey ⚠️ (wykrywa bug z Task #265)
Sprawdza że użycie `safe<T>` (bez klucza) gdy scope został utworzony z kluczem NIE działa.

**Kroki:**
1. Utwórz scope z kluczem `LinkDetailsKey(linkId=123, initialCommentId=null)`
2. Spróbuj wykonać operację używając `safe<T>` (bez klucza, id=null)
3. Zweryfikuj że operacja NIE została wykonana
4. Zweryfikuj że w logach pojawił się warning "launchScoped didn't find scope"

**Oczekiwany rezultat:**
- Operacja NIE została wykonana
- Warning w logach
- **To dokładnie symuluje bug z Task #265**

### 4. testScopeDestruction
Sprawdza czy scope jest prawidłowo niszczony.

**Kroki:**
1. Utwórz scope z kluczem
2. Zniszcz scope wywołując `destroyDependency`
3. Spróbuj użyć scope (powinno nie działać)
4. Zweryfikuj że scope został usunięty z mapy

**Oczekiwany rezultat:** Scope usunięty, kolejne operacje nie działają.

### 5. testMultipleNavigationsScenario
Symuluje rzeczywisty scenariusz nawigacji który spowodował bug.

**Kroki:**
1. Nawigacja 1: Utwórz scope z kluczem, wykonaj operację, zniszcz scope
2. Nawigacja 2: Utwórz scope z NOWYM kluczem (inny linkId)
3. Wykonaj operację w nowym scope
4. Zweryfikuj że operacja działa w NOWYM scope, nie w starym

**Oczekiwany rezultat:** Każda nawigacja działa ze swoim niezależnym scope.

### 6. testScopeCoroutineCancellation
Sprawdza czy coroutine scope jest anulowany przy niszczeniu.

**Kroki:**
1. Utwórz scope
2. Uruchom długą operację w scope
3. Zniszcz scope
4. Zweryfikuj że operacja została anulowana (CancellationException)

**Oczekiwany rezultat:** Operacje są anulowane przy niszczeniu scope.

## Implementacja

Testy znajdują się w pliku:
```
app/src/androidTest/java/io/github/wykopmobilny/tests/ScopeLifecycleTest.kt
```

Testy używają:
- `TestApp.instance` - dostęp do aplikacji i mechanizmu scope
- `LinkDetailsScope` + `LinkDetailsKey` - rzeczywisty scope używany w aplikacji
- `CountDownLatch` - synchronizacja asynchronicznych operacji
- Reflection (jeśli potrzebny dostęp do prywatnej mapy `scopes`)

## Jak uruchomić

```bash
# Uruchom wszystkie testy scope lifecycle
./gradlew :app:connectedDebugAndroidTest --tests "*.ScopeLifecycleTest"

# Uruchom konkretny test
./gradlew :app:connectedDebugAndroidTest --tests "*.ScopeLifecycleTest.testScopeUsageWithMismatchedKey"
```

## Wartość testów

1. **Wykrywanie błędów**: Automatycznie wykryją problem z użyciem `safe` zamiast `safeKeyed`
2. **Regression testing**: Zapobiegną ponownemu wprowadzeniu podobnego buga
3. **Dokumentacja**: Pokazują jak poprawnie używać mechanizmu scope
4. **Confidence**: Dają pewność że zmiany w mechanizmie scope nie psują istniejącej funkcjonalności
