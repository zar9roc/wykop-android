# Plan: feedback z testów aplikacji (lipiec 2026)

**Data:** 2026-07-16
**Status:** Proposal / roadmapa
**Źródło:** lista feedbacku po testach + problem "strona 448" (wpis z ~22 tys. komentarzy)

Wszystkie ustalenia poniżej zweryfikowane w kodzie i w spec `docs/wykop_api_v3_openapi.yaml`
(odnośniki plik:linia w treści).

---

## Przegląd i priorytety

| # | Temat | Typ | Wysiłek | Etap |
|---|-------|-----|---------|------|
| 1 | Ulubione: błąd przy dodawaniu (204) | bugfix | S | 1 |
| 2 | Crash pickera rok-miesiąc | bugfix | S | 1 |
| 3 | Powiadomienie "1 nowe" pokazuje tekst zbiorczy | bugfix | XS | 1 |
| 4 | Deep link: back zamyka aplikację | UX fix | S | 1 |
| 5 | Dolne kontrolki pod belką nawigacji systemowej | bugfix | S-M | 1 |
| 6 | Badge'e embedów Twitter/X/Facebook | feature | S | 2 |
| 7 | Eksport logów z ustawień (share) | feature | S | 2 |
| 8 | Zgłaszanie treści (brak reakcji) | feature+research | M | 2 |
| 9 | Czat: unread bold, preview, "przeczytana", refresh po wysłaniu | feature | M | 3 |
| 10 | Weryfikacja powiadomień (polling po usunięciu Firebase) | weryfikacja | S | 1 |
| 11 | Wpis "strona 448": dwukierunkowa paginacja + port ekranu wpisu | projekt | L | 4 |
| 12 | YouTube: wyszarzenie opcji + plan zastępczego playera | plan only | S (szarość) | 2/5 |
| 13 | Autoplay gifów (opcja, domyślnie off) | research/plan | M-L | 5 |

Wysiłek: XS < 1h, S ~0.5 dnia, M 1-2 dni, L ~tydzień+.

---

## Etap 1 — bugfixy

### 1. Ulubione: backend odpowiada 204, aplikacja rzuca błąd

**Przyczyna (potwierdzona w kodzie):**
- `FavouritesV3RetrofitApi.addFavourite/removeFavourite` deklarują zwrotkę
  `WykopApiResponseV3<Unit>`, a `POST/DELETE /v3/favourites` to endpointy
  **nieudokumentowane** w spec — bliźniacze mutacje v3 (np. `PUT /pm/read-all`,
  `DELETE /pm/conversations/{username}`) zwracają `204 No Content`.
- Pusty body → Moshi rzuca EOF przy dekodowaniu `WykopApiResponseV3<Unit>`.
- Nawet gdyby body było: `data == null` → `ErrorHandlerTransformerV3`
  (`app/.../api/errorhandler/ErrorHandlerTransformerV3.kt:29-34`) i suspend `unwrappingV3`
  (`:112`) rzucają "API v3 response contains null data".

**Fix:** zmienić sygnatury na `suspend fun ...(): Unit` (Retrofit poprawnie obsługuje 204
dla `Unit`) albo `Response<Unit>`. Poprawić konsumentów:
- `app/.../api/entries/EntriesRepository.kt:355-360` (usunąć compose z `ErrorHandlerTransformerV3<Unit>`),
- `domain/.../repositories/LinksRepository.kt:35,37,52,54`,
- `app/.../api/links/LinksRepository.kt:416,418`.
Obsługę błędów oprzeć na `HttpException` + `ErrorBodyParserV3`.

**Weryfikacja:** dodanie/usunięcie ulubionego wpisu, linku i komentarza w aplikacji.

### 2. Crash pickera rok-miesiąc

**Przyczyna:** `MonthYearPickerDialog.setYear()` (`app/.../ui/dialogs/MonthYearPickerDialog.kt:108-149`)
ustawia **najpierw** krótszą tablicę `displayedValues`, a dopiero potem `minValue/maxValue/value`.
`NumberPicker` formatuje bieżącą (starą) wartość indeksem do nowej tablicy →
`ArrayIndexOutOfBoundsException`. Ścieżka: picker w trybie "Cały rok" (value=12)
i zmiana roku na bieżący (2-elementowa tablica, `:113-118`) lub 2005 (1-elementowa, `:130-137`).

**Fix:** w `setYear()` zawsze ustawiać `value`/`minValue`/`maxValue` **przed** podmianą
`displayedValues` na krótszą tablicę (lub tymczasowo `displayedValues = null` przed zmianą zakresu).

### 3. Powiadomienia: nieosiągalna gałąź "1 nowe"

`GetNotificationsRefreshWorkDetailsQuery.doRefresh()` (`domain/.../work/...Query.kt:54-70`):
`when (newNotifications.size)` ma gałęzie w kolejności `0`, `notifications.size`, `1` —
gdy jest dokładnie 1 powiadomienie i jest nowe, dopasowuje się `notifications.size`(=1)
zamiast `1`, więc user dostaje tekst zbiorczy bez deep-linku. Zamienić kolejność gałęzi.

### 4. Deep link: back zamyka aplikację

**Przyczyna:** `DeepLinkActivity.kt:19` startuje cel z `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`
— np. `EntryActivity` ląduje samotnie jako root taska; back = zamknięcie aplikacji.

**Fix:** w `DeepLinkActivity` budować syntetyczny back-stack przez `TaskStackBuilder`:
`MainNavigationActivity` pod spodem + cel na wierzchu. `MainNavigationActivity.getIntent(context, targetFragment)`
(`MainNavigationActivity.kt:97-106`) już przyjmuje nazwę zakładki — mapowanie:
- `/wpis/...` → `"hot"` (mikroblog),
- `/link/...` → `"promoted"` (znaleziska),
- `/tag/...`, `/ludzie/...` → domyślny ekran.

Jedno miejsce zmiany, zero logiki w aktywnościach docelowych.

**Uwaga przy okazji:** `EntryActivity` nadpisuje `onBackPressed()` (`EntryActivity.kt:230`) —
na targetSdk 36 ta metoda nie jest wywoływana (patrz pitfall w pamięci projektu);
dialog potwierdzenia wyjścia przy wpisanym komentarzu jest już prawdopodobnie martwy.
Przy tej zmianie przenieść na `onBackPressedDispatcher.addCallback`.

### 5. Dolne insety (belka nawigacji zasłania kontrolki)

**Stan:** obsłużone są tylko górne insety status bara i IME
(`app/.../utils/StatusBarInsetsUtils.kt:24-40, 50-61`); **nigdzie** nie ma
`navigationBars()`/`systemBars()` dla dołu. `InputToolbar` przypięty do dołu okna w:
`activity_entry.xml:68-76`, `activity_link_details.xml`, `activity_conversation.xml`;
FAB w `activity_navigation.xml` — wszystkie potrafią wjechać pod belkę nawigacji.

**Fix:** dodać w `StatusBarInsetsUtils.kt` helper `View.applyNavigationBarInsets()`
(bottom padding = `navigationBars()`), zastosować do kontenerów `InputToolbar` i FAB.
Uwaga na kompozycję z IME: docelowo `max(ime, navigationBars)` w jednym listenerze,
żeby klawiatura nadal działała (obecny `applyImeInsetsToContent` liczy tylko `ime()`).

**Weryfikacja:** gesty + nawigacja 3-przyciskowa (tam belka jest wysoka i problem widoczny).

### 10. Weryfikacja powiadomień

Mechanizm po usunięciu Firebase jest **samowystarczalny i powinien działać**:
periodic worker `refresh_notifications` (`WorkManagerScheduler.kt:45-70`), planowany
z `InitializeApp` (`WykopApp.kt:132`), domyślnie co 15 min, wymaga zalogowania.
Push w API v3 **nie istnieje** (brak endpointu rejestracji tokenów w spec) — polling
to jedyna opcja, opóźnienie do ~15 min jest by design.

Do zrobienia: test na urządzeniu (świeża instalacja, login, sprawdzić `adb shell dumpsys`
/ WorkManager inspector czy job jest zaplanowany i odpala się), plus fix #3 powyżej.

---

## Etap 2 — małe feature'y

### 6. Badge'e embedów Twitter/X/Facebook + porządek we wzorcu

**Stan:** jedyny punkt decyzji o badge'u to `WykopEmbedView.setEmbedIcon()`
(`app/.../ui/widgets/WykopEmbedView.kt:96-168`) — dopasowanie po etykiecie domeny z URL.
Ikony `twitter`/`facebook` **wciąż istnieją** (`:132`, `:140`), ale `x.com`, `t.co`,
`fb.watch` wypadają do `else` → brak badge'a. Typ providera z API
(`EmbedResponseV3.type`) jest **wyrzucany** w `MediaMapperV3.kt:55-70` (wszystko → `"video"`).
Nowy stack (link details) i tak renderuje przez ten sam widok
(`CommentBindingsV3.bindEmbedV3` konwertuje z powrotem na legacy `Embed`).

**Plan:**
- **Faza A (szybka):** dodać do `when` w `setEmbedIcon` etykiety `"x"` → ikona X/Twitter,
  `"fb"`/`"fbwatch"` → Facebook; w `handleUrl()` (`:199-219`) nic nie zmieniać (browser).
  Nowe ikony jako **monochromatyczne, tintowalne vector drawable** (konwencja z CLAUDE.md —
  obecne mipmapy to zaszłość; nowych nie dodawać jako bitmap).
- **Faza B (porządek):** przenieść decyzję o providerze z parsowania URL na
  `EmbedResponseV3.type` — przepuścić pole przez `MediaMapperV3` do modelu `Embed`
  i w `setEmbedIcon` najpierw patrzeć na typ z API, z fallbackiem na domenę.
  To jest właściwy "wzorzec embedów" na przyszłość (nowe serwisy = 1 case).

### 7. Eksport logów przez share sheet

**Stan:** logi pisze `FileLogAntilog` do `Android/data/<pkg>/files/crashlogs/log.txt`
(rotacja do `log.1.txt` przy 512 KB) — tylko w buildach release/releaseTest
(`app/src/release/.../LoggingInitializer.kt`). FileProvider już zadeklarowany
(`AndroidManifest.xml:233-243`), a `provider_paths.xml:12-15` już obejmuje ten katalog.
Wzorzec share istnieje w `PhotoViewActions.kt:72-81` (ACTION_SEND + FileProvider).

**Plan:** nowa pozycja `Preference` w `ui/settings/.../general_preferences.xml`
(obok `clearhistory`), bind w `GeneralPreferencesFragment` (wzorzec `bindPreference`).
Klik → `ACTION_SEND` (lub `ACTION_SEND_MULTIPLE` dla `log.txt` + `log.1.txt`) przez chooser.
Moduł settings nie widzi `FileLogAntilog` (osobny moduł Gradle) — ścieżkę/akcję
przekazać przez `SettingsDependencies` (`ui/settings/api/.../SettingsDependencies.kt`).
W debug logów nie ma — pozycja powinna pokazać sensowny komunikat zamiast pustego share'a.

### 8. Zgłaszanie treści

**Ustalenia:** w API v3 **nie ma endpointu zgłaszania** (przeszukany cały spec).
Mechanizm v2 opierał się na `violation_url` z odpowiedzi serwera, otwieranym w przeglądarce
(`NewNavigator.openReportScreen`, `NewNavigator.kt:120`). Mappery v3 hardcodują
`violationUrl = null` (m.in. `EntryMapperV3.kt:29`, `LinkMapperV3.kt:40`,
`EntriesRepository.kt` ~10 miejsc), więc przycisk "Zgłoś" na treściach v3 w ogóle się
nie pokazuje — stąd "brak reakcji".

**Plan (dwustopniowy):**
1. **Research (krótki):** sprawdzić w przeglądarce, jak dziś wygląda URL zgłaszania na
   wykop.pl (czy da się zsyntetyzować z typu+id treści, np. modal na stronie treści).
2. **Implementacja:** pokazywać "Zgłoś" zawsze dla treści v3 i otwierać stronę
   treści/zgłoszenia na wykop.pl. Preferencja: **WebView z cookies sesji** —
   login i tak działa przez WebView (cookies są w aplikacji, logout je czyści),
   więc user będzie na stronie zalogowany i może dokończyć zgłoszenie.
   Fallback: Custom Tab (bez sesji, user loguje się sam).

---

## Etap 3 — czat

**Ustalenia (API vs kod):**
- `GET /pm/conversations` zwraca per konwersacja: `user`, `last_message` (pełny `PmMessage`
  z `content`!) i `unread: boolean` — a mapper **wyrzuca oba**
  (`PMRepository.kt:40-44`; model `Conversation` ma tylko `user` + `lastUpdate`).
- `PmMessage` ma pole `read: boolean` — nadaje się na "przeczytana" przy ostatniej
  własnej wiadomości.
- Wysłanie wiadomości (`POST /pm/conversations/{username}`) zwraca **201 z pełnym
  `PmMessage`** — a `ConversationPresenter.sendMessage` (`ConversationPresenter.kt:39-43`)
  wynik **odrzuca** i robi pełny reload przez `loadConversation()`.

**Plan:**
1. **Lista konwersacji:** rozszerzyć `Conversation` o `unread: Boolean` i
   `lastMessagePreview: String?`; w `conversation_list_item.xml` dodać linię preview
   (dane są w tej samej odpowiedzi ✓) i pogrubić nick+preview gdy `unread`.
2. **Refresh po wysłaniu:** zamiast czekać na reload — **optymistycznie dokleić**
   zwrócony z 201 `PmMessage` do adaptera (reverseLayout już jest), reload zostawić
   jako uzgodnienie w tle. To usuwa odczucie "nie odświeża się"; jeśli bug polega na
   czymś innym (np. wyścig reloadu), wyjdzie przy implementacji — dodać
   `DiagnosticCheckpoint.log()` na ścieżce send→reload i sprawdzić przez debug server.
3. **"Przeczytana":** pod ostatnią własną wiadomością z `read == true` mały tekst/ikonka.
4. **(Opcjonalnie)** polling `GET /pm/conversations/{username}/newer` co N sekund,
   gdy ekran rozmowy otwarty.

Czat jest w starym stacku MVP — zmiany robić w miejscu, bez portowania (mały zakres).

---

## Etap 4 — wpis "strona 448": dwukierunkowa paginacja

**Problem:** deep link `wykop.pl/wpis/…/strona/448#<commentId>` — dziś trzeba by
doładować 448 stron, żeby doscrollować. Bez sensu.

**Fakty z API:** `GET /entries/{id}/comments` przyjmuje tylko `page` + `limit`
(max 50, default 25); **brak parametru sortowania**; paginacja zwraca **tylko**
`per_page` i `total` (brak `last_page`). Czyli: ostatnia strona =
`ceil(total / per_page)` liczona po stronie klienta; numer strony celu mamy
**wprost z URL-a powiadomienia** (`/strona/NNN`).

**Projekt — paginacja zakotwiczona (anchor-based, bidirectional):**
1. Parsować `strona/NNN` w `WykopLinkHandler` i przekazywać `targetPage` do ekranu wpisu
   (dziś przekazywany jest tylko `commentId`).
2. Załadować **wyłącznie stronę N**, wyrenderować, doscrollować do `commentId`.
3. Scroll w dół → append strony N+1 (spinner na dole — już jest).
4. Scroll w górę → **prepend** strony N−1 ze spinnerem u góry; przy insercie
   kotwiczyć viewport (`notifyItemRangeInserted(0, n)` z LinearLayoutManager
   utrzymuje pozycję).
5. Nagłówek wpisu renderowany zawsze jako pierwszy element; między nagłówkiem
   a pierwszą załadowaną stroną element "· · · starsze komentarze · · ·"
   (tap → doładuj wstecz).
6. Bonus prawie za darmo: przycisk "przejdź do najnowszych" = skok do
   `ceil(total/per_page)`.

**Wehikuł implementacji — rekomendacja: port ekranu wpisu do nowego stacku.**
Repo ma już dwa pokolenia UI; link details + `LinkCommentsPager` to gotowy wzorzec
(domain + Store5 + coroutines). Budowanie dwukierunkowego pagera w umierającym
`EntryDetailPresenter` (RxJava MVP) to inwestycja w kod do skasowania. Zakres portu:
tylko ekran wpisu (EntryActivity), nie cała apka. Alternatywa (szybsza, gorsza):
dorobić prepend w obecnym prezenterze — możliwe, ale bez store/cache i do wyrzucenia
przy porcie.

**Ryzyka:** komentarze dochodzą na żywo (total się zmienia między requestami) —
strony mogą się przesuwać; przy prepend/append deduplikować po id. Highlight
komentarza z powiadomienia działa od razu, bo celujemy w jego stronę.

**Co do pełnego portu aplikacji :D** — nie big-bang. Kontynuować migrację
ekran-po-ekranie (wzorzec już jest); ekran wpisu jest następny w kolejce, bo ma
konkretny powód produktowy. Compose to osobna decyzja — obecny nowy stack
(views + coroutines) działa i nie blokuje żadnej z powyższych rzeczy.

---

## Etap 5 — research / plan-only (bez implementacji teraz)

### 12. YouTube

**Wyszarzenie (do zrobienia teraz, S):** `enableYoutubePlayer` w
`GetAppearancePreferencesQuery.mediaPlayerFlow()` (`:110-114`) dostaje
`isEnabled = false` (wzorzec jak `useAmoledTheme` przy jasnym motywie); dodatkowo
wymusić `false` przy odczycie (`SettingsPreferencesApi.enableYoutubePlayer` czyta też
stary stack), żeby zachowanie odpowiadało UI — YT zawsze otwiera się zewnętrznie.
Powód: `YTPlayer.kt` używa martwego `com.google.android.youtube.player.YouTubePlayerView`
(deprecated API Google, wymaga klucza, zwykle pada na `onInitializationFailure`).

**Plan zastępczy (bez implementacji):** "ytdlp" wprost się nie da (Python); realne opcje:
- **Opcja A — `android-youtube-player` (IFrame/WebView):** biblioteka
  PierfrancescoSoffritti, bez klucza API, gra oficjalnym playerem w WebView.
  Najmniej pracy, najmniejsze ryzyko blokad; jakość "webowa".
- **Opcja B — NewPipeExtractor + media3 ExoPlayer:** ekstrakcja URL streamu
  (odpowiednik yt-dlp na JVM, aktywnie utrzymywany) i odtwarzanie natywnie
  w istniejącym `EmbedViewActivity` (ExoPlayer już tam jest). Natywne UX,
  ale krucho przy zmianach po stronie YT + wątpliwości ToS.
- **Opcja C — youtubedl-android (yt-dlp przez Pythona w APK):** odpada — ~50+ MB
  i utrzymanie interpretera.

Rekomendacja: A jako domyślny in-app player, B jako eksperyment za flagą później.

### 13. Autoplay gifów (opcja, domyślnie OFF)

**Stan:** brak jakiejkolwiek maszynerii inline playback (ExoPlayer tylko w pełnoekranowym
`EmbedViewActivity`; gify na listach to statyczne thumbnaile Glide, pełny ekran =
`GifDrawable`). Czyli feature = nowa infrastruktura.

**Opcje:**
- **A — Glide `asGif()` inline:** najprostsza; ryzyko pamięci (patrz incydent 117MB
  w pamięci projektu) — obowiązkowo warianty `,w400` z CDN, playback tylko dla
  elementów w pełni widocznych (scroll listener start/stop), limit równoległych animacji (1-2).
- **B — mp4 zamiast gif + pula ExoPlayerów:** wymaga researchu, czy CDN wykopu
  serwuje rendition mp4 dla gifów (częsta praktyka); wydajniejsze niż GifDrawable,
  ale duży nakład (pool playerów, lifecycle w RecyclerView).
- **C — powiązać z portem UI (etap 4+):** nowe itemy komentarzy dostają
  wsparcie inline playback od razu w nowym stacku.

Rekomendacja: research B (1-2h, sprawdzić URL-e embedów gif w odpowiedziach API),
potem decyzja A vs B; implementacja dopiero po etapie 4. Nowy klucz ustawień
`settings.media.autoplay_gifs` (wzorzec dodawania w `UserSettings.kt` + XML + fragment).

---

## Kolejność sugerowana

1. **Etap 1** w jednej serii commitów (5 bugfixów + weryfikacja powiadomień na urządzeniu).
2. **Etap 2**: badge'e (faza A), eksport logów, research zgłaszania → implementacja.
3. **Etap 3**: czat (mapper → lista → optymistyczny send → read).
4. **Etap 4**: port ekranu wpisu + paginacja zakotwiczona (największy kawałek).
5. **Etap 5**: wyszarzenie YT od razu przy etapie 2 (to 1 linia + odczyt), reszta jako
   osobne decyzje po researchu.
