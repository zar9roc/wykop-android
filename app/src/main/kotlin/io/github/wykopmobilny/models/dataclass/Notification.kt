package io.github.wykopmobilny.models.dataclass

import kotlinx.datetime.Instant

/**
 * Rodzaj celu powiadomienia - decyduje o tresci wiersza zbiorczego
 * ("we wpisie" / "w znalezisku"). OTHER = powiadomienie bez kontekstu tresci
 * (systemowe, nowy obserwujacy, odznaka) - takie nigdy nie tworzy grupy.
 */
enum class NotificationTargetKind {
    ENTRY,
    LINK,
    OTHER,
}

open class Notification(
    // Identyfikator z API v3 - tekstowy hash (np. "3QVgryak"), nie liczba.
    // Wiersze syntetyczne (naglowek, wiersz zbiorczy) dostaja wlasne prefiksowane
    // identyfikatory, wiec nigdy nie koliduja z prawdziwymi powiadomieniami.
    val id: String,
    val author: Author?,
    val body: String,
    val date: Instant?,
    val type: String,
    val url: String?,
    var new: Boolean,
    // Cel powiadomienia (wpis / znalezisko) wraz ze slugiem - zasila tresc wiersza
    // zbiorczego w zakladce "Do mnie". Zakladka tagow tych pol nie uzywa.
    val targetKind: NotificationTargetKind = NotificationTargetKind.OTHER,
    val targetSlug: String? = null,
    // Tresc celu (wpis: tresc wpisu, znalezisko: tytul) - juz oczyszczona z HTML
    // i przycieta. Ma pierwszenstwo przed slugiem w tresci wiersza zbiorczego.
    val targetTitle: String? = null,
) {
    var visible = true

    // Klucz grupowania akordeonu. Zakladka tagow: nazwa taga wyciagana z tresci (fallback);
    // zakladka "Do mnie": ustawiany jawnie na URL celu przy grupowaniu po wpisie/znalezisku.
    var tag: String = ""
        get() = field.ifEmpty { body.substringAfter("#").substringBefore(" ") }

    override fun equals(other: Any?): Boolean =
        if (other !is Notification) {
            false
        } else {
            (other.id == id)
        }

    override fun hashCode(): Int = id.hashCode()
}
