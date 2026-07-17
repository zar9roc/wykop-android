package io.github.wykopmobilny.domain.microblog.feed

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.EntriesLastUpdate
import io.github.wykopmobilny.api.endpoints.v3.EntriesSort
import io.github.wykopmobilny.api.endpoints.v3.EntriesV3RetrofitApi
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.domain.microblog.feed.di.MicroblogFeedKey
import io.github.wykopmobilny.domain.microblog.feed.di.MicroblogFeedScope
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.entries.feed.MicroblogFeedSort
import io.github.wykopmobilny.kotlin.AppScopes
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * Forward-pager feedu mikrobloga (in-memory, bez Store5/cache - feed jest ephemeryczny).
 * `reload` czyści okno i pobiera pierwszą stronę wybranego trybu; `loadMore` dokleja
 * kolejną stronę po kursorze. `generation` unieważnia doładowania w locie po refresh/
 * zmianie trybu; dedup po id na sklejeniu.
 */
@MicroblogFeedScope
class EntryFeedPager
    @Inject
    internal constructor(
        private val key: MicroblogFeedKey,
        private val api: EntriesV3RetrofitApi,
        private val storage: MicroblogFeedStateStorage,
        private val appScopes: AppScopes,
    ) {
        private val generation = AtomicLong(0)
        private val isLoadingMore = AtomicBoolean(false)

        internal suspend fun initialLoad() {
            if (storage.state.value.sort != key.initialSort) {
                storage.update { it.copy(sort = key.initialSort) }
            }
            reload(key.initialSort)
        }

        /** Pełne przeładowanie okna dla danego trybu (refresh / zmiana trybu). */
        internal suspend fun reload(sort: MicroblogFeedSort) {
            val currentGeneration = generation.incrementAndGet()
            isLoadingMore.set(false)
            val response = fetch(sort, page = null)
            if (generation.get() != currentGeneration) return
            val data = response.data.orEmpty()
            val info = pageInfo(response, loadedPageNumber = 1)
            storage.update {
                it.copy(
                    entries = data,
                    sort = sort,
                    pageNumber = 1,
                    nextPage = info.nextPage,
                    hasMore = data.isNotEmpty() && info.hasMore,
                    isLoadingMore = false,
                    loaded = true,
                )
            }
        }

        /** Doładowanie kolejnej strony (scroll do dołu). Fire-and-forget. */
        fun loadMore() {
            val current = storage.state.value
            if (!current.hasMore || current.nextPage == null) return
            if (!isLoadingMore.compareAndSet(false, true)) return
            val currentGeneration = generation.get()
            val page = current.nextPage
            val sort = current.sort
            appScopes.safeKeyed<MicroblogFeedScope>(key) {
                try {
                    val response = fetch(sort, page = page)
                    if (generation.get() != currentGeneration) return@safeKeyed
                    val fresh = response.data.orEmpty()
                    storage.update { old ->
                        val known = old.entries.mapTo(HashSet()) { it.id }
                        val loadedPage = old.pageNumber + 1
                        val info = pageInfo(response, loadedPageNumber = loadedPage)
                        old.copy(
                            entries = old.entries + fresh.filterNot { it.id in known },
                            pageNumber = loadedPage,
                            nextPage = info.nextPage,
                            hasMore = info.hasMore,
                            isLoadingMore = false,
                        )
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    Napier.w("Failed to load next feed page", failure)
                    storage.update { it.copy(isLoadingMore = false) }
                } finally {
                    isLoadingMore.set(false)
                }
            }
            storage.update { it.copy(isLoadingMore = true) }
        }

        private suspend fun fetch(
            sort: MicroblogFeedSort,
            page: String?,
        ): WykopApiResponseV3<List<EntryResponseV3>> =
            api.getEntries(page = page, sort = sort.apiSort, lastUpdate = sort.lastUpdate)

        /**
         * Dwa tryby paginacji feedu v3 (patrz docs/api_v3_samples):
         * - zalogowany: odpowiedź niesie kursor `next` -> podążamy za nim, hasMore = next != null;
         * - niezalogowany: brak `next`, ale jest total+per_page -> stronicujemy numerem
         *   i znamy ostatnią stronę = ceil(total/per_page), hasMore = strona < ostatnia.
         * Fallback (brak metadanych): kontynuuj dopóki strona niepusta.
         */
        private fun pageInfo(
            response: WykopApiResponseV3<List<EntryResponseV3>>,
            loadedPageNumber: Int,
        ): PageInfo {
            response.pagination?.next?.let { cursor ->
                return PageInfo(nextPage = cursor, hasMore = true)
            }
            val total = response.pagination?.total
            val perPage = response.pagination?.perPage
            val hasMore =
                when {
                    total != null && perPage != null && perPage > 0 -> {
                        val lastPage = (total + perPage - 1) / perPage
                        loadedPageNumber < lastPage
                    }

                    else -> response.data.orEmpty().isNotEmpty()
                }
            return PageInfo(nextPage = (loadedPageNumber + 1).toString(), hasMore = hasMore)
        }

        private data class PageInfo(
            val nextPage: String?,
            val hasMore: Boolean,
        )
    }

private val MicroblogFeedSort.apiSort: EntriesSort
    get() =
        when (this) {
            MicroblogFeedSort.HOT_24, MicroblogFeedSort.HOT_12, MicroblogFeedSort.HOT_6, MicroblogFeedSort.HOT_2 -> EntriesSort.HOT
            MicroblogFeedSort.ACTIVE -> EntriesSort.ACTIVE
            MicroblogFeedSort.NEWEST -> EntriesSort.NEWEST
        }

private val MicroblogFeedSort.lastUpdate: EntriesLastUpdate?
    get() =
        when (this) {
            MicroblogFeedSort.HOT_24 -> EntriesLastUpdate.TWENTY_FOUR_HOURS
            MicroblogFeedSort.HOT_12 -> EntriesLastUpdate.TWELVE_HOURS
            MicroblogFeedSort.HOT_6 -> EntriesLastUpdate.SIX_HOURS
            MicroblogFeedSort.HOT_2 -> EntriesLastUpdate.TWO_HOURS
            MicroblogFeedSort.ACTIVE, MicroblogFeedSort.NEWEST -> null
        }
