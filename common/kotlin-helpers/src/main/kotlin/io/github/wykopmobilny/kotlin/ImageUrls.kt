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
