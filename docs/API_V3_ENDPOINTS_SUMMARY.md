# Wykop API v3 - Podsumowanie Dostępnych Endpointów

**Data utworzenia:** 2026-02-28
**Źródło:** `docs/wykop_api_v3_openapi.yaml`
**Wersja API:** 3.0.0-beta
**Specyfikacja:** OpenAPI 3.0.3

## Statystyki

- **Łączna liczba endpointów:** 145
- **Liczba schematów danych:** 55
- **Liczba parametrów:** 18
- **Metoda autoryzacji:** JWT Bearer Token
- **URL bazowy:** https://wykop.pl/api/v3/

## Kategorie Endpointów

| Kategoria | Liczba EP | Opis |
|-----------|-----------|------|
| Powiadomienia | 27 | Zarządzanie powiadomieniami (wpisy, PM, tagi, dyskusje) |
| Profile | 21 | Profile użytkowników, działania, odznaki, obserwowani |
| Znaleziska | 17 | Linki, głosowanie, AMA, przekierowania |
| Ustawienia | 16 | Ustawienia profilu, czarne listy, preferencje |
| Tagi | 16 | Zarządzanie tagami, stream, przypinanie |
| Obserwowane | 11 | Obserwowani użytkownicy, tagi, dyskusje |
| Mikroblog | 11 | Wpisy mikrobloga, głosowanie, obserwowanie |
| Komentarze znalezisk | 10 | CRUD komentarzy do znalezisk, głosowanie |
| Mikroblog - Komentarz | 9 | Komentarze do wpisów mikrobloga |
| PM | 7 | Wiadomości prywatne, konwersacje |
| Linki powiazane | 7 | Powiązane linki do znalezisk |
| Wyszukiwanie | 5 | Wyszukiwanie zawartości (all, stream, links, entries, users) |
| Zapisane wyszukiwania | 5 | Zarządzanie zapisanymi wyszukiwaniami |
| Rekomendacje | 5 | System rekomendacji użytkownika |
| Draft znalezisk | 5 | Szkice znalezisk |
| Artykuły | 5 | Artykuły, historie wersji |
| Notatki | 4 | Notatki użytkowników |
| Media | 4 | Upload zdjęć, embedy |
| Bezpieczeństwo | 4 | Auth, refresh token, connect, logout |
| Odznaki | 3 | Lista odznak, szczegóły, użytkownicy z odznaką |
| Mikroblog - Ankieta | 2 | Ankiety w mikroblogu |
| Hity | 2 | Hity (links, entries) |
| Użytkownicy | 1 | Autocomplete użytkowników |
| Ulubione | 1 | Zarządzanie ulubionymi |
| Ranking | 1 | Ranking użytkowników |
| Przypięte tagi | 1 | Lista przypiętych tagów |
| Kategorie | 1 | Lista kategorii |

## Kluczowe Endpointy (wg Funkcjonalności)

### 1. Autoryzacja i Bezpieczeństwo
- `POST /auth` - Autoryzacja aplikacji (JWT)
- `POST /refresh-token` - Odświeżanie tokena
- `GET /connect` - WykopConnect (OAuth)
- `GET /logout` - Wylogowanie

### 2. Profile Użytkowników
- `GET /profile` - Profil zalogowanego użytkownika (pełny)
- `GET /profile/short` - Profil zalogowanego (skrócony)
- `GET /profile/users/{username}` - Profil konkretnego użytkownika
- `GET /profile/users/{username}/short` - Profil skrócony
- `GET /profile/users/{username}/actions` - Działania użytkownika
- `GET /profile/users/{username}/entries/added` - Dodane wpisy
- `GET /profile/users/{username}/entries/voted` - Głosowane wpisy
- `GET /profile/users/{username}/entries/commented` - Komentowane wpisy
- `GET /profile/users/{username}/links/*` - Linki (added, published, up, down, commented, related)
- `GET /profile/users/{username}/badges` - Odznaki użytkownika
- `GET /profile/users/{username}/tags` - Tagi użytkownika
- `GET /profile/users/{username}/observed/*` - Obserwowani (tags, users/following, users/followers)

### 3. Mikroblog (Entries)
- `GET /entries` - Lista wpisów
- `POST /entries` - Utworzenie wpisu
- `GET /entries/{entryId}` - Szczegóły wpisu
- `PUT /entries/{entryId}` - Edycja wpisu
- `DELETE /entries/{entryId}` - Usunięcie wpisu
- `POST /entries/{entryId}/votes` - Głosowanie na wpis
- `DELETE /entries/{entryId}/votes` - Usunięcie głosu
- `GET /entries/{entryId}/newer` - Sprawdzenie nowszych wpisów
- `POST /entries/{entryId}/observed-discussions` - Obserwowanie dyskusji
- `DELETE /entries/{entryId}/observed-discussions` - Usunięcie obserwowania

### 4. Komentarze do Wpisów
- `GET /entries/{entryId}/comments` - Lista komentarzy
- `POST /entries/{entryId}/comments` - Dodanie komentarza
- `GET /entries/{entryId}/comments/newer` - Nowsze komentarze
- `GET /entries/{entryId}/comments/{commentId}` - Szczegóły komentarza
- `PUT /entries/{entryId}/comments/{commentId}` - Edycja komentarza
- `DELETE /entries/{entryId}/comments/{commentId}` - Usunięcie komentarza
- `POST /entries/{entryId}/comments/{commentId}/votes` - Głosowanie
- `DELETE /entries/{entryId}/comments/{commentId}/votes` - Usunięcie głosu

### 5. Znaleziska (Links)
- `GET /links` - Lista znalezisk
- `POST /links` - Dodanie znaleziska
- `GET /links/{linkId}` - Szczegóły znaleziska
- `PUT /links/{linkId}` - Edycja znaleziska
- `DELETE /links/{linkId}` - Usunięcie znaleziska
- `POST /links/{linkId}/votes/up` - Głos w górę
- `POST /links/{linkId}/votes/down/{reason}` - Głos w dół
- `DELETE /links/{linkId}/votes` - Usunięcie głosu
- `GET /links/{linkId}/votes` - Lista głosujących
- `GET /links/{linkId}/upvotes/{type}` - Szczegóły głosów w górę
- `GET /links/stats/upcoming` - Statystyki nadchodzących
- `GET /links/url` - Pobierz link po URL
- `GET /links/{linkId}/redirect` - Przekierowanie do linku
- `POST /links/{linkId}/observed-discussions` - Obserwowanie dyskusji

### 6. AMA (Ask Me Anything)
- `POST /links/{linkId}/ama/start` - Start AMA
- `POST /links/{linkId}/ama/finish` - Zakończenie AMA
- `POST /links/{linkId}/ama/mute` - Wyciszenie AMA
- `DELETE /links/{linkId}/ama/unmute` - Odciszenie AMA

### 7. Komentarze do Znalezisk
- `GET /links/{linkId}/comments` - Lista komentarzy
- `POST /links/{linkId}/comments` - Dodanie komentarza
- `GET /links/{linkId}/comments/{commentId}` - Szczegóły komentarza
- `PUT /links/{linkId}/comments/{commentId}` - Edycja komentarza
- `DELETE /links/{linkId}/comments/{commentId}` - Usunięcie komentarza
- `POST /links/{linkId}/comments/{commentId}/comments` - Odpowiedź na komentarz
- `POST /links/{linkId}/comments/{commentId}/votes` - Głosowanie
- `DELETE /links/{linkId}/comments/{commentId}/votes` - Usunięcie głosu
- `GET /links/{linkId}/comments/{commentId}/votes/{type}` - Szczegóły głosów
- `POST /links/{linkId}/comments/{commentId}/observed-discussions` - Obserwowanie

### 8. Linki Powiązane (Related)
- `GET /links/{linkId}/related` - Lista linków powiązanych
- `POST /links/{linkId}/related` - Dodanie linku powiązanego
- `GET /links/{linkId}/related/{relatedId}` - Szczegóły linku powiązanego
- `PUT /links/{linkId}/related/{relatedId}` - Edycja
- `DELETE /links/{linkId}/related/{relatedId}` - Usunięcie
- `POST /links/{linkId}/related/{relatedId}/votes` - Głosowanie
- `GET /links/{linkId}/related/{relatedId}/votes/{type}` - Szczegóły głosów

### 9. Tagi
- `GET /tags/autocomplete` - Podpowiadanie tagów
- `GET /tags/popular` - Popularne tagi
- `GET /tags/popular-user-tags` - Popularne tagi autorskie
- `GET /tags/{tagName}/related` - Powiązane tagi
- `GET /tags/{tagName}` - Szczegóły tagu
- `PUT /tags/{tagName}` - Edycja tagu (tło + opis)
- `GET /tags/{tagName}/stream` - Stream wpisy+linki z tagu
- `GET /tags/{tagName}/newer` - Sprawdzenie nowszych
- `GET /tags/{tagName}/users` - Autorzy tagu autorskiego
- `POST /tags/{tagName}/users/{username}` - Dodanie współautora
- `DELETE /tags/{tagName}/users/{username}` - Usunięcie współautora
- `GET /tags/{tagName}/pinned` - Przypięta zawartość
- `POST /tags/{tagName}/pin/{resource}/{id}` - Przypięcie
- `DELETE /tags/{tagName}/unpin/{resource}/{id}` - Odpięcie
- `GET /tags/{tagName}/notifications` - Stan powiadomień
- `PUT /tags/{tagName}/notifications` - Edycja powiadomień

### 10. Powiadomienia
- `GET /notifications/status` - Status nowych powiadomień
- **Entries:**
  - `GET /notifications/entries` - Lista powiadomień
  - `PUT /notifications/entries/all` - Oznacz wszystkie jako przeczytane
  - `DELETE /notifications/entries/all` - Usuń wszystkie
  - `GET /notifications/entries/{id}` - Pojedyncze powiadomienie
  - `PUT /notifications/entries/{id}` - Oznacz jako przeczytane
  - `DELETE /notifications/entries/{id}` - Usuń
- **PM:**
  - `GET /notifications/pm` - Lista powiadomień PM
  - `PUT /notifications/pm/all` - Oznacz wszystkie
  - `DELETE /notifications/pm/all` - Usuń wszystkie
  - `GET /notifications/pm/{id}` - Pojedyncze
  - `PUT /notifications/pm/{id}` - Oznacz
  - `DELETE /notifications/pm/{id}` - Usuń
- **Tagi:**
  - `GET /notifications/tags` - Lista powiadomień tagów
  - `PUT /notifications/tags/all` - Oznacz wszystkie
  - `DELETE /notifications/tags/all` - Usuń wszystkie
  - `GET /notifications/tags/{id}` - Pojedyncze
  - `PUT /notifications/tags/{id}` - Oznacz
  - `DELETE /notifications/tags/{id}` - Usuń
- **Obserwowane dyskusje:**
  - `GET /notifications/observed-discussions` - Lista
  - `PUT /notifications/observed-discussions/all` - Oznacz wszystkie
  - `DELETE /notifications/observed-discussions/all` - Usuń wszystkie
  - `GET /notifications/observed-discussions/{id}` - Pojedyncze
  - `PUT /notifications/observed-discussions/{id}` - Oznacz
  - `DELETE /notifications/observed-discussions/{id}` - Usuń
- **Grupy:**
  - `GET /notifications/groups/{group_id}` - Powiadomienia grupy
  - `PUT /notifications/groups/{group_id}` - Oznacz wszystkie w grupie
  - `DELETE /notifications/groups/{group_id}` - Usuń wszystkie w grupie

### 11. Wiadomości Prywatne (PM)
- `POST /pm/open` - Otwarcie konwersacji
- `PUT /pm/read-all` - Oznacz wszystkie jako przeczytane
- `GET /pm/conversations` - Lista konwersacji
- `GET /pm/conversations/{username}` - Konwersacja z użytkownikiem
- `POST /pm/conversations/{username}` - Wysłanie wiadomości
- `DELETE /pm/conversations/{username}` - Usunięcie konwersacji
- `GET /pm/conversations/{username}/newer` - Nowsze wiadomości

### 12. Ustawienia
- **Czarne listy:**
  - `GET /settings/blacklists/stats` - Statystyki czarnych list
  - `GET /settings/blacklists/domains` - Lista zablokowanych domen
  - `POST /settings/blacklists/domains` - Dodanie domeny
  - `DELETE /settings/blacklists/domains/{domain}` - Usunięcie domeny
  - `GET /settings/blacklists/tags` - Lista zablokowanych tagów
  - `POST /settings/blacklists/tags` - Dodanie tagu
  - `DELETE /settings/blacklists/tags/{tag}` - Usunięcie tagu
  - `GET /settings/blacklists/users` - Lista zablokowanych użytkowników
  - `POST /settings/blacklists/users` - Dodanie użytkownika
  - `DELETE /settings/blacklists/users/{username}` - Usunięcie użytkownika
- **Profil:**
  - `GET /settings/profile` - Ustawienia profilu
  - `PUT /settings/profile` - Aktualizacja profilu
  - `POST /settings/profile/avatar` - Upload avatara
  - `POST /settings/profile/background` - Upload tła profilu
- **Ogólne:**
  - `GET /settings/general` - Ustawienia ogólne
  - `PUT /settings/general` - Aktualizacja ustawień

### 13. Media
- `POST /media/photos/upload` - Upload zdjęcia (multipart)
- `POST /media/photos` - Upload zdjęcia (base64)
- `DELETE /media/photos/{key}` - Usunięcie zdjęcia
- `POST /media/embed` - Generowanie embed (YouTube, etc.)

### 14. Wyszukiwanie
- `GET /search/all` - Wyszukiwanie wszystkiego
- `GET /search/stream` - Stream wyników
- `GET /search/links` - Wyszukiwanie znalezisk
- `GET /search/entries` - Wyszukiwanie wpisów
- `GET /search/users` - Wyszukiwanie użytkowników
- **Zapisane wyszukiwania:**
  - `GET /saved-search` - Lista zapisanych
  - `POST /saved-search` - Utworzenie zapisanego
  - `GET /saved-search/{id}` - Szczegóły
  - `PUT /saved-search/{id}` - Edycja
  - `DELETE /saved-search/{id}` - Usunięcie

### 15. Obserwowane
- `GET /observed/users` - Lista obserwowanych użytkowników
- `GET /observed/users/newer` - Sprawdzenie nowych
- `POST /observed/users/{username}` - Obserwowanie użytkownika
- `DELETE /observed/users/{username}` - Usuń obserwowanie
- `POST /observed/tags/{tagName}` - Obserwowanie tagu
- `DELETE /observed/tags/{tagName}` - Usuń obserwowanie
- `GET /observed/tags/stream` - Stream obserwowanych tagów
- `GET /observed/discussions` - Obserwowane dyskusje
- `GET /observed/all` - Wszystko obserwowane
- `GET /observed/tags/{tagName}/notifications` - Powiadomienia tagu
- `GET /observed/tags/{tagName}/pinned` - Przypięte w tagu

### 16. Rekomendacje
- `GET /recommendation/user/stream` - Stream rekomendacji
- `POST /recommendation/user/not-interesting/link/{link_id}` - Oznacz link jako nieinteresujący
- `POST /recommendation/user/interesting/link/{link_id}` - Oznacz link jako interesujący
- `POST /recommendation/user/not-interesting/entry/{entry_id}` - Oznacz wpis jako nieinteresujący
- `POST /recommendation/user/interesting/entry/{entry_id}` - Oznacz wpis jako interesujący

### 17. Ankiety (Survey)
- `POST /entries/survey` - Utworzenie wpisu z ankietą
- `POST /entries/{id}/survey/votes` - Głosowanie w ankiecie
- `DELETE /entries/{id}/survey/votes` - Usunięcie głosu

### 18. Artykuły
- `GET /articles` - Lista artykułów
- `GET /articles/{id}` - Szczegóły artykułu
- `POST /articles` - Utworzenie artykułu
- `PUT /articles/{id}` - Edycja artykułu
- `GET /articles/{articleId}/histories/{articleContentId}` - Historia wersji

### 19. Draft Znalezisk
- `GET /links/draft` - Lista szkiców
- `POST /links/draft` - Utworzenie szkicu
- `GET /links/draft/{key}` - Szczegóły szkicu
- `PUT /links/draft/{key}` - Edycja szkicu
- `DELETE /links/draft/{key}` - Usunięcie szkicu

### 20. Pozostałe
- `GET /users/autocomplete` - Podpowiadanie użytkowników
- `GET /pinned-tags` - Lista przypiętych tagów
- `GET /favourites` - Ulubione
- `GET /notes` - Lista notatek
- `GET /notes/{username}` - Notatki o użytkowniku
- `PUT /notes/{username}` - Edycja notatki
- `DELETE /notes/{username}` - Usunięcie notatki
- `GET /hits/links` - Hity znalezisk
- `GET /hits/entires` - Hity wpisów
- `GET /categories` - Lista kategorii
- `GET /badges` - Lista odznak
- `GET /badges/{slug}` - Szczegóły odznaki
- `GET /badges/{slug}/users` - Użytkownicy z odznaką
- `GET /rank` - Ranking użytkowników

## Główne Schematy Danych

### Modele Użytkowników
- `UserShort` - Skrócony profil użytkownika
- `UserFull` - Pełny profil publiczny
- `UserFullPrivate` - Pełny profil (dane prywatne)

### Modele Treści
- `Entry` - Wpis mikrobloga
- `EntryComment` - Komentarz do wpisu
- `Link` - Znalezisko
- `LinkComment` - Komentarz do znaleziska
- `LinkRelated` - Link powiązany
- `LinkWithComments` - Link z komentarzami
- `LinkWithRelated` - Link z powiązanymi
- `Article` - Artykuł

### Modele Tagów
- `ShortTag` - Skrócone info o tagu
- `Tag` - Pełne dane tagu

### Modele Mediów
- `Photo` - Zdjęcie
- `Embed` - Embed (video, etc.)
- `Survey` - Ankieta

### Modele Powiadomień
- `NotificationEntry` - Powiadomienie o wpisie
- `NotificationPm` - Powiadomienie o PM
- `NotificationTag` - Powiadomienie o tagu
- `NotificationObservedDiscussion` - Powiadomienie o dyskusji

### Modele PM
- `PmConversation` - Konwersacja
- `PmMessage` - Wiadomość
- `PmCreateMessage` - Payload do utworzenia wiadomości

### Modele CRUD
- `CreateUpdateEntry` - Payload do utworzenia/edycji wpisu
- `CreateUpdateComment` - Payload do utworzenia/edycji komentarza
- `CreateUpdateSurvey` - Payload do utworzenia/edycji ankiety
- `CreateUpdateArticle` - Payload do utworzenia/edycji artykułu

### Pozostałe
- `Badge` - Odznaka
- `Color` - Kolor (użytkownika, tagu)
- `Pagination` - Paginacja standardowa
- `UserPagination` - Paginacja użytkowników
- `UserYearSummary` - Podsumowanie roku użytkownika
- `AdRss` - Reklama RSS
- `BasicError` - Błąd podstawowy
- `LimitExceeded` - Błąd limitu

## Parametry Endpointów

### Wspólne Parametry Query
- `page` (Page) - Numer strony
- `limit` (Limit) - Limit wyników na stronę
- `sort` (Sort) - Sortowanie
- `type` (Type) - Typ contentu
- `category` (Category) - Kategoria
- `bucket` (Bucket) - Bucket czasowy
- `multimedia` (Multimedia) - Filtr multimediów
- `year` (DateYear) - Rok
- `month` (DateMonth) - Miesiąc

### Parametry Specyficzne
- `tagName` (TagName) - Nazwa tagu
- `username` (Username) - Nazwa użytkownika
- `entryId` (EntryId) - ID wpisu
- `id` (Id) - Uniwersalne ID
- `tags_sort` (TagsSort) - Sortowanie tagów
- `tags_type` (TagsType) - Typ tagu
- `entries_sort` (EntriesSort) - Sortowanie wpisów
- `entries_filter` (EntriesFilter) - Filtr wpisów
- `show_grouped` (ShowGrouped) - Pokazywanie grupowane

## Notatki Implementacyjne

### Autoryzacja
- Większość endpointów wymaga JWT Bearer Token
- Wyjątki (security: []): `/auth`, `/refresh-token`, `/connect`
- Header: `Authorization: Bearer {token}`

### Paginacja
- Standardowa paginacja: `?page={nr}&limit={ile}`
- Niektóre endpointy używają `UserPagination` (next_page cursor)

### Głosowanie (Votes)
- POST dla dodania głosu
- DELETE dla usunięcia głosu
- GET dla pobrania listy głosujących
- GET z parametrem `{type}` dla szczegółów typu głosu

### Obserwowanie (Observed)
- POST dla rozpoczęcia obserwowania
- DELETE dla zakończenia obserwowania
- GET dla listy obserwowanych

### Powiadomienia (Notifications)
- Cztery typy: entries, pm, tags, observed-discussions
- Wspólny wzorzec: GET lista, PUT oznacz, DELETE usuń
- Endpoint `/all` dla operacji masowych
- Endpoint `/{id}` dla pojedynczych operacji

### CRUD Pattern
- GET - Odczyt
- POST - Utworzenie
- PUT - Edycja
- DELETE - Usunięcie
- GET z parametrem `/newer` - Sprawdzenie nowszych

## Migracja z API v2 na v3

### Zasady
1. **ZAWSZE sprawdzać** ten plik przed migracją endpointu
2. Plik `wykop_api_v3_openapi.yaml` to **jedyne źródło prawdy**
3. Wszystkie 145 endpointów są udokumentowane
4. Jeśli endpoint nie istnieje w tym pliku - **NIE ISTNIEJE w API v3**

### Główne Różnice
- v2 używało różnych schematów autentykacji, v3 tylko JWT
- v2 miało różne struktury response, v3 ma zunifikowane `{data: {...}}`
- v3 dodaje wiele nowych endpointów (rekomendacje, draft, zapisane wyszukiwania)
- v3 ma bardziej szczegółowe endpointy powiadomień
- v3 dodaje AMA functionality dla linków

### Rekomendowana Kolejność Migracji
1. **Auth & Profile** - podstawa (4 + 21 = 25 EP)
2. **Entries & Comments** - mikroblog (11 + 9 = 20 EP)
3. **Links & Comments** - znaleziska (17 + 10 = 27 EP)
4. **Tags** - system tagów (16 EP)
5. **Notifications** - powiadomienia (27 EP)
6. **Settings** - ustawienia (16 EP)
7. **PM** - wiadomości prywatne (7 EP)
8. **Search & Recommendations** - wyszukiwanie (5 + 5 = 10 EP)
9. **Media & Others** - pozostałe (23 EP)

---

**Ostatnia aktualizacja:** 2026-02-28
**Task:** #74 - Sprawdzić dostępność endpointów w docs/wykop_api_v3_openapi.yaml
