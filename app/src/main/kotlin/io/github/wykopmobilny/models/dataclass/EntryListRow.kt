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
