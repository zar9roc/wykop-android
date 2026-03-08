# Plan: Dodanie pobierania treści z /v3/observed/all do widoku "Mój Wykop"

## Context

Widok "Mój Wykop" (tab 0 - "Wszyscy") używa starego, deprecated API v2 (`MyWykopRetrofitApi.getIndex()`) które już nie odpowiada.
Celem jest migracja tego taba na endpoint v3: `GET /v3/observed/all` z paginacją hash-based (`page=null` dla pierwszej strony, hash string z `pagination.next` dla kolejnych).

Endpoint `/v3/observed/all` zwraca mieszaną listę entries i links (`anyOf: Link | Entry`). Wymaga to polimorficznego parsowania JSON — każdy element ma pole `resource` wskazujące typ ("entry" lub "link").

**Scope**: Tylko tab 0 (Index/Wszystko) migrowany do v3. Taby 1-2 (Tags, Users) pozostają na v2.

---

## Implementacja

### Krok 1: Model odpowiedzi — `ObservedItemV3` (sealed class)

**Nowy plik**: `data/wykop/api/src/main/kotlin/io/github/wykopmobilny/api/responses/v3/observed/ObservedItemV3.kt`

```kotlin
sealed class ObservedItemV3 {
    data class EntryItem(val entry: EntryResponseV3) : ObservedItemV3()
    data class LinkItem(val link: LinkResponseV3) : ObservedItemV3()
}
```

Sealed class pozwala na type-safe rozróżnianie entries od links po deserializacji.

---

### Krok 2: Moshi adapter — `ObservedItemV3Adapter`

**Nowy plik**: `data/wykop/api/src/main/kotlin/io/github/wykopmobilny/api/responses/v3/adapters/ObservedItemV3Adapter.kt`

Custom `JsonAdapter<ObservedItemV3>` który:
1. Czyta JSON element jako `Map<String, Any?>` via `reader.readJsonValue()`
2. Sprawdza wartość pola `resource` (discriminator)
3. Deleguje do `EntryResponseV3JsonAdapter` (gdy resource == "entry") lub `LinkResponseV3JsonAdapter` (gdy resource == "link")
4. Zwraca `null` dla nieznanych resource types (graceful handling)

Rejestracja jako `JsonAdapter.Factory` w companion object, wzorzec:
```kotlin
val FACTORY = JsonAdapter.Factory { type, _, moshi ->
    if (Types.getRawType(type) == ObservedItemV3::class.java) {
        ObservedItemV3Adapter(moshi)
    } else null
}
```

---

### Krok 3: Rejestracja adaptera w Moshi

**Modyfikacja**: `data/wykop/remote/src/main/kotlin/io/github/wykopmobilny/wykop/remote/RetrofitModule.kt`

W metodzie `moshi()` (linia 26-31) dodać:
```kotlin
add(ObservedItemV3Adapter.FACTORY)  // przed .build()
```

---

### Krok 4: Retrofit API — `ObservedV3RetrofitApi`

**Nowy plik**: `data/wykop/api/src/main/kotlin/io/github/wykopmobilny/api/endpoints/v3/ObservedV3RetrofitApi.kt`

```kotlin
interface ObservedV3RetrofitApi {
    @GET("v3/observed/all")
    suspend fun getObservedAll(
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<ObservedItemV3>>
}
```

Parametr `page: String?` — null = pierwsza strona (brak parametru w URL), hash string = kolejne strony.

---

### Krok 5: Rejestracja w Dagger (2 pliki)

**Modyfikacja**: `data/wykop/remote/.../WykopModule.kt` — dodać:
```kotlin
@Reusable @Provides
fun observedV3RetrofitApi(retrofit: Retrofit) = retrofit.create<ObservedV3RetrofitApi>()
```

**Modyfikacja**: `data/wykop/api/.../WykopApi.kt` — dodać:
```kotlin
fun observedV3RetrofitApi(): ObservedV3RetrofitApi
```

---

### Krok 6: Mapper — `ObservedItemV3` → `EntryLink`

**Nowy plik** (lub dodanie do istniejącego mappera): `app/src/main/kotlin/io/github/wykopmobilny/models/mapper/apiv3/ObservedMapperV3.kt`

```kotlin
fun ObservedItemV3.toEntryLink(owmContentFilter: OWMContentFilter): EntryLink = when (this) {
    is ObservedItemV3.EntryItem -> EntryLink(
        link = null,
        entry = entry.filterEntryV3(owmContentFilter),
    )
    is ObservedItemV3.LinkItem -> EntryLink(
        link = link.filterLinkV3(owmContentFilter),
        entry = null,
    )
}
```

Reużywa istniejące `filterEntryV3()` z `EntryMapperV3.kt` i `filterLinkV3()` z `LinkMapperV3.kt`.

---

### Krok 7: Aktualizacja `MyWykopApi` interface

**Modyfikacja**: `app/.../api/mywykop/MyWykopApi.kt`

Zmiana sygnatury `getIndex` na v3 pagination:
```kotlin
fun getIndex(page: String?): Single<FilteredData<EntryLink>>
```

`byTags(page: Int)` i `byUsers(page: Int)` pozostają bez zmian (v2).

---

### Krok 8: Aktualizacja `MyWykopRepository`

**Modyfikacja**: `app/.../api/mywykop/MyWykopRepository.kt`

1. Dodać `ObservedV3RetrofitApi` do konstruktora
2. Przepisać `getIndex()` na v3:

```kotlin
override fun getIndex(page: String?): Single<FilteredData<EntryLink>> =
    rxSingle { observedApiV3.getObservedAll(page) }
        .map { response ->
            FilteredData(
                totalCount = response.data?.size ?: 0,
                filtered = response.data.orEmpty().map { it.toEntryLink(owmContentFilter) },
                nextPage = response.pagination?.next,
            )
        }
```

Usunąć niepotrzebne: `userTokenRefresher`, `patronsApi`, `ErrorHandlerTransformer` — v3 API używa JWT (JwtInterceptor), nie wymaga tych warstw.

---

### Krok 9: Aktualizacja `MyWykopEntryLinkPresenter`

**Modyfikacja**: `app/.../ui/modules/mywykop/index/MyWykopEntryLinkPresenter.kt`

Zmiana paginacji w `loadIndex()`:
- Dodać `var indexPage: String? = null` i `var indexPageNumber: Int = 1` (wzorzec z HotPresenter)
- Zachować `var page = 1` dla loadTags/loadUsers (v2)
- loadIndex pattern:
```kotlin
fun loadIndex(shouldRefresh: Boolean) {
    if (shouldRefresh) { indexPage = null; indexPageNumber = 1 }
    myWykopApi.getIndex(indexPage)
        .subscribeOn(schedulers.backgroundThread())
        .observeOn(schedulers.mainThread())
        .subscribe(
            { data ->
                if (data.totalCount > 0) {
                    indexPage = data.nextPage ?: (++indexPageNumber).toString()
                    view?.addItems(data.filtered, shouldRefresh)
                } else {
                    view?.disableLoading()
                }
            },
            { view?.showErrorDialog(it) },
        ).intoComposite(compositeObservable)
}
```

`addItems` oczekuje `List<EntryLink>` — `data.filtered` to już `List<EntryLink>`.

---

## Pliki do zmiany (podsumowanie)

| Plik | Typ |
|------|-----|
| `data/wykop/api/.../responses/v3/observed/ObservedItemV3.kt` | NOWY |
| `data/wykop/api/.../responses/v3/adapters/ObservedItemV3Adapter.kt` | NOWY |
| `data/wykop/api/.../endpoints/v3/ObservedV3RetrofitApi.kt` | NOWY |
| `app/.../models/mapper/apiv3/ObservedMapperV3.kt` | NOWY |
| `data/wykop/remote/.../RetrofitModule.kt` | MODYFIKACJA |
| `data/wykop/remote/.../WykopModule.kt` | MODYFIKACJA |
| `data/wykop/api/.../WykopApi.kt` | MODYFIKACJA |
| `app/.../api/mywykop/MyWykopApi.kt` | MODYFIKACJA |
| `app/.../api/mywykop/MyWykopRepository.kt` | MODYFIKACJA |
| `app/.../ui/modules/mywykop/index/MyWykopEntryLinkPresenter.kt` | MODYFIKACJA |

---

## Weryfikacja

1. **Kompilacja**: `./gradlew assembleDebug` — projekt musi się skompilować
2. **Detekt**: `./gradlew detekt` — brak nowych naruszeń
3. **ktlint**: `./gradlew formatKotlinMain` — formatowanie
4. **Manual test**: Zalogować się → przejść do "Mój Wykop" → tab "Wszyscy" → powinien załadować listę entries i links → scroll w dół → powinna załadować kolejna strona
