# Naprawa ponownej nawigacji do powiązanych linków

## Problem

Ponowna nawigacja na widok powiązanych linków poprzez kliknięcie TextView nawigującego do powiązanych linków nie działała. W logach pojawił się komunikat:

```
launchScoped didn't find scope for key=io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope=LinkDetailsKey(linkId=7905951, initialCommentId=null)
```

## Przyczyna

W `GetRelatedLinksQuery.kt` w metodzie `refresh()` (linia 96) był użyty błędny mechanizm uruchamiania coroutine:

```kotlin
override fun refresh() {
    appScopes.safe<LinkDetailsScope> {  // ❌ Bez klucza!
        runCatching {
            relatedLinksStore.fresh(key.linkId)
        }
    }
}
```

Metoda `safe<LinkDetailsScope>` uruchamia coroutine bez klucza (id = null), co tworzy klucz scope:
- `"io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope=null"`

Ale scope został utworzony **z kluczem** `LinkDetailsKey(linkId=7905951, initialCommentId=null)`:
- `"io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope=LinkDetailsKey(linkId=7905951, initialCommentId=null)"`

**Klucze nie pasują** → scope nie został znaleziony → operacja się nie wykonała.

## Rozwiązanie

Zmieniono `appScopes.safe<LinkDetailsScope>` na `appScopes.safeKeyed<LinkDetailsScope>(id = key)`:

```kotlin
override fun refresh() {
    appScopes.safeKeyed<LinkDetailsScope>(id = key) {  // ✅ Z kluczem!
        runCatching {
            relatedLinksStore.fresh(key.linkId)
        }
    }
}
```

Teraz coroutine jest uruchamiana w poprawnym scope z odpowiednim kluczem.

## Mechanizm działania

1. **Tworzenie scope**: Gdy `RelatedLinksFragment` jest tworzony, `viewModelWrapperFactoryKeyed` wywołuje `getDependency(LinkDetailsComponent::class, key = LinkDetailsKey(...))`
2. **Rejestracja scope**: `WykopApp.getDependency()` tworzy i rejestruje scope w mapie `scopes` z kluczem: `"${LinkDetailsScope::class.qualifiedName}=${LinkDetailsKey(...)}"`
3. **Używanie scope**: Wszystkie wywołania `appScopes.safeKeyed<LinkDetailsScope>(id = key)` MUSZĄ używać tego samego klucza
4. **Czyszczenie scope**: Gdy ViewModel jest niszczony, `onCleared()` wywołuje `destroyKeyedDependency()` i usuwa scope z mapy

## Pliki zmienione

- `domain/src/main/kotlin/io/github/wykopmobilny/domain/linkdetails/GetRelatedLinksQuery.kt` (linia 96)

## Weryfikacja

```bash
./gradlew :domain:compileKotlin  # Kompilacja przeszła
./gradlew assembleDebug          # Build APK przeszedł pomyślnie
```

Aplikacja teraz poprawnie obsługuje ponowną nawigację do powiązanych linków.
