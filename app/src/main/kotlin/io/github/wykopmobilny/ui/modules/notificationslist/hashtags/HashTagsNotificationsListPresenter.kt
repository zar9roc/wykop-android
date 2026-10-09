package io.github.wykopmobilny.ui.modules.notificationslist.hashtags

import io.github.wykopmobilny.api.notifications.NotificationsApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.dataclass.NotificationHeader
import io.github.wykopmobilny.ui.modules.notificationslist.NotificationsListView
import io.github.wykopmobilny.utils.intoComposite

class HashTagsNotificationsListPresenter(
    val schedulers: Schedulers,
    private val notificationsApi: NotificationsApi,
) : BasePresenter<NotificationsListView>() {
    var page = 1

    // Rozmiar pierwszej strony - wzorzec "pelnej" strony. API nie mowi, ile stron
    // jest, wiec koniec listy rozpoznajemy po odpowiedzi pustej albo krotszej.
    private var fullPageSize = 0

    // Pula trybu grupowania - wszystkie dotad pobrane powiadomienia. Doladowanie
    // dokleja do niej kolejna strone i przegrupowuje calosc.
    private val groupedData = arrayListOf<Notification>()
    private val groupedIds = hashSetOf<String>()

    /**
     * Tryb plaski - jedno zapytanie na wejscie/odswiezenie, kolejne strony wylacznie
     * po klinieciu stopki "pokaz starsze" (scroll niczego nie wyzwala).
     */
    fun loadData(shouldRefresh: Boolean) {
        if (shouldRefresh) {
            page = 1
            fullPageSize = 0
        }
        val requestedPage = page
        notificationsApi
            .getHashTagNotifications(requestedPage)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { data ->
                    if (requestedPage == 1) fullPageSize = data.size
                    page = requestedPage + 1
                    view?.addNotifications(data, shouldRefresh)
                    hideFooterWhenLastPage(data)
                },
                { if (shouldRefresh) view?.showErrorDialog(it) else view?.showLoadMoreError(it) },
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

    /**
     * Tryb grupowania - wejscie na ekran i odswiezenie pobieraja DOKLADNIE JEDNA
     * strone, dokladnie jak zakladka "Do mnie". Wczesniej ten tryb dociagal z gory
     * do 13 stron tylko po to, zeby skleic naglowki tagow po stronie klienta.
     * Grupy skladaja sie wiec z tego, co faktycznie pobrano, i rosna po doladowaniu.
     */
    fun loadGrouped(shouldRefresh: Boolean) {
        if (shouldRefresh) {
            page = 1
            fullPageSize = 0
            groupedData.clear()
            groupedIds.clear()
        }
        fetchGroupedPage(shouldRefresh)
    }

    /** Klikniecie stopki w trybie grupowania - kolejna strona do tej samej puli. */
    fun loadMoreGrouped() = fetchGroupedPage(shouldRefresh = false)

    /**
     * Odtworzenie puli po obrocie ekranu - z listy w adapterze zdejmujemy naglowki
     * i wracamy do surowych powiadomien, zeby doladowanie doklejalo do nich kolejna
     * strone zamiast podmienic cala liste tym, co wlasnie przyszlo.
     */
    fun restoreLoaded(notifications: List<Notification>) {
        groupedData.clear()
        groupedIds.clear()
        notifications
            .filterNot { it is NotificationHeader }
            .forEach { if (groupedIds.add(it.id)) groupedData.add(it) }
    }

    private fun fetchGroupedPage(shouldRefresh: Boolean) {
        val requestedPage = page
        notificationsApi
            .getHashTagNotifications(requestedPage)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { data ->
                    if (requestedPage == 1) fullPageSize = data.size
                    page = requestedPage + 1
                    data.forEach { if (groupedIds.add(it.id)) groupedData.add(it) }
                    publishGrouped()
                    hideFooterWhenLastPage(data)
                },
                { if (shouldRefresh) view?.showErrorDialog(it) else view?.showLoadMoreError(it) },
            ).intoComposite(compositeObservable)
    }

    // Grupuje WSZYSTKIE powiadomienia po tagu (nie tylko nieprzeczytane - w API v3
    // wiekszosc jest przeczytana i filtr po nieprzeczytanych dawal pusta zakladke).
    private fun publishGrouped() {
        val sortedData = arrayListOf<Notification>()
        for (tag in groupedData.map { it.tag }.distinct()) {
            val group = groupedData.filter { it.tag == tag }
            // Licznik w naglowku = tylko NIEPRZECZYTANE wpisy w grupie.
            sortedData.add(NotificationHeader(tag, group.count { it.new }))
            sortedData.addAll(group)
        }
        // Cala lista budowana od nowa - nowa strona ma wpasc pod istniejacy naglowek
        // taga, a nie utworzyc drugi naglowek tego samego taga.
        view?.addNotifications(sortedData, true)
    }

    /** Pusta albo niepelna strona = dalej nic nie ma, stopka znika na dobre. */
    private fun hideFooterWhenLastPage(data: List<Notification>) {
        if (data.isEmpty() || data.size < fullPageSize) view?.disableLoading()
    }
}
