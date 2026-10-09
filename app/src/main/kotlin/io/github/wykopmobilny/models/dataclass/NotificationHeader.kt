package io.github.wykopmobilny.models.dataclass

/**
 * Naglowek-akordeon zakladki tagow. Nie pochodzi z API, wiec identyfikator
 * budujemy z jego tresci (nazwy taga) - prefiks gwarantuje, ze nie zderzy sie
 * z hashem prawdziwego powiadomienia ani z wierszem zbiorczym.
 */
class NotificationHeader(
    body: String,
    var notificationsCount: Int,
    // Tytul wyswietlany zamiast "#tag" - dla grup "Do mnie" (fragment tresci celu).
    val title: String? = null,
    // Nawigacja po kliknieciu naglowka - dla grup "Do mnie" URL wpisu/znaleziska
    // bez kotwicy komentarza. null = domyslne otwarcie TagActivity (zakladka tagow).
    val navigationUrl: String? = null,
) : Notification("header:$body", null, body, null, "header", "", false)
