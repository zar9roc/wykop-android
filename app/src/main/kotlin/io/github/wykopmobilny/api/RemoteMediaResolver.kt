package io.github.wykopmobilny.api

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.MediaV3RetrofitApi
import io.github.wykopmobilny.api.exceptions.handleMediaUpload
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.media.CreateEmbedRequestV3
import io.github.wykopmobilny.api.requests.v3.media.UploadPhotoByUrlRequestV3

/**
 * Wspolna obsluga zdalnych zalacznikow wpisu/komentarza/wiadomosci.
 *
 * Zrodla: klucz zdjecia z uploadu pliku, URL z historycznego slotu zdjecia
 * (moze byc obrazkiem albo linkiem medialnym) i URL ze slotu embed. Obrazki ida
 * przez POST /media/photos (pole "photo"), linki do wspieranych serwisow
 * (YouTube, streamable...) przez POST /media/embed (pole "embed").
 */
internal data class RemoteMediaKeys(
    val photoKey: String? = null,
    val embedKey: String? = null,
)

// Konca sciezki wystarczaja do rozpoznania bezposredniego obrazka; wszystko inne
// probujemy najpierw jako embed (serwer sam wie, ktore serwisy wspiera).
private val directImageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp")

internal fun String.looksLikeDirectImageUrl(): Boolean {
    val path = substringBefore('?').substringBefore('#').lowercase()
    return directImageExtensions.any { path.endsWith(it) }
}

/**
 * Rozstrzyga wszystkie zrodla naraz i pilnuje, zeby kazdy klucz trafil we wlasciwe
 * pole requestu. Klucz z uploadu pliku ma pierwszenstwo w polu "photo".
 */
internal suspend fun MediaV3RetrofitApi.resolveAttachments(
    photoKey: String? = null,
    photoUrl: String? = null,
    embedUrl: String? = null,
    type: String = "comments",
): RemoteMediaKeys {
    val photoSlot = resolveSingleUrl(photoUrl, type)
    val embedSlot = resolveSingleUrl(embedUrl, type)
    return RemoteMediaKeys(
        photoKey = photoKey ?: photoSlot.photoKey ?: embedSlot.photoKey,
        embedKey = embedSlot.embedKey ?: photoSlot.embedKey,
    )
}

/**
 * Pojedynczy URL: obrazek -> klucz photo, inne -> klucz embed. Gdy /media/embed
 * odrzuci adres (np. obrazek bez rozszerzenia w sciezce), probuje jeszcze
 * /media/photos. Pusty/null URL -> puste klucze.
 */
private suspend fun MediaV3RetrofitApi.resolveSingleUrl(
    url: String?,
    type: String,
): RemoteMediaKeys {
    val mediaUrl = url?.takeIf { it.isNotBlank() } ?: return RemoteMediaKeys()
    if (!mediaUrl.looksLikeDirectImageUrl()) {
        val embedKey =
            runCatching {
                handleMediaUpload { createEmbed(WykopApiRequestV3(CreateEmbedRequestV3(url = mediaUrl))) }.key
            }.onFailure { Napier.i("media/embed odrzucil $mediaUrl, probuje jako zdjecie", it) }
                .getOrNull()
        if (embedKey != null) return RemoteMediaKeys(embedKey = embedKey)
    }
    val photoKey =
        handleMediaUpload {
            uploadPhotoByUrl(WykopApiRequestV3(UploadPhotoByUrlRequestV3(url = mediaUrl)), type = type)
        }.key
    return RemoteMediaKeys(photoKey = photoKey)
}
