package io.github.wykopmobilny.models.dataclass

/**
 * Wiersz zbiorczy zakladki "Do mnie": jeden kontekst (wpis albo znalezisko) =
 * DOKLADNIE jeden wiersz. W odroznieniu od [NotificationHeader] nie jest osobnym
 * naglowkiem nad lista - sam jest podsumowaniem calej grupy, a powiadomienia
 * skladowe sa domyslnie zwiniete i pokazuja sie dopiero po kliknieciu chevronu.
 */
class NotificationGroup(
    // Najnowsze powiadomienie grupy - zrodlo avatara, nicka i daty wiersza.
    val newest: Notification,
    // Powiadomienie, pod ktore nawiguje klikniecie wiersza: najstarsze NIEPRZECZYTANE
    // z grupy (zeby widok przewinal sie wlasnie do niego - nowsze sa i tak pod spodem),
    // a gdy wszystkie sa przeczytane - najnowsze.
    val navigationTarget: Notification,
    // Liczba pozostalych (roznych) osob w grupie, bez autora pokazanego w nicku.
    val othersCount: Int,
    unreadCount: Int,
    groupKey: String,
) : Notification(
        // Wiersz zbiorczy nie jest powiadomieniem z API - identyfikator budujemy
        // z klucza grupy (URL celu). Prefiks odroznia go zarowno od hashy
        // prawdziwych powiadomien, jak i od naglowkow.
        id = "group:$groupKey",
        author = newest.author,
        body = newest.body,
        date = newest.date,
        type = newest.type,
        url = navigationTarget.url,
        new = unreadCount > 0,
        targetKind = newest.targetKind,
        targetSlug = newest.targetSlug,
        targetTitle = newest.targetTitle,
    ) {
    // Licznik NIEPRZECZYTANYCH powiadomien w grupie (zielona liczba na wierszu).
    // var, bo klikniecie wiersza/dziecka zdejmuje jedno powiadomienie z puli.
    var unreadCount: Int = unreadCount

    val nick: String
        get() = author?.nick ?: body.substringBefore(" ")

    init {
        tag = groupKey
    }
}
