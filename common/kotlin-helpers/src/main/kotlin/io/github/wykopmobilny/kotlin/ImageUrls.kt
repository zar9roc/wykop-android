package io.github.wykopmobilny.kotlin

/**
 * Dodaje parametry renderowania CDN do URL obrazka.
 * Format: [adres bez rozszerzenia],[parametry][rozszerzenie]
 *
 * Przyklad: "https://example.com/image.jpg".withImageParams("w400") -> "https://example.com/image,w400.jpg"
 *
 * @param params Parametry renderowania (np. "w220h142", "w400", "q80") lub null dla oryginalnego URL
 * @return URL z dodanymi parametrami lub oryginalny URL jesli params jest null lub brak rozszerzenia
 */
fun String.withImageParams(params: String?): String {
    if (params.isNullOrBlank()) return this

    val lastDotIndex = lastIndexOf('.')
    return if (lastDotIndex > 0) {
        "${substring(0, lastDotIndex)},$params${substring(lastDotIndex)}"
    } else {
        this
    }
}

private const val BYTES_IN_KILOBYTE = 1024L
private const val BYTES_IN_MEGABYTE = 1024L * 1024L
private const val HALF_KB = 512L

/**
 * Czytelny rozmiar pliku do znacznika GIF-a w rogu miniatury ("390 KB", "2.4 MB").
 * Wspolny dla wpisow (MediaMapperV3) i komentarzy znalezisk - badge ma wygladac tak samo.
 */
fun formatFileSize(bytes: Long): String =
    if (bytes < BYTES_IN_MEGABYTE) {
        "${(bytes + HALF_KB) / BYTES_IN_KILOBYTE} KB"
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f MB", bytes.toDouble() / BYTES_IN_MEGABYTE)
    }
