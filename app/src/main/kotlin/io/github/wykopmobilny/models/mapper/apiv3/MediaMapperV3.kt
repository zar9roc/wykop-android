package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.media.EmbedResponseV3
import io.github.wykopmobilny.api.responses.v3.media.MediaResponseV3
import io.github.wykopmobilny.api.responses.v3.media.PhotoResponseV3
import io.github.wykopmobilny.models.dataclass.Embed
import io.github.wykopmobilny.utils.withImageParams

private const val BYTES_IN_KILOBYTE = 1024L
private const val BYTES_IN_MEGABYTE = 1024L * 1024L
private const val HALF_KB = 512L

object MediaMapperV3 {
    // Flaga 18+ (adult) jest na poziomie tresci (wpis/komentarz), a NIE na zdjeciu -
    // schemat Photo w API nie ma pola plus18. Dlatego adult musi trafic tu z zewnatrz,
    // inaczej obrazki 18+/nsfw nigdy nie byly zaslaniane (plus18 zawsze false).
    fun map(
        value: MediaResponseV3,
        adult: Boolean,
    ): Embed? {
        val photo = value.photo
        val embed = value.embed
        return when {
            photo != null -> mapPhoto(photo, adult)
            embed != null -> mapEmbed(embed, adult)
            else -> null
        }
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
        )

    private fun formatFileSize(bytes: Long): String =
        if (bytes < BYTES_IN_MEGABYTE) {
            "${(bytes + HALF_KB) / BYTES_IN_KILOBYTE} KB"
        } else {
            String.format(java.util.Locale.getDefault(), "%.1f MB", bytes.toDouble() / BYTES_IN_MEGABYTE)
        }

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
