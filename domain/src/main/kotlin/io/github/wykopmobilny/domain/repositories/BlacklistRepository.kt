package io.github.wykopmobilny.domain.repositories

import io.github.wykopmobilny.api.endpoints.v3.BlacklistV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.SuggestV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.blacklist.BlacklistDomainRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import javax.inject.Inject

internal class BlacklistRepository
    @Inject
    constructor(
        private val blacklistApiV3: BlacklistV3RetrofitApi,
        private val suggestApiV3: SuggestV3RetrofitApi,
    ) {
        suspend fun getUsers(): List<String> =
            fetchAllPages { page -> blacklistApiV3.getBlacklistedUsers(page) }
                .map { it.username }

        suspend fun getTags(): List<String> =
            fetchAllPages { page -> blacklistApiV3.getBlacklistedTags(page) }
                .map { it.name }

        suspend fun getDomains(): List<String> =
            fetchAllPages { page -> blacklistApiV3.getBlacklistedDomains(page) }
                .map { it.domain }

        suspend fun blockDomain(domain: String) {
            blacklistApiV3.blockDomain(WykopApiRequestV3(BlacklistDomainRequestV3(domain = domain.trim())))
        }

        suspend fun unblockDomain(domain: String) {
            blacklistApiV3.unblockDomain(domain)
        }

        suspend fun suggestUsers(query: String): List<String> =
            suggestApiV3.getUserSuggestions(query).data.orEmpty().map { it.username }

        suspend fun suggestTags(query: String): List<String> =
            suggestApiV3.getTagSuggestions(query).data.orEmpty().map { it.name }

        // Listy czarnej listy sa paginowane (per_page=30). Pobieramy kolejne strony
        // (pierwsza page=null) az zbierzemy `total` elementow; brak `total` = jedna strona.
        private suspend fun <T> fetchAllPages(load: suspend (Int?) -> WykopApiResponseV3<List<T>>): List<T> {
            val all = mutableListOf<T>()
            var page: Int? = null
            while (true) {
                val response = load(page)
                val data = response.data.orEmpty()
                all += data
                val total = response.pagination?.total ?: break
                if (data.isEmpty() || all.size >= total) break
                page = (page ?: 1) + 1
            }
            return all
        }
    }
