package io.github.wykopmobilny.domain.entrydetails

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.endpoints.v3.EntriesV3RetrofitApi
import io.github.wykopmobilny.api.responses.v3.entries.EntryCommentResponseV3
import io.github.wykopmobilny.domain.api.ApiClient
import io.github.wykopmobilny.domain.entrydetails.di.EntryDetailsKey
import io.github.wykopmobilny.domain.entrydetails.di.EntryDetailsScope
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.kotlin.AppScopes
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * Dwukierunkowy pager komentarzy wpisu (problem "strony 448").
 *
 * Zamiast ładować wszystkie strony po kolei, ekran startuje od strony-kotwicy
 * (z deep-linka `/strona/NNN` albo 1) i doładowuje sąsiednie strony przy
 * scrollu: w dół (loadNewer) i w górę (loadOlder). Skok do najnowszych
 * przeładowuje okno na ostatnią stronę wyliczoną z total/per_page.
 *
 * Wyniki lądują w [EntryDetailsStateStorage]; UI obserwuje wyłącznie stan.
 */
@EntryDetailsScope
class EntryCommentsPager
    @Inject
    internal constructor(
        private val key: EntryDetailsKey,
        private val api: EntriesV3RetrofitApi,
        private val apiClient: ApiClient,
        private val storage: EntryDetailsStateStorage,
        private val appScopes: AppScopes,
    ) {
        // Unieważnia doładowania w locie po refresh/jump (stan został zresetowany).
        private val generation = AtomicLong(0)
        private val isLoadingNewer = AtomicBoolean(false)
        private val isLoadingOlder = AtomicBoolean(false)

        /**
         * Pierwsze ładowanie: wpis + strona-kotwica komentarzy. Gdy kotwica z deep-linka
         * wypada poza zakres (wpis mógł stracić strony), spada na ostatnią stronę.
         */
        internal suspend fun initialLoad() {
            val currentGeneration = generation.incrementAndGet()
            isLoadingNewer.set(false)
            isLoadingOlder.set(false)

            val entry = apiClient.mutation { api.getEntry(key.entryId) }
            if (generation.get() != currentGeneration) return
            storage.update { it.copy(entry = entry) }

            val anchor = key.initialPage?.coerceAtLeast(1) ?: 1
            var response = fetchPage(anchor)
            var loadedPage = anchor
            if (anchor > 1 && response.data.orEmpty().isEmpty()) {
                // Kotwica poza zakresem - klamrujemy do ostatniej strony z paginacji.
                val lastPage = lastPageOf(response) ?: 1
                loadedPage = lastPage.coerceAtLeast(1)
                response = fetchPage(loadedPage)
            }
            if (generation.get() != currentGeneration) return

            val comments = response.data.orEmpty()
            storage.update {
                it.copy(
                    comments = comments,
                    oldestLoadedPage = loadedPage,
                    newestLoadedPage = loadedPage,
                    hasNewer = hasNextPage(loadedPage, comments.size, response),
                    totalCount = response.pagination?.total,
                    perPage = response.pagination?.perPage,
                    isLoadingOlder = false,
                    isLoadingNewer = false,
                )
            }
        }

        /** Doładowanie nowszej strony (scroll w dół). Fire-and-forget. */
        fun loadNewer() {
            val state = storage.state.value
            if (!state.hasNewer || state.newestLoadedPage == 0) return
            if (!isLoadingNewer.compareAndSet(false, true)) return
            val currentGeneration = generation.get()
            val page = state.newestLoadedPage + 1
            appScopes.safeKeyed<EntryDetailsScope>(key) {
                try {
                    val response = fetchPage(page)
                    if (generation.get() != currentGeneration) return@safeKeyed
                    val fresh = response.data.orEmpty()
                    storage.update { old ->
                        val known = old.comments.mapTo(HashSet()) { it.id }
                        old.copy(
                            comments = old.comments + fresh.filterNot { it.id in known },
                            newestLoadedPage = page,
                            hasNewer = hasNextPage(page, fresh.size, response),
                            totalCount = response.pagination?.total ?: old.totalCount,
                            perPage = response.pagination?.perPage ?: old.perPage,
                            isLoadingNewer = false,
                        )
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    Napier.w("Failed to load newer comments page=$page", failure)
                    storage.update { it.copy(isLoadingNewer = false) }
                } finally {
                    isLoadingNewer.set(false)
                }
            }
            storage.update { it.copy(isLoadingNewer = true) }
        }

        /** Doładowanie starszej strony (scroll w górę). Fire-and-forget. */
        fun loadOlder() {
            val state = storage.state.value
            if (!state.hasOlder) return
            if (!isLoadingOlder.compareAndSet(false, true)) return
            val currentGeneration = generation.get()
            val page = state.oldestLoadedPage - 1
            appScopes.safeKeyed<EntryDetailsScope>(key) {
                try {
                    val response = fetchPage(page)
                    if (generation.get() != currentGeneration) return@safeKeyed
                    val fresh = response.data.orEmpty()
                    storage.update { old ->
                        val known = old.comments.mapTo(HashSet()) { it.id }
                        old.copy(
                            comments = fresh.filterNot { it.id in known } + old.comments,
                            oldestLoadedPage = page,
                            totalCount = response.pagination?.total ?: old.totalCount,
                            perPage = response.pagination?.perPage ?: old.perPage,
                            isLoadingOlder = false,
                        )
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    Napier.w("Failed to load older comments page=$page", failure)
                    storage.update { it.copy(isLoadingOlder = false) }
                } finally {
                    isLoadingOlder.set(false)
                }
            }
            storage.update { it.copy(isLoadingOlder = true) }
        }

        /** Skok do najnowszych: przeładowanie okna na ostatnią stronę. */
        internal suspend fun jumpToNewest() {
            val lastKnown = storage.state.value.lastPage ?: return
            val currentGeneration = generation.incrementAndGet()
            isLoadingNewer.set(false)
            isLoadingOlder.set(false)

            var response = fetchPage(lastKnown)
            var loadedPage = lastKnown
            // Między wejściem a skokiem mogły dojść komentarze - paginacja z odpowiedzi
            // jest świeższa niż stan; doklejamy ewentualną nowszą ostatnią stronę.
            val freshLast = lastPageOf(response)
            if (freshLast != null && freshLast > lastKnown) {
                loadedPage = freshLast
                response = fetchPage(freshLast)
            }
            if (generation.get() != currentGeneration) return

            val comments = response.data.orEmpty()
            storage.update {
                it.copy(
                    comments = comments,
                    oldestLoadedPage = loadedPage,
                    newestLoadedPage = loadedPage,
                    hasNewer = hasNextPage(loadedPage, comments.size, response),
                    totalCount = response.pagination?.total ?: it.totalCount,
                    perPage = response.pagination?.perPage ?: it.perPage,
                    isLoadingOlder = false,
                    isLoadingNewer = false,
                )
            }
        }

        private suspend fun fetchPage(page: Int): WykopApiResponseV3<List<EntryCommentResponseV3>> =
            apiClient.mutation {
                // mutation() rozpakowuje pole data - opakowujemy całą odpowiedź,
                // żeby nie zgubić paginacji (total/per_page).
                val raw = api.getEntryComments(key.entryId, page = page)
                WykopApiResponseV3(data = raw, pagination = null)
            }

        private fun lastPageOf(response: WykopApiResponseV3<List<EntryCommentResponseV3>>): Int? {
            val total = response.pagination?.total?.takeIf { it > 0 } ?: return null
            val perPage = response.pagination?.perPage?.takeIf { it > 0 } ?: return null
            return (total + perPage - 1) / perPage
        }

        private fun hasNextPage(
            page: Int,
            receivedCount: Int,
            response: WykopApiResponseV3<List<EntryCommentResponseV3>>,
        ): Boolean {
            val lastPage = lastPageOf(response)
            if (lastPage != null) return page < lastPage
            // Brak paginacji w odpowiedzi - heurystyka jak w starym ekranie:
            // pełna strona = pewnie jest kolejna.
            val perPage = response.pagination?.perPage
            return receivedCount > 0 && (perPage == null || receivedCount >= perPage)
        }
    }
