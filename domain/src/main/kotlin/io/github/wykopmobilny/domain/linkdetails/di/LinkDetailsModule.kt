package io.github.wykopmobilny.domain.linkdetails.di

import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.domain.api.ApiClient
import io.github.wykopmobilny.domain.di.ScopeInitializer
import io.github.wykopmobilny.domain.linkdetails.GetLinkDetailsQuery
import io.github.wykopmobilny.domain.linkdetails.InitializeLinkDetails
import io.github.wykopmobilny.domain.linkdetails.LinkComment
import io.github.wykopmobilny.domain.linkdetails.RelatedLink
import io.github.wykopmobilny.domain.linkdetails.datasource.linkCommentsSourceOfTruth
import io.github.wykopmobilny.domain.linkdetails.datasource.linkDetailsSourceOfTruth
import io.github.wykopmobilny.domain.linkdetails.datasource.persistLinkComments
import io.github.wykopmobilny.domain.linkdetails.datasource.relatedLinksSourceOfTruth
import io.github.wykopmobilny.domain.profile.LinkInfo
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.links.details.GetLinkDetails
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.kotlin.AppScopes
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicLong

@Module
internal abstract class LinkDetailsModule {
    companion object {
        @LinkDetailsScope
        @Provides
        fun linkDetailsStore(
            retrofitApi: LinksV3RetrofitApi,
            appScopes: AppScopes,
            cache: AppCache,
            apiClient: ApiClient,
        ): Store<Long, LinkInfo> =
            StoreBuilder
                .from(
                    fetcher = apiClient.fetcher(retrofitApi::getLink),
                    sourceOfTruth = linkDetailsSourceOfTruth(cache),
                ).scope(appScopes.applicationScope)
                .build()

        /**
         * API v3 zwraca na /links/{id}/comments TYLKO watki nadrzedne (stronicowane) -
         * odpowiedzi zyja pod /comments/{commentId}/comments.
         *
         * Fetcher pobiera i emituje WYLACZNIE watki nadrzedne (1-2 szybkie requesty),
         * zeby lista renderowala sie od razu. Odpowiedzi dociagane sa w tle, watek po
         * watku, z zapisem kazdego watku prosto do cache - reader source of truth
         * (flow z sqldelight) doklada je do widoku przyrostowo. Fetcher NIE moze
         * emitowac czesciowych wynikow (petla restartow z flowSourceOfTruth), a caly
         * sekwencyjny przebieg potrafi trwac minute przez rate-limiting API
         * (throttlowane odpowiedzi po 3-7s).
         *
         * Nawigacja z powiadomienia (key.initialCommentId) ma priorytet: watek
         * zawierajacy wskazany komentarz jest dociagany jako pierwszy.
         */
        @LinkDetailsScope
        @Provides
        fun linkComments(
            key: LinkDetailsKey,
            retrofitApi: LinksV3RetrofitApi,
            appScopes: AppScopes,
            cache: AppCache,
            apiClient: ApiClient,
        ): Store<Long, Map<LinkComment, List<LinkComment>>> {
            // Kolejny fetch (np. pull-to-refresh) uniewaznia poprzednie doladowywanie.
            val repliesGeneration = AtomicLong(0)
            return StoreBuilder
                .from(
                    fetcher =
                        apiClient.fetcher { linkId ->
                            val parents =
                                fetchAllPages { page ->
                                    retrofitApi.getLinkComments(linkId = linkId, sortBy = "oldest", page = page)
                                }
                            val generation = repliesGeneration.incrementAndGet()
                            appScopes.safeKeyed<LinkDetailsScope>(key) {
                                fetchRepliesInBackground(
                                    retrofitApi = retrofitApi,
                                    cache = cache,
                                    linkId = linkId,
                                    parents = parents,
                                    priorityCommentId = key.initialCommentId,
                                    isCurrent = { repliesGeneration.get() == generation },
                                )
                            }
                            WykopApiResponseV3(data = parents, pagination = null)
                        },
                    sourceOfTruth = linkCommentsSourceOfTruth(cache),
                ).scope(appScopes.applicationScope)
                .build()
        }

        /**
         * Sekwencyjnie (rownolegly burst wyzwala rate-limiting API) dociaga odpowiedzi
         * dla kazdego watku i zapisuje je do cache od razu po pobraniu.
         */
        private suspend fun fetchRepliesInBackground(
            retrofitApi: LinksV3RetrofitApi,
            cache: AppCache,
            linkId: Long,
            parents: List<LinkCommentResponseV3>,
            priorityCommentId: Long?,
            isCurrent: () -> Boolean,
        ) {
            // Watki nadrzedne najpierw do cache - odpowiedzi nie moga wyladowac
            // w bazie przed rodzicami (reader pomija sieroty).
            persistLinkComments(cache, linkId, parents)

            val threads =
                parents
                    .filter { (it.comments?.count ?: UNKNOWN_REPLY_COUNT) != 0 }
                    .take(MAX_REPLY_THREADS)
            val priorityThreadId = resolvePriorityThreadId(retrofitApi, linkId, threads, priorityCommentId)
            Napier.d(
                "replies loader: priorityCommentId=$priorityCommentId resolvedThread=$priorityThreadId threads=${threads.size}",
                tag = "LinkCommentsLoader",
            )
            val ordered = threads.sortedByDescending { it.id == priorityThreadId }

            for (parent in ordered) {
                if (!isCurrent()) return
                val replies =
                    runCatching {
                        fetchAllPages { page ->
                            retrofitApi.getLinkCommentReplies(linkId = linkId, commentId = parent.id, page = page)
                        }
                    }.getOrElse { failure ->
                        if (failure is CancellationException) throw failure
                        emptyList()
                    }
                if (!isCurrent()) return
                if (replies.isNotEmpty()) {
                    persistLinkComments(cache, linkId, replies)
                }
            }
        }

        /**
         * Komentarz z powiadomienia moze byc watkiem nadrzednym albo odpowiedzia -
         * dla odpowiedzi rodzica wskazuje pojedynczy GET komentarza.
         */
        private suspend fun resolvePriorityThreadId(
            retrofitApi: LinksV3RetrofitApi,
            linkId: Long,
            threads: List<LinkCommentResponseV3>,
            priorityCommentId: Long?,
        ): Long? {
            priorityCommentId ?: return null
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
            while (perPage > 0 && lastPageSize >= perPage && page <= MAX_COMMENT_PAGES) {
                val data = fetchPage(page).data.orEmpty()
                all += data
                lastPageSize = data.size
                page++
            }
            return all
        }

        // Bezpieczniki dla bardzo dlugich dyskusji.
        private const val MAX_COMMENT_PAGES = 50
        private const val MAX_REPLY_THREADS = 100

        // Gdy API nie zwraca licznika odpowiedzi, probujemy dociagnac watek mimo wszystko.
        private const val UNKNOWN_REPLY_COUNT = -1

        @LinkDetailsScope
        @Provides
        fun relatedLinksStore(
            retrofitApi: LinksV3RetrofitApi,
            appScopes: AppScopes,
            cache: AppCache,
            apiClient: ApiClient,
        ): Store<Long, List<RelatedLink>> =
            StoreBuilder
                .from(
                    fetcher = apiClient.fetcher(retrofitApi::getRelated),
                    sourceOfTruth = relatedLinksSourceOfTruth(cache),
                ).scope(appScopes.applicationScope)
                .build()
    }

    @Binds
    abstract fun getProfileDetails(impl: GetLinkDetailsQuery): GetLinkDetails

    @Binds
    abstract fun getRelatedLinks(
        impl: io.github.wykopmobilny.domain.linkdetails.GetRelatedLinksQuery,
    ): io.github.wykopmobilny.links.details.GetRelatedLinks

    @Binds
    abstract fun refreshRelatedLinks(
        impl: io.github.wykopmobilny.domain.linkdetails.GetRelatedLinksQuery,
    ): io.github.wykopmobilny.links.details.RefreshRelatedLinks

    @Binds
    abstract fun addRelatedLink(
        impl: io.github.wykopmobilny.domain.linkdetails.GetRelatedLinksQuery,
    ): io.github.wykopmobilny.links.details.AddRelatedLink

    @Binds
    abstract fun scopeInitializer(impl: InitializeLinkDetails): ScopeInitializer
}
