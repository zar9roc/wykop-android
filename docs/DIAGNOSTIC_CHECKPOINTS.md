# Diagnostic Checkpoints

System diagnostycznych checkpointow do weryfikacji dzialania zmian UI/layout w runtime.

## Zasada projektu

> W przypadku pracy nad layoutem UI i wprowadzaniem zmian w dzialaniu aplikacji, dodaj diagnostyczny log checkpoint ktory bedzie odczytywany przez HTTP debug server aby potwierdzic, ze dana funkcjonalnosc zadziala zgodnie z zalozeniem.

## API - DiagnosticCheckpoint

Singleton `DiagnosticCheckpoint` w pakiecie `io.github.wykopmobilny.debug` (dostepny tylko w debug buildach).

### Logowanie checkpointu

```kotlin
import io.github.wykopmobilny.debug.DiagnosticCheckpoint

// W kodzie UI/Fragment/ViewHolder:
DiagnosticCheckpoint.log("LinkDetails", "Header loaded: title=${link.title}, votes=${link.voteCount}")
DiagnosticCheckpoint.log("EntryVote", "Vote toggled: voted=$isVoted, count=$voteCount")
DiagnosticCheckpoint.log("Navigation", "Opened tab: $tabName")
DiagnosticCheckpoint.log("Pagination", "Loaded page $page with ${items.size} items")
```

### Parametry `log(tag, message)`

| Parametr | Opis |
|----------|------|
| `tag` | Krotki identyfikator feature/ekranu (np. "LinkDetails", "EntryVote") |
| `message` | Opis co sie wydarzylo, z konkretnymi wartosciami |

### Konwencja tagow

| Tag | Zastosowanie |
|-----|-------------|
| `LinkDetails` | Widok szczegolowy znaleziska |
| `EntryDetails` | Widok szczegolowy wpisu |
| `EntryVote` | Glosowanie na wpis |
| `LinkVote` | Glosowanie na znalezisko |
| `Navigation` | Nawigacja miedzy ekranami |
| `Pagination` | Ladowanie kolejnych stron |
| `ImageLoad` | Ladowanie obrazkow/miniatur |
| `Login` | Flow logowania |

### Zaimplementowane checkpointy

#### LinkDetails (LinkDetailsFragment)

1. **Header loaded** - zalogowane gdy header zostanie zaladowany z danymi
   - Format: `"Header loaded: title={title}, votes={count}, comments={label}"`
   - Miejsce: `LinkDetailsFragment.kt` (collect UI state)

2. **Adapter list updated** - zalogowane przy kazdej aktualizacji listy adaptera
   - Format: `"Adapter list updated: {size} items"`
   - Miejsce: `LinkDetailsFragment.kt` (adapterList.collect)

3. **Scrolled to comment** - zalogowane gdy automatyczny scroll do komentarza zostanie wykonany
   - Format: `"Scrolled to comment: commentId={id}, position={index}"`
   - Miejsce: `LinkDetailsFragment.kt` (scroll to target comment)

## Odczyt checkpointow - HTTP Debug Server

### Wymagana konfiguracja

```bash
adb forward tcp:8899 tcp:8899
```

### Endpointy

| Metoda | URL | Opis |
|--------|-----|------|
| GET | `/checkpoints` | Wszystkie checkpointy |
| GET | `/checkpoints?tag=LinkDetails` | Filtrowanie po tagu |
| DELETE | `/checkpoints` | Wyczysc wszystkie checkpointy |
| GET | `/action/clear-checkpoints` | Wyczysc (wygodne z przegladarki) |

### Przyklad odpowiedzi

```json
{
  "count": 3,
  "filter_tag": null,
  "checkpoints": [
    {
      "timestamp": "2026-03-13 14:30:01.123",
      "timestamp_ms": 1710340201123,
      "tag": "LinkDetails",
      "message": "Header loaded: title=Przykladowy link, votes=42",
      "thread": "main"
    },
    {
      "timestamp": "2026-03-13 14:30:01.456",
      "timestamp_ms": 1710340201456,
      "tag": "LinkDetails",
      "message": "Comments loaded: count=15",
      "thread": "main"
    },
    {
      "timestamp": "2026-03-13 14:30:02.789",
      "timestamp_ms": 1710340202789,
      "tag": "LinkVote",
      "message": "Vote toggled: voted=true, count=43",
      "thread": "main"
    }
  ]
}
```

## Odczyt checkpointow - Logcat

Checkpointy sa rownoczesnie logowane przez Napier z tagiem `DiagnosticCheckpoint`:

```bash
adb logcat -s DiagnosticCheckpoint
```

Przykladowy output:
```
D/DiagnosticCheckpoint: [LinkDetails] Header loaded: title=Przykladowy link, votes=42
D/DiagnosticCheckpoint: [LinkVote] Vote toggled: voted=true, count=43
```

## Przyklad workflow weryfikacji

1. Wyczysc checkpointy:
   ```bash
   curl -X DELETE http://localhost:8899/checkpoints
   ```

2. Wykonaj akcje w aplikacji (np. otworz link details)

3. Sprawdz checkpointy:
   ```bash
   curl http://localhost:8899/checkpoints?tag=LinkDetails
   ```

4. Zweryfikuj ze oczekiwane checkpointy sa obecne z poprawnymi wartosciami

## Limity

- Maksymalnie 200 checkpointow w pamieci (najstarsze usuwane automatycznie)
- Checkpointy dostepne tylko w debug buildzie
- Checkpointy nie przetrwaja restartu aplikacji

## Pliki

| Plik | Opis |
|------|------|
| `app/src/debug/.../debug/DiagnosticCheckpoint.kt` | Singleton - logowanie i storage |
| `app/src/debug/.../debug/DebugHttpServer.kt` | Endpointy `/checkpoints` |
| `docs/DIAGNOSTIC_CHECKPOINTS.md` | Ta dokumentacja |
