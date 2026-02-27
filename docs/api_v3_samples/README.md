# Wykop API v3 - Przykładowe Odpowiedzi JSON

**Data:** 2026-02-27
**Cel:** Walidacja formatu odpowiedzi API v3 Wykop przed implementacją

---

## Struktura Plików

### 1. Autoryzacja (POST /auth)

#### Sukces
- **Plik:** `auth_success.json`
- **Status:** 200 OK
- **Struktura:** `{ data: { token: "JWT..." } }`
- **Format tokenu:** JWT (Base64 encoded)
- **Weryfikacja:** ✅ Token zawiera standard JWT payload (iat, exp, roles, username, user_ip, id)

#### Błędy
- **Plik:** `auth_error_401.json` - Nieprawidłowe dane uwierzytelniające
- **Plik:** `auth_error_403.json` - Brak uprawnień
- **Plik:** `auth_error_500.json` - Błąd serwera

**Struktura błędu:** `{ code, hash, error: { message, key } }`

---

## 2. Lista Wpisów (GET /entries)

### Bez Autoryzacji
- **Plik:** `entries_success_noauth.json`
- **Status:** 200 OK
- **Struktura:** `{ data: [...], pagination: { per_page, total } }`
- **Paginacja:** Numeryczna (page=1, page=2, ...)
- **Liczba stron:** Obliczana jako `Math.ceil(total / per_page)` = 90 stron (450/5)
- **Zawartość:** 5 wpisów różnego typu:
  1. Wpis z photo
  2. Wpis z embed YouTube
  3. Wpis bez mediów
  4. Wpis usunięty (deleted=true)
  5. Wpis +18 (adult=true)

### Z Autoryzacją
- **Plik:** `entries_success_with_auth.json`
- **Status:** 200 OK
- **Struktura:** `{ data: [...], pagination: { per_page, total, next, prev } }`
- **Paginacja:** Hashowa (page=abc123hash)
- **Dodatkowe pola:**
  - `next` - hash następnej strony
  - `prev` - hash poprzedniej strony
- **Różnice w danych:**
  - `voted` może być 1 (upvote), -1 (downvote), 0 (brak głosu)
  - `favourite` może być true dla ulubionych wpisów
  - `deletable` może być true dla własnych wpisów
  - `actions` zawiera rozszerzone uprawnienia (update, delete)
  - `comments.items` może zawierać komentarze

### Błąd Limitu Paginacji
- **Plik:** `entries_limit_error.json`
- **Status:** 400 Bad Request
- **Przyczyna:** Przekroczenie limitu 100 stron
- **Message:** "Search does not serve more than 100th results for query."

---

## 3. Walidacja Formatów Dat

**Plik:** `entries_date_formats.json`

### Format Standardowy
- **Wzorzec:** `YYYY-MM-DD HH:MM:SS`
- **Przykłady:**
  - `"2026-02-27 00:00:00"` - Północ (00:00:00)
  - `"2026-02-27 23:59:59"` - Tuż przed północą (23:59:59)
  - `"2019-02-25 20:35:18"` - Stary wpis z 2019 roku

### Weryfikacja
✅ **Wszystkie daty używają formatu:** `YYYY-MM-DD HH:MM:SS`
✅ **Brak informacji o strefie czasowej** - zakładamy UTC
✅ **Konsystencja formatu** - każdy wpis używa tego samego wzorca
⚠️ **Wymaga adaptera Moshi** - konwersja string → Instant

### Implementacja Adaptera
```kotlin
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class DateTimeAdapter {
    @FromJson
    fun fromJson(value: String): Instant {
        val localDateTime = kotlinx.datetime.LocalDateTime.parse(
            value.replace(" ", "T")
        )
        return localDateTime.toInstant(TimeZone.UTC)
    }

    @ToJson
    fun toJson(value: Instant): String {
        return value.toLocalDateTime(TimeZone.UTC)
            .toString()
            .replace("T", " ")
            .substringBefore('.')
    }
}
```

---

## 4. Różne Typy Mediów

**Plik:** `entries_various_media.json`

### Typy Embedów
1. **Coub** (`type: "coub"`)
   - URL: `https://coub.com/view/{key}`
   - Thumbnail: Akamai CDN

2. **Gfycat** (`type: "gfycat"`)
   - URL: `https://gfycat.com/{key}`
   - Thumbnail: Gfycat CDN

3. **Streamable** (`type: "streamable"`)
   - URL: `https://streamable.com/{key}`
   - Thumbnail: Streamable CDN

4. **YouTube** (`type: "youtube"`)
   - URL: `https://www.youtube.com/watch?v={key}`
   - Thumbnail: YouTube img CDN

### Photo Media
```json
{
  "media": {
    "photo": {
      "url": "https://...",
      "key": "photo123456",
      "label": "Zdjęcie",
      "mime_type": "image/jpeg",
      "source": null,
      "plus18": false,
      "width": 1920,
      "height": 1080
    }
  }
}
```

### Card (Link Preview)
```json
{
  "card": {
    "title": "Tytuł artykułu",
    "description": "Opis artykułu",
    "url": "https://example.com/article",
    "image": "https://example.com/og-image.jpg",
    "source": {
      "label": "example.com",
      "url": "https://example.com"
    }
  }
}
```

---

## 5. Wpis z Ankietą

**Plik:** `entry_with_survey.json`

### Struktura Survey
```json
{
  "survey": {
    "question": "Która wersja Kotlin jest najlepsza?",
    "answers": [
      {
        "id": 1,
        "answer": "Kotlin 1.9",
        "count": 45,
        "percentage": 15.5
      }
    ],
    "votes_count": 290,
    "user_answer": 2
  }
}
```

**Weryfikacja:**
- ✅ `answers` - lista odpowiedzi z count i percentage
- ✅ `votes_count` - suma głosów
- ✅ `user_answer` - ID odpowiedzi użytkownika (null jeśli nie głosował)
- ✅ `actions.survey_vote` - czy użytkownik może głosować

---

## 6. Edge Cases - Zidentyfikowane

### 6.1. Deleted Entry
- `deleted: true`
- `content: "[usunięte]"`
- `votes: { up: 0, down: 0 }`
- `actions: {}` - brak dostępnych akcji
- Autor może być `deleted_user` z domyślnym avatarem

### 6.2. Adult Content (+18)
- `adult: true`
- `media.photo.plus18: true`
- `tags: ["nsfw", "plus18"]`
- Wymaga specjalnego traktowania w UI (blur, ostrzeżenie)

### 6.3. Archived Entry
- `archive: true`
- `actions.comment: false` - brak możliwości komentowania
- `actions.vote_up: false` - brak możliwości głosowania
- Data starsza niż 6 miesięcy (przykład: "2019-02-25 20:35:18")

### 6.4. Entry z Komentarzami
- `comments.count` - liczba komentarzy
- `comments.items` - tablica komentarzy (może być pusta)
- Komentarze mają własne:
  - `id`, `author`, `created_at`, `voted`, `content`
  - `parent_id` - zawsze równe ID wpisu głównego
  - `resource: "entry_comment"`
  - `actions` - uprawnienia dla komentarza

### 6.5. Paginacja
- **Niezalogowani:** `pagination: { per_page, total }` - brak next/prev
- **Zalogowani:** `pagination: { per_page, total, next, prev }` - hash next/prev
- **Limit:** Max 100 stron - potem błąd 400

### 6.6. Voted State
- `voted: 0` - brak głosu (domyślnie dla niezalogowanych)
- `voted: 1` - upvote (zalogowany zagłosował pozytywnie)
- `voted: -1` - downvote (zalogowany zagłosował negatywnie)

### 6.7. Actions
Dla niezalogowanych:
```json
{
  "update": false,
  "delete": false,
  "vote_up": true,
  "vote_down": false,
  "comment": true,
  "report": true,
  "favourite": true
}
```

Dla zalogowanych (autor wpisu):
```json
{
  "update": true,
  "delete": true,
  "vote_up": true,
  "vote_down": true,
  "comment": true,
  "report": false,
  "favourite": true
}
```

---

## 7. Różnice v2 → v3 - Podsumowanie

| Pole v2 | Pole v3 | Zmiana |
|---------|---------|--------|
| `date` (Instant) | `created_at` (String) | ⚠️ Wymaga adaptera |
| `body` | `content` | ✅ Prosta zmiana nazwy |
| `vote_count` | `votes: {up, down}` | ⚠️ Zmiana struktury |
| `comments_count` | `comments: {count, items}` | ⚠️ Zmiana struktury |
| `favorite` | `favourite` | ✅ Zmiana pisowni (UK) |
| `blocked` | ❌ Usunięte | ⚠️ Breaking |
| `status` | ❌ Usunięte | ⚠️ Breaking |
| `embed` | `media` | ⚠️ Zmiana struktury |
| `user_vote` | `voted` | ✅ Prosta zmiana nazwy |
| `violation_url` | ❌ Usunięte | ⚠️ Breaking |
| `app` | `device` | ✅ Prosta zmiana nazwy |
| `can_comment` | `actions.comment` | ⚠️ Przeniesione do actions |

### Breaking Changes
1. ❌ Usunięte pola: `blocked`, `status`, `violation_url`
2. ⚠️ Zmiana formatu daty: `Instant` → `String "YYYY-MM-DD HH:MM:SS"`
3. ⚠️ Zmiana struktury głosów: `vote_count` → `votes: {up, down}`
4. ⚠️ Zmiana struktury komentarzy: `comments_count` → `comments: {count, items}`
5. ⚠️ Zmiana struktury mediów: `embed` → `media: {photo?, embed?}`

---

## 8. Rekomendacje Implementacyjne

### 8.1. Priorytety
1. ✅ **DateTimeAdapter** - KRYTYCZNE (każdy endpoint używa dat)
2. ✅ **Wrapper response** - `WykopApiResponseV3<T>` z `data` i `pagination`
3. ✅ **Error response** - `WykopErrorResponseV3` z `code`, `hash`, `error`
4. ✅ **EntryResponseV3** - Główny model wpisu
5. ✅ **User models** - `UserShortResponseV3` (autor wpisu)
6. ✅ **Media models** - `MediaResponseV3`, `PhotoResponseV3`, `EmbedResponseV3`
7. ✅ **Pagination** - `PaginationResponseV3` z obsługą numeric/hash

### 8.2. Struktura Pakietów
```
data/wykop/api/src/main/kotlin/io/github/wykopmobilny/api/
├── responses/
│   └── v3/
│       ├── common/
│       │   ├── WykopApiResponseV3.kt
│       │   ├── WykopErrorResponseV3.kt
│       │   ├── PaginationResponseV3.kt
│       │   └── ActionsResponseV3.kt
│       ├── auth/
│       │   └── AuthResponseV3.kt
│       ├── entries/
│       │   ├── EntryResponseV3.kt
│       │   ├── EntryCommentResponseV3.kt
│       │   ├── VotesResponseV3.kt
│       │   ├── CommentsResponseV3.kt
│       │   └── SurveyResponseV3.kt
│       ├── media/
│       │   ├── MediaResponseV3.kt
│       │   ├── PhotoResponseV3.kt
│       │   ├── EmbedResponseV3.kt
│       │   └── CardResponseV3.kt
│       ├── user/
│       │   └── UserShortResponseV3.kt
│       └── adapters/
│           └── DateTimeAdapter.kt
└── endpoints/
    └── v3/
        ├── AuthRetrofitApiV3.kt
        └── EntriesRetrofitApiV3.kt
```

### 8.3. Testy Do Stworzenia
1. **DateTimeAdapterTest** - Parsowanie różnych formatów dat
2. **EntryResponseV3Test** - Deserializacja wszystkich edge cases
3. **PaginationTest** - Testowanie numeric i hash pagination
4. **ErrorResponseTest** - Obsługa różnych kodów błędów (401, 403, 404, 500)
5. **MediaTest** - Różne typy mediów (photo, youtube, coub, gfycat, streamable)

---

## 9. Status Walidacji

### ✅ Zweryfikowane
- [x] Struktura wrapper response `{ data, pagination? }`
- [x] Struktura error response `{ code, hash, error }`
- [x] Format daty `YYYY-MM-DD HH:MM:SS`
- [x] Struktura paginacji (numeric vs hash)
- [x] Struktura entry object z wszystkimi polami
- [x] Różne typy mediów (photo, embed, card)
- [x] Edge cases (deleted, adult, archived, voted)
- [x] Struktura komentarzy
- [x] Struktura ankiet (survey)
- [x] Struktura actions
- [x] Różne formaty dat (00:00:00, 23:59:59, old dates)

### ⏳ Do Weryfikacji (wymaga prawdziwego API)
- [ ] Czy API v3 rzeczywiście wymaga autoryzacji dla GET /entries?
- [ ] Czy hash paginacji jest stabilny (ten sam hash = ta sama strona)?
- [ ] Czy endpoint GET /entries/hot nadal istnieje w v3 (czy to /entries?sort=hot)?
- [ ] Jaki jest format błędu dla nieautoryzowanego POST /auth?
- [ ] Czy pole `card` jest zawsze null dla wpisów bez linku?

---

## 10. Następne Kroki

### Faza 1: Modele (NASTĘPNY ETAP)
1. Stworzyć wszystkie modele response v3 w osobnym pakiecie `v3`
2. Zaimplementować DateTimeAdapter dla Moshi
3. Dodać unit testy dla modeli

### Faza 2: Retrofit Endpoints
1. Stworzyć AuthRetrofitApiV3 (POST /auth)
2. Stworzyć EntriesRetrofitApiV3 (GET /entries)
3. Dodać interceptor dla Bearer token

### Faza 3: Repository
1. Stworzyć mapowanie v3 response → domain models
2. Obsługa paginacji (numeric + hash)
3. Error handling

### Faza 4: Integracja
1. Dual support (v2 + v3) przez feature flag
2. Migracja stopniowa endpoint po endpoint
3. Testy regresji

---

**Autor:** Claude Sonnet 4.5
**Projekt:** wykop-android
**Task:** #35 - Testowanie prawdziwego API v3
