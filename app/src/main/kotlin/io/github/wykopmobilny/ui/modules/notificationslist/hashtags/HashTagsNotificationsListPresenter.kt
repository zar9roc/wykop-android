package io.github.wykopmobilny.ui.modules.notificationslist.hashtags

import io.github.wykopmobilny.api.notifications.NotificationsApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.dataclass.NotificationHeader
import io.github.wykopmobilny.ui.modules.notificationslist.NotificationsListView
import io.github.wykopmobilny.utils.intoComposite
import io.reactivex.Single

class HashTagsNotificationsListPresenter(
    val schedulers: Schedulers,
    private val notificationsApi: NotificationsApi,
) : BasePresenter<NotificationsListView>() {
    var page = 1

    fun loadData(shouldRefresh: Boolean) {
        if (shouldRefresh) page = 1
        notificationsApi
            .getHashTagNotifications(page)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    if (it.isNotEmpty()) {
                        page++
                        view?.addNotifications(it, shouldRefresh)
                    } else {
                        view?.disableLoading()
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    fun readNotifications() {
        notificationsApi
            .readHashTagNotifications()
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe({ view?.showReadToast() }, { view?.showErrorDialog(it) })
            .intoComposite(compositeObservable)
    }

    fun loadAllNotifications(shouldRefresh: Boolean) = fetchAllPages(shouldRefresh)

    private fun fetchAllPages(shouldRefresh: Boolean) {
        if (shouldRefresh) page = 1
        val allData = arrayListOf<Notification>()
        var fetchedPages = 0
        var done = false
        Single
            .defer { notificationsApi.getHashTagNotifications(page) }
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .repeatUntil { done }
            .subscribe(
                { data ->
                    allData.addAll(data)
                    fetchedPages++
                    if (data.isEmpty() || fetchedPages >= MAX_GROUPED_PAGES) {
                        done = true
                        publishGrouped(allData)
                    } else {
                        page++
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    // Grupuje WSZYSTKIE powiadomienia po tagu (nie tylko nieprzeczytane - w API v3
    // wiekszosc jest przeczytana i filtr po nieprzeczytanych dawal pusta zakladke).
    private fun publishGrouped(allData: List<Notification>) {
        val sortedData = arrayListOf<Notification>()
        for (tag in allData.map { it.tag }.distinct()) {
            val group = allData.filter { it.tag == tag }
            // Licznik w naglowku = tylko NIEPRZECZYTANE wpisy w grupie.
            sortedData.add(NotificationHeader(tag, group.count { it.new }))
            sortedData.addAll(group)
        }
        view?.addNotifications(sortedData, true)
        view?.disableLoading()
    }

    companion object {
        // 13 stron x 25 = 325 powiadomien - gorna granica trybu grupowania,
        // zeby nie stronicowac calej historii powiadomien.
        private const val MAX_GROUPED_PAGES = 13
    }
}
