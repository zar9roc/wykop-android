package io.github.wykopmobilny.models.dataclass

import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.utils.toPrettyDate
import kotlinx.datetime.Instant

class LinkCommentV3Item(
    val response: LinkCommentResponseV3,
    val linkId: Long,
    val author: Author,
    var embed: Embed?,
    var body: String?,
    var isCollapsed: Boolean = false,
    var isParentCollapsed: Boolean = false,
    var childCommentCount: Int = 0,
    var isNsfw: Boolean = false,
    var isBlocked: Boolean = false,
) {
    val id: Long get() = response.id
    val parentId: Long? get() = response.parentId
    val deletedReason: String? get() = response.deleted
    val slug: String? get() = response.slug
    var voteCountPlus: Int = response.votes.up
    var voteCountMinus: Int = response.votes.down
    var voteCount: Int = voteCountPlus - voteCountMinus
    var userVote: Int = response.voted ?: 0
    val app: String? get() = response.device
    val fullDate: Instant get() = response.createdAt
    val canVote: Boolean = true
    val favorite: Boolean = false
    val violationUrl: String? = null
    val url: String get() = "https://www.wykop.pl/link/$linkId/#comment-$id"

    val date: String
        get() = fullDate.toPrettyDate()
}
