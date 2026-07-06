package io.github.wykopmobilny.api.search

import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.Link
import io.reactivex.Single

interface SearchApi {
    fun searchLinks(
        page: String?,
        query: String,
    ): Single<FilteredData<Link>>

    fun searchEntries(
        page: String?,
        query: String,
    ): Single<FilteredData<Entry>>

    fun searchProfiles(query: String): Single<List<Author>>
}
