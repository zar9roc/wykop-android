package io.github.wykopmobilny.api.hits

import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.models.dataclass.Link
import io.reactivex.Single

interface HitsApi {
    fun currentWeek(page: String? = null): Single<FilteredData<Link>>

    fun currentDay(page: String? = null): Single<FilteredData<Link>>

    fun byMonth(
        year: Int,
        month: Int,
        page: String? = null,
    ): Single<FilteredData<Link>>

    fun byYear(
        year: Int,
        page: String? = null,
    ): Single<FilteredData<Link>>
}
