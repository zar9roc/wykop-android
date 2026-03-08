package io.github.wykopmobilny.api.mywykop

import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.MyWykopRetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.ObservedV3RetrofitApi
import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformer
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.patrons.PatronsApi
import io.github.wykopmobilny.models.dataclass.EntryLink
import io.github.wykopmobilny.models.mapper.apiv2.EntryLinkMapper
import io.github.wykopmobilny.models.mapper.apiv3.toEntryLink
import io.reactivex.Single
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

class MyWykopRepository
    @Inject
    constructor(
        private val myWykopApi: MyWykopRetrofitApi,
        private val observedApiV3: ObservedV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
        private val patronsApi: PatronsApi,
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

        override fun byUsers(page: Int): Single<List<EntryLink>> =
            rxSingle { myWykopApi.byUsers(page) }
                .retryWhen(userTokenRefresher)
                .flatMap { patronsApi.ensurePatrons(it) }
                .compose(ErrorHandlerTransformer())
                .map { it.map { response -> EntryLinkMapper.map(response, owmContentFilter) } }

        override fun byTags(page: Int): Single<List<EntryLink>> =
            rxSingle { myWykopApi.byTags(page) }
                .retryWhen(userTokenRefresher)
                .flatMap { patronsApi.ensurePatrons(it) }
                .compose(ErrorHandlerTransformer())
                .map { it.map { response -> EntryLinkMapper.map(response, owmContentFilter) } }
    }
