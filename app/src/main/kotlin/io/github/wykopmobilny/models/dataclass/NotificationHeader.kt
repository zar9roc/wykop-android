package io.github.wykopmobilny.models.dataclass

class NotificationHeader(
    body: String,
    var notificationsCount: Int,
    // Tytul wyswietlany zamiast "#tag" - dla grup "Do mnie" (fragment tresci celu).
    val title: String? = null,
    // Nawigacja po kliknieciu naglowka - dla grup "Do mnie" URL wpisu/znaleziska
    // bez kotwicy komentarza. null = domyslne otwarcie TagActivity (zakladka tagow).
    val navigationUrl: String? = null,
) : Notification(0, null, body, null, "header", "", false) {
    override fun equals(other: Any?): Boolean =
        if (other !is NotificationHeader) {
            false
        } else {
            (other.body == body)
        }

    override fun hashCode(): Int = body.hashCode()
}
