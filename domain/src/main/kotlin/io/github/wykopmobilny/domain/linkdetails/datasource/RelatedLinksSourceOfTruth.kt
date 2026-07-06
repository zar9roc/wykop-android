package io.github.wykopmobilny.domain.linkdetails.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.wykopmobilny.api.responses.v3.links.RelatedResponseV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.domain.di.flowSourceOfTruth
import io.github.wykopmobilny.data.cache.api.RelatedLinkEntity
import io.github.wykopmobilny.data.cache.api.linksRelated.SelectByLinkId
import io.github.wykopmobilny.domain.linkdetails.RelatedLink
import io.github.wykopmobilny.domain.profile.UserInfo
import io.github.wykopmobilny.domain.profile.datasource.asUserVote
import io.github.wykopmobilny.domain.profile.datasource.upsertV3
import io.github.wykopmobilny.domain.profile.toColorDomain
import io.github.wykopmobilny.domain.profile.toGenderDomain
import io.github.wykopmobilny.kotlin.AppDispatchers
import io.github.wykopmobilny.kotlin.withImageParams
import kotlinx.coroutines.flow.map

internal fun relatedLinksSourceOfTruth(cache: AppCache) =
    flowSourceOfTruth<Long, List<RelatedResponseV3>, List<RelatedLink>>(
        reader = { linkId ->
            cache.linksRelatedQueries
                .selectByLinkId(linkId = linkId)
                .asFlow()
                .mapToList(AppDispatchers.IO)
                .map { relatedLinks -> relatedLinks.map(SelectByLinkId::toDomain) }
        },
        writer = { linkId, links ->
            cache.transaction {
                links.forEachIndexed { index, link ->
                    cache.profileQueries.upsertV3(link.author)
                    cache.linksRelatedQueries.insertOrReplace(link.toEntity(orderOnPage = index, linkId = linkId))
                }
            }
        },
        delete = { linkId -> cache.linksRelatedQueries.deleteByLinkId(linkId) },
    )

private fun RelatedResponseV3.toEntity(
    orderOnPage: Int,
    linkId: Long,
) = RelatedLinkEntity(
    id = id,
    userVote = voted.asUserVote(),
    voteCount = votes.up,
    // Realny adres przychodzi w source.url; plaskie url to martwe pole.
    url = source?.url ?: url.orEmpty(),
    previewImageUrl = media?.photo?.url?.withImageParams("w400"),
    profileId = author.username,
    title = title.orEmpty(),
    linkId = linkId,
    orderOnPage = orderOnPage,
)

private fun SelectByLinkId.toDomain() =
    RelatedLink(
        id = id,
        url = url,
        previewImageUrl = previewImageUrl,
        voteCount = voteCount,
        author =
            profileId?.let {
                UserInfo(
                    profileId = it,
                    avatarUrl = avatar!!,
                    rank = rank,
                    gender = gender?.toGenderDomain(),
                    color = color!!.toColorDomain(),
                )
            },
        title = title,
        userVote = userVote,
    )
