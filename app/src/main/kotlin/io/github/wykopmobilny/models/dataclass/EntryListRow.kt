package io.github.wykopmobilny.models.dataclass

/**
 * Wiersz listy mieszanej wpisow i komentarzy. Komentarze ("najlepsze komentarze"
 * przy wpisie na listach, komentarze uzytkownika na profilu) sa osobnymi wierszami
 * adaptera renderowanymi tym samym szablonem co odpowiedzi pod wpisem - dzieki temu
 * lista czyta sie jak fragment dyskusji.
 */
sealed interface EntryListRow {
    data class EntryRow(
        val entry: Entry,
    ) : EntryListRow

    data class CommentRow(
        val comment: EntryComment,
    ) : EntryListRow

    /**
     * Komentarz znaleziska na liscie mieszanej (profil). [isReply] = odpowiedz w watku
     * (szablon odpowiedzi z wcieciem), inaczej komentarz glowny.
     */
    data class LinkCommentRow(
        val comment: LinkCommentV3Item,
        val isReply: Boolean,
    ) : EntryListRow

    /** Powiazane dodane do znaleziska [linkId] (profil > Znaleziska > Powiazane). */
    data class RelatedRow(
        val related: Related,
        val linkId: Long,
    ) : EntryListRow

    /** Wiersz list mieszanych wpis+znalezisko (MyWykop, profil > Akcje). */
    data class LinkRow(
        val entryLink: EntryLink,
    ) : EntryListRow
}

/** Wpis + jego komentarze jako kolejne wiersze listy. */
fun Entry.toRows(includeComments: Boolean): List<EntryListRow> =
    if (includeComments) {
        listOf(EntryListRow.EntryRow(this)) + comments.filterNot { it.isBlocked }.map(EntryListRow::CommentRow)
    } else {
        listOf(EntryListRow.EntryRow(this))
    }
