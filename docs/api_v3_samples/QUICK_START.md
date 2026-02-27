# Quick Start - Wykop API v3 Samples

Szybki przewodnik po przykładach API v3

## 📦 Co zawiera ten katalog?

- **10 plików JSON** - Przykładowe odpowiedzi API v3
- **4 pliki dokumentacji** - Pełna dokumentacja i przewodniki
- **1 skrypt walidacji** - Sprawdzanie formatów dat

## 🚀 Jak zacząć?

### 1. Przeczytaj dokumentację
```bash
# Pełna dokumentacja walidacji
cat README.md

# Szybkie odniesienie
cat INDEX.md

# Podsumowanie wyników
cat VALIDATION_SUMMARY.md
```

### 2. Zobacz przykłady API

#### Podstawowy GET /entries
```bash
cat entries_success_noauth.json
```
Zawiera 5 różnych typów wpisów (photo, embed, text, deleted, adult)

#### GET /entries z autoryzacją
```bash
cat entries_success_with_auth.json
```
Zawiera komentarze, hash pagination (next/prev)

#### Różne typy mediów
```bash
cat entries_various_media.json
```
Zawiera coub, gfycat, streamable, card

#### Wpis z ankietą
```bash
cat entry_with_survey.json
```
Zawiera survey z 3 odpowiedziami

### 3. Zweryfikuj formaty dat

```bash
python validate_dates.py
```

Sprawdza czy wszystkie daty używają formatu `YYYY-MM-DD HH:MM:SS`

## 📋 Najważniejsze Pliki

| Plik | Opis | Użycie |
|------|------|--------|
| `README.md` | Pełna dokumentacja | Przeczytaj najpierw |
| `INDEX.md` | Quick reference | Szybkie wyszukiwanie |
| `VALIDATION_SUMMARY.md` | Podsumowanie wyników | Zobacz co zrobiono |
| `entries_success_noauth.json` | Podstawowe entries | Zacznij od tego |
| `validate_dates.py` | Walidacja dat | Uruchom dla weryfikacji |

## 🎯 Najczęstsze Przypadki Użycia

### Dla Developerów
1. Implementacja modeli → `entries_success_noauth.json`
2. Unit testy → Użyj wszystkich JSON jako fixtures
3. Adapter dat → `validate_dates.py` + `entries_date_formats.json`

### Dla Testerów
1. Testowanie API → Porównaj z przykładami
2. Edge cases → `entries_date_formats.json`, `entries_various_media.json`
3. Error handling → `*_error_*.json`

### Dla Architektów
1. Planowanie → `README.md` sekcja 8
2. Breaking changes → `README.md` sekcja 7
3. Struktura pakietów → `README.md` sekcja 8.2

## 🔍 Szybkie Wyszukiwanie

**Wpis z komentarzami:**
```bash
cat entries_success_with_auth.json | grep -A 20 "comments"
```

**Wszystkie daty:**
```bash
python validate_dates.py
```

**Struktura paginacji:**
```bash
cat entries_success_with_auth.json | grep -A 5 "pagination"
```

**Typy mediów:**
```bash
cat entries_various_media.json | grep -A 3 '"type":'
```

## ✅ Weryfikacja

Sprawdź czy wszystko jest OK:

```bash
# Walidacja JSON
for file in *.json; do 
  python -c "import json; json.load(open('$file'))" && echo "✓ $file" || echo "✗ $file"
done

# Walidacja dat
python validate_dates.py

# Liczba plików
ls -1 *.json | wc -l  # Powinno być 10
ls -1 *.md | wc -l    # Powinno być 4
```

## 📚 Kolejność Czytania

1. **QUICK_START.md** ← Jesteś tu!
2. **INDEX.md** - Szybki przegląd
3. **VALIDATION_SUMMARY.md** - Co zostało zrobione
4. **README.md** - Pełna dokumentacja (12KB)
5. **Pliki JSON** - Przykłady API

## 🆘 Pomoc

**Problem:** Nie wiem od czego zacząć
→ Przeczytaj `INDEX.md`

**Problem:** Potrzebuję konkretnego przykładu
→ Sprawdź `INDEX.md` sekcja "Szybkie Wyszukiwanie"

**Problem:** Chcę zaimplementować modele
→ Przeczytaj `README.md` sekcja 8 "Rekomendacje Implementacyjne"

**Problem:** Format daty jest niejasny
→ Uruchom `python validate_dates.py`

---

**Data:** 2026-02-27
**Autor:** Claude Sonnet 4.5
**Projekt:** wykop-android
