package io.github.wykopmobilny.ui.modules.profile.links.comments

import io.github.wykopmobilny.api.profile.ProfileApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item
import io.github.wykopmobilny.ui.fragments.linkcomments.LinkCommentActionListener
import io.github.wykopmobilny.ui.fragments.linkcomments.LinkCommentInteractor
import io.github.wykopmobilny.utils.intoComposite
import io.reactivex.Single

class ProfileLinksFragmentPresenter(
    private val schedulers: Schedulers,
    private val profileApi: ProfileApi,
    private val linksInteractor: LinkCommentInteractor,
) : BasePresenter<ProfileLinkCommentsView>(),
    LinkCommentActionListener {
    var page = 1
    lateinit var username: String

    fun loadData(shouldRefresh: Boolean) {
        if (shouldRefresh) page = 1
        profileApi
            .getLinkComments(username, page)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    if (it.isNotEmpty()) {
                        page++
                        view?.addItems(it, shouldRefresh)
                    } else {
                        view?.disableLoading()
                        if (shouldRefresh) {
                            view?.showSearchEmptyView = true
                        }
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    override fun removeVote(comment: LinkCommentV3Item) = linksInteractor.commentVoteCancel(comment).processLinkCommentSingle(comment)

    override fun digComment(comment: LinkCommentV3Item) = linksInteractor.commentVoteUp(comment).processLinkCommentSingle(comment)

    override fun buryComment(comment: LinkCommentV3Item) = linksInteractor.commentVoteDown(comment).processLinkCommentSingle(comment)

    override fun deleteComment(comment: LinkCommentV3Item) = linksInteractor.removeComment(comment).processLinkCommentSingle(comment)

    private fun Single<LinkCommentV3Item>.processLinkCommentSingle(link: LinkCommentV3Item) {
        this
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { view?.updateComment(it) },
                {
                    view?.showErrorDialog(it)
                    view?.updateComment(link)
                },
            ).intoComposite(compositeObservable)
    }
}
