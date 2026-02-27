# Index Plików - Wykop API v3 Samples

## Quick Reference

### 📁 Struktura Plików

```
docs/api_v3_samples/
├── README.md                           # Pełna dokumentacja walidacji
├── INDEX.md                            # Ten plik (quick reference)
│
├── auth_success.json                   # ✅ POST /auth sukces (200)
├── auth_error_401.json                 # ❌ POST /auth błąd (401 Unauthorized)
├── auth_error_403.json                 # ❌ POST /auth błąd (403 Forbidden)
├── auth_error_500.json                 # ❌ POST /auth błąd (500 Internal Error)
│
├── entries_success_noauth.json         # ✅ GET /entries bez auth (numeric pagination)
├── entries_success_with_auth.json      # ✅ GET /entries z auth (hash pagination)
├── entries_limit_error.json            # ❌ GET /entries błąd limitu (400)
├── entries_date_formats.json           # 📅 Różne formaty dat (edge cases)
├── entries_various_media.json          # 🎬 Różne typy mediów (coub, gfycat, streamable, card)
└── entry_with_survey.json              # 📊 Wpis z ankietą (survey)
```

---

## 🔍 Szybkie Wyszukiwanie

### Chcę zobaczyć...

**...podstawową odpowiedź GET /entries:**
→ `entries_success_noauth.json` (5 wpisów, różne typy)

**...odpowiedź z paginacją hashową:**
→ `entries_success_with_auth.json` (zawiera next/prev)

**...wpis z komentarzami:**
→ `entries_success_with_auth.json` (entry ID 234567)

**...różne typy mediów:**
→ `entries_various_media.json` (coub, gfycat, streamable, card)

**...wpis z ankietą:**
→ `entry_with_survey.json` (survey z 3 odpowiedziami)

**...usunięty wpis:**
→ `entries_success_noauth.json` (entry ID 123459, deleted=true)

**...wpis +18:**
→ `entries_success_noauth.json` (entry ID 123460, adult=true)

**...wpis zarchiwizowany:**
→ `entries_date_formats.json` (entry ID 456791, archive=true)

**...format daty o północy:**
→ `entries_date_formats.json` (entry ID 456789, "2026-02-27 00:00:00")

**...błąd autoryzacji:**
→ `auth_error_401.json` (nieprawidłowe dane)

**...błąd limitu stron:**
→ `entries_limit_error.json` (przekroczenie 100 stron)

---

## 📊 Statystyki Przykładów

| Typ | Liczba Plików | Przykłady |
|-----|---------------|-----------|
| Sukces (2xx) | 6 | auth_success, entries_success_* |
| Błędy (4xx/5xx) | 4 | auth_error_*, entries_limit_error |
| Edge Cases | 3 | entries_date_formats, entries_various_media, entry_with_survey |
| **TOTAL** | **13** | 10 JSON + 2 MD + 1 INDEX |

---

## 🎯 Przypadki Użycia

### Dla Developerów:
1. **Implementacja modeli:** Rozpocznij od `entries_success_noauth.json` → podstawowa struktura
2. **Testy unit:** Użyj wszystkich plików jako test fixtures
3. **Adapter dat:** Zobacz `entries_date_formats.json` → 3 różne formaty
4. **Error handling:** Sprawdź wszystkie `*_error_*.json`

### Dla Testerów:
1. **Walidacja API:** Porównaj rzeczywiste API z przykładami
2. **Edge cases:** Sprawdź wszystkie edge cases z `entries_date_formats.json` i `entries_various_media.json`
3. **Regresja:** Użyj jako baseline dla testów regresji

### Dla Architektów:
1. **Planowanie modeli:** Zobacz pełną strukturę w `README.md` → sekcja 8.2
2. **Breaking changes:** Sprawdź tabelę v2→v3 w `README.md` → sekcja 7
3. **Rekomendacje:** Przeczytaj `README.md` → sekcja 8

---

## 📝 Notatki

### Format Daty
✅ **Wszystkie pliki używają:** `YYYY-MM-DD HH:MM:SS`
⚠️ **Brak timezone** - zakładamy UTC

### Paginacja
- **Numeric:** `{ per_page, total }` - dla niezalogowanych
- **Hash:** `{ per_page, total, next, prev }` - dla zalogowanych

### Voted State
- `0` = brak głosu (default)
- `1` = upvote
- `-1` = downvote

### Media Types
- `photo` - zdjęcie (url, width, height, mime_type, plus18)
- `embed` - video (youtube, coub, gfycat, streamable)
- `card` - link preview (title, description, url, image, source)
- `null` - brak mediów

---

**Ostatnia aktualizacja:** 2026-02-27
**Autor:** Claude Sonnet 4.5
**Projekt:** wykop-android
