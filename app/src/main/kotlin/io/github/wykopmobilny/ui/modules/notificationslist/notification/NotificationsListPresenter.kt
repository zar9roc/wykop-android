package io.github.wykopmobilny.ui.modules.notificationslist.notification

import io.github.wykopmobilny.api.notifications.NotificationsApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.dataclass.NotificationGroup
import io.github.wykopmobilny.models.dataclass.NotificationTargetKind
import io.github.wykopmobilny.ui.modules.notificationslist.NotificationsListView
import io.github.wykopmobilny.utils.intoComposite

class NotificationsListPresenter(
    val schedulers: Schedulers,
    val notificationsApi: NotificationsApi,
) : BasePresenter<NotificationsListView>() {
    var page = 1

    // Rozmiar pierwszej strony - wzorzec "pelnej" strony. API nie mowi, ile stron
    // jest, wiec koniec listy rozpoznajemy po odpowiedzi pustej albo krotszej.
    private var fullPageSize = 0

    // Powiadomienia pobrane dotad (surowe, przed grupowaniem). Grupy sklejamy
    // z tego, co juz mamy - rozwijanie wiersza NIE wysyla zapytania.
    private val loaded = arrayListOf<Notification>()
    private val loadedIds = hashSetOf<String>()

    /** Tryb plaski - kolejna strona dochodzi do juz pokazanych pozycji. */
    fun loadData(shouldRefresh: Boolean) =
        fetchPage(shouldRefresh) { data ->
            // Lista publikowana takze gdy jest pusta - inaczej ekran zostawalby
            // z krecacym sie wskaznikiem ladowania przy zerze powiadomien.
            view?.addNotifications(data, shouldRefresh)
        }

    /**
     * Tryb grupowania: powiadomienia prowadzace do tego samego wpisu lub znaleziska
     * skladaja sie w JEDEN wiersz zbiorczy. Nowa strona wchodzi do puli i cala lista
     * jest przegrupowywana od nowa - dzieki temu nowe powiadomienia wpadaja do
     * istniejacego wiersza zbiorczego zamiast tworzyc drugi wiersz tego samego celu.
     * Licznik "N innych osob" moze po doladowaniu urosnac - tak ma byc.
     */
    fun loadGrouped(shouldRefresh: Boolean) =
        fetchPage(shouldRefresh) { data ->
            data.forEach { if (loadedIds.add(it.id)) loaded.add(it) }
            view?.addNotifications(groupByTarget(loaded), true)
        }

    /**
     * Jedno zapytanie = jedna strona. Wejscie na ekran i odswiezenie pobieraja
     * strone 1; kolejne strony wylacznie po klinieciu stopki "pokaz starsze"
     * (scroll niczego nie wyzwala). Po ostatniej stronie stopka znika na dobre.
     */
    private fun fetchPage(
        shouldRefresh: Boolean,
        publish: (List<Notification>) -> Unit,
    ) {
        if (shouldRefresh) {
            page = 1
            fullPageSize = 0
            loaded.clear()
            loadedIds.clear()
        }
        val requestedPage = page
        notificationsApi
            .getNotifications(requestedPage)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { data ->
                    if (requestedPage == 1) fullPageSize = data.size
                    page = requestedPage + 1
                    publish(data)
                    // Pusta albo niepelna strona = dalej nic nie ma, chowamy stopke.
                    if (data.isEmpty() || data.size < fullPageSize) view?.disableLoading()
                },
                { if (shouldRefresh) view?.showErrorDialog(it) else view?.showLoadMoreError(it) },
            ).intoComposite(compositeObservable)
    }

    /**
     * Odtworzenie stanu po obrocie ekranu - adapter trzyma liste juz pogrupowana,
     * wiec zdejmujemy z niej wiersze zbiorcze i wracamy do surowych powiadomien.
     */
    fun restoreLoaded(notifications: List<Notification>) {
        loaded.clear()
        loadedIds.clear()
        notifications
            .filterNot { it is NotificationGroup }
            .forEach { if (loadedIds.add(it.id)) loaded.add(it) }
    }

    private fun groupByTarget(all: List<Notification>): List<Notification> {
        val result = arrayListOf<Notification>()
        all
            .groupBy(::groupKeyOf)
            .forEach { (key, group) ->
                if (group.size == 1) {
                    // Pojedynczy kontekst - zwykly wiersz z dotychczasowa trescia,
                    // bez licznika i bez chevronu.
                    result.addAll(group)
                } else {
                    // Od najnowszych - taka tez jest kolejnosc po rozwinieciu chevronem.
                    val sorted = group.sortedByDescending { it.date }
                    val newest = sorted.first()
                    val nick = newest.author?.nick ?: newest.body.substringBefore(" ")
                    sorted.forEach { it.tag = key }
                    result.add(
                        NotificationGroup(
                            newest = newest,
                            // Najstarsze NIEPRZECZYTANE, a gdy wszystkie przeczytane - najnowsze.
                            navigationTarget = sorted.lastOrNull { it.new } ?: newest,
                            othersCount =
                                sorted
                                    .mapNotNull { it.author?.nick }
                                    .filterNot { it == nick }
                                    .distinct()
                                    .size,
                            unreadCount = sorted.count { it.new },
                            groupKey = key,
                        ),
                    )
                    result.addAll(sorted)
                }
            }
        return result
    }

    /**
     * Klucz grupy = URL celu bez kotwicy komentarza. Powiadomienia bez kontekstu tresci
     * (systemowe, nowy obserwujacy, odznaka) dostaja klucz unikalny, zeby nigdy nie
     * trafialy do wspolnego wiersza - "odpowiedziano we wpisie" nie mialoby dla nich sensu.
     */
    private fun groupKeyOf(notification: Notification): String {
        val url = notification.url
        return if (notification.targetKind == NotificationTargetKind.OTHER || url == null) {
            "no-target-${notification.id}"
        } else {
            url.substringBefore("/#comment-")
        }
    }

    fun readNotifications() {
        notificationsApi
            .readNotifications()
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe({ view?.showReadToast() }, { view?.showErrorDialog(it) })
            .intoComposite(compositeObservable)
    }
}
