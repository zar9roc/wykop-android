package io.github.wykopmobilny.models.dataclass

import io.github.wykopmobilny.utils.toPrettyDate
import kotlinx.datetime.Instant

class EntryComment(
    val id: Long,
    var entryId: Long,
    val author: Author,
    var body: String,
    val fullDate: Instant,
    var isVoted: Boolean,
    var embed: Embed?,
    var voteCount: Int,
    val app: String?,
    val violationUrl: String?,
    var isNsfw: Boolean = false,
    var isBlocked: Boolean = false,
    val deletedReason: String? = null,
    val slug: String? = null,
    // Nick autora wpisu, pod ktorym wisi komentarz. API v3 nie podaje go przy samym
    // komentarzu, wiec wypelniamy go z kontekstu wywolania (wpis z listy, wpis ekranu
    // szczegolow, pierwszy element sciezki watku). Null = kontekstu nie bylo i komunikat
    // o usunieciu przez autora watku musi zdegradowac sie do wersji bez nicka.
    val entryAuthorNick: String? = null,
) {
    override fun equals(other: Any?): Boolean =
        if (other !is EntryComment) {
            false
        } else {
            (other.id == id)
        }

    override fun hashCode(): Int = id.toInt()

    val url: String
        get() = "https://www.wykop.pl/wpis/$entryId/#comment-$id"

    val date: String
        get() = this.fullDate.toPrettyDate()
}
