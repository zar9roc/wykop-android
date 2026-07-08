package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item
import io.github.wykopmobilny.kotlin.convertWykopContentToHtml

object LinkCommentMapperV3 {
    fun map(
        value: LinkCommentResponseV3,
        owmContentFilter: OWMContentFilter,
        linkId: Long,
    ) = owmContentFilter.filterLinkComment(
        LinkCommentV3Item(
            response = value,
            linkId = linkId,
            author = AuthorMapperV3.map(value.author),
            embed = value.media?.let { MediaMapperV3.map(it, adult = value.adult ?: false) },
            body = value.content?.convertWykopContentToHtml(),
            isNsfw = value.content?.lowercase()?.contains("#nsfw") ?: false,
            isBlocked = !value.deleted.isNullOrEmpty(),
        ),
    )
}
