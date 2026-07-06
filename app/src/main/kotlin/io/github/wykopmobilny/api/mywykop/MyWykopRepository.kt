package io.github.wykopmobilny.api.mywykop

import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.v3.ObservedV3RetrofitApi
import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.models.dataclass.EntryLink
import io.github.wykopmobilny.models.mapper.apiv3.toEntryLink
import io.reactivex.Single
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

class MyWykopRepository
    @Inject
    constructor(
        private val observedApiV3: ObservedV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
    ) : MyWykopApi {
        override fun getIndex(page: String?): Single<FilteredData<EntryLink>> =
            rxSingle { observedApiV3.getObservedAll(page) }
                .map { response ->
                    FilteredData(
                        totalCount = response.data?.size ?: 0,
                        filtered = response.data.orEmpty().map { it.toEntryLink(owmContentFilter) },
                        nextPage = response.pagination?.next,
                    )
                }

        // Stare endpointy v2 (myWykopApi.byUsers/byTags) zwracaja HTTP 405 -
        // strumienie obserwowanych zyja w v3 jako /observed/users
        // i /observed/tags/stream (mieszane Link|Entry, paginacja hashowa).
        override fun byUsers(page: String?): Single<FilteredData<EntryLink>> =
            rxSingle { observedApiV3.getObservedUsers(page) }
                .retryWhen(userTokenRefresher)
                .map { response ->
                    FilteredData(
                        totalCount = response.data?.size ?: 0,
                        filtered = response.data.orEmpty().map { it.toEntryLink(owmContentFilter) },
                        nextPage = response.pagination?.next,
                    )
                }

        override fun byTags(page: String?): Single<FilteredData<EntryLink>> =
            rxSingle { observedApiV3.getObservedTagsStream(page) }
                .retryWhen(userTokenRefresher)
                .map { response ->
                    FilteredData(
                        totalCount = response.data?.size ?: 0,
                        filtered = response.data.orEmpty().map { it.toEntryLink(owmContentFilter) },
                        nextPage = response.pagination?.next,
                    )
                }
    }
