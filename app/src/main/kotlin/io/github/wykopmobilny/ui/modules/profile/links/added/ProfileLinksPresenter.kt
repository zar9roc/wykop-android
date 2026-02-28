package io.github.wykopmobilny.ui.modules.profile.links.added

import io.github.wykopmobilny.api.profile.ProfileApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.ui.fragments.links.LinkActionListener
import io.github.wykopmobilny.ui.fragments.links.LinksInteractor
import io.github.wykopmobilny.utils.intoComposite
import io.reactivex.Single

class ProfileLinksPresenter(
    val schedulers: Schedulers,
    val profileApi: ProfileApi,
    val linksInteractor: LinksInteractor,
) : BasePresenter<ProfileLinksView>(),
    LinkActionListener {
    var page: String? = null
    lateinit var username: String

    fun loadAdded(shouldRefresh: Boolean) {
        if (shouldRefresh) page = null
        profileApi
            .getAdded(username, page)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    if (it.totalCount > 0) {
                        page = it.nextPage
                        view?.addItems(it.filtered, shouldRefresh)
                    } else {
                        view?.disableLoading()
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    fun loadBurried(shouldRefresh: Boolean) {
        if (shouldRefresh) page = null
        profileApi
            .getBuried(username, page)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    if (it.totalCount > 0) {
                        page = it.nextPage
                        view?.addItems(it.filtered, shouldRefresh)
                    } else {
                        view?.disableLoading()
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    fun loadDigged(shouldRefresh: Boolean) {
        if (shouldRefresh) page = null
        profileApi
            .getDigged(username, page)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    if (it.totalCount > 0) {
                        page = it.nextPage
                        view?.addItems(it.filtered, shouldRefresh)
                    } else {
                        view?.disableLoading()
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    fun loadPublished(shouldRefresh: Boolean) {
        if (shouldRefresh) page = null
        profileApi
            .getPublished(username, page)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    if (it.totalCount > 0) {
                        page = it.nextPage
                        view?.addItems(it.filtered, shouldRefresh)
                    } else {
                        view?.disableLoading()
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    override fun dig(link: Link) {
        linksInteractor.dig(link).processLinkSingle(link)
    }

    override fun removeVote(link: Link) {
        linksInteractor.voteRemove(link).processLinkSingle(link)
    }

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
