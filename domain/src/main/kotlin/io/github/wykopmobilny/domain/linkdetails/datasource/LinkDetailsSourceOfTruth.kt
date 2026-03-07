package io.github.wykopmobilny.domain.linkdetails.datasource

import com.dropbox.android.external.store4.SourceOfTruth
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.data.cache.api.LinkEntity
import io.github.wykopmobilny.domain.profile.LinkInfo
import io.github.wykopmobilny.domain.profile.UserInfo
import io.github.wykopmobilny.domain.profile.datasource.asUserVote
import io.github.wykopmobilny.domain.profile.datasource.upsertV3
import io.github.wykopmobilny.domain.profile.toColorDomain
import io.github.wykopmobilny.domain.profile.toGenderDomain
import io.github.wykopmobilny.kotlin.AppDispatchers
import kotlinx.coroutines.flow.map

internal fun linkDetailsSourceOfTruth(cache: AppCache) =
    SourceOfTruth.of<Long, LinkResponseV3, LinkInfo>(
        reader = { linkId ->
            cache.linksQueries
                .selectById(id = linkId)
                .asFlow()
                .mapToOneOrNull(AppDispatchers.IO)
                .map { link ->
                    link ?: return@map null
                    LinkInfo(
                        id = link.id,
                        title = link.title,
                        isHot = link.isHot,
                        description = link.description,
                        tags = link.tags.split(" ").map { it.removePrefix("#") },
                        sourceUrl = link.sourceUrl,
                        previewImageUrl = link.previewImageUrl,
                        fullImageUrl = link.fullImageUrl,
                        author =
                            UserInfo(
                                profileId = link.profileId,
                                avatarUrl = link.avatar,
                                rank = link.rank,
                                gender = link.gender?.toGenderDomain(),
                                color = link.color.toColorDomain(),
                            ),
                        commentsCount = link.commentsCount,
                        voteCount = link.voteCount,
                        buryCount = link.buryCount,
                        relatedCount = link.relatedCount,
                        postedAt = link.postedAt,
                        app = link.app,
                        userAction = link.userVote,
                        userFavorite = link.userFavorite,
                    )
                }
        },
        writer = { _, link ->
            cache.transaction {
                cache.profileQueries.upsertV3(link.author)
                val photoUrl = link.media?.photo?.url?.stripImageCompression()
                cache.linksQueries.insertOrReplace(
                    LinkEntity(
                        id = link.id,
                        title = link.title.orEmpty(),
                        description = link.description.orEmpty(),
                        tags = link.tags?.joinToString(" ") { "#$it" }.orEmpty(),
                        sourceUrl = link.sourceUrl.orEmpty(),
                        previewImageUrl = photoUrl,
                        fullImageUrl = photoUrl,
                        voteCount = link.votes.up,
                        buryCount = link.votes.down,
                        commentsCount = link.comments.count,
                        relatedCount = 0,
                        postedAt = link.createdAt,
                        plus18 = link.adult ?: false,
                        canVote = true,
                        isHot = link.hot ?: false,
                        userVote = link.voted.asUserVote(),
                        userFavorite = link.favourite ?: false,
                        userObserve = false,
                        app = null,
                        profileId = link.author.username,
                    ),
                )
            }
        },
        delete = { linkId -> cache.linksQueries.deleteById(linkId) },
    )

internal fun String.stripImageCompression(): String {
    val extension = substringAfterLast(".")
    val baseUrl = substringBeforeLast(",")
    return baseUrl + if (!baseUrl.endsWith(extension)) ".$extension" else ""
}
