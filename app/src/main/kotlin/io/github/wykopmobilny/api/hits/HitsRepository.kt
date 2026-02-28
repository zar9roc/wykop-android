package io.github.wykopmobilny.api.hits

import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.v3.HitsV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.models.mapper.apiv3.filterLinksV3
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

class HitsRepository
    @Inject
    constructor(
        private val hitsApiV3: HitsV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
    ) : HitsApi {
        override fun byMonth(
            year: Int,
            month: Int,
            page: Int,
        ) = rxSingle { hitsApiV3.getHits(page = page, sort = "all", year = year, month = month) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<List<LinkResponseV3>>())
            .map { links -> links.filterLinksV3(owmContentFilter) }

        override fun currentDay(page: Int) =
            rxSingle { hitsApiV3.getHits(page = page, sort = "day") }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<LinkResponseV3>>())
                .map { links -> links.filterLinksV3(owmContentFilter) }

        override fun byYear(
            year: Int,
            page: Int,
        ) = rxSingle { hitsApiV3.getHits(page = page, sort = "all", year = year) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<List<LinkResponseV3>>())
            .map { links -> links.filterLinksV3(owmContentFilter) }

        override fun currentWeek(page: Int) =
            rxSingle { hitsApiV3.getHits(page = page, sort = "week") }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<LinkResponseV3>>())
                .map { links -> links.filterLinksV3(owmContentFilter) }
    }
