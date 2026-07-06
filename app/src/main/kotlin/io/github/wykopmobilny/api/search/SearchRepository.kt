package io.github.wykopmobilny.api.search

import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.v3.SearchV3RetrofitApi
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.models.mapper.apiv3.AuthorMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.filterEntriesV3
import io.github.wykopmobilny.models.mapper.apiv3.filterLinksV3
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

// Stare endpointy wyszukiwania v1/v2 juz nie dzialaja - v3: /search/links|entries|users.
class SearchRepository
    @Inject
    constructor(
        private val searchApiV3: SearchV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
    ) : SearchApi {
        override fun searchLinks(
            page: String?,
            query: String,
        ) = rxSingle { searchApiV3.searchLinks(query, page) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                response.data.orEmpty().filterLinksV3(owmContentFilter, response.pagination)
            }

        override fun searchEntries(
            page: String?,
            query: String,
        ) = rxSingle { searchApiV3.searchEntries(query, page) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                response.data.orEmpty().filterEntriesV3(owmContentFilter, response.pagination)
            }

        override fun searchProfiles(query: String) =
            rxSingle { searchApiV3.searchUsers(query) }
                .retryWhen(userTokenRefresher)
                .map { it.data.orEmpty().map { response -> AuthorMapperV3.map(response) } }
    }
