package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.models.dataclass.LinkComment
import io.github.wykopmobilny.utils.textview.convertWykopContentToHtml

object LinkCommentMapperV3 {
    fun map(
        value: LinkCommentResponseV3,
        owmContentFilter: OWMContentFilter,
        linkId: Long,
    ) = owmContentFilter.filterLinkComment(
        LinkComment(
            id = value.id,
            author = AuthorMapperV3.map(value.author),
            fullDate = value.createdAt,
            body = value.content?.convertWykopContentToHtml(),
            favorite = false,
            voteCount = value.votes.up - value.votes.down,
            voteCountPlus = value.votes.up,
            voteCountMinus = value.votes.down,
            userVote = value.voted ?: 0,
            parentId = value.parentId,
            canVote = true,
            linkId = linkId,
            embed = value.media?.let(MediaMapperV3::map),
            app = value.device,
            isCollapsed = false,
            isParentCollapsed = false,
            childCommentCount = 0,
            violationUrl = null,
            isNsfw = value.content?.lowercase()?.contains("#nsfw") ?: false,
            isBlocked = !value.deleted.isNullOrEmpty(),
            deletedReason = value.deleted,
            slug = value.slug,
        ),
    )
}
