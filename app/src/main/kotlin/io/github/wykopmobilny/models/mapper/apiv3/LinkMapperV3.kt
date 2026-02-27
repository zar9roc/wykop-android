package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.utils.api.stripImageCompression
import io.github.wykopmobilny.utils.textview.removeHtml

fun LinkResponseV3.filterLinkV3(owmContentFilter: OWMContentFilter) =
    owmContentFilter.filterLink(
        Link(
            id = id,
            title = title?.removeHtml().orEmpty(),
            description = description?.removeHtml().orEmpty(),
            tags = tags?.joinToString(" ") { "#$it" }.orEmpty(),
            sourceUrl = url ?: sourceUrl.orEmpty(),
            voteCount = votes.up - votes.down,
            buryCount = votes.down,
            comments = mutableListOf(),
            commentsCount = comments.count,
            relatedCount = 0,
            author = AuthorMapperV3.map(author),
            fullDate = createdAt,
            previewImage = media?.photo?.url,
            fullImage = media?.photo?.url?.stripImageCompression(),
            plus18 = adult ?: false,
            canVote = true,
            isHot = hot ?: false,
            userVote = when (voted) {
                1 -> "dig"
                -1 -> "bury"
                else -> null
            },
            userFavorite = favourite ?: false,
            app = null,
            violationUrl = null,
            gotSelected = false,
            isBlocked = deleted ?: false,
        ),
    )

fun List<LinkResponseV3>.filterLinksV3(owmContentFilter: OWMContentFilter) =
    FilteredData(
        totalCount = size,
        filtered = map { response -> response.filterLinkV3(owmContentFilter) },
    )
