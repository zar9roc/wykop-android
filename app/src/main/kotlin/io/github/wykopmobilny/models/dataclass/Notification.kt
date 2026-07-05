package io.github.wykopmobilny.models.dataclass

import kotlinx.datetime.Instant

open class Notification(
    val id: Long,
    val author: Author?,
    val body: String,
    val date: Instant?,
    val type: String,
    val url: String?,
    var new: Boolean,
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

    override fun hashCode(): Int = id.toInt()
}
