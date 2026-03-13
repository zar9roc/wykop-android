# Plan migracji: Link Details → app module z API v3

## Cel

Przenieść logikę API v3 z modułu `ui/link-details` do modułu `app`, zachowując istniejący styl layoutów z `app/res/layout/`. Po migracji usunąć moduł `ui/link-details` całkowicie.

## Stan obecny

| Warstwa | `app` module (legacy) | `ui/link-details` module (nowy) |
|---------|----------------------|-------------------------------|
| Architektura | MVP (Presenter/View) — brak źródeł Presentera | MVVM (ViewModel + Flow) |
| API | v1/v2 (nieaktualne) | v3 (aktualne) |
| Layouty | Styl docelowy ✓ | Inny styl (Material scale) |
| Activity | `LinkDetailsActivityV2` (thin wrapper) | `LinkDetailsMainFragment` |
| Komentarze | `LinkCommentAdapter` + ViewHoldery | `LinkDetailsAdapter` (ListItem sealed class) |
| DI | — | `LinkDetailsComponent` + `LinkDetailsModule` |
| Nawigacja | Wszystkie nawigatory → `LinkDetailsActivityV2` | Fragment hostowany przez V2 |

## Etapy migracji

### Etap 1: Nowy Fragment w app module

**Cel**: Stworzyć `LinkDetailsFragment` w `app` module, który używa API v3 + istniejących layoutów.

**Pliki do utworzenia/modyfikacji:**
- `app/.../ui/modules/links/linkdetails/LinkDetailsFragment.kt` — nowy Fragment
  - Wzorować na `LinkDetailsMainFragment.kt` (logika Flow/ViewModel)
  - Używać layoutu `activity_link_details.xml` (CoordinatorLayout + RecyclerView + InputToolbar)
- `app/.../ui/modules/links/linkdetails/LinkDetailsPresenterV3.kt` — nowy Presenter/ViewModel
  - Przenieść logikę z `GetLinkDetailsQuery` (domain layer)
  - Korzystać z `LinksV3RetrofitApi` bezpośrednio

**Zależności do zachowania z domain:**
- `GetLinkDetailsQuery` — główna logika pobierania danych
- `LinkDetailsComponent` / `LinkDetailsModule` — DI (na razie zostawić, podpiąć pod nowy Fragment)
- Store5 cache (`LinkDetailsSourceOfTruth`, `LinkCommentsSourceOfTruth`, `RelatedLinksSourceOfTruth`)

### Etap 2: Adapter i ViewHoldery — modernizacja do API v3

**Cel**: Zaktualizować istniejące adaptery/viewholdery w `app` module do modeli v3.

**Pliki do modyfikacji:**

1. **`LinkCommentAdapter.kt`** — zmienić typ z `LinkComment` (stary model) na model v3
2. **`BaseLinkCommentViewHolder.kt`** — bindować pola z `LinkCommentV3` / `CommentResponseV3`
3. **`LinkCommentViewHolder.kt`** — zaktualizować binding do nowych nazw pól
4. **`TopLinkCommentViewHolder.kt`** — j.w.
5. **`LinkComment.kt`** (dataclass) — rozważyć usunięcie na rzecz bezpośredniego użycia modeli v3

**Layouty — zachować bez zmian:**
- `link_details_header_layout.xml` — bindować pola v3 do istniejących widgetów (`FavoriteButton`, `DigVoteButton`, `AvatarView`)
- `link_comment_layout.xml` — bindować komentarze v3
- `top_link_comment_layout.xml` — bindować top-level komentarze v3
- `link_related_layout.xml` — bindować powiązane linki v3

### Etap 3: Podpięcie nawigacji

**Cel**: `LinkDetailsActivityV2` hostuje nowy Fragment z app module zamiast fragmentu z `ui/link-details`.

**Pliki do modyfikacji:**
- `LinkDetailsActivityV2.kt` — zmienić `linkDetailsFragment()` na nowy `LinkDetailsFragment.newInstance()`
- Usunąć import `io.github.wykopmobilny.links.details.linkDetailsFragment`

**Nawigacja (bez zmian w sygnaturach):**
- `NewNavigator.kt` — `openLinkDetailsActivity()` bez zmian
- `Navigator.kt` — `openLinkDetailsActivity()` bez zmian
- `WykopLinkHandler.kt` — `getLinkIntent()` bez zmian
- `DebugStateHelper.kt` — `openLink()` bez zmian

### Etap 4: Usunięcie modułu ui/link-details ✅

**Zrealizowano:**
- Usunięto cały katalog `ui/link-details/` (android + api source files)
- Przeniesiono `GetLinkDetails.kt` i `LinkDetailsDependencies.kt` z `ui/link-details/api` do `domain/src/main/kotlin/io/github/wykopmobilny/links/details/` (ten sam pakiet — bez zmian importów)
- `domain/build.gradle` — zastąpiono `implementation(projects.ui.linkDetails.api)` na `implementation(projects.ui.base.api)` + `implementation(projects.ui.components.widgets.api)`
- `app/build.gradle` — usunięto `implementation(projects.ui.linkDetails.android)`
- `settings.gradle` — nie wymaga zmian (auto.include plugin automatycznie wykrył usunięcie)
- `WykopApp.kt` — zachowano getDependency/destroyDependency dla LinkDetails (nadal używane przez nowy LinkDetailsFragment w app)
- `detekt-baseline.xml` — usunięto stale entry dla LinkDetailsAdapter.kt

### Etap 5: Czyszczenie domain layer ✅

**Zrealizowano:**
- Usunięto interfejs `LinkDetailsDependencies` — zbędna warstwa abstrakcji po usunięciu modułu `ui/link-details`
- Przeniesiono `LinkDetailsKey` z `io.github.wykopmobilny.links.details` do `io.github.wykopmobilny.domain.linkdetails.di` (obok `LinkDetailsComponent`)
- `LinkDetailsComponent` bezpośrednio deklaruje `fun getLinkDetails(): GetLinkDetails` (bez pośrednictwa interfejsu)
- `LinkDetailsFragment` używa `LinkDetailsComponent` jako typu zależności w ViewModel
- `WykopApp` używa `LinkDetailsComponent::class` w getDependency/destroyDependency
- Zaktualizowano importy w `GetLinkDetailsQuery.kt` i `InitializeLinkDetails.kt`
- Usunięto plik `LinkDetailsDependencies.kt`

## Mapowanie layoutów: ui/link-details → app

| Layout w ui/link-details | Odpowiednik w app | Uwagi |
|--------------------------|-------------------|-------|
| `fragment_link_details.xml` | `activity_link_details.xml` | App ma InputToolbar zamiast prostego comment bar |
| `link_details_header.xml` | `link_details_header_layout.xml` | App ma custom widgety (FavoriteButton, DigVoteButton) |
| `link_details_parent_comment.xml` | `top_link_comment_layout.xml` | App używa AuthorHeaderView |
| `link_details_reply_comment.xml` | `link_comment_layout.xml` | App ma bardziej rozbudowany layout |
| `link_details_related.xml` | `link_related_layout.xml` | App ma CardView z pełnym UI |
| `link_details_*_hidden.xml` | Brak odpowiednika | Dodać logikę collapse do istniejących ViewHolderów |

## Mapowanie pól API v3 → istniejące widoki

### Header (`link_details_header_layout.xml`)
| Pole API v3 | Widget w layoucie | ID |
|-------------|-------------------|----|
| `media.photo.url` | ImageView | `linkImageView` |
| `title` | TextView | `titleTextView` |
| `description` | TextView | `descriptionTextView` |
| `author.avatar` | AvatarView | `avatarView` |
| `author.username` | TextView | `userNameTextView` |
| `created_at` | TextView | `dateTextView` |
| `source.url` | TextView | `urlTextView` |
| `tags[]` | TextView | `tagsTextView` |
| `votes.up` | DigVoteButton | `diggCountTextView` |
| `comments.count` | TextView | `commentsCountTextView` |
| `related.count` | TextView | `relatedCountTextView` |

### Komentarz (`link_comment_layout.xml` / `top_link_comment_layout.xml`)
| Pole API v3 | Widget w layoucie |
|-------------|-------------------|
| `author.avatar` | `avatarImageView` / AuthorHeaderView |
| `author.username` | `userNameTextView` / AuthorHeaderView |
| `created_at` | `dateTextView` / AuthorHeaderView |
| `content` | `entryContentTextView` |
| `media` | `entryImageView` |
| `votes.up` | `plusButton` |
| `votes.down` | `minusButton` |
| `votes.count` | `voteCountTextView` |

## Kolejność pracy

```
Etap 1 ──→ Etap 2 ──→ Etap 3 ──→ Etap 4 ──→ Etap 5
Fragment    Adaptery    Nawigacja   Usunięcie   Cleanup
                                   modułu      domain
```

Każdy etap powinien kończyć się kompilującym się kodem i działającym widokiem.

## Ryzyka

1. **Utrata funkcjonalności z ui/link-details** — nowy moduł ma collapse komentarzy, scroll-to-comment, sorting — trzeba to przenieść
2. **Testy screenshot** — `LinkDetailsMainFragmentTest.kt` zostanie usunięty; rozważyć odtworzenie testów dla nowego Fragmentu
3. **Store5 cache** — domain layer Store5 konfiguracja musi pozostać działająca po migracji
4. **Custom widgety** — `FavoriteButton`, `DigVoteButton`, `AvatarView` wymagają odpowiednich danych z API v3

## Weryfikacja

Po każdym etapie:
- [ ] `./gradlew assembleDebug` kompiluje się
- [ ] Widok szczegółów linka otwiera się poprawnie
- [ ] Header wyświetla dane (obraz, tytuł, opis, autor, tagi)
- [ ] Komentarze ładują się i wyświetlają
- [ ] Paginacja komentarzy działa
- [ ] Głosowanie (dig/bury) działa
- [ ] Powiązane linki wyświetlają się
- [ ] Deep linki do komentarzy działają (scroll-to-comment)
- [ ] Sortowanie komentarzy działa
