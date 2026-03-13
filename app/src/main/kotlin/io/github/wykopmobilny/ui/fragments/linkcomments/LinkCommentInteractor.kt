package io.github.wykopmobilny.ui.fragments.linkcomments

import io.github.wykopmobilny.api.links.LinksApi
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item
import io.reactivex.Single
import javax.inject.Inject

class LinkCommentInteractor
    @Inject
    constructor(
        val linksApi: LinksApi,
    ) {
        fun commentVoteUp(comment: LinkCommentV3Item): Single<LinkCommentV3Item> =
            linksApi
                .commentVoteUp(comment.linkId, comment.id)
                .map {
                    comment.voteCountPlus += 1
                    comment.voteCount = comment.voteCountPlus - comment.voteCountMinus
                    comment.userVote = 1
                    comment
                }

        fun commentVoteDown(comment: LinkCommentV3Item): Single<LinkCommentV3Item> =
            linksApi
                .commentVoteDown(comment.linkId, comment.id)
                .map {
                    comment.voteCountMinus += 1
                    comment.voteCount = comment.voteCountPlus - comment.voteCountMinus
                    comment.userVote = -1
                    comment
                }

        fun commentVoteCancel(comment: LinkCommentV3Item): Single<LinkCommentV3Item> =
            linksApi
                .commentVoteCancel(comment.linkId, comment.id)
                .map {
                    if (comment.userVote > 0) comment.voteCountPlus -= 1
                    if (comment.userVote < 0) comment.voteCountMinus -= 1
                    comment.voteCount = comment.voteCountPlus - comment.voteCountMinus
                    comment.userVote = 0
                    comment
                }

        fun removeComment(comment: LinkCommentV3Item): Single<LinkCommentV3Item> =
            linksApi
                .commentDelete(comment.linkId, comment.id)
                .map {
                    comment.embed = null
                    comment.body = "[Komentarz usunięty]"
                    comment
                }
    }
