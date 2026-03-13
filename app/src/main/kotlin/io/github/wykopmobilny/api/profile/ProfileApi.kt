package io.github.wykopmobilny.api.profile

import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.api.responses.ObserveStateResponse
import io.github.wykopmobilny.api.responses.v3.profile.BadgeResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserFullResponseV3
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryLink
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item
import io.github.wykopmobilny.models.dataclass.Related
import io.reactivex.Single

interface ProfileApi {
    fun getIndex(username: String): Single<UserFullResponseV3>

    fun getActions(username: String): Single<List<EntryLink>>

    fun getAdded(
        username: String,
        page: String? = null,
    ): Single<FilteredData<Link>>

    fun getPublished(
        username: String,
        page: String? = null,
    ): Single<FilteredData<Link>>

    fun getDigged(
        username: String,
        page: String? = null,
    ): Single<FilteredData<Link>>

    fun getBuried(
        username: String,
        page: String? = null,
    ): Single<FilteredData<Link>>

    fun getLinkComments(
        username: String,
        page: Int,
    ): Single<List<LinkCommentV3Item>>

    fun getEntries(
        username: String,
        page: Int,
    ): Single<FilteredData<Entry>>

    fun getEntriesComments(
        username: String,
        page: Int,
    ): Single<List<EntryComment>>

    fun getRelated(
        username: String,
        page: Int,
    ): Single<List<Related>>

    fun getBadges(
        username: String,
        page: Int,
    ): Single<List<BadgeResponseV3>>

    fun observe(tag: String): Single<ObserveStateResponse>

    fun unobserve(tag: String): Single<ObserveStateResponse>

    fun block(tag: String): Single<ObserveStateResponse>

    fun unblock(tag: String): Single<ObserveStateResponse>
}
