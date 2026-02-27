package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.media.EmbedResponseV3
import io.github.wykopmobilny.api.responses.v3.media.MediaResponseV3
import io.github.wykopmobilny.api.responses.v3.media.PhotoResponseV3
import io.github.wykopmobilny.models.dataclass.Embed
import io.github.wykopmobilny.models.mapper.Mapper

object MediaMapperV3 : Mapper<MediaResponseV3, Embed?> {
    override fun map(value: MediaResponseV3): Embed? {
        return when {
            value.photo != null -> mapPhoto(value.photo)
            value.embed != null -> mapEmbed(value.embed)
            else -> null
        }
    }

    private fun mapPhoto(photo: PhotoResponseV3): Embed {
        val size = if (photo.width != null && photo.height != null) {
            "${photo.width}x${photo.height}"
        } else {
            ""
        }
        return Embed(
            type = "image",
            preview = photo.url,
            url = photo.url,
            plus18 = photo.plus18 ?: false,
            source = photo.source,
            isAnimated = photo.mimeType == "image/gif",
            size = size,
        )
    }

    private fun mapEmbed(embed: EmbedResponseV3): Embed {
        return Embed(
            type = embed.type,
            preview = embed.thumbnail.orEmpty(),
            url = embed.url,
            plus18 = false,
            source = null,
            isAnimated = false,
            size = "",
        )
    }
}
