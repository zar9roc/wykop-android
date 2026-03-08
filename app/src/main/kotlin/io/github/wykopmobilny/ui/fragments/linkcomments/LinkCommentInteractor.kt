package io.github.wykopmobilny.ui.fragments.linkcomments

import io.github.wykopmobilny.api.links.LinksApi
import io.github.wykopmobilny.models.dataclass.LinkComment
import io.reactivex.Single
import javax.inject.Inject

class LinkCommentInteractor
    @Inject
    constructor(
        val linksApi: LinksApi,
    ) {
        fun commentVoteUp(comment: LinkComment): Single<LinkComment> =
            linksApi
                .commentVoteUp(comment.linkId, comment.id)
                .map {
                    comment.voteCountPlus += 1
                    comment.voteCount = comment.voteCountPlus - comment.voteCountMinus
                    comment.userVote = 1
                    comment
                }

        fun commentVoteDown(comment: LinkComment): Single<LinkComment> =
            linksApi
                .commentVoteDown(comment.linkId, comment.id)
                .map {
                    comment.voteCountMinus += 1
                    comment.voteCount = comment.voteCountPlus - comment.voteCountMinus
                    comment.userVote = -1
                    comment
                }

        fun commentVoteCancel(comment: LinkComment): Single<LinkComment> =
            linksApi
                .commentVoteCancel(comment.linkId, comment.id)
                .map {
                    if (comment.userVote > 0) comment.voteCountPlus -= 1
                    if (comment.userVote < 0) comment.voteCountMinus -= 1
                    comment.voteCount = comment.voteCountPlus - comment.voteCountMinus
                    comment.userVote = 0
                    comment
                }

        fun removeComment(comment: LinkComment): Single<LinkComment> =
            linksApi
                .commentDelete(comment.linkId, comment.id)
                .map {
                    comment.embed = null
                    comment.body = "[Komentarz usunięty]"
                    comment
                }
    }
