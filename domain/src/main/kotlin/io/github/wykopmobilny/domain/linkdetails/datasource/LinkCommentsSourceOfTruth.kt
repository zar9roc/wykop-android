package io.github.wykopmobilny.domain.linkdetails.datasource

import com.dropbox.android.external.store4.SourceOfTruth
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.data.cache.api.Embed
import io.github.wykopmobilny.data.cache.api.EmbedType
import io.github.wykopmobilny.data.cache.api.LinkCommentsEntity
import io.github.wykopmobilny.data.cache.api.linkComments.SelectByLinkId
import io.github.wykopmobilny.domain.linkdetails.LinkComment
import io.github.wykopmobilny.domain.profile.UserInfo
import io.github.wykopmobilny.domain.profile.datasource.asUserVote
import io.github.wykopmobilny.domain.profile.datasource.upsertV3
import io.github.wykopmobilny.domain.profile.toColorDomain
import io.github.wykopmobilny.domain.profile.toGenderDomain
import io.github.wykopmobilny.kotlin.AppDispatchers
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue

internal fun linkCommentsSourceOfTruth(cache: AppCache) =
    SourceOfTruth.of<Long, List<LinkCommentResponseV3>, Map<LinkComment, List<LinkComment>>>(
        reader = { linkId ->
            cache.linkCommentsQueries
                .selectByLinkId(linkId)
                .asFlow()
                .mapToList(AppDispatchers.IO)
                .map { comments ->
                    val commentsById =
                        comments
                            .asSequence()
                            .filter { it.parentId == null || it.id == it.parentId }
                            .associateBy { it.id }
                    comments
                        .groupBy { commentsById.getValue(it.parentId ?: it.id) }
                        .map { (key, value) ->
                            val parent = key.toContent()
                            val children = value.filterNot { it.parentId == null || it.id == it.parentId }.map { it.toContent() }

                            parent to children
                        }.toMap()
                }
        },
        writer = { linkId, comments ->
            cache.transaction {
                comments.forEach { comment ->
                    cache.profileQueries.upsertV3(comment.author)
                    val embedId = comment.media?.photo?.url ?: comment.media?.embed?.url
                    comment.media?.photo?.let { photo ->
                        cache.embedQueries.insertOrReplace(
                            Embed(
                                id = photo.url,
                                type = EmbedType.StaticImage,
                                fileName = photo.source,
                                preview = photo.url,
                                size = null,
                                hasAdultContent = photo.plus18 ?: false,
                                ratio = run {
                                    val pw = photo.width
                                    val ph = photo.height
                                    if (pw != null && ph != null && ph > 0) pw.toFloat() / ph.toFloat() else 1f
                                },
                            ),
                        )
                    }
                    comment.media?.embed?.let { embed ->
                        cache.embedQueries.insertOrReplace(
                            Embed(
                                id = embed.url,
                                type = when (embed.type) {
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
                    cache.linkCommentsQueries.insertOrReplace(
                        LinkCommentsEntity(
                            id = comment.id,
                            postedAt = comment.createdAt,
                            profileId = comment.author.username,
                            voteCount = comment.votes.up + comment.votes.down,
                            voteCountPlus = comment.votes.up,
                            body = comment.content.orEmpty(),
                            parentId = comment.parentId ?: comment.id,
                            canVote = true,
                            userVote = comment.voted.asUserVote(),
                            isBlocked = comment.deleted ?: false,
                            isFavorite = false,
                            linkId = linkId,
                            embedId = embedId,
                            app = comment.device,
                            violationUrl = null,
                        ),
                    )
                }
            }
        },
        delete = { linkId -> cache.linkCommentsQueries.deleteByLinkId(linkId) },
    )

private fun SelectByLinkId.toContent() =
    LinkComment(
        id = id,
        body = body,
        postedAt = postedAt,
        author =
            UserInfo(
                profileId = profileId,
                avatarUrl = avatar,
                rank = rank,
                gender = gender?.toGenderDomain(),
                color = color.toColorDomain(),
            ),
        plusCount = voteCountPlus,
        minusCount = (voteCount - voteCountPlus).absoluteValue,
        userAction = userVote,
        app = app,
        userFavorite = isFavorite,
        embed =
            embedId?.let {
                Embed(
                    id = it,
                    type = type!!,
                    fileName = fileName,
                    preview = preview!!,
                    size = size,
                    hasAdultContent = hasAdultContent!!,
                    ratio = ratio!!,
                )
            },
    )
