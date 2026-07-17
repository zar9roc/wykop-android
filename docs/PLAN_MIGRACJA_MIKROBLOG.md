# Plan: migracja mikrobloga na nowy stack (domain + coroutines)

**Data:** 2026-07-17
**Status:** Proposal — do zatwierdzenia zakresu i decyzji strategicznych.

Mikroblog (wpisy) to ostatni duży obszar na starym MVP+RxJava. Ekran **szczegółów
wpisu jest już przeniesiony** (`entry/v2/` + `domain/entrydetails/`). Zostaje reszta:
feedy, ekrany wejścia (input) i warstwa danych.

---

## 1. Co dokładnie zostało (inwentarz)

### Feedy / listy (9 powierzchni) — stary `EndlessProgressAdapter` + prezenter + Rx
| Feed | Fragment / prezenter | Źródło danych |
|---|---|---|
| Gorące / mikroblog | `HotFragment` + `EntriesFragment` | `EntriesApi.getHot/getStream/getActive` |
| Tag → wpisy | `TagEntriesFragment` | `TagApi.getTagEntries` |
| Ulubione → wpisy | `EntryFavoriteFragment` | `EntriesApi.getObserved` |
| Profil → Wpisy | `MicroblogEntriesFragment` | `ProfileApi.getEntries` |
| Profil → Skomentowane | `MicroblogCommentsFragment` | `ProfileApi.getEntriesComments` |
| Szukaj → wpisy | `EntrySearchFragment` | `SearchApi.searchEntries` |
| Mój Wykop (×3 zakładki) | `MyWykopEntryLinkFragment` | `MyWykopApi.getIndex/byTags/byUsers` (mieszane wpis+link) |
| Profil → Aktywność | `ActionsFragment` | `ProfileApi.getActions` (mieszane wpis+link) |
| Generyczna lista | `EntriesFragment` | (host dla powyższych) |

### Ekrany input (3) — `BaseInputActivity` + `InputPresenter`
- Dodaj wpis (`AddEntryActivity`), Edytuj wpis (`EditEntryActivity`), Edytuj komentarz (`EditEntryCommentActivity`).

### Wspólne renderowanie
- `EntryViewHolder` (~350 linii: nagłówek, treść, embed, ankieta, głos, ulubione, obcinanie, spoilery, menu), `EntryCommentViewHolder`, rodzina adapterów (`EntriesAdapter`, `EntryCommentAdapter`, `EntryLinksAdapter`), interfejsy `EntryActionListener`/`EntryCommentActionListener`.

### Warstwa danych (RxJava)
- `EntriesApi` + `EntriesRepository` (wewnątrz już coroutine przez `rxSingle`), interaktory `EntriesInteractor`/`EntryCommentInteractor`, **mutowalne** modele `Entry`/`EntryComment`.

### ⚠️ Sprzężenie z już-zrobionym detalem V2
Ekran szczegółów V2 **wciąż** używa `EntriesApi`, obu interaktorów, `EntryViewHolder`
i uruchamia stare aktywności edycji. Więc starej warstwy **nie da się skasować**,
dopóki nie przejdą mutacje + inputy + viewholdery.

---

## 2. Dwa niezależne podproblemy (różne wzorce)

**(A) Feedy = listy.** Na nowym stacku **nie ma jeszcze ani jednego feedu górnego
poziomu**. Są dwa wzorce do wyboru (patrz decyzja D2).

**(B) Mutacje + input** (głos/ulubione/usuń/ankieta/dodaj/edytuj). Wzorzec gotowy:
`ApiClient.mutation{}` z `domain/entrydetails` — jednorazowe wywołania v3, immutable
stan re-emitowany zamiast mutacji `Entry` w miejscu.

---

## 3. Decyzje strategiczne (do podjęcia)

### D1 — Renderowanie itemów: reużyć legacy viewholdery czy pisać bezstanowe bindingi V3?
- **Reużycie (jak w detalu V2):** mapujemy odpowiedź v3 → legacy `Entry`/`EntryComment`
  i karmimy istniejące `EntryViewHolder`/`EntryCommentViewHolder`. Szybko, mało ryzyka,
  spójne z precedensem. Koszt: zostają mutowalne modele + legacy viewholdery (unifikacja
  z linkami dalej zablokowana).
- **Bezstanowe bindingi V3 (jak link details):** immutable modele domenowe + `bind…V3`.
  Czyściej, odblokowuje unifikację z linkami, ale to przepisanie ~350 linii bogatego
  renderowania (ankieta/embed/spoiler/obcinanie/NSFW/menu) — duży nakład i ryzyko.
- **Rekomendacja:** **reużycie** w pierwszym podejściu. Celem „podniesienia stacka" jest
  zabicie prezenterów/interaktorów/Rx warstwy danych, nie repaint. Bindingi V3 zrobić
  później jako osobny refactor, gdy feedy już siedzą na nowej warstwie.

### D2 — Silnik paginacji + trwałość feedu
- **B: ręczny pager do przodu + `MutableStateFlow` (in-memory), jak entrydetails/search.**
  ~60 linii, zero zmian w cache, ephemeryczny feed. Rekomendowany/wdrożony kierunek.
- **A: Jetpack Paging (Store5 + `Pager` + SoT + SQLDelight), jak profil/promoted.** Offline,
  ale wymaga: nowej tabeli indeksu strony feedu w `entries.sq`, mapowania kursora
  String→Int, a jedyny renderer Pattern A na nowym stacku to **stub**.
- **Rekomendacja:** **B (in-memory, ręczny pager).** Feedy nie potrzebują offline,
  a to omija komplet blokerów cache (indeks strony, kursor String vs Int) i jest spójne
  z tym, co już działa. Jetpack Paging zostawić profilowi.

### D3 — Zakres i kolejność
- Migracja jest **per-feed** (różne API: Entries/Tag/Search/Profile/MyWykop), nie jedna.
- **Rekomendacja:** najpierw pionowy plaster na jednym feedzie (Gorące), potem decyzja
  „iść dalej / stop". NIE deklarujemy z góry wszystkich 9 feedów.

---

## 4. Fazy (przyrostowo, każda osobno testowalna i commitowalna)

**Faza 1 — pionowy plaster: feed Gorące + mutacje wpisu.**
- `domain/microblog/feed`: `GetMicroblogFeedQuery : Query<MicroblogFeedUi>`, ręczny
  `EntryFeedPager` (forward, `loadMore`, refresh, dedup po id), `FeedStateStorage`
  (in-memory), subkomponent + scope + klucz (klucz = typ feedu + parametry sortowania).
- `domain/microblog/EntryActions`: mutacje przez `ApiClient.mutation{}` (głos/ulubione/
  usuń/ankieta) — immutable re-emit.
- `HotFragmentV2` wg wzorca `LinkDetailsFragment` (repeatOnLifecycle+stateIn,
  collectSwipeRefresh/collectErrorDialog, scroll listener→loadMore), adapter reużywający
  `EntryViewHolder` (mapowanie v3→`Entry` na granicy widoku, jak V2 detal).
- Przełączyć nawigację Gorących na V2. Zweryfikować na urządzeniu.

**Faza 2 — pozostałe feedy jednorodne** (tag, szukaj, ulubione, profil-wpisy,
profil-skomentowane): każdy to cienki wariant Fazy 1 podmieniający wywołanie fetch.

**Faza 3 — feedy mieszane wpis+link** (Mój Wykop, Aktywność profilu): trudniejsze
(dwa typy itemów) — osobna decyzja, czy warto (może zostać na starym stacku najdłużej).

**Faza 4 — ekrany input** (dodaj/edytuj wpis, edytuj komentarz) → mutacje domenowe,
wygaszenie `InputPresenter`. Odblokuje niezależność detalu V2 od starych aktywności.

**Faza 5 — kasacja starej warstwy**: `EntriesApi`, `EntriesRepository`, interaktory,
`EndlessProgressAdapter` i rodzina, stare `Base*Fragment`/prezenter — gdy nikt już
nie referuje. Ostatni krok, czysty zysk (setki linii).

---

## 5. Ryzyka
- **Najgęściej używana powierzchnia** — regresje bolą. Stąd plaster pionowy + weryfikacja
  na urządzeniu po każdej fazie, revert = przełączenie nawigacji (stary ekran zostaje 1 wydanie).
- **Sprzężenie detal↔input↔viewholder** — starej warstwy nie ruszamy do Fazy 4-5.
- **Feedy mieszane** (Faza 3) mogą nie być warte zachodu — dopuszczalne zostawić na starym.
- **Reużycie legacy viewholderów** utrwala mutowalne modele — świadomy dług, spłacany
  osobnym refactorem renderowania później.

---

## 6. Rekomendowany pierwszy krok
Zrobić **tylko Fazę 1** (feed Gorące + mutacje) jako spike, ocenić realny nakład/ryzyko
na żywym ekranie, i dopiero wtedy decydować o Fazach 2-5.
