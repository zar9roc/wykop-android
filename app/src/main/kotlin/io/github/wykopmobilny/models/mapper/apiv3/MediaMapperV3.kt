package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.media.EmbedResponseV3
import io.github.wykopmobilny.api.responses.v3.media.MediaResponseV3
import io.github.wykopmobilny.api.responses.v3.media.PhotoResponseV3
import io.github.wykopmobilny.kotlin.formatFileSize
import io.github.wykopmobilny.models.dataclass.Embed
import io.github.wykopmobilny.utils.withImageParams

object MediaMapperV3 {
    // Flaga 18+ (adult) jest na poziomie tresci (wpis/komentarz), a NIE na zdjeciu -
    // schemat Photo w API nie ma pola plus18. Dlatego adult musi trafic tu z zewnatrz,
    // inaczej obrazki 18+/nsfw nigdy nie byly zaslaniane (plus18 zawsze false).
    fun map(
        value: MediaResponseV3,
        adult: Boolean,
    ): Embed? {
        val photo = value.photo ?: value.photos?.firstOrNull()
        val embed = value.embed
        return when {
            photo != null -> mapPhoto(photo, adult)
            embed != null -> mapEmbed(embed, adult)
            else -> null
        }
    }

    /**
     * Wszystkie zalaczniki wpisu/komentarza mikrobloga: do 4 zdjec (galeria)
     * i ewentualny embed (YouTube itp.) na koncu. [map] zostaje "glownym" medium
     * dla miejsc, ktore pokazuja tylko jedno (PM, komentarze znalezisk).
     */
    fun mapAttachments(
        value: MediaResponseV3,
        adult: Boolean,
    ): List<Embed> {
        val photos = value.photos?.takeIf { it.isNotEmpty() } ?: listOfNotNull(value.photo)
        return photos.map { mapPhoto(it, adult) } + listOfNotNull(value.embed?.let { mapEmbed(it, adult) })
    }

    private fun mapPhoto(
        photo: PhotoResponseV3,
        adult: Boolean,
    ): Embed =
        Embed(
            type = "image",
            // Wariant w400 z CDN - pelna rozdzielczosc (url) potrafi miec kilkanascie MP
            // i po kilku obrazkach zabija aplikacje OOM-em na prawdziwych urzadzeniach.
            preview = photo.url.withImageParams("w400"),
            url = photo.url,
            plus18 = adult,
            source = photo.source,
            isAnimated = photo.mimeType == "image/gif",
            // Badge gifa pokazuje ROZMIAR PLIKU (nie wymiary) - wczesniej tu byly
            // wymiary "WxH", ktore logika obcinania w WykopEmbedView mieszala.
            size = photo.size?.let(::formatFileSize).orEmpty(),
            label = photo.label?.trim()?.takeIf { it.isNotEmpty() },
        )

    private fun mapEmbed(
        embed: EmbedResponseV3,
        adult: Boolean,
    ): Embed =
        Embed(
            // API v3 zwraca typ per dostawca ("streamable", "youtube", "twitter"...),
            // a WykopEmbedView.handleUrl() routuje tylko "image"/"video" (po domenie URL).
            // Bez normalizacji tap na embed byl no-opem.
            type = "video",
            preview = embed.thumbnail.orEmpty(),
            url = embed.url,
            plus18 = adult,
            source = null,
            isAnimated = false,
            size = "",
        )
}
