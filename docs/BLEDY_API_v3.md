# Znane błędy API v3 (Wykop)

Spis potwierdzonych błędów backendu Wykop API v3, na które aplikacja stosuje obejścia.
Cel: móc po pewnym czasie zweryfikować, czy błąd został naprawiony po stronie API i wtedy
usunąć zbędne obejście z aplikacji.

**Jak weryfikować:** wykonaj kroki z sekcji „Weryfikacja". Jeśli API zachowuje się już
poprawnie → zmień status na „naprawiony (data)", usuń obejście wskazane w „Obejście w kodzie"
i odnotuj w tym pliku.

Legenda statusu: 🔴 aktywny · 🟡 do weryfikacji · 🟢 naprawiony (obejście usunięte)

---

## API-001 — pole `note` autora nie jest czyszczone po usunięciu notatki

- **Status:** 🔴 aktywny
- **Wykryto:** 2026-07-20
- **Endpointy:** `GET /v3/notes/{username}`, oraz pole `note` na obiekcie autora
  (`UserShortResponseV3`) we wszystkich odpowiedziach z komentarzami/wpisami.

### Opis
Po usunięciu notatki o użytkowniku (PUT `/v3/notes/{username}` z pustą treścią) backend
nadal zwraca `note: true` na obiekcie autora oraz w `GET /v3/notes/{username}` w polu
`user.note`, mimo że `content` jest już pusty (`""`). Skutek w UI: „wisząca" żółta kartka
notatki przy użytkownikach, którzy notatki już nie mają.

Kluczowa obserwacja: użytkownicy, którzy **nigdy** nie mieli notatki, mają poprawnie
`note: false`. Błędne `true` dotyczy tylko tych, którym notatkę **usunięto**.

### Weryfikacja
1. Dodaj notatkę użytkownikowi X (PUT z niepustą treścią).
2. Usuń ją (PUT z pustą treścią `""`).
3. `GET /v3/notes/X` → jeśli zwraca `content: ""` **oraz** `user.note: true` → błąd nadal
   występuje. Poprawne zachowanie: `user.note: false` (lub 404/brak wpisu).
4. Alternatywnie: pobierz komentarze/wpisy, gdzie X jest autorem → pole `author.note`
   powinno być `false`.

### Obejście w kodzie
Lokalny override statusu notatki wymuszający ukrycie kartki, gdy wiemy, że notatki nie ma:
- `NoteOverrideCache` (`app/.../api/notes/NoteOverrideCache.kt`) — zbiór loginów w pamięci
  + trwała tabela `noteOverrideEntity` (`data/cache/api/.../noteOverride.sq`).
- Zapis override w `NotesRepository` (`app/.../api/notes/NotesRepository.kt`): (a) gdy
  `getNote` zwraca pusty `content` a `user.note == true` (dowód błędu), (b) przy zapisie
  pustej notatki. Kasowanie override (samonaprawa), gdy `content` jest niepusty.
- Nałożenie override przy renderze: `AuthorMapperV3` (mikroblog i in.) oraz
  `persistLinkComments` (komentarze linków).

### Po naprawie API (co usunąć)
Usuń `NoteOverrideCache`, tabelę `noteOverrideEntity`, zapisy override w `NotesRepository`
oraz nałożenia w `AuthorMapperV3` i `persistLinkComments`. `hasNote` = wprost `author.note`.

---

## API-002 — spec kłamie o polu głosujących na linki

- **Status:** 🔴 aktywny (znane, obejście istnieje)
- **Opis:** dla głosujących na linki specyfikacja OpenAPI nie zgadza się z realną odpowiedzią
  (patrz pamięć projektu `api_v3_rules`). Szczegóły do uzupełnienia przy najbliższej okazji.
- **Weryfikacja:** porównaj realną odpowiedź endpointu głosujących z `docs/wykop_api_v3_openapi.yaml`.
