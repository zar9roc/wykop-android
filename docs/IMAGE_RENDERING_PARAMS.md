# Parametry Renderowania Obrazków

## Przegląd

Wykop API umożliwia generowanie obrazków w różnych rozmiarach i jakościach poprzez dodanie parametrów renderowania do URL.
Projekt wykop-android dostarcza elastyczne funkcje do pracy z tymi parametrami.

## Format URL

Parametry renderowania są wstawiane przed rozszerzeniem pliku:

```
[adres_bez_rozszerzenia],[parametry][rozszerzenie]
```

**Przykład:**
- Oryginalny URL: `https://example.com/image.jpg`
- Z parametrami `w220h142`: `https://example.com/image,w220h142.jpg`

## Dostępne Parametry

Backend Wykop obsługuje różne parametry renderowania. W aplikacji używamy następujących:

| Parametr | Zastosowanie | Opis |
|----------|--------------|------|
| `w220h142` | Miniatury list | Używane na Stronie Głównej, Wykopalisko, Hity |
| `w400` | Obrazki w treści | Używane w widoku Mikroblogu |
| `q80` | Awatary użytkowników | Kompresja jakości dla awatarów |

**Ważne:** Parametry nie są dynamiczne - różne obrazki na backendzie mają różne dostępne rendery.
Jeśli podany parametr nie jest dostępny, backend zwraca obraz w najwyższej jakości.

## API

### 1. String.withImageParams(params: String?)

Rozszerzenie String dodające parametry renderowania do URL obrazka.

```kotlin
fun String.withImageParams(params: String?): String
```

**Przykłady:**
```kotlin
val url = "https://wykop.pl/cdn/image.jpg"

// Miniatura dla list
val thumbnail = url.withImageParams("w220h142")
// -> "https://wykop.pl/cdn/image,w220h142.jpg"

// Obrazek dla mikroblogu
val microblogImage = url.withImageParams("w400")
// -> "https://wykop.pl/cdn/image,w400.jpg"

// Awatar użytkownika
val avatar = url.withImageParams("q80")
// -> "https://wykop.pl/cdn/image,q80.jpg"

// Bez parametrów (najwyższa jakość)
val fullSize = url.withImageParams(null)
// -> "https://wykop.pl/cdn/image.jpg"
```

### 2. ImageView.loadImage(url: String, renderParams: String?, signature: Int?)

Rozszerzenie ImageView do ładowania obrazków z opcjonalnymi parametrami renderowania.

```kotlin
fun ImageView.loadImage(
    url: String,
    renderParams: String? = null,
    signature: Int? = null
)
```

**Parametry:**
- `url` - URL obrazka do załadowania
- `renderParams` - Opcjonalne parametry renderowania (np. "w220h142", "w400", "q80")
- `signature` - Opcjonalna sygnatura dla cache invalidation

**Przykłady:**
```kotlin
// Pełny rozmiar (najwyższa jakość)
imageView.loadImage(imageUrl)

// Miniatura dla strony głównej
imageView.loadImage(imageUrl, renderParams = "w220h142")

// Obrazek dla mikroblogu
imageView.loadImage(imageUrl, renderParams = "w400")

// Awatar użytkownika
avatarImageView.loadImage(avatarUrl, renderParams = "q80")

// Z cache signature
imageView.loadImage(imageUrl, renderParams = "w220h142", signature = userId)
```

## Backward Compatibility

Stare funkcje zostały zachowane dla kompatybilności wstecznej, ale są oznaczone jako `@Deprecated`:

### String.toThumbnailUrl()

```kotlin
@Deprecated("Użyj withImageParams(\"w220h142\") dla większej elastyczności")
fun String.toThumbnailUrl(): String
```

**Implementacja:**
```kotlin
fun String.toThumbnailUrl(): String = withImageParams("w220h142")
```

### ImageView.loadImageThumbnail(url: String, signature: Int?)

```kotlin
@Deprecated("Użyj loadImage(url, renderParams = \"w220h142\") dla większej elastyczności")
fun ImageView.loadImageThumbnail(url: String, signature: Int? = null)
```

**Implementacja:**
```kotlin
fun ImageView.loadImageThumbnail(url: String, signature: Int? = null) {
    loadImage(url, renderParams = "w220h142", signature = signature)
}
```

## Lokalizacja Kodu

Wszystkie funkcje znajdują się w:
```
app/src/main/kotlin/io/github/wykopmobilny/utils/Extensions.kt
```

## Przypadki Użycia

### 1. Listy (Strona Główna, Wykopalisko, Hity)

```kotlin
// W adapterze RecyclerView
holder.imageView.loadImage(
    url = entry.media?.photo?.url.orEmpty(),
    renderParams = "w220h142"
)
```

### 2. Widok Mikroblogu

```kotlin
// W detalu mikroblogu
imageView.loadImage(
    url = entry.media?.photo?.url.orEmpty(),
    renderParams = "w400"
)
```

### 3. Awatary Użytkowników

```kotlin
// W komponencie AvatarView
avatarImageView.loadImage(
    url = user.avatar.url,
    renderParams = "q80"
)
```

### 4. Galeria/Pełny Widok

```kotlin
// W PhotoViewActivity (pełny rozmiar)
imageView.loadImage(
    url = photoUrl
    // renderParams = null jest domyślne
)
```

## Migracja z Starych Funkcji

### Przed:
```kotlin
// Stara metoda
imageView.loadImageThumbnail(url)
val thumbnailUrl = originalUrl.toThumbnailUrl()
```

### Po:
```kotlin
// Nowa metoda - bardziej elastyczna
imageView.loadImage(url, renderParams = "w220h142")
val thumbnailUrl = originalUrl.withImageParams("w220h142")

// Dla innych przypadków:
imageView.loadImage(url, renderParams = "w400")  // Mikroblog
imageView.loadImage(url, renderParams = "q80")   // Awatar
imageView.loadImage(url)                         // Pełny rozmiar
```

## Uwagi Techniczne

1. **Walidacja URL:** Funkcja `withImageParams()` sprawdza czy URL ma rozszerzenie (`.jpg`, `.png`, etc.).
   Jeśli nie ma, zwraca oryginalny URL bez modyfikacji.

2. **Parametry null/blank:** Jeśli `params` jest null lub pusty, funkcja zwraca oryginalny URL.

3. **Backend:** Parametry są interpretowane przez backend Wykop. Jeśli podany parametr nie jest dostępny
   dla danego obrazka, backend zwraca obraz w najwyższej dostępnej jakości.

4. **Glide:** Wszystkie funkcje używają biblioteki Glide do ładowania obrazków.

5. **Cache:** Parametr `signature` pozwala na kontrolę cache'owania przez Glide (przydatne dla awatarów
   użytkowników, które mogą się zmieniać).

## Roadmap

- [ ] Dodać stałe dla popularnych parametrów (np. `object ImageParams { const val THUMBNAIL = "w220h142" }`)
- [ ] Rozważyć typesafe DSL dla parametrów
- [ ] Dodać metryki użycia różnych parametrów dla optymalizacji
