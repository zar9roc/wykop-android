package io.github.wykopmobilny.domain.linkdetails

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.domain.linkdetails.datasource.persistLinkComments
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsKey
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.kotlin.AppScopes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import io.github.wykopmobilny.kotlin.AppDispatchers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * Leniwe ładowanie komentarzy znaleziska (API v3).
 *
 * Fetcher Store'a pobiera TYLKO pierwszą stronę wątków nadrzędnych (żeby lista
 * renderowała się od razu) i dociąga w tle wyłącznie wątek z powiadomienia
 * (priorytet, dla scroll-to-comment). Kolejne strony wątków oraz odpowiedzi dla
 * pozostałych wątków są dociągane leniwie — na żądanie z widoku, gdy wątek wchodzi
 * w viewport ([loadReplies]) lub gdy użytkownik doscrolluje do dołu
 * ([loadMoreTopLevel]). Każde pobranie zapisuje wynik prosto do cache; source of
 * truth (flow z SQLDelight) dokłada je do widoku przyrostowo.
 *
 * Dedup: [requestedReplyThreads] gwarantuje jeden fetch odpowiedzi na wątek,
 * [isLoadingMore] jeden fetch kolejnej strony naraz. [generation] unieważnia
 * doładowania w locie po odświeżeniu (nowy fetchFirstPage) i — przez [refreshes] —
 * sygnalizuje widokowi, że odpowiedzi widocznych wątków trzeba pobrać od nowa.
 */
@LinkDetailsScope
class LinkCommentsPager
    @Inject
    constructor(
        private val key: LinkDetailsKey,
        private val retrofitApi: LinksV3RetrofitApi,
        private val cache: AppCache,
        private val appScopes: AppScopes,
    ) {
        @Volatile
        private var nextPage: Int? = null
        private val isLoadingMore = AtomicBoolean(false)
        private val generation = AtomicLong(0)
        private val requestedReplyThreads = ConcurrentHashMap.newKeySet<Long>()
        private val refreshCounter = MutableStateFlow(0L)

        /** Zmienia sie po kazdym pobraniu pierwszej strony (otwarcie / swipe-refresh / nowy komentarz). */
        val refreshes: StateFlow<Long> = refreshCounter.asStateFlow()

        suspend fun fetchFirstPage(linkId: Long): WykopApiResponseV3<List<LinkCommentResponseV3>> {
            val currentGeneration = generation.incrementAndGet()
            requestedReplyThreads.clear()
            isLoadingMore.set(false)
            val response = retrofitApi.getLinkComments(linkId = linkId, sortBy = "oldest", page = null)
            val parents = response.data.orEmpty()
            // Odpowiedzi juz zaladowanych watkow tez sa nieaktualne - widok pobierze je
            // ponownie dla watkow w viewporcie (dedup wyczyszczony wyzej).
            refreshCounter.value = currentGeneration
            nextPage = nextPageOrNull(currentPage = 1, received = parents.size, perPage = response.pagination?.perPage)

            val priorityCommentId = key.initialCommentId
            if (priorityCommentId != null) {
                appScopes.safeKeyed<LinkDetailsScope>(key) {
                    if (generation.get() != currentGeneration) return@safeKeyed
                    val threadId = resolvePriorityThreadId(linkId, parents, priorityCommentId)
                    if (threadId != null && requestedReplyThreads.add(threadId)) {
                        loadRepliesInternal(linkId, threadId) { generation.get() == currentGeneration }
                    }
                }
            }
            return WykopApiResponseV3(data = parents, pagination = null)
        }

        fun loadMoreTopLevel(linkId: Long) {
            val currentGeneration = generation.get()
            appScopes.safeKeyed<LinkDetailsScope>(key) {
                val page = nextPage ?: return@safeKeyed
                if (!isLoadingMore.compareAndSet(false, true)) return@safeKeyed
                try {
                    val response = retrofitApi.getLinkComments(linkId = linkId, sortBy = "oldest", page = page)
                    if (generation.get() != currentGeneration) return@safeKeyed
                    val data = response.data.orEmpty()
                    nextPage = nextPageOrNull(currentPage = page, received = data.size, perPage = response.pagination?.perPage)
                    if (data.isNotEmpty()) persistLinkComments(cache, linkId, data)
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    Napier.w("Nie udalo sie doladowac komentarzy (strona $page)", failure, tag = "LinkCommentsPager")
                } finally {
                    isLoadingMore.set(false)
                }
            }
        }

        fun loadReplies(
            linkId: Long,
            commentId: Long,
        ) {
            val currentGeneration = generation.get()
            if (!requestedReplyThreads.add(commentId)) return
            appScopes.safeKeyed<LinkDetailsScope>(key) {
                val ok = loadRepliesInternal(linkId, commentId) { generation.get() == currentGeneration }
                // Błąd sieci → pozwól spróbować ponownie przy kolejnym wejściu w viewport.
                if (!ok) requestedReplyThreads.remove(commentId)
            }
        }

        /**
         * Komentarz dodany przez uzytkownika trafia od razu do cache (bez czekania na
         * odswiezenie, ktore i tak nie obejmuje odpowiedzi ani dalszych stron watkow),
         * a wątek, w którym odpowiedział, pobieramy od nowa.
         */
        fun onCommentPosted(
            linkId: Long,
            comment: LinkCommentResponseV3,
            parentId: Long?,
        ) {
            val posted = if (comment.parentId == null && parentId != null) comment.copy(parentId = parentId) else comment
            appScopes.safeKeyed<LinkDetailsScope>(key) {
                persistLinkComments(cache, linkId, listOf(posted))
                if (parentId != null) {
                    requestedReplyThreads.add(parentId)
                    val currentGeneration = generation.get()
                    loadRepliesInternal(linkId, parentId) { generation.get() == currentGeneration }
                }
            }
        }

        private suspend fun loadRepliesInternal(
            linkId: Long,
            commentId: Long,
            isCurrent: () -> Boolean,
        ): Boolean =
            try {
                val replies =
                    fetchAllPages { page ->
                        retrofitApi.getLinkCommentReplies(linkId = linkId, commentId = commentId, page = page)
                    }
                if (isCurrent()) {
                    if (replies.isNotEmpty()) persistLinkComments(cache, linkId, replies)
                    withContext(AppDispatchers.IO) {
                        cache.linkCommentsQueries.deleteThreadRepliesExcept(
                            linkId = linkId,
                            parentId = commentId,
                            keepIds = replies.map { it.id },
                        )
                    }
                }
                true
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                Napier.w("Nie udalo sie doladowac odpowiedzi watku $commentId", failure, tag = "LinkCommentsPager")
                false
            }

        private suspend fun resolvePriorityThreadId(
            linkId: Long,
            threads: List<LinkCommentResponseV3>,
            priorityCommentId: Long,
        ): Long? {
            if (threads.any { it.id == priorityCommentId }) return priorityCommentId
            return runCatching {
                retrofitApi.getLinkComment(linkId = linkId, commentId = priorityCommentId).data?.parentId
            }.getOrElse { failure ->
                if (failure is CancellationException) throw failure
                null
            }
        }

        private suspend fun fetchAllPages(
            fetchPage: suspend (page: Int?) -> WykopApiResponseV3<List<LinkCommentResponseV3>>,
        ): List<LinkCommentResponseV3> {
            val first = fetchPage(null)
            val all = first.data.orEmpty().toMutableList()
            val perPage = first.pagination?.perPage ?: return all
            var lastPageSize = all.size
            var page = 2
            while (perPage > 0 && lastPageSize >= perPage && page <= MAX_REPLY_PAGES) {
                val data = fetchPage(page).data.orEmpty()
                all += data
                lastPageSize = data.size
                page++
            }
            return all
        }

        private fun nextPageOrNull(
            currentPage: Int,
            received: Int,
            perPage: Int?,
        ): Int? =
            when {
                received == 0 -> null
                perPage != null && perPage > 0 && received < perPage -> null
                else -> currentPage + 1
            }

        private companion object {
            const val MAX_REPLY_PAGES = 50
        }
    }
