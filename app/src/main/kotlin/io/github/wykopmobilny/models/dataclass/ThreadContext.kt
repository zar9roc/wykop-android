package io.github.wykopmobilny.models.dataclass

/**
 * Dane ekranu kontekstu watku: sciezka w gore (wpis -> kolejni rodzice ->
 * komentarz docelowy) oraz TA CZESC poddrzewa, ktora juz mamy.
 *
 * Poddrzewo nie przychodzi w calosci: pierwsze zapytanie oddaje komentarz docelowy
 * razem z ekspansja (trzy poziomy w dol, ale tylko kilka pierwszych galezi), a reszta
 * dociagana jest leniwie - galaz po galezi, gdy wejdzie w pole widzenia. Galezie
 * czekajace na dociagniecie opisuje [pendingBranches].
 *
 * `EntryListRow` nie niesie poziomu zagniezdzenia, a UI potrzebuje go do wciec,
 * wiec potomkowie jada osobna struktura [ThreadDescendant] zamiast plaskiej listy wierszy.
 */
data class ThreadContext(
    /** Wpis, kolejni rodzice i na koncu komentarz docelowy. Nigdy nie jest pusta. */
    val path: List<EntryListRow>,
    /** `nesting` komentarza docelowego z API (wpis = 1, komentarz pierwszego poziomu = 2). */
    val targetNesting: Int,
    /** Poddrzewo w kolejnosci preorder: dziecko, potem jego wlasne poddrzewo. */
    val descendants: List<ThreadDescendant>,
    /** Galezie, w ktorych API ma wiecej odpowiedzi niz dostalismy - do dociagniecia na zadanie. */
    val pendingBranches: List<ThreadBranchState> = emptyList(),
) {
    /** Komentarz, na ktorym otwarto ekran - ostatni element sciezki. */
    val targetComment: EntryComment?
        get() = (path.lastOrNull() as? EntryListRow.CommentRow)?.comment

    /**
     * Nick autora wpisu - potrzebny mapperowi przy dociaganych galeziach, zeby adnotacja
     * o komentarzu usunietym przez autora watku wygladala tak samo jak w pierwszej partii.
     */
    val entryAuthorNick: String?
        get() = (path.firstOrNull() as? EntryListRow.EntryRow)?.entry?.author?.nick

    /** `nesting` dowolnego komentarza z ekranu; null, gdy go tu nie ma. */
    fun nestingOf(commentId: Long): Int? =
        when {
            targetComment?.id == commentId -> targetNesting
            else -> descendants.firstOrNull { it.comment.id == commentId }?.nesting
        }
}

/** Komentarz z poddrzewa pod komentarzem docelowym. */
data class ThreadDescendant(
    val row: EntryListRow.CommentRow,
    /** Glebokosc wzgledem komentarza docelowego: 1 = bezposrednia odpowiedz. */
    val depth: Int,
    /** Poziom zagniezdzenia z API (maksymalnie 7). */
    val nesting: Int,
) {
    val comment: EntryComment
        get() = row.comment
}

/**
 * Galaz do dociagniecia: komentarz, pod ktorym API ma wiecej bezposrednich odpowiedzi
 * niz dostalismy, razem z kursorem potrzebnym do pobrania brakujacej koncowki.
 *
 * Kursor to id ostatniej POSIADANEJ odpowiedzi - przy `sort=oldest` endpoint
 * `.../comments/{id}/comments?id=<kursor>` oddaje wylacznie elementy o wiekszym id,
 * wiec to, co przyszlo juz w ekspansji, nie zostanie pobrane drugi raz.
 */
data class ThreadBranchState(
    /** Komentarz-rodzic tej galezi (dla pierwszego poziomu - komentarz docelowy). */
    val parentId: Long,
    /** `nesting` rodzica - ponizej [io.github.wykopmobilny.api.endpoints.v3.EntriesV3RetrofitApi.THREAD_MAX_NESTING]. */
    val parentNesting: Int,
    /** Glebokosc DZIECI tej galezi wzgledem komentarza docelowego. */
    val childDepth: Int,
    /** Id ostatniej posiadanej odpowiedzi; null, gdy nie mamy jeszcze zadnej. */
    val cursor: Long?,
    /** Ile bezposrednich odpowiedzi tego rodzica juz mamy. */
    val loaded: Int,
    /** `comments.total` rodzica, czyli ile ich jest naprawde; null = API nie podalo. */
    val total: Int?,
)

/**
 * Porcja poddrzewa oddana przez jedno dociagniecie: nowi potomkowie w kolejnosci
 * preorder plus galezie, ktore z tej porcji zostaly do dociagniecia (kontynuacja
 * tego samego rodzica i niepelne ekspansje swiezo pobranych komentarzy).
 */
data class ThreadChunk(
    /** Rodzic, pod ktorego poddrzewo wchodzi ta porcja. */
    val parentId: Long,
    val descendants: List<ThreadDescendant>,
    val pendingBranches: List<ThreadBranchState>,
)
