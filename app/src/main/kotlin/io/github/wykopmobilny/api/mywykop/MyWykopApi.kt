package io.github.wykopmobilny.api.mywykop

import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.models.dataclass.EntryLink
import io.reactivex.Single

interface MyWykopApi {
    fun getIndex(page: String?): Single<FilteredData<EntryLink>>

    fun byTags(page: String?): Single<FilteredData<EntryLink>>

    fun byUsers(page: String?): Single<FilteredData<EntryLink>>
}
