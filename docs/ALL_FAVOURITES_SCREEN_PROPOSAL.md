# Propozycja: Ekran Wszystkich Ulubionych (Mixed Favourites)

**Data:** 2026-03-08
**Status:** Proposal
**Priorytet:** Enhancement

## Motywacja

Endpoint `/v3/favourites` zwraca zarówno linki jak i wpisy w jednej odpowiedzi. Obecnie aplikacja ma dwa osobne ekrany które filtrują wyniki:
- **Ulubione Linki** - filtruje tylko `ObservedItemV3.LinkItem`
- **Ulubione Wpisy** - filtruje tylko `ObservedItemV3.EntryItem`

Dodanie trzeciej zakładki **"Wszystkie"** pokazującej mieszaną listę może poprawić UX przez:
- Szybszy przegląd wszystkich ulubionych treści w jednym miejscu
- Chronologiczne wyświetlanie według kolejności dodania do ulubionych
- Jeden ekran zamiast przełączania między zakładkami

## Obecna Architektura

### Struktura UI

```
FavoriteFragment (główny fragment)
├─ ViewPager + TabLayout
└─ FavoritePagerAdapter
   ├─ Position 0: LinksFavoriteFragment (pokazuje tylko linki)
   └─ Position 1: EntryFavoriteFragment (pokazuje tylko wpisy)
```

### Flow Danych

```kotlin
// LinksFavoriteFragment
LinksRepository.getObserved()
  → favouritesApiV3.getFavourites()
  → filterIsInstance<ObservedItemV3.LinkItem>()
  → map { it.link }
  → List<LinkResponseV3>

// EntryFavoriteFragment
EntriesRepository.getObserved()
  → favouritesApiV3.getFavourites()
  → filterIsInstance<ObservedItemV3.EntryItem>()
  → map { it.entry }
  → List<EntryResponseV3>
```

### Model Danych

```kotlin
sealed class ObservedItemV3 {
    @JsonClass(generateAdapter = true)
    data class EntryItem(
        val entry: EntryResponseV3,
    ) : ObservedItemV3()

    @JsonClass(generateAdapter = true)
    data class LinkItem(
        val link: LinkResponseV3,
    ) : ObservedItemV3()
}
```

Adapter `ObservedItemV3Adapter` już obsługuje parsowanie mieszanych typów z API.

## Propozycja Rozwiązania

### 1. Rozszerzenie UI

#### Zaktualizować `FavoritePagerAdapter`

**Przed:**
```kotlin
override fun getCount() = 2
```

**Po:**
```kotlin
override fun getCount() = 3

override fun getItem(position: Int): Fragment =
    when (position) {
        0 -> AllFavouritesFragment.newInstance()  // NOWY: wszystkie
        1 -> LinksFavoriteFragment.newInstance()   // stary index 0
        2 -> EntryFavoriteFragment.newInstance()   // stary index 1
        else -> throw IllegalArgumentException("Invalid position: $position")
    }

override fun getPageTitle(position: Int) =
    when (position) {
        0 -> resources.getString(R.string.all)     // "Wszystkie"
        1 -> resources.getString(R.string.links)   // "Linki"
        2 -> resources.getString(R.string.entries) // "Wpisy"
        else -> throw IllegalArgumentException("Invalid position: $position")
    }
```

### 2. Nowe Komponenty

#### AllFavouritesFragment

```kotlin
class AllFavouritesFragment :
    BaseFragment(R.layout.fragment_all_favourites),
    AllFavouritesView {

    companion object {
        fun newInstance() = AllFavouritesFragment()
    }

    @Inject
    lateinit var presenter: AllFavouritesPresenter

    @Inject
    lateinit var mixedAdapter: MixedFavouritesAdapter

    override var loadDataListener: (Boolean) -> Unit = { presenter.loadData(it) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        presenter.subscribe(this)
        mixedAdapter.loadNewDataListener = { loadDataListener(false) }
        presenter.loadData(true)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            adapter = mixedAdapter
            layoutManager = LinearLayoutManager(context)
            // ... EndlessScrollListener etc
        }
    }

    override fun onDestroyView() {
        presenter.unsubscribe()
        super.onDestroyView()
    }
}
```

#### AllFavouritesPresenter

```kotlin
class AllFavouritesPresenter @Inject constructor(
    private val schedulers: SchedulersProvider,
    private val favouritesRepository: FavouritesRepository, // NOWE repository
) : BasePresenter<AllFavouritesView>() {

    private val data = ArrayList<ObservedItemV3>()
    private var page: String? = null
    private var pageNumber: Int = 0

    fun loadData(shouldRefresh: Boolean) {
        if (shouldRefresh) {
            page = null
            pageNumber = 0
            data.clear()
        }

        disposables.add(
            favouritesRepository.getAllFavourites(page)
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { response ->
                        data.addAll(response.data.orEmpty())
                        page = response.pagination?.next ?: (++pageNumber).toString()
                        view?.showFavourites(data)
                    },
                    { view?.showError(it) }
                )
        )
    }

    fun loadMore() = loadData(false)
}
```

#### MixedFavouritesAdapter

```kotlin
class MixedFavouritesAdapter @Inject constructor(
    private val userManagerApi: UserManagerApi,
    private val settingsPreferencesApi: SettingsPreferencesApi,
    private val navigator: NewNavigator,
    private val linkHandler: WykopLinkHandler,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = ArrayList<ObservedItemV3>()
    var loadNewDataListener: (() -> Unit)? = null

    lateinit var entryActionListener: EntryActionListener
    lateinit var linkActionListener: LinkActionListener

    companion object {
        private const val VIEW_TYPE_ENTRY = 0
        private const val VIEW_TYPE_LINK = 1
        private const val VIEW_TYPE_PROGRESS = 2
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= items.size) return VIEW_TYPE_PROGRESS
        return when (items[position]) {
            is ObservedItemV3.EntryItem -> VIEW_TYPE_ENTRY
            is ObservedItemV3.LinkItem -> VIEW_TYPE_LINK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ENTRY -> EntryViewHolder.inflateView(parent, userManagerApi)
            VIEW_TYPE_LINK -> LinkViewHolder.inflateView(parent, userManagerApi)
            VIEW_TYPE_PROGRESS -> ProgressViewHolder.inflateView(parent)
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EntryViewHolder -> {
                val item = items[position] as ObservedItemV3.EntryItem
                holder.bindView(
                    entry = item.entry,
                    entryActionListener = entryActionListener,
                    // ... pozostałe parametry
                )
            }
            is LinkViewHolder -> {
                val item = items[position] as ObservedItemV3.LinkItem
                holder.bindView(
                    link = item.link,
                    linkActionListener = linkActionListener,
                    // ... pozostałe parametry
                )
            }
            is ProgressViewHolder -> {
                loadNewDataListener?.invoke()
            }
        }
    }

    override fun getItemCount() = items.size + 1 // +1 dla progress indicator

    fun updateData(newItems: List<ObservedItemV3>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
```

### 3. Nowe Repository

#### FavouritesRepository (domain)

```kotlin
interface FavouritesRepository {
    /**
     * Pobiera wszystkie ulubione (linki + wpisy) bez filtrowania
     */
    fun getAllFavourites(page: String?): Single<PaginatedResponse<List<ObservedItemV3>>>
}
```

#### FavouritesRepositoryImpl

```kotlin
class FavouritesRepositoryImpl @Inject constructor(
    private val favouritesApiV3: FavouritesV3RetrofitApi,
    private val userTokenRefresher: UserTokenRefresher,
) : FavouritesRepository {

    override fun getAllFavourites(page: String?) =
        rxSingle { favouritesApiV3.getFavourites(page) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                PaginatedResponse(
                    data = response.data.orEmpty(),
                    pagination = response.pagination,
                )
            }
}
```

**UWAGA:** To nowe repository nie filtruje wyników - zwraca `List<ObservedItemV3>` bezpośrednio.

### 4. Dagger Moduły

#### AllFavouritesFragmentModule

```kotlin
@Module
abstract class AllFavouritesFragmentModule {
    @ContributesAndroidInjector
    abstract fun bindAllFavouritesFragment(): AllFavouritesFragment
}
```

#### Bindings w AppModule

```kotlin
@Binds
@Singleton
abstract fun bindFavouritesRepository(
    repository: FavouritesRepositoryImpl
): FavouritesRepository
```

### 5. Layout Resources

#### `res/layout/fragment_all_favourites.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/swiperefresh"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

#### `res/values/strings.xml`

```xml
<string name="all">Wszystkie</string>
```

## Rozważania Techniczne

### 1. ViewHolder Reuse

Używamy istniejących ViewHolderów:
- `EntryViewHolder` - już istnieje w `ui/adapters/viewholders/`
- `LinkViewHolder` - już istnieje w `ui/adapters/viewholders/`
- `ProgressViewHolder` - już istnieje dla infinite scroll

### 2. Paginacja

Standardowa paginacja API v3:
- Pierwsza strona: `page = null`
- Kolejne strony: `page = pagination.next` (hash string)
- Fallback: `page = (++pageNumber).toString()` gdy `next == null`

### 3. Content Filtering

Mieszana lista NIE potrzebuje filtrów `filterLinksV3` / `filterEntriesV3` które stosują:
- Blacklist użytkowników/tagów
- Adult content
- NSFW

**PROPOZYCJA:** Dodać osobny helper `filterMixedFavouritesV3()` który:
- Iteruje po `List<ObservedItemV3>`
- Filtruje LinkItem przez istniejący `shouldFilterLink()`
- Filtruje EntryItem przez istniejący `shouldFilterEntry()`
- Zwraca przefiltrowaną listę

### 4. Action Listeners

Adapter potrzebuje obu listenerów:
- `entryActionListener: EntryActionListener` - dla akcji na wpisach
- `linkActionListener: LinkActionListener` - dla akcji na linkach

Fragment musi implementować oba interfejsy lub delegować do presentera.

### 5. SwipeRefresh

Fragment implementuje `SwipeRefreshLayout.OnRefreshListener` dla spójności z innymi zakładkami.

### 6. Empty State

Jeśli lista jest pusta, pokazać komunikat:
```xml
<TextView
    android:id="@+id/emptyStateText"
    android:text="@string/no_favourites"
    android:visibility="gone" />
```

String resource:
```xml
<string name="no_favourites">Nie masz jeszcze żadnych ulubionych treści</string>
```

## Impact Analysis

### Pozytywne Aspekty

1. **Lepszy UX** - użytkownicy widzą wszystkie ulubione w jednym miejscu
2. **Chronologiczne sortowanie** - treści wyświetlane w kolejności dodania
3. **Wykorzystanie istniejącego API** - endpoint już zwraca mieszane typy
4. **Reuse komponentów** - używamy istniejących ViewHolderów
5. **Zgodność z architekturą** - wzorzec jak inne ekrany z paginacją

### Wyzwania

1. **Złożoność Adaptera** - musi obsługiwać oba typy ViewHolderów
2. **Duplicated Listeners** - fragment potrzebuje obu action listenerów
3. **Content Filtering** - nowy helper dla mieszanych typów
4. **Testing** - trzeba testować oba typy w jednej liście
5. **Performance** - RecyclerView z różnymi ViewHolderami (minimal impact)

## Alternatywne Podejścia

### Opcja A: Jedna Lista z Sekcjami (Headers)

Zamiast mieszanej listy, grupować po typie z headerami:

```
┌─────────────────┐
│ LINKI           │ ← Section Header
├─────────────────┤
│ Link 1          │
│ Link 2          │
├─────────────────┤
│ WPISY           │ ← Section Header
├─────────────────┤
│ Entry 1         │
│ Entry 2         │
└─────────────────┘
```

**Wady:**
- Nie chronologiczne
- Wymaga grupowania w prezenterze
- Dodatkowy typ ViewHoldera (SectionHeader)

### Opcja B: Zachować Status Quo

Pozostawić tylko dwie zakładki (Linki, Wpisy).

**Wady:**
- Traci chronologiczną kolejność dodania
- Użytkownik musi przełączać zakładki

### Opcja C: Dropdown Filtr Zamiast Zakładek

Jeden ekran z dropdown filtrem w Toolbar:
- "Wszystkie"
- "Tylko Linki"
- "Tylko Wpisy"

**Wady:**
- Zmiana paradygmu UI (reszta aplikacji używa TabLayout)
- Mniej intuicyjne niż zakładki

## Rekomendacje

1. **Implementować zakładkę "Wszystkie" jako pierwszą** (position 0)
   - Najbardziej użyteczna dla większości użytkowników
   - Zakładki "Linki" i "Wpisy" jako opcje filtrowania

2. **Utworzyć oddzielne `FavouritesRepository`**
   - Nie modyfikować istniejących `LinksRepository` i `EntriesRepository`
   - Czysta separacja odpowiedzialności

3. **Reużyć istniejące ViewHoldery**
   - Nie duplikować kodu renderowania
   - Testowane komponenty

4. **Dodać Content Filtering**
   - Helper `filterMixedFavouritesV3()` dla spójności z resztą aplikacji
   - Respektowanie blacklist/NSFW settings

5. **Testować na rzeczywistych danych**
   - Mieszane listy z różnymi proporcjami linków/wpisów
   - Edge cases: tylko linki, tylko wpisy, pusta lista

## Plan Implementacji

### Faza 1: Infrastruktura (1-2h)
1. Utworzyć `FavouritesRepository` interface i impl
2. Dodać bindings w Dagger
3. Dodać string resources

### Faza 2: UI Komponenty (2-3h)
1. Utworzyć `AllFavouritesFragment`
2. Utworzyć `AllFavouritesPresenter`
3. Utworzyć `MixedFavouritesAdapter`
4. Dodać layout `fragment_all_favourites.xml`

### Faza 3: Integracja (1h)
1. Zaktualizować `FavoritePagerAdapter`
2. Dodać Dagger module
3. Podłączyć action listeners

### Faza 4: Content Filtering (1h)
1. Utworzyć helper `filterMixedFavouritesV3()`
2. Zintegrować w prezenterze

### Faza 5: Testing & Polish (1-2h)
1. Testować na rzeczywistych danych
2. Empty state
3. SwipeRefresh
4. Error handling

**Szacowany czas:** 6-9 godzin

## Uwagi Końcowe

Propozycja jest zgodna z obecną architekturą aplikacji i wykorzystuje istniejące komponenty. Endpoint `/v3/favourites` naturalnie zwraca mieszane typy, więc dodanie ekranu "Wszystkie" jest logicznym rozszerzeniem funkcjonalności.

Warto rozważyć implementację jako eksperyment/feature flag - jeśli użytkownicy nie będą korzystać z zakładki "Wszystkie", można ją łatwo usunąć zachowując istniejące zakładki filtrowane.

## Powiązane Dokumenty

- `docs/FAVOURITES_ENDPOINT_FIX.md` - Fix routingu ulubionych linków
- `docs/ENTRY_FAVOURITES_ENDPOINT_FIX.md` - Fix routingu ulubionych wpisów
- `docs/wykop_api_v3_openapi.yaml` - Specyfikacja endpointu `/v3/favourites`
- `app/src/main/kotlin/io/github/wykopmobilny/api/responses/v3/observed/ObservedItemV3.kt` - Model danych
