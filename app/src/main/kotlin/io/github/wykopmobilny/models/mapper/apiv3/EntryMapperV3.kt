package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.v3.common.PaginationResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.kotlin.convertWykopContentToHtml

fun EntryResponseV3.filterEntryV3(owmContentFilter: OWMContentFilter) =
    owmContentFilter.filterEntry(
        Entry(
            id = id,
            author = AuthorMapperV3.map(author),
            body = content.orEmpty().convertWykopContentToHtml(),
            fullDate = createdAt,
            isVoted = (voted ?: 0) > 0,
            isFavorite = favourite ?: false,
            survey = survey?.let(SurveyMapperV3::map),
            embed = media?.let { MediaMapperV3.map(it, adult = adult ?: false) },
            voteCount = votes.up - votes.down,
            commentsCount = comments.count,
            comments =
                comments.items
                    .orEmpty()
                    .map { EntryCommentMapperV3.map(it, owmContentFilter, entryId = id) }
                    .toMutableList(),
            app = device,
            violationUrl = null,
            isNsfw = content?.lowercase()?.contains("#nsfw") == true,
            isBlocked = deleted ?: false,
            collapsed = true,
            isCommentingPossible = true,
        ),
    )

fun List<EntryResponseV3>.filterEntriesV3(
    owmContentFilter: OWMContentFilter,
    pagination: PaginationResponseV3? = null,
) = FilteredData(
    totalCount = size,
    filtered = map { response -> response.filterEntryV3(owmContentFilter) },
    nextPage = pagination?.next,
)
