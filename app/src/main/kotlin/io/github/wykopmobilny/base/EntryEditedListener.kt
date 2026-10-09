package io.github.wykopmobilny.base

import io.github.wykopmobilny.models.dataclass.Entry

/**
 * Lista wpisow, ktora po edycji wpisu (powrot z EditEntryActivity do aktywnosci-hosta)
 * podmienia ten jeden wiersz na swieza wersje z API - bez przeladowania i przewijania listy.
 */
interface EntryEditedListener {
    fun onEntryEdited(entry: Entry)
}
