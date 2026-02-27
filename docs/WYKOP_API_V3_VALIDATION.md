# Walidacja Formatu Odpowiedzi Wykop API v3

**Data:** 2026-02-27
**Cel:** Zweryfikować strukturę JSON, paginację i format dat przed pełną implementacją modeli response v3

## 1. POST /auth - Autoryzacja

### Endpoint
```
POST https://wykop.pl/api/v3/auth
```

### Request Body
```json
{
  "username": "string",
  "password": "string"
}
```

### Response Success (200)
```json
{
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Struktura:**
- `data` (object) - kontener główny
- `data.token` (string) - Token JWT do autoryzacji

**JWT Payload zawiera:**
- `iat` - issued at timestamp
- `exp` - expiration timestamp
- `roles` - role użytkownika
- `username` - nazwa użytkownika
- `user-agent` - user agent
- `user_ip` - IP użytkownika
- `id` - ID użytkownika

### Response Error (401)
```json
{
  "code": 401,
  "hash": "abc123",
  "error": {
    "message": "Nieprawidłowe dane uwierzytelniające",
    "key": 1001
  }
}
```

### Różnice v2 → v3
| v2 | v3 |
|----|-----|
| `{ profile, userkey }` | `{ data: { token } }` |
| userkey jako klucz auth | JWT token jako Bearer |
| Zagnieżdżony profil | Token zawiera claims |

---

## 2. GET /entries - Lista wpisów

### Endpoint
```
GET https://wykop.pl/api/v3/entries
```

### Query Parameters
| Parametr | Typ | Opis |
|----------|-----|------|
| page | int/string | Numer strony (int dla niezalogowanych, hash dla zalogowanych) |
| limit | int | Liczba wyników na stronę |
| sort | string | Sortowanie (hot, active, newest) |
| filter | string | Filtrowanie wpisów |
| category | string | Filtr po kategorii |
| bucket | string | Filtr po sekcji |
| multimedia | boolean | Filtr po multimediach |

### Response Success (200)
```json
{
  "data": [
    {
      "id": 123456,
      "author": {
        "username": "user123",
        "avatar": "https://...",
        "...": "..."
      },
      "device": "android",
      "created_at": "2019-02-25 20:35:18",
      "voted": 0,
      "content": "Treść wpisu...",
      "media": {
        "photo": {
          "url": "https://...",
          "...": "..."
        }
      },
      "adult": false,
      "tags": ["programowanie", "android"],
      "favourite": false,
      "deletable": true,
      "slug": "wpis-o-programowaniu",
      "votes": {
        "up": 42,
        "down": 3
      },
      "comments": {
        "count": 15,
        "items": []
      },
      "parent_id": null,
      "resource": "entry",
      "actions": {},
      "archive": false,
      "deleted": false,
      "observed_discussion": true,
      "pinnable": false,
      "card": null
    }
  ],
  "pagination": {
    "per_page": 25,
    "total": 450
  }
}
```

### Struktura Entry Object

| Pole | Typ | Opis |
|------|-----|------|
| id | integer | ID wpisu |
| author | user.short | Obiekt autora |
| device | string | Urządzenie (android, ios, web) |
| created_at | string | **Format: "YYYY-MM-DD HH:MM:SS"** |
| voted | int | Stan głosu użytkownika (0, 1, -1) |
| content | string | Treść wpisu |
| media | object | Obiekt mediów (photo, embed) |
| adult | boolean | Czy zawiera treści +18 |
| tags | array[string] | Lista tagów |
| favourite | boolean | Czy dodano do ulubionych |
| deletable | boolean | Czy można usunąć |
| slug | string | Slug SEO |
| votes | object | Obiekt głosów {up, down} |
| comments | object | Obiekt komentarzy {count, items} |
| parent_id | integer | ID rodzica (dla komentarzy) |
| resource | string | Typ zasobu ("entry") |
| actions | actions.entry | Dostępne akcje |
| archive | boolean | Czy zarchiwizowany |
| deleted | boolean | Czy usunięty |
| observed_discussion | boolean | Czy obserwowana dyskusja |
| pinnable | boolean | Czy można przypiąć |
| card | object | Karta podglądu |

### Struktura Pagination

```json
{
  "per_page": 25,
  "total": 450
}
```

**Uwaga:** Dla zalogowanych użytkowników paginacja może zwracać dodatkowe pola:
- `next` (string) - hash następnej strony
- `prev` (string) - hash poprzedniej strony

### Różnice v2 → v3

| v2 | v3 |
|----|-----|
| `date` (Instant) | `created_at` (string) |
| `body` | `content` |
| `vote_count` | `votes: {up, down}` |
| `comments_count` | `comments: {count, items}` |
| `favorite` | `favourite` |
| `blocked` | ❌ usunięte |
| `status` | ❌ usunięte |
| `embed` | `media` |
| `user_vote` | `voted` |
| `violation_url` | ❌ usunięte |
| `app` | `device` |
| `can_comment` | ❌ (w `actions`) |

---

## 3. Format Daty - KRYTYCZNE

### v2 (obecny)
```kotlin
@field:Json(name = "date") val date: Instant
```
- Automatyczne parsowanie przez kotlinx-datetime
- Format: ISO 8601 z timezone

### v3 (nowy)
```kotlin
@field:Json(name = "created_at") val createdAt: String
```
- **Format: "YYYY-MM-DD HH:MM:SS"** (np. "2019-02-25 20:35:18")
- **Brak informacji o strefie czasowej**
- Wymaga własnego adaptera Moshi lub konwersji do Instant

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
        // Zakładamy UTC jako domyślną strefę czasową
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

## 4. Struktura Wrapper Response

### v3 Standard
Wszystkie odpowiedzi sukcesu zwracają:
```json
{
  "data": <T>
}
```

Gdzie `<T>` to:
- Obiekt (np. dla POST /auth)
- Tablica (np. dla GET /entries)
- Obiekt z paginacją (GET /entries z parametrem page)

### Błędy
```json
{
  "code": 400/401/403/404/500,
  "hash": "unique_error_hash",
  "error": {
    "message": "Human readable message",
    "key": 1001
  }
}
```

---

## 5. Paginacja - Szczegóły

### Dla niezalogowanych użytkowników
```
GET /entries?page=1&limit=25
```
Response:
```json
{
  "data": [...],
  "pagination": {
    "per_page": 25,
    "total": 450
  }
}
```
**Obliczanie liczby stron:** `Math.ceil(total / per_page)` = 18 stron

### Dla zalogowanych użytkowników
```
GET /entries?page=abc123hash&limit=25
Authorization: Bearer <jwt_token>
```
Response:
```json
{
  "data": [...],
  "pagination": {
    "per_page": 25,
    "total": 450,
    "next": "def456hash",
    "prev": "xyz789hash"
  }
}
```
**Nawigacja:** Użyj hash z `next`/`prev` zamiast numerów stron

### Limit paginacji
API zwraca błąd 400 gdy przekroczono limit stron:
```json
{
  "code": 400,
  "error": {
    "message": "Search does not serve more than 100th results for query.",
    "key": "limit_exceeded"
  }
}
```

---

## 6. Rekomendacje Implementacyjne

### Priorytety
1. ✅ **Najpierw:** Stworzyć modele dla POST /auth (prosty)
2. ✅ **Następnie:** Stworzyć modele dla GET /entries (kompleksowy)
3. ✅ **Ostatnie:** Adapter dat i testy integracyjne

### Struktura Modeli v3

```
api/responses/v3/
├── WykopApiResponseV3.kt         # Wrapper { data, pagination? }
├── WykopErrorResponseV3.kt       # Error { code, hash, error }
├── AuthResponseV3.kt             # { token }
├── EntryResponseV3.kt            # Entry object
├── UserShortResponseV3.kt        # Author object
├── MediaResponseV3.kt            # Media object
├── VotesResponseV3.kt            # Votes { up, down }
├── CommentsResponseV3.kt         # Comments { count, items }
├── PaginationResponseV3.kt       # { per_page, total, next?, prev? }
└── adapters/
    └── DateTimeAdapter.kt        # Moshi adapter dla dat
```

### Testy Do Wykonania
1. ✅ POST /auth z poprawnymi danymi → Sprawdzić format tokenu JWT
2. ✅ POST /auth z błędnymi danymi → Sprawdzić strukturę błędu 401
3. ✅ GET /entries bez auth → Sprawdzić paginację numeryczną
4. ✅ GET /entries z auth → Sprawdzić paginację hashową
5. ✅ GET /entries?page=999999 → Sprawdzić błąd przekroczenia limitu
6. ✅ Sprawdzić format daty w różnych entry → Zweryfikować konsystencję

---

## 7. Potencjalne Problemy

### ⚠️ Format Daty
- Brak timezone w "YYYY-MM-DD HH:MM:SS"
- Trzeba założyć UTC lub pobrać z serwera info o strefie
- Może powodować problemy z lokalizacją czasu

### ⚠️ Paginacja
- Dwa różne typy paginacji (numeric vs hash)
- Wymaga oddzielnej logiki dla zalogowanych/niezalogowanych
- Hash jest nieprzewidywalny (nie można "skoczyć" do strony N)

### ⚠️ Breaking Changes
- Wszystkie modele v2 są niekompatybilne
- Wymaga przepisania całej warstwy API
- Retrofit endpoints wymagają zmiany ścieżek (/api/v3 prefix)

### ⚠️ Brak Backward Compatibility
- v2 używa `List<EntryResponse>` bezpośrednio
- v3 zawsze zwraca `{ data: [...] }`
- Wymaga zmiany typu zwracanego w Repository/UseCase

---

## 8. Plan Migracji

### Faza 1: Walidacja (AKTUALNY ETAP)
- ✅ Przeanalizować dokumentację Swagger
- ✅ Zidentyfikować różnice v2 → v3
- ⏳ **TODO:** Przetestować prawdziwe API (POST /auth + GET /entries)
- ⏳ **TODO:** Zweryfikować formaty dat w praktyce
- ⏳ **TODO:** Sprawdzić edge case'y paginacji

### Faza 2: Modele
- Stworzyć modele response v3
- Stworzyć adapter dat
- Unit testy dla modeli

### Faza 3: Endpoints
- Przepisać EntriesRetrofitApi na v3
- Przepisać LoginRetrofitApi na v3 (AuthRetrofitApi?)
- Dodać interceptor dla Bearer token

### Faza 4: Repository
- Mapowanie v3 response → domain models
- Obsługa paginacji (numeric + hash)
- Error handling

### Faza 5: Testy Integracyjne
- Mock server z v3 responses
- Testy end-to-end
- Testy regresji

---

## 9. Pytania Do Wyjaśnienia

1. ❓ Czy API v3 używa UTC dla wszystkich dat?
2. ❓ Czy hash paginacji jest stabilny (ten sam hash = ta sama strona)?
3. ❓ Czy endpoint GET /entries/hot nadal istnieje w v3?
4. ❓ Czy pola `next`/`prev` w paginacji są zawsze obecne dla zalogowanych?
5. ❓ Czy struktura `media` zawsze ma `photo`? Jak wygląda dla `embed`?
6. ❓ Czy pole `voted` może być null dla niezalogowanych?
7. ❓ Jak wygląda obiekt `actions.entry`?

---

## 10. Następne Kroki

### Natychmiastowe:
1. **Przetestować prawdziwe API:**
   - Wykonać POST /auth z testowymi danymi
   - Zapisać pełny JSON response
   - Wykonać GET /entries (bez auth i z auth)
   - Zapisać pełne JSON responses
   - Zweryfikować format dat

2. **Stworzyć proof-of-concept:**
   - Minimal working example z Retrofit + Moshi
   - 2 endpointy: POST /auth, GET /entries
   - Adapter dat

3. **Zdecydować o strategii:**
   - Big bang migration vs stopniowa
   - Dual support (v2 + v3) vs tylko v3
   - Timeline migracji

### Do Rozważenia:
- Czy zachować osobny moduł `api-v3`?
- Czy użyć code generation (OpenAPI → Kotlin)?
- Czy stworzyć wspólne interfejsy dla v2/v3?

---

**Status:** ⏳ Czeka na weryfikację z prawdziwym API
**Next:** Wykonać testy z rzeczywistym Wykop API v3
