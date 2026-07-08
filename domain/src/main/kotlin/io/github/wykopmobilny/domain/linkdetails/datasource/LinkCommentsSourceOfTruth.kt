package io.github.wykopmobilny.domain.linkdetails.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.domain.di.flowSourceOfTruth
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
import io.github.wykopmobilny.kotlin.withImageParams
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

internal fun linkCommentsSourceOfTruth(cache: AppCache) =
    flowSourceOfTruth<Long, List<LinkCommentResponseV3>, Map<LinkComment, List<LinkComment>>>(
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
                        // Odpowiedzi moga trafic do cache przed watkiem nadrzednym
                        // (doladowywanie w tle) albo zostac po usunietym watku -
                        // bez rodzica nie da sie ich pokazac, pomijamy zamiast crashowac.
                        .filter { commentsById.containsKey(it.parentId ?: it.id) }
                        .groupBy { commentsById.getValue(it.parentId ?: it.id) }
                        .map { (key, value) ->
                            val parent = key.toContent()
                            val children = value.filterNot { it.parentId == null || it.id == it.parentId }.map { it.toContent() }

                            parent to children
                        }.toMap()
                }
        },
        writer = { linkId, comments -> persistLinkComments(cache, linkId, comments) },
        delete = { linkId -> cache.linkCommentsQueries.deleteByLinkId(linkId) },
    )

/**
 * Wspolny zapis komentarzy do cache - uzywany przez writer source of truth (watki
 * nadrzedne z fetchera) oraz przez doladowywanie odpowiedzi w tle (LinkDetailsModule),
 * ktore omija fetcher, zeby nie wpasc w petle restartow flowSourceOfTruth.
 */
internal suspend fun persistLinkComments(
    cache: AppCache,
    linkId: Long,
    comments: List<LinkCommentResponseV3>,
) = withContext(AppDispatchers.IO) {
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
                        // Wariant w400 z CDN do wyswietlania na liscie - pelna
                        // rozdzielczosc (id) tylko w pelnoekranowym podgladzie.
                        preview = photo.url.withImageParams("w400"),
                        size = null,
                        // Flaga 18+ jest na komentarzu (adult), nie na zdjeciu - schemat Photo
                        // w API nie ma plus18, wiec dawniej hasAdultContent bylo zawsze false.
                        hasAdultContent = comment.adult ?: false,
                        ratio =
                            run {
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
                        // media.embed to zawsze zewnetrzne medium odtwarzalne, a v3 podaje
                        // typ per dostawca ("streamable"/"youtube"/"gfycat"/"coub"...), nie
                        // "video" - stara mapa dawala Unknown i film sie nie odtwarzal.
                        // MediaHandler routuje potem po hoscie (youtube -> player, reszta
                        // -> odtwarzacz/przegladarka).
                        type = EmbedType.Video,
                        fileName = null,
                        preview = embed.thumbnail ?: embed.url,
                        size = null,
                        hasAdultContent = comment.adult ?: false,
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
                    isBlocked = !comment.deleted.isNullOrEmpty(),
                    isFavorite = false,
                    linkId = linkId,
                    embedId = embedId,
                    app = comment.device,
                    violationUrl = null,
                    deletedReason = comment.deleted,
                    slug = comment.slug,
                ),
            )
        }
    }
}

private fun SelectByLinkId.toContent() =
    LinkComment(
        id = id,
        body = body,
        deletedReason = deletedReason,
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
