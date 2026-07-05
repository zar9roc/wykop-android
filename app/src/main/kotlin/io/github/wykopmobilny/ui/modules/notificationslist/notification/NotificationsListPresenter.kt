package io.github.wykopmobilny.ui.modules.notificationslist.notification

import io.github.wykopmobilny.api.notifications.NotificationsApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.dataclass.NotificationHeader
import io.github.wykopmobilny.ui.modules.notificationslist.NotificationsListView
import io.github.wykopmobilny.utils.intoComposite
import io.reactivex.Single

class NotificationsListPresenter(
    val schedulers: Schedulers,
    val notificationsApi: NotificationsApi,
) : BasePresenter<NotificationsListView>() {
    var page = 1

    fun loadData(shouldRefresh: Boolean) {
        if (shouldRefresh) page = 1
        notificationsApi
            .getNotifications(page)
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

    /**
     * Tryb grupowania: powiadomienia prowadzace do tego samego widoku (ten sam wpis
     * lub znalezisko) trafiaja pod wspolny naglowek-akordeon, jak w zakladce tagow.
     */
    fun loadAllGrouped(shouldRefresh: Boolean) {
        if (shouldRefresh) page = 1
        val allData = arrayListOf<Notification>()
        var fetchedPages = 0
        var done = false
        Single
            .defer { notificationsApi.getNotifications(page) }
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .repeatUntil { done }
            .subscribe(
                { data ->
                    allData.addAll(data)
                    fetchedPages++
                    if (data.isEmpty() || fetchedPages >= MAX_GROUPED_PAGES) {
                        done = true
                        view?.addNotifications(groupByTarget(allData), true)
                        view?.disableLoading()
                    } else {
                        page++
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    private fun groupByTarget(all: List<Notification>): List<Notification> {
        val result = arrayListOf<Notification>()
        all
            .groupBy { it.url?.substringBefore("/#comment-") ?: "no-target-${it.id}" }
            .forEach { (target, group) ->
                if (group.size == 1) {
                    result.add(group.first())
                } else {
                    // Akordeon jak w zakladce tagow: naglowek celu + rozgrupowane
                    // powiadomienia. Dzieci zachowuja kotwice #comment - klikniecie
                    // nawiguje do widoku i scrolluje do konkretnego komentarza.
                    group.forEach { it.tag = target }
                    val newest = group.first()
                    result.add(
                        NotificationHeader(
                            body = target,
                            notificationsCount = group.count { it.new },
                            title = newest.body.substringAfter(": ", newest.body).take(HEADER_TITLE_LENGTH),
                            navigationUrl = newest.url?.let { target },
                        ),
                    )
                    result.addAll(group)
                }
            }
        return result
    }

    fun readNotifications() {
        notificationsApi
            .readNotifications()
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe({ view?.showReadToast() }, { view?.showErrorDialog(it) })
            .intoComposite(compositeObservable)
    }

    companion object {
        // 13 stron x 25 = 325 powiadomien - gorna granica trybu grupowania.
        private const val MAX_GROUPED_PAGES = 13
        private const val HEADER_TITLE_LENGTH = 60
    }
}
