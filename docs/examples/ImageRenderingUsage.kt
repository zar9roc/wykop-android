package io.github.wykopmobilny.docs.examples

import android.widget.ImageView
import io.github.wykopmobilny.utils.loadImage
import io.github.wykopmobilny.utils.withImageParams

/**
 * Przykłady użycia funkcji do pobierania obrazków z parametrami renderowania.
 *
 * UWAGA: To jest plik dokumentacyjny, nie część produkcyjnego kodu.
 */
@Suppress("unused", "UNUSED_PARAMETER")
object ImageRenderingUsage {
    /**
     * Przykład 1: Ładowanie miniatur na stronie głównej
     */
    fun loadThumbnailsInMainList(imageView: ImageView, imageUrl: String) {
        // Sposób 1: Bezpośrednie użycie loadImage z parametrem
        imageView.loadImage(
            url = imageUrl,
            renderParams = "w220h142" // Format miniatur dla list
        )

        // Sposób 2: Transformacja URL przed załadowaniem
        val thumbnailUrl = imageUrl.withImageParams("w220h142")
        imageView.loadImage(thumbnailUrl)
    }

    /**
     * Przykład 2: Ładowanie obrazków w mikroblogu
     */
    fun loadMicroblogImages(imageView: ImageView, imageUrl: String) {
        imageView.loadImage(
            url = imageUrl,
            renderParams = "w400" // Format dla mikroblogu
        )
    }

    /**
     * Przykład 3: Ładowanie awatarów użytkowników
     */
    fun loadUserAvatar(avatarView: ImageView, avatarUrl: String, userId: Int) {
        avatarView.loadImage(
            url = avatarUrl,
            renderParams = "q80", // Kompresja jakości dla awatarów
            signature = userId // Cache signature - inwalidacja gdy user zmieni awatar
        )
    }

    /**
     * Przykład 4: Ładowanie pełnowymiarowych obrazków (galeria, PhotoView)
     */
    fun loadFullSizeImage(imageView: ImageView, imageUrl: String) {
        // Brak parametru renderParams = najwyższa jakość
        imageView.loadImage(url = imageUrl)

        // Lub explicite null
        imageView.loadImage(
            url = imageUrl,
            renderParams = null // Explicite najwyższa jakość
        )
    }

    /**
     * Przykład 5: Dynamiczny wybór parametrów w zależności od kontekstu
     */
    fun loadImageDynamically(
        imageView: ImageView,
        imageUrl: String,
        isListView: Boolean,
        isMicroblogView: Boolean
    ) {
        val renderParams = when {
            isListView -> "w220h142" // Miniatury dla list
            isMicroblogView -> "w400" // Obrazki dla mikroblogu
            else -> null // Pełny rozmiar dla innych widoków
        }

        imageView.loadImage(
            url = imageUrl,
            renderParams = renderParams
        )
    }

    /**
     * Przykład 6: Transformacja wielu URL-i naraz
     */
    fun transformMultipleUrls(urls: List<String>): List<String> {
        // Przekształcenie listy URL-i na miniatury
        return urls.map { it.withImageParams("w220h142") }
    }

    /**
     * Przykład 7: Sprawdzanie czy URL ma parametry
     */
    fun buildImageUrl(baseUrl: String, needsThumbnail: Boolean): String {
        return if (needsThumbnail) {
            baseUrl.withImageParams("w220h142")
        } else {
            baseUrl
        }
    }

    /**
     * Przykład 8: Adapter RecyclerView - lista wpisów
     */
    class EntryAdapter {
        fun onBindViewHolder(holder: ViewHolder, entry: Entry) {
            // Załaduj miniaturę dla listy
            holder.imageView.loadImage(
                url = entry.imageUrl,
                renderParams = "w220h142"
            )
        }

        // Placeholder klasy
        class ViewHolder(val imageView: ImageView)
        data class Entry(val imageUrl: String)
    }

    /**
     * Przykład 9: Fragment - szczegóły wpisu
     */
    class EntryDetailsFragment {
        fun loadEntryImage(imageView: ImageView, imageUrl: String) {
            // W szczegółach wpisu używamy większych obrazków
            imageView.loadImage(
                url = imageUrl,
                renderParams = "w400"
            )
        }
    }

    /**
     * Przykład 10: Custom View - Avatar Component
     */
    class AvatarComponent {
        fun loadAvatar(
            avatarImageView: ImageView,
            avatarUrl: String?,
            userId: Int
        ) {
            if (avatarUrl.isNullOrBlank()) {
                // Załaduj domyślny awatar
                avatarImageView.setImageResource(android.R.drawable.ic_menu_myplaces)
            } else {
                avatarImageView.loadImage(
                    url = avatarUrl,
                    renderParams = "q80",
                    signature = userId
                )
            }
        }
    }

    /**
     * Przykład 11: Backward compatibility - migracja ze starych funkcji
     */
    fun migrateFromOldApi(imageView: ImageView, imageUrl: String) {
        // STARA METODA (deprecated):
        // imageView.loadImageThumbnail(imageUrl)
        // val thumbnailUrl = imageUrl.toThumbnailUrl()

        // NOWA METODA (preferowana):
        imageView.loadImage(imageUrl, renderParams = "w220h142")
        val thumbnailUrl = imageUrl.withImageParams("w220h142")

        // Zalety nowej metody:
        // 1. Elastyczność - można używać różnych parametrów
        // 2. Czytelność - explicit co się dzieje
        // 3. Reusability - jedna funkcja dla wszystkich przypadków
    }

    /**
     * Przykład 12: Obsługa edge cases
     */
    fun handleEdgeCases(imageView: ImageView) {
        // Case 1: URL bez rozszerzenia
        val urlWithoutExtension = "https://example.com/image"
        imageView.loadImage(
            url = urlWithoutExtension,
            renderParams = "w220h142"
            // Funkcja zwróci oryginalny URL bez modyfikacji
        )

        // Case 2: Pusty string jako parametr
        val emptyParams = ""
        imageView.loadImage(
            url = "https://example.com/image.jpg",
            renderParams = emptyParams
            // Funkcja zwróci oryginalny URL (traktuje empty jak null)
        )

        // Case 3: Niepoprawny parametr (backend decyduje)
        imageView.loadImage(
            url = "https://example.com/image.jpg",
            renderParams = "w999h999"
            // Backend zwróci obraz w najwyższej dostępnej jakości
            // jeśli ten format nie istnieje
        )
    }

    /**
     * Przykład 13: Wzorzec Builder dla skomplikowanych przypadków
     */
    fun complexImageLoading(imageView: ImageView, imageUrl: String) {
        // Dla bardziej skomplikowanych przypadków można
        // używać bezpośrednio Glide z dodatkowymi opcjami:
        //
        // Glide.with(imageView)
        //     .load(imageUrl.withImageParams("w220h142"))
        //     .placeholder(R.drawable.placeholder)
        //     .error(R.drawable.error)
        //     .diskCacheStrategy(DiskCacheStrategy.ALL)
        //     .into(imageView)
    }
}
