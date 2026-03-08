package io.github.wykopmobilny.domain.profile.datasource

import io.github.wykopmobilny.domain.di.flowSourceOfTruth
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.wykopmobilny.api.responses.AuthorResponse
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.data.cache.api.Embed
import io.github.wykopmobilny.data.cache.api.EmbedType
import io.github.wykopmobilny.data.cache.api.EntryEntity
import io.github.wykopmobilny.data.cache.api.ProfileActionsEntity
import io.github.wykopmobilny.data.cache.api.ProfileQueries
import io.github.wykopmobilny.data.cache.api.UserVote
import io.github.wykopmobilny.domain.profile.EntryInfo
import io.github.wykopmobilny.domain.profile.LinkInfo
import io.github.wykopmobilny.domain.profile.ProfileAction
import io.github.wykopmobilny.domain.profile.UserInfo
import io.github.wykopmobilny.domain.profile.di.toColorEntity
import io.github.wykopmobilny.domain.profile.di.toColorEntityFromName
import io.github.wykopmobilny.domain.profile.di.toGenderEntity
import io.github.wykopmobilny.domain.profile.toColorDomain
import io.github.wykopmobilny.domain.profile.toGenderDomain
import io.github.wykopmobilny.kotlin.AppDispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal fun profileSourceOfTruth(
    profileId: String,
    cache: AppCache,
) = flowSourceOfTruth<Int, List<EntryResponseV3>, List<ProfileAction>>(
    reader = { page ->
        val linksStream =
            cache.profileActionsQueries
                .selectLinksPage(profileId = profileId, page = page)
                .asFlow()
                .mapToList(AppDispatchers.IO)
                .map { links ->
                    links.map { link ->
                        val entry =
                            LinkInfo(
                                id = link.id,
                                title = link.title,
                                isHot = link.isHot,
                                description = link.description,
                                tags = link.tags.split(" "),
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
                        entry to link.orderOnPage
                    }
                }

        val entriesStream =
            cache.profileActionsQueries
                .selectEntriesPage(profileId = profileId, page = page)
                .asFlow()
                .mapToList(AppDispatchers.IO)
                .map { entities ->
                    entities.map { action ->
                        val entry =
                            EntryInfo(
                                id = action.id,
                                postedAt = action.postedAt,
                                body = action.body,
                                voteCount = action.voteCount,
                                previewImageUrl = action.preview,
                                commentsCount = action.commentsCount,
                                author =
                                    UserInfo(
                                        profileId = action.profileId,
                                        avatarUrl = action.avatar,
                                        rank = action.rank,
                                        gender = action.gender?.toGenderDomain(),
                                        color = action.color.toColorDomain(),
                                    ),
                                app = action.app,
                                userAction = action.userVote,
                                isFavorite = action.isFavorite,
                            )
                        entry to action.orderOnPage
                    }
                }
        combine(
            linksStream,
            entriesStream,
        ) { links, entries ->
            val all = links + entries
            all.sortedBy { it.second }.map { it.first }
        }
    },
    writer = { page, pageData ->
        cache.profileActionsQueries.transaction {
            cache.profileActionsQueries.deletePage(profileId, page)
            pageData.forEachIndexed { index, entry ->
                cache.profileQueries.upsertV3(entry.author)
                entry.media?.photo?.let { photo ->
                    cache.embedQueries.insertOrReplace(
                        Embed(
                            id = photo.url,
                            type = EmbedType.StaticImage,
                            fileName = photo.source,
                            preview = photo.url,
                            size = null,
                            hasAdultContent = photo.plus18 ?: false,
                            ratio =
                                run {
                                    val pw = photo.width
                                    val ph = photo.height
                                    if (pw != null && ph != null && ph > 0) pw.toFloat() / ph.toFloat() else 1f
                                },
                        ),
                    )
                }
                entry.media?.embed?.let { embed ->
                    cache.embedQueries.insertOrReplace(
                        Embed(
                            id = embed.url,
                            type =
                                when (embed.type) {
                                    "video" -> EmbedType.Video
                                    else -> EmbedType.Unknown
                                },
                            fileName = null,
                            preview = embed.thumbnail ?: embed.url,
                            size = null,
                            hasAdultContent = false,
                            ratio = 1f,
                        ),
                    )
                }
                cache.entriesQueries.insertOrReplace(
                    EntryEntity(
                        id = entry.id,
                        postedAt = entry.createdAt,
                        body = entry.content.orEmpty(),
                        voteCount = entry.votes.up,
                        embedId = entry.media?.photo?.url ?: entry.media?.embed?.url,
                        commentsCount = entry.comments.count,
                        isBlocked = entry.deleted ?: false,
                        isFavorite = entry.favourite ?: false,
                        app = entry.device,
                        canComment = entry.actions?.comment ?: true,
                        violationUrl = null,
                        userVote = entry.voted.asUserVote(),
                        profileId = entry.author.username,
                    ),
                )
                cache.profileActionsQueries.insertPage(
                    ProfileActionsEntity(
                        profileId = profileId,
                        linkId = null,
                        entryId = entry.id,
                        page = page,
                        orderOnPage = index,
                    ),
                )
            }
        }
    },
    delete = { page -> cache.profileActionsQueries.deletePage(profileId, page = page) },
    deleteAll = { cache.profileActionsQueries.deleteByProfile(profileId) },
)

internal fun ProfileQueries.upsertV3(author: UserShortResponseV3) {
    upsert(
        id = author.username,
        avatar = author.avatar.orEmpty(),
        color = author.color.toColorEntityFromName(),
        gender = author.gender.toGenderEntity(),
    )
}

internal fun ProfileQueries.upsert(author: AuthorResponse) {
    upsert(
        id = author.login,
        avatar = author.avatar,
        color = author.color.toColorEntity(),
        gender = author.sex.toGenderEntity(),
    )
}

internal fun String?.asUserVote() =
    when (this) {
        "dig" -> UserVote.Up
        "bury" -> UserVote.Down
        else -> null
    }

internal fun Int?.asUserVote() =
    when {
        this != null && this > 0 -> UserVote.Up
        this != null && this < 0 -> UserVote.Down
        else -> null
    }
