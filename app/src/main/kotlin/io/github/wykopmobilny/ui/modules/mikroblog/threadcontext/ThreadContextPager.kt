package io.github.wykopmobilny.ui.modules.mikroblog.threadcontext

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.ThreadBranchState
import io.github.wykopmobilny.models.dataclass.ThreadChunk
import io.github.wykopmobilny.models.dataclass.ThreadContext
import io.reactivex.disposables.CompositeDisposable

/**
 * Leniwe dociaganie poddrzewa na ekranie kontekstu watku.
 *
 * Pierwsze dwa zapytania (sciezka + komentarz docelowy z ekspansja) oddaja ekran
 * od razu; galezie, ktorych ekspansja nie domknela, zostaja opisane jako
 * [ThreadBranchState] i sa dociagane dopiero na zadanie widoku - gdy komentarz-rodzic
 * wejdzie w pole widzenia ([requestBranch]) albo gdy uzytkownik doscrolluje do dolu
 * ([loadNext]). Jedno zadanie = jedna strona odpowiedzi, wiec lista rosnie przyrostowo,
 * a nie skokiem po kilkunastu round-tripach.
 *
 * Model skopiowany z LinkCommentsPager (ekran komentarzy znaleziska), razem z trzema
 * zabezpieczeniami:
 * - [requested] - jeden fetch na galaz+kursor (dedup zadan ze scrolla),
 * - [isLoading] - jedno doladowanie naraz,
 * - [generation] - uniewaznienie doladowan w locie po odswiezeniu ekranu.
 *
 * Caly stan dotykany jest wylacznie z watku glownego (zadania startuje widok,
 * odpowiedzi wracaja przez `observeOn(mainThread)`), wiec nie potrzebuje synchronizacji.
 */
internal class ThreadContextPager(
    private val entriesApi: EntriesApi,
    private val schedulers: Schedulers,
    private val entryId: Long,
    private val disposables: CompositeDisposable,
    /** Nowa porcja poddrzewa - widok wkleja ja pod odpowiedniego rodzica. */
    private val onChunk: (ThreadChunk) -> Unit,
    /** Zmiana stanu "cos sie dociaga" - widok pokazuje wskaznik na dole listy. */
    private val onLoadingChanged: (Boolean) -> Unit,
) {
    /** Galezie do dociagniecia, kluczowane rodzicem; kolejnosc = kolejnosc odkrywania (preorder). */
    private val pending = LinkedHashMap<Long, ThreadBranchState>()

    /** Klucze galaz+kursor, ktore juz poleciecialy - dedup powtarzanych zadan ze scrolla. */
    private val requested = mutableSetOf<String>()

    /** Ile stron pobrano juz pod danym rodzicem - jedyna ochrona przed petla paginacji. */
    private val pagesPerBranch = mutableMapOf<Long, Int>()

    private var generation = 0L
    private var entryAuthorNick: String? = null

    var isLoading: Boolean = false
        private set

    /** Zaczyna od nowa po (prze)ladowaniu kontekstu - doladowania w locie przestaja sie liczyc. */
    fun reset(context: ThreadContext) {
        generation++
        isLoading = false
        requested.clear()
        pagesPerBranch.clear()
        pending.clear()
        context.pendingBranches.forEach { pending[it.parentId] = it }
        entryAuthorNick = context.entryAuthorNick
        onLoadingChanged(false)
    }

    /** Czy pod tym komentarzem API ma jeszcze odpowiedzi, ktorych nie pobralismy. */
    fun hasPending(commentId: Long) = pending.containsKey(commentId)

    /** Dociaga galaz konkretnego komentarza (wszedl w pole widzenia / jest priorytetowy). */
    fun requestBranch(parentId: Long): Boolean {
        val branch = pending[parentId] ?: return false
        return start(branch)
    }

    /** Dociaga pierwsza galaz z kolejki - uzywane, gdy uzytkownik doscrollowal do dolu. */
    fun loadNext(): Boolean {
        val branch = pending.values.firstOrNull() ?: return false
        return start(branch)
    }

    private fun start(branch: ThreadBranchState): Boolean {
        if (isLoading) return false
        val page = pagesPerBranch[branch.parentId] ?: 0
        if (page >= MAX_PAGES_PER_BRANCH) {
            pending.remove(branch.parentId)
            return false
        }
        val key = "${branch.parentId}:${branch.cursor}"
        if (!requested.add(key)) return false

        val startedAt = generation
        pagesPerBranch[branch.parentId] = page + 1
        isLoading = true
        onLoadingChanged(true)
        disposables.add(
            entriesApi
                .getThreadBranch(entryId = entryId, branch = branch, entryAuthorNick = entryAuthorNick)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { chunk ->
                        if (startedAt != generation) return@subscribe
                        isLoading = false
                        pending.remove(branch.parentId)
                        chunk.pendingBranches.forEach { pending[it.parentId] = it }
                        onLoadingChanged(false)
                        onChunk(chunk)
                    },
                    { error ->
                        if (startedAt != generation) return@subscribe
                        isLoading = false
                        // Blad sieci nie moze zamknac galezi na zawsze - zdejmujemy dedup,
                        // zeby kolejne wejscie w pole widzenia sprobowalo jeszcze raz.
                        requested.remove(key)
                        pagesPerBranch[branch.parentId] = page
                        onLoadingChanged(false)
                        Napier.w("Nie udalo sie doladowac odpowiedzi watku ${branch.parentId}", error, tag = TAG)
                    },
                ),
        )
        return true
    }

    private companion object {
        const val TAG = "ThreadContextPager"

        /**
         * Ochrona przed petla paginacji jednej galezi (np. gdyby API przestalo przesuwac
         * kursor). 40 stron po 25 odpowiedzi to 1000 bezposrednich odpowiedzi jednego
         * komentarza - progu nie da sie osiagnac realna dyskusja, a petla konczy sie od razu.
         */
        const val MAX_PAGES_PER_BRANCH = 40
    }
}
