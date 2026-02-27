# Migracja Glide: KAPT → KSP

## Podsumowanie

Projekt został zmigrowany z procesora adnotacji KAPT (`glide-compiler`) na KSP (`glide-ksp`)
w ramach szerszej migracji z KAPT na KSP (Kotlin Symbol Processing).

## Kluczowe ograniczenie: GlideApp nie jest generowany przez KSP

**Przy użyciu Glide z KSP (zamiast KAPT) klasa `GlideApp` nie jest generowana.**
To jest oficjalne ograniczenie procesora KSP Glide, nie bug.

### Co to oznacza w praktyce

| Przed (KAPT)                        | Po (KSP)                           |
|--------------------------------------|-------------------------------------|
| `GlideApp.with(context).load(url)`   | `Glide.with(context).load(url)`     |
| `import ...GlideApp`                 | `import com.bumptech.glide.Glide`   |
| `GlideRequests`                      | `RequestManager`                    |

### Zasady dla developerów

1. **Zawsze używaj `Glide.with()`** zamiast `GlideApp.with()`.
2. **Import**: `com.bumptech.glide.Glide` zamiast wygenerowanego `GlideApp`.
3. Jeśli wcześniej korzystałeś z `GlideRequests`, użyj standardowego `RequestManager`.
4. Klasa `AppGlideModule` z adnotacją `@GlideModule` nadal działa poprawnie z KSP.

## Zmienione pliki

### Build configuration

- **`gradle/libs.versions.toml`** - dodano `glide-ksp`:
  ```toml
  glide-ksp = { module = "com.github.bumptech.glide:ksp", version.ref = "mavencentral-glide" }
  ```

- **`app/build.gradle`** - zmiana z `kapt` na `ksp`:
  ```groovy
  # Przed:
  kapt(libs.glide.compiler)

  # Po:
  ksp(libs.glide.ksp)
  ```

- **`common/screenshot-test-helpers/build.gradle`** - analogiczna zmiana.

### Kod źródłowy

Wszystkie odwołania do `GlideApp` zostały zamienione na `Glide`:

- `app/.../utils/Extensions.kt` - `loadImage()` extension function
- `app/.../ui/widgets/FloatingImageView.kt`
- `app/.../ui/adapters/viewholders/PMMessageViewHolder.kt`
- `app/.../ui/modules/photoview/PhotoViewActivity.kt`

### Pliki bez zmian

- `app/.../glide/GlideModule.kt` - `@GlideModule` z `AppGlideModule` działa z KSP bez zmian.
- `app/.../utils/KotlinGlideRequestListener.kt` - używa standardowych klas Glide.

## Wersja Glide

Aktualna wersja: **5.0.5** (zdefiniowana jako `mavencentral-glide` w `libs.versions.toml`).

### Zmiany w wersji 5.0.5 (od 4.15.1)

- Wymaga Java 8 i Kotlin 1.8 (projekt używa Kotlin 2.2.20 - kompatybilne)
- Dodano adnotacje `@Nullable` i `@NonNull` do `RequestListener` (kod projektu już używa nullable types - kompatybilne)
- Fix dla GlideUrl z adresami IPv6
- Brak breaking changes wymagających modyfikacji kodu

## Referencje

- [Glide KSP documentation](https://bumptech.github.io/glide/doc/download-setup.html#ksp)
- [Glide Generated API note](https://bumptech.github.io/glide/doc/generatedapi.html) - wyjaśnia dlaczego GlideApp nie jest generowany z KSP.
