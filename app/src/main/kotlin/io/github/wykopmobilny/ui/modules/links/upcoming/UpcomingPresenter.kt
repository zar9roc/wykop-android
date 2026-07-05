package io.github.wykopmobilny.ui.modules.links.upcoming

import io.github.wykopmobilny.api.links.LinksApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.ui.fragments.links.LinkActionListener
import io.github.wykopmobilny.ui.fragments.links.LinksInteractor
import io.github.wykopmobilny.utils.intoComposite
import io.reactivex.Single

class UpcomingPresenter(
    val schedulers: Schedulers,
    val linksInteractor: LinksInteractor,
    private val linksApi: LinksApi,
) : BasePresenter<UpcomingView>(),
    LinkActionListener {
    companion object {
        const val SORTBY_COMMENTS = "commented"
        const val SORTBY_VOTES = "digged"
        const val SORTBY_DATE = "newest"
        const val SORTBY_ACTIVE = "active"
    }

    var page: String? = null
    private var pageNumber = 1
    var sortBy = "commented"

    fun getUpcomingLinks(shouldRefresh: Boolean) {
        if (shouldRefresh) {
            page = null
            pageNumber = 1
        }
        linksApi
            .getUpcoming(page, sortBy)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    if (it.totalCount > 0) {
                        page = it.nextPage ?: (++pageNumber).toString()
                        view?.addItems(it.filtered, shouldRefresh)
                    } else {
                        view?.disableLoading()
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    override fun dig(link: Link) = linksInteractor.dig(link).processLinkSingle(link)

    override fun bury(
        link: Link,
        reason: Int,
    ) = linksInteractor.bury(link, reason).processLinkSingle(link)

    override fun removeVote(link: Link) = linksInteractor.voteRemove(link).processLinkSingle(link)

    fun Single<Link>.processLinkSingle(link: Link) {
        this
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { view?.updateLink(it) },
                {
                    view?.showErrorDialog(it)
                    view?.updateLink(link)
                },
            ).intoComposite(compositeObservable)
    }
}
