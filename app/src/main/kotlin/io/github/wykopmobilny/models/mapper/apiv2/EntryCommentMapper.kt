package io.github.wykopmobilny.models.mapper.apiv2

import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.EntryCommentResponse
import io.github.wykopmobilny.models.dataclass.EntryComment
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

object EntryCommentMapper {
    private val apiTimeZone = TimeZone.of("Europe/Warsaw")

    fun map(
        value: EntryCommentResponse,
        owmContentFilter: OWMContentFilter,
    ) = owmContentFilter.filterEntryComment(
        EntryComment(
            id = value.id,
            entryId = value.entryId ?: 0,
            author = AuthorMapper.map(value.author),
            body = value.body.orEmpty(),
            fullDate = LocalDateTime.parse(value.date.replace(' ', 'T')).toInstant(apiTimeZone),
            isVoted = value.userVote > 0,
            embed = value.embed?.let(EmbedMapper::map),
            voteCount = value.voteCount,
            app = value.app,
            violationUrl = value.violationUrl,
            isNsfw = value.body?.lowercase()?.contains("#nsfw") ?: false,
            isBlocked = value.blocked,
        ),
    )
}
