package io.github.wykopmobilny.ui.modules.tag

/**
 * Implementowane przez zakladki tagu (Znaleziska/Wpisy). Pozwala aktywnosci
 * (ktora jest wlascicielem menu) narzucic wspolny filtr - sortowanie
 * (najnowsze/najlepsze) oraz opcjonalne archiwum (rok + opcjonalny miesiac) -
 * na obie zakladki i przeladowac liste.
 */
interface TagFilterableFragment {
    fun applyTagFilter(
        sort: String,
        year: Int?,
        month: Int?,
    )
}
