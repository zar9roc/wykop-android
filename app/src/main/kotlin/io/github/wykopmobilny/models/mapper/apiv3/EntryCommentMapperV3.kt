package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.v3.entries.EntryCommentResponseV3
import io.github.wykopmobilny.models.dataclass.EntryComment

object EntryCommentMapperV3 {
    fun map(
        value: EntryCommentResponseV3,
        owmContentFilter: OWMContentFilter,
    ) = owmContentFilter.filterEntryComment(
        EntryComment(
            id = value.id,
            entryId = value.parentId ?: 0L,
            author = AuthorMapperV3.map(value.author),
            body = value.content.orEmpty(),
            fullDate = value.createdAt,
            isVoted = (value.voted ?: 0) > 0,
            embed = value.media?.let(MediaMapperV3::map),
            voteCount = value.votes.up - value.votes.down,
            app = value.device,
            violationUrl = null,
            isNsfw = value.content?.lowercase()?.contains("#nsfw") ?: false,
            isBlocked = !value.deleted.isNullOrEmpty(),
            deletedReason = value.deleted,
            slug = value.slug,
        ),
    )
}
