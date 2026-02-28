package io.github.wykopmobilny.api.hits

import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.models.dataclass.Link
import io.reactivex.Single

interface HitsApi {
    fun currentWeek(page: Int = 1): Single<FilteredData<Link>>

    fun currentDay(page: Int = 1): Single<FilteredData<Link>>

    fun byMonth(
        year: Int,
        month: Int,
        page: Int = 1,
    ): Single<FilteredData<Link>>

    fun byYear(
        year: Int,
        page: Int = 1,
    ): Single<FilteredData<Link>>
}
