package io.github.wykopmobilny.ui.modules.links.hits

import io.github.wykopmobilny.api.hits.HitsApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.ui.fragments.links.LinkActionListener
import io.github.wykopmobilny.ui.fragments.links.LinksInteractor
import io.github.wykopmobilny.utils.intoComposite
import io.reactivex.Single

class HitsPresenter(
    val schedulers: Schedulers,
    val linksInteractor: LinksInteractor,
    private val hitsApi: HitsApi,
) : BasePresenter<HitsView>(),
    LinkActionListener {
    companion object {
        const val HITS_DAY = "day"
        const val HITS_WEEK = "week"
        const val HITS_MONTH = "month"
        const val HITS_YEAR = "year"
    }

    var currentScreen = "day"
    var yearSelection = 0
    var monthSelection = 0
    private var currentPage: String? = null
    private var pageNumber = 1

    fun loadData() {
        currentPage = null
        pageNumber = 1
        loadPage(shouldRefresh = true)
    }

    fun loadMore() {
        loadPage(shouldRefresh = false)
    }

    private fun loadPage(shouldRefresh: Boolean) {
        when (currentScreen) {
            HITS_DAY -> hitsApi.currentDay(currentPage)
            HITS_WEEK -> hitsApi.currentWeek(currentPage)
            HITS_MONTH -> hitsApi.byMonth(yearSelection, monthSelection, currentPage)
            else -> hitsApi.byYear(yearSelection, currentPage)
        }.subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    if (it.totalCount > 0) {
                        currentPage = it.nextPage ?: (++pageNumber).toString()
                        view?.addItems(it.filtered, shouldRefresh)
                    } else {
                        view?.disableLoading()
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    override fun dig(link: Link) = linksInteractor.dig(link).processLinkSingle(link)

    override fun removeVote(link: Link) = linksInteractor.voteRemove(link).processLinkSingle(link)

    private fun Single<Link>.processLinkSingle(link: Link) {
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
