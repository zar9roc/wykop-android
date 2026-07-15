package io.github.wykopmobilny.domain.linkdetails

import io.github.wykopmobilny.data.cache.api.Embed
import io.github.wykopmobilny.data.cache.api.EmbedType
import io.github.wykopmobilny.data.cache.api.UserVote
import io.github.wykopmobilny.domain.profile.UserInfo
import io.github.wykopmobilny.ui.components.widgets.EmbedMediaUi
import kotlinx.datetime.Instant
import java.net.URL

internal data class LinkComment(
    val id: Long,
    val app: String?,
    val body: String,
    val postedAt: Instant,
    val author: UserInfo,
    val plusCount: Int,
    val minusCount: Int,
    val userAction: UserVote?,
    val embed: Embed?,
    val userFavorite: Boolean,
    // np. "moderator" - komentarz usuniety, API zwraca pusta tresc
    val deletedReason: String?,
) {
    val totalCount = plusCount - minusCount
}

internal fun LinkComment.wykopUrl(linkId: Long) = "https://www.wykop.pl/link/$linkId/#comment-$id"

internal fun Embed.toUi(
    useLowQualityImage: Boolean,
    overlay: EmbedMediaUi.Overlay?,
    clickAction: () -> Unit,
    widthToHeightRatio: Float,
) = EmbedMediaUi(
    content =
        when (type) {
            EmbedType.AnimatedImage -> {
                EmbedMediaUi.Content.PlayableMedia(
                    previewImage = preview,
                    domain = "Gif",
                )
            }

            EmbedType.Video -> {
                EmbedMediaUi.Content.PlayableMedia(
                    previewImage = preview,
                    domain = URL(id).userFriendlyDomain(includeTopLevel = false),
                )
            }

            EmbedType.StaticImage,
            EmbedType.Unknown,
            -> {
                // Na liscie zawsze wariant preview (w400) - pelna rozdzielczosc (id)
                // laduje dopiero pelnoekranowy podglad po kliknieciu.
                EmbedMediaUi.Content.StaticImage(
                    url = preview,
                )
            }
        },
    size = size.takeIf { useLowQualityImage || type == EmbedType.AnimatedImage },
    overlay = overlay,
    widthToHeightRatio = widthToHeightRatio,
    clickAction = clickAction,
    // id = docelowy URL medium; WykopEmbedView wybiera po nim ikone dostawcy.
    url = id,
    isAnimated = type == EmbedType.AnimatedImage,
)

internal fun URL.userFriendlyDomain(includeTopLevel: Boolean = true): String {
    val parts = host.split(".")
    return if (includeTopLevel) {
        parts.takeLast(2).joinToString(separator = ".")
    } else {
        if (host.endsWith("youtu.be")) {
            "Youtube"
        } else {
            parts.dropLast(1).lastOrNull() ?: host
        }
    }
}
